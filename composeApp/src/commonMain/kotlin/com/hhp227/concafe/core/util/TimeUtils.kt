package com.hhp227.concafe.core.util

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

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
        val normalized = normalizeDateOnlyOrNull(value) ?: return null
        val year = normalized.substring(0, 4).toIntOrNull() ?: return null
        val month = normalized.substring(5, 7).toIntOrNull() ?: return null
        val day = normalized.substring(8, 10).toIntOrNull() ?: return null
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
        val zone = TimeZone.currentSystemDefault()
        val localDateTime = Instant.fromEpochMilliseconds(millis).toLocalDateTime(zone)
        return localDateTime.hour
    }

    fun extractMinuteFromEpochMillis(millis: Long): Int {
        val zone = TimeZone.currentSystemDefault()
        val localDateTime = Instant.fromEpochMilliseconds(millis).toLocalDateTime(zone)
        return localDateTime.minute
    }

    fun formatIsoDateFromEpochMillis(millis: Long): String {
        val zone = TimeZone.currentSystemDefault()
        val localDate = Instant.fromEpochMilliseconds(millis).toLocalDateTime(zone).date
        return "${localDate.year}-${localDate.monthNumber.toString().padStart(2, '0')}-${localDate.dayOfMonth.toString().padStart(2, '0')}"
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
        return if (hour != null && minute != null && hour in 0..23 && minute in 0..59) hour to minute
        else defaultHour to defaultMinute
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
        val zone = TimeZone.currentSystemDefault()
        val localDate = Instant.fromEpochMilliseconds(millis).toLocalDateTime(zone).date
        val monthText = localDate.monthNumber.toString().padStart(2, '0')
        val dayText = localDate.dayOfMonth.toString().padStart(2, '0')
        val yearText = localDate.year.toString().padStart(4, '0')
        return "$monthText/$dayText/$yearText"
    }

    fun buildVisitedAtUtcString(dateMillis: Long, hour: Int, minute: Int): String {
        val zone = TimeZone.currentSystemDefault()
        val localDate = Instant.fromEpochMilliseconds(dateMillis).toLocalDateTime(zone).date
        val localDateTime = LocalDateTime(
            year = localDate.year,
            monthNumber = localDate.monthNumber,
            dayOfMonth = localDate.dayOfMonth,
            hour = hour.coerceIn(0, 23),
            minute = minute.coerceIn(0, 59),
            second = 0,
            nanosecond = 0
        )
        return localDateTime.toInstant(zone).toString()
    }

    fun currentIsoDate(): String {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return "${now.year}-${now.monthNumber.toString().padStart(2, '0')}-${now.dayOfMonth.toString().padStart(2, '0')}"
    }

    fun isCurrentDateVisitedAt(visitedAt: String): Boolean {
        val today = currentIsoDate()
        val localDate = runCatching {
            Instant.parse(visitedAt)
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date
                .toString()
        }.getOrNull()
        return if (localDate == null) visitedAt.startsWith(today) else localDate == today
    }

    fun currentMonthDayLabelKorean(): String {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return "${now.monthNumber}월 ${now.dayOfMonth}일"
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
        val normalized = normalizeDateOnlyOrNull(date) ?: return null
        val parts = normalized.split("-")
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

    private fun normalizeDateOnlyOrNull(value: String): String? {
        val candidate = value.trim()
        val regex = Regex("""(\d{4})[-./](\d{2})[-./](\d{2})""")
        val match = regex.find(candidate) ?: return null
        val year = match.groupValues.getOrNull(1) ?: return null
        val month = match.groupValues.getOrNull(2) ?: return null
        val day = match.groupValues.getOrNull(3) ?: return null
        return "$year-$month-$day"
    }
}
