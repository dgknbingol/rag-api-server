#!/usr/bin/env bash
#
# Tek Postgres icinde aislam_test + aislam_prod olusturur.
# Mevcut aislam verisini aislam_test'e kopyalar (canli dogukan-test icin).
# aislam_prod bos baslar.
#
#   cd /opt/aislam/deploy && sudo bash sql/split-test-prod-databases.sh
#
set -euo pipefail

DEPLOY_DIR="$(cd "$(dirname "$0")/.." && pwd)"
COMPOSE_FILE="$DEPLOY_DIR/docker-compose.prod.yml"

if [ "$(id -u)" -ne 0 ]; then
  echo "root gerekir: sudo $0" >&2
  exit 1
fi

cd "$DEPLOY_DIR"

POSTGRES_USER=$(grep '^POSTGRES_USER=' .env | cut -d= -f2- | tr -d '"' | tr -d "'")
POSTGRES_USER=${POSTGRES_USER:-aislam_dev}

psql_exec() {
  docker compose -f "$COMPOSE_FILE" exec -T postgres \
    psql -U "$POSTGRES_USER" "$@"
}

db_exists() {
  local name="$1"
  psql_exec -d postgres -tc "SELECT 1 FROM pg_database WHERE datname='${name}'" | grep -q 1
}

echo "==> Mevcut DB'ler"
psql_exec -d postgres -c '\l'

if db_exists aislam_test; then
  echo "aislam_test zaten var"
else
  echo "==> CREATE DATABASE aislam_test"
  psql_exec -d postgres -c "CREATE DATABASE aislam_test OWNER ${POSTGRES_USER};"
fi

if db_exists aislam_prod; then
  echo "aislam_prod zaten var"
else
  echo "==> CREATE DATABASE aislam_prod"
  psql_exec -d postgres -c "CREATE DATABASE aislam_prod OWNER ${POSTGRES_USER};"
fi

if ! db_exists aislam; then
  echo "Kaynak DB 'aislam' yok — kopyalama atlandi" >&2
  exit 1
fi

echo "==> aislam -> aislam_test (pg_dump | psql)"
psql_exec -d aislam_test -v ON_ERROR_STOP=1 -c \
  "DROP SCHEMA public CASCADE; CREATE SCHEMA public AUTHORIZATION ${POSTGRES_USER};"

docker compose -f "$COMPOSE_FILE" exec -T postgres \
  pg_dump -U "$POSTGRES_USER" --no-owner --no-acl aislam \
  | docker compose -f "$COMPOSE_FILE" exec -T postgres \
      psql -U "$POSTGRES_USER" -d aislam_test -v ON_ERROR_STOP=1

echo "==> Ozet"
psql_exec -d postgres -c '\l'
echo "Eski 'aislam' yedek olarak duruyor."
