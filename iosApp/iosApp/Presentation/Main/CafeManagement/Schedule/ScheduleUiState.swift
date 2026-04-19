//
//  ScheduleUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/11.
//

import Foundation
import Shared

struct ScheduleUiState {
    var managedCastId: String = ""
    var managedCafeId: String = ""
    var managedCafeName: String = ""
    var isLoading: Bool = false
    var isSaving: Bool = false
    var isEditSheetVisible: Bool = false
    var errorMessage: String?
    var castSummary = CastSummary()
    var weekRangeLabel: String = ""
    var weekDays: [ScheduleManagementWeekDay] = []
    var schedules: [ScheduleManagementDaySchedule] = []
    var selectedDayId: String = ""
    var infoMessage: String?
    var editingScheduleId: String?
    var editingScheduleTitle: String = ""
    var editStatus: CastScheduleStatus = .work
    var editStartTime: String = "10:00"
    var editEndTime: String = "19:00"
    var pendingUpdates: [PendingScheduleUpdate] = []
    var timeOptions: [String] = ScheduleUiState.defaultTimeOptions()

    var hasPendingChanges: Bool {
        !pendingUpdates.isEmpty
    }

    var isEditingWorking: Bool {
        editStatus == .work
    }

    var totalWorkDurationLabel: String {
        guard isEditingWorking else { return "schedule_duration_hours_only:0" }
        let duration = TimeUtils.computeDurationMinutes(start: editStartTime, end: editEndTime)
        let actual = max(duration - 60, 0)
        let hours = actual / 60
        let minutes = actual % 60
        return minutes == 0
            ? "schedule_duration_hours_only:\(hours)"
            : "schedule_duration_hours_minutes:\(hours):\(minutes)"
    }

    struct CastSummary {
        var title: String = ""
        var subtitle: String = ""
        var badge: String = "Cast Member"
        var initials: String = ""
    }

    struct PendingScheduleUpdate: Identifiable {
        let date: String
        let status: CastScheduleStatus
        let startTime: String?
        let endTime: String?

        var id: String { date }
    }

    static func defaultTimeOptions() -> [String] {
        TimeUtils.defaultHalfHourTimeOptions()
    }

    static func computeDurationMinutes(start: String, end: String) -> Int {
        TimeUtils.computeDurationMinutes(start: start, end: end)
    }
}
