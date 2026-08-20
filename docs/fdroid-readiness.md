# F-Droid preparation status

NexNote is not currently published on F-Droid. This document describes the
repository preparation for a possible future submission; it is not an F-Droid
listing, approval, or release announcement.

## Repository preparation

The repository provides:

- GPL-3.0-only project licensing and a third-party license inventory;
- Fastlane metadata under `fastlane/metadata/android/en-US/`;
- original, licensed launcher artwork and store assets;
- a source-build template in `docs/fdroid-submission-template.yml`;
- Gradle dependency locking and strict SHA-256 artifact verification;
- a Gradle wrapper with a pinned distribution checksum;
- build, unit-test, Android-test compilation, and lint tasks;
- no proprietary runtime services, account requirement, analytics, or ads.

The submission template intentionally contains
`REQUIRED_FULL_PUBLIC_RELEASE_COMMIT_SHA`. It must not be copied into
`fdroiddata` until that placeholder refers to the immutable public source ref
selected for the release.

## Build profile

| Property | Value |
|---|---|
| Application id | `io.github.r0x4nk.nexnote` |
| Version | `1.0.0` (`versionCode` 1) |
| Android SDK | minSdk 29, targetSdk 36 |
| Build tools | Gradle 9.3.1, Android Gradle Plugin 9.1.1, JDK 21 |
| Dependency repositories | Google Maven and Maven Central |
| Release output | unsigned APK for distributor signing |
| Android backup and device transfer | disabled |

The build packages `LICENSE` and `THIRD_PARTY_NOTICES.md` under
`assets/legal/`. These generated text assets are normalized to LF line endings
so their packaged bytes do not depend on the checkout platform.

## Database compatibility

Room migrations 5→6 and 6→7 are part of the supported upgrade path and are
covered individually and as the 5→7 chain. Database schemas earlier than 5 are
not supported because authentic schema snapshots are unavailable. NexNote does
not enable destructive migration fallback for those versions.

## Release validation

Before selecting a source ref for submission, run the project gates from a
clean checkout with Android SDK API 36 and JDK 21:

```bash
./gradlew clean
./gradlew ci
```

Instrumentation tests should also be run on a compatible device or emulator.
The release APK is expected to remain unsigned; F-Droid signs the artifact it
builds from source.

## Submission steps

Before a future F-Droid submission:

1. select an immutable public commit or semantic release tag for the intended
   version;
2. replace the source-ref placeholder in the metadata template with the full
   commit SHA;
3. reproduce and inspect the release build from that exact ref;
4. submit the completed metadata through the official `fdroiddata` process.

Until those steps are completed and accepted by F-Droid, NexNote must not be
described as available from or published on F-Droid.
