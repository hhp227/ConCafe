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
        guard isEditingWorking else { return "0시간" }
        let duration = Self.computeDurationMinutes(start: editStartTime, end: editEndTime)
        let actual = max(duration - 60, 0)
        let hours = actual / 60
        let minutes = actual % 60
        return minutes == 0 ? "\(hours)시간" : "\(hours)시간 \(minutes)분"
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
        var options: [String] = []
        for hour in 8...23 {
            options.append(String(format: "%02d:00", hour))
            if hour != 23 {
                options.append(String(format: "%02d:30", hour))
            }
        }
        return options
    }

    static func computeDurationMinutes(start: String, end: String) -> Int {
        func parse(_ time: String) -> Int {
            let parts = time.split(separator: ":")
            let hour = Int(parts.first ?? "0") ?? 0
            let minute = Int(parts.dropFirst().first ?? "0") ?? 0
            return hour * 60 + minute
        }

        return max(parse(end) - parse(start), 0)
    }
}
