//
//  CafeManagementViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import Foundation
import Combine

@MainActor
class CafeManagementViewModel: ObservableObject {
    @Published private(set) var uiState = CafeManagementUiState.preview

    let event = PassthroughSubject<CafeManagementEvent, Never>()

    private func selectCafe(_ cafeId: String) {
        uiState.selectedCafeId = cafeId
        uiState.infoMessage = nil
    }

    private func shortcutTapped(_ shortcut: CafeManagementUiState.Shortcut) {
        uiState.infoMessage = "\(shortcut.title) 화면 연결은 다음 단계에서 이어집니다."
    }

    private func searchCafeTapped() {
        uiState.infoMessage = "기존 카페 검색과 운영자 신청 플로우는 다음 단계에서 연결됩니다."
    }

    private func createCafeTapped() {
        uiState.infoMessage = "새 카페 등록 플로우는 다음 단계에서 연결됩니다."
    }

    private func dismissInfoMessageTapped() {
        uiState.infoMessage = nil
    }

    func onAction(_ action: CafeManagementAction) {
        switch action {
        case .selectCafe(let cafeId):
            selectCafe(cafeId)
        case .shortcutTapped(let shortcut):
            shortcutTapped(shortcut)
        case .searchCafeTapped:
            searchCafeTapped()
        case .createCafeTapped:
            createCafeTapped()
        case .dismissInfoMessageTapped:
            dismissInfoMessageTapped()
        }
    }
}
