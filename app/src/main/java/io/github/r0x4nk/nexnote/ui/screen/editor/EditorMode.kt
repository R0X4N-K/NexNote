package io.github.r0x4nk.nexnote.ui.screen.editor

/**
 * Describes why the editor is being opened.
 *
 * Navigation still serializes primitive ids in the route, but the editor
 * feature should reason about intent: creating a note, loading a note,
 * applying a template, or editing templates. Keeping those alternatives closed
 * makes the initial load path exhaustive and avoids threading three raw ids
 * through the ViewModel.
 */
sealed class EditorMode {
    data class NewNote(val initialCreationDate: Long? = null) : EditorMode()
    data object NewVaultNote : EditorMode()
    data class ExistingNote(val noteId: Long) : EditorMode()
    data class VaultNote(val noteId: Long) : EditorMode()
    data class NewFromTemplate(val templateId: Long) : EditorMode()
    data class NewVaultFromTemplate(val templateId: Long) : EditorMode()
    data object NewTemplate : EditorMode()
    data class EditTemplate(val templateId: Long) : EditorMode()

    internal val startsWithLoading: Boolean
        get() = this !is NewNote && this != NewVaultNote

    internal val initialContentAnimationsEnabled: Boolean
        get() = this !is ExistingNote && this !is VaultNote

    internal val isReadOnly: Boolean
        get() = false

    internal val isVaultNote: Boolean
        get() = this is VaultNote || this == NewVaultNote || this is NewVaultFromTemplate

    internal val routeNoteId: Long
        get() = when (this) {
            is ExistingNote -> noteId
            is EditTemplate,
            is NewFromTemplate,
            is NewVaultFromTemplate,
            is VaultNote,
            NewVaultNote,
            is NewNote,
            NewTemplate -> NO_ID
        }

    internal val routeTemplateId: Long
        get() = when (this) {
            is NewFromTemplate -> templateId
            is NewVaultFromTemplate -> templateId
            is EditTemplate,
            is ExistingNote,
            is VaultNote,
            NewVaultNote,
            is NewNote,
            NewTemplate -> NO_ID
        }

    internal val routeEditTemplateId: Long
        get() = when (this) {
            NewTemplate -> NEW_TEMPLATE_ID
            is EditTemplate -> templateId
            is ExistingNote,
            is NewFromTemplate,
            is NewVaultFromTemplate,
            is VaultNote,
            NewVaultNote,
            is NewNote -> NO_ID
        }

    internal val routeCreationDate: Long
        get() = when (this) {
            is NewNote -> initialCreationDate ?: NO_CREATION_DATE
            is EditTemplate,
            is ExistingNote,
            is NewFromTemplate,
            is NewVaultFromTemplate,
            is VaultNote,
            NewVaultNote,
            NewTemplate -> NO_CREATION_DATE
        }

    internal fun debugRouteSummary(): String {
        return "mode=$this routeNoteId=$routeNoteId routeTemplateId=$routeTemplateId " +
            "routeEditTemplateId=$routeEditTemplateId routeCreationDate=$routeCreationDate"
    }

    companion object {
        /** Sentinel: entity not yet persisted in DB, or route parameter not used. */
        const val NO_ID = 0L

        /** Route sentinel for opening the template editor in create mode. */
        const val NEW_TEMPLATE_ID = -1L

        /** Route sentinel for opening the editor to create a Vault note. */
        const val NEW_VAULT_NOTE_ID = -1L

        /** Sentinel: no route-provided creation date. */
        const val NO_CREATION_DATE = 0L

        fun fromRoute(
            noteId: Long = NO_ID,
            templateId: Long = NO_ID,
            editTemplateId: Long = NO_ID,
            creationDate: Long = NO_CREATION_DATE,
            vaultNoteId: Long = NO_ID
        ): EditorMode = when {
            vaultNoteId == NEW_VAULT_NOTE_ID && templateId != NO_ID ->
                NewVaultFromTemplate(templateId)
            vaultNoteId == NEW_VAULT_NOTE_ID -> NewVaultNote
            vaultNoteId != NO_ID -> VaultNote(vaultNoteId)
            editTemplateId == NEW_TEMPLATE_ID -> NewTemplate
            editTemplateId != NO_ID -> EditTemplate(editTemplateId)
            noteId != NO_ID -> ExistingNote(noteId)
            templateId != NO_ID -> NewFromTemplate(templateId)
            else -> NewNote(creationDate.takeIf { it != NO_CREATION_DATE })
        }
    }
}
