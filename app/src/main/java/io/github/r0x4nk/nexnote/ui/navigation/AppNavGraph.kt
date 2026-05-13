package io.github.r0x4nk.nexnote.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import io.github.r0x4nk.nexnote.ui.screen.agenda.AgendaScreen
import io.github.r0x4nk.nexnote.ui.screen.editor.EditorMode
import io.github.r0x4nk.nexnote.ui.screen.editor.EditorScreen
import io.github.r0x4nk.nexnote.ui.screen.export.ExportScreen
import io.github.r0x4nk.nexnote.ui.screen.home.HomeScreen
import io.github.r0x4nk.nexnote.ui.screen.settings.SettingsScreen
import io.github.r0x4nk.nexnote.ui.screen.tags.TagsScreen
import io.github.r0x4nk.nexnote.ui.screen.templates.TemplatesScreen
import io.github.r0x4nk.nexnote.ui.screen.trash.TrashScreen

@Composable
internal fun AppNavHost(
    navController: NavHostController,
    floatingBottomPadding: Dp = 0.dp
) {
    NavHost(
        navController      = navController,
        startDestination   = Screen.Home.route,
        modifier           = Modifier.fillMaxSize(),
        enterTransition    = { tabEnterTransition() },
        exitTransition     = { tabExitTransition() },
        popEnterTransition = { tabEnterTransition() },
        popExitTransition  = { tabExitTransition() }
    ) {
        bottomNavDestinations(navController, floatingBottomPadding)
        backStackDestinations(navController)
    }
}

private fun NavGraphBuilder.bottomNavDestinations(
    navController: NavHostController,
    floatingBottomPadding: Dp
) {
    homeDestination(navController, floatingBottomPadding)
    agendaDestination(navController, floatingBottomPadding)
    tagsDestination(navController, floatingBottomPadding)
    templatesDestination(navController, floatingBottomPadding)
    settingsDestination()
}

private fun NavGraphBuilder.backStackDestinations(navController: NavHostController) {
    trashDestination(navController)
    exportDestination(navController)
    editorDestination(navController)
}

private fun NavGraphBuilder.homeDestination(
    navController: NavHostController,
    floatingBottomPadding: Dp
) {
    composable(Screen.Home.route) {
        HomeScreen(
            onNoteClick = { noteId ->
                navController.navigate(Screen.Editor.existingNoteRoute(noteId))
            },
            onNewNote = {
                navController.navigate(Screen.Editor.newNoteRoute())
            },
            onNewNoteFromTemplate = { templateId ->
                navController.navigate(Screen.Editor.fromTemplateRoute(templateId))
            },
            onOpenTrash = {
                navController.navigate(Screen.Trash.route)
            },
            floatingBottomPadding = floatingBottomPadding
        )
    }
}

private fun NavGraphBuilder.agendaDestination(
    navController: NavHostController,
    floatingBottomPadding: Dp
) {
    composable(Screen.Agenda.route) {
        AgendaScreen(
            onNoteClick = { noteId ->
                navController.navigate(Screen.Editor.existingNoteRoute(noteId))
            },
            floatingBottomPadding = floatingBottomPadding
        )
    }
}

private fun NavGraphBuilder.tagsDestination(
    navController: NavHostController,
    floatingBottomPadding: Dp
) {
    composable(Screen.Tags.route) {
        TagsScreen(
            onNoteClick = { noteId ->
                navController.navigate(Screen.Editor.existingNoteRoute(noteId))
            },
            floatingBottomPadding = floatingBottomPadding
        )
    }
}

private fun NavGraphBuilder.templatesDestination(
    navController: NavHostController,
    floatingBottomPadding: Dp
) {
    composable(Screen.Templates.route) {
        TemplatesScreen(
            onNavigateToApplyTemplate = { templateId ->
                navController.navigate(Screen.Editor.fromTemplateRoute(templateId))
            },
            onNavigateToEditTemplate = { editTemplateId ->
                val route = if (editTemplateId == Screen.NEW_TEMPLATE_ID) {
                    Screen.Editor.newTemplateRoute()
                } else {
                    Screen.Editor.editTemplateRoute(editTemplateId)
                }
                navController.navigate(route)
            },
            floatingBottomPadding = floatingBottomPadding
        )
    }
}

private fun NavGraphBuilder.settingsDestination() {
    composable(Screen.Settings.route) {
        SettingsScreen()
    }
}

private fun NavGraphBuilder.trashDestination(navController: NavHostController) {
    composable(
        route              = Screen.Trash.route,
        enterTransition    = { forwardEnterTransition() },
        exitTransition     = { forwardExitTransition() },
        popEnterTransition = { backwardEnterTransition() },
        popExitTransition  = { forwardExitTransition() }
    ) {
        TrashScreen(navController = navController)
    }
}

private fun NavGraphBuilder.exportDestination(navController: NavHostController) {
    composable(
        route              = Screen.Export.route,
        enterTransition    = { forwardEnterTransition() },
        exitTransition     = { forwardExitTransition() },
        popEnterTransition = { backwardEnterTransition() },
        popExitTransition  = { forwardExitTransition() },
        arguments          = exportArguments()
    ) { backStack ->
        ExportScreen(
            noteId = backStack.exportNoteId(),
            onBack = { navController.popBackStack() }
        )
    }
}

private fun NavGraphBuilder.editorDestination(navController: NavHostController) {
    composable(
        route              = Screen.Editor.route,
        enterTransition    = { forwardEnterTransition() },
        exitTransition     = { forwardExitTransition() },
        popEnterTransition = { backwardEnterTransition() },
        popExitTransition  = { forwardExitTransition() },
        arguments          = editorArguments()
    ) { backStack ->
        EditorDestination(
            args = backStack.editorArgs(),
            navController = navController
        )
    }
}

@Composable
private fun EditorDestination(
    args: EditorRouteArgs,
    navController: NavHostController
) {
    val mode = args.toEditorMode()
    EditorScreen(
        mode          = mode,
        navController = navController,
        onExport      = editorExportAction(mode, navController)
    )
}

private fun editorExportAction(
    mode: EditorMode,
    navController: NavHostController
): (() -> Unit)? =
    when (mode) {
        is EditorMode.ExistingNote -> {
            { navController.navigate(Screen.Export.route(noteId = mode.noteId)) }
        }
        is EditorMode.EditTemplate,
        is EditorMode.NewFromTemplate,
        EditorMode.NewNote,
        EditorMode.NewTemplate -> null
    }

private fun exportArguments() = listOf(
    navArgument(Screen.Export.ARG_NOTE_ID) {
        type         = NavType.LongType
        defaultValue = Screen.NO_ID
    }
)

private fun editorArguments() = listOf(
    navArgument(Screen.Editor.ARG_NOTE_ID) {
        type         = NavType.LongType
        defaultValue = Screen.NO_ID
    },
    navArgument(Screen.Editor.ARG_TEMPLATE_ID) {
        type         = NavType.LongType
        defaultValue = Screen.NO_ID
    },
    navArgument(Screen.Editor.ARG_EDIT_TEMPLATE_ID) {
        type         = NavType.LongType
        defaultValue = Screen.NO_ID
    }
)

private fun NavBackStackEntry.exportNoteId(): Long =
    arguments?.getLong(Screen.Export.ARG_NOTE_ID) ?: Screen.NO_ID

private fun NavBackStackEntry.editorArgs(): EditorRouteArgs = EditorRouteArgs(
    noteId = arguments?.getLong(Screen.Editor.ARG_NOTE_ID) ?: Screen.NO_ID,
    templateId = arguments?.getLong(Screen.Editor.ARG_TEMPLATE_ID) ?: Screen.NO_ID,
    editTemplateId = arguments?.getLong(Screen.Editor.ARG_EDIT_TEMPLATE_ID) ?: Screen.NO_ID
)

private data class EditorRouteArgs(
    val noteId: Long,
    val templateId: Long,
    val editTemplateId: Long
) {
    fun toEditorMode(): EditorMode = EditorMode.fromRoute(
        noteId = noteId,
        templateId = templateId,
        editTemplateId = editTemplateId
    )
}
