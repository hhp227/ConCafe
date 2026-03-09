//
//  NotificationAction.swift
//  iosApp
//
//  Created by 홍희표 on 2026/03/06.
//

import Foundation

enum NotificationAction {
    case backTapped
    case notificationTapped(id: String, type: String, targetId: String?)
    case signInTapped
    case refresh
}
