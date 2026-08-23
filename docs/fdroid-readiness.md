# F-Droid release readiness audit

Audit date: 2026-08-22

Submission status updated: 2026-08-23

This document records the source-side audit of NexNote 1.0.1 (`versionCode` 2).
It is not an F-Droid approval or a claim that the app is already published.
Only F-Droid maintainers can accept the final `fdroiddata` merge request.

## Verdict

NexNote has been submitted for official F-Droid review. The immutable public
[`v1.0.1`](https://github.com/R0X4N-K/NexNote/releases/tag/v1.0.1) tag resolves to
commit `430175d07bf445c124f895622bc7a59ba619918c`. The metadata is under review in
[`fdroiddata` merge request !46620](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/46620).

The current merge-request pipeline passed all nine jobs, including `fdroid
build` and `check apk`. F-Droid successfully rebuilt version 1.0.1, transferred
the upstream v3 signature to its build, compared it with the supplied GitHub
APK, and accepted the configured signing certificate. This is not an F-Droid
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
| Reproducible build controls | Pass | F-Droid pipeline `#2783418223` reported `successfully verified`, successfully compared the rebuilt APK with the supplied reference binary, and accepted the pinned signer. |
| Store metadata | Pass | English title, descriptions, changelog, 512×512 icon, and two 1392×3120 screenshots are present upstream. |
| Immutable release ref | Pass | Public annotated tag `v1.0.1` resolves to `430175d07bf445c124f895622bc7a59ba619918c`. |
| F-Droid metadata validation | Pass | `readmeta`, `rewritemeta`, `checkupdates`, `lint`, `fdroid build`, and `check apk` passed; the merge-request pipeline completed successfully. |

The relevant rules are defined by F-Droid's
[Inclusion Policy](https://f-droid.org/en/docs/Inclusion_Policy/),
[Submission Quick Start Guide](https://f-droid.org/en/docs/Submitting_to_F-Droid_Quick_Start_Guide/),
and [Anti-Features documentation](https://f-droid.org/en/docs/Anti-Features/).

## Build and test evidence

| Property | Audited value |
|---|---|
| Application id | `io.github.r0x4nk.nexnote` |
| Version | `1.0.1` (`versionCode` 2) |
| SDK | minSdk 29, targetSdk 36, compileSdk 36 |
| Toolchain | Gradle 9.3.1, Android Gradle Plugin 9.1.1, Kotlin 2.2.10, JDK 21 |
| Database | Room schema 9; explicit non-destructive migrations from schema 5 through 9 |
| JVM tests | 815 passed on 1.0.1, 0 failed, 0 skipped |
| Android tests | 177 passed on 1.0.0 on a Pixel 9 Pro XL AVD running Android 16; 1.0.1 changes only version, release workflow, changelog, and documentation files |
| Lint | 0 errors; 13 advisory warnings per build variant |
| Release APK | 50,870,843 bytes, unsigned |
| Repeatability | Two clean offline APKs had identical SHA-256 hashes |
| Signed upstream APK | 50,880,967 bytes; SHA-256 `C01681FB8A87F37615E15BB9FB5A62BDDCB7D771ECF6BC13002E0C368B40E79A` |

The complete app gate was executed for 1.0.0 with every task forced and all
network resolution disabled:

```powershell
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat clean ci --offline --no-daemon --no-build-cache --rerun-tasks
.\gradlew.bat connectedDebugAndroidTest --offline --no-daemon --no-build-cache --rerun-tasks
```

Version 1.0.1 was then validated with a clean release build, all 815 JVM tests,
and release lint. Its two clean unsigned release builds were byte-identical.
The only source changes between the tested app revisions are version and
distribution-maintenance files; application and test source are unchanged.

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
- versionCode 1 and 2 changelogs: below 500 characters each;
- screenshots use the conventional `1.png` and `2.png` names;
- images are PNG, and the icon is 512×512.

Asset ownership, derivation, dimensions, licenses, and current SHA-256 values
are recorded in `docs/fdroid-asset-inventory.md`. F-Droid's current image and
description requirements are documented in
[All About Descriptions, Graphics, and Screenshots](https://f-droid.org/en/docs/All_About_Descriptions_Graphics_and_Screenshots/).

## Submission record and remaining actions

The completed submission is recorded by these public artifacts:

- source tag and signed upstream APK:
  [GitHub release `v1.0.1`](https://github.com/R0X4N-K/NexNote/releases/tag/v1.0.1);
- fork metadata branch: `R0X4N-K/fdroiddata:io.github.r0x4nk.nexnote`;
- canonical metadata commit:
  `d0073b5221248ad6dc54310a3429e86e1642067d`;
- official review request:
  [`fdroiddata` merge request !46620](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/46620);
- successful merge-request pipeline:
  [`#2783418223`](https://gitlab.com/R0X4N-K/fdroiddata/-/pipelines/2783418223).

While review is in progress, keep the repository and release tag public, never
rewrite `v1.0.1`, and respond to concrete bot or maintainer findings. Any
metadata correction must be committed to the existing submission branch so the
merge request and its pipeline update in place.

The authoritative workflow is the current
[F-Droid submission guide](https://f-droid.org/en/docs/Submitting_to_F-Droid_Quick_Start_Guide/)
and the
[`fdroiddata` contribution guide](https://gitlab.com/fdroid/fdroiddata/-/blob/master/CONTRIBUTING.md).

### After acceptance

- verify the package id, version, descriptions, icon, screenshots, license,
  source link, and issue tracker on the live F-Droid page;
- install the APK published by F-Droid on a clean supported device and repeat the
  primary note, Vault, export, migration, and deletion smoke tests;
- preserve application id, versionCode monotonicity, database migrations, and
  the upstream signing lineage for every update;
- tag each later release and update upstream Fastlane changelogs before F-Droid
  detects the new version.

This submission uses F-Droid's reproducible-build path. F-Droid rebuilds each
version from source and publishes the upstream-signed APK only when the rebuilt
binary matches and the signer equals `AllowedAPKSigningKeys`. The GitHub and
F-Droid channels therefore retain the same Android signing identity.
