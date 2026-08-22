package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.r0x4nk.nexnote.domain.model.NoteSearchSort

/** Sort choices shared by every note-search surface. */
@Composable
fun NoteSearchSortMenu(
    selected: NoteSearchSort,
    onSelect: (NoteSearchSort) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        NexIconButton(
            imageVector = Icons.AutoMirrored.Filled.Sort,
            contentDescription = "Sort search results: ${selected.label}",
            selected = selected != NoteSearchSort.RELEVANCE,
            onClick = { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            NoteSearchSort.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    trailingIcon = if (option == selected) {
                        { Icon(imageVector = Icons.Default.Check, contentDescription = null) }
                    } else {
                        null
                    },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    }
                )
            }
        }
    }
}

private val NoteSearchSort.label: String
    get() = when (this) {
        NoteSearchSort.RELEVANCE -> "Relevance"
        NoteSearchSort.MODIFIED_DESC -> "Newest modified"
        NoteSearchSort.MODIFIED_ASC -> "Oldest modified"
        NoteSearchSort.TITLE_ASC -> "Title A–Z"
        NoteSearchSort.TITLE_DESC -> "Title Z–A"
    }
