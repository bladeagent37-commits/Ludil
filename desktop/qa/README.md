# Ludil görsel kontrolü

`VisualHarness.cs`, üretimdeki `UiTheme.cs` ve `MainForm.Layout.cs` kaynaklarını değiştirmeden Linux Mono üzerinde çalıştırır. İş mantığı yerine örnek metin kullanır. Dört sayfayı 1040×750 ve 900×650 boyutlarında açar; sınır taşmasını ve sabit etiketlerdeki metin kesilmesini kontrol eder, önizleme görselleri üretir. Yeni metin türü seçicisini ve örnek menü görünümünü de gösterir.

Roslyn `csc.dll` ile üç dosyayı Mono `mscorlib`, `System`, `System.Core`, `System.Drawing` ve `System.Windows.Forms` referanslarıyla derle. Bir X sunucusu açıkken `mono --config /etc/mono/config VisualHarness.exe <çıktı-klasörü>` çalıştır.

Bu test yerleşim kontrolüdür. Windows’a özgü yakalama, kısayol, katman veya gerçek oyun çalışmasını doğrulamaz. Bunlar Windows dağıtımındaki `--ui-smoke` moduyla ayrıca sınanır. Son çalışmanın sonuçları `docs/TESTLER.md` içindedir.
