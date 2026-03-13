//
//  NoticeEventViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/11.
//

import Foundation
import Combine
import Shared

@MainActor
final class NoticeEventViewModel: ObservableObject {
    private let cafeId: String

    private let getCafeNoticePageUseCase: GetCafeNoticePageUseCase

    private let getCafeEventPageUseCase: GetCafeEventPageUseCase

    @Published private(set) var uiState = NoticeEventUiState()

    let event = PassthroughSubject<NoticeEventEvent, Never>()

    private var tasks: [TaskKey: Task<Void, Never>] = [:]

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
                    let mapped = items.map(mapNotice)
                    uiState.notices = append ? (uiState.notices + mapped) : mapped
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
                    let mapped = items.map(mapEvent)
                    uiState.events = append ? (uiState.events + mapped) : mapped
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
              let cursor = uiState.noticeNextCursor
        else { return }
        loadNoticePage(cursor: cursor, append: true)
    }

    private func loadMoreEvents() {
        guard !uiState.isLoadingEvents,
              !uiState.isLoadingMoreEvents,
              uiState.canLoadMoreEvents,
              let cursor = uiState.eventNextCursor
        else { return }
        loadEventPage(cursor: cursor, append: true)
    }

    private func submitForm() {
        guard uiState.isFormSubmitEnabled else { return }

        if uiState.selectedTab == .notice {
            let nextNotice = NoticeItem(
                id: "local-notice-\(uiState.notices.count + 1)",
                title: uiState.formTitle.trimmingCharacters(in: .whitespacesAndNewlines),
                date: "2026.03.13",
                isPinned: uiState.formPinned,
                statusLabel: uiState.formReservedAt.isEmpty ? "게시 중" : "임시 저장",
                statusAccent: uiState.formReservedAt.isEmpty ? .published : .draft
            )
            uiState.notices = [nextNotice] + uiState.notices
            uiState.infoMessage = "공지사항이 목록에 추가되었습니다."
        } else {
            let nextEvent = EventItem(
                id: "local-event-\(uiState.events.count + 1)",
                title: uiState.formTitle.trimmingCharacters(in: .whitespacesAndNewlines),
                period: uiState.formReservedAt.isEmpty ? "게시 일정 선택 필요" : uiState.formReservedAt,
                statusLabel: uiState.formReservedAt.isEmpty ? "진행 예정" : "진행 중",
                imageUrl: uiState.formImageUrl,
                isDimmed: false
            )
            uiState.events = [nextEvent] + uiState.events
            uiState.selectedTab = .event
            uiState.infoMessage = "이벤트가 목록에 추가되었습니다."
        }

        uiState.isFormSheetVisible = false
        uiState.formTitle = ""
        uiState.formContent = ""
        uiState.formImageUrl = ""
        uiState.formPinned = false
        uiState.formReservedAt = ""
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
            uiState.isFormSheetVisible = true
            uiState.formTitle = ""
            uiState.formContent = ""
            uiState.formImageUrl = ""
            uiState.formPinned = false
            uiState.formReservedAt = ""
            uiState.infoMessage = nil
        case .clickMoreEvents:
            uiState.infoMessage = "이벤트 전체 목록 연결은 다음 단계에서 이어집니다."
        case .clickEditNotice:
            uiState.infoMessage = "편집 기능은 다음 단계에서 연결됩니다."
        case .clickDeleteNotice:
            uiState.infoMessage = "삭제 기능은 다음 단계에서 연결됩니다."
        case .clickEventMenu:
            uiState.infoMessage = "이벤트 상세 메뉴는 다음 단계에서 연결됩니다."
        case .dismissFormSheet:
            uiState.isFormSheetVisible = false
        case .changeFormTitle(let value):
            uiState.formTitle = value
        case .changeFormContent(let value):
            uiState.formContent = value
        case .clickFormImage:
            if uiState.formImageUrl.isEmpty {
                uiState.formImageUrl = sampleEventImageUrl
                uiState.infoMessage = nil
            } else {
                uiState.infoMessage = "이미지는 한 장만 첨부할 수 있습니다."
            }
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

    private func mapNotice(_ item: CafeNoticeManagementItem) -> NoticeItem {
        let accent: NoticeStatusAccent
        switch item.statusAccent {
        case .published:
            accent = .published
        case .draft:
            accent = .draft
        case .ended:
            accent = .ended
        default:
            accent = .published
        }

        return NoticeItem(
            id: item.id,
            title: item.title,
            date: item.displayDate,
            isPinned: item.isPinned,
            statusLabel: item.statusLabel,
            statusAccent: accent
        )
    }

    private func mapEvent(_ item: CafeEventManagementItem) -> EventItem {
        EventItem(
            id: item.id,
            title: item.title,
            period: item.periodText,
            statusLabel: item.statusLabel,
            imageUrl: item.imageUrl,
            isDimmed: item.isDimmed
        )
    }

    init(
        cafeId: String,
        getCafeNoticePageUseCase: GetCafeNoticePageUseCase = KoinInitializerKt.resolveGetCafeNoticePageUseCase(),
        getCafeEventPageUseCase: GetCafeEventPageUseCase = KoinInitializerKt.resolveGetCafeEventPageUseCase()
    ) {
        self.cafeId = cafeId
        self.getCafeNoticePageUseCase = getCafeNoticePageUseCase
        self.getCafeEventPageUseCase = getCafeEventPageUseCase

        loadNoticePage(cursor: nil, append: false)
    }

    deinit {
        tasks.values.forEach { $0.cancel() }
        tasks.removeAll()
    }

    private enum TaskKey {
        case noticePage
        case eventPage
    }
}
