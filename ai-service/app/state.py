from typing import Any, TypedDict


class LoanAnalysisState(TypedDict, total=False):
    application: dict[str, Any]
    documents: list[dict[str, Any]]
    document_result: dict[str, Any]
    eligibility_result: dict[str, Any]
    risk_result: dict[str, Any]
    verification_summary: dict[str, Any]
    metrics: dict[str, Any]
