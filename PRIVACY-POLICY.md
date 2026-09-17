# Privacy Policy

Last updated: 2026-09-17

NexNote keeps your notes on your device. It does not require an account, send
notes to a server, or include advertising, analytics, or crash-reporting services.

## What the app stores

The app stores notes, tags, templates, preferences, images, and document
attachments in Android's private app storage. It also keeps local search and
writing-statistics indexes for ordinary notes, excluding Vault and trashed notes.

Ordinary notes and files are protected by Android's app sandbox. Vault content
has additional encryption using a key derived from your PIN. Optional unlock
with your Android screen-lock credential keeps protected unlock material in
Android Keystore. This option is off by default.

## Importing, opening, and sharing

Files you import are copied into NexNote's private storage. Text or images you
share to NexNote become new notes.

When you export a note or open an attachment in another app, NexNote creates a
temporary file and grants the receiving app read access through Android's
FileProvider. Incomplete exports are removed, print files are deleted when the
print flow finishes, and shared files older than 24 hours are cleaned up on a
later launch or export. Android may clear the cache sooner. Copies made by
another app are outside NexNote's control.

Vault notes must be moved out of the Vault before export, printing, or external
file opening. Those exported copies are not encrypted by NexNote.

Copying text places it on the Android clipboard. NexNote marks it as sensitive
to hide previews on compatible system surfaces; this does not encrypt it.

## Backup and deletion

NexNote disables Android cloud backup and device-to-device transfer for its data.
Uninstalling, clearing app data, or changing devices does not restore notes.
Export files you want to keep first. Exports are not a full database backup.

You can delete notes individually. The delete-all action in Settings removes
ordinary notes, including their trash; resetting the Vault removes Vault notes
separately. Files already saved outside the app must be deleted at their destination.

See [Vault and backup](docs/vault-and-backup.md) for encryption details, PIN
recovery limits, and behavior during interrupted operations.
