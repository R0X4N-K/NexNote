# Architecture

NexNote has one Android module. Within `app/src/main/java/io/github/r0x4nk/nexnote/`:

- `data` contains Room, DataStore, file storage, encryption, and repository implementations.
- `domain` defines models, repository interfaces, and use cases.
- `ui` contains Compose screens, shared components, and ViewModels.
- `di` groups use cases by feature and wires their dependencies.
- `fileimport` handles files and shares received from other apps.

`NexNoteApp` creates the application dependencies. `AppUseCases` collects the
feature groups; repositories are passed through constructors. Tests can replace
these interfaces without starting the Android application.

## Database and loading

Room schema 10 includes explicit migrations from schema 5 onward. Keep the
exported schemas in `app/schemas/`: migration tests need them. Schema 8 adds
the statistics index, schema 9 adds FTS4 search, and schema 10 adds the pending
file-deletion queue. Vault and trashed notes are excluded from the ordinary-note
search and statistics indexes.

Home initially requests 64 notes and increases the SQL result limit in batches
of 64. Search and tag/pin filters run in SQL; an extra row detects more results.
Select-all queries IDs, so it also covers notes outside the visible window.
This bounds the initial load, but scrolling through the whole library can still
retain every visited note body.

The all-notes flow is cold: each export gets a fresh database snapshot and
`first()` releases its subscription. Export still collects all selected notes
in memory. Streaming attached files does not make the entire export pipeline
constant-memory. A change to paging would need to preserve filtering, pin
ordering, selection of unloaded notes, and export completeness.

Search uses Room's FTS4 entity and custom synchronization triggers. Replacing
FTS4 would require a migration, compatible SQLite support, and tests for the
existing ranking and filtering behavior.

## File ownership and deletion

A note's `imagePaths` manifest owns both images and document attachments.
Duplicates receive independent files. See [Attachments](attachments.md) for
imports, undo retention, and export, and [Vault](vault-and-backup.md) for encrypted
storage and PIN changes.

Deleting all ordinary notes queues their file paths in the same Room transaction
as the database deletion. `PendingImageCleanup` retries at startup and every
15 minutes while the app process runs. Failed entries remain queued; missing
files count as already removed. This queue does not scan for historical orphans
or run while Android has stopped the app.

## UI state and operations

ViewModels own operations and their results. `NoteOperationRunner` prevents
concurrent mutations and releases its busy state after failure or cancellation;
`OperationProgressDialog` displays that state. The editor uses `TextFieldState`
for body input, with a `TextFieldValue` bridge for existing editing actions.

UI text lives in Android string and plural resources. `StringProvider` supplies
localized messages outside Compose. User text, tags, file paths, and Markdown
syntax are data and must not be translated.
