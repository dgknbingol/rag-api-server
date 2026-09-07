# e-islam.net statik site (gizlilik & koşullar)

Bu klasördeki dosyalar `https://e-islam.net` üzerinde yayınlanır.

## Dosyalar

| URL | Dosya |
|-----|-------|
| `/` | `index.html` |
| `/gizlilik` | `gizlilik.html` |
| `/kosullar` | `kosullar.html` |
| `/app-ads.txt` | `app-ads.txt` |

Mağaza listelerinde kullanılacak adresler:

- **Gizlilik politikası:** `https://e-islam.net/gizlilik`
- **Kullanım koşulları:** `https://e-islam.net/kosullar`

## app-ads.txt (AdMob)

AdMob, uygulama sahipliğini bu dosyadan doğrular. Ocak 2025 sonrası oluşturulan
uygulamalar doğrulanmazsa **kısıtlı reklam yayını** uygulanır ve gelir düşer.

Dosya tam olarak `https://e-islam.net/app-ads.txt` adresinde, `text/plain` olarak
sunulmalıdır. Play Store listesindeki "Web sitesi" alanı da `e-islam.net` olmalı;
AdMob crawler uygulama sayfasındaki adresten yola çıkar.

Doğrulama:

```bash
curl -sI https://e-islam.net/app-ads.txt   # 200 + Content-Type: text/plain
curl -s  https://e-islam.net/app-ads.txt
```

AdMob panelinde durum: **Uygulamalar → app-ads.txt** (doğrulama birkaç gün sürebilir).

## Sunucuya yükleme (Hetzner)

```bash
# Sunucuda klasör oluştur
sudo mkdir -p /var/www/e-islam.net
sudo rsync -av deploy/web/ user@89.167.0.187:/var/www/e-islam.net/
```

## Nginx yapılandırması

`deploy/nginx-e-islam.conf` dosyasını sunucuya kopyalayın:

```bash
sudo cp deploy/nginx-e-islam.conf /etc/nginx/sites-available/e-islam.net
sudo ln -sf /etc/nginx/sites-available/e-islam.net /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
```

SSL henüz yoksa:

```bash
sudo certbot --nginx -d e-islam.net -d www.e-islam.net
```

`api.e-islam.net` için mevcut API nginx bloğunu değiştirmeyin; bu yapılandırma yalnızca ana domain içindir.

## İletişim e-postası

İletişim e-postası: `info@e-islam.net`
