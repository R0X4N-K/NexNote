# F-Droid release readiness audit

Audit date: 2026-08-22

This document records the source-side audit of NexNote 1.0.0 (`versionCode` 1).
It is not an F-Droid approval or a claim that the app is already published.
Only F-Droid maintainers can accept the final `fdroiddata` merge request.

## Verdict

NexNote's application source and local source build are ready for an F-Droid
review. The release is **not yet eligible for submission** because two external
release requirements remain incomplete:

1. `https://github.com/R0X4N-K/NexNote` is not anonymously accessible. It
   returned HTTP 404 during this audit. F-Droid requires a publicly accessible
   source repository.
2. No immutable public `v1.0.0` tag exists. The local metadata template
   therefore still contains `REQUIRED_FULL_PUBLIC_RELEASE_COMMIT_SHA`.

An `fdroiddata` merge request has not been created, and the project has not been
built inside F-Droid's own build environment. Do not describe NexNote as
available on F-Droid until that merge request has been accepted and the package
appears in the official repository.

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
| Public source | **Blocked** | Anonymous access to the GitHub source returned HTTP 404 on the audit date. |
| Unique application id | Pass | Release id is `io.github.r0x4nk.nexnote`; no conflicting indexed package was found during the audit. Final acceptance remains F-Droid's decision. |
| No proprietary runtime service | Pass | No Firebase, Google Play Services, proprietary analytics, ads, crash reporting, account, or remote backend. |
| No network tracking | Pass | The release manifest does not request `android.permission.INTERNET`; the only permission in the built APK is AndroidX's package-scoped dynamic-receiver permission. |
| Source-buildable release | Pass locally | Flavorless `assembleRelease` succeeds offline and produces an unsigned APK without signing secrets. |
| Reproducible build controls | Pass locally | Two clean, offline, cache-free release builds were byte-identical. This does not replace F-Droid's own builder verification. |
| Store metadata | Pass | English title, descriptions, changelog, 512×512 icon, and two 1392×3120 screenshots are present upstream. |
| Immutable release ref | **Blocked** | No `v1.0.0` tag exists; the submission template deliberately rejects an unresolved ref. |
| F-Droid metadata validation | Pending | `fdroidserver` is not installed in the audited Windows environment; run the official checks in an `fdroiddata` checkout before opening the merge request. |

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

## Required maintainer actions

### 1. Make the source repository public

Change the GitHub repository visibility to public, then verify the repository,
license, issues, release source, and tag are accessible in a private browser
window without signing in. Do not rely on access from an authenticated GitHub
session.

PowerShell verification:

```powershell
Invoke-WebRequest -Method Head -Uri 'https://github.com/R0X4N-K/NexNote'
$env:GIT_TERMINAL_PROMPT = '0'
git -c credential.helper= ls-remote 'https://github.com/R0X4N-K/NexNote.git' 'refs/heads/main'
Remove-Item Env:GIT_TERMINAL_PROMPT
```

Both commands must succeed anonymously.

### 2. Select and tag the immutable release

Wait for the pushed `main` workflow to pass. Confirm that the working tree is
empty and that `main` is synchronized, then create the release tag:

```powershell
git switch main
git pull --ff-only origin main
git status --short
git tag -a v1.0.0 -m 'NexNote 1.0.0'
git push origin v1.0.0
git rev-parse 'v1.0.0^{commit}'
```

`git status --short` must print nothing. Record the full 40-character commit
hash printed by the final command. The tag must identify the same source used by
the 1.0.0 GitHub release.

### 3. Complete the F-Droid metadata

Copy `docs/fdroid-submission-template.yml` to
`metadata/io.github.r0x4nk.nexnote.yml` in a current fork of the official
`fdroiddata` repository. Replace
`REQUIRED_FULL_PUBLIC_RELEASE_COMMIT_SHA` with the full public commit hash from
the tag. Recheck every URL and version field. Never submit the placeholder.

The initial recipe is intentionally minimal:

```yaml
Builds:
  - versionName: 1.0.0
    versionCode: 1
    commit: FULL_40_CHARACTER_PUBLIC_COMMIT_SHA
    subdir: app
    gradle:
      - yes
```

F-Droid's `gradle: yes` build maps to the standard flavorless release build.
The invocation was tested locally from the `app` subdirectory and requires no
signing material.

### 4. Validate in an official `fdroiddata` checkout

Install the current `fdroidserver` toolchain using the official
[installation instructions](https://f-droid.org/en/docs/Installing_the_Server_and_Repo_Tools/).
From the root of the `fdroiddata` checkout, run:

```bash
fdroid readmeta
fdroid rewritemeta io.github.r0x4nk.nexnote
fdroid checkupdates io.github.r0x4nk.nexnote
fdroid lint io.github.r0x4nk.nexnote
fdroid build -v -l io.github.r0x4nk.nexnote
```

Review any rewrite before committing. All five commands must complete without
an unexplained error. The local source audit does not substitute for this step.

### 5. Open and monitor the `fdroiddata` merge request

Commit only the new metadata file in the `fdroiddata` fork, push the branch, and
open a GitLab merge request against `fdroid/fdroiddata`. Follow the repository's
current contribution template and apply the `New App` label. Respond to bot and
maintainer findings, keep the upstream source public, and do not rewrite the
tag while review is in progress.

The authoritative workflow is the current
[F-Droid submission guide](https://f-droid.org/en/docs/Submitting_to_F-Droid_Quick_Start_Guide/)
and the
[`fdroiddata` contribution guide](https://gitlab.com/fdroid/fdroiddata/-/blob/master/CONTRIBUTING.md).

### 6. After acceptance

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
