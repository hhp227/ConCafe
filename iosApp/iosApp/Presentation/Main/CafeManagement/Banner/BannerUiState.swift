//
//  BannerUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/15.
//

import Foundation

struct BannerUiState {
    var selectedTab: BannerTab = .active
    var banners: [BannerItem] = []
    var pendingDeleteBannerId: String? = nil

    var filteredBanners: [BannerItem] {
        banners.filter { $0.tab == selectedTab }
    }

    var pendingDeleteBanner: BannerItem? {
        guard let pendingDeleteBannerId else { return nil }
        return banners.first { $0.id == pendingDeleteBannerId }
    }
}

struct BannerItem: Identifiable {
    let id: String
    let cafeId: String?
    let title: String
    let description: String
    let periodDays: Int32
    let statusLabelKey: String
    let tab: BannerTab
    let accentHex: String
    let imageIcon: String
    let imageUrl: String?
}

enum BannerTab: String, CaseIterable {
    case active = "banner_section_active"
    case scheduled = "banner_section_scheduled"
    case ended = "banner_section_ended"
}
