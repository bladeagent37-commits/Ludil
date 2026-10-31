# Lodil 0.3.0 yerel doğrulama

Tarih: **31 Ekim 2026**. GitHub Actions kullanılmadı. Aşağıdaki testler bu sürümün kaynakları ve dağıtım dosyaları üzerinde yerelde çalıştırıldı.

| Alan | Sonuç | Doğrulanan davranış |
| --- | --- | --- |
| Windows x64 yayın derlemesi | Geçti | .NET SDK 10.0.403, kendi çalışma zamanını taşıyan Release paketi, sabit bağımlılık kilitleri. |
| Gerçek C# ONNX çıkarımı | Geçti | Önceki 5 cümle için birebir regresyon; ayrıca 6 gerçek cümle, Türkçe çıktı, önbellek ve iptal kontrolü. |
| C# derin regresyonlar | 74 kontrol geçti | Boş/uzun giriş, Unicode/NFKC, düşük ve geçersiz güven, tekrarlanan/kaybolan altyazı, ayar kurtarma, 1000 profil sınırı ve TV adres doğrulaması. |
| Oturum eşzamanlılığı | Geçti | 10.000 paralel oturum bileti birbirinden farklı; yalnızca son oturum geçerli. |
| Gerçek Java ONNX çıkarımı | Geçti | Gerçek model ile İngilizce → Türkçe; doğru hedef dil token’ı ve motor filtreleri. |
| Java filtre ve görüntü bölgesi | 38 kontrol geçti | Tekrar gösterim, boş metin, güven sınırları, sahne geçerliliği, görünmeyen katmanda tam okuma alanı ve görünür katmanın hariç tutulması. |
| Gerçek LAN alıcısı | Geçti | Kodla eşleştirme, token, kodun yeniden kullanılamaması, Origin reddi, UTF-8, mesaj sırası, TTL sınırları, boş/çok uzun veri ve temizleme. |
| Alıcı yaşam döngüsü | Geçti | 12 ardışık kapat/yeniden aç döngüsünde port yeniden kullanılabildi. |
| C# → Java birlikte çalışabilirliği | Geçti | Windows’un üretim TV istemcisi gerçek Java alıcısına eşleşti; 16 eşzamanlı gönderim, artan mesaj kimliği, Türkçe karakter ve temizleme sınandı. |
| Windows görsel yerleşimi | Geçti | Üretim tema ve yerleşim kodu Linux Mono’da dört sayfa × iki pencere boyutunda çalıştırıldı; sınır taşması ve sabit etiketlerde metin kesilmesi kontrol edildi. |
| Android Release + lint | Geçti | Sıfır lint hatası. Bir `InsecureBaseConfiguration` uyarısı: isteğe bağlı LAN protokolünün HTTP kullanması. Bu tasarım ve sınırı gizlilik belgesinde açıklanır. |
| Android yayın imzası | Geçti | APK Signature Scheme v2, RSA 4096 bit. `app.lodil`, versionCode 3, versionName 0.3.0. |
| Model/yerel bağımlılık özetleri | Geçti | 6 dosyanın SHA-256 ve boyutu sabitlenmiş kaynak bildirimleriyle eşleşti. |
| Son dağıtım dosyaları | Geçti | İki arşivin CRC kontrolü; APK’da tam 3 çeviri dosyası, 3 ABI ve imza anahtarı bulunmaması; Windows’ta tüm 299 dosyanın özeti, modeller, OCR, çalışma zamanı ve lisanslar. |

Ham sonuçlar [test-results](test-results/) klasöründedir. Çalıştırma komutları [geliştirme rehberinde](GELISTIRME.md) bulunur.

## Testlerin yakaladığı düzeltmeler

- Kısa süre kaybolup tekrar gelen aynı altyazının atlanması düzeltildi.
- Geçersiz güven değerleri ve boş metnin çıkarıma ulaşması engellendi.
- Yeni Windows TV göndericisinde alıcının beklediği sabit uzunluklu HTTP gövdesi kullanıldı; birlikte çalışabilirlik testiyle doğrulandı.
- Küçük Windows penceresinde iki adım açıklaması kısaltıldı; kesilen metinleri denetleyen görsel kontrol eklendi.
- Android’de derleme ortamının oluşturduğu gizli geçici model dosyaları varlık paketinden dışlandı. Son APK kontrolü yalnızca beklenen modellerin bulunduğunu doğrular.

## Henüz cihazda doğrulanmayanlar

- Gerçek Windows açılışı, Windows Tesseract ekran yakalaması, kısayollar ve oyun üzerinde katman görünürlüğü. Linux görsel testi bunları kanıtlamaz. Windows paketindeki `Lodil-Test.cmd` / `--ui-smoke` modu bu kontroller için hazırlanmıştır.
- Android cihazda açılış, bildirim/üzerinde gösterme izin akışı ve MediaProjection çalışma davranışı. Java host çıkarım testi Android cihaz testi değildir.
- Gerçek TCL HDMI katmanı, PlayStation görüntüsü ve Remote Play bağlantısı.
- Uzun süreli gerçek cihaz performansı, düşük bellek ve farklı üreticilerin güç yönetimi.

Bu nedenle **0.3.0 herkese açık önizleme** olarak hazırlanmıştır. Windows çalıştırılabilir dosyası için Authenticode yayıncı imzası eklenmedi.

Android yayın sertifikası SHA-256:

```text
af4abcea99afa1c357e0179514b0a30341ee93cce150f16de46f8e6310212d03
```
