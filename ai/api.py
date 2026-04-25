from fastapi import FastAPI, HTTPException, Depends, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from pydantic import BaseModel, Field
from typing import Optional, Any, List
import jwt
import os
import base64
import pandas as pd
import plotly.express as px
import plotly.graph_objects as go
from dotenv import load_dotenv
import logging
import time

# Load early
load_dotenv()

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger("API-Server")

from graph import create_graph
from db_utils import get_user_by_email
import uvicorn

app = FastAPI(
    title="ZorluKurt Trading: AI Analytics API",
    description="Secure AI-powered analytics for e-commerce data",
    version="1.0.0"
)

# CORS Middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"], # Tighten this in real production
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

graph_app = create_graph()
security = HTTPBearer()

# Secret handling - ensure it's loaded and valid
RAW_SECRET = os.getenv("JWT_SECRET")
if not RAW_SECRET:
    logger.warning("JWT_SECRET not found in environment. Using development fallback.")
    RAW_SECRET = "yahwvg2vqvz33035x26+vmQ9PzfRwdE/EneUx0SHsT4="

try:
    JWT_SECRET = base64.b64decode(RAW_SECRET)
except Exception:
    JWT_SECRET = RAW_SECRET

ALGORITHM = "HS256"

class ChatRequest(BaseModel):
    question: str = Field(..., example="What were the total sales last month?")
    history: Optional[List[dict]] = Field(default_factory=list)

class ChatResponse(BaseModel):
    final_answer: Optional[str]
    sql_query: Optional[str]
    query_result: Optional[List[Any]] = None
    visualization_code: Optional[str]
    visualization_data: Optional[Any] = None
    is_in_scope: bool
    error: Optional[str]
    execution_time: float

@app.middleware("http")
async def add_process_time_header(request: Request, call_next):
    start_time = time.time()
    response = await call_next(request)
    process_time = time.time() - start_time
    response.headers["X-Process-Time"] = str(process_time)
    return response

async def get_current_user(credentials: HTTPAuthorizationCredentials = Depends(security)):
    """Decodes the JWT token and extracts user info with DB validation."""
    token = credentials.credentials
    try:
        payload = jwt.decode(token, JWT_SECRET, algorithms=[ALGORITHM])
        email = payload.get("sub")
        
        if email is None:
            raise HTTPException(status_code=401, detail="Invalid token: missing subject")
            
        user_data = get_user_by_email(email)
        if not user_data:
            logger.error(f"User with email {email} not found in database during JWT validation")
            raise HTTPException(status_code=401, detail="User not found in system")
            
        return {
            "user_id": int(user_data["id"]), 
            "user_role": user_data["role_type"],
            "email": email
        }
    except jwt.ExpiredSignatureError:
        raise HTTPException(status_code=401, detail="Token has expired")
    except jwt.InvalidTokenError as e:
        logger.error(f"JWT Invalid: {e}")
        raise HTTPException(status_code=401, detail=f"Invalid token: {str(e)}")
    except Exception as e:
        logger.error(f"Auth Error: {e}")
        raise HTTPException(status_code=401, detail="Authentication failed")

import numpy as np

def convert_numpy(obj):
    if isinstance(obj, np.ndarray):
        return obj.tolist()
    if isinstance(obj, np.generic):
        return obj.item()
    if isinstance(obj, dict):
        return {k: convert_numpy(v) for k, v in obj.items()}
    if isinstance(obj, list):
        return [convert_numpy(i) for i in obj]
    return obj

@app.post("/api/chat/ask", response_model=ChatResponse)
async def ask_question(request: ChatRequest, user: dict = Depends(get_current_user)):
    start_time = time.time()
    logger.info(f"Question from {user['email']} ({user['user_role']}): {request.question}")
    
    try:
        initial_state = {
            "question": request.question,
            "history": request.history or [], 
            "user_role": user["user_role"],
            "user_id": user["user_id"],
            "is_in_scope": True,
            "is_greeting": False,
            "sql_query": None,
            "query_result": None,
            "error": None,
            "iteration_count": 0,
            "final_answer": None,
            "visualization_code": None,
            "needs_graph": False
        }
        
        # Invoke LangGraph
        final_state = graph_app.invoke(initial_state)
        
        viz_data = None
        viz_code = final_state.get("visualization_code")
        query_result = final_state.get("query_result")

        if viz_code and query_result:
            try:
                # Execution sandbox for Plotly
                local_vars = {"query_result": query_result, "pd": pd, "px": px, "go": go}
                exec(viz_code, {}, local_vars)
                if "fig" in local_vars:
                    viz_data = convert_numpy(local_vars["fig"].to_dict())
            except Exception as viz_err:
                logger.error(f"Visualization execution failed: {viz_err}")

        # Security: Only ADMIN sees internal SQL and Code
        is_admin = user["user_role"] == "ADMIN"
        
        execution_time = time.time() - start_time
        
        return ChatResponse(
            final_answer=final_state.get("final_answer"),
            sql_query=final_state.get("sql_query") if is_admin else None,
            query_result=convert_numpy(query_result) if is_admin else None,
            visualization_code=viz_code if is_admin else None,
            visualization_data=viz_data,
            is_in_scope=final_state.get("is_in_scope"),
            error=final_state.get("error"),
            execution_time=execution_time
        )
    except Exception as e:
        logger.exception("Internal Server Error during chat processing")
        raise HTTPException(status_code=500, detail="An internal error occurred while processing your request.")

@app.get("/health")
async def health_check():
    return {"status": "healthy", "timestamp": time.time()}

if __name__ == "__main__":
    import sys
    port = int(os.getenv("PORT", 8001))
    # Windows does not support socket inheritance for multi-worker mode
    if sys.platform == "win32":
        uvicorn.run(app, host="0.0.0.0", port=port, reload=False)
    else:
        uvicorn.run("api:app", host="0.0.0.0", port=port, reload=False, workers=4)
