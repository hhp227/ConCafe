//
//  ChangePasswordViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/15.
//

import Foundation
import Combine
import Shared

@MainActor
final class ChangePasswordViewModel: ObservableObject {
    private let changePasswordUseCase: ChangePasswordUseCase

    private let minimumPasswordLength = 8

    @Published private(set) var uiState = ChangePasswordUiState.empty

    let event = PassthroughSubject<ChangePasswordEvent, Never>()

    private var submitTask: Task<Void, Never>?

    private func changeCurrentPassword(_ value: String) {
        uiState.currentPassword = value
    }

    private func changeNewPassword(_ value: String) {
        uiState.newPassword = value
    }

    private func changeConfirmPassword(_ value: String) {
        uiState.confirmPassword = value
    }

    private func submit() {
        let currentPassword = uiState.currentPassword
        let newPassword = uiState.newPassword
        let confirmPassword = uiState.confirmPassword
        let validationMessage = validate(
            currentPassword: currentPassword,
            newPassword: newPassword,
            confirmPassword: confirmPassword
        )

        if validationMessage == nil {
            uiState.isSubmitting = true

            submitTask?.cancel()
            submitTask = Task {
                do {
                    let result = try await changePasswordUseCase.invoke(
                        currentPassword: currentPassword,
                        newPassword: newPassword
                    )

                    if result is AppResultSuccess<AnyObject> {
                        uiState = .empty
                        event.send(.showMessage("비밀번호가 안전하게 변경되었습니다."))
                    } else if let failure = result as? AppResultFailure {
                        uiState.isSubmitting = false
                        event.send(.showMessage(mapFailureMessage(failure)))
                    } else {
                        uiState.isSubmitting = false
                        event.send(.showMessage("비밀번호 변경에 실패했습니다. 잠시 후 다시 시도해 주세요."))
                    }
                } catch {
                    if Task.isCancelled {
                        submitTask = nil
                    } else {
                        uiState.isSubmitting = false
                        event.send(.showMessage("비밀번호 변경에 실패했습니다. 잠시 후 다시 시도해 주세요."))
                    }
                }
            }
        } else {
            event.send(.showMessage(validationMessage ?? "비밀번호 입력값을 확인해 주세요."))
        }
    }

    private func validate(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String
    ) -> String? {
        if currentPassword.isEmpty {
            return "현재 비밀번호를 입력해 주세요."
        } else if newPassword.count < minimumPasswordLength {
            return "새 비밀번호는 8자 이상이어야 합니다."
        } else if newPassword != confirmPassword {
            return "새 비밀번호 확인이 일치하지 않습니다."
        } else if currentPassword == newPassword {
            return "현재 비밀번호와 다른 새 비밀번호를 입력해 주세요."
        } else {
            return nil
        }
    }

    private func mapFailureMessage(_ failure: AppResultFailure) -> String {
        let rawError = String(describing: failure.error).uppercased()

        if rawError.contains("INVALID PASSWORD")
            || rawError.contains("INVALID_LOGIN_CREDENTIALS")
            || rawError.contains("INVALID_PASSWORD")
            || rawError.contains("EMAIL_NOT_FOUND")
            || rawError.contains("PASSWORD DOES NOT MATCH CURRENT USER") {
            return "현재 비밀번호가 올바르지 않습니다."
        } else if rawError.contains("REQUIRES_RECENT_LOGIN") {
            return "보안을 위해 다시 로그인한 뒤 비밀번호를 변경해 주세요."
        } else {
            return "비밀번호 변경에 실패했습니다. 잠시 후 다시 시도해 주세요."
        }
    }

    func onAction(_ action: ChangePasswordAction) {
        switch action {
        case .backTapped:
            event.send(.navigateBack)
        case .currentPasswordChanged(let value):
            changeCurrentPassword(value)
        case .newPasswordChanged(let value):
            changeNewPassword(value)
        case .confirmPasswordChanged(let value):
            changeConfirmPassword(value)
        case .submitTapped:
            if uiState.isSubmitting {
                break
            } else {
                submit()
            }
        }
    }

    init(
        changePasswordUseCase: ChangePasswordUseCase = KoinInitializerKt.resolveChangePasswordUseCase()
    ) {
        self.changePasswordUseCase = changePasswordUseCase
    }

    deinit {
        submitTask?.cancel()
        submitTask = nil
    }
}
