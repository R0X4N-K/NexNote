package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.ui.component.NexIconButton

private val EditorTopBarMinHeight = 44.dp
private val EditorSearchFieldMinHeight = 36.dp

@Composable
internal fun EditorTopBar(
    isSaving: Boolean,
    title: String,
    isTemplateMode: Boolean,
    containerColor: Color,
    searchState: NoteSearchState,
    searchFocusRequester: FocusRequester,
    onBack: () -> Unit,
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

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars),
        color = containerColor,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = EditorTopBarMinHeight)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NexIconButton(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = onBack
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                EditorTopBarTitle(
                    displayTitle = displayTitle,
                    searchState = searchState,
                    searchFocusRequester = searchFocusRequester,
                    onSearchQueryChange = onSearchQueryChange,
                    onSearchNext = onSearchNext
                )
            }
            EditorTopBarActions(
                isSaving = isSaving,
                searchState = searchState,
                onSearchOpen = onSearchOpen,
                onSearchClose = onSearchClose,
                onSearchPrevious = onSearchPrevious,
                onSearchNext = onSearchNext,
                onExport = onExport
            )
        }
    }
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = EditorSearchFieldMinHeight)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                singleLine = true,
                textStyle = MaterialTheme.typography.titleSmall.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearchNext() }),
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                text = "Search in note",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                            )
                        }
                        inner()
                    }
                }
            )
        }
    }
}

@Composable
private fun EditorTopBarActions(
    isSaving: Boolean,
    searchState: NoteSearchState,
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
}
