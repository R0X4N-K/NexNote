package io.github.r0x4nk.nexnote.ui.navigation

/**
 * Route definitions for the app.
 *
 * Bottom nav: Home, Agenda, Templates, Settings
 * Back stack: Editor (new / existing / from template / template editing), Trash, Vault
 *
 * Editor routes serialize primitive query parameters for Navigation, then
 * `AppNavGraph` maps them to `EditorMode` before entering the editor feature.
 *
 * Editor query parameters:
 *   noteId          = 0   → new note
 *   noteId          > 0   → open existing note
 *   templateId      > 0   → new note pre-filled from template
 *   editTemplateId  = -1  → open editor to create a brand-new template
 *   editTemplateId  > 0   → open editor to edit an existing template
 *   creationDate    > 0   → initial creation date for a brand-new note
 *   vaultNoteId     = -1  → create a brand-new Vault note through the Vault-only path
 *   vaultNoteId     = -1 and templateId > 0 → create a Vault note from a template
 *   vaultNoteId     > 0   → open an existing Vault note through the Vault-only path
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

    data object Vault : Screen("vault?moveNoteId={moveNoteId}") {
        const val ARG_MOVE_NOTE_ID = "moveNoteId"

        fun route(moveNoteId: Long = NO_ID): String = "vault?moveNoteId=$moveNoteId"

        fun moveNoteRoute(noteId: Long): String = route(moveNoteId = noteId)
    }

    data object Export : Screen("export?noteId={noteId}") {
        const val ARG_NOTE_ID = "noteId"
        fun route(noteId: Long = NO_ID) = "export?noteId=$noteId"
    }

    data object Editor : Screen(
        "editor?noteId={noteId}&templateId={templateId}" +
            "&editTemplateId={editTemplateId}&creationDate={creationDate}" +
            "&vaultNoteId={vaultNoteId}"
    ) {
        const val ARG_NOTE_ID          = "noteId"
        const val ARG_TEMPLATE_ID      = "templateId"
        const val ARG_EDIT_TEMPLATE_ID = "editTemplateId"
        const val ARG_CREATION_DATE    = "creationDate"
        const val ARG_VAULT_NOTE_ID    = "vaultNoteId"
        const val NEW_VAULT_NOTE_ID    = -1L

        fun route(
            noteId: Long         = NO_ID,
            templateId: Long     = NO_ID,
            editTemplateId: Long = NO_ID,
            creationDate: Long   = NO_CREATION_DATE,
            vaultNoteId: Long    = NO_ID
        ): String = "editor?noteId=$noteId&templateId=$templateId" +
            "&editTemplateId=$editTemplateId&creationDate=$creationDate" +
            "&vaultNoteId=$vaultNoteId"

        fun newNoteRoute(creationDate: Long = NO_CREATION_DATE): String =
            route(creationDate = creationDate)

        fun existingNoteRoute(noteId: Long): String = route(noteId = noteId)

        fun fromTemplateRoute(templateId: Long): String = route(templateId = templateId)

        fun newTemplateRoute(): String = route(editTemplateId = NEW_TEMPLATE_ID)

        fun editTemplateRoute(templateId: Long): String = route(editTemplateId = templateId)

        fun vaultNoteRoute(noteId: Long): String = route(vaultNoteId = noteId)

        fun newVaultNoteRoute(): String = route(vaultNoteId = NEW_VAULT_NOTE_ID)

        fun newVaultNoteFromTemplateRoute(templateId: Long): String =
            route(templateId = templateId, vaultNoteId = NEW_VAULT_NOTE_ID)
    }

    companion object {
        /** Sentinel: entity not yet persisted in DB, or parameter not used. */
        const val NO_ID = 0L

        /** Sentinel: no route-provided creation date. */
        const val NO_CREATION_DATE = 0L

        /**
         * editTemplateId sentinel: open the editor to create a brand-new
         * template (as opposed to editing an existing one, which uses the
         * template's real database id).
         */
        const val NEW_TEMPLATE_ID = -1L
    }
}
