package com.example.lexify.model

import java.util.Calendar

/**
 * Represents a single day cell in the calendar list.
 */
data class CalendarDay(
    val dayOfWeek: String,      // e.g., "Пн", "Вт" ...
    val date: Int,              // day of month
    val year: Int,
    val month: Int,             // Calendar.MONTH (0-based)
    val calendar: Calendar,     // exact calendar instance of this day
    val isCurrentDay: Boolean,
    val isWeekend: Boolean,
    val isEndOfWeek: Boolean,
    val dailyIncome: Int        // income for this day
) {
    /**
     * Returns the epoch millis for this day (at the time contained in [calendar]).
     */
    fun toDate(): Long = calendar.timeInMillis
}
