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
    var schedulePeriod: SchedulePeriod = .oneWeek
    var weekRangeLabel: String = ""
    var weekDays: [ScheduleManagementWeekDay] = []
    var schedules: [ScheduleManagementDaySchedule] = []
    var allWeekDays: [ScheduleManagementWeekDay] = []
    var allSchedules: [ScheduleManagementDaySchedule] = []
    var selectedDayId: String = ""
    var infoMessage: String?
    var editingScheduleId: String?
    var editingScheduleTitle: String = ""
    var editStatus: CastScheduleStatus = .work
    var editStartTime: String = ScheduleUiState.defaultStartTime
    var editEndTime: String = ScheduleUiState.defaultEndTime
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
        var profileImageUrl: String?
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

    static let defaultStartTime = "14:00"

    static let defaultEndTime = "22:00"

    static func computeDurationMinutes(start: String, end: String) -> Int {
        TimeUtils.computeDurationMinutes(start: start, end: end)
    }

    func applyingSchedulePeriod(_ period: SchedulePeriod) -> ScheduleUiState {
        var next = self
        next.schedulePeriod = period
        next.weekDays = Array(allWeekDays.prefix(period.dayCount))
        next.schedules = Array(allSchedules.prefix(period.dayCount))
        next.weekRangeLabel = resolveScheduleRangeLabel(next.weekDays)
        if !next.weekDays.contains(where: { $0.id == next.selectedDayId }) {
            next.selectedDayId = next.weekDays.first?.id ?? ""
        }
        return next
    }
}

enum SchedulePeriod: CaseIterable {
    case oneWeek
    case twoWeeks
    case oneMonth

    var dayCount: Int {
        switch self {
        case .oneWeek: return 7
        case .twoWeeks: return 14
        case .oneMonth: return 30
        }
    }

    var localizationKey: String {
        switch self {
        case .oneWeek: return "schedule_period_week"
        case .twoWeeks: return "schedule_period_two_weeks"
        case .oneMonth: return "schedule_period_month"
        }
    }
}

private func resolveScheduleRangeLabel(_ days: [ScheduleManagementWeekDay]) -> String {
    guard let first = days.first?.id, let last = days.last?.id else { return "" }
    let firstParts = first.split(separator: "-").map(String.init)
    let lastParts = last.split(separator: "-").map(String.init)
    guard firstParts.count == 3, lastParts.count == 3 else { return "" }
    let year = firstParts[0]
    let firstMonth = Int(firstParts[1]) ?? 0
    let firstDay = Int(firstParts[2]) ?? 0
    let lastMonth = Int(lastParts[1]) ?? 0
    let lastDay = Int(lastParts[2]) ?? 0
    return "\(year)년 \(firstMonth)월 \(firstDay)일 - \(lastMonth)월 \(lastDay)일"
}
