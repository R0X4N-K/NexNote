package io.github.r0x4nk.nexnote.ui.screen.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.activity.ComponentActivity
import android.content.ContextWrapper
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.r0x4nk.nexnote.R
import io.github.r0x4nk.nexnote.domain.usecase.GenerateDebugNotesUseCase

internal const val SETTINGS_DEVELOPER_NOTE_COUNT_FIELD_TAG =
    "settings_developer_note_count_field"
internal const val SETTINGS_DEVELOPER_GENERATE_BUTTON_TAG =
    "settings_developer_generate_button"

/** Adds debug-only data generation controls to the settings list. */
internal fun LazyListScope.developerToolsSection() {
    item {
        // Generation belongs to the activity so switching destinations does not cancel it.
        val owner = generateSequence(LocalContext.current) { (it as? ContextWrapper)?.baseContext }
            .filterIsInstance<ComponentActivity>().first()
        val viewModel: DeveloperToolsViewModel = viewModel(
            viewModelStoreOwner = owner,
            factory = DeveloperToolsViewModel.Factory
        )
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        DeveloperToolsContent(
            uiState = uiState,
            onNoteCountChange = viewModel::setNoteCountInput,
            onGenerate = viewModel::generateNotes
        )
    }
}

@Composable
internal fun DeveloperToolsContent(
    uiState: DeveloperToolsUiState,
    onNoteCountChange: (String) -> Unit,
    onGenerate: () -> Unit
) {
    SettingsSectionSurface {
        SettingsSectionHeader(stringResource(R.string.debug_developer_tools))
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.debug_generate_notes_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = uiState.noteCountInput,
            onValueChange = onNoteCountChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(SETTINGS_DEVELOPER_NOTE_COUNT_FIELD_TAG),
            enabled = !uiState.isGenerating,
            singleLine = true,
            isError = uiState.error == DeveloperToolsError.INVALID_NOTE_COUNT,
            label = { Text(stringResource(R.string.debug_number_of_notes)) },
            supportingText = {
                Text(
                    stringResource(
                        R.string.debug_allowed_range,
                        GenerateDebugNotesUseCase.MIN_NOTE_COUNT,
                        GenerateDebugNotesUseCase.MAX_NOTE_COUNT
                    )
                )
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onGenerate() })
        )
        Spacer(Modifier.height(12.dp))
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(SETTINGS_DEVELOPER_GENERATE_BUTTON_TAG),
            enabled = !uiState.isGenerating,
            onClick = onGenerate,
            shape = MaterialTheme.shapes.extraLarge
        ) {
            if (uiState.isGenerating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f)
                )
                Text(
                    text = "  ${uiState.generatedCount}/${uiState.requestedCount}"
                )
            } else {
                Text(stringResource(R.string.debug_generate_test_notes))
            }
        }
        DeveloperToolsFeedback(uiState)
    }
}

@Composable
private fun DeveloperToolsFeedback(uiState: DeveloperToolsUiState) {
    val message = when {
        uiState.error == DeveloperToolsError.INVALID_NOTE_COUNT ->
            stringResource(R.string.debug_error_invalid_count)
        uiState.error == DeveloperToolsError.GENERATION_FAILED ->
            stringResource(R.string.debug_error_generation_stopped, uiState.generatedCount)
        uiState.lastGeneratedCount != null ->
            stringResource(R.string.debug_generated_count, uiState.lastGeneratedCount)
        else -> null
    }
    val color = if (uiState.error == null) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }

    message?.let {
        Spacer(Modifier.height(10.dp))
        Text(
            text = it,
            style = MaterialTheme.typography.bodyMedium,
            color = color
        )
    }
}
