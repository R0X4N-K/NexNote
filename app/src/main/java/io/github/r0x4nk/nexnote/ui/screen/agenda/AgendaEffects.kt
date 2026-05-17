package io.github.r0x4nk.nexnote.ui.screen.agenda

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import io.github.r0x4nk.nexnote.ui.component.radial.RadialFabActionEffect

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
internal fun AgendaNewNoteFab(
    selectedDateMillis: Long,
    actions: AgendaActions
) {
    RadialFabActionEffect(
        contentDescription = "Create note",
        onClick = remember(selectedDateMillis, actions) {
            { actions.onNewNote(selectedDateMillis) }
        }
    )
}
