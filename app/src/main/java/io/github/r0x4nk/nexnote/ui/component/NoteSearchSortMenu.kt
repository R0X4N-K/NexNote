package io.github.r0x4nk.nexnote.ui.component

import androidx.annotation.StringRes
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
import androidx.compose.ui.res.stringResource
import io.github.r0x4nk.nexnote.R
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
            contentDescription = stringResource(
                R.string.common_sort_results,
                stringResource(selected.labelRes)
            ),
            selected = selected != NoteSearchSort.RELEVANCE,
            onClick = { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            NoteSearchSort.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(option.labelRes)) },
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

private val NoteSearchSort.labelRes: Int
    @StringRes
    get() = when (this) {
        NoteSearchSort.RELEVANCE -> R.string.search_sort_relevance
        NoteSearchSort.MODIFIED_DESC -> R.string.search_sort_modified_desc
        NoteSearchSort.MODIFIED_ASC -> R.string.search_sort_modified_asc
        NoteSearchSort.TITLE_ASC -> R.string.search_sort_title_asc
        NoteSearchSort.TITLE_DESC -> R.string.search_sort_title_desc
    }
