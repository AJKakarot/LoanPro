from typing import Any, Literal

from pydantic import BaseModel, ConfigDict, Field
from pydantic.alias_generators import to_camel

DISCLAIMER = "AI-generated analysis. Final decision must be made by an authorized human reviewer."


class CamelModel(BaseModel):
    model_config = ConfigDict(
        alias_generator=to_camel,
        populate_by_name=True,
        extra="ignore",
        serialize_by_alias=True,
    )


class AnalyzeRequest(CamelModel):
    application: dict[str, Any] = Field(default_factory=dict)
    documents: list[dict[str, Any]] = Field(default_factory=list)


class DocumentAnalysis(CamelModel):
    status: Literal["PASS", "REVIEW_REQUIRED"]
    missing_documents: list[str] = Field(default_factory=list)
    mismatches: list[str] = Field(default_factory=list)
    warnings: list[str] = Field(default_factory=list)
    summary: str = ""


class EligibilityAnalysis(CamelModel):
    assessment: Literal["LIKELY_ELIGIBLE", "REVIEW_REQUIRED"]
    factors: list[str] = Field(default_factory=list)
    warnings: list[str] = Field(default_factory=list)
    summary: str = ""
    estimated_emi: float | None = None
    debt_to_income_ratio: float | None = None
    disposable_income: float | None = None


class RiskAnalysis(CamelModel):
    risk_level: Literal["LOW", "MEDIUM", "HIGH"]
    risk_factors: list[str] = Field(default_factory=list)
    warnings: list[str] = Field(default_factory=list)
    manual_checks: list[str] = Field(default_factory=list)


class VerificationSummary(CamelModel):
    document_status: str
    eligibility_assessment: str
    risk_level: str
    key_issues: list[str] = Field(default_factory=list)
    recommended_manual_checks: list[str] = Field(default_factory=list)
    summary: str = ""
    disclaimer: str = DISCLAIMER


class AnalyzeResponse(CamelModel):
    document_analysis: DocumentAnalysis
    eligibility_analysis: EligibilityAnalysis
    risk_analysis: RiskAnalysis
    verification_summary: VerificationSummary
