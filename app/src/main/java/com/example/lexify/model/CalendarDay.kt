package com.example.lexify.model

import java.util.Calendar

data class CalendarDay(
    val dayOfWeek: String,
    val date: Int,
    val year: Int,
    val month: Int,
    val calendar: Calendar,
    val isCurrentDay: Boolean = false,
    val isWeekend: Boolean = false,
    val isEndOfWeek: Boolean = false,
    val dailyIncome: Int = 0
) {
    fun toDate(): Long {
        return calendar.timeInMillis
    }
}
