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
import io.github.r0x4nk.nexnote.ui.screen.statistics.StatisticsScreen
import io.github.r0x4nk.nexnote.ui.screen.tags.TagsScreen
import io.github.r0x4nk.nexnote.ui.screen.templates.TemplatesScreen
import io.github.r0x4nk.nexnote.ui.screen.trash.TrashScreen
import io.github.r0x4nk.nexnote.ui.screen.vault.VaultScreen

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
        backStackDestinations(navController, floatingBottomPadding)
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
    settingsDestination(navController, floatingBottomPadding)
}

private fun NavGraphBuilder.backStackDestinations(
    navController: NavHostController,
    floatingBottomPadding: Dp
) {
    trashDestination(navController)
    statisticsDestination(navController)
    vaultDestination(navController, floatingBottomPadding)
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
            onOpenStatistics = {
                navController.navigate(Screen.Statistics.route)
            },
            onOpenVault = {
                navController.navigate(Screen.Vault.route())
            },
            onMoveNoteToVault = { noteId ->
                navController.navigate(Screen.Vault.moveNoteRoute(noteId))
            },
            floatingBottomPadding = floatingBottomPadding
        )
    }
}

private fun NavGraphBuilder.statisticsDestination(navController: NavHostController) {
    composable(
        route              = Screen.Statistics.route,
        enterTransition    = { forwardEnterTransition() },
        exitTransition     = { forwardExitTransition() },
        popEnterTransition = { backwardEnterTransition() },
        popExitTransition  = { forwardExitTransition() }
    ) {
        StatisticsScreen(onBack = { navController.popBackStack() })
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
            onNewNote = { creationDate ->
                navController.navigate(Screen.Editor.newNoteRoute(creationDate))
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

private fun NavGraphBuilder.settingsDestination(
    navController: NavHostController,
    floatingBottomPadding: Dp
) {
    composable(Screen.Settings.route) {
        SettingsScreen(
            onOpenVault = {
                navController.navigate(Screen.Vault.route())
            },
            floatingBottomPadding = floatingBottomPadding
        )
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

private fun NavGraphBuilder.vaultDestination(
    navController: NavHostController,
    floatingBottomPadding: Dp
) {
    composable(
        route              = Screen.Vault.route,
        enterTransition    = { forwardEnterTransition() },
        exitTransition     = { forwardExitTransition() },
        popEnterTransition = { backwardEnterTransition() },
        popExitTransition  = { forwardExitTransition() },
        arguments          = vaultArguments()
    ) { backStack ->
        VaultScreen(
            pendingMoveNoteId = backStack.vaultMoveNoteId(),
            onBack = { navController.popBackStack() },
            onCreateVaultNote = {
                navController.navigate(Screen.Editor.newVaultNoteRoute())
            },
            onCreateVaultNoteFromTemplate = { templateId ->
                navController.navigate(Screen.Editor.newVaultNoteFromTemplateRoute(templateId))
            },
            onNoteClick = { noteId ->
                navController.navigate(Screen.Editor.vaultNoteRoute(noteId))
            },
            floatingBottomPadding = floatingBottomPadding
        )
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
        is EditorMode.NewVaultFromTemplate,
        EditorMode.NewVaultNote,
        is EditorMode.NewNote,
        is EditorMode.VaultNote,
        EditorMode.NewTemplate -> null
    }

private fun exportArguments() = listOf(
    navArgument(Screen.Export.ARG_NOTE_ID) {
        type         = NavType.LongType
        defaultValue = Screen.NO_ID
    }
)

private fun vaultArguments() = listOf(
    navArgument(Screen.Vault.ARG_MOVE_NOTE_ID) {
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
    },
    navArgument(Screen.Editor.ARG_CREATION_DATE) {
        type         = NavType.LongType
        defaultValue = Screen.NO_CREATION_DATE
    },
    navArgument(Screen.Editor.ARG_VAULT_NOTE_ID) {
        type         = NavType.LongType
        defaultValue = Screen.NO_ID
    }
)

private fun NavBackStackEntry.exportNoteId(): Long =
    arguments?.getLong(Screen.Export.ARG_NOTE_ID) ?: Screen.NO_ID

private fun NavBackStackEntry.vaultMoveNoteId(): Long =
    arguments?.getLong(Screen.Vault.ARG_MOVE_NOTE_ID) ?: Screen.NO_ID

private fun NavBackStackEntry.editorArgs(): EditorRouteArgs = EditorRouteArgs(
    noteId = arguments?.getLong(Screen.Editor.ARG_NOTE_ID) ?: Screen.NO_ID,
    templateId = arguments?.getLong(Screen.Editor.ARG_TEMPLATE_ID) ?: Screen.NO_ID,
    editTemplateId = arguments?.getLong(Screen.Editor.ARG_EDIT_TEMPLATE_ID) ?: Screen.NO_ID,
    creationDate = arguments?.getLong(Screen.Editor.ARG_CREATION_DATE)
        ?: Screen.NO_CREATION_DATE,
    vaultNoteId = arguments?.getLong(Screen.Editor.ARG_VAULT_NOTE_ID) ?: Screen.NO_ID
)

private data class EditorRouteArgs(
    val noteId: Long,
    val templateId: Long,
    val editTemplateId: Long,
    val creationDate: Long,
    val vaultNoteId: Long
) {
    fun toEditorMode(): EditorMode = EditorMode.fromRoute(
        noteId = noteId,
        templateId = templateId,
        editTemplateId = editTemplateId,
        creationDate = creationDate,
        vaultNoteId = vaultNoteId
    )
}
