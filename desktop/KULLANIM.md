# Ludil kurulumu

[English documentation](https://github.com/bladeagent37-commits/Ludil/blob/main/README.md) | [Türkçe belgeler](https://github.com/bladeagent37-commits/Ludil/blob/main/README.tr.md)

Yayımlanmış önizleme **v0.3.0** sürümüdür: [gerçek release sayfası](https://github.com/bladeagent37-commits/Ludil/releases/tag/v0.3.0). **v0.4.0 geliştirme aşamasındadır**; menü ve ayar modu yalnızca bu dalın yerel derlemelerinde bulunur. Eski indirme adresleri korunur. Aşağıdaki yeni yürütülebilir dosya adları v0.4.0 derlemelerine aittir.

## Windows

1. Yayımlanmış önizleme için release sayfasındaki Windows ZIP dosyasını indir. v0.4.0'ı deniyorsan yerelde hazırlanmış `Ludil-0.4.0-Windows-x64.zip` paketini kullan.
2. ZIP'in tamamını tek klasöre çıkart; EXE, modeller, OCR ve çalışma zamanı dosyalarını birlikte tut.
3. v0.4.0 derlemesinde `Ludil.exe` dosyasını aç. İlk çeviri model yüklemesi nedeniyle daha uzun sürebilir.
4. **Örnek çeviriyi dene** ile örnek görüntünün okunup çevrilebildiğini kontrol et.
5. Oyunun İngilizce altyazısını aç; pencere veya kenarlıksız tam ekran modunu dene.
6. **Altyazı** metin türünde yalnızca kaynak satırlarını işaretle ve **Çeviriyi başlat** düğmesine bas.

Windows 10/11 x64 gerekir. Paket .NET'i içerir. Yerel OCR bileşeni yüklenemezse uygulamanın Yardım sayfasına bak; gerekirse [resmî Microsoft Visual C++ x64 çalışma zamanını](https://aka.ms/vs/17/release/vc_redist.x64.exe) kur. Windows EXE'sinde Authenticode yayıncı imzası yoktur.

Altyazı katmanı fare ve klavye odağını almaz. Kontrol penceresi küçülürse bildirim alanındaki Ludil simgesine çift tıkla. Yazı çok küçükse oyunun kendi altyazı boyutunu büyüt. Korunan ekranlar veya bazı özel tam ekran modları yakalamayı engelleyebilir.

| Kısayol | İşlem |
| --- | --- |
| Ctrl+Shift+F8 | Çeviriyi başlat / durdur |
| Ctrl+Shift+F9 | Türkçe yazıyı gizle / göster |
| Ctrl+Shift+F10 | Kaynak alanını yeniden seç |

Oyun profilleri alan, metin türü ve görünümü hatırlar. v0.4.0 ayarları `%LOCALAPPDATA%\Ludil\settings.json` konumuna yazılır; yeni ayar yoksa önceki sürüm dizinindeki ayarlar okunur. Ekran görüntüsü veya çeviri geçmişi saklanmaz.

## Android / Android TV

1. APK'yı indirip cihaza aktar. Android isterse kullandığın dosya yöneticisine uygulama yükleme izni ver.
2. Ludil'i aç. Android 8.0 / API 26 veya üstü ve ARM64, ARMv7 ya da x86_64 gerekir; en az 1 GB boş alan önerilir.
3. Dahili örnek görüntü OCR / çeviri kontrolünü çalıştır.
4. **Çeviriyi başlat** üzerinden giriş kaynağını seç.
5. Ekran okuma için sistem ekran paylaşım onayını; çeviri katmanı için **Diğer uygulamaların üzerinde göster** iznini ver.
6. Bildirimdeki veya uygulamadaki **Durdur** düğmesiyle oturumu kapat.

Arayüz dar ve geniş ekranlara uyarlanır, kumanda odağı gösterilir. Fiziksel Android cihazlardaki izin, yakalama ve görünürlük akışları henüz doğrulanmadı. Güncelleme uyumluluğu için uygulamanın paket kimliği ve imza anahtarı korunur.

## Menü ve ayarlar — yalnızca v0.4.0 geliştirmesi

Windows'ta **Menü ve ayarlar** metin türünü seç; çevrilecek menü alanını işaretle. Çeviri paneli için sağda veya solda boş bir sütun bırak. Tam ekranın tamamını menü alanı olarak seçmek uygun değildir.

Android'de yeni menü/ayar kaynağını seç. Görünüm bölümünden menünün sol/orta veya sağ bölgede olacağını ayarla; bu tercihi değiştirmeden önce yakalamayı durdur. Çeviriler karşı kenarda gösterilir.

Settings → Ayarlar, New Game → Yeni Oyun ve Continue → Devam Et gibi bilinen etiketler ortak sözlükle çevrilir. Diğer kısa satırlar seçilen çeviri motoruna gider. Bir tarama en fazla 12 satır / 600 karakter kabul eder. Oyun dosyaları değiştirilmez; panel oyun düğmelerinin yerine geçmez. Bu geliştirme işlevi yayımlanmış v0.3.0 indirmelerinde bulunmaz.

## Windows'tan TV'ye aktarım

1. TV ve PC'yi güvendiğin aynı yerel ağa bağla.
2. TV'de Ludil → **Bağlantı → Bilgisayardan altyazı almaya başla** yolunu aç.
3. TV adresini ve altı haneli kodu gör.
4. Windows'ta Ludil → **Yardım → TV'ye bağlan** seçeneğine bu bilgileri yaz.
5. Kaynak alanını seçip çeviriyi başlat. Yeni menü modunu test ederken iki cihazda uyumlu geliştirme sürümlerini kullan.

Kod beş dakika geçerlidir ve bir kez kullanılabilir. Oturum alıcı durana kadar sürer. Yeniden eşleşmek için yeni kod oluştur. Aktarım kaynak ve çeviri metnini HTTP üzerinden yollar; TLS şifrelemesi yoktur. Windows'taki bağlantıyı kes seçeneği gönderimi kapatır. Durdurmak TV yazısını temizler; ağ kesilirse yazı süre sonunda kaldırılır. Fiziksel TV akışı henüz doğrulanmadı.

## HDMI ve PlayStation

Ludil PSN bilgilerini istemez ve kendi başına Remote Play bağlantısı kurmaz. Android ekran paylaşımı her TV'de HDMI kaynağını yakalayamaz. TCL HDMI üzerinde katman görünürlüğü ayrıca fiziksel cihazda sınanmalıdır.

**HDMI yazı testi** sabit örnek Türkçe metin gösterir; görüntü okuma veya gerçek PlayStation çevirisini kanıtlamaz. İzin verilen Windows Remote Play görüntüsü kaynak olarak denenebilir; bu akış da henüz cihazda doğrulanmadı. Son yerel sonuçlar [test raporundadır](https://github.com/bladeagent37-commits/Ludil/blob/main/docs/TESTLER.md).
