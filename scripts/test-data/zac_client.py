#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
# HTTP transport and Keycloak authentication for the local ZAC test-data scripts.
#
# Shared by create-load.py and create-zaak.py; contains no ZAC domain logic.

import base64
import json
import sys
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
from typing import Any

# ---------------------------------------------------------------------------
# Constants — sourced from src/itest/kotlin/nl/info/zac/itest/config/
# ---------------------------------------------------------------------------

KEYCLOAK_REALM = "zaakafhandelcomponent"
KEYCLOAK_CLIENT_ID = "zaakafhandelcomponent"
KEYCLOAK_CLIENT_SECRET = "keycloakZaakafhandelcomponentClientSecret"

# beheerder1newiam = BEHEERDER_ELK_ZAAKTYPE
CONFIG_USER = "beheerder1newiam"
CONFIG_PASSWORD = "beheerder1newiam"

# Use the same beheerder user for zaak creation: they have access to all zaaktypes
# (behandelaar1newiam is restricted to domein_test_1 only)
ZAAK_USER = "beheerder1newiam"
ZAAK_PASSWORD = "beheerder1newiam"

_TOKEN_REFRESH_MARGIN = 30  # seconds before expiry at which to proactively refresh


# ---------------------------------------------------------------------------
# HTTP helpers
# ---------------------------------------------------------------------------


def http_request(method: str, url: str, body: Any = None, headers: dict | None = None) -> tuple[int, str]:
    """Perform an HTTP request. Returns (status_code, response_body)."""
    if headers is None:
        headers = {}
    data = None
    if body is not None:
        if isinstance(body, bytes):
            data = body
        elif isinstance(body, dict) and headers.get("Content-Type") == "application/x-www-form-urlencoded":
            data = urllib.parse.urlencode(body).encode()
        else:
            data = json.dumps(body).encode()
            headers.setdefault("Content-Type", "application/json")
    request = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request) as response:
            return response.status, response.read().decode()
    except urllib.error.HTTPError as httpError:
        return httpError.code, httpError.read().decode()


def auth_headers(token: str) -> dict:
    return {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}


def build_multipart(fields: list) -> tuple:
    """Build a multipart/form-data body from a list of (name, value, filename, content_type) tuples.
    value may be str or bytes. Returns (body_bytes, content_type_header_value).
    """
    boundary = uuid.uuid4().hex
    parts = []
    for name, value, filename, content_type in fields:
        disposition = f'form-data; name="{name}"'
        if filename is not None:
            disposition += f'; filename="{filename}"'
        header_lines = f"Content-Disposition: {disposition}\r\n"
        if content_type is not None:
            header_lines += f"Content-Type: {content_type}\r\n"
        part_header = f"--{boundary}\r\n{header_lines}\r\n".encode("utf-8")
        if isinstance(value, str):
            value = value.encode("utf-8")
        parts.append(part_header + value + b"\r\n")
    body = b"".join(parts) + f"--{boundary}--\r\n".encode("utf-8")
    return body, f"multipart/form-data; boundary={boundary}"


# ---------------------------------------------------------------------------
# Keycloak authentication
# ---------------------------------------------------------------------------


def get_tokens(username: str, password: str, keycloak_url: str) -> tuple[str, str]:
    """Obtain Keycloak access + refresh tokens via Resource Owner Password flow."""
    url = f"{keycloak_url}/realms/{KEYCLOAK_REALM}/protocol/openid-connect/token"
    body = {
        "grant_type": "password",
        "client_id": KEYCLOAK_CLIENT_ID,
        "client_secret": KEYCLOAK_CLIENT_SECRET,
        "username": username,
        "password": password,
    }
    status, response = http_request(
        "POST", url, body=body, headers={"Content-Type": "application/x-www-form-urlencoded"}
    )
    if status != 200:
        print(f"ERROR: Keycloak auth failed for '{username}' (HTTP {status}): {response[:200]}")
        sys.exit(1)
    data = json.loads(response)
    return data["access_token"], data["refresh_token"]


def get_token(username: str, password: str, keycloak_url: str) -> str:
    """Obtain a Keycloak Bearer token via Resource Owner Password flow."""
    return get_tokens(username, password, keycloak_url)[0]


def jwt_expiry(token: str) -> float:
    """Return the exp claim from a JWT without verifying the signature."""
    payload = token.split(".")[1]
    payload += "=" * (-len(payload) % 4)
    return float(json.loads(base64.urlsafe_b64decode(payload))["exp"])


class TokenManager:
    """Thread-safe Keycloak token holder that auto-refreshes before expiry."""

    def __init__(self, username: str, password: str, keycloak_url: str) -> None:
        self._username = username
        self._password = password
        self._keycloak_url = keycloak_url
        self._lock = threading.Lock()
        self._access_token, self._refresh_token = get_tokens(username, password, keycloak_url)
        self._expiry = jwt_expiry(self._access_token)

    def get_token(self) -> str:
        """Return a valid access token, refreshing proactively when near expiry."""
        with self._lock:
            if time.time() >= self._expiry - _TOKEN_REFRESH_MARGIN:
                self._do_refresh()
            # Refresh token may itself have been expired, leaving us with a still-expired
            # access token. Fall back to full re-authentication in that case.
            if time.time() >= self._expiry:
                print("  [Token] Token still expired after refresh — re-authenticating...")
                self._access_token, self._refresh_token = get_tokens(
                    self._username, self._password, self._keycloak_url
                )
                self._expiry = jwt_expiry(self._access_token)
                print(f"  [Token] Re-authenticated (expires in {int(self._expiry - time.time())}s)")
            return self._access_token

    def _do_refresh(self) -> None:
        url = f"{self._keycloak_url}/realms/{KEYCLOAK_REALM}/protocol/openid-connect/token"
        body = {
            "grant_type": "refresh_token",
            "client_id": KEYCLOAK_CLIENT_ID,
            "client_secret": KEYCLOAK_CLIENT_SECRET,
            "refresh_token": self._refresh_token,
        }
        status, response = http_request(
            "POST", url, body=body, headers={"Content-Type": "application/x-www-form-urlencoded"}
        )
        if status == 200:
            data = json.loads(response)
            self._access_token = data["access_token"]
            self._refresh_token = data["refresh_token"]
            self._expiry = jwt_expiry(self._access_token)
            remaining = int(self._expiry - time.time())
            if remaining > 0:
                print(f"  [Token] Refreshed (expires in {remaining}s)")
            else:
                # Keycloak session has expired; the returned token is already stale.
                # get_token() will detect this and re-authenticate after we return.
                print(f"  [Token] Refresh returned expired token ({remaining}s), will re-authenticate")
        else:
            print(f"  [Token] Refresh failed (HTTP {status}), re-authenticating...")
            self._access_token, self._refresh_token = get_tokens(
                self._username, self._password, self._keycloak_url
            )
            self._expiry = jwt_expiry(self._access_token)
