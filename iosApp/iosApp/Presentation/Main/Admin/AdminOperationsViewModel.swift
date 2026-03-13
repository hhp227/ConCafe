//
//  AdminOperationsViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import Foundation

@MainActor
final class AdminOperationsViewModel: ObservableObject {
    @Published private(set) var uiState = AdminOperationsUiState()

    private func handlePendingResult(id: String, approved: Bool) {
        guard let request = uiState.pendingRequests.first(where: { $0.id == id }) else { return }
        uiState.pendingRequests.removeAll { $0.id == id }
        uiState.infoMessage = approved
            ? "\(request.title) 요청을 승인했습니다."
            : "\(request.title) 요청을 반려했습니다."
    }

    func onAction(_ action: AdminOperationsAction) {
        switch action {
        case .clickNotifications:
            uiState.hasUnreadNotifications = false
            uiState.infoMessage = "새 알림을 모두 확인했습니다."
        case .clickSeeAllPending:
            uiState.infoMessage = "전체보기 연결은 다음 단계에서 이어집니다."
        case .selectPendingFilter(let filter):
            uiState.selectedPendingFilter = filter
            uiState.infoMessage = nil
        case .approvePending(let id):
            handlePendingResult(id: id, approved: true)
        case .rejectPending(let id):
            handlePendingResult(id: id, approved: false)
        case .clickQuickMenu(let id):
            let label = uiState.quickMenus.first(where: { $0.id == id })?.title ?? "메뉴"
            uiState.infoMessage = "\(label) 연결은 다음 단계에서 이어집니다."
        case .dismissInfoMessage:
            uiState.infoMessage = nil
        }
    }
}
