# NexNote

[![Build](https://github.com/R0X4N-K/NexNote/actions/workflows/build.yml/badge.svg)](https://github.com/R0X4N-K/NexNote/actions/workflows/build.yml)
[![Release](https://img.shields.io/github/v/release/R0X4N-K/NexNote)](https://github.com/R0X4N-K/NexNote/releases/latest)
[![F-Droid submission](https://img.shields.io/badge/F--Droid-submission%20under%20review-orange)](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/46620)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

NexNote is an offline, local-first Android note-taking app built with Kotlin,
Jetpack Compose, Material 3, Room, DataStore, Coroutines, and Flow.

## Features

- Local notes stored on device with Room.
- Markdown-oriented editing and preview.
- Full-text search with scope, pin, and ordering controls.
- Tags, agenda view, reusable templates, and trash.
- On-device writing statistics with yearly activity and streak insights.
- An encrypted Vault for selected notes and their images.
- PDF/export flow with Android FileProvider sharing.
- Theme, accent color, font scale, timezone, and left-handed preferences.
- Select-all and bulk data-management actions, including deleting all notes.
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
- Current source version: `1.0.3` / versionCode `35`

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
./gradlew ci
./gradlew clean assembleDebug
./gradlew assembleRelease
./gradlew compileDebugAndroidTestKotlin
./gradlew lintRelease --offline
```

On Windows PowerShell:

```powershell
.\gradlew.bat ci
.\gradlew.bat clean assembleDebug
.\gradlew.bat assembleRelease
.\gradlew.bat compileDebugAndroidTestKotlin
.\gradlew.bat lintRelease --offline
```

The `ci` task is the canonical validation gate. It runs all configured local
unit tests, compiles instrumentation tests, runs debug and release lint, and
assembles both APK variants.

The Android-test command compiles instrumentation tests; running them requires a
connected device or AVD. Dependency lock state and strict checksum verification
are committed in `app/gradle.lockfile`, `settings-gradle.lockfile`, and
`gradle/verification-metadata.xml`. Distribution license material is generated
into each APK under `assets/legal/` from `LICENSE` and
`THIRD_PARTY_NOTICES.md`.

## Release

Signed upstream releases are built from immutable tags by the tag-triggered
GitHub Actions release workflow. Each release publishes one optimized universal
APK containing native libraries for `armeabi-v7a`, `arm64-v8a`, `x86`, and
`x86_64`. Releases use this semantic-tag process:

```bash
git tag -a v1.1.0 -m "NexNote 1.1.0"
git push origin v1.1.0
```

The release workflow expects these GitHub Secrets:

- `KEYSTORE_FILE`: base64-encoded production keystore.
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

Keep the production signing key stable forever once the app is distributed. Never commit keystores or signing property files.

## F-Droid Status

NexNote has been submitted to the official F-Droid repository and is currently
awaiting maintainer review in
[`fdroiddata` merge request !46620](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/46620).
Each metadata revision is validated by the F-Droid merge-request pipeline,
including its source build and APK checks.

The submission includes:

- GPL-3.0-only license;
- localized Fastlane metadata in `fastlane/metadata/android/en-US/`;
- the reviewed metadata snapshot in `docs/fdroid-submission-template.yml`;
- Gradle dependency locking and strict artifact checksum verification;
- no proprietary runtime services;
- GitHub CI for build, tests, and lint;
- R8-minified, resource-shrunk universal release builds;
- a release workflow that publishes the matching upstream reference APK;
- F-Droid reproducible-build verification pinned to the upstream signing
  certificate.

NexNote is **not yet available in the F-Droid catalog**. Publication occurs only
after F-Droid maintainers accept and merge the submission and the package is
included in an official repository index. See
[`docs/fdroid-readiness.md`](docs/fdroid-readiness.md) for the complete audit,
submission evidence, and post-acceptance checks.

The GitHub release and the future F-Droid package are separate distribution
channels, but both use the same upstream production signing identity. F-Droid
rebuilds the app from source, verifies that the upstream signature can be
transferred to its byte-equivalent build, and publishes the verified
upstream-signed APK.

## Privacy

NexNote is offline-first. See `PRIVACY-POLICY.md` for the current privacy statement.

## Contributing

See `CONTRIBUTING.md`.

## License

NexNote is licensed under the GNU General Public License v3.0 only. See `LICENSE`.
