//
//  NotificationSettingsUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/14.
//

import Foundation
import Shared

struct NotificationSettingsUiState {
    var isLoading: Bool
    var isSaving: Bool
    var errorMessage: String?
    var isPushNotificationsEnabled: Bool
    var isShiftNotificationsEnabled: Bool
    var isBirthdayNotificationsEnabled: Bool
    var isNoticeNotificationsEnabled: Bool
    var quietHoursOption: NotificationQuietHoursMode

    static let initial = NotificationSettingsUiState(
        isLoading: false,
        isSaving: false,
        errorMessage: nil,
        isPushNotificationsEnabled: true,
        isShiftNotificationsEnabled: true,
        isBirthdayNotificationsEnabled: true,
        isNoticeNotificationsEnabled: false,
        quietHoursOption: .night
    )
}
