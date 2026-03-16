//
//  SettingsEvent.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import Foundation

enum SettingsEvent {
    case navigateBack
    case navigateToAccountSettings
    case navigateToNotificationSettings
    case navigateToInquiry
    case navigateToExternalLink(title: String, url: String)
}
