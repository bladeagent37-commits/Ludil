# Yerel geliştirme

## Depo düzeni

| Klasör | İçerik |
| --- | --- |
| `app/` | Android Java arayüz, OCR, çeviri, ekran katmanı ve eşleştirme alıcısı |
| `desktop/Lodil.Core/` | .NET çeviri, tokenizer, altyazı filtresi, oturum ve TV göndericisi |
| `desktop/Lodil.Windows/` | WinForms arayüz, Windows ekran yakalama, Tesseract OCR ve altyazı katmanı |
| `desktop/Lodil.Smoke/` | Gerçek çıkarım ve davranış regresyonları; Windows gerektirmez |
| `desktop/qa/` | Üretim arayüz kaynaklarını kullanan Linux Mono görsel kontrolü |
| `tests/` | Java motor, filtre, ekran bölgesi ve HTTP protokol senaryoları |
| `companion/` | İsteğe bağlı Python kaynak yardımcısı |
| `scripts/` | Model indirme/özet kontrolü ve yerel test komutları |

## Gereksinimler

Android: JDK 17; Android SDK platform 35; Build Tools 35.0.0. Gradle 8.9 wrapper özeti sabitlenmiştir. Windows/C# için `global.json` .NET SDK 10.0.403 sürümünü seçer. Model indirme ve Java host test sürücüsü için Python 3.10+ gerekir.

`python3 scripts/fetch-assets.py` büyük ONNX dosyalarını sabitlenmiş Hugging Face revizyonundan, Android AAR dosyalarını sürümü sabit Maven Central/JitPack adreslerinden indirip SHA-256 ile doğrular. `--check` yalnızca kontrol yapar. ONNX ve AAR dosyaları Git’e eklenmez. Tokenizer, İngilizce OCR verisi ve indirilen Android AAR bağımlılıkları `docs/DEPENDENCIES.json` ile doğrulanır.

## Android

```sh
python3 scripts/fetch-assets.py
./gradlew :app:assembleRelease :app:lintRelease
```

İmzalı APK için ortam değişkenlerini ayarla:

- `LODIL_SIGNING_STORE`: PKCS12/JKS dosyasının mutlak yolu.
- `LODIL_SIGNING_PASSWORD`: anahtar deposu ve anahtar parolası.
- `LODIL_SIGNING_ALIAS`: varsayılan `lodil`.

Anahtarı ve parolasını paylaşma veya Git’e ekleme. Güncellemeler aynı Android uygulama kimliği `app.lodil`, aynı yayın anahtarı ve artan versionCode kullanmalıdır. Anahtar verilmeden derlenen release APK imzasızdır.

## Windows

PowerShell’den:

```powershell
./desktop/publish-windows.ps1 -Output ./dist/Lodil-0.3.0-Windows-x64
```

Çıktı .NET dahil, kendi çalışma zamanını taşıyan x64 dağıtımdır. Script modelleri, OCR verisini, kullanım rehberini ve lisansları ekler. Paketler `packages.lock.json` ile sabitlenir. Bağımlılık değiştiriyorsan lock dosyalarını bilinçli olarak yeniden üret.

## Yerel testler

Depo kökünden:

```sh
python3 scripts/fetch-assets.py --check
dotnet run --project desktop/Lodil.Smoke -- app/src/main/assets/translation
python3 scripts/test-host.py
```

Java sürücüsü JDK 17’yi `JAVA_HOME` veya PATH üzerinden bulur. Gerçek host ONNX Runtime ve JSON JAR dosyalarını Maven Central’dan geçici klasöre indirir. Java → C# ortak protokol testi için `LODIL_DOTNET` değişkenine `dotnet` yürütülebilir dosyasının mutlak yolunu ver; C# native TV istemcisini gerçek Java alıcısına bağlar.

Windows’ta gerçek ekran testleri:

```powershell
./Lodil.exe --ui-smoke "$env:TEMP/Lodil-QA"
```

Bu mod üretim uygulamasını test ayarlarıyla açar, örnek görüntüde gerçek Windows OCR + çeviriyi, test penceresinden yakalamayı, altyazı konumunu ve hızlı başlat/durdurmayı sınar. Test raporu ve örnek ekran görüntülerini belirtilen klasöre yazar; kullanıcının normal oyun profillerini değiştirmez.

Linux görsel harness yalnızca yerleşimi ve temayı sınar; gerçek Windows yakalamasını kanıtlamaz. Kullanımı `desktop/qa/README.md` içindedir.

GitHub Actions oluşturulmaz veya çalıştırılmaz. Yayın dosyaları yerelde üretilir, imza ve içerik kontrollerinden sonra Releases bölümüne yüklenir.
