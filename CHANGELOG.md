# Changelog

All notable changes to NexNote will be documented in this file.

The format follows Keep a Changelog, and this project uses semantic versioning.

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
