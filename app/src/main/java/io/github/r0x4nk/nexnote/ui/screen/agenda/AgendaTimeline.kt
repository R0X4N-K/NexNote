package io.github.r0x4nk.nexnote.ui.screen.agenda

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.R
import io.github.r0x4nk.nexnote.domain.model.NoteCardStyle
import io.github.r0x4nk.nexnote.domain.model.NoteSearchSort
import io.github.r0x4nk.nexnote.ui.common.SelectionUiState
import io.github.r0x4nk.nexnote.ui.common.animateNoteItem
import io.github.r0x4nk.nexnote.ui.component.NexEmptyState
import io.github.r0x4nk.nexnote.ui.component.NoteTagFolderExpansionState
import io.github.r0x4nk.nexnote.ui.component.ScrollToTopButton
import io.github.r0x4nk.nexnote.ui.component.radial.RadialMenuOverlayDefaults
import io.github.r0x4nk.nexnote.util.DateUtils

@Composable
internal fun AgendaTimelinePage(
    sections: List<AgendaTimelineSection>,
    listState: LazyListState,
    expansionState: NoteTagFolderExpansionState,
    isSearchActive: Boolean,
    searchQuery: String,
    searchSort: NoteSearchSort,
    hasActiveSearchFilters: Boolean,
    noteCount: Int,
    searchFocusRequester: FocusRequester,
    noteCardStyle: NoteCardStyle,
    selectionState: SelectionUiState,
    floatingBottomPadding: Dp,
    actions: AgendaActions,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Fixed, transparent controls bar over the screen gradient — the same
        // treatment as the Home top bar, so it never reads as a dark band.
        // Hidden during selection so only the contextual top bar exposes an
        // overflow menu.
        if (!selectionState.isActive) {
            AgendaTimelineControlsRow(
                isSearchActive = isSearchActive,
                searchQuery = searchQuery,
                searchSort = searchSort,
                hasActiveSearchFilters = hasActiveSearchFilters,
                isAllSectionsCollapsed = expansionState.isAllCollapsed,
                noteCount = noteCount,
                searchFocusRequester = searchFocusRequester,
                onToggleAllSections = expansionState::toggleAll,
                actions = actions
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (sections.isEmpty()) {
                AgendaTimelineEmptyState(
                    isSearchActive = isSearchActive,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                AgendaTimelineList(
                    sections = sections,
                    listState = listState,
                    expansionState = expansionState,
                    noteCardStyle = noteCardStyle,
                    selectionState = selectionState,
                    floatingBottomPadding = floatingBottomPadding,
                    actions = actions
                )
                ScrollToTopButton(
                    listState = listState,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(
                            end = 16.dp,
                            bottom = if (selectionState.isActive) {
                                floatingBottomPadding + 16.dp
                            } else {
                                RadialMenuOverlayDefaults.fabBottomClearance(floatingBottomPadding)
                            }
                        )
                )
            }
        }
    }
}

@Composable
private fun AgendaTimelineList(
    sections: List<AgendaTimelineSection>,
    listState: LazyListState,
    expansionState: NoteTagFolderExpansionState,
    noteCardStyle: NoteCardStyle,
    selectionState: SelectionUiState,
    floatingBottomPadding: Dp,
    actions: AgendaActions
) {
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds(),
        contentPadding = PaddingValues(
            top = 4.dp,
            bottom = 16.dp
        )
    ) {
        sections.forEach { section ->
            item(
                key = "agenda_section_header_${section.key}",
                contentType = "agenda_section_header"
            ) {
                AgendaSectionHeader(
                    title = rememberTimelineSectionTitle(section),
                    noteCount = section.noteCount,
                    isExpanded = section.key !in expansionState.collapsedFolderIds,
                    onToggle = { expansionState.onToggleFolder(section.key) },
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                        .then(animateNoteItem())
                )
            }

            if (section.key !in expansionState.collapsedFolderIds) {
                items(
                    items = section.items,
                    key = { item -> item.note.id },
                    contentType = { "note_card" }
                ) { item ->
                    AgendaNoteCardItem(
                        scored = item,
                        noteCardStyle = noteCardStyle,
                        selectionState = selectionState,
                        actions = actions,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .then(animateNoteItem())
                    )
                }
            }
        }

        item {
            Spacer(
                Modifier.height(
                    RadialMenuOverlayDefaults.fabBottomClearance(floatingBottomPadding)
                )
            )
        }
    }
}

@Composable
private fun AgendaTimelineEmptyState(
    isSearchActive: Boolean,
    modifier: Modifier = Modifier
) {
    NexEmptyState(
        icon = Icons.AutoMirrored.Filled.EventNote,
        title = stringResource(
            if (isSearchActive) R.string.agenda_empty_search_title
            else R.string.agenda_timeline_empty_title
        ),
        message = stringResource(
            if (isSearchActive) R.string.agenda_empty_search_message
            else R.string.agenda_timeline_empty_message
        ),
        modifier = modifier
    )
}

@Composable
private fun AgendaSectionHeader(
    title: String,
    noteCount: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val noteCountText = pluralStringResource(R.plurals.home_note_count, noteCount, noteCount)

    Surface(
        onClick = onToggle,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = colorScheme.surfaceContainerLow,
        contentColor = colorScheme.onSurface,
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = noteCountText,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(12.dp))
            Icon(
                imageVector = if (isExpanded) {
                    Icons.Default.KeyboardArrowDown
                } else {
                    Icons.AutoMirrored.Filled.KeyboardArrowRight
                },
                contentDescription = stringResource(
                    if (isExpanded) {
                        R.string.agenda_timeline_collapse_section
                    } else {
                        R.string.agenda_timeline_expand_section
                    }
                ),
                tint = colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun rememberTimelineSectionTitle(section: AgendaTimelineSection): String {
    val todayLabel = stringResource(R.string.agenda_today)
    val yesterdayLabel = stringResource(R.string.agenda_timeline_yesterday)
    val last7DaysLabel = stringResource(R.string.agenda_timeline_last_7_days)
    val last30DaysLabel = stringResource(R.string.agenda_timeline_last_30_days)
    return remember(section, todayLabel, yesterdayLabel, last7DaysLabel, last30DaysLabel) {
        agendaTimelineSectionTitle(
            section = section,
            todayLabel = todayLabel,
            yesterdayLabel = yesterdayLabel,
            last7DaysLabel = last7DaysLabel,
            last30DaysLabel = last30DaysLabel
        )
    }
}

/**
 * Resolves the localized title for a timeline section. Relative buckets reuse
 * the labels provided by the UI so the function stays testable without a
 * Compose resource context.
 */
internal fun agendaTimelineSectionTitle(
    section: AgendaTimelineSection,
    todayLabel: String,
    yesterdayLabel: String,
    last7DaysLabel: String,
    last30DaysLabel: String
): String = when (section.kind) {
    AgendaSectionKind.TODAY -> todayLabel
    AgendaSectionKind.YESTERDAY -> yesterdayLabel
    AgendaSectionKind.LAST_7_DAYS -> last7DaysLabel
    AgendaSectionKind.LAST_30_DAYS -> last30DaysLabel
    AgendaSectionKind.MONTH -> DateUtils.formatMonthYear(section.year, section.month)
    AgendaSectionKind.YEAR -> section.year.toString()
}
