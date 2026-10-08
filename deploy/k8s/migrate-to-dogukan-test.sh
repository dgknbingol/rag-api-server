#!/usr/bin/env bash
#
# eislam namespace -> dogukan-test tasima + dogukan-prod olusturma + eislam temizligi.
#   sudo /opt/aislam/deploy/k8s/migrate-to-dogukan-test.sh
#
set -euo pipefail

SRC=eislam
DST=dogukan-test
K8S_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [ "$(id -u)" -ne 0 ]; then
  echo "root gerekir: sudo $0" >&2
  exit 1
fi

copy_resource() {
  local kind="$1"
  local name="$2"
  if ! k3s kubectl -n "$SRC" get "$kind" "$name" >/dev/null 2>&1; then
    echo "  atlandi (yok): $kind/$name"
    return 0
  fi
  k3s kubectl -n "$SRC" get "$kind" "$name" -o json | python3 -c "
import json, sys
obj = json.load(sys.stdin)
md = obj.get('metadata') or {}
for k in ('resourceVersion', 'uid', 'creationTimestamp', 'generation',
          'managedFields', 'selfLink', 'ownerReferences'):
    md.pop(k, None)
ann = md.get('annotations') or {}
ann.pop('kubectl.kubernetes.io/last-applied-configuration', None)
if ann:
    md['annotations'] = ann
else:
    md.pop('annotations', None)
md['namespace'] = '$DST'
obj['metadata'] = md
obj.pop('status', None)
json.dump(obj, sys.stdout)
" | k3s kubectl apply -f -
  echo "  kopyalandi: $kind/$name"
}

echo "==> 1/6 Namespace'ler"
k3s kubectl apply -f "$K8S_DIR/namespace.yaml"

echo "==> 2/6 Secret / ConfigMap / TLS kopyala ($SRC -> $DST)"
copy_resource secret rag-api-secret
copy_resource secret api-eislam-tls
copy_resource secret web-eislam-tls
copy_resource configmap rag-api-config
copy_resource configmap web-content
copy_resource configmap web-nginx-conf

echo "==> 3/6 Deploy + Service (Ingress henuz yok — cift host olmasin)"
k3s kubectl apply -f "$K8S_DIR/configmap.yaml"
k3s kubectl apply -f "$K8S_DIR/web-nginx-conf.yaml"
k3s kubectl apply -f "$K8S_DIR/service.yaml"
k3s kubectl apply -f "$K8S_DIR/web-service.yaml"
k3s kubectl apply -f "$K8S_DIR/deployment.yaml"
k3s kubectl apply -f "$K8S_DIR/web-deployment.yaml"
k3s kubectl -n "$DST" rollout status deploy/rag-api --timeout=300s
k3s kubectl -n "$DST" rollout status deploy/web --timeout=180s

echo "==> 4/6 Eski Ingress kaldir, yenisini uygula (kisa kesinti)"
k3s kubectl -n "$SRC" delete ingress rag-api web --ignore-not-found
k3s kubectl apply -f "$K8S_DIR/ingress.yaml"
k3s kubectl apply -f "$K8S_DIR/web-ingress.yaml"

echo "==> 5/6 Eski eislam workload temizligi"
k3s kubectl -n "$SRC" delete deploy rag-api web --ignore-not-found
k3s kubectl -n "$SRC" delete svc rag-api web --ignore-not-found
k3s kubectl -n "$SRC" delete certificate api-eislam-tls web-eislam-tls --ignore-not-found
# Namespace'i silmek yerine bos birak (secret yedegi istersen kalsin); tamamen sil:
k3s kubectl delete namespace "$SRC" --wait=false || true

echo "==> 6/6 Health"
sleep 8
if curl -fsS --max-time 20 https://api.e-islam.net/api/health; then
  echo
  echo "Tamam. Aktif namespace: $DST"
else
  echo >&2
  echo "Health basarisiz. Log:" >&2
  k3s kubectl -n "$DST" get pods,ingress
  k3s kubectl -n "$DST" logs deploy/rag-api --tail=50 || true
  exit 1
fi

k3s kubectl get ns
k3s kubectl -n "$DST" get pods,ingress
k3s kubectl -n dogukan-prod get ns 2>/dev/null || k3s kubectl get ns dogukan-prod
