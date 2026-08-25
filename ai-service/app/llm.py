import json
import logging

from langchain_groq import ChatGroq
from pydantic import BaseModel, ValidationError

from app.config import get_settings

logger = logging.getLogger(__name__)


def get_llm() -> ChatGroq | None:
    settings = get_settings()
    if not settings.groq_api_key:
        logger.warning("GROQ_API_KEY is not set; using deterministic analysis only")
        return None
    return ChatGroq(
        api_key=settings.groq_api_key,
        model=settings.groq_model,
        temperature=0,
        timeout=settings.groq_timeout_seconds,
        max_retries=1,
    )


def invoke_structured(schema: type[BaseModel], system: str, user: str, fallback: BaseModel) -> BaseModel:
    llm = get_llm()
    if llm is None:
        return fallback
    settings = get_settings()
    structured = llm.with_structured_output(schema)
    last_error: Exception | None = None
    for attempt in range(settings.groq_max_retries + 1):
        try:
            result = structured.invoke([
                ("system", system),
                ("human", user),
            ])
            if isinstance(result, schema):
                return result
            return schema.model_validate(result)
        except (ValidationError, ValueError, TypeError, Exception) as exc:
            last_error = exc
            logger.warning("Structured LLM output failed (attempt %s): %s", attempt + 1, exc)
    logger.error("LLM structured output exhausted retries: %s", last_error)
    return fallback


def payload_json(data: object) -> str:
    return json.dumps(data, default=str, indent=2)
