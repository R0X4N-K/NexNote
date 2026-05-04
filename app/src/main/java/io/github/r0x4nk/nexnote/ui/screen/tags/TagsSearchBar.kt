package io.github.r0x4nk.nexnote.ui.screen.tags

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@Composable
internal fun InlineSearchBar(
    query: String,
    isVisible: Boolean,
    focusRequester: FocusRequester,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(isVisible, focusRequester) {
        if (isVisible) focusRequester.requestFocus()
    }
    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically(tween(130)) + fadeIn(tween(130)),
        exit = shrinkVertically(tween(120)) + fadeOut(tween(100))
    ) {
        InlineSearchBarContent(
            query = query,
            focusRequester = focusRequester,
            onQueryChange = onQueryChange,
            onClose = onClose,
            modifier = modifier
        )
    }
}

@Composable
private fun InlineSearchBarContent(
    query: String,
    focusRequester: FocusRequester,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SearchLeadingIcon()
        Spacer(Modifier.width(8.dp))
        SearchTextField(
            query = query,
            focusRequester = focusRequester,
            onQueryChange = onQueryChange,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Clear search",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun SearchLeadingIcon() {
    Icon(
        Icons.Default.Search,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        modifier = Modifier.size(20.dp)
    )
}

@Composable
private fun SearchTextField(
    query: String,
    focusRequester: FocusRequester,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.focusRequester(focusRequester),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = {}),
        decorationBox = { inner -> SearchDecoration(query, inner) }
    )
}

@Composable
private fun SearchDecoration(query: String, inner: @Composable () -> Unit) {
    Box {
        if (query.isEmpty()) {
            Text(
                "Search tags…",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            )
        }
        inner()
    }
}
