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
    case navigateToCafeDashboard(id: String)
    case navigateToCafeInfoEdit(id: String)
    case navigateToCastEdit(cafeId: String?, castId: String?)
    case navigateToSchedule(castId: String? = nil)
    case navigateToMenuGoods(id: String)
    case navigateToMenuGoodsEdit(cafeId: String, itemId: String? = nil)
    case navigateToSignIn
    case navigateToSignUp
    case navigateToNotification
    case navigateToSettings
    case navigateBack
}
