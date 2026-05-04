package io.github.r0x4nk.nexnote.ui.screen.tags

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.ui.component.NexIconButton
import io.github.r0x4nk.nexnote.ui.component.NexSearchField

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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NexSearchField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = "Search tags",
            focusRequester = focusRequester,
            textStyle = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.size(8.dp))
        NexIconButton(
            imageVector = Icons.Default.Close,
            contentDescription = "Clear search",
            onClick = onClose
        )
    }
}
