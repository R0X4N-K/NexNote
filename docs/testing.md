# Running tests

Set up a [complete JDK 21](build-jdk.md) and the Android SDK before running the
Gradle wrapper. From the repository root:

```bash
./gradlew ci
```

On Windows, use `.\gradlew.bat ci`. This task runs local unit tests, compiles
Android tests, runs debug and release lint, builds both APK variants, and
verifies the AAPT2 artifacts used on supported build hosts. It does not run tests
on an Android device. GitHub Actions runs this task and additionally checks that
the release APK has an R8 mapping file and is smaller than 15 MiB.

## Android tests

Use a disposable emulator or test device without personal NexNote data. Tests
create notes and exercise deletion, Vault reset, and PIN changes in the debug
app (`io.github.r0x4nk.nexnote.debug`). With only the intended device connected:

```bash
./gradlew :app:connectedDebugAndroidTest
```

A focused run can select a test class:

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=io.github.r0x4nk.nexnote.fileimport.ExternalIntentTaskReuseTest
```

`ExternalIntentTaskReuseTest` uses a separate sender activity and provider to
check URI permissions and reuse of the app task. `EditorVaultAttachmentPickerTest`
uses Android's document picker to check locking, unlocking, and pending imports.
These fixtures belong to the test APK, not the release app.

## Useful regression suites

| Area | Tests |
| --- | --- |
| Schema upgrades and search | `NexNoteDatabaseMigrationTest`, `NoteRepositoryTest` |
| Fresh export snapshots | `NoteRepositorySnapshotTest` |
| File import and recovery | `InternalNoteAttachmentStorageTest`, `AttachmentImportJournalTest` |
| Encryption and PIN throttling | `VaultFileStreamsTest`, `VaultImageFileStorageTest`, `VaultPinAttemptLimiterTest` |
| Deferred deletion and undo retention | `PendingImageCleanupTest`, `EditorFileRetentionTest` |
| ZIP and media export options | `AttachmentBundleExporterTest`, `ExportMediaPolicyTest` |
| Accessibility | `RadialMenuAccessibilityTest`, `MarkdownTaskAccessibilityTest` |
| Colors and card metadata | `ColorContrastTest`, `NoteCardMetadataTest` |

For example, run the encryption unit tests with:

```bash
./gradlew :app:testDebugUnitTest --tests '*VaultFileStreamsTest'
```

To check streaming with a small heap, save this Gradle init script outside the
tracked project files and pass its path with `--init-script`:

```groovy
allprojects {
    tasks.withType(Test).configureEach { maxHeapSize = '64m' }
}
```

For example, after saving it as `/tmp/nexnote-small-heap.gradle` on Linux:

```bash
./gradlew :app:testDebugUnitTest --tests '*VaultFileStreamsTest' \
  --tests '*VaultImageFileStorageTest' --init-script /tmp/nexnote-small-heap.gradle
```

On Windows, use `.\gradlew.bat`, the path where you saved the script, and one
command line instead of the Bash line continuation.

The large-file case checks streaming beyond the test process's heap size;
it does not measure the memory or frame rate of the whole Android app.

## Reviewing results

Gradle writes reports beneath `app/build/reports/` and machine-readable results
beneath `app/build/test-results/` and `app/build/outputs/androidTest-results/`.
They are generated files and should not be committed. Record the tested commit,
commands, device/API, and any failures in the pull request.

For UI changes, also check light, dark, OLED, and device colors; larger text;
TalkBack labels and focus; and editor typing, undo, save, and reopen.
`ThemeGalleryTest` can generate palette images for visual review. Automated
semantics and contrast checks do not replace that review.
