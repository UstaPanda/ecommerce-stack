from typing import TypedDict, Optional, List, Any, Annotated
import operator
from langchain_google_genai import ChatGoogleGenerativeAI
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder
from langchain_core.output_parsers import StrOutputParser
from langchain_core.messages import BaseMessage, HumanMessage, AIMessage
from pydantic import BaseModel, Field
import os
import re
import logging
from dotenv import load_dotenv
from db_utils import execute_sql, get_schema_info

# Setup logging
logger = logging.getLogger("AI-Agents")

load_dotenv()

# Initialize direct Google Gemini API
google_api_key = os.getenv("GOOGLE_API_KEY")
if not google_api_key:
    logger.critical("GOOGLE_API_KEY not found in environment.")

# Model IDs for robustness (Original specific order for high RPM/RPD)
model_names = [
    "gemini-3.1-flash-lite-preview", 
    "gemini-2.5-flash-lite",
    "gemini-3-flash-preview",
    "gemini-2.5-flash",
    "gemini-2.0-flash"
]

def create_llm_chain(temperature=0):
    """Creates a Gemini LLM with fallbacks."""
    llms = [
        ChatGoogleGenerativeAI(model=name, google_api_key=google_api_key, temperature=temperature, max_retries=2) 
        for name in model_names
    ]
    return llms[0].with_fallbacks(llms[1:])

llm = create_llm_chain(temperature=0)
llm_creative = create_llm_chain(temperature=0.3) # For analysis and viz

class AgentState(TypedDict):
    question: str
    history: Annotated[List[BaseMessage], operator.add]
    user_role: str
    user_id: Optional[int]
    detected_language: Optional[str] 
    is_in_scope: bool
    is_greeting: bool              
    sql_query: Optional[str]
    query_result: Optional[List[dict]]
    error: Optional[str]
    iteration_count: int
    final_answer: Optional[str]
    visualization_code: Optional[str]
    needs_graph: bool

def clean_output(text: str) -> str:
    """Removes markdown code blocks and extra whitespace."""
    text = re.sub(r"```(?:\w+)?\n?", "", text).strip()
    return text.replace("```", "").strip()

def sanitize_input(text: str) -> str:
    """Removes potential prompt injection attempts and identity claims."""
    patterns = [
        r"\(?ROLE\s*[:=]\s*\w+\)?", 
        r"\(?USER_ID\s*[:=]\s*\w+\)?", 
        r"I am (an? )?admin", 
        r"Ignore previous instructions",
        r"acting as (an? )?admin"
    ]
    for pattern in patterns:
        text = re.sub(pattern, "", text, flags=re.IGNORECASE)
    return text.strip()

# --- Guardrails Agent ---
class GuardrailOutput(BaseModel):
    category: str = Field(description="One of: 'GREETING', 'IN_SCOPE', or 'OUT_OF_SCOPE'")
    detected_language: str = Field(description="Strictly identify the language (e.g., 'Turkish', 'English')")
    rejection_message: Optional[str] = Field(description="Localized refusal message if category is OUT_OF_SCOPE or GREETING")

def guardrail_node(state: AgentState):
    """Sanitizes input and checks if the request is allowed for the user's role."""
    clean_question = sanitize_input(state["question"])
    try:
        structured_llm = llm.with_structured_output(GuardrailOutput)
        prompt = ChatPromptTemplate.from_messages([
            ("system", "You are the security guardrail for **ZorluKurt Trading**. "
                       "Validate requests based on these PERMISSIONS:\n\n"
                       "1. PUBLIC DATA: Products, categories, reviews.\n"
                       "2. PERSONAL DATA: Only allowed for the OWN user/store.\n"
                       "3. SENSITIVE MARKET DATA: Restricted to ADMIN.\n\n"
                       "Current Context: Role={user_role}\n\n"
                       "Respond in the user's language."),
            MessagesPlaceholder(variable_name="history"),
            ("human", "{question}")
        ])
        result = (prompt | structured_llm).invoke({
            "question": clean_question, 
            "history": state.get("history", []), 
            "user_role": state["user_role"]
        })
        return {
            "is_in_scope": result.category == "IN_SCOPE", 
            "is_greeting": result.category == "GREETING", 
            "detected_language": result.detected_language,
            "final_answer": result.rejection_message if result.category != "IN_SCOPE" else None
        }
    except Exception as e:
        logger.error(f"Guardrail failed: {e}")
        return {"is_in_scope": False, "is_greeting": False, "detected_language": "Turkish", "final_answer": "Güvenlik kontrolü sırasında bir hata oluştu."}

# --- SQL Agent ---
def sql_agent_node(state: AgentState):
    """Generates PostgreSQL query based on schema and RBAC rules."""
    schema = get_schema_info()
    try:
        prompt = ChatPromptTemplate.from_messages([
            ("system", "You are a senior PostgreSQL expert for **ZorluKurt Trading**. Language: {detected_language}.\n"
                       "Generate exactly ONE raw PostgreSQL query.\n\n"
                       "PUBLIC DATA (no user filter, applies to ALL roles including INDIVIDUAL):\n"
                       "- 'products', 'categories', 'stores', 'reviews' tables are PUBLIC.\n"
                       "- Questions about popular products, best sellers, product listings, store info, categories → query WITHOUT any user_id filter.\n\n"
                       "RBAC Rules for PERSONAL data (STRICT):\n"
                       "- ADMIN: Full access to all tables, no filters.\n"
                       "- CORPORATE: \n"
                       "  * Accessing 'stores': Filter by 'owner_id = {user_id}'.\n"
                       "  * Accessing 'orders', 'order_items': MUST JOIN with 'stores' and filter by 'stores.owner_id = {user_id}'.\n"
                       "  * Accessing 'shipments': Join with 'orders' and 'stores', filter by 'stores.owner_id = {user_id}'.\n"
                       "- INDIVIDUAL: Filter by 'user_id = {user_id}' ONLY on personal tables: 'orders', 'customer_profiles', 'carts', 'cart_items' (join carts on carts.user_id).\n"
                       "  * NEVER add user_id filter when querying 'products', 'categories', or 'reviews'.\n\n"
                       "IMPORTANT RULES:\n"
                       "- REVENUE CALCULATIONS: When calculating revenue or total sales, ALWAYS exclude orders with status 'CANCELLED' or 'RETURNED' (status NOT IN ('CANCELLED', 'RETURNED')).\n"
                       "- For popular/best-selling products: use LEFT JOIN, never filter by user_id. Example: SELECT p.id, p.name, p.unit_price, COALESCE(COUNT(oi.id),0) AS order_count, COALESCE(AVG(r.rating),0) AS avg_rating FROM products p LEFT JOIN order_items oi ON oi.product_id = p.id LEFT JOIN reviews r ON r.product_id = p.id GROUP BY p.id ORDER BY order_count DESC, avg_rating DESC LIMIT 10\n"
                       "- Always use LEFT JOIN (never INNER JOIN) for optional data tables like order_items, reviews, shipments.\n"
                       "- Never use SQL line comments (--) inside the query.\n\n"
                       "Return ONLY raw SQL inside markdown: ```sql [QUERY] ```\n\n"
                       "Schema:\n{schema}"),
            MessagesPlaceholder(variable_name="history"),
            ("human", "{question}")
        ])
        
        chain = prompt | llm | StrOutputParser()
        response = chain.invoke({
            "question": sanitize_input(state["question"]),
            "history": state.get("history", []),
            "detected_language": state.get("detected_language", "English"),
            "schema": schema,
            "user_role": state["user_role"],
            "user_id": state.get("user_id", "NULL")
        })
        
        sql_match = re.search(r"```sql\n?(.*?)\n?```", response, re.DOTALL | re.IGNORECASE)
        sql_query = sql_match.group(1).strip() if sql_match else clean_output(response)
            
        sql_query = re.sub(r'--[^\n]*', '', sql_query).strip()
        return {"sql_query": sql_query, "iteration_count": 0, "error": None}
    except Exception as e:
        logger.error(f"SQL Agent failed: {e}")
        return {"error": str(e)}

# --- Error Agent ---
def error_agent_node(state: AgentState):
    """Attempts to fix a failing SQL query."""
    schema = get_schema_info()
    try:
        prompt = ChatPromptTemplate.from_messages([
            ("system", "The SQL failed. Fix it while maintaining RBAC for {user_role} (user_id={user_id}).\n"
                       "REMINDER: 'products', 'categories', 'reviews' are PUBLIC — never add user_id filter on them.\n"
                       "Always use LEFT JOIN instead of INNER JOIN for optional tables.\n"
                       "Never use SQL line comments (--).\n"
                       "Failed Query: {sql_query}\nError: {error}\n"
                       "Schema:\n{schema}\n"
                       "Return ONLY raw SQL without any markdown or comments."),
            ("human", "{question}")
        ])
        fixed_sql = (prompt | llm | StrOutputParser()).invoke({
            "question": state["question"],
            "schema": schema,
            "sql_query": state["sql_query"],
            "error": state["error"],
            "user_role": state["user_role"],
            "user_id": state.get("user_id", "NULL")
        })
        return {"sql_query": clean_output(fixed_sql), "iteration_count": state["iteration_count"] + 1, "error": None}
    except Exception as e:
        return {"error": str(e)}

# --- Execution & Analysis ---
def execute_sql_node(state: AgentState):
    """Executes the generated SQL via the DB utility."""
    query = state.get("sql_query")
    if not query: 
        return {"error": "No query generated."}
    
    result = execute_sql(query)
    if isinstance(result, dict) and "error" in result:
        return {"error": result["error"]}
    
    return {"query_result": result, "error": None}

def analysis_node(state: AgentState):
    """Analyzes query results and generates a natural language response."""
    try:
        class AnalysisOutput(BaseModel):
            final_answer: str = Field(description="Natural language summary.")
            needs_graph: bool = Field(description="True if a chart would help visualize this data.")
        
        prompt = ChatPromptTemplate.from_messages([
            ("system", "You are a data analyst for **ZorluKurt Trading**. Language: {detected_language}.\n"
                       "Summarize the findings from the data provided. Be accurate and professional."),
            MessagesPlaceholder(variable_name="history"),
            ("human", "Question: {question}\nResults: {results}")
        ])
        
        result = (prompt | llm_creative.with_structured_output(AnalysisOutput)).invoke({
            "question": state["question"],
            "history": state.get("history", []),
            "results": str(state["query_result"]) if state.get("query_result") is not None else "No data found.",
            "detected_language": state.get("detected_language", "English")
        })
        return {"final_answer": result.final_answer, "needs_graph": result.needs_graph}
    except Exception as e:
        logger.error(f"Analysis node failed: {e}")
        return {"final_answer": "Veri analizi sırasında bir hata oluştu.", "needs_graph": False}

def visualization_node(state: AgentState):
    """Generates Plotly Python code for visualization."""
    try:
        prompt = ChatPromptTemplate.from_messages([
            ("system", "Generate Python Plotly code. Language: {detected_language}.\n"
                       "Data is in a list of dicts called 'query_result'.\n"
                       "1. df = pd.DataFrame(query_result)\n"
                       "2. Create 'fig' using px or go.\n"
                       "IMPORTANT: Do NOT use fig.show() or any command that opens a browser. Assign the chart to the variable 'fig' only.\n"
                       "Return ONLY raw python code."),
            ("human", "{question}. Results: {results}")
        ])
        viz_code = (prompt | llm_creative | StrOutputParser()).invoke({
            "question": state["question"], 
            "results": str(state["query_result"]),
            "detected_language": state.get("detected_language", "English")
        })
        return {"visualization_code": clean_output(viz_code)}
    except Exception as e:
        logger.error(f"Visualization node failed: {e}")
        return {"visualization_code": None}
