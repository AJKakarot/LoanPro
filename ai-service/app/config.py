from functools import lru_cache
from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict

_SERVICE_DIR = Path(__file__).resolve().parent.parent
_ROOT_DIR = _SERVICE_DIR.parent


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=(_SERVICE_DIR / ".env", _ROOT_DIR / ".env"),
        extra="ignore",
    )

    groq_api_key: str = ""
    groq_model: str = "openai/gpt-oss-120b"
    groq_timeout_seconds: int = 30
    groq_max_retries: int = 1


@lru_cache
def get_settings() -> Settings:
    return Settings()
