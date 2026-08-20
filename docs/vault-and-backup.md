# Vault and backup model

This document describes the behavior implemented in version 1.0.0. It is not a
claim that every file in NexNote is encrypted.

## Storage boundaries

- Ordinary notes, tags, templates, preferences, and ordinary note images live in
  Android private app storage. They rely on the Android application sandbox and
  are not encrypted with a separate NexNote key.
- Vault note fields and Vault image files are encrypted at rest with AES-GCM.
  The AES-256 key is derived from the user's non-empty PIN using
  PBKDF2-HMAC-SHA-256, a random 16-byte salt, and 310,000 iterations.
- Vault plaintext is handled in memory while the Vault is unlocked. NexNote does
  not claim protection against a compromised operating system, a rooted process,
  screen capture outside protected Vault surfaces, or memory inspection.
- The current PIN rule requires only a non-empty value. PBKDF2 slows offline
  guessing but cannot compensate for a weak PIN; users should choose a long,
  difficult-to-guess value.

The default settings protect Vault previews in Android Recents, lock the Vault
when the app goes to the background or the screen turns off, and use an immediate
auto-lock timeout. These controls are configurable. Optional unlock through the
Android device credential is disabled by default; when enabled, the required
unlock material is protected by Android Keystore authentication.

## PIN changes and reset

A successful PIN change rotates the key for active and trashed Vault rows and
for every referenced Vault image. The operation stages image replacements,
updates Room data transactionally, commits the new PIN configuration, and only
then removes superseded files. Failure or coroutine cancellation rolls back the
database/key state and removes staged files so the previous PIN remains usable.

Resetting the Vault is destructive. It deletes Vault rows and their image files
and clears the Vault configuration and optional device-credential material.

## Backup and transfer

The manifest and both Android backup-rule formats exclude all app data from cloud
backup and device-to-device transfer. Consequently:

- reinstalling NexNote does not restore its private database or Vault;
- moving to another device does not automatically migrate notes;
- losing the device, app data, or Vault PIN can make data unrecoverable.

Use the explicit export flow before uninstalling, resetting the Vault, or moving
devices. Files saved or shared through another app leave NexNote's private
storage and are governed by that destination. Exported Vault content is plaintext
once deliberately exported; the receiving location must be protected separately.

## Export and cache lifecycle

NexNote creates share/export files with unique names in its cache. Failed exports
are removed immediately, print files are removed after the print flow finishes,
and share files older than 24 hours are removed on a later launch or export.
Android may evict cache earlier. The system share sheet grants temporary access
only to the file chosen by the user through a non-exported `FileProvider`.
