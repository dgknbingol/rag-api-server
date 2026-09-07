# Backend'i k3s üzerinde yayına alma

Bu klasör **yalnızca Spring Boot API'sini** Kubernetes'e taşır.
Postgres ve Qdrant sunucuda Docker Compose ile çalışmaya devam eder — veri taşınmaz.

```
İnternet → Traefik (80/443) → Ingress → Service → rag-api pod
                                                      ↓
                              Docker: postgres:5432, qdrant:6333
```

## Neden bu yapı

Traefik zaten 80 ve 443 portlarını tutuyor ama hiçbir ingress kuralı tanımlı olmadığı
için her isteğe `404 page not found` dönüyordu. Aşağıdaki adımlar `api.e-islam.net`
için kural ve gerçek TLS sertifikası tanımlar.

---

## 1. Postgres ve Qdrant'ı pod'lara açın

Şu an ikisi de yalnızca Docker'ın iç ağında; host'ta yayınlanmış portları yok.
Pod'ların erişebilmesi için `deploy/docker-compose.prod.yml` içine port ekleyin:

```yaml
  postgres:
    ports:
      - "172.17.0.1:5432:5432"

  qdrant:
    ports:
      - "172.17.0.1:6333:6333"
```

> **`0.0.0.0` veya port numarasını tek başına YAZMAYIN.** Docker yayınlanan portlar için
> `ufw` kurallarını atlar; `- "5432:5432"` yazarsanız veritabanı doğrudan internete açılır.
> `172.17.0.1` host'un docker0 köprü adresidir: pod'lardan erişilir, dışarıdan erişilmez.

Uygulayın:

```bash
cd /path/to/rag-api-server/deploy
sudo docker compose -f docker-compose.prod.yml up -d postgres qdrant
```

Dışarıya kapalı olduğunu doğrulayın (bağlantı **kurulmamalı**):

```bash
curl -sS --max-time 5 http://89.167.0.187:6333/ ; echo "cikis: $?"
```

## 2. API konteynerini Compose'dan çıkarın

Artık API'yi Kubernetes çalıştıracak, aynı anda ikisi çalışmasın:

```bash
sudo docker compose -f docker-compose.prod.yml stop api
sudo docker compose -f docker-compose.prod.yml rm -f api
```

## 3. İmajı k3s'e aktarın

k3s containerd kullanır, Docker'ın imaj deposunu görmez:

```bash
sudo docker save aislam-rag-api:prod | sudo k3s ctr images import -
sudo k3s ctr images ls | grep aislam-rag-api
```

> Kod değiştiğinde: imajı yeniden `docker build` edip bu komutu tekrar çalıştırın,
> ardından `sudo k3s kubectl -n eislam rollout restart deploy/rag-api`.

## 4. cert-manager kurun

Let's Encrypt sertifikalarını otomatik alıp yeniler. Bir kez kurulur, diğer
projelerinizde de kullanılır.

```bash
sudo k3s kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.16.2/cert-manager.yaml
sudo k3s kubectl -n cert-manager rollout status deploy/cert-manager-webhook --timeout=180s
```

## 5. Manifestleri uygulayın

```bash
sudo k3s kubectl apply -f namespace.yaml
cp secret.example.yaml secret.yaml   # icini .env degerleriyle doldur
sudo k3s kubectl apply -f secret.yaml
sudo k3s kubectl apply -f configmap.yaml
sudo k3s kubectl apply -f deployment.yaml
sudo k3s kubectl apply -f service.yaml
sudo k3s kubectl apply -f cluster-issuer.yaml
sudo k3s kubectl apply -f ingress.yaml
```

## 6. Doğrulayın

```bash
# Pod ayakta mi
sudo k3s kubectl -n eislam get pods
sudo k3s kubectl -n eislam logs deploy/rag-api --tail=50

# Sertifika alindi mi (READY=True olmali, 1-2 dakika surebilir)
sudo k3s kubectl -n eislam get certificate

# Disaridan gercek sertifikayla cevap veriyor mu
curl -sS https://api.e-islam.net/api/health
```

Son komut JSON döndürüyorsa iş tamam. `TRAEFIK DEFAULT CERT` hatası alıyorsanız
sertifika henüz hazır değil, `kubectl -n eislam describe certificate` ile bakın.

---

## Sorun giderme

**Pod `CrashLoopBackOff`** — veritabanına bağlanamıyor olabilir. Pod içinden test:

```bash
sudo k3s kubectl -n eislam run netcheck --rm -it --image=busybox --restart=Never -- \
  sh -c "nc -zv 172.17.0.1 5432; nc -zv 172.17.0.1 6333"
```

Bağlanmıyorsa 1. adımdaki port yayınlaması eksik ya da adres farklı.
Host'un docker0 adresini şöyle görürsünüz: `ip -4 addr show docker0`

**Sertifika alınamıyor** — HTTP-01 doğrulaması 80 portunu kullanır. Traefik'in 80'i
dinlediğini ve DNS'in bu sunucuya baktığını doğrulayın:

```bash
sudo k3s kubectl -n cert-manager logs deploy/cert-manager --tail=50
```

## Statik site (e-islam.net) ayrı bir iş

`e-islam.net` hâlâ 404 dönüyor. Play Store gizlilik politikası (`/gizlilik`) ve
AdMob doğrulaması (`/app-ads.txt`) için o adresin de yayında olması gerekir.
Bu klasör yalnızca API'yi kapsar.
