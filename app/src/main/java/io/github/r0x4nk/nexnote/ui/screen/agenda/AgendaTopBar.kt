package io.github.r0x4nk.nexnote.ui.screen.agenda

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import io.github.r0x4nk.nexnote.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AgendaTopBar(
    displayedYear: Int,
    displayedMonth: Int,
    actions: AgendaActions
) {
    val monthTitle = remember(displayedYear, displayedMonth) {
        DateUtils.formatMonthYear(displayedYear, displayedMonth)
    }

    TopAppBar(
        title = { AgendaMonthTitle(monthTitle) },
        navigationIcon = { AgendaPreviousMonthButton(actions.onPreviousMonth) },
        actions = { AgendaMonthActions(actions) }
    )
}

@Composable
private fun AgendaMonthTitle(monthTitle: String) {
    Text(
        text = monthTitle,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun AgendaPreviousMonthButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = "Previous month"
        )
    }
}

@Composable
private fun AgendaMonthActions(actions: AgendaActions) {
    IconButton(onClick = actions.onGoToToday) {
        Icon(Icons.Default.CalendarToday, contentDescription = "Go to today")
    }
    IconButton(onClick = actions.onNextMonth) {
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Next month"
        )
    }
}
