# Lodil kurulumu

## Windows

1. Sürümler sayfasından `Lodil-0.3.0-Windows-x64.zip` dosyasını indir.
2. ZIP’in tamamını bir klasöre çıkart. Sadece EXE’yi başka yere kopyalama; yanındaki model ve çalışma zamanı dosyaları gerekir.
3. `Lodil.exe` dosyasını aç. İlk çeviri model yüklemesi nedeniyle daha uzun sürebilir.
4. **Örnek çeviriyi dene** ile örnek bir görüntünün okunup çevrilebildiğini kontrol et.
5. Oyunun İngilizce altyazısını etkinleştir; pencere veya kenarlıksız tam ekran kullan.
6. **Alanı seç** ile yalnızca İngilizce altyazı satırlarını işaretle. **Çeviriyi başlat** düğmesine bas.

Altyazı katmanı fare tıklamalarını ve klavye odağını almaz. Uygulama penceresi küçülürse sağ alttaki Lodil simgesine çift tıkla. Yazı çok küçükse oyunun kendi altyazı boyutunu büyüt. Korunan ekranlar veya bazı özel tam ekran modları yakalamaya izin vermeyebilir.

| Kısayol | İşlem |
| --- | --- |
| Ctrl+Shift+F8 | Çeviriyi başlat/durdur |
| Ctrl+Shift+F9 | Türkçe yazıyı gizle/göster |
| Ctrl+Shift+F10 | İngilizce altyazı alanını yeniden seç |

Oyun profilleri **Oyun profilleri** bölümündedir. Her profilin alanı ve görünümü ayrı saklanır. Ayar dosyası `%LOCALAPPDATA%\Lodil\settings.json` konumundadır; ekran görüntüsü veya çeviri geçmişi içermez.

Yerel OCR bileşeni yüklenemezse uygulama Yardım sayfasında açıklama gösterir. Microsoft Visual C++ x64 çalışma zamanı eksik olan sistemler için resmi yükleyici: <https://aka.ms/vs/17/release/vc_redist.x64.exe>.

## Android / Android TV

1. APK’yı indirip cihaza aktar. Android isterse kullandığın dosya yöneticisinin uygulama yükleme iznini aç.
2. Lodil’i aç. Android 8 veya üstü gerekir; en az 1 GB boş alan önerilir.
3. **Örnek görüntüde oku ve Türkçeye çevir** aracını çalıştır.
4. **Çeviriyi başlat** düğmesinden Android ekranını okuma veya bilgisayardan altyazı alma modunu seç.
5. Ekran okumak için sistem ekran paylaşım iznini; altyazı katmanı için **Diğer uygulamaların üzerinde göster** iznini ver.
6. Bildirimdeki veya uygulamadaki **Durdur** düğmesiyle oturumu kapat.

Telefonlarda üstte kaydırılabilir menü, geniş ekran ve TV’lerde sol menü kullanılır. Kumandada odak mercan çerçeveyle görünür.

## Windows’tan TV’ye altyazı

1. TV ve PC’yi güvendiğin aynı ev ağına bağla.
2. TV’de Lodil → **Bağlantı → Bilgisayardan altyazı almaya başla** yolunu aç.
3. TV adresini ve 6 haneli kodu gör.
4. Windows’ta Lodil → **Yardım → TV’ye bağlan** seçeneğine bu iki bilgiyi yaz.
5. Windows’ta oyun/izinli Remote Play görüntüsünün İngilizce altyazı alanını seç ve çeviriyi başlat.

Kod 5 dakika geçerlidir, bir kez kullanılabilir. Oturum uygulama hizmeti durana kadar açıktır. Yeniden bağlanmak için TV’de yeni kod oluştur. Yalnızca altyazı metni LAN üzerinden gider; bu protokol TLS ile şifrelenmez. Ev ağına güvenilmiyorsa çevrimdışı cihaz modunu kullan.

Windows’taki **TV bağlantısını kes** seçeneği gönderimi kapatır. Çeviriyi durdurmak TV’deki yazıyı da temizler; ağ koparsa TV yazıyı otomatik olarak süre sonunda kaldırır.

## HDMI ve PlayStation

Lodil, PlayStation hesabına giriş yapmaz ve PSN şifresi istemez. APK tek başına Remote Play bağlantısı kurmaz. Android ekran paylaşımı HDMI kaynağını her TV’de yakalayamaz. TCL HDMI katmanında yazı görünürlüğü de cihazda ayrıca sınanmalıdır.

**HDMI yazı testi**ni açıp HDMI girişine geç; örnek Türkçe yazı görünür kalıyor mu kontrol et. Bu test sabit metin gösterir. Görüntü okumayı veya gerçek PlayStation çevirisini test etmez. Gerekirse Windows’ta izin verilen Remote Play görüntüsü kaynak olarak kullanılabilir; bu akışın cihazda doğrulanması gerekir.
