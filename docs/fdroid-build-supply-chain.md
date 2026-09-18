# Build and release maintenance

This guide describes NexNote 1.0.5 (code 37), prepared for tag `v1.0.5`.

## Build configuration

| Setting | Current value |
| --- | --- |
| Release version / code | 1.0.5 / 37 |
| Minimum Android version | API 29 |
| Compile SDK | 36.1 |
| Target SDK | 36 |
| Build JDK | 21 |
| Java source/target level | 11 |
| Gradle wrapper | 9.3.1 |
| Android Gradle Plugin | 9.1.1 |
| Kotlin / Compose compiler plugin | 2.2.10 |
| KSP | 2.3.9 |
| Room schema | 10, with migrations from 5 onward |

The wrapper distribution checksum is pinned in
`gradle/wrapper/gradle-wrapper.properties`. The checked-in wrapper JAR has SHA-256
`b3a875ddc1f044746e1b1a55f645584505f4a10438c1afea9f15e92a7c42ec13`.
Use [JDK setup](build-jdk.md) for local configuration.

Plugins and dependencies resolve from Google Maven and Maven Central.
`app/gradle.lockfile` and `settings-gradle.lockfile` fix dependency resolution;
`gradle/verification-metadata.xml` checks artifact SHA-256 values. Source and
Javadoc artifacts used by the IDE are covered too.

The [third-party notices](../THIRD_PARTY_NOTICES.md) list runtime, test, build,
and font licenses. The build packages those notices and the project license
under `assets/legal/`.

## Updating dependencies

Keep Gradle, AGP, and the matching AAPT2 build number aligned. Review changed
versions, lockfiles, licenses, and checksums together. Preserve the exported Room
schemas and test migrations when changing database code or dependencies.

After changing dependencies, resolve the affected configurations while updating
lock state and generating checksum candidates. Review both diffs, then rerun
the checks in strict mode:

```bash
./gradlew --no-daemon --refresh-dependencies --rerun-tasks \
  --write-locks --write-verification-metadata sha256 ci
./gradlew --no-daemon --refresh-dependencies --rerun-tasks ci
./gradlew --no-daemon --offline --no-build-cache clean ci
```

Verify new checksum entries against trusted upstream artifacts before committing
them. Recording a checksum is not itself a trust check. Do not disable verification
to make the build pass. Offline checks need previously downloaded dependencies.

The root `ci` task resolves AAPT2 for Linux, macOS, and Windows as well as the
normal build tasks. Run [device tests](testing.md) separately.

## Release APK and signing

`assembleRelease` produces an unsigned universal APK with R8 minification and
resource shrinking. The workflows require four ABIs, an R8 mapping file, and an
unsigned APK below 15 MiB. Only the release workflow checks all four ABIs; the
normal build workflow checks the mapping and size.

The tag-triggered release workflow checks that the tag matches `versionName`,
checks 16 KiB ZIP alignment, and signs with Build Tools 34.0.0 using v2 and v3
signatures with v1 disabled. It then publishes one GitHub release APK.
See [Contributing](../CONTRIBUTING.md#releases) for secrets and release steps.

Version codes 31-34 belonged to the older ABI-specific release. Code 35 replaced
them with a universal APK; later releases need a larger code. Never rewrite an
existing release tag or replace its APK with different contents.

## F-Droid

The [metadata template](fdroid-submission-template.yml) targets 1.0.5. It uses
`Binaries` to locate the signed upstream APK and `AllowedAPKSigningKeys` to pin
the [production certificate](../signature/README.md). Update checks read
`baseVersionCode` and `versionName` from `app/build.gradle.kts`.

A builder needs JDK 21 and SDK platform 36.1 for the current source. The unsigned
build does not need signing secrets. Verify the effective recipe and server
reproducibility result for each new release; the local YAML is not the live
F-Droid configuration. The [submission record](fdroid-readiness.md) preserves
historical evidence for 1.0.1, not approval of subsequent changes.
