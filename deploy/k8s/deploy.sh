#!/usr/bin/env bash
#
# Backend'i yeniden derleyip k3s'teki dagitima uygular.
#
#   sudo /opt/aislam/deploy/k8s/deploy.sh
#
# Kod degistiginde calistirilacak tek komut budur. Imaj etiketi sabit
# (aislam-rag-api:prod) oldugu icin Kubernetes yeni icerigi kendiliginden
# fark etmez; bu yuzden aktarimdan sonra rollout restart gerekir.

set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE_FILE="$REPO_DIR/deploy/docker-compose.prod.yml"
IMAGE="aislam-rag-api:prod"
NAMESPACE="eislam"
HEALTH_URL="https://api.e-islam.net/api/health"

if [ "$(id -u)" -ne 0 ]; then
  echo "Bu betik root gerektirir: sudo $0" >&2
  exit 1
fi

echo "==> 1/4 Imaj derleniyor"
docker compose -f "$COMPOSE_FILE" --profile docker-only build api

echo "==> 2/4 Imaj k3s'e aktariliyor (containerd Docker deposunu gormez)"
docker save "$IMAGE" | k3s ctr images import -

echo "==> 3/4 Dagitim yenileniyor"
k3s kubectl -n "$NAMESPACE" rollout restart deploy/rag-api
k3s kubectl -n "$NAMESPACE" rollout status deploy/rag-api --timeout=300s

echo "==> 4/4 Saglik kontrolu"
if curl -fsS --max-time 15 "$HEALTH_URL"; then
  echo
  echo "Tamam."
else
  echo >&2
  echo "Saglik kontrolu basarisiz. Loglara bakin:" >&2
  echo "  sudo k3s kubectl -n $NAMESPACE logs deploy/rag-api --tail=80" >&2
  exit 1
fi
