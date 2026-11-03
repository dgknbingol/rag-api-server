#!/usr/bin/env python3
import json
import urllib.error
import urllib.request
from pathlib import Path

env_path = Path(__file__).resolve().parents[2] / ".env"
auth = None
for line in env_path.read_text(encoding="utf-8").splitlines():
    if line.startswith("REVENUECAT_WEBHOOK_AUTH="):
        auth = line.split("=", 1)[1].strip().strip('"').strip("'")
        break
if not auth:
    raise SystemExit("REVENUECAT_WEBHOOK_AUTH missing in rag-api-server/.env")

body = json.dumps(
    {
        "api_version": "1.0",
        "event": {
            "type": "TEST",
            "app_user_id": "00000000-0000-0000-0000-000000000000",
            "entitlement_ids": ["premium"],
        },
    }
).encode()

req = urllib.request.Request(
    "https://api.e-islam.net/api/webhooks/revenuecat",
    data=body,
    headers={
        "Content-Type": "application/json",
        "Authorization": auth,
    },
    method="POST",
)
print(f"auth_len={len(auth)}")
try:
    with urllib.request.urlopen(req, timeout=30) as resp:
        print(f"HTTP {resp.status}")
        print(resp.read()[:200])
except urllib.error.HTTPError as e:
    print(f"HTTP {e.code}")
    print(e.read()[:300])
