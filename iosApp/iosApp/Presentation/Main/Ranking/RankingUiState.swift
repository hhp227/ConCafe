//
//  RankingUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/06.
//

import Foundation
import Shared

struct RankingUiState {
    enum TabType: String, CaseIterable {
        case maids = "캐스트 랭킹"
        case cafes = "카페 랭킹"
    }

    enum RegionFilter: String, CaseIterable {
        case all = "전체"
        case seoul = "서울"
        case tokyo = "도쿄"
        case osaka = "오사카"

        var country: String? {
            switch self {
            case .all:
                return nil
            case .seoul:
                return "KR"
            case .tokyo, .osaka:
                return "JP"
            }
        }

        var city: String? {
            switch self {
            case .all:
                return nil
            case .seoul:
                return "Seoul"
            case .tokyo:
                return "Tokyo"
            case .osaka:
                return "Osaka"
            }
        }
    }

    var isLoading: Bool = false
    var isLoggedIn: Bool = false
    var isLoginPromptVisible: Bool = false
    var errorMessage: String? = nil
    var selectedTab: TabType = .maids
    var selectedPeriod: RankingPeriod = .weekly
    var selectedRegion: RegionFilter = .all
    var selectedAdIndex: Int = 0
    var ads: [Shared.RankingPromoAd] = []
    var maidRankings: [Shared.RankingFeedEntry] = []
    var cafeRankings: [Shared.RankingFeedEntry] = []

    var currentAd: Shared.RankingPromoAd {
        ads.isEmpty
            ? Shared.RankingPromoAd(id: "", badge: "", title: "", subtitle: "", desc: "", startColorHex: "F6A8C5", endColorHex: "FFC8A2", symbol: "🎀")
            : ads[min(max(selectedAdIndex, 0), ads.count - 1)]
    }

    var rankingEntries: [Shared.RankingFeedEntry] {
        selectedTab == .maids ? maidRankings : cafeRankings
    }

    static let empty = RankingUiState()

}
