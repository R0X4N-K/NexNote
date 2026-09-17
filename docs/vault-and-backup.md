# Vault and backup

Ordinary notes use Android's private app storage. The Vault adds encryption for
selected notes and their attached files. This document describes the current
source tree; older releases may use the earlier file format.

## What is protected

Ordinary notes, tags, templates, preferences, images, and documents are not
encrypted with a separate NexNote key. Vault titles, bodies, and file manifests
use AES-GCM with an
AES-256 key derived from the PIN using PBKDF2-HMAC-SHA-256, a random 16-byte salt,
and 310,000 iterations. New Vault files use the segmented encryption described
in [Vault file format](vault-file-streaming.md). Dates, pin/trash state, colors,
and other row metadata remain outside those encrypted fields.

Decrypted note content is held in memory while the Vault is unlocked. These
protections do not cover a compromised operating system or memory inspection.
The PIN must be non-empty, but the app does not enforce a minimum length;
choose a long, difficult-to-guess PIN.

By default, NexNote protects Vault previews in Recents and locks the Vault when
the app goes into the background or the screen turns off. Auto-lock settings
are configurable. Optional Android device-credential unlock is off by default;
when enabled, its unlock material is protected by Android Keystore with a
30-second authentication window.

## PIN attempts, changes, and reset

Five failed or interrupted PIN checks trigger a 30-second delay. Further failures
double the delay, up to 30 minutes. Successful verification resets the counter.
The counter is persisted before verification, applies to unlock and PIN changes,
and survives app restarts. A device reboot restarts the outstanding penalty.
This limits guesses through the app; it cannot stop offline attacks on copied data.

Changing the PIN rotates the key for active and trashed Vault notes and their
files. File replacements are staged, Room data is updated transactionally, and
the new PIN configuration is saved before old files are removed. Handled errors
and cancellation attempt rollback using encrypted backups. Room, the filesystem,
and DataStore do not share one transaction: a process crash during this sequence
can still leave inconsistent state.

Resetting the Vault permanently deletes its notes and files, PIN configuration,
and device-credential unlock material. There is no PIN recovery service.

## Adding attachments

The editor saves before opening Android's file picker. The picker does not bypass
auto-lock. If the Vault locks, unlock in the editor to finish importing, or cancel
to discard the selection. An incorrect PIN leaves it pending.

The pending selection stays in the editor ViewModel. It is not saved to disk and
does not contain a copy of the note, PIN, or key. The file is not opened until
unlock succeeds. After process death, select the file again. A new lock pauses
resumption until the next unlock; cancellation, Vault reset, or a switch to a
different note discards the selection.

## Backup, export, and data loss

Android cloud backup and device-to-device transfer are disabled. Reinstalling,
clearing app data, or moving to another device does not restore notes. Losing
the device or Vault PIN can make data unrecoverable.

Vault notes cannot be exported, printed, or opened in an external viewer while
in the Vault. To export one, first move it out of the Vault. Exported files are
then unencrypted and must be protected at their destination. ZIP export is not
a restorable backup of the app database.

Share/export files use unique names in the app cache. Failed exports are removed
immediately, print files after printing finishes, and share files older than
24 hours on a later launch or export. Android may clear the cache sooner.
FileProvider grants temporary read access to the selected file. Copies saved by
other apps are outside NexNote's control. Deleting a file does not securely
overwrite its physical contents on flash storage.
