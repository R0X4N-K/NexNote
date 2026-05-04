package com.example.nexnote.ui.navigation

/**
 * Route definitions for the app.
 *
 * Bottom nav: Home, Agenda, Templates, Settings
 * Back stack: Editor (new / existing / from template / template editing), Trash
 *
 * Editor uses optional query parameters:
 *   noteId          = 0   → new note
 *   noteId          > 0   → open existing note
 *   templateId      > 0   → new note pre-filled from template
 *   editTemplateId  = -1  → open editor to create a brand-new template
 *   editTemplateId  > 0   → open editor to edit an existing template
 */
sealed class Screen(val route: String) {

    // ── Bottom nav ────────────────────────────────────────────────────────────

    data object Home      : Screen("home")
    data object Agenda    : Screen("agenda")
    data object Tags      : Screen("tags")
    data object Templates : Screen("templates")
    data object Settings  : Screen("settings")

    // ── Back stack ────────────────────────────────────────────────────────────

    data object Trash : Screen("trash")

    data object Export : Screen("export?noteId={noteId}") {
        const val ARG_NOTE_ID = "noteId"
        fun route(noteId: Long = NO_ID) = "export?noteId=$noteId"
    }

    data object Editor : Screen(
        "editor?noteId={noteId}&templateId={templateId}&editTemplateId={editTemplateId}"
    ) {
        const val ARG_NOTE_ID          = "noteId"
        const val ARG_TEMPLATE_ID      = "templateId"
        const val ARG_EDIT_TEMPLATE_ID = "editTemplateId"

        fun route(
            noteId: Long         = NO_ID,
            templateId: Long     = NO_ID,
            editTemplateId: Long = NO_ID
        ): String = "editor?noteId=$noteId&templateId=$templateId&editTemplateId=$editTemplateId"
    }

    companion object {
        /** Sentinel: entity not yet persisted in DB, or parameter not used. */
        const val NO_ID = 0L

        /**
         * editTemplateId sentinel: open the editor to create a brand-new
         * template (as opposed to editing an existing one, which uses the
         * template's real database id).
         */
        const val NEW_TEMPLATE_ID = -1L
    }
}
