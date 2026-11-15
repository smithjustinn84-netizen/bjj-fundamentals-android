package dev.justinsmith.bjj.ui.home

import dev.justinsmith.bjj.core.LocalZone
import dev.justinsmith.bjj.core.dayKeyOf
import dev.justinsmith.bjj.core.dayNumberOf
import dev.justinsmith.bjj.core.isDayKey
import dev.justinsmith.bjj.core.localDayNumber

// Dates in words. The app is in English, as the web app's own text is, so the names are too.

// Day 0 of the count, 1 January 1970, was a Thursday.
private val WEEKDAYS = listOf("Thursday", "Friday", "Saturday", "Sunday", "Monday", "Tuesday", "Wednesday")
private val MONTHS = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
)
private const val SHORT = 3

/** Today's date in words, such as "Sunday 15 November". */
fun todayLabel(now: Long, zone: LocalZone): String {
    val day = localDayNumber(now, zone)
    val (_, month, dayOfMonth) = dayKeyOf(day).split('-').map { it.toInt() }
    return "${WEEKDAYS[day.mod(WEEKDAYS.size)]} $dayOfMonth ${MONTHS[month - 1]}"
}

/** A day key in words, such as "Sat 14 Nov 2026"; the key itself if it is not a day. */
fun formatDay(dayKey: String): String {
    if (!isDayKey(dayKey)) return dayKey
    val (year, month, date) = dayKey.split('-').map { it.toInt() }
    val weekday = WEEKDAYS[dayNumberOf(year, month, date).mod(WEEKDAYS.size)]
    return "${weekday.take(SHORT)} $date ${MONTHS[month - 1].take(SHORT)} $year"
}
