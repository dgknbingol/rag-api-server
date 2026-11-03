#!/usr/bin/env python3
import json
import urllib.error
import urllib.request
from pathlib import Path

auth = None
for line in Path(__file__).resolve().parents[2].joinpath(".env").read_text(encoding="utf-8").splitlines():
    if line.startswith("REVENUECAT_WEBHOOK_AUTH="):
        auth = line.split("=", 1)[1].strip().strip('"').strip("'")
        break
if not auth:
    raise SystemExit("missing auth")

token = auth[7:].strip() if auth.lower().startswith("bearer ") else auth
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

cases = {
    "full": auth,
    "token_only": token,
    "double_bearer": f"Bearer {auth}",
    "bearer_nospace": f"Bearer{token}",
    "none": None,
}

for label, value in cases.items():
    headers = {"Content-Type": "application/json"}
    if value is not None:
        headers["Authorization"] = value
    req = urllib.request.Request(
        "https://api.e-islam.net/api/webhooks/revenuecat",
        data=body,
        headers=headers,
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=20) as resp:
            print(f"{label}: {resp.status}")
    except urllib.error.HTTPError as exc:
        print(f"{label}: {exc.code} {exc.read()[:120]!r}")
