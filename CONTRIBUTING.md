# Contributing to NexNote

Bug reports, fixes, translations, and documentation improvements are welcome.
Keep each change focused so it is easy to review.

## Getting started

1. Clone the repository and install Android Studio or the Android SDK platform 36.1.
2. Select a [complete JDK 21](docs/build-jdk.md).
3. Run `./gradlew assembleDebug` and `./gradlew ci`.

On Windows, use `.\gradlew.bat` instead of `./gradlew`.
See [Running tests](docs/testing.md) for device tests and targeted checks.

## Working on the app

Follow the package boundaries described in [Architecture](docs/architecture.md).
Reuse the existing UI components and [theme roles](docs/color-design-system.md)
where possible. Put user-facing text in string or plural resources and update
the existing translations.

Keep dependencies at fixed versions and preserve checksum verification. When
changing dependencies, review the lockfiles and third-party notices too; the
[build guide](docs/fdroid-build-supply-chain.md) describes the process.

The app keeps user data local. Do not introduce accounts, analytics, advertising,
or proprietary runtime services without first discussing a change to that design.
Never commit signing keys, credentials, local machine settings, APKs, or build output.

## Pull requests

Explain the problem, the change, and how you checked it. For code changes, run
`./gradlew ci` and any device tests relevant to the behavior you changed. For
documentation-only edits, check facts, links, and `git diff --check`; a full
Android build is usually unnecessary.

Include screenshots for visible UI changes where they help the review. Update
the unreleased changelog and documentation when behavior changes. If a check
could not run, say which one and why.

## Releases

1. Choose a new `versionName` and increase `baseVersionCode` in
   `app/build.gradle.kts`. Existing releases must keep their original tags.
2. Move the relevant entries out of `Unreleased` in `CHANGELOG.md` and add
   `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`.
   Update the store description and screenshots to match the release.
3. Validate the release commit from a clean checkout with `./gradlew ci` and
   the Android tests. Check migrations and upgrades from the previous release.
4. Verify that the universal release APK includes `armeabi-v7a`, `arm64-v8a`,
   `x86`, and `x86_64`. The workflows require an R8 mapping file and an
   unsigned APK smaller than 15 MiB.
5. Create and push a `v<versionName>` tag pointing to that commit.

Pushing the tag starts the release workflow, which signs the APK and publishes a
GitHub release. It requires these repository secrets:

- `KEYSTORE_FILE`: the production keystore encoded as base64.
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

Keep the established [signing identity](signature/README.md). The release workflow
checks the version, ABIs, size, and alignment; it does not run the full test suite,
so complete the checks before tagging. Verify the F-Droid recipe and reproducible
build result separately for each release.
