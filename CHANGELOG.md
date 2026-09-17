# Changelog

All notable changes to NexNote will be documented in this file.

The format follows Keep a Changelog, and this project uses semantic versioning.

## [Unreleased]

## [1.0.4] - 2026-09-18

### Added

- Attach documents, audio, and other files; share them with notes in portable ZIP bundles.
- Receive plain text and images shared from other apps.
- Choose Android device colors and the System, Inter, Lora, Fira Code, or JetBrains Mono typeface.
- Browse Calendar and Agenda in separate tabs, with search and collapsible day groups.
- Change a note's creation date from Home, Agenda, Tags, or Vault.
- Check or uncheck all checklist items and remove all tags from the editor menu.
- Indent and outdent lines with Tab/Shift+Tab or the editor toolbar.
- Preview the three note-card styles in Settings.

### Changed

- Refresh cards, menus, dialogs, and colors across light, dark, OLED, and device themes.
- Show date, tags, and files in detailed note cards; use a localized placeholder for untitled notes.
- Render Markdown in template previews.
- Group selection actions consistently and make Move to Vault available from Agenda and Tags.
- Keep duplication and pin actions in note lists instead of repeating them in the editor menu.
- Add an Include media option to control bundled files. TXT and Markdown can omit media; PDF and printing retain embedded images.
- Stream new Vault files during encryption, copying, and PIN changes. Document attachments have no app-imposed size quota.
- Release unused ordinary-note files after editor undo history is discarded.
- Expand localized text and accessibility labels across the interface.
- Shorten the Vault pull-to-unlock gesture and add haptic feedback.

### Fixed

- Roll back failed duplication and preserve independent ownership of copied files.
- Avoid repeated text scanning when duplicating long notes with many tags.
- Reuse the app task for incoming shares and open each imported note in the correct editor.
- Resume pending Vault attachment imports after unlocking.
- Persist PIN attempt limits and retry file cleanup after deleting all ordinary notes.
- Read fresh database snapshots for export.
- Prevent note swipes from interfering with Calendar/Agenda navigation.
- Improve large-text layouts, system-bar contrast, and Vault reset dialog stability.

## [1.0.3] - 2026-08-31

### Changed

- Enabled R8 code minification and resource shrinking for release builds.
- Returned to a single universal release APK after reducing its size by more than 90%.

## [1.0.2] - 2026-08-24

### Added

- Added an About section in Settings with a direct link to the NexNote source code.

### Changed

- Added architecture-specific release APKs and ordered version codes for F-Droid.

### Fixed

- Corrected the editor's bottom fade so content extends naturally into it without overlapping editing controls.

## [1.0.1] - 2026-08-23

### Changed

- Pinned release signing to Android Build Tools 34 for F-Droid reproducible-build verification.
- Preserved 16 KiB native-library alignment checks before signing.

## [1.0.0] - 2026-08-22

### Added

- Local-first Markdown notes with tags, agenda, reusable templates, trash, and export.
- Full-text search with scope, pinned-state, sorting, and bounded result controls.
- On-device writing statistics, activity history, streaks, and tag insights.
- An encrypted Vault for selected notes and their images.
- Bulk selection and data-management actions, including deleting all stored notes.
- Debug-only large-dataset generation and performance validation tools.
- Database migrations through schema version 9 without destructive fallback.
- F-Droid metadata, original project artwork, dependency verification, and GPL-3.0-only licensing.
