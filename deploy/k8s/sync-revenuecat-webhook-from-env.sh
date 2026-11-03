#!/usr/bin/env bash
# Sunucuda çalıştır: /opt/aislam/deploy/.env içindeki REVENUECAT_WEBHOOK_AUTH'u
# k8s secret'a yazar ve rag-api'yi restart eder.
# Secret'ı ekrana basmaz.
set -euo pipefail

ENV_FILE="${1:-/opt/aislam/deploy/.env}"
NS="${NS:-dogukan-test}"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "ENV bulunamadi: $ENV_FILE" >&2
  exit 1
fi

python3 - <<PY | sudo k3s kubectl apply -f -
import base64, json, sys

vals = {}
for line in open("$ENV_FILE", encoding="utf-8"):
    s = line.strip()
    if not s or s.startswith("#") or "=" not in s:
        continue
    k, v = s.split("=", 1)
    if len(v) >= 2 and v[0] == v[-1] and v[0] in ("\"", "'"):
        v = v[1:-1]
    vals[k.strip()] = v

auth = vals.get("REVENUECAT_WEBHOOK_AUTH", "")
print(f"REVENUECAT_WEBHOOK_AUTH len={len(auth)} has_space={'yes' if ' ' in auth else 'no'}", file=sys.stderr)
if not auth:
    sys.exit("REVENUECAT_WEBHOOK_AUTH bos")

# Mevcut secret'i al, sadece webhook alanini guncelle
import subprocess
raw = subprocess.check_output(
    ["sudo", "k3s", "kubectl", "-n", "$NS", "get", "secret", "rag-api-secret", "-o", "json"],
    text=True,
)
sec = json.loads(raw)
data = sec.get("data") or {}
data["REVENUECAT_WEBHOOK_AUTH"] = base64.b64encode(auth.encode()).decode()
print(json.dumps({
    "apiVersion": "v1",
    "kind": "Secret",
    "metadata": {"name": "rag-api-secret", "namespace": "$NS"},
    "type": "Opaque",
    "data": data,
}))
PY

sudo k3s kubectl -n "$NS" rollout restart deploy/rag-api
sudo k3s kubectl -n "$NS" rollout status deploy/rag-api --timeout=300s
echo "OK: webhook secret sync + rollout"
