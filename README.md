# NexNote

[![Build](https://github.com/R0X4N-K/NexNote/actions/workflows/build.yml/badge.svg)](https://github.com/R0X4N-K/NexNote/actions/workflows/build.yml)
[![Release](https://img.shields.io/github/v/release/R0X4N-K/NexNote)](https://github.com/R0X4N-K/NexNote/releases/latest)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

NexNote is an Android app for keeping notes on your device. It works without an
account, a server, or an internet connection. It has no ads or analytics.

This README describes NexNote 1.0.4. See the
[changelog](CHANGELOG.md#104---2026-09-18) for the changes in this release.

## What you can do

- Write Markdown notes with a preview, checklists, tables, images, and file attachments.
- Find notes with full-text search, tags, pin filters, and sorting.
- Browse notes in Calendar and Agenda, reuse templates, and view writing statistics.
- Select notes for bulk actions, or recover deleted notes from the trash.
- Keep selected notes and their files in a PIN-protected Vault.
- Receive text and images from other apps, or open supported text files as notes.
- Export to text, Markdown, or PDF; print notes or share document attachments in a ZIP.
- Choose light, dark, OLED, or system appearance, nine accent colors, Android
  device colors, and one of five typefaces, and adjust the text size.

NexNote requires Android 10 (API 29) or later. Device colors require Android 12.
Ordinary notes use Android's private app storage; Vault content has additional
PIN-based encryption. Automatic Android backup and device transfer are disabled.
Export anything you want to keep before uninstalling or changing devices.
See [Vault and backup](docs/vault-and-backup.md) for details.

## Build and test

You need a complete JDK 21 and the Android SDK platform 36.1. Android Studio can
install the SDK components requested by Gradle. See [JDK setup](docs/build-jdk.md)
if Gradle uses the wrong Java installation.

From the repository root:

```bash
./gradlew assembleDebug
./gradlew ci
```

On Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat ci
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.
The `ci` task runs local tests, compiles Android tests, checks lint, and builds
debug and release APKs. To run the Android tests, use a disposable emulator
and follow [Running tests](docs/testing.md).

`assembleRelease` creates an unsigned, optimized APK. It does not use signing
secrets or publish a release. Dependencies are locked and checked against
`gradle/verification-metadata.xml`; an offline build needs a populated cache.

## Project layout

The app uses Kotlin, Compose Material 3, Room, DataStore, Coroutines, and Flow
in one Android module.

| Path | Contents |
| --- | --- |
| `app/src/` | Application code, resources, and tests |
| `app/schemas/` | Room schemas used by migration tests |
| `gradle/libs.versions.toml` | Dependency and plugin versions |
| `.github/workflows/` | Build and signed-release workflows |
| `fastlane/metadata/android/` | Store descriptions, release notes, and artwork |
| `docs/` | User and contributor guides |
| `artwork/` | Logo sources and exports |

The release package is `io.github.r0x4nk.nexnote`; debug adds `.debug`.
The source version is `1.0.4` / code `36`, released from tag `v1.0.4`.

## Releases and F-Droid

The [release workflow](.github/workflows/release.yml) builds and signs a universal
APK when a `v*` tag is pushed. Read [Contributing](CONTRIBUTING.md#releases) before
creating a release tag: its version must match the build configuration.

The F-Droid submission is tracked in
[merge request !46620](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/46620).
The submission remains under review. The local metadata targets 1.0.4;
F-Droid publication and reproducibility require verification for that release.
Store screenshots still need to be refreshed for the redesigned interface.
See the [build guide](docs/fdroid-build-supply-chain.md),
[historical submission record](docs/fdroid-readiness.md), and
[signing certificate](signature/README.md).

## Documentation

- [Attachments, imports, and export](docs/attachments.md)
- [Vault and backup](docs/vault-and-backup.md)
- [Architecture](docs/architecture.md)
- [Colors and typography](docs/color-design-system.md)
- [Running tests](docs/testing.md)
- [Asset sources and licenses](docs/fdroid-asset-inventory.md)
- [Privacy](PRIVACY-POLICY.md) and [security reports](SECURITY.md)

Contributions are welcome; see [Contributing](CONTRIBUTING.md).
NexNote is licensed under [GPL-3.0-only](LICENSE). Third-party components and
fonts retain the licenses listed in [Third-party notices](THIRD_PARTY_NOTICES.md).
