# Ludil v0.4.0 development — local verification

Run date: **1 November 2026**. All checks below ran locally on Linux. No GitHub Actions workflow was added or run. **v0.4.0 has not been published as a GitHub release**; the published preview remains [v0.3.0](https://github.com/bladeagent37-commits/Ludil/releases/tag/v0.3.0).

These results concern the current development source and locally prepared packages. They establish the specific component/build properties below. They do not establish physical-device or universal game compatibility.

| Area | Result | Evidence and scope |
| --- | --- | --- |
| Windows x64 Release publish | Passed | .NET SDK 10.0.403, self-contained win-x64 build, locked package restore; [publish output](test-results/v0.4.0/windows-publish.log). No native Windows execution. |
| C# menu regressions | 39 checks passed | Shared glossary, label/value combinations, key hints, bounded fallback, two-letter values, panel geometry, profile persistence and contrast processing; [core log](test-results/v0.4.0/core-tests.log). |
| Existing C# deep regressions | 74 checks passed | Real ONNX inference, Unicode and input limits, invalid confidence, subtitle disappearance/reappearance, cache/cancellation, settings recovery, profile limits and TV address validation. |
| Session concurrency | Passed | 10,000 concurrent invalidations; only the latest session ticket remains valid. |
| Java menu regressions | 35 checks passed | The same independent glossary cases, values, line limits, capture/panel geometry and contrast checks; [Java log](test-results/v0.4.0/java-tests.log). |
| Existing Java engine/filter | Passed; 38 gate/geometry checks | Real host ONNX inference and English-to-Turkish output; subtitle confidence, repeated scenes and overlay exclusion. This runs on a JDK, not Android. |
| Java LAN receiver | Passed | Pairing, authentication, code reuse rejection, Origin rejection, UTF-8, ordered messages, TTL bounds, replay, empty/oversized input and clear. |
| Receiver lifecycle | Passed | 12 consecutive close/restart cycles reuse the listening port. |
| C# → Java interoperability | Passed | Production C# TV client with the real Java receiver: pairing, 16 concurrent sends, ordered IDs, Turkish text and clear. |
| Synthetic-video OCR/translation | 3 clips, 72 decoded samples, 18 assertions passed | Compressed English subtitle/menu/transition clips, production C# menu contrast processing, actual Tesseract 5.3.4 OCR, glossary/ONNX translation and subtitle stabilization; [report](test-results/v0.4.0/video/video-report.json). |
| Windows visual layout | Passed | Unchanged production theme/layout sources compiled with a test harness on Linux Mono; four pages at 1040×750 and 900×650 plus sample subtitle/menu views. Bounds and fixed-label text clipping checked; [result](test-results/v0.4.0/visual-layout.txt). |
| Android Release build and lint | Passed; 0 errors, 3 warnings | [Build log](test-results/v0.4.0/android-build.log), [lint XML](test-results/v0.4.0/android-lint.xml), [lint explanation](test-results/v0.4.0/android-lint.txt). HTTP is used by the optional LAN protocol; physical LEFT/RIGHT menu placement produces two RTL warnings. Those directions intentionally track physical capture regions. |
| Android package and signature | Passed | Display name Ludil, versionName 0.4.0, versionCode 4, minSdk 26, three ABIs, package identity retained for updates. APK v2 signature, RSA 4096-bit, same public certificate digest as v0.3.0; [package metadata](test-results/v0.4.0/android-package.txt), [signature output](test-results/v0.4.0/android-signature.txt). |
| Pinned model/dependency integrity | Passed | SHA-256 and size checks for all six entries in [DEPENDENCIES.json](DEPENDENCIES.json); `python3 scripts/fetch-assets.py --check`. |
| Final local packages | Passed | CRC checks, exact model payload and glossary, three Android ABIs, no private signing key in the APK; Windows PE/runtime/OCR/model/license contents and complete SHA-256 manifest coverage of 300 files; [package report](test-results/v0.4.0/package-verification.json), [artifact hashes](test-results/v0.4.0/SHA256SUMS.txt). |
| Bilingual README checks | Passed | Top language navigation, local targets, equal link/command sets, version/requirements/shortcut consistency, displayed Ludil name, no workflows. All advertised downloads match the actual published release [metadata snapshot](test-results/v0.4.0/published-releases.json). |

## Reproduce the checks

See [development instructions](GELISTIRME.md) for SDK versions, asset preparation and commands. Run `python3 scripts/check-docs.py` after documentation changes. The optional `--release-metadata` argument accepts the committed metadata snapshot to check advertised assets against that observed release state.

The video suite requires FFmpeg, Tesseract 5 CLI, Pillow and .NET. `python3 scripts/test-video.py` generates the inputs, decodes them at two frames per second, records OCR output, runs the production C# core and checks outputs. A menu sentence absent from the glossary exercises real ONNX fallback. Empty scenes and the same subtitle returning after a gap are included. Selected colored menu rows and two-letter On/Off values exposed the contrast issue corrected during this work.

## Inspect the synthetic evidence

All six clips below were generated locally from text and a moving synthetic background. They contain no game footage. The translated clips visualize production core outputs with a test renderer; they are not recordings of the native Windows or Android overlay.

| Scenario | Input | Annotated component output |
| --- | --- | --- |
| Menu labels and setting values | [English menu clip](test-results/v0.4.0/video/menus-input.mp4) | [Translated menu clip](test-results/v0.4.0/video/menus-translated.mp4) |
| Subtitles and reappearance | [English subtitle clip](test-results/v0.4.0/video/subtitles-input.mp4) | [Translated subtitle clip](test-results/v0.4.0/video/subtitles-translated.mp4) |
| On/Off, controls and empty frames | [English transition clip](test-results/v0.4.0/video/transitions-input.mp4) | [Translated transition clip](test-results/v0.4.0/video/transitions-translated.mp4) |

[OCR records](test-results/v0.4.0/video/ocr-input.json) and [production translations](test-results/v0.4.0/video/translations.json) accompany the report; its SHA-256 hashes identify the exact clips. [Menu UI sample](images/windows-menu-preview.png) and [subtitle UI sample](images/windows-onizleme.png) are Linux Mono layout renders with sample data.

## Still unverified on physical devices

- Native Windows startup, Tesseract DLL loading, desktop/game capture, hotkeys and overlay visibility; the Linux visual harness does not verify these. The local Windows package includes `Ludil-Test.cmd` / `--ui-smoke` for this work.
- Android startup, system/overlay/notification permissions, MediaProjection lifecycle, actual menu capture and overlay positioning.
- Real TCL HDMI visibility, PlayStation output and Remote Play workflows.
- Long-running device performance, low-memory behavior and manufacturer power management.

The Windows executable has no Authenticode publisher signature. Locally prepared v0.4.0 artifacts remain development previews pending those checks and a release decision.

Android public signing-certificate SHA-256 (unchanged):

```text
af4abcea99afa1c357e0179514b0a30341ee93cce150f16de46f8e6310212d03
```

Historical [v0.3.0 test evidence](test-results/v0.3.0/) is preserved separately. It is not relabeled as a v0.4.0 result.
