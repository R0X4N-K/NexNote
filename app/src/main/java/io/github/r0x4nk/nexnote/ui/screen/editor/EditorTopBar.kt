package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.ui.component.NexIconButton
import io.github.r0x4nk.nexnote.ui.component.NexSearchField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditorTopBar(
    isSaving: Boolean,
    isMarkdown: Boolean,
    title: String,
    isTemplateMode: Boolean,
    containerColor: Color,
    searchState: NoteSearchState,
    searchFocusRequester: FocusRequester,
    onBack: () -> Unit,
    onMarkdownToggle: () -> Unit,
    onSearchOpen: () -> Unit,
    onSearchClose: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchPrevious: () -> Unit,
    onSearchNext: () -> Unit,
    onExport: (() -> Unit)? = null
) {
    val displayTitle = when {
        title.isNotBlank() -> title
        isTemplateMode -> "New template"
        else -> "New note"
    }

    TopAppBar(
        title = {
            EditorTopBarTitle(
                displayTitle = displayTitle,
                searchState = searchState,
                searchFocusRequester = searchFocusRequester,
                onSearchQueryChange = onSearchQueryChange,
                onSearchNext = onSearchNext
            )
        },
        navigationIcon = {
            NexIconButton(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = onBack
            )
        },
        actions = {
            EditorTopBarActions(
                isSaving = isSaving,
                isMarkdown = isMarkdown,
                searchState = searchState,
                onMarkdownToggle = onMarkdownToggle,
                onSearchOpen = onSearchOpen,
                onSearchClose = onSearchClose,
                onSearchPrevious = onSearchPrevious,
                onSearchNext = onSearchNext,
                onExport = onExport
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            scrolledContainerColor = containerColor,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

@Composable
private fun EditorTopBarTitle(
    displayTitle: String,
    searchState: NoteSearchState,
    searchFocusRequester: FocusRequester,
    onSearchQueryChange: (String) -> Unit,
    onSearchNext: () -> Unit
) {
    if (searchState.isActive) {
        EditorSearchField(
            value = searchState.query,
            focusRequester = searchFocusRequester,
            onValueChange = onSearchQueryChange,
            onSearchNext = onSearchNext
        )
    } else {
        Text(
            text = displayTitle,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EditorSearchField(
    value: String,
    focusRequester: FocusRequester,
    onValueChange: (String) -> Unit,
    onSearchNext: () -> Unit
) {
    NexSearchField(
        value = value,
        onValueChange = onValueChange,
        placeholder = "Search in note",
        modifier = Modifier
            .fillMaxWidth(),
        focusRequester = focusRequester,
        textStyle = MaterialTheme.typography.titleMedium,
        onSearch = onSearchNext
    )
}

@Composable
private fun EditorTopBarActions(
    isSaving: Boolean,
    isMarkdown: Boolean,
    searchState: NoteSearchState,
    onMarkdownToggle: () -> Unit,
    onSearchOpen: () -> Unit,
    onSearchClose: () -> Unit,
    onSearchPrevious: () -> Unit,
    onSearchNext: () -> Unit,
    onExport: (() -> Unit)?
) {
    if (isSaving) EditorSavingIndicator()

    if (searchState.isActive) {
        EditorSearchActions(
            searchState = searchState,
            onSearchClose = onSearchClose,
            onSearchPrevious = onSearchPrevious,
            onSearchNext = onSearchNext
        )
    } else {
        EditorBrowsingActions(
            isMarkdown = isMarkdown,
            onMarkdownToggle = onMarkdownToggle,
            onSearchOpen = onSearchOpen,
            onExport = onExport
        )
    }
}

@Composable
private fun EditorSavingIndicator() {
    CircularProgressIndicator(
        modifier = Modifier.size(20.dp).padding(end = 4.dp),
        strokeWidth = 2.dp,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
    )
    Spacer(Modifier.width(8.dp))
}

@Composable
private fun EditorSearchActions(
    searchState: NoteSearchState,
    onSearchClose: () -> Unit,
    onSearchPrevious: () -> Unit,
    onSearchNext: () -> Unit
) {
    if (searchState.resultLabel.isNotEmpty()) {
        Text(
            text = searchState.resultLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = 40.dp)
        )
    }
    NexIconButton(
        imageVector = Icons.Default.KeyboardArrowUp,
        contentDescription = "Previous match",
        onClick = onSearchPrevious,
        enabled = searchState.hasMatches
    )
    NexIconButton(
        imageVector = Icons.Default.KeyboardArrowDown,
        contentDescription = "Next match",
        onClick = onSearchNext,
        enabled = searchState.hasMatches
    )
    NexIconButton(
        imageVector = Icons.Default.Close,
        contentDescription = "Close search",
        onClick = onSearchClose
    )
}

@Composable
private fun EditorBrowsingActions(
    isMarkdown: Boolean,
    onMarkdownToggle: () -> Unit,
    onSearchOpen: () -> Unit,
    onExport: (() -> Unit)?
) {
    if (onExport != null) {
        NexIconButton(
            imageVector = Icons.Default.IosShare,
            contentDescription = "Export note",
            onClick = onExport
        )
    }
    NexIconButton(
        imageVector = Icons.Default.Search,
        contentDescription = "Search in note",
        onClick = onSearchOpen
    )
    NexIconButton(
        imageVector = Icons.Default.Code,
        contentDescription = if (isMarkdown) "Disable Markdown" else "Enable Markdown",
        onClick = onMarkdownToggle,
        selected = isMarkdown
    )
}
