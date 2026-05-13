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
    data object NewNote : EditorMode()
    data class ExistingNote(val noteId: Long) : EditorMode()
    data class NewFromTemplate(val templateId: Long) : EditorMode()
    data object NewTemplate : EditorMode()
    data class EditTemplate(val templateId: Long) : EditorMode()

    internal val startsWithLoading: Boolean
        get() = this !is NewNote

    internal val initialContentAnimationsEnabled: Boolean
        get() = this !is ExistingNote

    internal val routeNoteId: Long
        get() = when (this) {
            is ExistingNote -> noteId
            is EditTemplate,
            is NewFromTemplate,
            NewNote,
            NewTemplate -> NO_ID
        }

    internal val routeTemplateId: Long
        get() = when (this) {
            is NewFromTemplate -> templateId
            is EditTemplate,
            is ExistingNote,
            NewNote,
            NewTemplate -> NO_ID
        }

    internal val routeEditTemplateId: Long
        get() = when (this) {
            NewTemplate -> NEW_TEMPLATE_ID
            is EditTemplate -> templateId
            is ExistingNote,
            is NewFromTemplate,
            NewNote -> NO_ID
        }

    internal fun debugRouteSummary(): String {
        return "mode=$this routeNoteId=$routeNoteId routeTemplateId=$routeTemplateId " +
            "routeEditTemplateId=$routeEditTemplateId"
    }

    companion object {
        /** Sentinel: entity not yet persisted in DB, or route parameter not used. */
        const val NO_ID = 0L

        /** Route sentinel for opening the template editor in create mode. */
        const val NEW_TEMPLATE_ID = -1L

        fun fromRoute(
            noteId: Long = NO_ID,
            templateId: Long = NO_ID,
            editTemplateId: Long = NO_ID
        ): EditorMode = when {
            editTemplateId == NEW_TEMPLATE_ID -> NewTemplate
            editTemplateId != NO_ID -> EditTemplate(editTemplateId)
            noteId != NO_ID -> ExistingNote(noteId)
            templateId != NO_ID -> NewFromTemplate(templateId)
            else -> NewNote
        }
    }
}
