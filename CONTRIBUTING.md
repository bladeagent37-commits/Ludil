# Contributing to Ludil

Contributions to Ludil are provided under the project's [MIT License](LICENSE). Bug fixes, reproducible tests, accessibility, performance and documentation improvements are welcome.

1. Fetch the pinned local assets with `python3 scripts/fetch-assets.py`; verify existing files with `--check`.
2. Run the [local component checks](README.md#local-verification) relevant to your change. Report the operating system, test commands and results.
3. Clearly distinguish physical-device validation from host component tests, synthetic-video tests and the Linux layout harness.
4. Keep [README.md](README.md) and [README.tr.md](README.tr.md) aligned, including release status, requirements, download links and limitations.
5. Explain the problem, resulting behavior and validation in your pull request. Preserve existing release URLs and update compatibility identifiers deliberately.

Do not add or trigger GitHub Actions workflows. Builds and tests run locally. Keep APKs, Windows distribution packages, large ONNX/AAR files, signing credentials, server keys and personal screenshots out of Git. Generated synthetic test evidence may be committed when it is small and clearly labeled.

For OCR or translation issues, include a short shareable English sample, expected Turkish meaning, version, platform and selected reading mode. Use only images or video that you are authorized to share. Do not include temporary pairing tokens or credentials.
