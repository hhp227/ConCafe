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
    var isCastRole: Bool
    var isPushNotificationsEnabled: Bool
    var isShiftNotificationsEnabled: Bool
    var isBirthdayNotificationsEnabled: Bool
    var isNoticeNotificationsEnabled: Bool
    var isFollowNotificationsEnabled: Bool
    var isEventNotificationsEnabled: Bool
    var isCommunityNotificationsEnabled: Bool
    var quietHoursOption: NotificationQuietHoursMode

    static let initial = NotificationSettingsUiState(
        isLoading: false,
        isSaving: false,
        errorMessage: nil,
        isCastRole: false,
        isPushNotificationsEnabled: true,
        isShiftNotificationsEnabled: true,
        isBirthdayNotificationsEnabled: true,
        isNoticeNotificationsEnabled: true,
        isFollowNotificationsEnabled: true,
        isEventNotificationsEnabled: true,
        isCommunityNotificationsEnabled: true,
        quietHoursOption: .off
    )
}
