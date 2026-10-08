#!/usr/bin/env bash
# aislam_test semasini aislam_prod'a kopyalar (veri yok).
set -euo pipefail
cd /opt/aislam/deploy
COMPOSE="docker compose -f docker-compose.prod.yml"
USER=aislam_dev

$COMPOSE exec -T postgres psql -U "$USER" -d aislam_prod -v ON_ERROR_STOP=1 -c \
  "DROP SCHEMA public CASCADE; CREATE SCHEMA public AUTHORIZATION ${USER};"

$COMPOSE exec -T postgres pg_dump -U "$USER" --schema-only --no-owner --no-acl aislam_test \
  | $COMPOSE exec -T postgres psql -U "$USER" -d aislam_prod -v ON_ERROR_STOP=1

echo "==> aislam_prod tablolari:"
$COMPOSE exec -T postgres psql -U "$USER" -d aislam_prod -c '\dt'
