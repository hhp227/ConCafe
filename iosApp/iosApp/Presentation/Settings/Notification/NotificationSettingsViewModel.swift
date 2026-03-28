//
//  NotificationSettingsViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/14.
//

import Foundation
import Combine
import Shared

@MainActor
final class NotificationSettingsViewModel: ObservableObject {
    private let getNotificationSettingsUseCase: GetNotificationSettingsUseCase

    private let updateNotificationSettingsUseCase: UpdateNotificationSettingsUseCase

    @Published private(set) var uiState = NotificationSettingsUiState.initial

    let event = PassthroughSubject<NotificationSettingsEvent, Never>()

    private func clickBack() {
        event.send(.navigateBack)
    }

    private func loadSettings() {
        uiState.isLoading = true
        uiState.errorMessage = nil

        Task {
            do {
                let result = try await getNotificationSettingsUseCase.invoke()

                if let success = result as? AppResultSuccess<AnyObject>,
                   let settings = success.data as? UserNotificationSettings {

                    uiState.isLoading = false
                    uiState.isSaving = false
                    uiState.errorMessage = nil
                    uiState.isPushNotificationsEnabled = settings.isPushNotificationsEnabled
                    uiState.isShiftNotificationsEnabled = settings.isShiftNotificationsEnabled
                    uiState.isBirthdayNotificationsEnabled = settings.isBirthdayNotificationsEnabled
                    uiState.isNoticeNotificationsEnabled = settings.isNoticeNotificationsEnabled
                    uiState.quietHoursOption = settings.quietHoursMode
                } else if let failure = result as? AppResultFailure {
                    uiState.isLoading = false
                    uiState.isSaving = false
                    uiState.errorMessage = failure.error.toMessage()
                } else {
                    uiState.isLoading = false
                    uiState.isSaving = false
                    uiState.errorMessage = "알림 설정을 불러오지 못했습니다."
                }
            } catch {
                if Task.isCancelled {
                    return
                } else {
                    uiState.isLoading = false
                    uiState.isSaving = false
                    uiState.errorMessage = "알림 설정을 불러오지 못했습니다."
                }
            }
        }
    }

    private func updateSettings(updateBlock: (inout NotificationSettingsUiState) -> Void) {
        let previousState = uiState
        var nextState = uiState

        updateBlock(&nextState)
        uiState = nextState
        uiState.isSaving = true
        uiState.errorMessage = nil

        let request = UserNotificationSettings(
            isPushNotificationsEnabled: nextState.isPushNotificationsEnabled,
            isShiftNotificationsEnabled: nextState.isShiftNotificationsEnabled,
            isBirthdayNotificationsEnabled: nextState.isBirthdayNotificationsEnabled,
            isNoticeNotificationsEnabled: nextState.isNoticeNotificationsEnabled,
            quietHoursMode: nextState.quietHoursOption
        )

        Task {
            do {
                let result = try await updateNotificationSettingsUseCase.invoke(settings: request)

                if let success = result as? AppResultSuccess<AnyObject>,
                   let settings = success.data as? UserNotificationSettings {

                    uiState.isLoading = false
                    uiState.isSaving = false
                    uiState.errorMessage = nil
                    uiState.isPushNotificationsEnabled = settings.isPushNotificationsEnabled
                    uiState.isShiftNotificationsEnabled = settings.isShiftNotificationsEnabled
                    uiState.isBirthdayNotificationsEnabled = settings.isBirthdayNotificationsEnabled
                    uiState.isNoticeNotificationsEnabled = settings.isNoticeNotificationsEnabled
                    uiState.quietHoursOption = settings.quietHoursMode
                } else if let failure = result as? AppResultFailure {
                    uiState = previousState
                    uiState.errorMessage = failure.error.toMessage()
                    event.send(.showMessage(failure.error.toMessage()))
                } else {
                    uiState = previousState
                    uiState.errorMessage = "알림 설정 저장에 실패했습니다."
                    event.send(.showMessage("알림 설정 저장에 실패했습니다."))
                }
            } catch {
                if Task.isCancelled {
                    return
                } else {
                    uiState = previousState
                    uiState.errorMessage = "알림 설정 저장에 실패했습니다."
                    event.send(.showMessage("알림 설정 저장에 실패했습니다."))
                }
            }
        }
    }

    func onAction(_ action: NotificationSettingsAction) {
        switch action {
        case .backTapped:
            clickBack()
        case .pushNotificationsToggled(let enabled):
            updateSettings { state in
                state.isPushNotificationsEnabled = enabled
            }
        case .shiftNotificationsToggled(let enabled):
            updateSettings { state in
                state.isShiftNotificationsEnabled = enabled
            }
        case .birthdayNotificationsToggled(let enabled):
            updateSettings { state in
                state.isBirthdayNotificationsEnabled = enabled
            }
        case .noticeNotificationsToggled(let enabled):
            updateSettings { state in
                state.isNoticeNotificationsEnabled = enabled
            }
        case .quietHoursSelected(let option):
            updateSettings { state in
                state.quietHoursOption = option
            }
        }
    }

    init(
        getNotificationSettingsUseCase: GetNotificationSettingsUseCase = KoinInitializerKt.resolveGetNotificationSettingsUseCase(),
        updateNotificationSettingsUseCase: UpdateNotificationSettingsUseCase = KoinInitializerKt.resolveUpdateNotificationSettingsUseCase()
    ) {
        self.getNotificationSettingsUseCase = getNotificationSettingsUseCase
        self.updateNotificationSettingsUseCase = updateNotificationSettingsUseCase

        loadSettings()
    }
}

private extension AppError {
    func toMessage() -> String {
        if self is AppErrorUnauthorized {
            return "로그인 후 알림 설정을 변경해 주세요."
        } else if self is AppErrorPermissionDenied {
            return "알림 설정 변경 권한이 없습니다."
        } else if self is AppErrorNotFound {
            return "알림 설정 데이터를 찾을 수 없습니다."
        } else if let error = self as? AppErrorValidationFailed {
            return error.reason
        } else if let error = self as? AppErrorNetworkError {
            return error.message ?? "네트워크 오류가 발생했습니다."
        } else if let error = self as? AppErrorUnknown {
            return error.cause ?? "알림 설정 저장에 실패했습니다."
        } else {
            return "알림 설정 저장에 실패했습니다."
        }
    }
}
