#!/usr/bin/env python3
"""
Run Schemathesis API tests against a schema.

This script forwards unknown command-line arguments directly to the Schemathesis
CLI.  Common flags such as ``--include-method``, ``--include-operation-id``, and
``--include-path`` are therefore supported and will be passed unchanged.
"""

from __future__ import annotations

import argparse
import os
from pathlib import Path
import subprocess

from keycloak_auth import fetch_token

_SCHEMATHESIS_VERSION=os.environ.get("SCHEMATHESIS_VERSION", "4.17.0")

def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("schema_location")
    parser.add_argument("--base-url")
    args, schemathesis_args = parser.parse_known_args()
    args.schemathesis_args = schemathesis_args
    return args


def main() -> int:
    args = parse_args()

    env = os.environ.copy()
    bearer_token = env.get("BEARER_TOKEN")
    if not bearer_token and {"KEYCLOAK_ENDPOINT", "CLIENT_ID", "CLIENT_SECRET"} <= env.keys():
        bearer_token = fetch_token()

    Path("output/junit").mkdir(parents=True, exist_ok=True)
    Path("output/allure/results").mkdir(parents=True, exist_ok=True)

    command = [
        "uvx",
        "--from",
        f"schemathesis[allure]=={_SCHEMATHESIS_VERSION}",
        "st",
        "--config-file",
        "schemathesis.toml",
        "run",
    ]

    if args.base_url:
        command.extend(["--url", args.base_url])
    if bearer_token:
        command.extend(["--header", f"Authorization: Bearer {bearer_token}"])
    command.extend([args.schema_location, *args.schemathesis_args])

    result = subprocess.run(command, env=env, check=False)
    return result.returncode


if __name__ == "__main__":
    raise SystemExit(main())
