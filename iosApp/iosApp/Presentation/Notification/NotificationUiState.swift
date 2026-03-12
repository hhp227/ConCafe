//
//  NotificationUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/06.
//

import Foundation
import Shared

struct NotificationUiState {
    var isLoading: Bool = true
    var errorMessage: String?
    var isLoggedIn: Bool = false
    var unreadCount: Int32 = 0
    var sections: [NotificationSection] = []

    static let empty = NotificationUiState()
}
