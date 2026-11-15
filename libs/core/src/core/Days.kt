package dev.justinsmith.bjj.core

// Calendar days without a calendar: core has no time zone of its own, so whoever builds it says how far local
// time is from UTC at a given moment ([LocalZone]), and the rest is arithmetic on day numbers.

const val MINUTE_MS: Long = 60 * 1000L
const val HOUR_MS: Long = 60 * MINUTE_MS
const val DAY_MS: Long = 24 * HOUR_MS

/** How far local time is ahead of UTC, in milliseconds, at a moment. The app asks the device; a test fixes it. */
fun interface LocalZone {
    fun offsetMillisAt(epochMillis: Long): Long

    companion object {
        val Utc = LocalZone { 0 }
    }
}

/** The local calendar day a moment falls on, counted in days since 1 January 1970. */
fun localDayNumber(timestamp: Long, zone: LocalZone): Long =
    (timestamp + zone.offsetMillisAt(timestamp)).floorDiv(DAY_MS)

/** `YYYY-MM-DD` for a day number, by the proleptic Gregorian calendar. */
fun dayKeyOf(dayNumber: Long): String {
    // Days to civil date, counting years from 1 March so the leap day falls at a year's end, in 400-year eras.
    val shifted = dayNumber + DAYS_FROM_MARCH_YEAR_ZERO
    val era = shifted.floorDiv(DAYS_PER_ERA)
    val dayOfEra = shifted - era * DAYS_PER_ERA
    val leapDays = dayOfEra / (DAYS_PER_4_YEARS - 1) - dayOfEra / DAYS_PER_CENTURY + dayOfEra / (DAYS_PER_ERA - 1)
    val yearOfEra = (dayOfEra - leapDays) / DAYS_PER_YEAR
    val dayOfYear = dayOfEra - (DAYS_PER_YEAR * yearOfEra + yearOfEra / YEARS_PER_LEAP - yearOfEra / YEARS_PER_CENTURY)
    // Months from March come in a five-month pattern of 153 days.
    val marchMonth = (MONTHS_PER_PATTERN * dayOfYear + 2) / DAYS_PER_PATTERN
    val day = dayOfYear - (DAYS_PER_PATTERN * marchMonth + 2) / MONTHS_PER_PATTERN + 1
    val month = if (marchMonth <
        MARCH_MONTHS_BEFORE_JANUARY
    ) {
        marchMonth + MARCH
    } else {
        marchMonth - MARCH_MONTHS_BEFORE_JANUARY + 1
    }
    val year = yearOfEra + era * YEARS_PER_ERA + if (month <= FEBRUARY) 1 else 0
    return listOf(year.pad(YEAR_DIGITS), month.pad(2), day.pad(2)).joinToString("-")
}

private fun Long.pad(digits: Int) = toString().padStart(digits, '0')

/** The day number of a calendar date, the reverse of [dayKeyOf]. */
fun dayNumberOf(year: Int, month: Int, day: Int): Long {
    // Counting years from 1 March, as [dayKeyOf] does.
    val marchYear = (if (month <= FEBRUARY) year - 1 else year).toLong()
    val era = marchYear.floorDiv(YEARS_PER_ERA)
    val yearOfEra = marchYear - era * YEARS_PER_ERA
    val marchMonth = if (month > FEBRUARY) month - MARCH else month + MARCH_MONTHS_BEFORE_JANUARY - 1
    val dayOfYear = (DAYS_PER_PATTERN * marchMonth + 2) / MONTHS_PER_PATTERN + day - 1
    val dayOfEra = yearOfEra * DAYS_PER_YEAR + yearOfEra / YEARS_PER_LEAP - yearOfEra / YEARS_PER_CENTURY + dayOfYear
    return era * DAYS_PER_ERA + dayOfEra - DAYS_FROM_MARCH_YEAR_ZERO
}

/** The local calendar day a moment falls on, as `YYYY-MM-DD`. */
fun localDayKey(timestamp: Long, zone: LocalZone): String = dayKeyOf(localDayNumber(timestamp, zone))

private const val DAYS_FROM_MARCH_YEAR_ZERO = 719_468L
private const val DAYS_PER_ERA = 146_097L
private const val DAYS_PER_YEAR = 365L
private const val DAYS_PER_4_YEARS = 1461L
private const val DAYS_PER_CENTURY = 36_524L
private const val YEARS_PER_LEAP = 4
private const val YEARS_PER_CENTURY = 100
private const val YEARS_PER_ERA = 400
private const val MONTHS_PER_PATTERN = 5
private const val DAYS_PER_PATTERN = 153
private const val MARCH = 3
private const val FEBRUARY = 2
private const val MARCH_MONTHS_BEFORE_JANUARY = 10
private const val YEAR_DIGITS = 4
