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
    case cafeDashboard(param: String)
    case bannerEdit(cafeId: String?)
    case cafeInfoEdit(param: String?, isRegistrationMode: Bool)
    case noticeEvent(param: String)
    case castEdit(cafeId: String?, castId: String?)
    case schedule(castId: String?)
    case menuGoods(param: String)
    case menuGoodsEdit(cafeId: String, itemId: String?)
    case reviewEdit(cafeId: String)
    case signIn
    case signUp
    case notification
    case settings
}
