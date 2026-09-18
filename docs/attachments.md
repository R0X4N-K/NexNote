# Attachments, imports, and export

## Adding a file to a note

Use the editor's paperclip to choose a photo or a document. NexNote copies the
file into its private storage, so the note does not depend on the original
remaining available.

Photos go through the image importer, which handles orientation and downsampling.
Documents, audio, archives, and other files chosen as attachments are copied
without changing their bytes. There is no app-imposed quota on document size or
attachment count, but copies and exports need free disk space. The image importer
applies a 25 MiB limit when copying unmodified image bytes;
images needing rotation or downsampling use a separate decode-and-reencode path.

A document appears as a card with its name, file type, and size. Tap it to open
a temporary copy in an installed viewer. NexNote does not contain a document or
audio player. Vault cards hide file size and cannot open files externally;
see [Vault and backup](vault-and-backup.md#adding-attachments).

Removing a link hides its card. For ordinary notes, NexNote keeps the file while
the editor's undo history can restore it. When that history is discarded, it
saves the updated file list before attempting to delete unused files. Vault,
template, and read-only editors do not use this pruning step. Permanently
deleting a note also deletes its owned files. Duplicates have independent copies.

## Opening text files and receiving shares

Opening a supported text file with NexNote creates a new note containing its
text. It does not attach the original file or restore an exported database.
Supported types include plain text, Markdown, CSV/TSV, JSON, XML, YAML, and TOML.
Files must be valid UTF-8; a UTF-8 BOM is accepted. The importer limits input
to 2,000,003 bytes and 500,000 UTF-16 code units, and rejects binary control
characters other than tabs and line breaks. The filename supplies the title.

Android's Share action accepts plain text or one image, optionally with text.
The shared subject supplies the title. Each deliberate share creates a new
ordinary note. Multiple-item shares, HTML-only shares, and shared document
streams are not supported. To attach a document, use the paperclip in its note.

Shared text uses Markdown, like notes created in the editor and imported text
files. Starting with 1.0.5, note cards also render Markdown for notes previously
shared into 1.0.4; their stored text and modification dates are unchanged.

The two paths have different limits: shared text and its subject are each
checked against the byte limit above; the text-file character limit applies
to opened files, not to shared text.

## Exporting and printing

You can export selected notes as TXT, Markdown, or PDF, or print them.
**Include media** is enabled by default.

| Content and option | Shared output |
| --- | --- |
| At least one referenced document attachment, media included | ZIP with the note export and referenced files |
| No document attachments, media included | Direct TXT, Markdown, or PDF file |
| Media excluded, TXT or Markdown | Direct file with media references replaced by labels |
| Media excluded, PDF | Direct PDF with images still embedded and document links reduced to labels |

In a ZIP, Markdown links are rewritten to archive paths. Referenced images are
included too; files retained only for undo are left out. A missing or encrypted
referenced file makes the bundle fail.

For an image-only note, TXT and Markdown export do not package the image files.
PDF and printing keep embedded images even when **Include media** is off.
Document attachments are represented by labels or cards, never merged into the
PDF or printed as documents. Excluding media preserves ordinary web links and
code and does not change the stored note.

Exports and files opened in another app use snapshots in the app cache. Files
older than 24 hours are cleaned up on a later launch or export; Android may
remove them sooner. ZIP export is not a restorable backup of NexNote.

## Implementation notes

`NoteAttachmentStorage` stores documents under randomized paths in
`images/attachments/`. Original names are kept in Markdown link labels.
The existing `imagePaths` manifest owns both images and documents; use
`copyStoredNoteFile` when copying a stored file.

Imports write a journal marker and a `.part` file, sync the completed data, and
publish it by atomic rename. Recovery checks previous-session markers at startup
and after Vault unlock. It removes partial or unowned imports and preserves
referenced files. Unreadable Vault manifests defer cleanup. This covers new
document imports, not every historical orphan or interrupted PIN change.

Document copies use streaming IO. Preview cards read metadata, not document
contents. Vault encryption uses the [version 2 file format](vault-file-streaming.md).
Note text, image decoding, and export formatting can still allocate whole buffers.

`ExternalImportViewModel` queues incoming imports and survives activity
recreation. `MainActivity` uses `singleTask` and `documentLaunchMode="never"`
to reuse its task through `onNewIntent`. Recreation must not import the launch
intent again or display an imported note through another note's editor state.

See [Running tests](testing.md) for the import, export, recovery, and Vault suites.
