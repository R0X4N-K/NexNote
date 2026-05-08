package io.github.r0x4nk.nexnote.ui.screen.agenda

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.foundation.lazy.LazyListState
import io.github.r0x4nk.nexnote.ui.common.NoteListViewMode
import io.github.r0x4nk.nexnote.ui.component.radial.RadialMenuEffect
import io.github.r0x4nk.nexnote.ui.component.radial.RadialMenuItem

@Composable
internal fun AgendaCalendarVisibilityEffects(
    isSearchActive: Boolean,
    isToolbarSticky: Boolean,
    listState: LazyListState,
    searchFocusRequester: FocusRequester,
    onCalendarVisibilityChange: (Boolean) -> Unit
) {
    LaunchedEffect(isSearchActive, listState, searchFocusRequester, onCalendarVisibilityChange) {
        if (isSearchActive) {
            onCalendarVisibilityChange(false)
            searchFocusRequester.requestFocus()
        } else if (listState.firstVisibleItemIndex <= CONTROLS_ROW_INDEX) {
            onCalendarVisibilityChange(true)
        }
    }

    LaunchedEffect(isToolbarSticky, isSearchActive, onCalendarVisibilityChange) {
        if (!isToolbarSticky && !isSearchActive) {
            onCalendarVisibilityChange(true)
        }
    }
}

@Composable
internal fun AgendaRadialMenu(
    viewMode: NoteListViewMode,
    actions: AgendaActions
) {
    RadialMenuEffect(items = remember(viewMode, actions) {
        listOf(
            RadialMenuItem(Icons.Default.CalendarToday, "") { actions.onGoToToday() },
            RadialMenuItem(Icons.Default.SwapVert, "") { actions.onToggleSort() },
            RadialMenuItem(
                icon = if (viewMode == NoteListViewMode.LIST) {
                    Icons.Default.GridView
                } else {
                    Icons.AutoMirrored.Filled.ViewList
                },
                label = ""
            ) { actions.onToggleView() },
            RadialMenuItem(Icons.Default.Search, "") { actions.onSearchToggle(true) }
        )
    })
}
