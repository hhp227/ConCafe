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
                                conceptRole = "schedule_concept_maid"
                            case "butler":
                                conceptRole = "schedule_concept_butler"
                            case "idol":
                                conceptRole = "schedule_concept_idol"
                            default:
                                conceptRole = event.cast.conceptRole.prefix(1).uppercased() + event.cast.conceptRole.dropFirst()
                            }
                            self.uiState.castSummary = ScheduleUiState.CastSummary(
                                title: event.cast.name,
                                subtitle: "\(conceptRole) / \(cafeName)",
                                badge: self.uiState.castSummary.badge,
                                initials: String(event.cast.name.prefix(2)).uppercased(),
                                profileImageUrl: event.cast.profileImage
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
                                message = "schedule_info_saved_work"
                            case .off:
                                message = "schedule_info_saved_off"
                            default:
                                message = "schedule_info_saved_vacation"
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
        let currentPeriod = uiState.schedulePeriod
        let currentSelectedDayId = uiState.selectedDayId
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
                        conceptRole = "schedule_concept_maid"
                    case "butler":
                        conceptRole = "schedule_concept_butler"
                    case "idol":
                        conceptRole = "schedule_concept_idol"
                    default:
                        conceptRole = data.detail.cast.conceptRole.prefix(1).uppercased() + data.detail.cast.conceptRole.dropFirst()
                    }
                    uiState = ScheduleUiState(
                        managedCastId: data.detail.cast.id,
                        managedCafeId: data.detail.cafe.id,
                        managedCafeName: data.detail.cafe.name,
                        isLoading: false,
                        isSaving: false,
                        errorMessage: nil,
                        castSummary: .init(
                            title: data.detail.cast.name,
                            subtitle: "\(conceptRole) / \(data.detail.cafe.name)",
                            badge: "schedule_badge_cast_member",
                            initials: String(data.detail.cast.name.prefix(2)).uppercased(),
                            profileImageUrl: data.detail.cast.profileImage
                        ),
                        allWeekDays: data.weekDays,
                        allSchedules: data.daySchedules,
                        selectedDayId: currentSelectedDayId.isEmpty ? data.selectedDayId : currentSelectedDayId,
                        infoMessage: nil
                    ).applyingSchedulePeriod(currentPeriod)
                } else {
                    unbindCastEvent()
                    tasks.removeValue(forKey: .scheduleEvent)?.cancel()
                    uiState = ScheduleUiState(
                        isLoading: false,
                        isSaving: false,
                        errorMessage: "schedule_info_load_failed"
                    )
                }
            } catch {
                if Task.isCancelled { return }
                unbindCastEvent()
                tasks.removeValue(forKey: .scheduleEvent)?.cancel()
                uiState = ScheduleUiState(
                    isLoading: false,
                    isSaving: false,
                    errorMessage: "schedule_info_load_failed"
                )
            }
        }
    }

    func onAction(_ action: ScheduleAction) {
        switch action {
        case .clickBack:
            event.send(.navigateBack)
        case .clickMore:
            uiState.infoMessage = "schedule_info_more_next_step"
        case .clickCalendar:
            let cafeId = uiState.managedCafeId
            let cafeName = uiState.managedCafeName
            if cafeId.isEmpty {
                uiState.infoMessage = "schedule_info_calendar_next_step"
            } else {
                event.send(.navigateToCastManagement(cafeId: cafeId, cafeName: cafeName))
            }
        case .selectPeriod(let period):
            uiState = uiState.applyingSchedulePeriod(period)
        case .selectDay(let id):
            uiState.selectedDayId = id
        case .clickEditDay(let id):
            guard let selected = uiState.schedules.first(where: { $0.id == id }) else { return }
            uiState.isEditSheetVisible = true
            uiState.editingScheduleId = selected.id
            uiState.editingScheduleTitle = selected.title
            uiState.editStatus = selected.status
            uiState.editStartTime = selected.timeLabel.components(separatedBy: " - ").first.flatMap { $0.contains(":") ? $0 : nil } ?? ScheduleUiState.defaultStartTime
            uiState.editEndTime = selected.timeLabel.components(separatedBy: " - ").last?.components(separatedBy: " ").first.flatMap { $0.contains(":") ? $0 : nil } ?? ScheduleUiState.defaultEndTime
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
            if uiState.editStatus == .work && ScheduleUiState.computeDurationMinutes(start: uiState.editStartTime, end: uiState.editEndTime) <= 0 {
                uiState.errorMessage = "schedule_error_end_after_start"
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
            uiState.infoMessage = "schedule_info_edit_applied"
            uiState.allSchedules = uiState.allSchedules.map { schedule in
                guard schedule.id == editingId else { return schedule }
                let timeLabel: String
                switch pendingUpdate.status {
                case .work:
                    timeLabel = "\(pendingUpdate.startTime ?? ScheduleUiState.defaultStartTime) - \(pendingUpdate.endTime ?? ScheduleUiState.defaultEndTime)"
                case .off:
                    timeLabel = "schedule_status_off"
                default:
                    timeLabel = "schedule_status_vacation"
                }
                let statusLabel: String
                switch pendingUpdate.status {
                case .work:
                    statusLabel = "schedule_status_work"
                case .off:
                    statusLabel = "schedule_status_off"
                default:
                    statusLabel = "schedule_status_vacation"
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
            uiState.allWeekDays = uiState.allWeekDays.map { day in
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
            uiState = uiState.applyingSchedulePeriod(uiState.schedulePeriod)
        case .clickSave:
            guard !uiState.managedCastId.isEmpty else { return }
            if uiState.pendingUpdates.isEmpty {
                uiState.infoMessage = "schedule_info_no_changes"
                uiState.errorMessage = nil
                return
            }
            uiState.isSaving = true
            uiState.errorMessage = nil
            uiState.infoMessage = nil

            let managedCastId = uiState.managedCastId
            let pendingUpdates = uiState.pendingUpdates
            let requestBatchId = "\(managedCastId)_\(Int(Date().timeIntervalSince1970 * 1000))"
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
                                endTime: pendingUpdate.endTime,
                                requestBatchId: requestBatchId
                            )
                        )
                        if Task.isCancelled { return }
                        if let failure = result as? AppResultFailure {
                            if let validation = failure.error as? AppErrorValidationFailed {
                                switch validation.reason {
                                case "start time is required":
                                    uiState.errorMessage = "schedule_error_start_required"
                                case "end time is required":
                                    uiState.errorMessage = "schedule_error_end_required"
                                case "end time must be after start time":
                                    uiState.errorMessage = "schedule_error_end_after_start"
                                default:
                                    uiState.errorMessage = "schedule_error_save_failed"
                                }
                            } else {
                                uiState.errorMessage = "schedule_error_week_save_failed"
                            }
                            uiState.isSaving = false
                            return
                        }
                    }
                    uiState.isSaving = false
                    uiState.errorMessage = nil
                    uiState.infoMessage = nil
                    uiState.pendingUpdates = []
                    self.event.send(.showMessage("schedule_event_week_saved"))
                } catch {
                    if Task.isCancelled { return }
                    uiState.isSaving = false
                    uiState.errorMessage = "schedule_error_week_save_failed"
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

