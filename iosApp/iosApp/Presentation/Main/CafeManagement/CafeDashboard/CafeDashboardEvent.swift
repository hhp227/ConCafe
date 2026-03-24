//
//  CafeDashboardEvent.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/10.
//

import Foundation

enum CafeDashboardEvent {
    case navigateBack
    case navigateToBanner(cafeId: String)
    case navigateToBannerEdit(cafeId: String)
    case navigateToCafeInfoEdit(cafeId: String)
    case navigateToNoticeEvent(cafeId: String)
    case navigateToMenuGoods(cafeId: String)
    case navigateToCastEdit(cafeId: String, castId: String?)
    case navigateToSchedule(castId: String?)
    case navigateToExternalLink(title: String, url: String)
}
