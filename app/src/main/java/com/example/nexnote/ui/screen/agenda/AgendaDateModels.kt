package com.example.nexnote.ui.screen.agenda

import java.util.Calendar

internal data class DisplayedMonth(val year: Int, val month: Int)

internal data class SelectedDate(val year: Int, val month: Int, val day: Int)

internal data class AgendaInitialDate(
    val displayedMonth: DisplayedMonth,
    val selectedDate: SelectedDate
)

internal fun currentAgendaInitialDate(
    calendar: Calendar = Calendar.getInstance()
): AgendaInitialDate {
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    return AgendaInitialDate(
        displayedMonth = DisplayedMonth(year, month),
        selectedDate = SelectedDate(year, month, day)
    )
}

internal fun DisplayedMonth.shiftByMonths(delta: Int): DisplayedMonth {
    val calendar = Calendar.getInstance().apply {
        set(year, month, 1)
        add(Calendar.MONTH, delta)
    }
    return DisplayedMonth(
        year = calendar.get(Calendar.YEAR),
        month = calendar.get(Calendar.MONTH)
    )
}
