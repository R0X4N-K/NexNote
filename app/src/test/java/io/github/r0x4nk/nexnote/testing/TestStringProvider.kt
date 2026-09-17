package io.github.r0x4nk.nexnote.testing

import io.github.r0x4nk.nexnote.R
import io.github.r0x4nk.nexnote.di.StringProvider

/**
 * Deterministic [StringProvider] for JVM unit tests.
 *
 * Unit tests cannot load Android resources, so this maps the view-model string
 * ids used by the Trash, Vault and Editor view models back to their English
 * default values. Format arguments are applied with [String.format] so tests
 * can assert the same messages the app renders in the default locale.
 */
internal object TestStringProvider : StringProvider {

    override fun get(id: Int, vararg formatArgs: Any): String {
        val template = messages[id] ?: "string/$id"
        return if (formatArgs.isEmpty()) template else String.format(template, *formatArgs)
    }

    private val messages: Map<Int, String> = mapOf(
        R.string.trash_progress_restore_note to "Restoring note…",
        R.string.trash_progress_restore_notes to "Restoring notes…",
        R.string.trash_progress_delete_note to "Deleting note permanently…",
        R.string.trash_progress_empty to "Emptying trash…",
        R.string.trash_error_restore_note to "Could not restore the note. Try again.",
        R.string.trash_error_restore_notes to "Could not restore notes. Try again.",
        R.string.trash_error_delete_note to "Could not permanently delete the note. Try again.",
        R.string.trash_error_empty to "Could not empty the trash. Try again.",
        R.string.vault_error_generic to "Could not complete the operation. Try again.",
        R.string.vault_progress_move_note to "Moving note…",
        R.string.vault_message_removed to "Note removed from Vault",
        R.string.vault_error_remove to "Could not remove note from Vault",
        R.string.vault_progress_move_trash to "Moving notes to trash…",
        R.string.vault_error_move_trash to "Could not move note to trash",
        R.string.vault_error_restore to "Could not restore note",
        R.string.vault_error_pin to "Could not update note pin",
        R.string.vault_progress_duplicate to "Duplicating note…",
        R.string.vault_error_duplicate to "Could not duplicate note",
        R.string.vault_progress_restore to "Restoring note…",
        R.string.vault_progress_delete to "Deleting note permanently…",
        R.string.vault_message_deleted to "Note permanently deleted",
        R.string.vault_error_delete to "Could not delete note",
        R.string.vault_progress_move_to_vault to "Moving note to Vault…",
        R.string.vault_error_move_to_vault to "Could not move note to Vault",
        R.string.vault_message_moved to "Note moved to Vault",
        R.string.vault_message_duplicated to "Vault note duplicated",
        R.string.editor_error_resume_attachment to "Could not resume attachment",
        R.string.editor_error_text_too_long to "Text too long (max %1\$dk characters)",
        R.string.editor_progress_move_to_trash to "Moving note to trash.",
        R.string.editor_error_move_to_trash to "Could not move note to trash. Try again.",
        R.string.editor_progress_duplicate to "Duplicating note.",
        R.string.editor_error_duplicate to "Could not duplicate note. Try again.",
        R.string.editor_error_save_attachment to "Could not save attachment",
        R.string.editor_error_attach_file to "Could not attach file",
        R.string.editor_error_insert_image to "Could not insert image",
        R.string.editor_error_remove_image to "Could not remove image",
        R.string.editor_error_note_not_found to "Note not found",
        R.string.editor_error_vault_note_unavailable to "Vault note not available",
        R.string.editor_error_template_not_found to "Template not found",
        R.string.editor_error_save_failed to "Save failed",
        R.string.editor_default_template_name to "Template",
        R.string.editor_vault_locked_title to "Vault locked",
        R.string.templates_progress_delete to "Deleting templates…",
        R.string.templates_error_delete to "Could not delete templates",
        R.string.templates_error_delete_single to "Could not delete template",
        R.string.untitled_note to "Untitled note",
        R.string.note_op_generic_error to "Could not complete the operation. Try again.",
        R.string.note_op_duplicate_error to "Could not duplicate note",
        R.string.note_op_duplicated to "Duplicated \"%1\$s\"",
        R.string.note_op_duplicate_failed to "Could not duplicate \"%1\$s\"",
        R.string.note_op_update_date_progress to "Updating creation date.",
        R.string.note_op_date_updated to "Creation date updated",
        R.string.note_op_date_update_failed to "Could not update the creation date",
        R.string.home_error_load_selected to "Could not load selected notes. Please try again.",
        R.string.vault_progress_move_trash to "Moving notes to trash…",
        R.string.imported_note_title to "Imported note",
        R.string.import_error_unsupported_encoding to "Unsupported file encoding",
        R.string.import_error_file_too_large to "File is too large",
        R.string.import_error_not_text to "File does not look like text",
        R.string.markdown_image_alt_placeholder to "image",
        R.string.markdown_placeholder_text to "text",
        R.string.markdown_placeholder_code to "code",
        R.string.export_error_load to "Could not load notes. Try again.",
        R.string.export_default_file_name to "Note",
        R.string.share_subject to "NexNote note",
        R.string.share_subject_many to "%1\$d NexNote notes"
    )
}
