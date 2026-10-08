#!/usr/bin/env bash
# Kubernetes Dashboard: Traefik Basic Auth + skip-login (token yok).
set -euo pipefail

NS=kubernetes-dashboard
HOST=k8s-control-dashboard.e-islam.net
USER_NAME=aislam
CREDS_FILE=/root/k8s-dashboard-credentials.txt

if [ "$(id -u)" -ne 0 ]; then
  echo "root gerekir: sudo $0" >&2
  exit 1
fi

command -v htpasswd >/dev/null || apt-get install -y apache2-utils

# Mevcut sifre varsa koru
if [ -f "$CREDS_FILE" ] && grep -q '^Sifre:' "$CREDS_FILE"; then
  PASS="$(awk -F': *' '/^Sifre:/{print $2; exit}' "$CREDS_FILE")"
  USER_NAME="$(awk -F': *' '/^Kullanici:/{print $2; exit}' "$CREDS_FILE")"
  USER_NAME="${USER_NAME:-aislam}"
else
  PASS="$(openssl rand -base64 18 | tr -d '/+=' | head -c 20)"
fi

HTPASS="$(htpasswd -nbB "$USER_NAME" "$PASS")"

k3s kubectl -n "$NS" create secret generic dashboard-basicauth \
  --from-literal=users="$HTPASS" \
  --dry-run=client -o yaml | k3s kubectl apply -f -

k3s kubectl apply -f - <<EOF
apiVersion: traefik.io/v1alpha1
kind: Middleware
metadata:
  name: dashboard-basicauth
  namespace: $NS
spec:
  basicAuth:
    secret: dashboard-basicauth
    removeHeader: true
---
apiVersion: traefik.io/v1alpha1
kind: ServersTransport
metadata:
  name: dashboard-transport
  namespace: $NS
spec:
  insecureSkipVerify: true
---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: kubernetes-dashboard
  namespace: $NS
  annotations:
    cert-manager.io/cluster-issuer: letsencrypt-prod
    traefik.ingress.kubernetes.io/router.entrypoints: web,websecure
    traefik.ingress.kubernetes.io/router.middlewares: ${NS}-dashboard-basicauth@kubernetescrd
    traefik.ingress.kubernetes.io/service.serversscheme: https
    traefik.ingress.kubernetes.io/service.serverstransport: ${NS}-dashboard-transport@kubernetescrd
spec:
  ingressClassName: traefik
  tls:
    - hosts:
        - $HOST
      secretName: k8s-dashboard-tls
  rules:
    - host: $HOST
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: kubernetes-dashboard
                port:
                  number: 443
EOF

ARGS='["--auto-generate-certificates","--namespace=kubernetes-dashboard","--enable-skip-login","--disable-settings-authorizer"]'
k3s kubectl -n "$NS" patch deploy kubernetes-dashboard --type='json' -p="[
  {\"op\":\"replace\",\"path\":\"/spec/template/spec/containers/0/args\",\"value\":$ARGS}
]"

k3s kubectl apply -f - <<EOF
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: kubernetes-dashboard-skip-admin
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: cluster-admin
subjects:
  - kind: ServiceAccount
    name: kubernetes-dashboard
    namespace: $NS
EOF

k3s kubectl -n "$NS" delete svc kubernetes-dashboard-nodeport --ignore-not-found
k3s kubectl -n "$NS" rollout status deploy/kubernetes-dashboard --timeout=120s

umask 077
cat > "$CREDS_FILE" <<EOF
URL:      https://$HOST
Kullanici: $USER_NAME
Sifre:     $PASS

DNS: $HOST A -> 89.167.0.187
EOF

echo "Kurulum tamam. Bilgiler: $CREDS_FILE"
cat "$CREDS_FILE"
