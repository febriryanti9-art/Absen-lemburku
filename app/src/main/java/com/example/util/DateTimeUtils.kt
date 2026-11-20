package com.example.util

import com.example.ui.i18n.AppStrings
import java.util.Calendar
import java.util.Locale

data class CalendarDayCell(
    val dayOfMonth: Int?,
    val isoDate: String?
)

data class DurationCalculation(
    val totalMinutes: Int,
    val isOvernight: Boolean
) {
    val hours: Int get() = totalMinutes / 60
    val minutes: Int get() = totalMinutes % 60
}

object DateTimeUtils {

    fun todayIsoDate(): String {
        val cal = Calendar.getInstance()
        return formatIsoDate(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun todayDayOfMonth(): Int = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

    fun currentYearMonth(): Pair<Int, Int> {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.YEAR) to (cal.get(Calendar.MONTH) + 1)
    }

    fun currentHourOfDay(): Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

    fun formatIsoDate(year: Int, month: Int, day: Int): String {
        return String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
    }

    fun parseIsoDate(isoDate: String): Triple<Int, Int, Int>? {
        val parts = isoDate.split("-")
        if (parts.size != 3) return null
        val y = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        val d = parts[2].toIntOrNull() ?: return null
        return Triple(y, m, d)
    }

    fun isoDateToEpochMillis(isoDate: String): Long {
        val (y, m, d) = parseIsoDate(isoDate) ?: return System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, y)
        cal.set(Calendar.MONTH, m - 1)
        cal.set(Calendar.DAY_OF_MONTH, d)
        cal.set(Calendar.HOUR_OF_DAY, 12)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun epochMillisToIsoDate(millis: Long): String {
        val cal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        cal.timeInMillis = millis
        return formatIsoDate(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun daysInMonth(year: Int, month: Int): Int {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month - 1)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun shiftMonth(year: Int, month: Int, deltaMonths: Int): Pair<Int, Int> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month - 1)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.add(Calendar.MONTH, deltaMonths)
        return cal.get(Calendar.YEAR) to (cal.get(Calendar.MONTH) + 1)
    }

    fun buildMonthCalendarCells(year: Int, month: Int): List<CalendarDayCell> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month - 1)
        cal.set(Calendar.DAY_OF_MONTH, 1)

        // Calendar.SUNDAY = 1 .. Calendar.SATURDAY = 7 -> 0..6 offset
        val firstDayOffset = cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
        val totalDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val cells = mutableListOf<CalendarDayCell>()
        repeat(firstDayOffset) {
            cells.add(CalendarDayCell(dayOfMonth = null, isoDate = null))
        }
        for (day in 1..totalDays) {
            cells.add(CalendarDayCell(dayOfMonth = day, isoDate = formatIsoDate(year, month, day)))
        }
        while (cells.size % 7 != 0) {
            cells.add(CalendarDayCell(dayOfMonth = null, isoDate = null))
        }
        return cells
    }

    fun yearMonthPrefix(year: Int, month: Int): String {
        return String.format(Locale.US, "%04d-%02d", year, month)
    }

    fun formatTime24(hour: Int, minute: Int): String {
        return String.format(Locale.US, "%02d:%02d", hour.coerceIn(0, 23), minute.coerceIn(0, 59))
    }

    fun calculateDuration(
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int
    ): DurationCalculation {
        val startTotal = startHour * 60 + startMinute
        val endTotal = endHour * 60 + endMinute
        if (startTotal == endTotal) {
            return DurationCalculation(totalMinutes = 0, isOvernight = false)
        }
        return if (endTotal > startTotal) {
            DurationCalculation(totalMinutes = endTotal - startTotal, isOvernight = false)
        } else {
            // Crosses midnight into next day
            DurationCalculation(totalMinutes = (24 * 60 - startTotal) + endTotal, isOvernight = true)
        }
    }

    fun formatDurationFull(totalMinutes: Int, strings: AppStrings): String {
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        return when {
            h > 0 && m > 0 -> "$h ${strings.hoursUnit} $m ${strings.minutesUnit}"
            h > 0 -> "$h ${strings.hoursUnit}"
            else -> "$m ${strings.minutesUnit}"
        }
    }

    fun formatDurationCompact(totalMinutes: Int, strings: AppStrings): String {
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        return when {
            h > 0 && m > 0 -> "${h}${strings.hoursShort} ${m}${strings.minutesShort}"
            h > 0 -> "${h}${strings.hoursShort}"
            else -> "${m}${strings.minutesShort}"
        }
    }

    fun formatDisplayDate(isoDate: String, strings: AppStrings): String {
        val (y, m, d) = parseIsoDate(isoDate) ?: return isoDate
        val monthName = strings.monthsFull.getOrNull(m - 1) ?: m.toString()
        return "$d $monthName $y"
    }

    fun formatShortDate(isoDate: String, strings: AppStrings): String {
        val (_, m, d) = parseIsoDate(isoDate) ?: return isoDate
        val monthShort = (strings.monthsFull.getOrNull(m - 1) ?: "").take(3)
        return "$d $monthShort"
    }

    fun formatMonthYear(year: Int, month: Int, strings: AppStrings): String {
        val monthName = strings.monthsFull.getOrNull(month - 1) ?: month.toString()
        return "$monthName $year"
    }

    fun currentTimeParts(): Triple<Int, Int, Int> {
        val cal = Calendar.getInstance()
        return Triple(
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            cal.get(Calendar.SECOND)
        )
    }

    fun formatClock24WithSeconds(epochMillis: Long): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = epochMillis
        return String.format(
            Locale.US,
            "%02d:%02d:%02d",
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            cal.get(Calendar.SECOND)
        )
    }

    fun formatStopwatchHms(elapsedMillis: Long): String {
        val totalSeconds = (elapsedMillis / 1000L).coerceAtLeast(0L)
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun epochMillisToLocalDateAndTime(epochMillis: Long): Triple<String, Int, Int> {
        val cal = Calendar.getInstance()
        cal.timeInMillis = epochMillis
        val iso = formatIsoDate(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
        return Triple(iso, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
    }

    fun generateRandomCompanyCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val suffix = (1..4).map { chars.random() }.joinToString("")
        return "TIM-$suffix"
    }
}
