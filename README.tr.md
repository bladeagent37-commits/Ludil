[English](README.md) | [Türkçe](README.tr.md)

# Ludil

**Oyunu kendi dilinde yaşa.**

Ludil, Windows ve Android / Android TV için ücretsiz ve açık kaynak bir **İngilizce → Türkçe oyun metni çeviricisidir**. OCR, yerel çeviri modelleri ve altyazı katmanını birleştirir. Uygulama arayüzü şu anda Türkçedir; bu depoda İngilizce ve Türkçe belgeler sunulur.

<img src="docs/images/ludil-mark.svg" width="64" alt="Ludil logosu" />

## Projenin durumu

**Yayındaki sürüm, herkese açık önizleme olan v0.3.0'dır. v0.4.0 bu dalda geliştirilmektedir ve GitHub release olarak henüz yayımlanmamıştır.** Yeni menü ve ayarlar modu bu geliştirme çalışmasına aittir; aşağıdaki v0.3.0 indirmelerinde bulunmaz.

Yerel bileşen testleri gerçek cihaz uyumluluğunu kanıtlamaz. Windows'ta gerçek oyun yakalama, Android izin/yakalama davranışı, TCL HDMI katmanı ve PlayStation / Remote Play akışları fiziksel cihazlarda doğrulanmadı. [Doğrulama sonuçlarına](docs/TESTLER.md) bak.

## İndirmeler

Şu an kullanılabilen önizleme için [yayımlanmış v0.3.0 sürümünü](https://github.com/bladeagent37-commits/Ludil/releases/tag/v0.3.0) kullan.

| Platform | Yayımlanmış indirme | Paketin içeriği |
| --- | --- | --- |
| Windows x64 | [Windows önizleme v0.3.0](https://github.com/bladeagent37-commits/Ludil/releases/download/v0.3.0/Lodil-0.3.0-Windows-x64.zip) | Taşınabilir uygulama, .NET çalışma zamanı, OCR ve çeviri modelleri |
| Android / Android TV | [Android önizleme v0.3.0](https://github.com/bladeagent37-commits/Ludil/releases/download/v0.3.0/Lodil-0.3.0-Android.apk) | İngilizce OCR ve çeviri modellerini içeren imzalı APK |
| Bütünlük kontrolü | [SHA-256 özetleri](https://github.com/bladeagent37-commits/Ludil/releases/download/v0.3.0/SHA256SUMS.txt) | Yayımlanmış iki paketin özetleri |

Mevcut indirme adresleri uyumluluk için korunur. Yeni derleme dosyaları resmî ad olan **Ludil** adını kullanır. Yayımlanmadan önce v0.4.0 release indirme bağlantısı sunulmaz.

## Özellikler ve kullanılabilirlik

| Yetenek | Durumu |
| --- | --- |
| Seçilen altyazı alanında OCR ve çevrimdışı İngilizce → Türkçe çeviri | v0.3.0 önizlemede mevcut; fiziksel cihaz doğrulaması bekliyor |
| Windows oyun profilleri, altyazı görünümü ve başlat/durdur kısayolları | v0.3.0'da mevcut |
| Sistem onayı ve üzerinde gösterme izniyle Android ekran okuma | v0.3.0'da mevcut; cihaz davranışı doğrulanmadı |
| Eşleştirilen yerel bağlantı üzerinden isteğe bağlı Windows → TV altyazısı | v0.3.0'da mevcut; C# gönderici / Java alıcı birlikte çalışabilirliği yerelde test edildi |
| Settings, New Game ve Continue gibi menü etiketleri ile ayarların çevirisi | v0.4.0 için geliştiriliyor; yayımlanmış v0.3.0 paketlerinde yok |
| Ortak menü sözlüğü, satır bazında çeviri yedeği ve ayrı menü gösterim alanı | v0.4.0 için geliştiriliyor |
| Otomatik PlayStation bağlantısı veya garantili HDMI yakalama | Uygulanmadı / garanti edilmiyor |

Hesap, reklam, abonelik veya ücretli çeviri API'si gerekmez. Paketlenen yerel model şu anda **İngilizce → Türkçe** destekler; her dil çifti için çeviri sunmaz.

## Sistem gereksinimleri

| Kullanım | Gereksinimler |
| --- | --- |
| Windows paketi | Windows 10/11, x64; yakalanabilir masaüstü alanı; pencere veya kenarlıksız oyun modu önerilir |
| Android paketi | Android 8.0 / API 26 veya üstü; ARM64, ARMv7 veya x86_64; ilgili mod için sistem ekran paylaşımı ve üzerinde gösterme izinleri |
| Android depolama | Kurulum ve model çıkarımı için en az 1 GB boş alan önerilir |
| PC → TV modu | Aynı güvenilen yerel ağa bağlı iki cihaz; açıkça eşleştirilen TV adresi ve geçici kod |
| Kaynaktan geliştirme | .NET SDK 10.0.403, JDK 17, Android SDK 35 / Build Tools 35.0.0, varlık/test yardımcıları için Python 3.10+ |

Windows paketi .NET'i içerir; normal uygulama kullanımı için Python gerekmez. Yerel OCR bağımlılığı eksik sistemlerde [resmî Microsoft Visual C++ x64 çalışma zamanı](https://aka.ms/vs/17/release/vc_redist.x64.exe) gerekebilir. Yerel model yüklenirken ilk çeviri daha uzun sürebilir. Düşük bellekli cihaz performansı henüz doğrulanmadı.

## Windows kurulumu

1. Yayımlanmış sürümden Windows ZIP dosyasını indir.
2. **Arşivin tamamını** tek klasöre çıkart; çalıştırılabilir dosya, çalışma zamanı ve modelleri birlikte tut.
3. Klasördeki uygulama dosyasını aç. Güncel geliştirme dalından alınan derlemelerde adı `Ludil.exe` olur.
4. Oyun alanını seçmeden önce örnek görüntü çevirisi kontrolünü çalıştır.
5. Oyunda İngilizce altyazıyı aç, ekrandaki alanını seç ve çeviriyi başlat.

Yerel OCR yükleme hatasında uygulamanın Yardım bölümüne ve yukarıdaki çalışma zamanı bağlantısına bak. Mevcut Windows çalıştırılabilir dosyasında Authenticode yayıncı imzası yoktur.

## Android / Android TV kurulumu

1. APK dosyasını indirip cihaza aktar.
2. Dosya yöneticisinden aç; Android isterse o dosya yöneticisine uygulama kurma izni ver.
3. Ludil'i aç ve dahili örnek OCR / çeviri kontrolünü dene.
4. Giriş kaynağını seç; seçilen mod gerektiriyorsa ekran paylaşma ve diğer uygulamaların üzerinde gösterme izinlerini ver.
5. Oturumu uygulamadaki veya bildirimindeki Durdur seçeneğiyle kapat.

Arayüz dar/geniş ekranlara uyarlanır ve D-pad odak görünümü sunar. Bu davranış fiziksel Android cihazlarda henüz doğrulanmadı.

## Kullanım

### Altyazılar

Windows'ta yalnızca İngilizce altyazının alanını seçip çeviriyi başlat. Katman seçilen kaynak alanının dışında tutulur. Oyun profilleri alanı ve görünümü hatırlar. Android ekran okuma modu, sistemin onayladığı paylaşılan görüntüde seçilmiş altyazı bandını işler.

| Windows kısayolu | İşlem |
| --- | --- |
| Ctrl+Shift+F8 | Çeviriyi başlat / durdur |
| Ctrl+Shift+F9 | Altyazıyı gizle / göster |
| Ctrl+Shift+F10 | Kaynak alanını yeniden seç |

### Menü ve ayar metinleri — v0.4.0 geliştirmesi

Geliştirme dalına özel bir menü modu eklenmektedir. Windows'ta menü ve ayarlar metin türünü seçip menü alanını işaretle. Android'de yeni kaynak seçeneği sol/orta veya sağ menü alanını okur, çeviriyi karşı kenarda gösterir. Bilinen etiketler ortak sözlükle, diğer kısa satırlar seçilen çeviri motoruyla (varsayılan olarak yerelde) çevrilir.

Bu mod her taramada en fazla 12 görünür metin satırı ve 600 karakter için tasarlanmıştır. Ekran görüntüsünü okuyup çeviri paneli gösterir; oyun dosyalarını değiştirmez, oyun arayüzünün yerine geçmez ve çevrilen düğmelere tıklamaz. Doğrulama durumu ve test kanıtları ayrıca test raporunda kaydedilir. Bu talimatlar geliştirme davranışını anlatır; yayımlanmış v0.3.0 APK/ZIP dosyalarındaki kullanılabilir işlevleri anlatmaz.

### İsteğe bağlı PC → TV aktarımı

TV'de bağlantı bölümünden altyazı alıcısını başlat. Windows'ta Yardım → TV'ye bağlan yolunu açıp TV'deki adresi ve altı haneli kodu gir. Kod beş dakika geçerlidir ve bir kez kullanılabilir; oturum token'ı alıcı durana kadar geçerlidir. Bu akış için Python gerekmez. Yeni menü modunu denerken iki cihazda da uyumlu geliştirme sürümlerini kullan.

### HDMI ve PlayStation

Ludil bir Remote Play istemcisi değildir ve PSN bilgilerini istemez. TV'nin HDMI girişi Android ekran paylaşımıyla erişilebilir olmayabilir. HDMI yazı testi sabit örnek altyazı gösterir; HDMI yakalamayı veya PlayStation çevirisini kanıtlamaz. Mevcut sınırlar için [kurulum rehberine](docs/KURULUM.md) bak.

## Kaynaktan derleme

```sh
python3 scripts/fetch-assets.py
./gradlew :app:assembleRelease :app:lintRelease
dotnet run --project desktop/Ludil.Smoke -- app/src/main/assets/translation
```

Varlık yardımcısı sürümü sabitlenmiş ONNX modellerini ve Android kütüphanelerini indirir; boyutlarını ve SHA-256 özetlerini [DEPENDENCIES.json](docs/DEPENDENCIES.json) ile karşılaştırır. Büyük ONNX/AAR dosyaları Git'e eklenmez. Yayımlanmış paketler gereken modelleri zaten içerir.

Windows'ta masaüstü uygulamasını paketlemek için:

```powershell
./desktop/publish-windows.ps1 -Output ./dist/Ludil-Windows-x64
```

Android yayın imzası özel ortam değişkenleri ve depo dışında sağlanan anahtar deposuyla alınır. Güncelleme uyumluluğu için mevcut uygulama kimliği ve imza korunur. İmza anahtarını veya parolasını Git'e ekleme. [Geliştirme notlarına](docs/GELISTIRME.md) bak.

## Yerel doğrulama

```sh
python3 scripts/fetch-assets.py --check
dotnet run --project desktop/Ludil.Smoke -- app/src/main/assets/translation
python3 scripts/test-host.py
python3 scripts/test-video.py
python3 scripts/check-docs.py
```

Video testleri ayrıca FFmpeg, Tesseract 5 CLI ve Pillow (`python3 -m pip install Pillow`) gerektirir; kanıtları `qa/video/` klasörüne yazar.

Host testleri gerçek C#/Java ONNX çıkarımını, altyazı filtresini, oturum geçersizleştirmeyi, ayar kurtarmayı, eşleştirmeyi ve yerel HTTP protokolünü sınar. Geliştirme çalışması menü regresyonları ile temsili video OCR/çeviri testlerini de ekler; tamamlanan sonuçlar [TESTLER.md](docs/TESTLER.md) belgesinde, ham kanıtlar [docs/test-results](docs/test-results/) altında tutulur.

Windows'a özgü kontroller `--ui-smoke <çıktı-klasörü>` ile kullanılabilir. Linux görsel yardımcısı üretim yerleşim/tema kaynaklarını örnek verilerle kontrol eder; Windows yakalamayı taklit etmez. Video testlerinde gerçek oyun veya cihaz kaydı yerine üretilmiş İngilizce metinler ve temsili arka planlar kullanılır. **GitHub Actions kullanılmaz: derlemeler ve doğrulamalar yerelde çalıştırılır.**

## Bilinen sınırlamalar

- OCR; yazı tipine, boyutuna, kontrasta ve kararlı görüntüye bağlıdır. Stilize veya hızlı değişen metinler kaçabilir.
- Yerel makine çevirisi hatalı olabilir. Paketlenen model yalnızca İngilizce → Türkçe destekler.
- Menü kapsamı seçilen alan, satır/karakter sınırları ve sözlükle sınırlıdır. Bilinmeyen kısa etiketler modelle çevrilir.
- Korumalı içerik, bazı özel tam ekran oyunlar ve cihaz güç yönetimi yakalamayı/katmanı engelleyebilir veya kesebilir.
- Fiziksel Windows/Android çalışması, TCL HDMI görünürlüğü ve PlayStation / Remote Play akışları doğrulanmadı.
- Bileşen, görsel ve temsili video testleri her oyun veya cihazla uyumluluğu kanıtlamaz.

## Gizlilik

Normal kullanımda yakalanan görüntüler bellekte işlenir; ekran görüntüsü veya çeviri geçmişi kaydedilmez. Varsayılan çeviri yerelde çalışır. Kaydedilen tercihler oyun adlarını, seçilen alanları ve görünüm ayarlarını içerir.

İsteğe bağlı TV aktarımı kaynak/çeviri metnini eşleştirilen yerel cihaza gönderir. Bu LAN protokolü TLS yerine HTTP kullanır; güvenilen ağlar için tasarlanmıştır. Oturum bilgileri bellekte tutulur. Kullanıcının isteğe bağlı tanımladığı yerel çeviri sunucusu İngilizce metni alır; adresi ve girilen anahtar Android'in özel uygulama tercihlerine kaydedilir.

Geliştirici test modları, seçilen çıktı klasörüne temsili kare, ekran görüntüsü veya raporları açıkça kaydeder. Kaynaktan kurulum sırasında model/bağımlılık indirmeleri ilgili kaynak sunucularla iletişim kurar. [Gizlilik notlarına](docs/GIZLILIK.md) bak.

## Lisans ve üçüncü taraf bileşenler

Ludil proje kodu [MIT Lisansı](LICENSE) ile sunulur. OCR kütüphaneleri, ONNX Runtime, .NET ve çeviri modellerinin kendi lisansları ile bildirimleri korunur; proje MIT lisansı bunların yerine geçmez. [LICENSES.md](LICENSES.md), [model kaynağı](docs/DEPENDENCIES.json), [ortak bağımlılık bildirimleri](docs/licenses/) ve [Windows bildirimlerine](desktop/licenses/) bak.

## Katkı

Hata bildirimleri, tekrarlanabilir testler, erişilebilirlik, performans ve belge iyileştirmeleri kabul edilir. Issue veya pull request içinde sürüm, işletim sistemi, giriş modu, beklenen davranış ve ilgili yerel test sonuçlarını belirt. Hangi davranışların fiziksel cihazda sınandığını, hangilerinin sınanmadığını açıkla.

Değişiklik göndermeden önce ilgili yerel kontrolleri çalıştır. İngilizce ve Türkçe README içeriğini uyumlu tut; mevcut release adreslerini koru; imza bilgilerini, kişisel ekran görüntülerini ve üretilmiş paketleri Git'e ekleme. GitHub Actions iş akışı ekleme veya tetikleme. [CONTRIBUTING.md](CONTRIBUTING.md) belgesine bak.

## Proje kanıtları

Yayımlanmış paketler, sabitlenmiş bağımlılık özetleri, kaynak kod ve depoya kaydedilmiş test raporları projenin doğrulanabilir kaydını oluşturur. Planlanan işler, yayımlanmamış geliştirmeler ve yalnızca yerelde yapılan testler açıkça etiketlenir.
