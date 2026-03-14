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

    private let observeScheduleManagementEventUseCase: ObserveScheduleManagementEventUseCase

    private let observeCurrentUserUseCase: ObserveCurrentUserUseCase

    private let updateCastScheduleUseCase: UpdateCastScheduleUseCase

    @Published private(set) var uiState = ScheduleUiState(isLoading: true)

    let event = PassthroughSubject<ScheduleEvent, Never>()

    private var tasks: [TaskKey: Task<Void, Never>] = [:]

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
                    if event.cast.id == castId {
                        self.loadSchedule()
                    }
                case let event as Shared.CastEvent.Updated:
                    if event.cast.id == castId {
                        let cafeName = self.uiState.castSummary.subtitle.components(separatedBy: " / ").last ?? ""
                        self.uiState.castSummary = ScheduleUiState.CastSummary(
                            title: event.cast.name,
                            subtitle: "\(event.cast.conceptRole.toDisplayConceptRole()) / \(cafeName)",
                            badge: self.uiState.castSummary.badge,
                            initials: event.cast.name.toInitials()
                        )
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

    private func bindScheduleManagementEvent(_ castId: String) {
        watchHandles[.scheduleEvent]?.cancel()
        watchHandles[.scheduleEvent] = observeScheduleManagementEventUseCase.watch { [weak self] event in
            guard let self else { return }
            Task { @MainActor in
                switch event {
                case let event as Shared.ScheduleManagementEvent.Updated:
                    if event.castId == castId {
                        self.loadSchedule(showLoading: false)
                        let message: String
                        switch event.status {
                        case .work:
                            message = "근무 시간이 저장되었습니다."
                        case .off:
                            message = "휴무로 변경되었습니다."
                        default:
                            message = "휴가 일정으로 변경되었습니다."
                        }
                        self.event.send(.showMessage(message))
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

    private func loadSchedule(showLoading: Bool = true) {
        tasks[.load]?.cancel()
        uiState.isLoading = showLoading
        uiState.isSaving = false
        uiState.errorMessage = nil
        uiState.infoMessage = nil

        tasks[.load] = Task {
            do {
                let result = try await getScheduleManagementDataUseCase.invoke(castId: castId)

                if let success = result as? AppResultSuccess<AnyObject>,
                   let data = success.data as? Shared.ScheduleManagementData {
                    bindCastEvent(data.detail.cast.id)
                    bindScheduleManagementEvent(data.detail.cast.id)
                    uiState = data.toUiState()
                } else {
                    unbindCastEvent()
                    watchHandles.removeValue(forKey: .scheduleEvent)?.cancel()
                    uiState = ScheduleUiState(
                        isLoading: false,
                        isSaving: false,
                        errorMessage: "출근표 데이터를 불러오지 못했습니다."
                    )
                }
            } catch {
                if Task.isCancelled { return }
                unbindCastEvent()
                watchHandles.removeValue(forKey: .scheduleEvent)?.cancel()
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
            uiState.isEditSheetVisible = true
            uiState.editingScheduleId = selected.id
            uiState.editingScheduleTitle = selected.title
            uiState.editStatus = selected.status
            uiState.editStartTime = selected.timeLabel.components(separatedBy: " - ").first.flatMap { $0.contains(":") ? $0 : nil } ?? "10:00"
            uiState.editEndTime = selected.timeLabel.components(separatedBy: " - ").last.flatMap { $0.contains(":") ? $0 : nil } ?? "19:00"
            uiState.infoMessage = nil
        case .dismissEditSheet:
            uiState.isEditSheetVisible = false
            uiState.editingScheduleId = nil
        case .changeEditStatus(let status):
            uiState.editStatus = status
        case .changeEditStartTime(let value):
            uiState.editStartTime = value
        case .changeEditEndTime(let value):
            uiState.editEndTime = value
        case .submitEditDay:
            guard let editingId = uiState.editingScheduleId else { return }
            guard !uiState.managedCastId.isEmpty else { return }
            if uiState.editStatus == .work && uiState.editStartTime >= uiState.editEndTime {
                uiState.errorMessage = "종료 시간은 시작 시간보다 늦어야 합니다."
                return
            }
            uiState.isEditSheetVisible = false
            uiState.editingScheduleId = nil
            uiState.isSaving = true
            uiState.errorMessage = nil
            uiState.infoMessage = nil

            let managedCastId = uiState.managedCastId
            let editStatus = uiState.editStatus
            let editStartTime = uiState.editStartTime
            let editEndTime = uiState.editEndTime

            tasks[.submit]?.cancel()
            tasks[.submit] = Task { [weak self] in
                guard let self else { return }
                do {
                    let result = try await updateCastScheduleUseCase.invoke(
                        input: CastScheduleUpdate(
                            castId: managedCastId,
                            date: editingId,
                            status: editStatus.toDomainStatus(),
                            startTime: editStatus == .work ? editStartTime : nil,
                            endTime: editStatus == .work ? editEndTime : nil
                        )
                    )
                    if Task.isCancelled { return }
                    if let failure = result as? AppResultFailure {
                        if let validation = failure.error as? AppErrorValidationFailed {
                            uiState.errorMessage = validation.reason.toScheduleValidationMessage()
                        } else {
                            uiState.errorMessage = "근무 시간 저장에 실패했습니다."
                        }
                        uiState.isSaving = false
                    }
                } catch {
                    if Task.isCancelled { return }
                    uiState.isSaving = false
                    uiState.errorMessage = "근무 시간 저장에 실패했습니다."
                }
            }
        case .clickSave:
            uiState.infoMessage = "일자별 수정 시 즉시 저장됩니다."
        case .dismissInfoMessage:
            uiState.infoMessage = nil
            uiState.errorMessage = nil
        }
    }

    init(
        castId: String? = nil,
        getScheduleManagementDataUseCase: GetScheduleManagementDataUseCase = KoinInitializerKt.resolveGetScheduleManagementDataUseCase(),
        observeCastEventUseCase: ObserveCastEventUseCase = KoinInitializerKt.resolveObserveCastEventUseCase(),
        observeScheduleManagementEventUseCase: ObserveScheduleManagementEventUseCase = KoinInitializerKt.resolveObserveScheduleManagementEventUseCase(),
        observeCurrentUserUseCase: ObserveCurrentUserUseCase = KoinInitializerKt.resolveObserveCurrentUserUseCase(),
        updateCastScheduleUseCase: UpdateCastScheduleUseCase = KoinInitializerKt.resolveUpdateCastScheduleUseCase()
    ) {
        self.castId = castId
        self.getScheduleManagementDataUseCase = getScheduleManagementDataUseCase
        self.observeCastEventUseCase = observeCastEventUseCase
        self.observeScheduleManagementEventUseCase = observeScheduleManagementEventUseCase
        self.observeCurrentUserUseCase = observeCurrentUserUseCase
        self.updateCastScheduleUseCase = updateCastScheduleUseCase

        observeSession()
    }

    deinit {
        tasks.values.forEach { $0.cancel() }
        tasks.removeAll()
        watchHandles.values.forEach { $0.cancel() }
        watchHandles.removeAll()
    }

    private enum TaskKey {
        case load
        case submit
    }

    private enum WatchKey {
        case session
        case castEvent
        case scheduleEvent
    }
}

private extension Shared.ScheduleManagementData {
    func toUiState() -> ScheduleUiState {
        ScheduleUiState(
            managedCastId: detail.cast.id,
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
                    isWorking: schedule.isWorking,
                    status: schedule.status.toUiStatus()
                )
            },
            selectedDayId: selectedDayId,
            infoMessage: nil
        )
    }
}

private extension String {
    func toScheduleValidationMessage() -> String {
        switch self {
        case "start time is required":
            return "시작 시간을 선택해주세요."
        case "end time is required":
            return "종료 시간을 선택해주세요."
        case "end time must be after start time":
            return "종료 시간은 시작 시간보다 늦어야 합니다."
        default:
            return "근무 시간 저장에 실패했습니다."
        }
    }

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

private extension ScheduleEditStatus {
    func toDomainStatus() -> CastScheduleStatus {
        switch self {
        case .work:
            return .work
        case .off:
            return .off
        case .vacation:
            return .vacation
        }
    }
}

private extension CastScheduleStatus {
    func toUiStatus() -> ScheduleEditStatus {
        switch self {
        case .work:
            return .work
        case .off:
            return .off
        default:
            return .vacation
        }
    }
}
