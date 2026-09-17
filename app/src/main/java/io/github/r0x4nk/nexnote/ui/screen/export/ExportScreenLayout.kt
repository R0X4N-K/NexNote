package io.github.r0x4nk.nexnote.ui.screen.export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import io.github.r0x4nk.nexnote.ui.theme.nexNoteBackground
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.R
import io.github.r0x4nk.nexnote.ui.component.OperationLoadingState
import io.github.r0x4nk.nexnote.ui.component.OperationProgressDialog

@Composable
internal fun ExportScreenLayout(
    uiState: ExportUiState,
    hasInitialNote: Boolean,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    actions: ExportActions
) {
    OperationProgressDialog(
        stringResource(R.string.export_preparing).takeIf { uiState.isExporting }
    )
    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.nexNoteBackground(),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { ExportTopBar(onBack = onBack) }
    ) { padding ->
        if (uiState.isLoading) {
            ExportLoadingState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        } else {
            ExportScreenContent(
                uiState = uiState,
                hasInitialNote = hasInitialNote,
                actions = actions,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun ExportLoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        OperationLoadingState()
    }
}

@Composable
private fun ExportScreenContent(
    uiState: ExportUiState,
    hasInitialNote: Boolean,
    actions: ExportActions,
    modifier: Modifier = Modifier
) {
    val includeMediaDescription = stringResource(R.string.export_include_media)
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        ExportSection {
            ExportScopeSelector(
                selectedScope = uiState.scope,
                hasInitialNote = hasInitialNote,
                onScopeSelect = actions.onScopeSelect
            )
        }
        if (uiState.scope == ExportScope.DateRange) {
            ExportSection {
                DateRangeSelector(
                    dateFrom = uiState.dateFrom,
                    dateTo = uiState.dateTo,
                    onRangeSelected = actions.onDateRangeSelect
                )
            }
        }
        ExportSection {
            ExportFormatSelector(
                selectedFormat = uiState.format,
                onFormatSelect = actions.onFormatSelect
            )
            Spacer(Modifier.height(12.dp))
            ExportSummary(noteCount = uiState.notes.size)
            ListItem(
                headlineContent = { Text(includeMediaDescription) },
                supportingContent = {
                    Text(
                        stringResource(
                            if (uiState.format.embedsImages) {
                                R.string.export_include_media_hint_files
                            } else {
                                R.string.export_include_media_hint_text
                            }
                        )
                    )
                },
                trailingContent = {
                    Switch(
                        checked = uiState.includeMedia,
                        onCheckedChange = actions.onIncludeMediaChange,
                        enabled = !uiState.isExporting,
                        modifier = Modifier.semantics {
                            contentDescription = includeMediaDescription
                        }
                    )
                }
            )
            if (uiState.includeMedia) Text(
                text = stringResource(
                    if (uiState.format == ExportFormat.PRINT) R.string.attachment_print_hint
                    else R.string.attachment_export_hint
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(4.dp))
        ExportButton(
            uiState = uiState,
            noteCount = uiState.notes.size,
            onClick = actions.onExportClick
        )
    }
}

@Composable
private fun ExportSection(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportScopeSelector(
    selectedScope: ExportScope,
    hasInitialNote: Boolean,
    onScopeSelect: (ExportScope) -> Unit
) {
    SectionLabel(stringResource(R.string.export_scope_label))

    val scopeOptions = buildList {
        if (hasInitialNote) add(ExportScope.SingleNote)
        add(ExportScope.DateRange)
        add(ExportScope.AllNotes)
    }
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        scopeOptions.forEachIndexed { idx, scope ->
            SegmentedButton(
                selected = selectedScope == scope,
                onClick = { onScopeSelect(scope) },
                shape = SegmentedButtonDefaults.itemShape(idx, scopeOptions.size),
                colors = exportSegmentedButtonColors(),
                label = { Text(scope.label) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportFormatSelector(
    selectedFormat: ExportFormat,
    onFormatSelect: (ExportFormat) -> Unit
) {
    SectionLabel(stringResource(R.string.export_format_label))

    val formatOptions = ExportFormat.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        formatOptions.forEachIndexed { idx, format ->
            SegmentedButton(
                selected = selectedFormat == format,
                onClick = { onFormatSelect(format) },
                shape = SegmentedButtonDefaults.itemShape(idx, formatOptions.size),
                colors = exportSegmentedButtonColors(),
                label = { Text(format.label) }
            )
        }
    }
}

@Composable
private fun ExportSummary(noteCount: Int) {
    Text(
        text = if (noteCount == 0) {
            stringResource(R.string.export_no_notes_selected)
        } else {
            pluralStringResource(R.plurals.export_notes_selected, noteCount, noteCount)
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(
            alpha = if (noteCount == 0) 0.45f else 0.7f
        )
    )
}

@Composable
private fun ExportButton(
    uiState: ExportUiState,
    noteCount: Int,
    onClick: () -> Unit
) {
    Button(
        modifier = Modifier.fillMaxWidth(),
        enabled = noteCount > 0 && !uiState.isExporting,
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        if (uiState.isExporting) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
            )
        } else {
            Text(
                if (uiState.format == ExportFormat.PRINT) {
                    stringResource(R.string.export_format_print)
                } else {
                    stringResource(R.string.export_title)
                }
            )
        }
    }
}

@Composable
private fun exportSegmentedButtonColors() = SegmentedButtonDefaults.colors(
    activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
    activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    activeBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.48f),
    inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    inactiveBorderColor = MaterialTheme.colorScheme.outlineVariant
)