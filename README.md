# NexNote

[![Build](https://github.com/R0X4N-K/NexNote/actions/workflows/build.yml/badge.svg)](https://github.com/R0X4N-K/NexNote/actions/workflows/build.yml)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

NexNote is an offline, local-first Android note-taking app built with Kotlin,
Jetpack Compose, Material 3, Room, DataStore, Coroutines, and Flow.

## Features

- Local notes stored on device with Room.
- Markdown-oriented editing and preview.
- Tags, note search, agenda view, templates, and trash.
- An encrypted Vault for selected notes and their images.
- PDF/export flow with Android FileProvider sharing.
- Theme, accent color, font scale, timezone, and left-handed preferences.
- No account, analytics, Firebase, Google Play Services, or remote backend.

Ordinary notes are protected by Android's private app sandbox. Vault note fields
and images are additionally encrypted at rest with a PIN-derived key. Android
backup and device-to-device transfer are disabled, so users must export data they
want to keep before uninstalling or moving devices. See
[`docs/vault-and-backup.md`](docs/vault-and-backup.md) for the exact guarantees
and limitations.

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
|-- docs/                         Project and distribution documentation
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
./gradlew assembleRelease
./gradlew testDebugUnitTest
./gradlew compileDebugAndroidTestKotlin
./gradlew lintDebug lintRelease
./gradlew lintRelease --offline
```

On Windows PowerShell:

```powershell
.\gradlew.bat clean assembleDebug
.\gradlew.bat assembleRelease
.\gradlew.bat testDebugUnitTest
.\gradlew.bat compileDebugAndroidTestKotlin
.\gradlew.bat lintDebug lintRelease
.\gradlew.bat lintRelease --offline
```

The Android-test command compiles instrumentation tests; running them requires a
connected device or AVD. Dependency lock state and strict checksum verification
are committed in `app/gradle.lockfile`, `settings-gradle.lockfile`, and
`gradle/verification-metadata.xml`. Distribution license material is generated
into each APK under `assets/legal/` from `LICENSE` and
`THIRD_PARTY_NOTICES.md`.

## Release

The release workflow is designed to run from a semantic tag such as `v1.0.0`.
Creating or pushing that tag is a maintainer action and is not part of a local
build:

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

## F-Droid Status

NexNote is not currently published on F-Droid. The repository includes the
source-side material intended to support a future submission:

- GPL-3.0-only license;
- localized Fastlane metadata in `fastlane/metadata/android/en-US/`;
- a source-build metadata template in `docs/fdroid-submission-template.yml`;
- Gradle dependency locking and strict artifact checksum verification;
- no proprietary runtime services;
- GitHub CI for build, tests, and lint;
- a tag-triggered release workflow.

The metadata template is not a completed F-Droid submission. Before submitting
a release, the maintainer must select an immutable public commit or tag, replace
the release-reference placeholder, and validate a clean source build from that
exact ref. See [`docs/fdroid-readiness.md`](docs/fdroid-readiness.md) for the
repository preparation status and remaining submission steps.

## Privacy

NexNote is offline-first. See `PRIVACY-POLICY.md` for the current privacy statement.

## Contributing

See `CONTRIBUTING.md`.

## License

NexNote is licensed under the GNU General Public License v3.0 only. See `LICENSE`.
