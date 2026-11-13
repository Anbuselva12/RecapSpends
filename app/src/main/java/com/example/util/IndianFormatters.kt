package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

object IndianFormatters {
    private val months = arrayOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )
    private val weekdays = arrayOf("S", "M", "T", "W", "T", "F", "S")

    fun formatIndianInteger(value: Long): String {
        val negative = value < 0
        val absStr = abs(value).toString()
        val formatted = if (absStr.length <= 3) {
            absStr
        } else {
            val lastThree = absStr.takeLast(3)
            val rest = absStr.dropLast(3)
            val grouped = rest.reversed().chunked(2).joinToString(",").reversed()
            "$grouped,$lastThree"
        }
        return if (negative) "-$formatted" else formatted
    }

    fun formatInr(amount: Double, decimals: Int = 0): String {
        val negative = amount < 0
        val absVal = abs(amount)
        val result = if (decimals <= 0) {
            "₹" + formatIndianInteger(absVal.roundToLong())
        } else {
            val factor = Math.pow(10.0, decimals.toDouble())
            val rounded = (absVal * factor).roundToLong()
            val intPart = rounded / factor.toLong()
            val fracPart = (rounded % factor.toLong()).toString().padStart(decimals, '0')
            "₹${formatIndianInteger(intPart)}.$fracPart"
        }
        return if (negative) "-$result" else result
    }

    fun formatLakhCrore(amount: Double): String {
        val absVal = abs(amount)
        val sign = if (amount < 0) "-" else ""
        return when {
            absVal >= 10_000_000.0 -> {
                val cr = absVal / 10_000_000.0
                val s = DecimalFormat("0.##", DecimalFormatSymbols(Locale.US)).format(cr)
                "${sign}₹$s Cr"
            }
            absVal >= 100_000.0 -> {
                val l = absVal / 100_000.0
                val s = DecimalFormat("0.##", DecimalFormatSymbols(Locale.US)).format(l)
                "${sign}₹$s L"
            }
            else -> formatInr(amount)
        }
    }

    fun formatDate(epochMs: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = epochMs }
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val mon = months[cal.get(Calendar.MONTH)]
        val year = cal.get(Calendar.YEAR)
        return "$day $mon $year"
    }

    fun formatShortDate(epochMs: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = epochMs }
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val mon = months[cal.get(Calendar.MONTH)]
        return "$day $mon"
    }

    fun dayOfMonth(epochMs: Long): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = epochMs }
        return cal.get(Calendar.DAY_OF_MONTH)
    }

    fun dayInitial(epochMs: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = epochMs }
        return weekdays[cal.get(Calendar.DAY_OF_WEEK) - 1]
    }

    fun sameDay(aMs: Long, bMs: Long): Boolean {
        val ca = Calendar.getInstance().apply { timeInMillis = aMs }
        val cb = Calendar.getInstance().apply { timeInMillis = bMs }
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
            ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
    }

    fun startOfDay(epochMs: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = epochMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun addDays(epochMs: Long, days: Int): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = epochMs
            add(Calendar.DAY_OF_YEAR, days)
        }
        return cal.timeInMillis
    }

    fun addMonths(epochMs: Long, monthsToAdd: Int): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = epochMs
            add(Calendar.MONTH, monthsToAdd)
        }
        return cal.timeInMillis
    }

    fun daysBetween(targetMs: Long, baseMs: Long): Int {
        val t = startOfDay(targetMs)
        val b = startOfDay(baseMs)
        return ((t - b) / 86_400_000L).toInt()
    }

    fun epochForDate(year: Int, month1to12: Int, day: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(year, month1to12 - 1, day, 9, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun isSameMonth(epochMs: Long, refMs: Long): Boolean {
        val ca = Calendar.getInstance().apply { timeInMillis = epochMs }
        val cb = Calendar.getInstance().apply { timeInMillis = refMs }
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
            ca.get(Calendar.MONTH) == cb.get(Calendar.MONTH)
    }

    fun daysLeftInMonth(refMs: Long): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = refMs }
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val today = cal.get(Calendar.DAY_OF_MONTH)
        return (maxDay - today).coerceAtLeast(0)
    }

    fun monthLabel(refMs: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = refMs }
        return months[cal.get(Calendar.MONTH)]
    }

    fun fullMonthHeader(refMs: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = refMs }
        val dow = when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> "Sunday"
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            else -> "Saturday"
        }
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val mon = months[cal.get(Calendar.MONTH)]
        val yr = cal.get(Calendar.YEAR)
        return "$dow, $day $mon $yr"
    }
}
