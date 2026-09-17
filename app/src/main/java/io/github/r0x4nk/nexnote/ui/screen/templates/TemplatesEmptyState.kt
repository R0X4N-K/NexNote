package io.github.r0x4nk.nexnote.ui.screen.templates

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.R
import io.github.r0x4nk.nexnote.ui.component.NexEmptyState
import io.github.r0x4nk.nexnote.ui.component.OperationLoadingState

@Composable
internal fun TemplatesLoadingState(padding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        OperationLoadingState()
    }
}

@Composable
internal fun TemplatesEmptyState(
    isSearchActive: Boolean,
    padding: PaddingValues,
    onCreateTemplate: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            NexEmptyState(
                icon = if (isSearchActive) {
                    Icons.AutoMirrored.Filled.ManageSearch
                } else {
                    Icons.Default.Description
                },
                title = if (isSearchActive) {
                    stringResource(R.string.templates_no_results)
                } else {
                    stringResource(R.string.templates_no_templates)
                },
                message = if (isSearchActive) {
                    stringResource(R.string.home_no_matching_templates_message)
                } else {
                    stringResource(R.string.templates_no_templates_message)
                }
            )
            if (!isSearchActive) {
                FilledTonalButton(onClick = onCreateTemplate) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.templates_create))
                }
            }
        }
    }
}