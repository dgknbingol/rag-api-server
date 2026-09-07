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
Gerekli değişiklikler `deploy/docker-compose.prod.yml` içinde **zaten yapılmış** durumda:

- `postgres` → `172.18.0.1:5432`, `qdrant` → `172.18.0.1:6333` adreslerine yayınlanıyor
- `internal` ağının subnet'i `172.18.0.0/16` olarak sabitlendi
- `api` servisi `docker-only` profiline alındı, artık varsayılan olarak başlamıyor

### Neden `172.18.0.1`, `172.17.0.1` değil

`172.17.0.1` Docker'ın varsayılan `docker0` köprüsüdür, ama bu sunucuda hiçbir
konteyner ona bağlı olmadığı için arayüz `state DOWN` durumda. Arayüz kapalıyken
çekirdek o adrese gelen paketleri yerel kabul etmez, yani pod'lar bağlanamaz.
`172.18.0.1` ise servislerin bağlı olduğu `internal` köprüsünün host tarafındaki
ağ geçidi — her zaman açık ve Docker'ın kendi oluşturduğu bir arayüz olduğu için
yeniden başlatmalarda konteynerlerden önce hazır oluyor.

> **`0.0.0.0` veya port numarasını tek başına YAZMAYIN.** Docker yayınlanan portlar için
> `ufw` kurallarını atlar; `- "5432:5432"` yazarsanız veritabanı doğrudan internete açılır.
> `172.18.0.1` özel bir adres: pod'lardan erişilir, dışarıdan erişilmez.

### Uygulayın

Ağ tanımı değiştiği için Compose ağı yeniden oluşturmalı, bu da konteynerlerin
yeniden yaratılmasını gerektiriyor. Veriler `pg_data` ve `qdrant_data` adlı
volume'larda durduğu için kaybolmaz.

```bash
cd /opt/aislam/deploy
sudo docker compose -f docker-compose.prod.yml --profile docker-only down
sudo docker compose -f docker-compose.prod.yml up -d
```

> **`down` komutuna `-v` EKLEMEYİN.** `-v` volume'ları da siler; veritabanı ve
> vektör indeksi tamamen gider.

`down` komutundaki `--profile docker-only` şart: profil belirtilmezse Compose
`api` servisini kapsam dışı bırakır, eski `aislam-api-1` konteyneri ayakta kalır
ve ağa bağlı olduğu için `Network aislam_internal ... Resource is still in use`
hatasıyla ağ yeniden oluşturulamaz.

`up -d` yalnızca postgres ve qdrant'ı başlatır — `api` profil altında olduğu için
atlanır, yani 2. adıma ayrıca gerek kalmaz.

### Doğrulayın

Adres ve portlar beklendiği gibi mi:

```bash
sudo docker compose -f docker-compose.prod.yml ps
ip -4 addr show br-$(sudo docker network inspect aislam_internal -f '{{.Id}}' | cut -c1-12)
```

Dışarıya kapalı olduğunu doğrulayın (bağlantı **kurulmamalı**, `cikis: 28` ya da `7` beklenir):

```bash
curl -sS --max-time 5 http://89.167.0.187:6333/ ; echo "cikis: $?"
```

Pod ağından erişilebildiğini doğrulayın (ikisi de `open` demeli):

```bash
sudo k3s kubectl run netcheck --rm -it --image=busybox --restart=Never -- \
  sh -c "nc -zv 172.18.0.1 5432; nc -zv 172.18.0.1 6333"
```

Bu son kontrol geçmeden sonraki adımlara geçmeyin — geçmezse pod açılışta
veritabanına bağlanamaz ve `CrashLoopBackOff` olur.

## 2. API konteyneri

Ayrı bir işlem gerekmiyor: `api` servisi Compose'da `docker-only` profiline
alındığı için 1. adımdaki `up -d` onu başlatmıyor, `down` ise eski çalışan
konteyneri zaten kaldırdı. `docker ps` çıktısında `aislam-api-1` görünmemeli.

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
  sh -c "nc -zv 172.18.0.1 5432; nc -zv 172.18.0.1 6333"
```

Bağlanmıyorsa 1. adımdaki port yayınlaması eksik ya da adres farklı.
`internal` köprüsünün güncel adresini şöyle görürsünüz:

```bash
sudo docker network inspect aislam_internal \
  -f '{{range .IPAM.Config}}{{.Gateway}}{{end}}'
```

**Sertifika alınamıyor** — HTTP-01 doğrulaması 80 portunu kullanır. Traefik'in 80'i
dinlediğini ve DNS'in bu sunucuya baktığını doğrulayın:

```bash
sudo k3s kubectl -n cert-manager logs deploy/cert-manager --tail=50
```

## Statik site (e-islam.net) ayrı bir iş

`e-islam.net` hâlâ 404 dönüyor. Play Store gizlilik politikası (`/gizlilik`) ve
AdMob doğrulaması (`/app-ads.txt`) için o adresin de yayında olması gerekir.
Bu klasör yalnızca API'yi kapsar.
