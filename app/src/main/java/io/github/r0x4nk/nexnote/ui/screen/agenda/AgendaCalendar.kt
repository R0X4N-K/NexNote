package io.github.r0x4nk.nexnote.ui.screen.agenda

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.util.DateUtils
import java.util.Calendar

@Composable
internal fun AgendaCalendarSection(
    uiState: AgendaUiState,
    isCalendarVisible: Boolean,
    actions: AgendaActions
) {
    val today = rememberToday()
    val calendarCells = rememberCalendarCells(uiState.displayedYear, uiState.displayedMonth)
    val daysWithNotes = rememberDaysWithNotesInMonth(uiState)

    AnimatedVisibility(
        visible = isCalendarVisible,
        enter = agendaCalendarEnter(),
        exit = agendaCalendarExit()
    ) {
        AgendaCalendarContent(uiState, calendarCells, daysWithNotes, today, actions)
    }
}

@Composable
private fun AgendaCalendarContent(
    uiState: AgendaUiState,
    calendarCells: List<Int?>,
    daysWithNotes: Set<Int>,
    today: AgendaToday,
    actions: AgendaActions
) {
    Column {
        WeekdayHeader()
        CalendarGrid(
            cells = calendarCells,
            selectedDay = uiState.selectedDay,
            daysWithNotes = daysWithNotes,
            todayDay = today.dayFor(uiState.displayedYear, uiState.displayedMonth),
            onDayClick = { day ->
                actions.onSelectDate(uiState.displayedYear, uiState.displayedMonth, day)
            }
        )
        HorizontalDivider(Modifier.padding(top = 4.dp))
    }
}

private fun agendaCalendarEnter(): EnterTransition {
    return expandVertically(animationSpec = tween(durationMillis = 180)) +
        fadeIn(animationSpec = tween(durationMillis = 150))
}

private fun agendaCalendarExit(): ExitTransition {
    return shrinkVertically(animationSpec = tween(durationMillis = 180)) +
        fadeOut(animationSpec = tween(durationMillis = 150))
}

@Composable
private fun rememberCalendarCells(year: Int, month: Int): List<Int?> {
    return remember(year, month) {
        val offset = DateUtils.firstWeekdayOfMonth(year, month)
        val daysInMonth = DateUtils.daysInMonth(year, month)
        buildList {
            repeat(offset) { add(null) }
            for (day in 1..daysInMonth) add(day)
            repeat((7 - size % 7) % 7) { add(null) }
        }
    }
}

@Composable
private fun rememberDaysWithNotesInMonth(uiState: AgendaUiState): Set<Int> {
    return remember(uiState.displayedYear, uiState.displayedMonth, uiState.daysWithNotes) {
        val daysInMonth = DateUtils.daysInMonth(uiState.displayedYear, uiState.displayedMonth)
        (1..daysInMonth).filterTo(mutableSetOf()) { day ->
            val noon = DateUtils.toMillis(uiState.displayedYear, uiState.displayedMonth, day)
            DateUtils.startOfDay(noon) in uiState.daysWithNotes
        }
    }
}

@Composable
private fun rememberToday(): AgendaToday {
    return remember {
        val today = Calendar.getInstance()
        AgendaToday(
            year = today.get(Calendar.YEAR),
            month = today.get(Calendar.MONTH),
            day = today.get(Calendar.DAY_OF_MONTH)
        )
    }
}

private data class AgendaToday(
    val year: Int,
    val month: Int,
    val day: Int
) {
    fun dayFor(displayedYear: Int, displayedMonth: Int): Int? {
        return if (year == displayedYear && month == displayedMonth) day else null
    }
}

@Composable
private fun WeekdayHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su").forEach { label ->
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    cells: List<Int?>,
    selectedDay: Int,
    daysWithNotes: Set<Int>,
    todayDay: Int?,
    onDayClick: (Int) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        cells.chunked(7).forEach { week ->
            CalendarWeek(
                week = week,
                selectedDay = selectedDay,
                daysWithNotes = daysWithNotes,
                todayDay = todayDay,
                onDayClick = onDayClick
            )
        }
    }
}

@Composable
private fun CalendarWeek(
    week: List<Int?>,
    selectedDay: Int,
    daysWithNotes: Set<Int>,
    todayDay: Int?,
    onDayClick: (Int) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        week.forEach { day ->
            CalendarDaySlot(day, selectedDay, daysWithNotes, todayDay, onDayClick)
        }
        repeat(7 - week.size) {
            Box(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun RowScope.CalendarDaySlot(
    day: Int?,
    selectedDay: Int,
    daysWithNotes: Set<Int>,
    todayDay: Int?,
    onDayClick: (Int) -> Unit
) {
    Box(
        modifier = Modifier.weight(1f),
        contentAlignment = Alignment.Center
    ) {
        if (day != null) {
            DayCell(
                day = day,
                isSelected = day == selectedDay,
                isToday = day == todayDay,
                hasDot = day in daysWithNotes,
                onClick = { onDayClick(day) }
            )
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    isSelected: Boolean,
    isToday: Boolean,
    hasDot: Boolean,
    onClick: () -> Unit
) {
    val circleBg = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface
    }
    val textColor = dayTextColor(isSelected, isToday)
    val dotColor = dayDotColor(hasDot, isSelected)

    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DayCircle(day, circleBg, textColor, isToday, isSelected)
        DayDot(dotColor)
    }
}

@Composable
private fun DayDot(dotColor: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .padding(top = 3.dp)
            .size(4.dp)
            .clip(CircleShape)
            .background(dotColor)
    )
}

@Composable
private fun dayTextColor(isSelected: Boolean, isToday: Boolean) = when {
    isSelected -> MaterialTheme.colorScheme.onPrimary
    isToday -> MaterialTheme.colorScheme.primary
    else -> MaterialTheme.colorScheme.onSurface
}

@Composable
private fun dayDotColor(hasDot: Boolean, isSelected: Boolean) = when {
    !hasDot -> MaterialTheme.colorScheme.surface
    isSelected -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
}

@Composable
private fun DayCircle(
    day: Int,
    circleBg: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color,
    isToday: Boolean,
    isSelected: Boolean
) {
    val circleModifier = Modifier
        .size(32.dp)
        .clip(CircleShape)
        .background(circleBg)
        .then(
            if (isToday && !isSelected) {
                Modifier.border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
            } else {
                Modifier
            }
        )

    Box(modifier = circleModifier, contentAlignment = Alignment.Center) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = textColor
        )
    }
}
