#!/usr/bin/env bash
#
# e-islam.net statik sitesini gunceller.
#
#   sudo /opt/aislam/deploy/k8s/deploy-web.sh
#
# deploy/web/ altindaki dosyalari ConfigMap'e yeniden yazar ve nginx pod'unu
# yeniden baslatir. ConfigMap degistiginde Kubernetes pod'u kendiliginden
# yenilemedigi icin restart adimi gerekli.

set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
WEB_DIR="$REPO_DIR/deploy/web"
NAMESPACE="eislam"

if [ "$(id -u)" -ne 0 ]; then
  echo "Bu betik root gerektirir: sudo $0" >&2
  exit 1
fi

# README.md siteye ait degil, disarida birakilir.
FILES=(index.html gizlilik.html kosullar.html styles.css app-ads.txt)

args=()
for f in "${FILES[@]}"; do
  path="$WEB_DIR/$f"
  if [ ! -f "$path" ]; then
    echo "Eksik dosya: $path" >&2
    exit 1
  fi
  args+=(--from-file="$path")
done

echo "==> 1/3 Icerik ConfigMap'e yaziliyor (${#FILES[@]} dosya)"
k3s kubectl -n "$NAMESPACE" create configmap web-content "${args[@]}" \
  --dry-run=client -o yaml | k3s kubectl apply -f -

echo "==> 2/3 Pod yeniden baslatiliyor"
k3s kubectl -n "$NAMESPACE" rollout restart deploy/web
k3s kubectl -n "$NAMESPACE" rollout status deploy/web --timeout=120s

echo "==> 3/3 Dogrulama"
for url in https://e-islam.net/app-ads.txt https://e-islam.net/gizlilik; do
  code=$(curl -s -o /dev/null -w '%{http_code}' --max-time 15 "$url" || echo 000)
  echo "  $url -> HTTP $code"
  [ "$code" = "200" ] || { echo "Beklenen 200 alinamadi." >&2; exit 1; }
done

echo "Tamam."
