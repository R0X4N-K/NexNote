package io.github.r0x4nk.nexnote.ui.screen.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.util.NoteLinkMarkdown

@Immutable
internal data class NoteLinkTarget(
    val id: Long,
    val title: String
)

internal data class NoteLinkAutocompleteMatch(
    val start: Int,
    val endExclusive: Int,
    val query: String
)

@Composable
internal fun NoteLinkPickerDialog(
    targets: List<NoteLinkTarget>,
    onDismiss: () -> Unit,
    onTargetSelected: (NoteLinkTarget) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filteredTargets = remember(targets, query) {
        filterNoteLinkTargets(targets, query, limit = Int.MAX_VALUE)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Link note") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search notes"
                        )
                    },
                    placeholder = { Text("Search notes") }
                )
                Spacer(Modifier.height(10.dp))
                NoteLinkTargetList(
                    targets = filteredTargets,
                    emptyText = if (targets.isEmpty()) "No notes available" else "No matches",
                    onTargetSelected = onTargetSelected,
                    modifier = Modifier.heightIn(max = 360.dp)
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
internal fun BoxScope.EditorNoteLinkAutocompletePopup(
    contentValue: TextFieldValue,
    targets: List<NoteLinkTarget>,
    enabled: Boolean,
    onTargetSelected: (NoteLinkAutocompleteMatch, NoteLinkTarget) -> Unit
) {
    val match = remember(contentValue) { findNoteLinkAutocompleteMatch(contentValue) }
    val suggestions = remember(targets, match?.query) {
        filterNoteLinkTargets(targets, match?.query.orEmpty(), limit = AUTOCOMPLETE_LIMIT)
    }
    val isVisible = enabled && match != null && suggestions.isNotEmpty()

    AnimatedVisibility(
        visible = isVisible,
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(start = 20.dp, top = 10.dp, end = 20.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp),
            shape = RoundedCornerShape(8.dp),
            tonalElevation = 4.dp,
            shadowElevation = 6.dp,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            NoteLinkTargetList(
                targets = suggestions,
                emptyText = "",
                onTargetSelected = { target ->
                    match?.let { onTargetSelected(it, target) }
                },
                modifier = Modifier.heightIn(max = 240.dp)
            )
        }
    }
}

internal fun noteLinkMarkdownFor(target: NoteLinkTarget): String =
    NoteLinkMarkdown.create(target.id, target.title)

internal fun findNoteLinkAutocompleteMatch(value: TextFieldValue): NoteLinkAutocompleteMatch? {
    if (!value.selection.collapsed) return null

    val cursor = value.selection.end.coerceIn(0, value.text.length)
    val triggerIndex = value.text.lastIndexOf("[[", startIndex = (cursor - 1).coerceAtLeast(0))
    if (triggerIndex == -1 || triggerIndex > cursor) return null

    val query = value.text.substring(triggerIndex + 2, cursor)
    if (!query.isValidNoteLinkQuery()) return null

    return NoteLinkAutocompleteMatch(
        start = triggerIndex,
        endExclusive = cursor,
        query = query.removePrefix("note:").trim()
    )
}

internal fun filterNoteLinkTargets(
    targets: List<NoteLinkTarget>,
    query: String,
    limit: Int
): List<NoteLinkTarget> {
    val normalizedQuery = query.trim().lowercase()
    return targets
        .asSequence()
        .filter { target ->
            normalizedQuery.isBlank() ||
                target.title.lowercase().contains(normalizedQuery) ||
                target.id.toString() == normalizedQuery
        }
        .take(limit)
        .toList()
}

@Composable
private fun NoteLinkTargetList(
    targets: List<NoteLinkTarget>,
    emptyText: String,
    onTargetSelected: (NoteLinkTarget) -> Unit,
    modifier: Modifier = Modifier
) {
    if (targets.isEmpty()) {
        Text(
            text = emptyText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 18.dp)
        )
        return
    }

    LazyColumn(modifier = modifier.fillMaxWidth()) {
        items(targets, key = { target -> target.id }) { target ->
            NoteLinkTargetRow(
                target = target,
                onClick = { onTargetSelected(target) }
            )
            if (target != targets.last()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            }
        }
    }
}

@Composable
private fun NoteLinkTargetRow(
    target: NoteLinkTarget,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = target.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "#${target.id}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun String.isValidNoteLinkQuery(): Boolean =
    length <= MAX_AUTOCOMPLETE_QUERY_LENGTH &&
        !contains('\n') &&
        !contains('\r') &&
        !contains("]]") &&
        !contains('[') &&
        !contains(']')

private const val AUTOCOMPLETE_LIMIT = 6
private const val MAX_AUTOCOMPLETE_QUERY_LENGTH = 80
