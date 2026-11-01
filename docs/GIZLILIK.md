# Gizlilik

- Hesap, reklam, analitik veya uzaktan hata toplama yoktur.
- Mikrofon, kamera ve erişilebilirlik hizmeti kullanılmaz.
- Ekran okuma Windows’ta kullanıcının seçtiği alanla sınırlıdır. Android’de sistem onayı gerekir; seçilen altyazı bandı veya geliştirme sürümündeki menü bölgesi işlenir.
- Görüntüler geçici bellekte işlenir. Normal kullanım ekran görüntüsü veya çeviri geçmişi kaydetmez.
- Çevrimdışı çeviri sırasında metin bir internet çeviri API’sine gönderilmez.
- Windows ayar dosyasında oyun adları, alan koordinatları ve metin türü ve görünüm tercihleri tutulur.
- TV bağlantısı isteğe bağlıdır. Sadece kaynak ve çeviri metni, açıkça eşleştirilen yerel adrese gönderilir. 6 haneli eşleştirme kodu 5 dakika geçerlidir; oturum token’ı diske yazılmaz.
- LAN aktarımı HTTP kullanır. Metinler ağ üzerinde TLS ile şifrelenmez; güvendiğin ev ağı için tasarlanmıştır.
- Android’de isteğe bağlı yerel çeviri sunucusu tanımlanırsa İngilizce metin o sunucuya gönderilir. Sunucu adresi ve girilen anahtar cihazın özel uygulama ayarlarında saklanır; bu alanları boş bırakarak çevrimdışı çalışabilirsin.
- `--ui-smoke` geliştirici test modu açıkça çağrıldığında örnek test ekranlarının görüntülerini ve test raporunu belirtilen klasöre yazar. Normal çalışma bunu yapmaz.
- Video testleri yalnızca üretilmiş temsili metin ve görüntülerden kayıtlar oluşturur; kişisel oyun kaydı gerekmez.
- GitHub’dan indirme ve isteğe bağlı model indirme, GitHub/Hugging Face hizmetlerinin ağ bağlantısını gerektirir. Hazır paketlerin ilk açılışında model indirilmez.
