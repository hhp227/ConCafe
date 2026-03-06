//
//  NavigationAction.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/05.
//

import Foundation

enum NavigationAction {
    case navigateToMain(initialTab: String? = nil)
    case navigateToCastDetail(id: String)
    case navigateToCafeDetail(id: String)
    case navigateToNotification
}
