//
//  ScheduleViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/11.
//

import Foundation
import Combine

@MainActor
final class ScheduleViewModel: ObservableObject {
    @Published private(set) var uiState = ScheduleUiState()

    let event = PassthroughSubject<ScheduleEvent, Never>()

    func onAction(_ action: ScheduleAction) {
        switch action {
        case .clickBack:
            event.send(.navigateBack)
        case .clickMore:
            uiState.infoMessage = "추가 메뉴는 다음 단계에서 제공합니다."
        case .clickCalendar:
            uiState.infoMessage = "달력 보기 연결은 다음 단계에서 제공합니다."
        case .selectDay(let id):
            uiState.selectedDayId = id
            uiState.weekDays = uiState.weekDays.map { day in
                var nextDay = day
                nextDay.isSelected = day.id == id
                return nextDay
            }
        case .clickEditDay(let id):
            guard let selected = uiState.schedules.first(where: { $0.id == id }) else { return }
            uiState.infoMessage = "\(selected.title) 수정은 다음 단계에서 제공합니다."
        case .clickSave:
            uiState.isSaving = true
            uiState.infoMessage = "주간 시간표를 저장했습니다."
            uiState.isSaving = false
        case .dismissInfoMessage:
            uiState.infoMessage = nil
        }
    }
}
