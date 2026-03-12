//
//  ScheduleUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/11.
//

import Foundation

struct ScheduleUiState {
    var isSaving: Bool = false
    var castSummary = CastSummary()
    var weekRangeLabel: String = "2024년 5월 12일 - 5월 18일"
    var weekDays: [WeekDay] = WeekDay.defaults
    var schedules: [DaySchedule] = DaySchedule.defaults
    var selectedDayId: String = WeekDay.defaults.first(where: \.isSelected)?.id ?? ""
    var infoMessage: String?

    struct CastSummary {
        var title: String = "사쿠라 (Sakura)"
        var subtitle: String = "메이드 / No. 12 / 에이스"
        var badge: String = "Cast Member"
    }

    struct WeekDay: Identifiable {
        let id: String
        let label: String
        let number: String
        var isSelected: Bool

        static let defaults: [WeekDay] = [
            .init(id: "2024-05-12", label: "12(일)", number: "12", isSelected: false),
            .init(id: "2024-05-13", label: "13(월)", number: "13", isSelected: true),
            .init(id: "2024-05-14", label: "14(화)", number: "14", isSelected: false),
            .init(id: "2024-05-15", label: "15(수)", number: "15", isSelected: false),
            .init(id: "2024-05-16", label: "16(목)", number: "16", isSelected: false)
        ]
    }

    struct DaySchedule: Identifiable {
        let id: String
        let title: String
        let timeLabel: String
        let statusLabel: String
        let isWorking: Bool

        static let defaults: [DaySchedule] = [
            .init(id: "2024-05-13", title: "5월 13일 (월)", timeLabel: "14:00 - 22:00 (8시간)", statusLabel: "근무 중", isWorking: true),
            .init(id: "2024-05-14", title: "5월 14일 (화)", timeLabel: "16:00 - 23:00 (7시간)", statusLabel: "근무 중", isWorking: true),
            .init(id: "2024-05-15", title: "5월 15일 (수)", timeLabel: "일정이 없습니다", statusLabel: "휴무", isWorking: false),
            .init(id: "2024-05-16", title: "5월 16일 (목)", timeLabel: "14:00 - 22:00 (8시간)", statusLabel: "근무 중", isWorking: true),
            .init(id: "2024-05-17", title: "5월 17일 (금)", timeLabel: "17:00 - 24:00 (7시간)", statusLabel: "근무 중", isWorking: true)
        ]
    }
}
