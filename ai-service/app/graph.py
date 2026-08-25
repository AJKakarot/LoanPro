from langgraph.graph import END, START, StateGraph

from app.agents.document_agent import run_document_agent
from app.agents.eligibility_agent import run_eligibility_agent
from app.agents.risk_agent import run_risk_agent
from app.agents.summary_agent import run_summary_agent
from app.schemas.analysis import AnalyzeResponse
from app.state import LoanAnalysisState


def document_node(state: LoanAnalysisState) -> dict:
    result = run_document_agent(state.get("application") or {}, state.get("documents") or [])
    return {"document_result": result.model_dump(by_alias=True)}


def eligibility_node(state: LoanAnalysisState) -> dict:
    result, metrics = run_eligibility_agent(state.get("application") or {})
    return {
        "eligibility_result": result.model_dump(by_alias=True),
        "metrics": metrics,
    }


def risk_node(state: LoanAnalysisState) -> dict:
    result = run_risk_agent(
        state.get("application") or {},
        state.get("documents") or [],
        state.get("document_result") or {},
        state.get("eligibility_result") or {},
        state.get("metrics") or {},
    )
    return {"risk_result": result.model_dump(by_alias=True)}


def summary_node(state: LoanAnalysisState) -> dict:
    result = run_summary_agent(
        state.get("document_result") or {},
        state.get("eligibility_result") or {},
        state.get("risk_result") or {},
    )
    return {"verification_summary": result.model_dump(by_alias=True)}


def build_graph():
    graph = StateGraph(LoanAnalysisState)
    graph.add_node("document_agent", document_node)
    graph.add_node("eligibility_agent", eligibility_node)
    graph.add_node("risk_agent", risk_node)
    graph.add_node("verification_summary_agent", summary_node)
    graph.add_edge(START, "document_agent")
    graph.add_edge("document_agent", "eligibility_agent")
    graph.add_edge("eligibility_agent", "risk_agent")
    graph.add_edge("risk_agent", "verification_summary_agent")
    graph.add_edge("verification_summary_agent", END)
    return graph.compile()


loan_graph = build_graph()


def analyze_loan(application: dict, documents: list[dict]) -> AnalyzeResponse:
    final_state = loan_graph.invoke(
        {
            "application": application or {},
            "documents": documents or [],
        }
    )
    return AnalyzeResponse.model_validate(
        {
            "documentAnalysis": final_state.get("document_result"),
            "eligibilityAnalysis": final_state.get("eligibility_result"),
            "riskAnalysis": final_state.get("risk_result"),
            "verificationSummary": final_state.get("verification_summary"),
        }
    )
