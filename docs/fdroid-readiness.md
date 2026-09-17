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
