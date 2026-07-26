//
//  CastManagementViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/11.
//

import Foundation
import Combine
import Shared

@MainActor
final class CastManagementViewModel: ObservableObject {
    private let cafeId: String

    private let cafeName: String

    private let getCafeScheduleCalendarUseCase: GetCafeScheduleCalendarUseCase

    @Published private(set) var uiState: CastManagementUiState

    let event = PassthroughSubject<CastManagementEvent, Never>()

    private var tasks: [TaskKey: Task<Void, Never>] = [:]

    private func loadCalendar() {
        tasks[.load]?.cancel()
        uiState.isLoading = true
        uiState.errorMessage = nil

        let (fromDate, toDate, mode) = computeDateRange(viewMode: uiState.viewMode)
        uiState.periodStart = fromDate
        uiState.periodEnd = toDate

        tasks[.load] = Task {
            do {
                let result = try await getCafeScheduleCalendarUseCase.invoke(
                    cafeId: cafeId,
                    fromDate: self.formatDate(fromDate),
                    toDate: self.formatDate(toDate)
                )
                if Task.isCancelled { return }

                if let success = result as? AppResultSuccess<AnyObject>,
                   let days = success.data as? [CafeCalendarDay] {
                    switch mode {
                    case .week:
                        buildWeekState(days: days, fromDate: fromDate)
                    case .month:
                        buildMonthState(days: days, fromDate: fromDate)
                    }
                    uiState.isLoading = false
                } else {
                    uiState.isLoading = false
                    uiState.errorMessage = "cast_management_error_load_failed"
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isLoading = false
                uiState.errorMessage = "cast_management_error_load_failed"
            }
        }
    }

    private func computeDateRange(viewMode: CastScheduleViewMode) -> (fromDate: Date, toDate: Date, mode: CastScheduleViewMode) {
        let calendar = Calendar(identifier: .iso8601)
        let today = Date()

        if viewMode == .week {
            let weekday = calendar.component(.weekday, from: today)
            let daysFromMonday = weekday == 1 ? 6 : weekday - 2
            let monday = calendar.date(byAdding: .day, value: -daysFromMonday, to: today) ?? today
            let sunday = calendar.date(byAdding: .day, value: 6, to: monday) ?? today
            return (monday, sunday, .week)
        } else {
            let components = calendar.dateComponents([.year, .month], from: today)
            var firstComponents = DateComponents()
            firstComponents.year = components.year
            firstComponents.month = components.month
            firstComponents.day = 1
            let firstDay = calendar.date(from: firstComponents) ?? today
            let range = calendar.range(of: .day, in: .month, for: firstDay)!
            let lastDay = calendar.date(byAdding: .day, value: range.count - 1, to: firstDay) ?? today
            return (firstDay, lastDay, .month)
        }
    }

    private func buildWeekState(days: [CafeCalendarDay], fromDate: Date) {
        let calendar = Calendar(identifier: .iso8601)
        let shortSymbols = localizedWeekdayShortSymbols()

        uiState.weekColumns = (0..<7).map { offset in
            let date = calendar.date(byAdding: .day, value: offset, to: fromDate)!
            let dateStr = formatDate(date)
            let monthDay = localizedShortDate(date)
            let castNames = days.first(where: { $0.date == dateStr })?.castNames ?? []
            return CastManagementUiState.WeekColumn(
                id: dateStr,
                dayLabel: shortSymbols[offset],
                dateLabel: monthDay,
                castNames: castNames
            )
        }
    }

    private func buildMonthState(days: [CafeCalendarDay], fromDate: Date) {
        let calendar = Calendar(identifier: .iso8601)
        let weekday = calendar.component(.weekday, from: fromDate)
        let offset = weekday == 1 ? 6 : weekday - 2
        uiState.monthOffset = offset

        let range = calendar.range(of: .day, in: .month, for: fromDate)!
        uiState.monthCells = range.map { dayNumber in
            var comps = calendar.dateComponents([.year, .month], from: fromDate)
            comps.day = dayNumber
            let date = calendar.date(from: comps)!
            let dateStr = formatDate(date)
            let castNames = days.first(where: { $0.date == dateStr })?.castNames ?? []
            return CastManagementUiState.MonthCell(
                id: dateStr,
                dayNumber: dayNumber,
                castNames: castNames
            )
        }
    }

    private func formatDate(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        formatter.locale = Locale(identifier: "en_US_POSIX")
        return formatter.string(from: date)
    }

    private func localizedShortDate(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale.current
        formatter.setLocalizedDateFormatFromTemplate("Md")
        return formatter.string(from: date)
    }

    // Returns Mon–Sun short weekday symbols in ISO order (Monday first)
    private func localizedWeekdayShortSymbols() -> [String] {
        var cal = Calendar.current
        cal.locale = Locale.current
        let symbols = cal.veryShortWeekdaySymbols // Sun=0, Mon=1, ..., Sat=6
        return Array(symbols[1...]) + [symbols[0]] // Mon, Tue, ..., Sun
    }

    func onAction(_ action: CastManagementAction) {
        switch action {
        case .clickBack:
            event.send(.navigateBack)
        case .changeViewMode(let mode):
            guard uiState.viewMode != mode else { return }
            uiState.viewMode = mode
            loadCalendar()
        }
    }

    init(
        cafeId: String,
        cafeName: String,
        getCafeScheduleCalendarUseCase: GetCafeScheduleCalendarUseCase = KoinInitializerKt.resolveGetCafeScheduleCalendarUseCase()
    ) {
        self.cafeId = cafeId
        self.cafeName = cafeName
        self.getCafeScheduleCalendarUseCase = getCafeScheduleCalendarUseCase
        self.uiState = CastManagementUiState(cafeName: cafeName)
        loadCalendar()
    }

    deinit {
        tasks.values.forEach { $0.cancel() }
        tasks.removeAll()
    }

    private enum TaskKey {
        case load
    }
}
