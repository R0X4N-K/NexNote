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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.r0x4nk.nexnote.ui.component.NexIconButton
import io.github.r0x4nk.nexnote.ui.component.roundedClickableTarget
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(Modifier.padding(vertical = 12.dp)) {
            CalendarMonthHeader(
                year = uiState.displayedYear,
                month = uiState.displayedMonth,
                onPreviousMonth = actions.onPreviousMonth,
                onNextMonth = actions.onNextMonth
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)
            )
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
        }
    }
}

@Composable
private fun CalendarMonthHeader(
    year: Int,
    month: Int,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val monthTitle = remember(year, month) { DateUtils.formatMonthYear(year, month) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "MONTH",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = monthTitle,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        NexIconButton(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = "Previous month",
            onClick = onPreviousMonth
        )
        NexIconButton(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Next month",
            onClick = onNextMonth
        )
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
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }
    val textColor = dayTextColor(isSelected, isToday)
    val dotColor = dayDotColor(hasDot, isSelected)

    Column(
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DayNumber(
            day = day,
            containerColor = containerColor,
            textColor = textColor,
            isToday = isToday,
            isSelected = isSelected,
            onClick = onClick
        )
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
    !hasDot -> Color.Transparent
    isSelected -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
}

@Composable
private fun DayNumber(
    day: Int,
    containerColor: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .semantics {
                selected = isSelected
                if (isSelected || isToday) {
                    stateDescription = when {
                        isSelected && isToday -> "Selected, today"
                        isSelected -> "Selected"
                        else -> "Today"
                    }
                }
            }
            .roundedClickableTarget(
                shape = RoundedCornerShape(13.dp),
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(containerColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
