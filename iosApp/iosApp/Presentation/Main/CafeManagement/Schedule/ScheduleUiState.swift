//
//  ScheduleUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/11.
//

import Foundation

struct ScheduleUiState {
    var isLoading: Bool = false
    var isSaving: Bool = false
    var errorMessage: String?
    var castSummary = CastSummary()
    var weekRangeLabel: String = ""
    var weekDays: [WeekDay] = []
    var schedules: [DaySchedule] = []
    var selectedDayId: String = ""
    var infoMessage: String?

    struct CastSummary {
        var title: String = ""
        var subtitle: String = ""
        var badge: String = "Cast Member"
        var initials: String = ""
    }

    struct WeekDay: Identifiable {
        let id: String
        let label: String
        let number: String
        var isSelected: Bool
        let isWorking: Bool
    }

    struct DaySchedule: Identifiable {
        let id: String
        let title: String
        let timeLabel: String
        let statusLabel: String
        let isWorking: Bool
    }
}
