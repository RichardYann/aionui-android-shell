# AionUi Shell (Android)

Lightweight Android WebView shell for AionUi WebUI.

## What it does

- First run: enter AionUi WebUI URL (http/https) and open it in a WebView
- Later runs: auto-open the last saved URL
- Web navigation: back/refresh/change server + copy/open-in-browser
- Upload: supports `<input type="file">`
- Download: uses system DownloadManager (Downloads folder)

## Open / Build

This folder contains the Android app source code. The easiest way to build is:

1. Android Studio → Open… → select `android-shell/`
2. Let Android Studio sync Gradle (it may create/update Gradle wrapper files locally)
3. Run on device/emulator

## CI build (GitHub Actions)

This repo includes a GitHub Actions workflow that builds a debug-signed APK and uploads it as an artifact:

- Workflow: `.github/workflows/android-shell-build.yml`
- Artifact: `app-debug-apk` → `app-debug.apk`

## APK outputs (when built locally)

- Debug: `android-shell/app/build/outputs/apk/debug/app-debug.apk`
- Release (unsigned by default): `android-shell/app/build/outputs/apk/release/app-release.apk`

For a proper release APK install, configure signing in Android Studio (keystore should NOT be committed).
