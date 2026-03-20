//
//  BannerUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/15.
//

import Foundation

struct BannerUiState {
    var screenTitle = "배너 관리"
    var selectedTab: BannerTab = .active
    var locationLabel = "홈 화면 상단"
    var banners: [BannerItem] = []
    var pendingDeleteBannerId: String? = nil

    var filteredBanners: [BannerItem] {
        banners.filter { $0.tab == selectedTab }
    }

    var sectionTitle: String {
        switch selectedTab {
        case .active:
            return "현재 노출 중인 배너"
        case .scheduled:
            return "노출 예정 배너"
        case .ended:
            return "종료된 배너"
        }
    }

    var sectionCountLabel: String {
        "\(sectionTitle) (\(filteredBanners.count))"
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
    let periodText: String
    let statusLabel: String
    let tab: BannerTab
    let accentHex: String
    let imageIcon: String
}

enum BannerTab: String, CaseIterable {
    case active = "진행 중"
    case scheduled = "예약"
    case ended = "종료"
}
