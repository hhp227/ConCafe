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
    case navigateToCafeEvent(cafeId: String, eventId: String, showCafeButton: Bool = false)
    case replaceWithCafe(id: String)
    case navigateToCafeDashboard(id: String)
    case navigateToBanner(cafeId: String? = nil)
    case navigateToBannerEdit(cafeId: String? = nil, bannerId: String? = nil)
    case navigateToExternalLink(title: String, url: String)
    case navigateToCafeInfoEdit(id: String?, isRegistrationMode: Bool = false)
    case navigateToNoticeEvent(id: String)
    case navigateToCastEdit(cafeId: String?, castId: String?)
    case navigateToSchedule(castId: String? = nil)
    case navigateToCastManagement(cafeId: String, cafeName: String)
    case navigateToCastList(cafeId: String)
    case navigateToMenuGoods(id: String)
    case navigateToMenuGoodsEdit(cafeId: String, itemId: String? = nil)
    case navigateToReviewEdit(cafeId: String, reviewId: String? = nil)
    case navigateToPicture(imageUrl: String)
    case navigateToCheckInMap(initialRegionKey: String? = nil)
    case navigateToSignIn
    case navigateToSignUp
    case navigateToResetPassword
    case navigateToNotification
    case navigateToSettings
    case navigateToNotificationSettings
    case navigateToAccountSettings
    case navigateToInquiry
    case navigateToUserManagement
    case navigateToDormantAccount
    case navigateToChangePassword
    case navigateToCommunity
    case navigateToPostEdit(postId: String? = nil)
    case navigateToPostDetail(postId: String)
    case navigateBack
    case refreshUnreadNotificationCount
}
