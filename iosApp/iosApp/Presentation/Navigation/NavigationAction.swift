//
//  NavigationAction.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/05.
//

import Foundation

enum NavigationAction {
    case navigateToMain(initialTab: String? = nil)
    case navigateToCast(id: String)
    case navigateToCafe(id: String)
    case navigateToSignIn
    case navigateToNotification
    case navigateToSettings
    case navigateBack
}
