# F-Droid build supply chain

This document describes the source-build configuration for NexNote 1.0.0
(`versionCode` 1).

## Toolchain and repositories

- The wrapper requests Gradle 9.3.1 and pins the official binary distribution
  SHA-256 `b266d5ff6b90eada6dc3b20cb090e3731302e553a27c5d3e4df1f0d76beaff06`.
- `gradle-wrapper.jar` has SHA-256
  `b3a875ddc1f044746e1b1a55f645584505f4a10438c1afea9f15e92a7c42ec13`,
  matching Gradle's published 9.3.1 wrapper checksum.
- The project requires JDK 21. `gradle/gradle-daemon-jvm.properties` records only
  that version; it contains no vendor download URL or Foojay dependency. An
  F-Droid builder must provide JDK 21 before invoking the wrapper.
- Plugins and dependencies resolve only from Google's Maven repository and Maven
  Central. Gradle Plugin Portal and the Foojay resolver plugin are not used.
- Android Gradle Plugin 9.1.1 is paired with Gradle 9.3.1. Kotlin is 2.2.10 and
  KSP is 2.3.9, which supports AGP 9 built-in Kotlin source handling.

Official verification sources:

- <https://gradle.org/release-checksums/>
- <https://developer.android.com/build/releases/agp-9-1-0-release-notes>
- <https://github.com/google/ksp/releases>

## Resolution controls

`app/gradle.lockfile` and `settings-gradle.lockfile` fix dependency resolution.
`gradle/verification-metadata.xml` enables strict SHA-256 verification of
resolved artifacts. Its coverage includes Gradle/IDE source and Javadoc
artifacts requested by Android Studio models, so project sync can retain strict
verification. The version catalog was reconciled with versions already selected
by the graph where doing so removed misleading nominal pins.

The complete runtime license inventory is in `THIRD_PARTY_NOTICES.md`. Runtime,
test-only, and build-only components are deliberately separated there. All
release runtime components and native libraries are FLOSS and resolve from the
two declared repositories.

## Deliberate version dispositions

Lint version checks are advisory, not proof that an upgrade is compatible. These
families remain pinned pending a separately tested upgrade:

| Family | Current disposition |
|---|---|
| Gradle/AGP | Gradle 9.3.1 and AGP 9.1.1 are an officially supported pair; do not move either independently. |
| Kotlin/Compose compiler | Kotlin 2.2.10 is retained with the current Compose and KSP graph. |
| Compose BOM | The resolved 2026.02.01 platform and its Compose 1.10.4 modules are retained as one tested set. |
| Room | 2.7.0 supports the validated 5→6, 6→7, and 5→7 migration baseline. Database schemas earlier than 5 are outside the supported upgrade path. |
| Navigation, DataStore, ExifInterface | Retained to keep dependency changes scoped to the current release. |
| AndroidX test/Espresso/JUnit | Test-only pins; upgrade separately with device-backed instrumentation. |
| Coroutines | 1.10.1 retained with the current cancellation regression coverage. |

## Android API and native libraries

The app compiles and targets API 36. API 37 targeting and its Android 17
behavior changes are outside the scope of version 1.0.0. `OldTargetApi`
therefore remains visible rather than being suppressed. The deprecated
device-credential confirmation API is retained until a modern Biometric
migration can be validated on supported devices.

The APK's two AndroidX native libraries are supplied for four ABIs. The
dependencies ship stripped binaries without `.debug*` sections or `.symtab`;
`.dynsym` remains as required for dynamic linking.

## F-Droid invocation

With Android SDK platform/API 36 and JDK 21 provisioned, the recipe needs only
the standard Gradle build (`gradle: yes`), which maps to `assembleRelease` for
this flavorless single-module project. The unsigned APK does not depend on
signing secrets. `docs/fdroid-submission-template.yml` intentionally leaves the
public release ref unresolved until a real, immutable commit or tag exists.
