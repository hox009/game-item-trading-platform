"""Thin HTTP client for the Java API gateway.

Every business capability the assistant needs (search catalog, look up an item,
check an order) is a REST call through the gateway. This keeps the assistant a
pure orchestration layer with no direct database access.
"""
from __future__ import annotations

from typing import Any

import httpx

from app.config import settings


class GatewayClient:
    def __init__(self, base_url: str | None = None, token: str | None = None) -> None:
        self._base_url = (base_url or settings.gateway_url).rstrip("/")
        self._token = token if token is not None else settings.service_token

    def _headers(self) -> dict[str, str]:
        headers = {"Content-Type": "application/json"}
        if self._token:
            headers["Authorization"] = f"Bearer {self._token}"
        return headers

    def _get(self, path: str, params: dict[str, Any] | None = None) -> Any:
        with httpx.Client(timeout=settings.request_timeout_seconds) as client:
            resp = client.get(f"{self._base_url}{path}", params=params, headers=self._headers())
            resp.raise_for_status()
            return _unwrap(resp.json())

    def _post(self, path: str, body: dict[str, Any]) -> Any:
        with httpx.Client(timeout=settings.request_timeout_seconds) as client:
            resp = client.post(f"{self._base_url}{path}", json=body, headers=self._headers())
            resp.raise_for_status()
            return _unwrap(resp.json())

    # --- Business capabilities exposed to the agent as tools ---

    def search_items(self, keyword: str | None = None, game: str | None = None,
                     max_price: float | None = None, size: int = 10) -> Any:
        params: dict[str, Any] = {"size": size}
        if keyword:
            params["keyword"] = keyword
        if game:
            params["game"] = game
        if max_price is not None:
            params["maxPrice"] = max_price
        return self._get("/api/items", params=params)

    def get_item(self, item_id: int) -> Any:
        return self._get(f"/api/items/{item_id}")

    def get_order(self, order_id: int) -> Any:
        return self._get(f"/api/orders/{order_id}")


def _unwrap(payload: dict[str, Any]) -> Any:
    """Unwrap the platform's ApiResponse envelope, raising on business errors."""
    if not isinstance(payload, dict):
        return payload
    code = payload.get("code", 0)
    if code != 0:
        raise RuntimeError(payload.get("message", "gateway error"))
    return payload.get("data")
