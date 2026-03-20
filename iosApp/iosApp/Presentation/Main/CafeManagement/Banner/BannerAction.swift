//
//  BannerAction.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/15.
//

import Foundation

enum BannerAction {
    case backTapped
    case selectTab(BannerTab)
    case createBannerTapped
    case editBannerTapped(id: String)
    case deleteBannerTapped(id: String)
    case confirmDeleteBanner
    case dismissDeleteBannerDialog
}
