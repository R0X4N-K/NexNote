# F-Droid release readiness audit

Audit date: 2026-08-22

Submission status updated: 2026-08-23

This document records the source-side audit of NexNote 1.0.0 (`versionCode` 1).
It is not an F-Droid approval or a claim that the app is already published.
Only F-Droid maintainers can accept the final `fdroiddata` merge request.

## Verdict

NexNote has been submitted for official F-Droid review. The immutable public
[`v1.0.0`](https://github.com/R0X4N-K/NexNote/releases/tag/v1.0.0) tag resolves to
commit `b26bbca1792c3d86241e80ecbb6db1d1de08baf9`. The metadata is under review in
[`fdroiddata` merge request !46620](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/46620).

Both the pre-submission branch pipeline and the merge-request pipeline passed
all nine jobs, including `fdroid build` and `check apk`. This is not an F-Droid
approval or a claim that the app is already published. Do not describe NexNote
as available on F-Droid until maintainers merge the request and the package
appears in the official repository index.

## Audited repository state

The review covered every file intended for the final Git index, including
application and test source, Gradle configuration, Room schemas, workflows,
documentation, Fastlane metadata, vector artwork, and PNG assets.

The following repository hygiene checks passed:

- no password, token, keystore, signing property, private key, or production
  credential is tracked;
- no personal filesystem path, private email address, or personal note content
  is present in publishable files;
- the only email-like test value is the synthetic `person@example.com` URI;
- local Android SDK, IDE, Gradle, Kotlin, temporary, and `.claude` state is
  ignored by Git;
- local task prompts and remediation reminders were removed from the workspace;
- placeholder Android Studio tests and obsolete process comments were removed;
- comments retained in source explain contracts, security boundaries,
  concurrency, migrations, parser behavior, or non-obvious UI invariants;
- all project-authored comments and documentation intended for publication are
  in English;
- PNG EXIF, textual, timestamp, and compressed-text metadata is absent;
- screenshots contain only synthetic project content and no personal data.

Generated build directories and local configuration are outside the submission
scope. F-Droid builds from the committed public source ref, not from a local
working directory.

## Policy mapping

| Requirement | Result | Evidence |
|---|---|---|
| FLOSS application license | Pass | Project is `GPL-3.0-only`; `LICENSE` is tracked. |
| FLOSS dependencies and toolchain | Pass | Runtime dependencies are AndroidX/Kotlin FLOSS components; plugins and artifacts resolve only from Google Maven and Maven Central; F-Droid must provide OpenJDK 21. |
| Public source | Pass | The repository and `main` ref are anonymously accessible with Git credentials disabled. GitHub also labels the signed-out repository view as public. |
| Unique application id | Pass | Release id is `io.github.r0x4nk.nexnote`; no conflicting indexed package was found during the audit. Final acceptance remains F-Droid's decision. |
| No proprietary runtime service | Pass | No Firebase, Google Play Services, proprietary analytics, ads, crash reporting, account, or remote backend. |
| No network tracking | Pass | The release manifest does not request `android.permission.INTERNET`; the only permission in the built APK is AndroidX's package-scoped dynamic-receiver permission. |
| Source-buildable release | Pass locally | Flavorless `assembleRelease` succeeds offline and produces an unsigned APK without signing secrets. |
| Reproducible build controls | Pass locally | Two clean, offline, cache-free release builds were byte-identical. This does not replace F-Droid's own builder verification. |
| Store metadata | Pass | English title, descriptions, changelog, 512×512 icon, and two 1392×3120 screenshots are present upstream. |
| Immutable release ref | Pass | Public annotated tag `v1.0.0` resolves to `b26bbca1792c3d86241e80ecbb6db1d1de08baf9`. |
| F-Droid metadata validation | Pass | `readmeta`, `rewritemeta`, `checkupdates`, `lint`, `fdroid build`, and `check apk` passed; the merge-request pipeline completed successfully. |

The relevant rules are defined by F-Droid's
[Inclusion Policy](https://f-droid.org/en/docs/Inclusion_Policy/),
[Submission Quick Start Guide](https://f-droid.org/en/docs/Submitting_to_F-Droid_Quick_Start_Guide/),
and [Anti-Features documentation](https://f-droid.org/en/docs/Anti-Features/).

## Build and test evidence

| Property | Audited value |
|---|---|
| Application id | `io.github.r0x4nk.nexnote` |
| Version | `1.0.0` (`versionCode` 1) |
| SDK | minSdk 29, targetSdk 36, compileSdk 36 |
| Toolchain | Gradle 9.3.1, Android Gradle Plugin 9.1.1, Kotlin 2.2.10, JDK 21 |
| Database | Room schema 9; explicit non-destructive migrations from schema 5 through 9 |
| JVM tests | 815 passed, 0 failed, 0 skipped |
| Android tests | 177 passed on a Pixel 9 Pro XL AVD running Android 16, 0 failed, 0 skipped |
| Lint | 0 errors; 13 advisory warnings per build variant |
| Release APK | 50,870,839 bytes, unsigned, 154 ZIP entries |
| Repeatability | Two clean offline APKs had identical SHA-256 hashes |
| Signed upstream APK | 50,897,550 bytes; SHA-256 `24EDDE261B160640C1E6881FBB13C11281BE75E817CD7549D1E2F6C28C515F91` |

The complete local gate was executed with every task forced and all network
resolution disabled:

```powershell
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat clean ci --offline --no-daemon --no-build-cache --rerun-tasks
.\gradlew.bat connectedDebugAndroidTest --offline --no-daemon --no-build-cache --rerun-tasks
```

The `ci` task executes unit tests, compiles instrumentation tests, runs debug
and release lint, and assembles both APK variants. `connectedDebugAndroidTest`
executes the device-backed suite, including Room migrations 5→9, FTS search,
statistics indexing, large datasets, and Compose UI behavior.

The release APK was also inspected with Android SDK tools:

- package, version, minimum SDK, and target SDK match the metadata;
- it is unsigned, as expected for distributor signing;
- it contains `assets/legal/GPL-3.0-only.txt` and
  `assets/legal/THIRD_PARTY_NOTICES.md` with canonical LF line endings;
- it contains no debug-only note generator;
- it contains eight AndroidX native library entries across four ABIs;
- it contains no signature block or signing certificate.

The 13 lint warnings are 12 dependency-update notices and one `OldTargetApi`
notice for API 36 while API 37 is installed. They are intentionally visible and
non-blocking. Upgrading AGP, Kotlin, Compose, Room, or the target SDK immediately
before release would widen the tested scope and require another device-backed
compatibility pass. Kotlin also reports Android's deprecation of the current
device-credential confirmation API; migrating that Vault flow to Biometric is a
separate compatibility change, not an F-Droid eligibility failure.

## Database and privacy review

Room migrations 5→6, 6→7, 7→8, and 8→9 are explicit and covered both directly
and through supported migration chains. Schema 8 adds the derived ordinary-note
statistics index. Schema 9 adds FTS for active ordinary notes. Vault and trash
notes are excluded from both derived indexes. Destructive migration fallback is
not enabled, and schemas earlier than 5 remain unsupported because authentic
historical snapshots are unavailable.

NexNote remains offline and account-free. Ordinary notes use Android's private
app sandbox. Vault note fields and images receive additional PIN-derived
encryption. Android backup and device-to-device transfer are disabled. Exported
or shared files leave the app only through an explicit user action. These
boundaries are documented in `PRIVACY-POLICY.md` and
`docs/vault-and-backup.md`.

## Metadata and asset review

Upstream metadata is in `fastlane/metadata/android/en-US/` and complies with the
documented F-Droid limits:

- title: 7 characters, below the 50-character limit;
- short description: below 80 characters and without a trailing period;
- full description: below 4,000 characters;
- versionCode 1 changelog: below 500 characters;
- screenshots use the conventional `1.png` and `2.png` names;
- images are PNG, and the icon is 512×512.

Asset ownership, derivation, dimensions, licenses, and current SHA-256 values
are recorded in `docs/fdroid-asset-inventory.md`. F-Droid's current image and
description requirements are documented in
[All About Descriptions, Graphics, and Screenshots](https://f-droid.org/en/docs/All_About_Descriptions_Graphics_and_Screenshots/).

## Submission record and remaining actions

The completed submission is recorded by these public artifacts:

- source tag and signed upstream APK:
  [GitHub release `v1.0.0`](https://github.com/R0X4N-K/NexNote/releases/tag/v1.0.0);
- fork metadata branch: `R0X4N-K/fdroiddata:io.github.r0x4nk.nexnote`;
- canonical metadata commit:
  `12ba5badc6aed35662d1f51009a874c99d8031b8`;
- successful pre-submission pipeline:
  [`#2783220213`](https://gitlab.com/R0X4N-K/fdroiddata/-/pipelines/2783220213);
- official review request:
  [`fdroiddata` merge request !46620](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/46620);
- successful merge-request pipeline:
  [`#2783228159`](https://gitlab.com/R0X4N-K/fdroiddata/-/pipelines/2783228159).

While review is in progress, keep the repository and release tag public, never
rewrite `v1.0.0`, and respond to concrete bot or maintainer findings. Any
metadata correction must be committed to the existing submission branch so the
merge request and its pipeline update in place.

The authoritative workflow is the current
[F-Droid submission guide](https://f-droid.org/en/docs/Submitting_to_F-Droid_Quick_Start_Guide/)
and the
[`fdroiddata` contribution guide](https://gitlab.com/fdroid/fdroiddata/-/blob/master/CONTRIBUTING.md).

### After acceptance

- verify the package id, version, descriptions, icon, screenshots, license,
  source link, and issue tracker on the live F-Droid page;
- install the F-Droid-signed APK on a clean supported device and repeat the
  primary note, Vault, export, migration, and deletion smoke tests;
- publish F-Droid's signing certificate fingerprint in `signature/README.md`;
- preserve application id, versionCode monotonicity, database migrations, and
  the F-Droid signing lineage for every update;
- tag each later release and update upstream Fastlane changelogs before F-Droid
  detects the new version.

F-Droid signs its own APK. The optional GitHub-signed release and the F-Droid
release therefore normally have different signing certificates. Users cannot
install one distribution channel directly over the other unless a separately
approved reproducible-build/signing arrangement is implemented.
