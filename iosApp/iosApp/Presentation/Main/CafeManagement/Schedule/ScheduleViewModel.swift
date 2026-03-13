//
//  ScheduleViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/11.
//

import Foundation
import Combine
import Shared

@MainActor
final class ScheduleViewModel: ObservableObject {
    private let castId: String?

    private let getScheduleManagementDataUseCase: GetScheduleManagementDataUseCase

    private let observeCastEventUseCase: ObserveCastEventUseCase

    private let observeCurrentUserUseCase: ObserveCurrentUserUseCase

    @Published private(set) var uiState = ScheduleUiState(isLoading: true)

    let event = PassthroughSubject<ScheduleEvent, Never>()

    private var loadTask: Task<Void, Never>?

    private var watchHandles: [WatchKey: WatchHandle] = [:]

    private func observeSession() {
        watchHandles[.session]?.cancel()
        watchHandles[.session] = observeCurrentUserUseCase.watch { [weak self] _ in
            guard let self else { return }
            Task { @MainActor in
                self.unbindCastEvent()
                self.loadSchedule()
            }
        }
    }

    private func bindCastEvent(_ castId: String) {
        watchHandles[.castEvent]?.cancel()
        watchHandles[.castEvent] = observeCastEventUseCase.watch { [weak self] event in
            guard let self else { return }
            Task { @MainActor in
                switch event {
                case let event as Shared.CastEvent.Created:
                    if event.castId == castId {
                        self.loadSchedule()
                    }
                case let event as Shared.CastEvent.Updated:
                    if event.castId == castId {
                        self.loadSchedule()
                    }
                case let event as Shared.CastEvent.Deleted:
                    if event.castId == castId {
                        self.loadSchedule()
                    }
                default:
                    break
                }
            }
        }
    }

    private func unbindCastEvent() {
        watchHandles.removeValue(forKey: .castEvent)?.cancel()
    }

    private func loadSchedule() {
        loadTask?.cancel()
        uiState.isLoading = true
        uiState.errorMessage = nil
        uiState.infoMessage = nil

        loadTask = Task {
            do {
                let result = try await getScheduleManagementDataUseCase.invoke(castId: castId)

                if let success = result as? AppResultSuccess<AnyObject>,
                   let data = success.data as? Shared.ScheduleManagementData {
                    bindCastEvent(data.detail.cast.id)
                    uiState = data.toUiState()
                } else {
                    unbindCastEvent()
                    uiState = ScheduleUiState(
                        isLoading: false,
                        isSaving: false,
                        errorMessage: "출근표 데이터를 불러오지 못했습니다."
                    )
                }
            } catch {
                if Task.isCancelled { return }
                unbindCastEvent()
                uiState = ScheduleUiState(
                    isLoading: false,
                    isSaving: false,
                    errorMessage: "출근표 데이터를 불러오지 못했습니다."
                )
            }
        }
    }

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
            uiState.errorMessage = nil
        }
    }

    init(
        castId: String? = nil,
        getScheduleManagementDataUseCase: GetScheduleManagementDataUseCase = KoinInitializerKt.resolveGetScheduleManagementDataUseCase(),
        observeCastEventUseCase: ObserveCastEventUseCase = KoinInitializerKt.resolveObserveCastEventUseCase(),
        observeCurrentUserUseCase: ObserveCurrentUserUseCase = KoinInitializerKt.resolveObserveCurrentUserUseCase()
    ) {
        self.castId = castId
        self.getScheduleManagementDataUseCase = getScheduleManagementDataUseCase
        self.observeCastEventUseCase = observeCastEventUseCase
        self.observeCurrentUserUseCase = observeCurrentUserUseCase

        observeSession()
    }

    deinit {
        loadTask?.cancel()
        watchHandles.values.forEach { $0.cancel() }
        watchHandles.removeAll()
    }

    private enum WatchKey {
        case session
        case castEvent
    }
}

private extension Shared.ScheduleManagementData {
    func toUiState() -> ScheduleUiState {
        ScheduleUiState(
            isLoading: false,
            isSaving: false,
            errorMessage: nil,
            castSummary: .init(
                title: detail.cast.name,
                subtitle: "\(detail.cast.conceptRole.toDisplayConceptRole()) / \(detail.cafe.name)",
                badge: "Cast Member",
                initials: detail.cast.name.toInitials()
            ),
            weekRangeLabel: weekRangeLabel,
            weekDays: weekDays.map { day in
                ScheduleUiState.WeekDay(
                    id: day.id,
                    label: day.label,
                    number: day.number,
                    isSelected: day.id == selectedDayId,
                    isWorking: day.isWorking
                )
            },
            schedules: daySchedules.map { schedule in
                ScheduleUiState.DaySchedule(
                    id: schedule.id,
                    title: schedule.title,
                    timeLabel: schedule.timeLabel,
                    statusLabel: schedule.statusLabel,
                    isWorking: schedule.isWorking
                )
            },
            selectedDayId: selectedDayId,
            infoMessage: nil
        )
    }
}

private extension String {
    func toDisplayConceptRole() -> String {
        switch lowercased() {
        case "maid":
            return "메이드"
        case "butler":
            return "버틀러"
        case "idol":
            return "아이돌"
        default:
            return prefix(1).uppercased() + dropFirst()
        }
    }

    func toInitials() -> String {
        String(prefix(2)).uppercased()
    }
}
