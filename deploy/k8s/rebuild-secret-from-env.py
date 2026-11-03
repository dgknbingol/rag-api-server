#!/usr/bin/env python3
"""Rebuild rag-api-secret from deploy .env. Prints key lengths only (not values)."""
from __future__ import annotations

import base64
import json
import sys

ENV_FILE = sys.argv[1] if len(sys.argv) > 1 else "/opt/aislam/deploy/.env"
NS = sys.argv[2] if len(sys.argv) > 2 else "dogukan-test"

vals: dict[str, str] = {}
with open(ENV_FILE, encoding="utf-8") as f:
    for line in f:
        s = line.strip()
        if not s or s.startswith("#") or "=" not in s:
            continue
        k, v = s.split("=", 1)
        if len(v) >= 2 and v[0] == v[-1] and v[0] in ("\"", "'"):
            v = v[1:-1]
        vals[k.strip()] = v

mapping = {
    "SPRING_DATASOURCE_USERNAME": "POSTGRES_USER",
    "SPRING_DATASOURCE_PASSWORD": "POSTGRES_PASSWORD",
    "DEEPSEEK_API_KEY": "DEEPSEEK_API_KEY",
    "APP_AUTH_JWT_SECRET": "AISLAM_JWT_SECRET",
    "REVENUECAT_WEBHOOK_AUTH": "REVENUECAT_WEBHOOK_AUTH",
    "PUSH_ADMIN_AUTH": "PUSH_ADMIN_AUTH",
    "GOOGLE_OAUTH_CLIENT_IDS": "GOOGLE_OAUTH_CLIENT_IDS",
    "APPLE_OAUTH_AUDIENCE": "APPLE_OAUTH_AUDIENCE",
}

for target, source in mapping.items():
    v = vals.get(source, "")
    print(f"{target:28} len={len(v)} space={'yes' if ' ' in v else 'no'}", file=sys.stderr)

print(
    json.dumps(
        {
            "apiVersion": "v1",
            "kind": "Secret",
            "metadata": {"name": "rag-api-secret", "namespace": NS},
            "type": "Opaque",
            "data": {
                t: base64.b64encode(vals.get(s, "").encode()).decode()
                for t, s in mapping.items()
            },
        }
    )
)
