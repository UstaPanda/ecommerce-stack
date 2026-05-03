from typing import TypedDict, Optional, List, Any, Annotated
import operator
from langchain_openai import ChatOpenAI
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

# Initialize OpenAI API
openai_api_key = os.getenv("OPENAI_API_KEY")
if not openai_api_key:
    logger.critical("OPENAI_API_KEY not found in environment.")

# Model IDs for robustness
model_names = [
    "gpt-4o",
    "gpt-4o-mini"
]

def create_llm_chain(temperature=0):
    """Creates an OpenAI LLM with fallbacks."""
    llms = [
        ChatOpenAI(model=name, api_key=openai_api_key, temperature=temperature, max_retries=2) 
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
                       "1. PUBLIC DATA: Products, categories, stores, and reviews are PUBLIC. "
                       "   - Users CAN ask about other stores' public info, top sellers, review averages, and market comparisons.\n"
                       "2. PERSONAL DATA: Users CAN query their own specific orders, history, and profile.\n"
                       "3. RESTRICTED DATA: PII (emails, addresses) of OTHER users is FORBIDDEN. Platform-wide financial totals (total site revenue) are restricted to ADMIN.\n\n"
                       "Current Context: Role={user_role}\n\n"
                       "If the query asks for market trends, store comparisons, or review analysis using public tables, it is 'IN_SCOPE'.\n"
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
                       "- Questions about product listings, store info, ratings, and categories → query WITHOUT any user_id filter.\n\n"
                       "RBAC Rules for PERSONAL data (STRICT):\n"
                       "- ADMIN: Full access to all tables, no filters.\n"
                       "- CORPORATE: \n"
                       "  * Accessing 'stores': Filter by 'owner_id = {user_id}'.\n"
                       "  * Accessing 'orders', 'order_items': MUST JOIN with 'stores' and filter by 'stores.owner_id = {user_id}'.\n"
                       "  * Accessing 'shipments': Join with 'orders' and 'stores', filter by 'stores.owner_id = {user_id}'.\n"
                       "- INDIVIDUAL: Filter by 'user_id = {user_id}' ONLY on personal tables: 'orders', 'customer_profiles', 'carts', 'cart_items'.\n\n"
                       "ENTITY HANDLING (CRITICAL):\n"
                       "- 'OUR PRODUCT' / 'MY PRODUCT': If the user refers to 'their product' but doesn't name it, YOU MUST AGGREGATE across ALL products in their store. Use a subquery to find their store_id via owner_id.\n"
                       "- 'BIGGEST COMPETITOR': For non-admins, identify the 'biggest store' using PUBLIC metrics like COUNT(products) or COUNT(reviews). Compare your store's AGGREGATE reviews (median/average) against the competitor store's AGGREGATE reviews.\n\n"
                       "COMPETITOR ANALYSIS (NON-ADMINS):\n"
                       "- You are STICKTLY FORBIDDEN from querying 'orders' or 'order_items' for stores that do not belong to the user.\n"
                       "- NEVER reveal another store's revenue or exact sales volume.\n\n"
                       "IMPORTANT RULES:\n"
                       "- STORE REVENUE & ORDERS (CRITICAL): When calculating total orders or revenue for a store (that the user OWNS), YOU MUST QUERY THE 'orders' TABLE DIRECTLY (GROUP BY orders.store_id).\n"
                       "- ANTI-FAN-OUT: NEVER join 'orders' and 'reviews' in the same flat query. Use separate CTEs.\n"
                       "- REVENUE CALCULATIONS: ALWAYS exclude orders with status 'CANCELLED' or 'RETURNED'.\n"
                       "- Always use LEFT JOIN for optional data tables.\n"
                       "- Never use SQL line comments (--).\n\n"
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
        
        # Log the generated SQL for transparency
        logger.info(f"Generated SQL: {sql_query}")
        
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
