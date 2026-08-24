# Changelog

All notable changes to NexNote will be documented in this file.

The format follows Keep a Changelog, and this project uses semantic versioning.

## [1.0.2] - 2026-08-24

### Added

- Added an About section in Settings with a direct link to the NexNote source code.

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
