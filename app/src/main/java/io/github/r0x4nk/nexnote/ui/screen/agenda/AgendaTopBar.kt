package io.github.r0x4nk.nexnote.ui.screen.agenda

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import io.github.r0x4nk.nexnote.ui.component.NexIconButton
import io.github.r0x4nk.nexnote.ui.component.nexTopAppBarColors
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
        actions = { AgendaMonthActions(actions) },
        colors = nexTopAppBarColors()
    )
}

@Composable
private fun AgendaMonthTitle(monthTitle: String) {
    Text(
        text = monthTitle,
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun AgendaPreviousMonthButton(onClick: () -> Unit) {
    NexIconButton(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
        contentDescription = "Previous month",
        onClick = onClick
    )
}

@Composable
private fun AgendaMonthActions(actions: AgendaActions) {
    NexIconButton(
        imageVector = Icons.Default.CalendarToday,
        contentDescription = "Go to today",
        onClick = actions.onGoToToday,
        selected = true
    )
    NexIconButton(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = "Next month",
        onClick = actions.onNextMonth
    )
}
