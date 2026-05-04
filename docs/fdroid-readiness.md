# F-Droid Readiness

This file tracks the repository work needed before NexNote moves from private GitHub development to IzzyOnDroid and then F-Droid official.

## Current Repository Status

- [x] Single Android app module.
- [x] Kotlin DSL Gradle setup.
- [x] Version Catalog in `gradle/libs.versions.toml`.
- [x] `RepositoriesMode.FAIL_ON_PROJECT_REPOS`.
- [x] Stable non-example release application id: `io.github.r0x4nk.nexnote`.
- [x] Debug application id suffix: `.debug`.
- [x] GPL-3.0-only license.
- [x] No Firebase, Google Play Services, analytics SDK, or proprietary runtime service detected.
- [x] Fastlane metadata directory.
- [x] GitHub Actions build workflow.
- [x] Tag-based release workflow.
- [x] Issue templates and pull request template.
- [x] Privacy, security, and contributing docs.

## Before First Public Release

- [ ] Create one stable production signing key and keep it backed up.
- [ ] Configure GitHub Secrets:
  - `KEYSTORE_FILE`
  - `KEYSTORE_PASSWORD`
  - `KEY_ALIAS`
  - `KEY_PASSWORD`
- [ ] Publish the first signed GitHub release from tag `v1.0.0`.
- [ ] Add the release certificate SHA-256 fingerprint to `signature/README.md`.
- [ ] Verify a clean clone build:

```bash
./gradlew clean assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
```

- [ ] Confirm `fastlane/metadata/android/en-US/images/phoneScreenshots/` contains real current screenshots.
- [ ] Confirm `fastlane/metadata/android/en-US/changelogs/1.txt` matches versionCode `1`.
- [ ] Confirm no secrets exist in git history before the repository becomes public.

## Future F-Droid Data Template

When the repository is public and the first signed release exists, create a metadata file in `fdroiddata` similar to:

```yaml
Categories:
  - Writing
License: GPL-3.0-only
AuthorName: R0X4N-K
SourceCode: https://github.com/R0X4N-K/NexNote
IssueTracker: https://github.com/R0X4N-K/NexNote/issues

AutoName: NexNote

RepoType: git
Repo: https://github.com/R0X4N-K/NexNote.git

Builds:
  - versionName: 1.0.0
    versionCode: 1
    commit: v1.0.0
    subdir: app
    gradle:
      - yes

AutoUpdateMode: Version
UpdateCheckMode: Tags
CurrentVersion: 1.0.0
CurrentVersionCode: 1
```

Review this against the current F-Droid metadata reference before opening the real PR.
