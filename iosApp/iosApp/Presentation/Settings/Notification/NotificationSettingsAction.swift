//
//  NotificationSettingsAction.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/14.
//

import Foundation
import Shared

enum NotificationSettingsAction {
    case backTapped
    case pushNotificationsToggled(Bool)
    case shiftNotificationsToggled(Bool)
    case birthdayNotificationsToggled(Bool)
    case noticeNotificationsToggled(Bool)
    case quietHoursSelected(NotificationQuietHoursMode)
}
