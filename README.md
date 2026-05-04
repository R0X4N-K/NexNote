# NexNote

[![Build](https://github.com/R0X4N-K/NexNote/actions/workflows/build.yml/badge.svg)](https://github.com/R0X4N-K/NexNote/actions/workflows/build.yml)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

NexNote is a local-first Android note-taking app built with Kotlin, Jetpack Compose, Material 3, Room, DataStore, Coroutines, and Flow.

The project is prepared as a single-module Android repository for solo development, with GitHub Actions, F-Droid metadata, GPL-3.0-only licensing, privacy documentation, contribution templates, and release-signing guidance.

## Features

- Local notes stored on device with Room.
- Markdown-oriented editing and preview.
- Tags, note search, agenda view, templates, and trash.
- PDF/export flow with Android FileProvider sharing.
- Theme, accent color, font scale, timezone, and left-handed preferences.
- No account, analytics, Firebase, Google Play Services, or remote backend.

## Android Package

- Release application id: `io.github.r0x4nk.nexnote`
- Debug application id: `io.github.r0x4nk.nexnote.debug`
- Current version: `1.0.0` / versionCode `1`

The Kotlin namespace and release application id are both `io.github.r0x4nk.nexnote`, with the debug variant using `io.github.r0x4nk.nexnote.debug`.

## Project Shape

```text
NexNote/
|-- app/                         Android app module
|-- gradle/libs.versions.toml     Version Catalog
|-- fastlane/metadata/android/    Store metadata for F-Droid/IzzyOnDroid
|-- .github/workflows/            CI and release workflows
|-- docs/                         Maintainer notes and F-Droid checklist
|-- signature/                    Release signing notes
```

The app intentionally stays single-module. That matches the guide for a solo note-taking app and keeps build, review, and maintenance overhead low.

## Build

Prerequisites:

- Android Studio or Android SDK.
- JDK 21, matching `gradle/gradle-daemon-jvm.properties`.

Useful commands:

```bash
./gradlew clean assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
```

On Windows PowerShell:

```powershell
.\gradlew.bat clean assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
```

## Release

Releases are tag-driven. Push a semantic tag such as:

```bash
git tag v1.0.0
git push origin v1.0.0
```

The release workflow expects these GitHub Secrets:

- `KEYSTORE_FILE`: base64-encoded production keystore.
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

Keep the production signing key stable forever once the app is distributed. Never commit keystores or signing property files.

## F-Droid Readiness

This repository includes:

- GPL-3.0-only license.
- F-Droid/IzzyOnDroid Fastlane metadata in `fastlane/metadata/android/en-US/`.
- Version Catalog based Gradle setup.
- No proprietary runtime services.
- GitHub CI for build, tests, and lint.
- Tag-based release workflow.

Before submitting to F-Droid official, follow `docs/fdroid-readiness.md`, verify a clean clone build, publish a signed release, and open the future metadata PR in `fdroiddata`.

## Privacy

NexNote is offline-first. See `PRIVACY-POLICY.md` for the current privacy statement.

## Contributing

See `CONTRIBUTING.md`.

## License

NexNote is licensed under the GNU General Public License v3.0 only. See `LICENSE`.
