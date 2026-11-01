[English](README.md) | [Türkçe](README.tr.md)

# Ludil

**Experience the game in your own language.**

Ludil is a free, open-source **English-to-Turkish game text translator** for Windows and Android / Android TV. It combines OCR, local translation models and a caption overlay. The application interface is currently Turkish; this repository provides English and Turkish documentation.

<img src="docs/images/ludil-mark.svg" width="64" alt="Ludil logo" />

## Project status

**The published version is v0.3.0, a public preview. v0.4.0 is being developed on this branch and has not been published as a GitHub release.** The new menu-and-settings mode belongs to that development work; it is not included in the v0.3.0 downloads below.

Local component tests do not prove real-device compatibility. Native Windows game capture, Android permission/capture behavior, TCL HDMI overlays and PlayStation / Remote Play workflows have not been validated on physical devices. See [verification results](docs/TESTLER.md).

## Downloads

Use the [published v0.3.0 release](https://github.com/bladeagent37-commits/Ludil/releases/tag/v0.3.0) for the currently available preview.

| Platform | Published download | Package contents |
| --- | --- | --- |
| Windows x64 | [Windows preview v0.3.0](https://github.com/bladeagent37-commits/Ludil/releases/download/v0.3.0/Lodil-0.3.0-Windows-x64.zip) | Portable application, .NET runtime, OCR and translation models |
| Android / Android TV | [Android preview v0.3.0](https://github.com/bladeagent37-commits/Ludil/releases/download/v0.3.0/Lodil-0.3.0-Android.apk) | Signed APK with English OCR and translation models |
| Integrity checks | [SHA-256 checksums](https://github.com/bladeagent37-commits/Ludil/releases/download/v0.3.0/SHA256SUMS.txt) | Checksums for both published packages |

Existing download URLs are preserved for compatibility. New build artifacts use the official name **Ludil**. No v0.4.0 release download is advertised before publication.

## Features and availability

| Capability | Availability |
| --- | --- |
| OCR and offline English-to-Turkish translation of a selected subtitle region | Implemented in the v0.3.0 preview; physical-device validation remains pending |
| Windows game profiles, subtitle appearance and start/stop shortcuts | Implemented in v0.3.0 |
| Android screen capture with system consent and an overlay permission | Implemented in v0.3.0; device behavior remains unverified |
| Optional Windows-to-TV subtitle delivery over a paired local connection | Implemented in v0.3.0; C# sender / Java receiver interoperability tested locally |
| Translation of menu labels and settings, such as Settings, New Game and Continue | In development for v0.4.0; not part of the published v0.3.0 packages |
| A shared menu glossary, per-line translation fallback and a separate menu display region | In development for v0.4.0 |
| Automatic PlayStation connection or guaranteed HDMI capture | Not implemented / not guaranteed |

No account, advertising, subscription or paid translation API is required. The bundled local model currently supports **English → Turkish**, not arbitrary language pairs.

## System requirements

| Use | Requirements |
| --- | --- |
| Windows package | Windows 10/11, x64; a capturable desktop region; windowed or borderless games are recommended |
| Android package | Android 8.0 / API 26 or newer; ARM64, ARMv7 or x86_64; system screen-sharing and overlay permissions for the relevant mode |
| Android storage | At least 1 GB free space is recommended for installation and model extraction |
| PC-to-TV mode | Both devices on a trusted local network; an explicitly paired TV address and temporary code |
| Source development | .NET SDK 10.0.403, JDK 17, Android SDK 35 / Build Tools 35.0.0, Python 3.10+ for asset/test helpers |

The Windows package includes .NET; users do not need Python for normal application use. Systems missing the native OCR dependency may need the [official Microsoft Visual C++ x64 runtime](https://aka.ms/vs/17/release/vc_redist.x64.exe). The first translation can take longer while the local model is loaded. Performance on low-memory devices remains unverified.

## Install on Windows

1. Download the Windows ZIP from the published release.
2. Extract the **entire archive** into one folder; keep the executable, runtime and model files together.
3. Open the application executable included in that folder. Builds from the current development branch use `Ludil.exe`.
4. Run the sample-image translation check before selecting a game region.
5. Enable English subtitles in the game, select their screen region and start translation.

For a native OCR loading error, check the application's Help page and the runtime link above. The current Windows executable does not have an Authenticode publisher signature.

## Install on Android / Android TV

1. Download and transfer the APK to the device.
2. Open it through a file manager and allow that file manager to install the application if Android requests it.
3. Launch Ludil and try the included sample OCR / translation check.
4. Choose the input source, then grant screen-sharing and display-over-other-apps permissions when required by that mode.
5. Use Stop in the application or its notification to end the session.

The interface adapts to narrow and wide screens and provides D-pad focus styling. This behavior has not yet been verified on physical Android devices.

## Usage

### Subtitles

On Windows, select only the English subtitle area, then start translation. The overlay avoids the selected source region. Game profiles remember the region and appearance. On Android, the screen-reading mode processes the selected subtitle band of the system-approved shared image.

| Windows shortcut | Action |
| --- | --- |
| Ctrl+Shift+F8 | Start / stop translation |
| Ctrl+Shift+F9 | Hide / show captions |
| Ctrl+Shift+F10 | Select the source region again |

### Menu and settings text — v0.4.0 development

The development branch adds a dedicated menu mode. On Windows, choose the menu-and-settings text type and select the menu region. On Android, the new source option reads the left/center or right menu region and places translations on the opposite side. Known labels use the shared glossary; other short lines use the configured translation engine (local by default).

This mode is designed for up to 12 visible text lines and 600 characters per scan. It reads pixels and displays a translation panel; it does not modify game files, replace the game's UI or click translated buttons. Validation status and test evidence are recorded separately in the verification report. These instructions describe development behavior, not functionality available in the published v0.3.0 APK/ZIP.

### Optional PC-to-TV delivery

On the TV, start the subtitle receiver from the connection page. On Windows, open Help → connect to TV and enter the address and six-digit code shown by the TV. The code expires after five minutes and can be used once; the session token lasts until the receiver stops. Python is not required for this flow. Use matching development versions on both devices when testing the new menu mode.

### HDMI and PlayStation

Ludil is not a Remote Play client and does not request PSN credentials. A TV's HDMI input is not necessarily accessible through Android screen sharing. The HDMI text test displays fixed sample captions; it does not demonstrate HDMI capture or PlayStation translation. See the [setup guide](docs/KURULUM.md) for the current limitations.

## Build from source

```sh
python3 scripts/fetch-assets.py
./gradlew :app:assembleRelease :app:lintRelease
dotnet run --project desktop/Ludil.Smoke -- app/src/main/assets/translation
```

The asset helper downloads pinned ONNX models and Android libraries, then checks their sizes and SHA-256 hashes against [DEPENDENCIES.json](docs/DEPENDENCIES.json). Large ONNX/AAR files are not committed to Git. Published packages already include their required models.

On Windows, package the desktop application with:

```powershell
./desktop/publish-windows.ps1 -Output ./dist/Ludil-Windows-x64
```

Android release signing uses private environment variables and a keystore supplied outside the repository. The existing application identifier and signing identity are retained for update compatibility. Never commit a signing key or its password. See [development notes](docs/GELISTIRME.md).

## Local verification

```sh
python3 scripts/fetch-assets.py --check
dotnet run --project desktop/Ludil.Smoke -- app/src/main/assets/translation
python3 scripts/test-host.py
python3 scripts/test-video.py
python3 scripts/check-docs.py
```

The video suite additionally requires FFmpeg, Tesseract 5 CLI and Pillow (`python3 -m pip install Pillow`); it writes evidence to `qa/video/`.

The host suites exercise real C#/Java ONNX inference, subtitle filtering, session invalidation, settings recovery, pairing and the local HTTP protocol. The development work also adds menu regressions and synthetic-video OCR/translation tests; completed results are listed in [TESTLER.md](docs/TESTLER.md), with raw evidence under [docs/test-results](docs/test-results/).

Windows-native checks are available through `--ui-smoke <output-folder>`. The Linux visual harness checks production layout/theme sources with sample data and does not emulate Windows capture. Video tests use generated English text and synthetic backgrounds, not recordings of a real game or device. **GitHub Actions is not used: builds and verification run locally.**

## Known limitations

- OCR depends on font, size, contrast and a stable frame; stylized or rapidly changing text can be missed.
- Local machine translation can be inaccurate. Only English-to-Turkish translation is supported by the bundled model.
- Menu coverage is bounded by the selected region, the line/character limits and the glossary. Unknown short labels use model fallback.
- Protected content, some exclusive-fullscreen games and device power management can prevent or interrupt capture/overlays.
- Physical Windows/Android execution, TCL HDMI visibility and PlayStation / Remote Play workflows remain unverified.
- Component, visual and synthetic-video tests do not establish universal game or device compatibility.

## Privacy

Normal operation processes captured images in memory and does not save screenshots or translation history. Default translation runs locally. Saved preferences include game names, selected regions and appearance settings.

Optional TV delivery sends source/translated text to the paired local device. This LAN protocol uses HTTP, not TLS, and is intended for trusted networks. Session credentials are kept in memory. An optional user-configured local translation server receives English text; its address and entered key are stored in private Android preferences.

Developer test modes explicitly save synthetic frames, screenshots or reports to their chosen output folder. Model/dependency downloads contact their upstream hosts during source setup. See [privacy notes](docs/GIZLILIK.md).

## License and third-party components

Ludil project code is available under the [MIT License](LICENSE). OCR libraries, ONNX Runtime, .NET and translation models retain their own licenses and notices; the project's MIT license does not replace them. See [LICENSES.md](LICENSES.md), [model provenance](docs/DEPENDENCIES.json), [shared dependency notices](docs/licenses/) and [Windows notices](desktop/licenses/).

## Contributing

Bug reports, reproducible tests, accessibility, performance and documentation improvements are welcome. Include the version, operating system, input mode, expected behavior and relevant local test results in an issue or pull request. Describe which behaviors were tested on a physical device and which were not.

Run the relevant local checks before submitting changes. Keep English and Turkish README content aligned, preserve existing release URLs, and keep signing credentials, personal screenshots and generated packages out of Git. Do not add or trigger GitHub Actions workflows. See [CONTRIBUTING.md](CONTRIBUTING.md).

## Project evidence

Published artifacts, pinned dependency hashes, source code and committed test reports provide the verifiable project record. Roadmap items, unreleased work and local-only tests are labeled as such.
