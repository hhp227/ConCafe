//
//  ExploreUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/06.
//

import Foundation
import Shared

struct ExploreUiState {
    var isLoading: Bool
    var isLoggedIn: Bool
    var isLoginPromptVisible: Bool
    var errorMessage: String?
    var query: String
    var selectedTab: TabType
    var selectedRegion: RegionFilter
    var selectedSort: SortFilter
    var cafes: [Cafe]
    var cafesNextCursor: String?
    var canLoadMoreCafes: Bool
    var isLoadingMoreCafes: Bool
    var maids: [Cast]
    var maidsNextCursor: String?
    var canLoadMoreMaids: Bool
    var isLoadingMoreMaids: Bool

    static let empty = ExploreUiState(
        isLoading: false,
        isLoggedIn: false,
        isLoginPromptVisible: false,
        errorMessage: nil,
        query: "",
        selectedTab: .cafe,
        selectedRegion: .all,
        selectedSort: .popular,
        cafes: [],
        cafesNextCursor: nil,
        canLoadMoreCafes: false,
        isLoadingMoreCafes: false,
        maids: [],
        maidsNextCursor: nil,
        canLoadMoreMaids: false,
        isLoadingMoreMaids: false
    )

    enum TabType: String, CaseIterable {
        case cafe = "카페"
        case maid = "캐스트"
    }

    enum RegionFilter: String, CaseIterable {
        case all = "all"
        case seoul = "seoul"
        case tokyo = "tokyo"
        case osaka = "osaka"

        var label: String {
            switch self {
            case .all: return "전체"
            case .seoul: return "서울"
            case .tokyo: return "도쿄"
            case .osaka: return "오사카"
            }
        }
    }

    enum SortFilter: String, CaseIterable {
        case popular = "popular"
        case latest = "latest"
        case rating = "rating"

        var label: String {
            switch self {
            case .popular: return "인기순"
            case .latest: return "최신순"
            case .rating: return "평점순"
            }
        }
    }
}
