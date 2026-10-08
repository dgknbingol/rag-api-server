#!/usr/bin/env python3
"""Update FIREBASE_CREDENTIALS_JSON in rag-api-secret from firebase-sa.json."""
import base64
import json
import subprocess
import sys

PATH = "/opt/aislam/deploy/firebase-sa.json"
EXPECTED_PROJECT = "e-islam-41f1a"

with open(PATH, "rb") as f:
    raw = f.read()

meta = json.loads(raw)
project_id = meta.get("project_id")
print(f"project_id={project_id}", file=sys.stderr)
print(f"client_email={meta.get('client_email')}", file=sys.stderr)
if project_id != EXPECTED_PROJECT:
    sys.exit(f"YANLIS PROJE: {project_id} (beklenen {EXPECTED_PROJECT})")

sec = json.loads(
    subprocess.check_output(
        ["k3s", "kubectl", "-n", "eislam", "get", "secret", "rag-api-secret", "-o", "json"]
    )
)
data = sec.get("data") or {}
data["FIREBASE_CREDENTIALS_JSON"] = base64.b64encode(raw).decode()

payload = {
    "apiVersion": "v1",
    "kind": "Secret",
    "metadata": {"name": "rag-api-secret", "namespace": "eislam"},
    "type": "Opaque",
    "data": data,
}
print(json.dumps(payload))
print(f"FIREBASE_CREDENTIALS_JSON length={len(raw)}", file=sys.stderr)
