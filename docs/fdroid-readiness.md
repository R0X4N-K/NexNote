# F-Droid submission record

This is a historical record of the 1.0.1 submission, last updated on
23 August 2026. It does not establish the review status or build results of
later releases. See the [merge request](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/46620)
for subsequent activity.

## Public records

- [Upstream release v1.0.1](https://github.com/R0X4N-K/NexNote/releases/tag/v1.0.1),
  source commit `430175d07bf445c124f895622bc7a59ba619918c`.
- Submission branch: `R0X4N-K/fdroiddata:io.github.r0x4nk.nexnote`.
- Metadata commit: `d0073b5221248ad6dc54310a3429e86e1642067d`.
- [Pipeline #2783418223](https://gitlab.com/R0X4N-K/fdroiddata/-/pipelines/2783418223).
- [Production signing certificate and APK hashes](../signature/README.md).

The recorded pipeline passed all nine jobs, including the source build and APK
check. It rebuilt version 1.0.1, verified the reference APK against the rebuilt
binary, and accepted the pinned upstream signing certificate.

The source-side checks recorded 815 passing JVM tests and two byte-identical
unsigned release builds for 1.0.1. The 177 Android tests were run on 1.0.0;
1.0.1 changed version and distribution files, not application source. These
counts describe those releases only.

## Maintaining the submission

The local [metadata template](fdroid-submission-template.yml) now describes
1.0.3, version code 35. It is separate from the historical 1.0.1 evidence above
and from the recipe maintained in `fdroiddata`.

For a new release:

1. Run the [build and dependency checks](fdroid-build-supply-chain.md) and
   [tests](testing.md) on the release commit.
2. Update the version, changelog, and Fastlane metadata. Keep existing release
   tags and APKs unchanged.
3. Publish the new tagged release with the established signing key, then update
   or verify the F-Droid recipe for that version.
4. Check the F-Droid pipeline's reproducibility result and, after publication,
   the catalog metadata and installation/upgrade behavior.

A successful local build does not establish F-Droid acceptance or reproducibility
on its build servers.

## Release preparation — 2026-09-18

The metadata template now targets 1.0.4 (version code 36, tag v1.0.4),
retaining the 1.0.3 build entry and the production signing certificate.
The earlier results above remain historical evidence only. Verification of
1.0.4 on F-Droid and replacement of the pre-restyling store screenshots
remain separate release checks.

## Markdown preview fix and screenshot refresh — 2026-09-18

Version 1.0.5 (code 37, tag v1.0.5) corrects Markdown card previews for shared
text, including existing notes imported by 1.0.4. The source-side `ci` checks
pass all 947 JVM tests. Eleven focused Android tests passed on an Android 16
emulator, covering shared-text persistence, legacy card previews, and cold/warm
share intents. These are focused device results, not a full device-suite run.

The two Fastlane screenshots now show the 1.0.5 interface and synthetic notes;
see the [asset inventory](fdroid-asset-inventory.md) for capture details and hashes.
The metadata template retains the earlier build entries and adds 1.0.5.
The 1.0.4 [F-Droid pipeline #2860628475](https://gitlab.com/R0X4N-K/fdroiddata/-/pipelines/2860628475)
passed all nine jobs and verified reproducibility. The 1.0.5 server result must
be recorded separately after its signed APK is published.

## Published APK and upgrade verification — 2026-09-18

The public [1.0.5 release](https://github.com/R0X4N-K/NexNote/releases/tag/v1.0.5)
contains `NexNote-v1.0.5.apk` (5,995,728 bytes). The downloaded APK's SHA-256
matches the GitHub asset digest recorded in [release signatures](../signature/README.md).
`apksigner verify` accepts its v3 signature and unchanged production certificate.
The package reports version 1.0.5 / code 37 and includes all four expected ABIs.
Both [upstream CI](https://github.com/R0X4N-K/NexNote/actions/runs/35347801957)
and the [release workflow](https://github.com/R0X4N-K/NexNote/actions/runs/35347830817)
passed.

A separate manual upgrade check used the public signed APKs on a Pixel 9 Pro XL
Android 16 emulator. Three synthetic notes shared into 1.0.4 displayed raw
Markdown in their cards. Installing 1.0.5 with `adb install -r`, without clearing
app data or editing those notes, preserved all three and rendered their headings,
bold text, links, and checklist items correctly. This also verifies the fix in
the minified production APK, beyond the earlier debug instrumentation tests.

A new note shared into the signed 1.0.5 APK also rendered its Markdown heading,
bold text, and checked list item correctly.

## F-Droid reproducibility verification — 2026-09-18

The existing [merge request !46620](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/46620)
now includes [commit 667a7970](https://gitlab.com/R0X4N-K/fdroiddata/-/commit/667a7970297783cc815522cf7e8ae81c8dbac38f),
adding 1.0.5 / code 37 / tag v1.0.5 without changing the pinned signing key.
The local submission template matches that metadata apart from its introductory
comment. The [build job](https://gitlab.com/R0X4N-K/fdroiddata/-/jobs/16595014854)
successfully compared the rebuilt code-37 APK with the supplied reference binary
and accepted the production signer. It also reverified the historical builds 35
and 36. [Pipeline #2862358067](https://gitlab.com/R0X4N-K/fdroiddata/-/pipelines/2862358067)
passed all nine jobs, including `fdroid build`, `check apk`, `checkupdates`, and
`fdroid rewritemeta`. F-Droid publication remains pending maintainer review and merge.
