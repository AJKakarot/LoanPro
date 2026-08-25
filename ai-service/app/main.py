import logging

from fastapi import FastAPI, HTTPException
from fastapi.responses import JSONResponse

from app.graph import analyze_loan
from app.schemas.analysis import AnalyzeRequest, AnalyzeResponse

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="LoanPro AI Analysis Service", version="1.0.0")


@app.get("/health")
def health():
    return {"status": "UP"}


@app.post("/api/ai/analyze-loan", response_model=AnalyzeResponse)
def analyze(request: AnalyzeRequest) -> AnalyzeResponse:
    try:
        return analyze_loan(request.application, request.documents)
    except Exception:
        logger.exception("Loan analysis graph failed")
        raise HTTPException(status_code=503, detail="AI analysis is temporarily unavailable") from None


@app.exception_handler(HTTPException)
def http_error(_, exc: HTTPException):
    return JSONResponse(status_code=exc.status_code, content={"message": exc.detail, "available": False})
