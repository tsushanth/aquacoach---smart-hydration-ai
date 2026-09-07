package com.factory.aquacoachsmarthydrationai.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object DateUtils {

    fun today(): LocalDate = LocalDate.now()

    fun todayEpochDay(): Long = today().toEpochDay()

    fun shortDayLabel(epochDay: Long): String {
        val date = LocalDate.ofEpochDay(epochDay)
        return date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    }

    fun mediumDateLabel(epochDay: Long): String {
        val date = LocalDate.ofEpochDay(epochDay)
        return date.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))
    }

    fun timeLabel(epochMillis: Long): String {
        val instant = java.time.Instant.ofEpochMilli(epochMillis)
        val time = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
        return time.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
    }
}
