//
//  RankingUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/06.
//

import Foundation

struct RankingUiState {
    enum TabType: String, CaseIterable {
        case maids = "메이드 랭킹"
        case cafes = "카페 랭킹"
    }

    enum PeriodFilter: String, CaseIterable {
        case weekly = "주간"
        case monthly = "월간"
    }

    enum RegionFilter: String, CaseIterable {
        case all = "전체"
        case seoul = "서울"
        case tokyo = "도쿄"
        case osaka = "오사카"
    }

    struct PromoAd: Identifiable {
        let id: String
        let badge: String
        let title: String
        let subtitle: String
        let description: String
        let startColorHex: String
        let endColorHex: String
        let symbol: String
    }

    struct RankingEntry: Identifiable {
        let id: String
        let rank: Int
        let name: String
        let subtitle: String
        let score: Int
        let change: String
        let startColorHex: String
        let endColorHex: String
        let symbol: String
    }

    var isLoading: Bool = false
    var errorMessage: String? = nil
    var selectedTab: TabType = .maids
    var selectedPeriod: PeriodFilter = .weekly
    var selectedRegion: RegionFilter = .all
    var selectedAdIndex: Int = 0
    var ads: [PromoAd] = []
    var maidRankings: [RankingEntry] = []
    var cafeRankings: [RankingEntry] = []

    var currentAd: PromoAd {
        ads.isEmpty
            ? PromoAd(id: "", badge: "", title: "", subtitle: "", description: "", startColorHex: "F6A8C5", endColorHex: "FFC8A2", symbol: "🎀")
            : ads[min(max(selectedAdIndex, 0), ads.count - 1)]
    }

    var rankingEntries: [RankingEntry] {
        selectedTab == .maids ? maidRankings : cafeRankings
    }

    static let empty = RankingUiState()

}
