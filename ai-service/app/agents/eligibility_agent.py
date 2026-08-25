from app.agents.metrics import financial_metrics
from app.llm import invoke_structured, payload_json
from app.schemas.analysis import EligibilityAnalysis

SYSTEM = """You are a credit eligibility analyst assisting makers and checkers.
You never approve or reject a loan. Arithmetic is already computed; do not recalculate it.
Interpret the metrics and employment context. If any key metric is weak, use REVIEW_REQUIRED.
LIKELY_ELIGIBLE means the file appears within policy ranges, not that it should be approved."""


def _heuristic(metrics: dict) -> EligibilityAnalysis:
    dti = float(metrics.get("debtToIncomeRatio") or 0)
    income = float(metrics.get("totalMonthlyIncome") or 0)
    disposable = float(metrics.get("disposableIncome") or 0)
    employment = str(metrics.get("employmentType") or "")
    factors: list[str] = [
        f"Estimated EMI is {metrics.get('estimatedEmi')}.",
        f"Debt-to-income ratio is {dti:.2%}.",
        f"Disposable income after EMI is {disposable:.2f}.",
    ]
    warnings: list[str] = []
    if income <= 0:
        warnings.append("No usable monthly income was provided.")
    if dti > 0.50:
        warnings.append("Proposed DTI exceeds the 50% policy threshold.")
    elif dti > 0.40:
        warnings.append("DTI is elevated and needs manual affordability review.")
    if disposable < 0:
        warnings.append("Cash flow after existing obligations and the new EMI is negative.")
    if employment in {"UNEMPLOYED", ""}:
        warnings.append("Employment type is missing or unemployed.")
    assessment = "LIKELY_ELIGIBLE" if income > 0 and dti <= 0.50 and disposable >= 0 else "REVIEW_REQUIRED"
    summary = (
        "Financial ratios sit within typical policy bands. This is not an approval."
        if assessment == "LIKELY_ELIGIBLE"
        else "Affordability metrics are outside comfortable policy bands and need human review."
    )
    return EligibilityAnalysis(
        assessment=assessment,
        factors=factors,
        warnings=warnings,
        summary=summary,
        estimated_emi=metrics.get("estimatedEmi"),
        debt_to_income_ratio=metrics.get("debtToIncomeRatio"),
        disposable_income=metrics.get("disposableIncome"),
    )


def run_eligibility_agent(application: dict) -> tuple[EligibilityAnalysis, dict]:
    metrics = financial_metrics(application)
    fallback = _heuristic(metrics)
    user = (
        "Interpret these pre-computed metrics. Do not redo the arithmetic. "
        "Never approve or reject the loan.\n\n"
        f"Application:\n{payload_json(application)}\n\n"
        f"Metrics:\n{payload_json(metrics)}"
    )
    result = invoke_structured(EligibilityAnalysis, SYSTEM, user, fallback)
    if not isinstance(result, EligibilityAnalysis):
        result = fallback
    result.estimated_emi = metrics.get("estimatedEmi")
    result.debt_to_income_ratio = metrics.get("debtToIncomeRatio")
    result.disposable_income = metrics.get("disposableIncome")
    if fallback.assessment == "REVIEW_REQUIRED":
        result.assessment = "REVIEW_REQUIRED"
        result.warnings = list(dict.fromkeys(result.warnings + fallback.warnings))
    return result, metrics
