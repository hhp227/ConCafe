//
//  CastScheduleAttendanceUtils.swift
//  ConCafe
//
//  Created by Codex on 2026/04/10.
//

import Foundation
import Shared

final class CastScheduleAttendanceUtils {
    static func todaySchedule(
        from schedules: [CastSchedule],
        currentDate: String = TimeUtils.currentIsoDate()
    ) -> CastSchedule? {
        schedules.first { $0.date == currentDate }
    }

    static func todayAttendanceStatus(
        schedules: [CastSchedule],
        currentDate: String = TimeUtils.currentIsoDate(),
        currentMinutes: Int = TimeUtils.currentTimeMinutes()
    ) -> CastAttendanceStatus {
        attendanceStatus(
            schedule: todaySchedule(from: schedules, currentDate: currentDate),
            currentMinutes: currentMinutes
        )
    }

    static func attendanceStatus(
        schedule: CastSchedule?,
        currentMinutes: Int = TimeUtils.currentTimeMinutes()
    ) -> CastAttendanceStatus {
        guard let schedule else {
            return .off
        }

        let startMinutes = TimeUtils.parseTimeMinutes(schedule.startTime)
        let endMinutes = TimeUtils.parseTimeMinutes(schedule.endTime)
        if currentMinutes < startMinutes {
            return .upcoming
        }
        if currentMinutes <= endMinutes {
            return .onShift
        }
        return .completed
    }
}
