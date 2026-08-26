"""Application configuration loaded from environment variables."""
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    # Base URL of the Java API gateway that fronts all business services.
    gateway_url: str = "http://localhost:8080"

    # A service token used to call protected business endpoints on behalf of a user.
    # In production this would be a short-lived JWT minted for the assistant.
    service_token: str = ""

    # OpenAI configuration. When openai_api_key is empty the assistant falls back
    # to a deterministic rule-based planner so it still runs offline.
    openai_api_key: str = ""
    openai_model: str = "gpt-4o-mini"
    openai_base_url: str = ""  # optional override for Azure/OpenAI-compatible endpoints

    # RAG knowledge base file (markdown).
    knowledge_path: str = "data/knowledge.md"

    request_timeout_seconds: float = 10.0

    @property
    def llm_enabled(self) -> bool:
        return bool(self.openai_api_key)


settings = Settings()
