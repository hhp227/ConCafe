//
//  CafeDashboardViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/10.
//

import Foundation
import Combine

@MainActor
final class CafeDashboardViewModel: ObservableObject {
    @Published private(set) var uiState: CafeDashboardUiState

    let event = PassthroughSubject<CafeDashboardEvent, Never>()

    init(cafeId: String) {
        uiState = CafeDashboardUiState.preview(cafeId: cafeId)
    }

    private func clickBack() {
        event.send(.navigateBack)
    }

    private func clickShortcut(_ shortcut: CafeDashboardUiState.Shortcut) {
        uiState.infoMessage = "\(shortcut.title) 연결은 다음 단계에서 이어집니다."
    }

    private func dismissInfoMessage() {
        uiState.infoMessage = nil
    }

    func onAction(_ action: CafeDashboardAction) {
        switch action {
        case .clickBack:
            clickBack()
        case .clickShortcut(let shortcut):
            clickShortcut(shortcut)
        case .dismissInfoMessage:
            dismissInfoMessage()
        }
    }
}
