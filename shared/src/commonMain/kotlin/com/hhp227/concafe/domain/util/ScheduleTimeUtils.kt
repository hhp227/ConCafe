package com.hhp227.concafe.domain.util

fun computeScheduleDurationMinutes(startTime: String, endTime: String): Int {
    val startMinutes = startTime.toScheduleMinutes()
    val endMinutes = endTime.toScheduleMinutes()
    val durationMinutes = endMinutes - startMinutes
    return if (durationMinutes < 0) durationMinutes + 24 * 60 else durationMinutes
}

fun isScheduleEndAfterStart(startTime: String, endTime: String): Boolean {
    return computeScheduleDurationMinutes(startTime, endTime) > 0
}

fun isWithinScheduleTime(currentMinutes: Int, startTime: String, endTime: String): Boolean {
    val startMinutes = startTime.toScheduleMinutes()
    val endMinutes = endTime.toScheduleMinutes()
    val adjustedEndMinutes = if (endMinutes < startMinutes) endMinutes + 24 * 60 else endMinutes
    val adjustedCurrentMinutes = if (endMinutes < startMinutes && currentMinutes < startMinutes) {
        currentMinutes + 24 * 60
    } else {
        currentMinutes
    }
    return adjustedCurrentMinutes in startMinutes..adjustedEndMinutes
}

private fun String.toScheduleMinutes(): Int {
    val hour = substringBefore(':').toIntOrNull() ?: 0
    val minute = substringAfter(':').toIntOrNull() ?: 0
    return hour * 60 + minute
}
