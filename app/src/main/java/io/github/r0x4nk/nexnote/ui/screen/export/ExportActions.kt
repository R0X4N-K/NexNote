package io.github.r0x4nk.nexnote.ui.screen.export

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

internal data class ExportActions(
    val onScopeSelect: (ExportScope) -> Unit,
    val onDateRangeSelect: (Long, Long) -> Unit,
    val onFormatSelect: (ExportFormat) -> Unit,
    val onExportClick: () -> Unit
)

@Composable
internal fun rememberExportActions(
    uiState: ExportUiState,
    viewModel: ExportViewModel,
    context: Context,
    coroutineScope: CoroutineScope,
    exportManager: ExportManager
): ExportActions {
    val currentState by rememberUpdatedState(uiState)
    return remember(viewModel, context, coroutineScope, exportManager) {
        ExportActions(
            onScopeSelect = viewModel::selectScope,
            onDateRangeSelect = viewModel::selectDateRange,
            onFormatSelect = viewModel::selectFormat,
            onExportClick = {
                coroutineScope.launch {
                    exportCurrentSelection(
                        uiState = currentState,
                        viewModel = viewModel,
                        context = context,
                        exportManager = exportManager
                    )
                }
            }
        )
    }
}

private suspend fun exportCurrentSelection(
    uiState: ExportUiState,
    viewModel: ExportViewModel,
    context: Context,
    exportManager: ExportManager
) {
    viewModel.onExportStart()
    try {
        if (uiState.format == ExportFormat.PRINT) {
            exportManager.print(uiState.notes)
        } else {
            val intent = exportManager.buildShareIntent(uiState.notes, uiState.format)
            context.startActivity(Intent.createChooser(intent, "Share"))
        }
        viewModel.onExportComplete()
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        viewModel.onExportError("Export failed. Please try again.")
    }
}
