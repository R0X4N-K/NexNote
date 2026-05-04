package io.github.r0x4nk.nexnote.ui.screen.templates

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ManageSearch
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.r0x4nk.nexnote.ui.component.NexEmptyState

@Composable
internal fun TemplatesLoadingState(padding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
internal fun TemplatesEmptyState(
    isSearchActive: Boolean,
    padding: PaddingValues
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        NexEmptyState(
            icon = if (isSearchActive) Icons.Default.ManageSearch else Icons.Default.Description,
            title = if (isSearchActive) "No results" else "No templates",
            message = if (isSearchActive) {
                "Try different words"
            } else {
                "Use the + button to create a custom template"
            }
        )
    }
}
