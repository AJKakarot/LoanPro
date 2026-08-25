from app.llm import invoke_structured, payload_json
from app.schemas.analysis import RiskAnalysis

SYSTEM = """You are a credit risk analyst for a maker-checker loan desk.
You never approve, reject, or auto-decline a loan.
Flag risk indicators and recommend manual checks only.
Use LOW, MEDIUM, or HIGH. HIGH means extra scrutiny, not rejection."""


def _heuristic(application: dict, documents: list[dict], document_result: dict, eligibility_result: dict, metrics: dict) -> RiskAnalysis:
    factors: list[str] = []
    warnings: list[str] = []
    checks: list[str] = ["Verify original identity and income documents in person or via trusted sources."]
    dti = float(metrics.get("debtToIncomeRatio") or eligibility_result.get("debtToIncomeRatio") or 0)
    related = int(application.get("relatedApplicationCount") or 0)
    missing = document_result.get("missingDocuments") or []
    mismatches = document_result.get("mismatches") or []

    if mismatches:
        factors.append("Document or identity mismatches were reported.")
        checks.append("Reconcile applicant name, ID and income documents.")
    if missing:
        factors.append("Required documents are missing.")
        checks.append("Collect missing documents before a credit decision.")
    if dti > 0.50:
        factors.append("High existing plus proposed EMI burden.")
        checks.append("Recompute affordability with latest salary credits.")
    elif dti > 0.40:
        factors.append("Elevated EMI burden.")
    if related > 1:
        factors.append("Multiple applications are associated with this customer.")
        warnings.append("Review related files for duplicate borrowing.")
        checks.append("Check other open applications for the same customer.")
    if str(application.get("employmentType") or "") == "UNEMPLOYED":
        factors.append("Employment type is unemployed.")
        checks.append("Confirm current income source.")
    income = float(application.get("monthlyIncome") or 0)
    other = float(application.get("otherIncome") or 0)
    if other > income > 0:
        warnings.append("Other income exceeds salary. Confirm source documents.")
        checks.append("Verify other-income proofs.")

    if any("HIGH" in factor.upper() for factor in factors) or dti > 0.50 or mismatches:
        level = "HIGH"
    elif factors or warnings or missing:
        level = "MEDIUM"
    else:
        level = "LOW"
        factors.append("No material automated risk flags from available metadata.")

    return RiskAnalysis(
        risk_level=level,
        risk_factors=factors,
        warnings=warnings,
        manual_checks=checks,
    )


def run_risk_agent(
    application: dict,
    documents: list[dict],
    document_result: dict,
    eligibility_result: dict,
    metrics: dict,
) -> RiskAnalysis:
    fallback = _heuristic(application, documents, document_result, eligibility_result, metrics)
    user = (
        "Assess operational and credit risk. Never reject the loan.\n\n"
        f"Application:\n{payload_json(application)}\n\n"
        f"Documents:\n{payload_json(documents)}\n\n"
        f"Document analysis:\n{payload_json(document_result)}\n\n"
        f"Eligibility analysis:\n{payload_json(eligibility_result)}\n\n"
        f"Metrics:\n{payload_json(metrics)}"
    )
    result = invoke_structured(RiskAnalysis, SYSTEM, user, fallback)
    if not isinstance(result, RiskAnalysis):
        return fallback
    if fallback.risk_level == "HIGH" and result.risk_level == "LOW":
        result.risk_level = "HIGH"
        result.risk_factors = list(dict.fromkeys(result.risk_factors + fallback.risk_factors))
    result.manual_checks = list(dict.fromkeys(result.manual_checks + fallback.manual_checks))
    return result
