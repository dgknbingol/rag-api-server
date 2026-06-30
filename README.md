# AiSLAM RAG API

Spring Boot RAG servisi: PDF indeksleme, Qdrant vektör arama, LM Studio ile cevap üretimi.

Mobil istemci: [../aislam-mobile](../aislam-mobile)

## Gereksinimler

1. **LM Studio** — `http://localhost:1234`
   - Embedding: `text-embedding-bge-m3`
   - Chat: `qwen/qwen3.5-9b`
2. **Qdrant** — `http://localhost:6333`
3. **Java 21**
4. **Docker Desktop** — yerel PostgreSQL için

## Veritabanı (PostgreSQL)

Yerel geliştirmede H2 yerine Docker Compose ile PostgreSQL kullanılır.

```powershell
# rag-api-server klasöründe
docker compose up -d
```

Varsayılan bağlantı:

| Alan | Değer |
|------|--------|
| Host | `localhost:5432` |
| Database | `aislam` |
| User | `aislam` |
| Password | `aislam` |

İsteğe bağlı: `.env.example` → `.env` kopyalayıp şifreyi değiştirin.

API ilk açılışta `users` tablosunu otomatik oluşturur (`ddl-auto: update`).

Durdurmak:

```powershell
docker compose down
```

Veriyi silerek durdurmak:

```powershell
docker compose down -v
```

## Çalıştırma

PostgreSQL ayaktayken:

```bash
./mvnw spring-boot:run
```

Windows:

```powershell
.\scripts\run.ps1
```

Sunucu: `http://localhost:8080`

## Mobil bağlantı

Telefon/emülatör bu API'ye HTTP ile bağlanır. Geliştirme IP'si mobil tarafta `.env` içinde:

```
EXPO_PUBLIC_RAG_API_URL=http://192.168.x.x:8080
```

| Ortam | URL |
|--------|-----|
| Android emülatör | `http://10.0.2.2:8080` |
| Fiziksel cihaz (aynı Wi-Fi) | `http://<PC_LAN_IP>:8080` |

CORS: `app.cors.allowed-origin-patterns` (`application.yml`) — Expo Web için.

## API özeti

| Method | Path | Açıklama |
|--------|------|----------|
| POST | `/api/auth/register` | Kullanıcı kaydı |
| POST | `/api/auth/login` | Giriş (JWT) |
| GET | `/api/users/me` | Profil (Bearer token) |
| POST | `/api/rag/ask` | Soru → cevap + kaynaklar |
| POST | `/api/rag/retrieve` | Sadece kaynak arama |
| POST | `/api/documents/upload` | PDF yükleme |
| POST | `/api/documents/index` | Metin indeksleme |

### Örnek: soru sor

```http
POST /api/rag/ask
Content-Type: application/json

{"question": "İslam nedir?"}
```

## Yerel yapılandırma (isteğe bağlı)

`application-local.yml` oluşturun (git'e eklenmez):

```yaml
lmstudio:
  base-url: http://localhost:1234
qdrant:
  base-url: http://localhost:6333
```

`application-local.yml.example` dosyasına bakın.

## Test

```bash
./mvnw test
```
