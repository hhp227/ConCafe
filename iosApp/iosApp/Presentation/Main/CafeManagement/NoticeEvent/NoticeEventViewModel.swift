//
//  NoticeEventViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/11.
//

import Foundation
import Combine

@MainActor
final class NoticeEventViewModel: ObservableObject {
    @Published private(set) var uiState = NoticeEventUiState()

    let event = PassthroughSubject<NoticeEventEvent, Never>()

    private func submitForm() {
        guard uiState.isFormSubmitEnabled else { return }

        if uiState.selectedTab == .notice {
            let nextNotice = NoticeItem(
                id: "notice-\(uiState.notices.count + 1)",
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
                id: "event-\(uiState.events.count + 1)",
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
        case .changeQuery(let value):
            uiState.query = value
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
}
