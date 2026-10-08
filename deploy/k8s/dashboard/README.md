# Kubernetes Dashboard — kullanıcı / şifre

Resmi Dashboard token ister. Bu kurulum Traefik **Basic Auth** koyar ve
Dashboard’da **Skip login** açar; tarayıcıda yalnızca kullanıcı/şifre sorulur.

URL: `https://k8s.e-islam.net`

## DNS

`k8s.e-islam.net` → A kaydı → `89.167.0.187`

## Uygula

```bash
sudo /opt/aislam/deploy/k8s/dashboard/install.sh
```

Şifre `/root/k8s-dashboard-credentials.txt` içine yazılır.
