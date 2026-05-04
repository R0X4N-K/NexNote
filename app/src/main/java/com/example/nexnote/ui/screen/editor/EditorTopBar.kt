package com.example.nexnote.ui.screen.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

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
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
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
        colors = TopAppBarDefaults.topAppBarColors(containerColor = containerColor)
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
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        singleLine = true,
        textStyle = MaterialTheme.typography.titleMedium.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearchNext() }),
        decorationBox = { innerTextField ->
            EditorSearchDecoration(value, innerTextField)
        }
    )
}

@Composable
private fun EditorSearchDecoration(
    value: String,
    innerTextField: @Composable () -> Unit
) {
    Box {
        if (value.isEmpty()) {
            Text(
                text = "Search in note…",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            )
        }
        innerTextField()
    }
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
    IconButton(onClick = onSearchPrevious, enabled = searchState.hasMatches) {
        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous match")
    }
    IconButton(onClick = onSearchNext, enabled = searchState.hasMatches) {
        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next match")
    }
    IconButton(onClick = onSearchClose) {
        Icon(Icons.Default.Close, contentDescription = "Close search")
    }
}

@Composable
private fun EditorBrowsingActions(
    isMarkdown: Boolean,
    onMarkdownToggle: () -> Unit,
    onSearchOpen: () -> Unit,
    onExport: (() -> Unit)?
) {
    if (onExport != null) {
        IconButton(onClick = onExport) {
            Icon(Icons.Default.IosShare, contentDescription = "Export note")
        }
    }
    IconButton(onClick = onSearchOpen) {
        Icon(Icons.Default.Search, contentDescription = "Search in note")
    }
    IconButton(
        onClick = onMarkdownToggle,
        modifier = Modifier
            .clip(CircleShape)
            .background(
                if (isMarkdown) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
                } else {
                    Color.Transparent
                }
            )
    ) {
        Icon(
            imageVector = Icons.Default.Code,
            contentDescription = if (isMarkdown) "Disable Markdown" else "Enable Markdown",
            tint = if (isMarkdown) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            }
        )
    }
}
