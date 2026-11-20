package com.bunkerstock.app.platform

import com.bunkerstock.app.data.AppLanguage
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Lightweight, zero-dependency pure-Kotlin proleptic Gregorian date & year-month
 * (Howard Hinnant's exact civil-day algorithms) shared across Android and Web/Wasm.
 */
data class BunkerDate(
    val year: Int,
    val month: Int, // 1..12
    val day: Int    // 1..31
) : Comparable<BunkerDate> {

    fun toEpochDay(): Long {
        val y = year.toLong() - if (month <= 2) 1L else 0L
        val era = if (y >= 0) y / 400L else (y - 399L) / 400L
        val yoe = y - era * 400L
        val m = month.toLong()
        val doy = (153L * (m + if (m > 2) -3L else 9L) + 2L) / 5L + day.toLong() - 1L
        val doe = yoe * 365L + yoe / 4L - yoe / 100L + doy
        return era * 146097L + doe - 719468L
    }

    fun plusDays(days: Long): BunkerDate = fromEpochDay(toEpochDay() + days)

    fun daysUntil(other: BunkerDate): Long = other.toEpochDay() - toEpochDay()

    /** ISO-8601 day of week: 1 = Monday .. 7 = Sunday. */
    fun dayOfWeekIso(): Int {
        val epochDay = toEpochDay() // 1970-01-01 was Thursday (4)
        val mod = ((epochDay + 3L) % 7L + 7L) % 7L
        return (mod + 1L).toInt()
    }

    fun isBefore(other: BunkerDate): Boolean = toEpochDay() < other.toEpochDay()
    fun isAfter(other: BunkerDate): Boolean = toEpochDay() > other.toEpochDay()

    override fun compareTo(other: BunkerDate): Int = toEpochDay().compareTo(other.toEpochDay())

    fun toIso(): String =
        "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"

    fun toDisplayDdMmYyyy(): String =
        "${day.toString().padStart(2, '0')}/${month.toString().padStart(2, '0')}/${year.toString().padStart(4, '0')}"

    fun toDisplayDdMmYy(): String =
        "${day.toString().padStart(2, '0')}/${month.toString().padStart(2, '0')}/${(year % 100).toString().padStart(2, '0')}"

    fun formatHeader(language: AppLanguage): String {
        val daysEs = listOf("LUN", "MAR", "MIÉ", "JUE", "VIE", "SÁB", "DOM")
        val daysEn = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
        val monthsEs = listOf("ENE", "FEB", "MAR", "ABR", "MAY", "JUN", "JUL", "AGO", "SEP", "OCT", "NOV", "DIC")
        val monthsEn = listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")
        val dow = (dayOfWeekIso() - 1).coerceIn(0, 6)
        val mon = (month - 1).coerceIn(0, 11)
        return if (language == AppLanguage.ES) {
            "${daysEs[dow]}, $day ${monthsEs[mon]} $year"
        } else {
            "${daysEn[dow]}, $day ${monthsEn[mon]} $year"
        }
    }

    companion object {
        fun isLeapYear(year: Int): Boolean =
            (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)

        fun lengthOfMonth(year: Int, month: Int): Int = when (month) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            2 -> if (isLeapYear(year)) 29 else 28
            else -> 30
        }

        fun ofOrNull(year: Int, month: Int, day: Int): BunkerDate? {
            if (year < 1 || month !in 1..12) return null
            val maxDay = lengthOfMonth(year, month)
            if (day !in 1..maxDay) return null
            return BunkerDate(year, month, day)
        }

        fun parseIso(raw: String): BunkerDate? {
            val parts = raw.trim().split("-")
            if (parts.size != 3) return null
            val y = parts[0].toIntOrNull() ?: return null
            val m = parts[1].toIntOrNull() ?: return null
            val d = parts[2].toIntOrNull() ?: return null
            return ofOrNull(y, m, d)
        }

        fun fromEpochDay(epochDay: Long): BunkerDate {
            val z = epochDay + 719468L
            val era = if (z >= 0) z / 146097L else (z - 146096L) / 146097L
            val doe = z - era * 146097L
            val yoe = (doe - doe / 1460L + doe / 36524L - doe / 146096L) / 365L
            val y = yoe + era * 400L
            val doy = doe - (365L * yoe + yoe / 4L - yoe / 100L)
            val mp = (5L * doy + 2L) / 153L
            val d = doy - (153L * mp + 2L) / 5L + 1L
            val m = mp + if (mp < 10L) 3L else -9L
            val finalYear = y + if (m <= 2L) 1L else 0L
            return BunkerDate(finalYear.toInt(), m.toInt(), d.toInt())
        }

        fun fromEpochMillisUtc(millis: Long): BunkerDate {
            val dayMs = 86_400_000L
            val epochDay = floor(millis.toDouble() / dayMs.toDouble()).toLong()
            return fromEpochDay(epochDay)
        }
    }
}

data class BunkerYearMonth(
    val year: Int,
    val month: Int // 1..12
) {
    fun atDay(day: Int): BunkerDate = BunkerDate(year, month, day.coerceIn(1, lengthOfMonth()))
    fun lengthOfMonth(): Int = BunkerDate.lengthOfMonth(year, month)

    fun plusMonths(delta: Int): BunkerYearMonth {
        val total = year * 12 + (month - 1) + delta
        val ny = floor(total.toDouble() / 12.0).toInt()
        val nm = ((total % 12) + 12) % 12 + 1
        return BunkerYearMonth(ny, nm)
    }

    fun minusMonths(delta: Int): BunkerYearMonth = plusMonths(-delta)

    fun formatTitle(language: AppLanguage): String {
        val monthsEs = listOf(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        )
        val monthsEn = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        val idx = (month - 1).coerceIn(0, 11)
        val name = if (language == AppLanguage.ES) monthsEs[idx] else monthsEn[idx]
        return "$name $year"
    }

    companion object {
        fun from(date: BunkerDate): BunkerYearMonth = BunkerYearMonth(date.year, date.month)
    }
}

fun formatOneDecimal(value: Double): String {
    val tenths = (value * 10.0).roundToInt()
    val sign = if (tenths < 0) "-" else ""
    val a = abs(tenths)
    return "$sign${a / 10}.${a % 10}"
}

fun formatMacroNumber(value: Double): String {
    val tenths = (value * 10.0).roundToInt()
    return if (tenths % 10 == 0) {
        (tenths / 10).toString()
    } else {
        formatOneDecimal(value)
    }
}

fun urlEncodeUtf8(input: String): String {
    val bytes = input.encodeToByteArray()
    val sb = StringBuilder(bytes.size * 2)
    val hex = "0123456789ABCDEF"
    for (b in bytes) {
        val c = b.toInt() and 0xFF
        val isUnreserved = (c in 'a'.code..'z'.code) ||
            (c in 'A'.code..'Z'.code) ||
            (c in '0'.code..'9'.code) ||
            c == '-'.code || c == '_'.code || c == '.'.code || c == '~'.code
        if (isUnreserved) {
            sb.append(c.toChar())
        } else {
            sb.append('%')
            sb.append(hex[(c ushr 4) and 0xF])
            sb.append(hex[c and 0xF])
        }
    }
    return sb.toString()
}

fun urlDecodeUtf8(input: String): String {
    val out = ArrayList<Byte>(input.length)
    var i = 0
    while (i < input.length) {
        val ch = input[i]
        when {
            ch == '+' -> {
                out.add(' '.code.toByte())
                i++
            }
            ch == '%' && i + 2 < input.length -> {
                val hi = input[i + 1].digitToIntOrNull(16)
                val lo = input[i + 2].digitToIntOrNull(16)
                if (hi != null && lo != null) {
                    out.add(((hi shl 4) or lo).toByte())
                    i += 3
                } else {
                    out.add('%'.code.toByte())
                    i++
                }
            }
            else -> {
                val encoded = ch.toString().encodeToByteArray()
                for (b in encoded) out.add(b)
                i++
            }
        }
    }
    return out.toByteArray().decodeToString()
}
