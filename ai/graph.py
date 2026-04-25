from langgraph.graph import StateGraph, END
import logging
from agents import (
    AgentState, guardrail_node, sql_agent_node, execute_sql_node,
    error_agent_node, analysis_node, visualization_node
)

# Configure logging for the graph
logger = logging.getLogger("Graph-Orchestrator")

def create_graph():
    """
    Creates a production-ready LangGraph for the ZorluKurt Trading AI Analytics.
    Flow: Guardrail -> [SQL Gen -> Execute (Loop if Error) -> Analyze -> Visualize]
    """
    workflow = StateGraph(AgentState)

    # 1. Define Nodes
    workflow.add_node("guardrail", guardrail_node)
    workflow.add_node("generate_sql", sql_agent_node)
    workflow.add_node("execute_sql", execute_sql_node)
    workflow.add_node("fix_sql", error_agent_node)
    workflow.add_node("analyze", analysis_node)
    workflow.add_node("visualize", visualization_node)

    # 2. Define Entry Point
    workflow.set_entry_point("guardrail")

    # 3. Guardrail Logic (Router)
    def router_guardrail(state: AgentState):
        if state.get("is_greeting"):
            logger.info("Guardrail: Detected greeting. Ending flow.")
            return "end"
        if state.get("is_in_scope"):
            logger.info("Guardrail: Request in scope. Proceeding to SQL Generation.")
            return "generate_sql"
        logger.warning("Guardrail: Request OUT OF SCOPE. Ending flow.")
        return "end"

    workflow.add_conditional_edges(
        "guardrail",
        router_guardrail,
        {
            "generate_sql": "generate_sql",
            "end": END
        }
    )

    # 4. Standard Flow
    workflow.add_edge("generate_sql", "execute_sql")

    # 5. SQL Execution Router (Self-Correction Loop)
    def router_execution(state: AgentState):
        if state.get("error"):
            if state.get("iteration_count", 0) < 3:
                logger.warning(f"Execution Error: {state['error']}. Attempting fix (Iteration {state['iteration_count'] + 1})")
                return "fix_sql"
            else:
                logger.error("Execution Error: Max retries reached. Moving to analysis with error.")
                return "analyze"
        
        logger.info("Execution: Success. Moving to analysis.")
        return "analyze"

    workflow.add_conditional_edges(
        "execute_sql",
        router_execution,
        {
            "fix_sql": "fix_sql",
            "analyze": "analyze"
        }
    )

    # 6. Fix Loop
    workflow.add_edge("fix_sql", "execute_sql")

    # 7. Visualization Router
    def router_visualization(state: AgentState):
        if state.get("needs_graph") and state.get("query_result"):
            logger.info("Analysis: Chart requested and data available. Moving to visualization.")
            return "visualize"
        
        logger.info("Analysis: No chart needed or no data. Ending flow.")
        return "end"

    workflow.add_conditional_edges(
        "analyze",
        router_visualization,
        {
            "visualize": "visualize",
            "end": END
        }
    )

    # 8. Final Edge
    workflow.add_edge("visualize", END)

    # Compile the graph
    app = workflow.compile()
    logger.info("ZorluKurt Trading Agent Graph compiled successfully.")
    return app

if __name__ == "__main__":
    # Basic validation check
    logging.basicConfig(level=logging.INFO)
    graph = create_graph()
    print("Graph structure validated.")
