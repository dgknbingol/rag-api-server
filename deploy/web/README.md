# e-islam.net statik site (gizlilik & koşullar)

Bu klasördeki dosyalar `https://e-islam.net` üzerinde yayınlanır.

## Dosyalar

| URL | Dosya |
|-----|-------|
| `/` | `index.html` |
| `/gizlilik` | `gizlilik.html` |
| `/kosullar` | `kosullar.html` |

Mağaza listelerinde kullanılacak adresler:

- **Gizlilik politikası:** `https://e-islam.net/gizlilik`
- **Kullanım koşulları:** `https://e-islam.net/kosullar`

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
