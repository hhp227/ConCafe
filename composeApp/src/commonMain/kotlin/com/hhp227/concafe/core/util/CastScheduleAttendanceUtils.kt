package com.hhp227.concafe.core.util

import com.hhp227.concafe.domain.model.CastAttendanceStatus
import com.hhp227.concafe.domain.model.CastSchedule

object CastScheduleAttendanceUtils {
    fun todaySchedule(
        schedules: List<CastSchedule>,
        currentDate: String = TimeUtils.currentIsoDate()
    ): CastSchedule? {
        return schedules.firstOrNull { schedule -> schedule.date == currentDate }
    }

    fun todayAttendanceStatus(
        schedules: List<CastSchedule>,
        currentDate: String = TimeUtils.currentIsoDate(),
        currentMinutes: Int = TimeUtils.currentTimeMinutes()
    ): CastAttendanceStatus {
        return attendanceStatus(
            schedule = todaySchedule(schedules, currentDate),
            currentMinutes = currentMinutes
        )
    }

    fun attendanceStatus(
        schedule: CastSchedule?,
        currentMinutes: Int = TimeUtils.currentTimeMinutes()
    ): CastAttendanceStatus {
        if (schedule == null) {
            return CastAttendanceStatus.OFF
        }

        val startMinutes = TimeUtils.parseTimeToMinutes(schedule.startTime)
        val endMinutes = TimeUtils.parseTimeToMinutes(schedule.endTime)
        return when {
            currentMinutes < startMinutes -> CastAttendanceStatus.UPCOMING
            currentMinutes <= endMinutes -> CastAttendanceStatus.ON_SHIFT
            else -> CastAttendanceStatus.COMPLETED
        }
    }
}
