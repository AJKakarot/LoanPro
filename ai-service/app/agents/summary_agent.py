from app.llm import invoke_structured, payload_json
from app.schemas.analysis import DISCLAIMER, VerificationSummary

SYSTEM = """You write concise verification summaries for loan makers and checkers.
You never approve or reject a loan.
Always include that a human reviewer must make the final decision.
Keep the summary short and operational."""


def _heuristic(document_result: dict, eligibility_result: dict, risk_result: dict) -> VerificationSummary:
    issues = []
    issues.extend(document_result.get("missingDocuments") or [])
    issues.extend(document_result.get("mismatches") or [])
    issues.extend(eligibility_result.get("warnings") or [])
    issues.extend(risk_result.get("riskFactors") or [])
    checks = list(risk_result.get("manualChecks") or [])
    if not checks:
        checks = ["Complete maker verification of customer, documents and financials."]
    summary = (
        f"Documents: {document_result.get('status')}. "
        f"Eligibility: {eligibility_result.get('assessment')}. "
        f"Risk: {risk_result.get('riskLevel')}. "
        f"{DISCLAIMER}"
    )
    return VerificationSummary(
        document_status=str(document_result.get("status") or "REVIEW_REQUIRED"),
        eligibility_assessment=str(eligibility_result.get("assessment") or "REVIEW_REQUIRED"),
        risk_level=str(risk_result.get("riskLevel") or "MEDIUM"),
        key_issues=list(dict.fromkeys(str(item) for item in issues if item))[:8],
        recommended_manual_checks=list(dict.fromkeys(checks))[:8],
        summary=summary,
        disclaimer=DISCLAIMER,
    )


def run_summary_agent(document_result: dict, eligibility_result: dict, risk_result: dict) -> VerificationSummary:
    fallback = _heuristic(document_result, eligibility_result, risk_result)
    user = (
        "Combine these agent outputs into a maker/checker briefing. Never decide the loan.\n\n"
        f"Documents:\n{payload_json(document_result)}\n\n"
        f"Eligibility:\n{payload_json(eligibility_result)}\n\n"
        f"Risk:\n{payload_json(risk_result)}"
    )
    result = invoke_structured(VerificationSummary, SYSTEM, user, fallback)
    if not isinstance(result, VerificationSummary):
        result = fallback
    result.disclaimer = DISCLAIMER
    if DISCLAIMER.lower() not in (result.summary or "").lower():
        result.summary = f"{result.summary} {DISCLAIMER}".strip()
    result.document_status = fallback.document_status
    result.eligibility_assessment = fallback.eligibility_assessment
    result.risk_level = fallback.risk_level
    return result
