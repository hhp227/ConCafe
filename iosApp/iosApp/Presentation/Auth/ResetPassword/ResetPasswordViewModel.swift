//
//  ResetPasswordViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/28.
//

import Foundation
import Combine
import Shared

@MainActor
final class ResetPasswordViewModel: ObservableObject {
    private let requestPasswordResetUseCase: RequestPasswordResetUseCase

    @Published private(set) var uiState = ResetPasswordUiState.empty

    let event = PassthroughSubject<ResetPasswordEvent, Never>()

    private var submitTask: Task<Void, Never>?

    private func changeEmail(_ value: String) {
        uiState.email = value
    }

    private func submit() {
        let email = uiState.email.trimmingCharacters(in: .whitespacesAndNewlines)
        let validationMessage = validate(email)

        if validationMessage == nil {
            uiState.isSubmitting = true

            submitTask?.cancel()
            submitTask = Task {
                do {
                    let result = try await requestPasswordResetUseCase.invoke(email: email)

                    if result is AppResultSuccess<AnyObject> {
                        uiState = .empty
                        event.send(.showMessage("비밀번호 재설정 메일을 발송했습니다."))
                    } else if let failure = result as? AppResultFailure {
                        uiState.isSubmitting = false
                        event.send(.showMessage(mapFailureMessage(failure)))
                    } else {
                        uiState.isSubmitting = false
                        event.send(.showMessage("재설정 메일 발송에 실패했습니다. 잠시 후 다시 시도해 주세요."))
                    }
                } catch {
                    if Task.isCancelled {
                        submitTask = nil
                    } else {
                        uiState.isSubmitting = false
                        event.send(.showMessage("재설정 메일 발송에 실패했습니다. 잠시 후 다시 시도해 주세요."))
                    }
                }
            }
        } else {
            event.send(.showMessage(validationMessage ?? "이메일을 확인해 주세요."))
        }
    }

    private func validate(_ email: String) -> String? {
        let pattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
        let range = NSRange(location: 0, length: email.utf16.count)
        let regex = try? NSRegularExpression(pattern: pattern)
        let hasMatch = regex?.firstMatch(in: email, options: [], range: range) != nil

        if email.isEmpty {
            return "이메일을 입력해 주세요."
        } else if !hasMatch {
            return "올바른 이메일 형식을 입력해 주세요."
        } else {
            return nil
        }
    }

    private func mapFailureMessage(_ failure: AppResultFailure) -> String {
        let rawError = String(describing: failure.error).uppercased()

        if rawError.contains("EMAIL_NOT_FOUND") {
            return "등록되지 않은 이메일입니다."
        } else if rawError.contains("TOO_MANY_ATTEMPTS_TRY_LATER") {
            return "요청이 많습니다. 잠시 후 다시 시도해 주세요."
        } else {
            return "재설정 메일 발송에 실패했습니다. 잠시 후 다시 시도해 주세요."
        }
    }

    func onAction(_ action: ResetPasswordAction) {
        switch action {
        case .backTapped:
            event.send(.navigateBack)
        case .emailChanged(let value):
            changeEmail(value)
        case .submitTapped:
            if uiState.isSubmitting {
                break
            } else {
                submit()
            }
        }
    }

    init(
        requestPasswordResetUseCase: RequestPasswordResetUseCase = KoinInitializerKt.resolveRequestPasswordResetUseCase()
    ) {
        self.requestPasswordResetUseCase = requestPasswordResetUseCase
    }

    deinit {
        submitTask?.cancel()
        submitTask = nil
    }
}
