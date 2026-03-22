//
//  ScheduleViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/11.
//

import Foundation
import Combine
import Shared
import KMPNativeCoroutinesAsync

@MainActor
final class ScheduleViewModel: ObservableObject {
    private let castId: String?

    private let getScheduleManagementDataUseCase: GetScheduleManagementDataUseCase

    private let observeCurrentUserUseCase: ObserveCurrentUserUseCase

    private let updateCastScheduleUseCase: UpdateCastScheduleUseCase

    private let castEventPublisher: CastEventPublisher

    private let scheduleManagementEventPublisher: ScheduleManagementEventPublisher

    @Published private(set) var uiState = ScheduleUiState(isLoading: true)

    let event = PassthroughSubject<ScheduleEvent, Never>()

    private var tasks: [TaskKey: Task<Void, Never>] = [:]

    private func observeSession() {
        tasks[.session]?.cancel()
        tasks[.session] = Task {
            do {
                for try await _ in asyncSequence(for: observeCurrentUserUseCase.invoke()) {
                    self.unbindCastEvent()
                    self.loadSchedule()
                }
            } catch {
                print("Error: \(error)")
            }
        }
    }

    private func bindCastEvent(_ castId: String) {
        tasks[.castEvent]?.cancel()
        tasks[.castEvent] = Task {
            do {
                for try await event in asyncSequence(for: castEventPublisher.events) {
                    switch event {
                    case let event as Shared.CastEvent.Created:
                        if event.cast.id == castId {
                            self.loadSchedule()
                        }
                    case let event as Shared.CastEvent.Updated:
                        if event.cast.id == castId {
                            let cafeName = self.uiState.castSummary.subtitle.components(separatedBy: " / ").last ?? ""
                            let conceptRole: String
                            switch event.cast.conceptRole.lowercased() {
                            case "maid":
                                conceptRole = "메이드"
                            case "butler":
                                conceptRole = "버틀러"
                            case "idol":
                                conceptRole = "아이돌"
                            default:
                                conceptRole = event.cast.conceptRole.prefix(1).uppercased() + event.cast.conceptRole.dropFirst()
                            }
                            self.uiState.castSummary = ScheduleUiState.CastSummary(
                                title: event.cast.name,
                                subtitle: "\(conceptRole) / \(cafeName)",
                                badge: self.uiState.castSummary.badge,
                                initials: String(event.cast.name.prefix(2)).uppercased()
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
            } catch {
                print("Error: \(error)")
            }
        }
    }

    private func bindScheduleManagementEvent(_ castId: String) {
        tasks[.scheduleEvent]?.cancel()
        tasks[.scheduleEvent] = Task {
            do {
                for try await event in asyncSequence(for: scheduleManagementEventPublisher.events) {
                    if self.uiState.isSaving {
                        continue
                    }
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
            } catch {
                print("Error: \(error)")
            }
        }
    }

    private func unbindCastEvent() {
        tasks.removeValue(forKey: .castEvent)?.cancel()
        tasks.removeValue(forKey: .scheduleEvent)?.cancel()
    }

    private func loadSchedule(showLoading: Bool = true) {
        tasks[.load]?.cancel()
        uiState.isLoading = showLoading
        uiState.isSaving = false
        uiState.errorMessage = nil
        uiState.infoMessage = nil
        uiState.pendingUpdates = []

        tasks[.load] = Task {
            do {
                let result = try await getScheduleManagementDataUseCase.invoke(castId: castId)

                if let success = result as? AppResultSuccess<AnyObject>,
                   let data = success.data as? Shared.ScheduleManagementData {
                    bindCastEvent(data.detail.cast.id)
                    bindScheduleManagementEvent(data.detail.cast.id)
                    let conceptRole: String
                    switch data.detail.cast.conceptRole.lowercased() {
                    case "maid":
                        conceptRole = "메이드"
                    case "butler":
                        conceptRole = "버틀러"
                    case "idol":
                        conceptRole = "아이돌"
                    default:
                        conceptRole = data.detail.cast.conceptRole.prefix(1).uppercased() + data.detail.cast.conceptRole.dropFirst()
                    }
                    uiState = ScheduleUiState(
                        managedCastId: data.detail.cast.id,
                        isLoading: false,
                        isSaving: false,
                        errorMessage: nil,
                        castSummary: .init(
                            title: data.detail.cast.name,
                            subtitle: "\(conceptRole) / \(data.detail.cafe.name)",
                            badge: "Cast Member",
                            initials: String(data.detail.cast.name.prefix(2)).uppercased()
                        ),
                        weekRangeLabel: data.weekRangeLabel,
                        weekDays: data.weekDays,
                        schedules: data.daySchedules,
                        selectedDayId: data.selectedDayId,
                        infoMessage: nil
                    )
                } else {
                    unbindCastEvent()
                    tasks.removeValue(forKey: .scheduleEvent)?.cancel()
                    uiState = ScheduleUiState(
                        isLoading: false,
                        isSaving: false,
                        errorMessage: "출근표 데이터를 불러오지 못했습니다."
                    )
                }
            } catch {
                if Task.isCancelled { return }
                unbindCastEvent()
                tasks.removeValue(forKey: .scheduleEvent)?.cancel()
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
            if uiState.editStatus == .work && uiState.editStartTime >= uiState.editEndTime {
                uiState.errorMessage = "종료 시간은 시작 시간보다 늦어야 합니다."
                return
            }
            let pendingUpdate = ScheduleUiState.PendingScheduleUpdate(
                date: editingId,
                status: uiState.editStatus,
                startTime: uiState.editStatus == .work ? uiState.editStartTime : nil,
                endTime: uiState.editStatus == .work ? uiState.editEndTime : nil
            )
            uiState.isEditSheetVisible = false
            uiState.editingScheduleId = nil
            uiState.errorMessage = nil
            uiState.infoMessage = "편집 내용을 화면에 반영했습니다. 하단 버튼으로 실제 저장을 완료하세요."
            uiState.schedules = uiState.schedules.map { schedule in
                guard schedule.id == editingId else { return schedule }
                let timeLabel: String
                switch pendingUpdate.status {
                case .work:
                    timeLabel = "\(pendingUpdate.startTime ?? "10:00") - \(pendingUpdate.endTime ?? "19:00")"
                case .off:
                    timeLabel = "휴무"
                default:
                    timeLabel = "휴가"
                }
                let statusLabel: String
                switch pendingUpdate.status {
                case .work:
                    statusLabel = "근무"
                case .off:
                    statusLabel = "휴무"
                default:
                    statusLabel = "휴가"
                }
                return ScheduleManagementDaySchedule(
                    id: pendingUpdate.date,
                    title: schedule.title,
                    timeLabel: timeLabel,
                    statusLabel: statusLabel,
                    isWorking: pendingUpdate.status == .work,
                    status: pendingUpdate.status
                )
            }
            uiState.weekDays = uiState.weekDays.map { day in
                guard day.id == editingId else { return day }
                return ScheduleManagementWeekDay(
                    id: day.id,
                    label: day.label,
                    number: day.number,
                    isWorking: pendingUpdate.status == .work
                )
            }
            uiState.pendingUpdates.removeAll { $0.date == editingId }
            uiState.pendingUpdates.append(pendingUpdate)
        case .clickSave:
            guard !uiState.managedCastId.isEmpty else { return }
            if uiState.pendingUpdates.isEmpty {
                uiState.infoMessage = "저장할 변경사항이 없습니다."
                uiState.errorMessage = nil
                return
            }
            uiState.isSaving = true
            uiState.errorMessage = nil
            uiState.infoMessage = nil

            let managedCastId = uiState.managedCastId
            let pendingUpdates = uiState.pendingUpdates
            tasks[.submit]?.cancel()
            tasks[.submit] = Task { [weak self] in
                guard let self else { return }
                do {
                    for pendingUpdate in pendingUpdates {
                        let result = try await updateCastScheduleUseCase.invoke(
                            input: CastScheduleUpdate(
                                castId: managedCastId,
                                date: pendingUpdate.date,
                                status: pendingUpdate.status,
                                startTime: pendingUpdate.startTime,
                                endTime: pendingUpdate.endTime
                            )
                        )
                        if Task.isCancelled { return }
                        if let failure = result as? AppResultFailure {
                            if let validation = failure.error as? AppErrorValidationFailed {
                                switch validation.reason {
                                case "start time is required":
                                    uiState.errorMessage = "시작 시간을 선택해주세요."
                                case "end time is required":
                                    uiState.errorMessage = "종료 시간을 선택해주세요."
                                case "end time must be after start time":
                                    uiState.errorMessage = "종료 시간은 시작 시간보다 늦어야 합니다."
                                default:
                                    uiState.errorMessage = "근무 시간 저장에 실패했습니다."
                                }
                            } else {
                                uiState.errorMessage = "주간 시간표 저장에 실패했습니다."
                            }
                            uiState.isSaving = false
                            return
                        }
                    }
                    self.loadSchedule(showLoading: false)
                    self.event.send(.showMessage("주간 시간표를 저장했습니다."))
                } catch {
                    if Task.isCancelled { return }
                    uiState.isSaving = false
                    uiState.errorMessage = "주간 시간표 저장에 실패했습니다."
                }
            }
        case .dismissInfoMessage:
            uiState.infoMessage = nil
            uiState.errorMessage = nil
        }
    }

    init(
        castId: String? = nil,
        getScheduleManagementDataUseCase: GetScheduleManagementDataUseCase = KoinInitializerKt.resolveGetScheduleManagementDataUseCase(),
        observeCurrentUserUseCase: ObserveCurrentUserUseCase = KoinInitializerKt.resolveObserveCurrentUserUseCase(),
        updateCastScheduleUseCase: UpdateCastScheduleUseCase = KoinInitializerKt.resolveUpdateCastScheduleUseCase(),
        castEventPublisher: CastEventPublisher = KoinInitializerKt.resolveCastEventPublisher(),
        scheduleManagementEventPublisher: ScheduleManagementEventPublisher = KoinInitializerKt.resolveScheduleManagementEventPublisher()
    ) {
        self.castId = castId
        self.getScheduleManagementDataUseCase = getScheduleManagementDataUseCase
        self.observeCurrentUserUseCase = observeCurrentUserUseCase
        self.updateCastScheduleUseCase = updateCastScheduleUseCase
        self.castEventPublisher = castEventPublisher
        self.scheduleManagementEventPublisher = scheduleManagementEventPublisher

        observeSession()
    }

    deinit {
        tasks.values.forEach { $0.cancel() }
        tasks.removeAll()
    }

    private enum TaskKey {
        case session
        case castEvent
        case scheduleEvent
        case load
        case submit
    }
}
