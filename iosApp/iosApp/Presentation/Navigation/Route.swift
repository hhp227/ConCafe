//
//  Route.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/05.
//

import Foundation

enum Route: Hashable {
    case entry
    case main(initialTab: String?)
    case cast(param: String)
    case cafe(param: String)
    case cafeEvent(cafeId: String, eventId: String)
    case cafeDashboard(param: String)
    case banner(cafeId: String?)
    case bannerEdit(cafeId: String?, bannerId: String?)
    case externalLink(title: String, url: String)
    case cafeInfoEdit(param: String?, isRegistrationMode: Bool)
    case noticeEvent(param: String)
    case castEdit(cafeId: String?, castId: String?)
    case schedule(castId: String?)
    case castManagement(cafeId: String, cafeName: String)
    case menuGoods(param: String)
    case menuGoodsEdit(cafeId: String, itemId: String?)
    case reviewEdit(cafeId: String, reviewId: String?)
    case picture(imageUrl: String)
    case checkInMap(initialRegionKey: String?)
    case signIn
    case signUp
    case resetPassword
    case notification
    case settings
    case notificationSettings
    case accountSettings
    case inquiry
    case changePassword
    case community
    case postEdit
    case postDetail(postId: String)
}
