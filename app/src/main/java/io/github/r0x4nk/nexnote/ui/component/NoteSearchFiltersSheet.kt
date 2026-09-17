package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.R
import io.github.r0x4nk.nexnote.domain.model.NotePinnedFilter
import io.github.r0x4nk.nexnote.domain.model.NoteSearchScope

/** Note-specific filters shared by Home, Agenda, and the unlocked Vault. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteSearchFiltersSheet(
    searchScope: NoteSearchScope,
    pinnedFilter: NotePinnedFilter,
    selectedTagFilters: Set<String>,
    availableTagNames: Collection<String>,
    onSearchScopeChange: (NoteSearchScope) -> Unit,
    onPinnedFilterChange: (NotePinnedFilter) -> Unit,
    onToggleTagFilter: (String) -> Unit,
    onClearTagFilters: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { NexSheetDragHandle() },
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp)
        ) {
            NexSheetHeader(
                title = stringResource(R.string.search_filters_title),
                onBack = onDismiss
            )
            Column(
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                Spacer(Modifier.height(12.dp))
                SearchScopeSelector(searchScope, onSearchScopeChange)
                Spacer(Modifier.height(20.dp))
                PinnedFilterSelector(pinnedFilter, onPinnedFilterChange)
                Spacer(Modifier.height(20.dp))
                SearchTagFilters(
                    tagNames = (selectedTagFilters + availableTagNames).toSortedSet(),
                    selectedTagFilters = selectedTagFilters,
                    onToggle = onToggleTagFilter
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        enabled = searchScope != NoteSearchScope.TITLE_AND_CONTENT ||
                            pinnedFilter != NotePinnedFilter.ALL ||
                            selectedTagFilters.isNotEmpty(),
                        onClick = {
                            onSearchScopeChange(NoteSearchScope.TITLE_AND_CONTENT)
                            onPinnedFilterChange(NotePinnedFilter.ALL)
                            onClearTagFilters()
                        }
                    ) {
                        Text(stringResource(R.string.search_filters_reset))
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchScopeSelector(
    selected: NoteSearchScope,
    onSelect: (NoteSearchScope) -> Unit
) {
    Text(stringResource(R.string.search_filters_scope_label), style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(8.dp))
    val options = listOf(
        NoteSearchScope.TITLE_AND_CONTENT to stringResource(R.string.search_filters_scope_all),
        NoteSearchScope.TITLE to stringResource(R.string.search_filters_scope_titles),
        NoteSearchScope.CONTENT to stringResource(R.string.search_filters_scope_content)
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (scope, label) ->
            SegmentedButton(
                selected = selected == scope,
                onClick = { onSelect(scope) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                label = { Text(label) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PinnedFilterSelector(
    selected: NotePinnedFilter,
    onSelect: (NotePinnedFilter) -> Unit
) {
    Text(
        stringResource(R.string.search_filters_pinned_label),
        style = MaterialTheme.typography.titleSmall
    )
    Spacer(Modifier.height(8.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            NotePinnedFilter.ALL to stringResource(R.string.search_filters_pinned_all),
            NotePinnedFilter.PINNED to stringResource(R.string.search_filters_pinned_only),
            NotePinnedFilter.UNPINNED to stringResource(R.string.search_filters_pinned_none)
        ).forEach { (filter, label) ->
            FilterChip(
                selected = selected == filter,
                onClick = { onSelect(filter) },
                label = { Text(label) }
            )
        }
    }
}

@Composable
private fun SearchTagFilters(
    tagNames: Collection<String>,
    selectedTagFilters: Set<String>,
    onToggle: (String) -> Unit
) {
    Text(
        stringResource(R.string.search_filters_frequent_tags),
        style = MaterialTheme.typography.titleSmall
    )
    Spacer(Modifier.height(8.dp))
    if (tagNames.isEmpty()) {
        Text(
            stringResource(R.string.no_tags_yet),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tagNames.forEach { name ->
            FilterChip(
                selected = name in selectedTagFilters,
                onClick = { onToggle(name) },
                label = { Text("#$name") }
            )
        }
    }
}
