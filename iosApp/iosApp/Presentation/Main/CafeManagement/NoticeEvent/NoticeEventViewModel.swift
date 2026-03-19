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
            uiState.infoMessage = "수정할 공지사항을 찾지 못했습니다."
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
            uiState.infoMessage = "수정할 이벤트를 찾지 못했습니다."
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
                    uiState.infoMessage = "공지사항을 불러오지 못했습니다."
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isLoadingNotices = false
                uiState.isLoadingMoreNotices = false
                uiState.infoMessage = "공지사항을 불러오지 못했습니다."
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
                    uiState.infoMessage = "이벤트를 불러오지 못했습니다."
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isLoadingEvents = false
                uiState.isLoadingMoreEvents = false
                uiState.infoMessage = "이벤트를 불러오지 못했습니다."
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
                    if let editingId = uiState.formEditingId {
                        result = try await updateCafeEventUseCase.invoke(
                            input: CafeEventUpdate(
                                cafeId: cafeId,
                                eventId: editingId,
                                title: uiState.formTitle,
                                content: uiState.formContent,
                                imageUrl: uiState.formImageUrl,
                                periodText: uiState.formReservedAt.isEmpty ? nil : uiState.formReservedAt
                            )
                        )
                    } else {
                        result = try await createCafeEventUseCase.invoke(
                            input: CafeEventCreate(
                                cafeId: cafeId,
                                title: uiState.formTitle,
                                content: uiState.formContent,
                                imageUrl: uiState.formImageUrl,
                                periodText: uiState.formReservedAt.isEmpty ? nil : uiState.formReservedAt
                            )
                        )
                    }
                }

                if Task.isCancelled { return }

                if let failure = result as? AppResultFailure {
                    if let validation = failure.error as? AppErrorValidationFailed {
                        uiState.infoMessage = validation.reason.toNoticeEventValidationMessage()
                    } else if selectedTab == .notice {
                        uiState.infoMessage = isEditing ? "공지사항 수정에 실패했습니다." : "공지사항 등록에 실패했습니다."
                    } else {
                        uiState.infoMessage = isEditing ? "이벤트 수정에 실패했습니다." : "이벤트 등록에 실패했습니다."
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
                    uiState.infoMessage = isEditing ? "공지사항이 수정되었습니다." : "공지사항이 등록되었습니다."
                } else {
                    uiState.infoMessage = isEditing ? "이벤트가 수정되었습니다." : "이벤트가 등록되었습니다."
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isSubmittingForm = false
                if selectedTab == .notice {
                    uiState.infoMessage = isEditing ? "공지사항 수정에 실패했습니다." : "공지사항 등록에 실패했습니다."
                } else {
                    uiState.infoMessage = isEditing ? "이벤트 수정에 실패했습니다." : "이벤트 등록에 실패했습니다."
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
                uiState.infoMessage = result is AppResultSuccess<AnyObject> ? "공지사항이 삭제되었습니다." : "공지사항 삭제에 실패했습니다."
            } catch {
                if Task.isCancelled { return }
                uiState.infoMessage = "공지사항 삭제에 실패했습니다."
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
                uiState.infoMessage = result is AppResultSuccess<AnyObject> ? "이벤트가 삭제되었습니다." : "이벤트 삭제에 실패했습니다."
            } catch {
                if Task.isCancelled { return }
                uiState.infoMessage = "이벤트 삭제에 실패했습니다."
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
            uiState.infoMessage = "이벤트 전체 목록 연결은 다음 단계에서 이어집니다."
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
            uiState.infoMessage = "이미지를 첨부하려면 이미지 선택 기능을 사용해 주세요."
        case .clickRemoveFormImage:
            uiState.formImageUrl = ""
            uiState.infoMessage = nil
        case .changeFormPinned(let value):
            uiState.formPinned = value
        case .clickReserveSchedule:
            uiState.infoMessage = "게시 예약 기능은 다음 단계에서 연결됩니다."
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
        noticeManagementEventPublisher: NoticeManagementEventPublisher = KoinInitializerKt.resolveNoticeManagementEventPublisher()
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

        observeNoticeManagementEvent()
        loadNoticePage(cursor: nil, append: false)
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
}

private extension String {
    func toNoticeEventValidationMessage() -> String {
        switch self {
        case "cafeId is required":
            return "카페 정보를 찾을 수 없습니다."
        case "noticeId is required":
            return "공지사항 정보를 찾을 수 없습니다."
        case "eventId is required":
            return "이벤트 정보를 찾을 수 없습니다."
        case "notice title is required", "event title is required":
            return "제목을 입력해 주세요."
        case "notice content is required", "event content is required":
            return "내용을 입력해 주세요."
        case "event image is required":
            return "이벤트 대표 이미지를 첨부해 주세요."
        default:
            return self
        }
    }
}
