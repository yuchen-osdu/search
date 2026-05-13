#!/usr/bin/env python3

from __future__ import annotations

import json
import os
import sys
import urllib.parse
import urllib.request
import urllib.error
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

def env(name: str) -> str:
    """Get an environment variable or exit if not set."""
    value = os.environ.get(name)
    if not value:
        logger.error("Missing required environment variable: %s", name)
        raise SystemExit(1)
    return value

def fetch_token() -> str:
    """
    Fetch an access token from Keycloak using client credentials.
    Returns:
        str: The access token.
    Raises:
        SystemExit: If the token cannot be fetched.
    """
    endpoint = env("KEYCLOAK_ENDPOINT")
    client_id = env("CLIENT_ID")
    client_secret = env("CLIENT_SECRET")
    scope = os.environ.get("KEYCLOAK_SCOPE", "openid")

    payload = urllib.parse.urlencode(
        {
            "grant_type": "client_credentials",
            "client_id": client_id,
            "client_secret": client_secret,
            "scope": scope,
        }
    ).encode("utf-8")

    request = urllib.request.Request(
        endpoint,
        data=payload,
        headers={"Content-Type": "application/x-www-form-urlencoded"},
        method="POST",
    )

    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            if response.status != 200:
                logger.error("Keycloak returned non-200 status: %d", response.status)
                raise SystemExit(1)
            body = json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        logger.error("HTTP error: %s", e)
        raise SystemExit(1)
    except urllib.error.URLError as e:
        logger.error("URL error: %s", e)
        raise SystemExit(1)
    except Exception:
        logger.exception("Unexpected error while fetching Keycloak token")
        raise SystemExit(1)

    token = body.get("access_token")
    if not token:
        logger.error("Keycloak response does not contain access_token")
        raise SystemExit(1)
    return token

def main() -> int:
    sys.stdout.write(fetch_token())
    return 0

if __name__ == "__main__":
    raise SystemExit(main())