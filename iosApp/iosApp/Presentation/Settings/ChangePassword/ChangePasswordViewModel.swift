//
//  ChangePasswordViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/15.
//

import Foundation
import Combine

@MainActor
final class ChangePasswordViewModel: ObservableObject {
    @Published private(set) var uiState = ChangePasswordUiState.empty

    let event = PassthroughSubject<ChangePasswordEvent, Never>()

    func onAction(_ action: ChangePasswordAction) {
        switch action {
        case .backTapped:
            event.send(.navigateBack)
        case .currentPasswordChanged(let value):
            uiState.currentPassword = value
        case .newPasswordChanged(let value):
            uiState.newPassword = value
        case .confirmPasswordChanged(let value):
            uiState.confirmPassword = value
        case .submitTapped:
            submit()
        }
    }

    private func submit() {
        let message: String
        if uiState.currentPassword.isEmpty {
            message = "현재 비밀번호를 입력해 주세요."
        } else if uiState.newPassword.count < minimumPasswordLength {
            message = "새 비밀번호는 8자 이상이어야 합니다."
        } else if uiState.newPassword != uiState.confirmPassword {
            message = "새 비밀번호 확인이 일치하지 않습니다."
        } else {
            uiState = .empty
            message = "비밀번호 변경 요청을 처리했어요. 현재 단계에서는 확인 피드백만 제공됩니다."
        }
        event.send(.showMessage(message))
    }

    private let minimumPasswordLength = 8
}
