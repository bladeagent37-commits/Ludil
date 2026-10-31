# Yerel altyazı protokolü v1

TV APK’sında alıcı yalnızca kullanıcı başlatınca açılır. Port 8765. IPv4 özel/yerel adresler kabul edilir. Ağ dışı erişim için router/port yönlendirmesi açılmaz.

- `GET /health`: uygulama, sürüm ve eşleştirme durumu; kod/token içermez.
- `POST /pair`: `{"code":"123456"}`. Kod rastgele altı rakam, beş dakika geçerli, tek başarılı eşleştirme. Dakikada en fazla sekiz deneme. Yanıt `token` içerir; sadece bu oturumda kullanılır.
- `POST /subtitle`: `Authorization: Bearer <token>` ve `Content-Type: application/json`.

```json
{"id":1,"sourceText":"Stay here.","translatedText":"Burada kal.","ttlMs":4500}
```

Kimlikler monoton artmalıdır. Tekrarlanan/eski kimlik 409 alır. Çeviri en fazla 1600, kaynak en fazla 800 karakter. TTL 500–10000 ms arasında sınırlandırılır. Yazı süresi istemci saatine değil TV’nin monoton saatine göre hesaplanır.

- `POST /clear`: aynı yetkiyle `{"id":2}`.
- Yetkisiz istekler reddedilir. `Origin` bulunan tarayıcı istekleri reddedilir; CORS izni verilmez.
- Gövde en fazla 8192 bayt; socket zaman aşımı üç saniye; iki iş parçacığı ve dört bekleyen istek sınırı.
- Alıcı durdurulunca token/kod sıfırlanır. Uygulama yedeklemesine kimlik bilgisi dahil edilmez.

Bağlantı kopunca kullanıcı uygulamadan yeni kod üretip yardımcıyı tekrar eşleştirebilir. Token kalıcı diske yazılmaz. HTTP yerel ağ içindir; gizli PSN bilgilerinin taşıma yolu değildir.
