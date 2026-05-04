# Contributing to NexNote

Thanks for helping keep NexNote small, useful, and F-Droid friendly.

## Development Setup

1. Install Android Studio or the Android SDK.
2. Use JDK 21.
3. Clone the repository.
4. Run:

```bash
./gradlew clean assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
```

On Windows:

```powershell
.\gradlew.bat clean assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
```

## Project Rules

- Keep the app single-module unless there is a real, repeated boundary that justifies a new module.
- Prefer existing package patterns under `data`, `domain`, `ui`, `di`, and `util`.
- Do not add Firebase, Google Play Services, analytics SDKs, crash-reporting SDKs, or proprietary runtime services.
- Do not commit keystores, passwords, tokens, API keys, local properties, APKs, AABs, or build output.
- Use fixed dependency versions in `gradle/libs.versions.toml`; do not use `+` or snapshots.
- Keep user data local unless a future feature is explicitly designed and documented around sync.

## Branches

Use short branch names:

- `feature/note-links`
- `fix/editor-save`
- `docs/fdroid`
- `test/repository`

## Pull Request Checklist

- [ ] The change is scoped and does not include unrelated refactors.
- [ ] `./gradlew testDebugUnitTest` passes.
- [ ] `./gradlew lintDebug` passes or any lint issue is explained.
- [ ] F-Droid compatibility is preserved.
- [ ] Documentation or metadata is updated when user-facing behavior changes.
- [ ] No secrets, signing files, generated APKs, or local IDE state are included.

## Release Checklist

- Update `CHANGELOG.md`.
- Update `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`.
- Ensure `versionName` and the git tag match, for example `1.0.0` and `v1.0.0`.
- Ensure `versionCode` only increases.
- Build from a clean clone before tagging.
