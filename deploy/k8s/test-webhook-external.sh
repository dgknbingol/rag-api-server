#!/usr/bin/env bash
set -euo pipefail
AUTH=$(sudo k3s kubectl -n dogukan-test exec deploy/rag-api -- printenv REVENUECAT_WEBHOOK_AUTH)
echo "auth_len=${#AUTH}"
echo "=== external https ==="
curl -sS -w "\nHTTP:%{http_code}\n" -X POST "https://api.e-islam.net/api/webhooks/revenuecat" \
  -H "Content-Type: application/json" \
  -H "Authorization: ${AUTH}" \
  --data-binary @/tmp/rc-wh.json
echo
echo "=== external with X-RevenueCat-Authorization only (expect 401 until code supports it) ==="
curl -sS -w "\nHTTP:%{http_code}\n" -X POST "https://api.e-islam.net/api/webhooks/revenuecat" \
  -H "Content-Type: application/json" \
  -H "X-RevenueCat-Authorization: ${AUTH}" \
  --data-binary @/tmp/rc-wh.json
