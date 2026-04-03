//
//  NoticeEventViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/11.
//

import Foundation
import Combine
import Shared
import KMPNativeCoroutinesAsync

@MainActor
final class NoticeEventViewModel: ObservableObject {
    private let cafeId: String

    private let getCafeNoticePageUseCase: GetCafeNoticePageUseCase

    private let getCafeEventPageUseCase: GetCafeEventPageUseCase

    private let createCafeNoticeUseCase: CreateCafeNoticeUseCase

    private let createCafeEventUseCase: CreateCafeEventUseCase

    private let updateCafeNoticeUseCase: UpdateCafeNoticeUseCase

    private let updateCafeEventUseCase: UpdateCafeEventUseCase

    private let deleteCafeNoticeUseCase: DeleteCafeNoticeUseCase

    private let deleteCafeEventUseCase: DeleteCafeEventUseCase

    private let noticeManagementEventPublisher: NoticeManagementEventPublisher

    private let uploadImageUseCase: UploadImageUseCase

    @Published private(set) var uiState = NoticeEventUiState()

    let event = PassthroughSubject<NoticeEventEvent, Never>()

    private var tasks: [TaskKey: Task<Void, Never>] = [:]

    private func openCreateForm() {
        uiState.isFormSheetVisible = true
        uiState.isSubmittingForm = false
        uiState.formEditingId = nil
        uiState.formTitle = ""
        uiState.formContent = ""
        uiState.formImageUrl = ""
        uiState.formPinned = false
        uiState.formReservedAt = ""
        uiState.infoMessage = nil
    }
    
    private func openEditNoticeForm(_ id: String) {
        guard let target = uiState.notices.first(where: { $0.id == id }) else {
            uiState.infoMessage = MessageKey.noticeEditTargetNotFound
            return
        }

        uiState.selectedTab = .notice
        uiState.isFormSheetVisible = true
        uiState.isSubmittingForm = false
        uiState.formEditingId = target.id
        uiState.formTitle = target.title
        uiState.formContent = target.content
        uiState.formImageUrl = ""
        uiState.formPinned = target.isPinned
        uiState.formReservedAt = target.statusAccent == .draft ? target.displayDate : ""
        uiState.infoMessage = nil
    }

    private func openEditEventForm(_ id: String) {
        guard let target = uiState.events.first(where: { $0.id == id }) else {
            uiState.infoMessage = MessageKey.eventEditTargetNotFound
            return
        }

        uiState.selectedTab = .event
        uiState.isFormSheetVisible = true
        uiState.isSubmittingForm = false
        uiState.formEditingId = target.id
        uiState.formTitle = target.title
        uiState.formContent = target.content
        uiState.formImageUrl = target.imageUrl
        uiState.formPinned = false
        uiState.formReservedAt = target.periodText
        uiState.infoMessage = nil
    }
    
    private func loadNoticePage(cursor: String?, append: Bool) {
        tasks[.noticePage]?.cancel()
        uiState.isLoadingNotices = !append
        uiState.isLoadingMoreNotices = append
        if !append {
            uiState.infoMessage = nil
        }

        tasks[.noticePage] = Task { [weak self] in
            guard let self else { return }

            do {
                let result = try await getCafeNoticePageUseCase.invoke(
                    cafeId: cafeId,
                    query: uiState.query,
                    cursor: cursor,
                    pageSize: 15
                )

                if Task.isCancelled { return }

                if let success = result as? AppResultSuccess<AnyObject>,
                   let page = success.data as? PagedResult<CafeNoticeManagementItem> {
                    let items = page.items as! [CafeNoticeManagementItem]
                    uiState.notices = append ? (uiState.notices + items) : items
                    uiState.noticeNextCursor = page.nextCursor
                    uiState.canLoadMoreNotices = page.hasNext
                    uiState.isLoadingNotices = false
                    uiState.isLoadingMoreNotices = false
                } else {
                    uiState.isLoadingNotices = false
                    uiState.isLoadingMoreNotices = false
                    uiState.infoMessage = MessageKey.noticeLoadFailed
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isLoadingNotices = false
                uiState.isLoadingMoreNotices = false
                uiState.infoMessage = MessageKey.noticeLoadFailed
            }
        }
    }

    private func loadEventPage(cursor: String?, append: Bool) {
        tasks[.eventPage]?.cancel()
        uiState.isLoadingEvents = !append
        uiState.isLoadingMoreEvents = append
        if !append {
            uiState.infoMessage = nil
        }

        tasks[.eventPage] = Task { [weak self] in
            guard let self else { return }

            do {
                let result = try await getCafeEventPageUseCase.invoke(
                    cafeId: cafeId,
                    query: uiState.query,
                    cursor: cursor,
                    pageSize: 15
                )

                if Task.isCancelled { return }

                if let success = result as? AppResultSuccess<AnyObject>,
                   let page = success.data as? PagedResult<CafeEventManagementItem> {
                    let items = page.items as! [CafeEventManagementItem]
                    uiState.events = append ? (uiState.events + items) : items
                    uiState.eventNextCursor = page.nextCursor
                    uiState.canLoadMoreEvents = page.hasNext
                    uiState.isLoadingEvents = false
                    uiState.isLoadingMoreEvents = false
                } else {
                    uiState.isLoadingEvents = false
                    uiState.isLoadingMoreEvents = false
                    uiState.infoMessage = MessageKey.eventLoadFailed
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isLoadingEvents = false
                uiState.isLoadingMoreEvents = false
                uiState.infoMessage = MessageKey.eventLoadFailed
            }
        }
    }

    private func refreshCurrentTab() {
        switch uiState.selectedTab {
        case .notice:
            loadNoticePage(cursor: nil, append: false)
        case .event:
            loadEventPage(cursor: nil, append: false)
        }
    }

    private func loadMoreNotices() {
        guard !uiState.isLoadingNotices,
              !uiState.isLoadingMoreNotices,
              uiState.canLoadMoreNotices,
              let cursor = uiState.noticeNextCursor else { return }
        loadNoticePage(cursor: cursor, append: true)
    }

    private func loadMoreEvents() {
        guard !uiState.isLoadingEvents,
              !uiState.isLoadingMoreEvents,
              uiState.canLoadMoreEvents,
              let cursor = uiState.eventNextCursor else { return }
        loadEventPage(cursor: cursor, append: true)
    }

    private func submitForm() {
        guard uiState.isFormSubmitEnabled else { return }
        tasks[.submit]?.cancel()
        tasks[.submit] = Task { [weak self] in
            guard let self else { return }

            let isEditing = uiState.formEditingId != nil
            let selectedTab = uiState.selectedTab
            uiState.isSubmittingForm = true
            uiState.infoMessage = nil

            do {
                let result: AppResult
                if selectedTab == .notice {
                    if let editingId = uiState.formEditingId {
                        result = try await updateCafeNoticeUseCase.invoke(
                            input: CafeNoticeUpdate(
                                cafeId: cafeId,
                                noticeId: editingId,
                                title: uiState.formTitle,
                                content: uiState.formContent,
                                isPinned: uiState.formPinned,
                                reservedAt: uiState.formReservedAt.isEmpty ? nil : uiState.formReservedAt
                            )
                        )
                    } else {
                        result = try await createCafeNoticeUseCase.invoke(
                            input: CafeNoticeCreate(
                                cafeId: cafeId,
                                title: uiState.formTitle,
                                content: uiState.formContent,
                                isPinned: uiState.formPinned,
                                reservedAt: uiState.formReservedAt.isEmpty ? nil : uiState.formReservedAt
                            )
                        )
                    }
                } else {
                    let uploadedImageUrl = try await uploadEventImageIfNeeded(uiState.formImageUrl)
                    if let editingId = uiState.formEditingId {
                        result = try await updateCafeEventUseCase.invoke(
                            input: CafeEventUpdate(
                                cafeId: cafeId,
                                eventId: editingId,
                                title: uiState.formTitle,
                                content: uiState.formContent,
                                imageUrl: uploadedImageUrl,
                                periodText: uiState.formReservedAt.isEmpty ? nil : uiState.formReservedAt
                            )
                        )
                    } else {
                        result = try await createCafeEventUseCase.invoke(
                            input: CafeEventCreate(
                                cafeId: cafeId,
                                title: uiState.formTitle,
                                content: uiState.formContent,
                                imageUrl: uploadedImageUrl,
                                periodText: uiState.formReservedAt.isEmpty ? nil : uiState.formReservedAt
                            )
                        )
                    }
                }

                if Task.isCancelled { return }

                if let failure = result as? AppResultFailure {
                    if let validation = failure.error as? AppErrorValidationFailed {
                        switch validation.reason {
                        case "cafeId is required":
                            uiState.infoMessage = "noticeevent_validation_cafe_required"
                        case "noticeId is required":
                            uiState.infoMessage = "noticeevent_validation_notice_required"
                        case "eventId is required":
                            uiState.infoMessage = "noticeevent_validation_event_required"
                        case "notice title is required", "event title is required":
                            uiState.infoMessage = "noticeevent_validation_title_required"
                        case "notice content is required", "event content is required":
                            uiState.infoMessage = "noticeevent_validation_content_required"
                        case "event image is required":
                            uiState.infoMessage = "noticeevent_validation_event_image_required"
                        default:
                            uiState.infoMessage = validation.reason
                        }
                    } else if selectedTab == .notice {
                        uiState.infoMessage = isEditing ? MessageKey.noticeUpdateFailed : MessageKey.noticeCreateFailed
                    } else {
                        uiState.infoMessage = isEditing ? MessageKey.eventUpdateFailed : MessageKey.eventCreateFailed
                    }
                    uiState.isSubmittingForm = false
                    return
                }

                uiState.isFormSheetVisible = false
                uiState.isSubmittingForm = false
                uiState.formEditingId = nil
                uiState.formTitle = ""
                uiState.formContent = ""
                uiState.formImageUrl = ""
                uiState.formPinned = false
                uiState.formReservedAt = ""
                if selectedTab == .notice {
                    uiState.infoMessage = isEditing ? MessageKey.noticeUpdated : MessageKey.noticeCreated
                } else {
                    uiState.infoMessage = isEditing ? MessageKey.eventUpdated : MessageKey.eventCreated
                }
                refreshCurrentTab()
            } catch {
                if Task.isCancelled { return }
                uiState.isSubmittingForm = false
                if selectedTab == .notice {
                    uiState.infoMessage = isEditing ? MessageKey.noticeUpdateFailed : MessageKey.noticeCreateFailed
                } else {
                    uiState.infoMessage = isEditing ? MessageKey.eventUpdateFailed : MessageKey.eventCreateFailed
                }
            }
        }
    }

    private func deleteNotice(_ id: String) {
        tasks[.delete]?.cancel()
        tasks[.delete] = Task { [weak self] in
            guard let self else { return }

            do {
                let result = try await deleteCafeNoticeUseCase.invoke(cafeId: cafeId, noticeId: id)
                if Task.isCancelled { return }
                uiState.infoMessage = result is AppResultSuccess<AnyObject> ? MessageKey.noticeDeleteSuccess : MessageKey.noticeDeleteFailed
            } catch {
                if Task.isCancelled { return }
                uiState.infoMessage = MessageKey.noticeDeleteFailed
            }
        }
    }

    private func deleteEvent(_ id: String) {
        tasks[.delete]?.cancel()
        tasks[.delete] = Task { [weak self] in
            guard let self else { return }

            do {
                let result = try await deleteCafeEventUseCase.invoke(cafeId: cafeId, eventId: id)
                if Task.isCancelled { return }
                uiState.infoMessage = result is AppResultSuccess<AnyObject> ? MessageKey.eventDeleteSuccess : MessageKey.eventDeleteFailed
            } catch {
                if Task.isCancelled { return }
                uiState.infoMessage = MessageKey.eventDeleteFailed
            }
        }
    }

    private func patchNotice(_ item: CafeNoticeManagementItem) {
        uiState.notices = uiState.notices.map { $0.id == item.id ? item : $0 }
    }

    private func patchEvent(_ item: CafeEventManagementItem) {
        uiState.events = uiState.events.map { $0.id == item.id ? item : $0 }
    }

    private func removeNotice(_ id: String) {
        uiState.notices.removeAll { $0.id == id }
    }

    private func removeEvent(_ id: String) {
        uiState.events.removeAll { $0.id == id }
    }

    private func observeNoticeManagementEvent() {
        tasks[.noticeManagementEvent]?.cancel()
        tasks[.noticeManagementEvent] = Task {
            do {
                for try await event in asyncSequence(for: noticeManagementEventPublisher.events) {
                    if let event = event as? NoticeManagementEvent.NoticeCreated {
                        if event.cafeId == self.cafeId {
                            self.loadNoticePage(cursor: nil, append: false)
                        }
                    } else if let event = event as? NoticeManagementEvent.NoticeUpdated {
                        if event.cafeId == self.cafeId {
                            self.patchNotice(event.notice)
                        }
                    } else if let event = event as? NoticeManagementEvent.NoticeDeleted {
                        if event.cafeId == self.cafeId {
                            self.removeNotice(event.noticeId)
                        }
                    } else if let event = event as? NoticeManagementEvent.EventCreated {
                        if event.cafeId == self.cafeId {
                            self.loadEventPage(cursor: nil, append: false)
                        }
                    } else if let event = event as? NoticeManagementEvent.EventUpdated {
                        if event.cafeId == self.cafeId {
                            self.patchEvent(event.event)
                        }
                    } else if let event = event as? NoticeManagementEvent.EventDeleted {
                        if event.cafeId == self.cafeId {
                            self.removeEvent(event.eventId)
                        }
                    }
                }
            } catch {
                print("Error: \(error)")
            }
        }
    }

    func onAction(_ action: NoticeEventAction) {
        switch action {
        case .clickBack:
            event.send(.navigateBack)
        case .selectTab(let tab):
            uiState.selectedTab = tab
            if tab == .notice && uiState.notices.isEmpty {
                loadNoticePage(cursor: nil, append: false)
            } else if tab == .event && uiState.events.isEmpty {
                loadEventPage(cursor: nil, append: false)
            }
        case .changeQuery(let value):
            uiState.query = value
            refreshCurrentTab()
        case .loadMoreNotices:
            loadMoreNotices()
        case .loadMoreEvents:
            loadMoreEvents()
        case .clickRegister:
            openCreateForm()
        case .clickMoreEvents:
            uiState.infoMessage = MessageKey.moreEventsNextStep
        case .clickEditNotice(let id):
            openEditNoticeForm(id)
        case .clickDeleteNotice(let id):
            deleteNotice(id)
        case .clickEditEvent(let id):
            openEditEventForm(id)
        case .clickDeleteEvent(let id):
            deleteEvent(id)
        case .dismissFormSheet:
            uiState.isFormSheetVisible = false
            uiState.formEditingId = nil
        case .changeFormTitle(let value):
            uiState.formTitle = value
        case .changeFormContent(let value):
            uiState.formContent = value
        case .changeFormImage(let value):
            uiState.formImageUrl = value
            uiState.infoMessage = nil
        case .clickFormImage:
            uiState.infoMessage = MessageKey.imagePickRequired
        case .clickRemoveFormImage:
            uiState.formImageUrl = ""
            uiState.infoMessage = nil
        case .changeFormPinned(let value):
            uiState.formPinned = value
        case .clickReserveSchedule:
            uiState.infoMessage = MessageKey.reserveScheduleNextStep
        case .clickSubmitForm:
            submitForm()
        case .dismissInfoMessage:
            uiState.infoMessage = nil
        }
    }

    init(
        cafeId: String,
        getCafeNoticePageUseCase: GetCafeNoticePageUseCase = KoinInitializerKt.resolveGetCafeNoticePageUseCase(),
        getCafeEventPageUseCase: GetCafeEventPageUseCase = KoinInitializerKt.resolveGetCafeEventPageUseCase(),
        createCafeNoticeUseCase: CreateCafeNoticeUseCase = KoinInitializerKt.resolveCreateCafeNoticeUseCase(),
        createCafeEventUseCase: CreateCafeEventUseCase = KoinInitializerKt.resolveCreateCafeEventUseCase(),
        updateCafeNoticeUseCase: UpdateCafeNoticeUseCase = KoinInitializerKt.resolveUpdateCafeNoticeUseCase(),
        updateCafeEventUseCase: UpdateCafeEventUseCase = KoinInitializerKt.resolveUpdateCafeEventUseCase(),
        deleteCafeNoticeUseCase: DeleteCafeNoticeUseCase = KoinInitializerKt.resolveDeleteCafeNoticeUseCase(),
        deleteCafeEventUseCase: DeleteCafeEventUseCase = KoinInitializerKt.resolveDeleteCafeEventUseCase(),
        noticeManagementEventPublisher: NoticeManagementEventPublisher = KoinInitializerKt.resolveNoticeManagementEventPublisher(),
        uploadImageUseCase: UploadImageUseCase = KoinInitializerKt.resolveUploadImageUseCase()
    ) {
        self.cafeId = cafeId
        self.getCafeNoticePageUseCase = getCafeNoticePageUseCase
        self.getCafeEventPageUseCase = getCafeEventPageUseCase
        self.createCafeNoticeUseCase = createCafeNoticeUseCase
        self.createCafeEventUseCase = createCafeEventUseCase
        self.updateCafeNoticeUseCase = updateCafeNoticeUseCase
        self.updateCafeEventUseCase = updateCafeEventUseCase
        self.deleteCafeNoticeUseCase = deleteCafeNoticeUseCase
        self.deleteCafeEventUseCase = deleteCafeEventUseCase
        self.noticeManagementEventPublisher = noticeManagementEventPublisher
        self.uploadImageUseCase = uploadImageUseCase

        observeNoticeManagementEvent()
        loadNoticePage(cursor: nil, append: false)
    }

    private func uploadEventImageIfNeeded(_ imageUrl: String) async throws -> String {
        let trimmed = imageUrl.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty {
            return trimmed
        }
        let result = try await uploadImageUseCase.invoke(localPath: trimmed, folder: "events")
        if let success = result as? AppResultSuccess<AnyObject>, let data = success.data as? String {
            return data
        }
        if let failure = result as? AppResultFailure, let validation = failure.error as? AppErrorValidationFailed {
            throw NSError(domain: "NoticeEvent", code: 1, userInfo: [NSLocalizedDescriptionKey: validation.reason])
        }
        throw NSError(domain: "NoticeEvent", code: 1, userInfo: [NSLocalizedDescriptionKey: MessageKey.imageUploadFailed])
    }

    deinit {
        tasks.values.forEach { $0.cancel() }
        tasks.removeAll()
    }

    private enum TaskKey {
        case noticePage
        case eventPage
        case submit
        case delete
        case noticeManagementEvent
    }

    private enum MessageKey {
        static let noticeEditTargetNotFound = "noticeevent_info_notice_edit_target_not_found"
        static let eventEditTargetNotFound = "noticeevent_info_event_edit_target_not_found"
        static let noticeLoadFailed = "noticeevent_info_notice_load_failed"
        static let eventLoadFailed = "noticeevent_info_event_load_failed"
        static let noticeCreated = "noticeevent_info_notice_created"
        static let noticeUpdated = "noticeevent_info_notice_updated"
        static let eventCreated = "noticeevent_info_event_created"
        static let eventUpdated = "noticeevent_info_event_updated"
        static let noticeCreateFailed = "noticeevent_info_notice_create_failed"
        static let noticeUpdateFailed = "noticeevent_info_notice_update_failed"
        static let eventCreateFailed = "noticeevent_info_event_create_failed"
        static let eventUpdateFailed = "noticeevent_info_event_update_failed"
        static let noticeDeleteSuccess = "noticeevent_info_notice_delete_success"
        static let noticeDeleteFailed = "noticeevent_info_notice_delete_failed"
        static let eventDeleteSuccess = "noticeevent_info_event_delete_success"
        static let eventDeleteFailed = "noticeevent_info_event_delete_failed"
        static let imageUploadFailed = "noticeevent_info_image_upload_failed"
        static let moreEventsNextStep = "noticeevent_info_more_events_next_step"
        static let imagePickRequired = "noticeevent_info_image_pick_required"
        static let reserveScheduleNextStep = "noticeevent_info_reserve_schedule_next_step"
    }
}
