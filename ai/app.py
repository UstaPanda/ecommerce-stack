import chainlit as cl
from chainlit.input_widget import Select, TextInput
from graph import create_graph
from langchain_core.messages import HumanMessage, AIMessage
import pandas as pd
import plotly.express as px
import plotly.graph_objects as go

app = create_graph()

@cl.on_chat_start
async def on_chat_start():
    # Initialize message history in the session
    cl.user_session.set("history", [])
    
    # Define settings for switching roles
    settings = await cl.ChatSettings([
        Select(
            id="user_role",
            label="Kullanıcı Rolü",
            values=["ADMIN", "CORPORATE", "INDIVIDUAL"],
            initial_index=0,
        ),
        TextInput(
            id="user_id",
            label="Kullanıcı ID (Kurumsal/Bireysel için)",
            initial="1",
        )
    ]).send()
    
    # Store initial settings in session
    cl.user_session.set("user_role", settings["user_role"])
    cl.user_session.set("user_id", int(settings["user_id"]))
    
    welcome_msg = (
        "**ZorluKurt Trading** Yapay Zeka Analiz Asistanına Hoş Geldiniz! 📈\n\n"
        "E-ticaret verilerinizi analiz edebilir ve anında grafikler oluşturabilirim.\n"
        "**Not:** Rolünüzü ve Kullanıcı ID'nizi sol taraftaki ayarlar panelinden değiştirebilirsiniz.\n\n"
        "Şunları sormayı deneyin:\n"
        "- *Kategoriye göre satışları göster*\n"
        "- *Gelire göre en iyi 5 müşterim kim?*\n"
        "- *Bu ayı geçen ayla karşılaştır*"
    )
    await cl.Message(content=welcome_msg).send()

@cl.on_settings_update
async def setup_agent(settings):
    # Update session when settings change
    cl.user_session.set("user_role", settings["user_role"])
    try:
        uid = int(settings["user_id"])
    except ValueError:
        uid = 1
    cl.user_session.set("user_id", uid)
    await cl.Message(content=f"Ayarlar güncellendi: Rol **{settings['user_role']}**, Kullanıcı ID **{uid}** olarak ayarlandı.").send()

@cl.on_message
async def on_message(message: cl.Message):
    user_role = cl.user_session.get("user_role")
    user_id = cl.user_session.get("user_id")
    history = cl.user_session.get("history", [])

    initial_state = {
        "question": message.content,
        "history": history,
        "user_role": user_role,
        "user_id": user_id,
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

    # Track the full state throughout the stream
    current_state = initial_state.copy()
    
    # Stream the multi-agent workflow
    async with cl.Step(name="AI Agent Workflow") as main_step:
        for output in app.stream(initial_state):
            for node_name, node_update in output.items():
                # Merge the node's output into our current tracking state
                current_state.update(node_update)
                
                async with cl.Step(name=node_name.replace("_", " ").title()) as step:
                    if node_name == "guardrail":
                        step.output = f"In Scope: {node_update.get('is_in_scope')}"
                        if not node_update.get('is_in_scope'):
                            step.is_error = True
                    elif node_name == "generate_sql":
                        step.output = f"```sql\n{node_update.get('sql_query')}\n```"
                    elif node_name == "execute_sql":
                        if node_update.get("error"):
                            step.output = f"Error: {node_update.get('error')}"
                            step.is_error = True
                        else:
                            rows = len(node_update.get("query_result", []))
                            step.output = f"Query executed successfully. Retrieved {rows} rows."
                    elif node_name == "fix_sql":
                        step.output = f"Fixed SQL:\n```sql\n{node_update.get('sql_query')}\n```"
                    elif node_name == "analyze":
                        step.output = node_update.get("final_answer")
                    elif node_name == "visualize":
                        step.output = f"```python\n{node_update.get('visualization_code')}\n```"
        
    # Handle Greetings or Out of Scope using the merged state
    if current_state.get("is_greeting") or not current_state.get("is_in_scope"):
        response = current_state.get("final_answer")
        if not response:
            response = "Üzgünüm, sadece e-ticaret analizleri ile ilgili soruları yanıtlayabilirim."
        await cl.Message(content=response).send()
        
        # Update history
        history.append(HumanMessage(content=message.content))
        history.append(AIMessage(content=response))
        cl.user_session.set("history", history[-10:])
        return

    answer_content = current_state.get("final_answer")
    elements = []
    
    # Execute Visualization Code to render Plotly Chart in UI
    if current_state.get("visualization_code") and current_state.get("query_result"):
        code = current_state.get("visualization_code")
        query_result = current_state.get("query_result")
        
        try:
            # Provide the necessary execution context
            local_vars = {"query_result": query_result, "pd": pd, "px": px, "go": go}
            exec(code, {}, local_vars)
            
            if "fig" in local_vars:
                fig = local_vars["fig"]
                # Add Plotly element to Chainlit response
                elements.append(cl.Plotly(name="Analytics Chart", figure=fig, display="inline"))
        except Exception as e:
            answer_content += f"\n\n> ⚠️ *Note: Could not generate visualization due to an error: {e}*"

    await cl.Message(content=answer_content, elements=elements).send()
    
    # Update history for the next turn
    history.append(HumanMessage(content=message.content))
    history.append(AIMessage(content=answer_content))
    cl.user_session.set("history", history[-10:]) # Keep last 10 messages for context
