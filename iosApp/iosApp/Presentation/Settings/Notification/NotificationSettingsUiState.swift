//
//  NotificationSettingsUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/14.
//

import Foundation

struct NotificationSettingsUiState {
    var isPushNotificationsEnabled: Bool
    var isShiftNotificationsEnabled: Bool
    var isBirthdayNotificationsEnabled: Bool
    var isNoticeNotificationsEnabled: Bool
    var quietHoursOption: NotificationQuietHoursOption

    static let initial = NotificationSettingsUiState(
        isPushNotificationsEnabled: true,
        isShiftNotificationsEnabled: true,
        isBirthdayNotificationsEnabled: true,
        isNoticeNotificationsEnabled: false,
        quietHoursOption: .night
    )
}
