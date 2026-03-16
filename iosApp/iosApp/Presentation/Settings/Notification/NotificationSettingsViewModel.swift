//
//  NotificationSettingsViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/14.
//

import Foundation
import Combine

@MainActor
final class NotificationSettingsViewModel: ObservableObject {
    @Published private(set) var uiState = NotificationSettingsUiState.initial

    let event = PassthroughSubject<NotificationSettingsEvent, Never>()

    private func clickBack() {
        event.send(.navigateBack)
    }

    private func togglePushNotifications(_ enabled: Bool) {
        uiState.isPushNotificationsEnabled = enabled
    }

    private func toggleShiftNotifications(_ enabled: Bool) {
        uiState.isShiftNotificationsEnabled = enabled
    }

    private func toggleBirthdayNotifications(_ enabled: Bool) {
        uiState.isBirthdayNotificationsEnabled = enabled
    }

    private func toggleNoticeNotifications(_ enabled: Bool) {
        uiState.isNoticeNotificationsEnabled = enabled
    }

    private func selectQuietHours(_ option: NotificationQuietHoursOption) {
        uiState.quietHoursOption = option
    }

    func onAction(_ action: NotificationSettingsAction) {
        switch action {
        case .backTapped:
            clickBack()
        case .pushNotificationsToggled(let enabled):
            togglePushNotifications(enabled)
        case .shiftNotificationsToggled(let enabled):
            toggleShiftNotifications(enabled)
        case .birthdayNotificationsToggled(let enabled):
            toggleBirthdayNotifications(enabled)
        case .noticeNotificationsToggled(let enabled):
            toggleNoticeNotifications(enabled)
        case .quietHoursSelected(let option):
            selectQuietHours(option)
        }
    }
}
