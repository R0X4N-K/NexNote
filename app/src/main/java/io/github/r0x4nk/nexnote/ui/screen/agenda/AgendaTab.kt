package io.github.r0x4nk.nexnote.ui.screen.agenda

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import io.github.r0x4nk.nexnote.R

/**
 * The two surfaces that share the Agenda destination: the month calendar with
 * the selected day's notes, and the chronological timeline of every note.
 */
internal enum class AgendaTab(val pageIndex: Int) {
    CALENDAR(0),
    AGENDA(1);

    companion object {
        fun fromPage(page: Int): AgendaTab =
            entries.firstOrNull { it.pageIndex == page } ?: CALENDAR
    }
}

@Composable
internal fun AgendaTabs(
    selectedTab: AgendaTab,
    onTabSelected: (AgendaTab) -> Unit,
    modifier: Modifier = Modifier
) {
    PrimaryTabRow(
        selectedTabIndex = selectedTab.pageIndex,
        modifier = modifier,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        AgendaTab.entries.forEach { tab ->
            Tab(
                selected = tab == selectedTab,
                onClick = { onTabSelected(tab) },
                text = {
                    Text(
                        text = stringResource(tab.labelRes),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    }
}

internal val AgendaTab.labelRes: Int
    get() = when (this) {
        AgendaTab.CALENDAR -> R.string.agenda_tab_calendar
        AgendaTab.AGENDA -> R.string.agenda_tab_agenda
    }
