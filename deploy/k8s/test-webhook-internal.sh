#!/usr/bin/env bash
set -euo pipefail
NS=dogukan-test
SVC=$(sudo k3s kubectl -n "$NS" get svc rag-api -o jsonpath='{.spec.clusterIP}')
AUTH=$(sudo k3s kubectl -n "$NS" exec deploy/rag-api -- printenv REVENUECAT_WEBHOOK_AUTH)
echo "SVC=$SVC auth_len=${#AUTH}"
curl -sS -w "\nHTTP:%{http_code}\n" -X POST "http://${SVC}:8080/api/webhooks/revenuecat" \
  -H "Content-Type: application/json" \
  -H "Authorization: ${AUTH}" \
  --data-binary @/tmp/rc-wh.json
echo
# Also test wrong auth
curl -sS -w "\nHTTP:%{http_code}\n" -X POST "http://${SVC}:8080/api/webhooks/revenuecat" \
  -H "Content-Type: application/json" \
  -H "Authorization: wrong" \
  --data-binary @/tmp/rc-wh.json
