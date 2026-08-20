# Privacy Policy

Last updated: 2026-08-16

NexNote is a local-first note-taking app.

## Data Stored By The App

NexNote stores notes, tags, templates, preferences, and imported note images on the device. The app uses local Android storage, Room, and DataStore.

Ordinary notes rely on Android's private app sandbox and are not encrypted by a
separate NexNote key. Notes and images placed in the Vault are additionally
encrypted at rest using a key derived from the user's Vault PIN. The optional
Android device-credential unlock stores protected Vault unlock material in the
Android Keystore; it is disabled by default. See `docs/vault-and-backup.md` for
security boundaries and limitations.

## Network And Accounts

NexNote currently does not require an account, does not use a remote backend, and does not include analytics, advertising, Firebase, Google Play Services, or crash-reporting SDKs.

## Export And Sharing

When you export or share a note, Android's system share sheet and FileProvider may give another app temporary read access to the exported file you choose to share. NexNote stores these files in its cache with unique names. It deletes incomplete exports immediately, deletes print files when the print flow finishes, and removes share files older than 24 hours on a later app launch or export. Android may clear cache files sooner. Files saved by another app are outside NexNote's control.

When you copy note text, that text is placed on the Android system clipboard and can be pasted into other apps. NexNote marks it as sensitive so compatible system surfaces obscure the clipboard preview; this flag does not encrypt the clipboard.

## Backups

NexNote opts its private app data out of Android cloud backup and device-to-device transfer. Reinstalling the app or moving to a new device therefore does not restore notes automatically. Use the app's export feature to create files you can manage and transfer yourself.

## User Control

Deleting notes or clearing app data removes local app data according to Android's normal storage behavior. Exported files that you save or share outside the app must be managed separately.

## Changes

This policy will be updated when NexNote adds behavior that changes how user data is stored, shared, synced, or transmitted.
