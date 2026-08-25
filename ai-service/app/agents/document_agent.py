import re
from datetime import date

from app.llm import invoke_structured, payload_json
from app.schemas.analysis import DocumentAnalysis

SYSTEM = """You are a document verification analyst for a loan operations desk.
You advise makers and checkers. You never approve or reject a loan.
Use only the provided application fields and document metadata.
Return structured findings. If evidence is incomplete, use REVIEW_REQUIRED.
Do not invent extracted document text that was not provided."""


def _required_types(application: dict) -> list[str]:
    raw = application.get("requiredDocuments") or "IDENTITY,ADDRESS_PROOF,INCOME_PROOF"
    if isinstance(raw, list):
        return [str(item).strip().upper() for item in raw if str(item).strip()]
    return [part.strip().upper() for part in str(raw).split(",") if part.strip()]


def _heuristic(application: dict, documents: list[dict]) -> DocumentAnalysis:
    required = _required_types(application)
    uploaded = {str(doc.get("documentType") or "").upper() for doc in documents}
    missing = [item for item in required if item not in uploaded]
    mismatches: list[str] = []
    warnings: list[str] = []
    applicant = str(application.get("fullName") or "").strip()
    year = date.today().year

    for doc in documents:
        name = str(doc.get("originalFileName") or "")
        size = int(doc.get("fileSize") or 0)
        status = str(doc.get("verificationStatus") or "")
        if size and size < 8_000:
            warnings.append(f"{name or 'A file'} is unusually small and may be incomplete.")
        if status == "REJECTED":
            warnings.append(f"{name or 'A document'} was previously rejected by a maker.")
        years = [int(value) for value in re.findall(r"(20\d{2})", name)]
        if years and max(years) < year - 3:
            warnings.append(f"{name} looks dated and may be expired. Confirm issue dates.")
        if applicant and re.search(r"[A-Za-z]{3,}", name):
            tokens = {part.lower() for part in re.split(r"[^A-Za-z]+", applicant) if len(part) > 2}
            file_tokens = {part.lower() for part in re.split(r"[^A-Za-z]+", name) if len(part) > 2}
            if tokens and file_tokens and tokens.isdisjoint(file_tokens) and "identity" in str(doc.get("documentType") or "").lower():
                mismatches.append("Identity file name does not match the applicant name on the application.")

    income = float(application.get("monthlyIncome") or 0)
    if income <= 0:
        mismatches.append("Monthly income is missing or zero while income proof is expected.")
    if "INCOME_PROOF" in missing:
        mismatches.append("Income proof is missing, so declared income cannot be corroborated.")

    status = "REVIEW_REQUIRED" if missing or mismatches or warnings else "PASS"
    summary = (
        "Document set is complete and metadata is consistent. Maker should still open each file."
        if status == "PASS"
        else "Document metadata needs human review before the file can be treated as complete."
    )
    return DocumentAnalysis(
        status=status,
        missing_documents=missing,
        mismatches=mismatches,
        warnings=warnings,
        summary=summary,
    )


def run_document_agent(application: dict, documents: list[dict]) -> DocumentAnalysis:
    fallback = _heuristic(application, documents)
    user = (
        "Analyze this loan application document package. "
        "Never approve or reject the loan.\n\n"
        f"Application:\n{payload_json(application)}\n\n"
        f"Documents:\n{payload_json(documents)}\n\n"
        f"Deterministic checks already found: {payload_json(fallback.model_dump())}"
    )
    result = invoke_structured(DocumentAnalysis, SYSTEM, user, fallback)
    if not isinstance(result, DocumentAnalysis):
        return fallback
    if fallback.missing_documents and result.status == "PASS":
        result.status = "REVIEW_REQUIRED"
        result.missing_documents = list(dict.fromkeys(result.missing_documents + fallback.missing_documents))
    return result
