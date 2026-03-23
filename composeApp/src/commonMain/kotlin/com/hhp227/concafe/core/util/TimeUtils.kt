package com.hhp227.concafe.core.util

object TimeUtils {
    private const val MILLIS_PER_SECOND = 1000L
    private const val MILLIS_PER_MINUTE = 60L * MILLIS_PER_SECOND
    private const val MILLIS_PER_HOUR = 60L * MILLIS_PER_MINUTE
    private const val MILLIS_PER_DAY = 24L * MILLIS_PER_HOUR
    private val WEEKDAY_LABELS = listOf("월", "화", "수", "목", "금", "토", "일")

    fun weekdayLabelFromIsoDateOrNull(date: String): String? {
        val dayOfWeekIndex = dayOfWeekIndexFromIsoDateOrNull(date) ?: return null
        return WEEKDAY_LABELS.getOrNull(dayOfWeekIndex)
    }

    fun epochDayFromIsoDateOrNull(value: String): Long? {
        if (value.length != 10 || value[4] != '-' || value[7] != '-') return null

        val year = value.substring(0, 4).toIntOrNull() ?: return null
        val month = value.substring(5, 7).toIntOrNull() ?: return null
        val day = value.substring(8, 10).toIntOrNull() ?: return null
        if (month !in 1..12 || day !in 1..31) return null

        val adjustedYear = year - if (month <= 2) 1 else 0
        val era = if (adjustedYear >= 0) adjustedYear / 400 else (adjustedYear - 399) / 400
        val yearOfEra = adjustedYear - era * 400
        val adjustedMonth = month + if (month > 2) -3 else 9
        val dayOfYear = (153 * adjustedMonth + 2) / 5 + day - 1
        val dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear
        return era * 146097L + dayOfEra - 719468L
    }

    fun relativeVisitedLabel(
        visitedAt: String,
        visitedLabel: String,
        referenceDate: String
    ): String {
        val visitedDate = visitedAt.take(10)
        val referenceEpochDay = epochDayFromIsoDateOrNull(referenceDate) ?: return visitedLabel
        val visitedEpochDay = epochDayFromIsoDateOrNull(visitedDate) ?: return visitedLabel
        val daysAgo = referenceEpochDay - visitedEpochDay
        return when {
            daysAgo < 0 -> visitedLabel
            daysAgo.toInt() == 0 -> "오늘"
            daysAgo.toInt() == 1 -> "어제"
            else -> "${daysAgo}일 전"
        }
    }

    fun extractHourFromEpochMillis(millis: Long): Int {
        val normalized = ((millis % MILLIS_PER_DAY) + MILLIS_PER_DAY) % MILLIS_PER_DAY
        return (normalized / MILLIS_PER_HOUR).toInt()
    }

    fun extractMinuteFromEpochMillis(millis: Long): Int {
        val normalized = ((millis % MILLIS_PER_DAY) + MILLIS_PER_DAY) % MILLIS_PER_DAY
        return ((normalized % MILLIS_PER_HOUR) / MILLIS_PER_MINUTE).toInt()
    }

    fun formatIsoDateFromEpochMillis(millis: Long): String {
        val (year, month, day) = dateFromEpochMillis(millis)
        return "${year}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
    }

    fun formatHourMinute(hour: Int, minute: Int): String {
        return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
    }

    fun parseHourMinuteOrDefault(
        value: String,
        defaultHour: Int = 10,
        defaultMinute: Int = 0
    ): Pair<Int, Int> {
        val matched = Regex("""^\s*(\d{1,2}):(\d{2})\s*$""").find(value)
        val hour = matched?.groupValues?.getOrNull(1)?.toIntOrNull()
        val minute = matched?.groupValues?.getOrNull(2)?.toIntOrNull()
        return if (hour != null && minute != null && hour in 0..23 && minute in 0..59) {
            hour to minute
        } else {
            defaultHour to defaultMinute
        }
    }

    fun extractNormalizedHourMinuteList(value: String): List<String> {
        return Regex("""(\d{1,2}):(\d{2})""")
            .findAll(value)
            .mapNotNull { match ->
                val hour = match.groupValues[1].toIntOrNull() ?: return@mapNotNull null
                val minute = match.groupValues[2].toIntOrNull() ?: return@mapNotNull null
                return@mapNotNull if (hour in 0..23 && minute in 0..59) {
                    formatHourMinute(hour, minute)
                } else {
                    null
                }
            }
            .toList()
    }

    fun normalizeBirthdayInput(raw: String): String {
        val digits = raw.filter { it.isDigit() }.take(8)
        return when {
            digits.length <= 2 -> digits
            digits.length <= 4 -> "${digits.take(2)}/${digits.drop(2)}"
            else -> "${digits.take(2)}/${digits.substring(2, 4)}/${digits.drop(4)}"
        }
    }

    fun parseBirthdayToEpochMillisOrNull(value: String): Long? {
        val matched = Regex("""^\s*(\d{2})/(\d{2})/(\d{4})\s*$""").find(value) ?: return null
        val month = matched.groupValues[1].toIntOrNull() ?: return null
        val day = matched.groupValues[2].toIntOrNull() ?: return null
        val year = matched.groupValues[3].toIntOrNull() ?: return null
        if (month !in 1..12 || day !in 1..31) return null
        val iso = "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
        val epochDay = epochDayFromIsoDateOrNull(iso) ?: return null
        return epochDay * MILLIS_PER_DAY
    }

    fun formatBirthdayFromEpochMillis(millis: Long): String {
        val (year, month, day) = dateFromEpochMillis(millis)
        val monthText = month.toString().padStart(2, '0')
        val dayText = day.toString().padStart(2, '0')
        val yearText = year.toString().padStart(4, '0')
        return "$monthText/$dayText/$yearText"
    }

    fun buildVisitedAtUtcString(dateMillis: Long, hour: Int, minute: Int): String {
        return "${formatIsoDateFromEpochMillis(dateMillis)}T${formatHourMinute(hour, minute)}:00Z"
    }

    fun defaultHalfHourTimeOptions(startHour: Int = 8, endHour: Int = 23): List<String> {
        val options = mutableListOf<String>()

        for (hour in startHour..endHour) {
            options += "%02d:00".format(hour)
            if (hour != endHour) {
                options += "%02d:30".format(hour)
            }
        }
        return options
    }

    fun computeDurationMinutes(start: String, end: String): Int {
        val startMinutes = parseTimeToMinutes(start)
        val endMinutes = parseTimeToMinutes(end)
        return (endMinutes - startMinutes).coerceAtLeast(0)
    }

    private fun dayOfWeekIndexFromIsoDateOrNull(date: String): Int? {
        val parts = date.split("-")
        if (parts.size != 3) return null
        val year = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null
        val day = parts[2].toIntOrNull() ?: return null
        return dayOfWeekIndex(year, month, day)
    }

    private fun dayOfWeekIndex(year: Int, month: Int, day: Int): Int {
        var adjustedYear = year
        var adjustedMonth = month
        if (adjustedMonth < 3) {
            adjustedMonth += 12
            adjustedYear -= 1
        }
        val k = adjustedYear % 100
        val j = adjustedYear / 100
        val h = (day + (13 * (adjustedMonth + 1)) / 5 + k + (k / 4) + (j / 4) + (5 * j)) % 7
        return when (h) {
            2 -> 0
            3 -> 1
            4 -> 2
            5 -> 3
            6 -> 4
            0 -> 5
            else -> 6
        }
    }

    private fun parseTimeToMinutes(time: String): Int {
        val parts = time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return hour * 60 + minute
    }

    private fun dateFromEpochMillis(millis: Long): DateParts {
        val epochDays = millis / MILLIS_PER_DAY
        val z = epochDays + 719468L
        val era = z / 146097L
        val doe = z - era * 146097L
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
        val y = yoe + era * 400
        val doy = doe - (365L * yoe + yoe / 4 - yoe / 100)
        val mp = (5L * doy + 2L) / 153
        val day = (doy - (153L * mp + 2L) / 5 + 1L).toInt()
        val month = (mp + if (mp < 10L) 3L else -9L).toInt()
        val year = (y + if (month <= 2) 1L else 0L).toInt()
        return DateParts(year = year, month = month, day = day)
    }

    private data class DateParts(
        val year: Int,
        val month: Int,
        val day: Int
    )
}
