#!/usr/bin/env python3
"""Patch rag-api-secret from deploy .env (merge; preserve FIREBASE/APNS etc.)."""
from __future__ import annotations

import base64
import json
import subprocess
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

# Mevcut secret ile birleştir — FIREBASE_CREDENTIALS_JSON / APNS_* silinmesin.
try:
    existing = json.loads(
        subprocess.check_output(
            ["k3s", "kubectl", "-n", NS, "get", "secret", "rag-api-secret", "-o", "json"],
            stderr=subprocess.DEVNULL,
        )
    )
    data: dict[str, str] = dict(existing.get("data") or {})
except Exception:
    data = {}

for target, source in mapping.items():
    v = vals.get(source, "")
    print(f"{target:28} len={len(v)} space={'yes' if ' ' in v else 'no'}", file=sys.stderr)
    data[target] = base64.b64encode(v.encode()).decode()

print(
    f"preserved_keys={sorted(k for k in data if k not in mapping)}",
    file=sys.stderr,
)

print(
    json.dumps(
        {
            "apiVersion": "v1",
            "kind": "Secret",
            "metadata": {"name": "rag-api-secret", "namespace": NS},
            "type": "Opaque",
            "data": data,
        }
    )
)
