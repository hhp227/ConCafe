//
//  CafeUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/06.
//

import Foundation
import Shared

struct CafeUiState {
    var isLoading: Bool
    var isLoadingMoreCasts: Bool
    var errorMessage: String?
    var selectedTab: TabType
    var detail: CafeDetail?
    var casts: [CafeDetailCast]
    var castsNextCursor: String?
    var canLoadMoreCasts: Bool
    var isLoadingMenuGoods: Bool
    var hasLoadedMenuGoods: Bool
    var isLoadingMoreNotices: Bool
    var noticesNextCursor: String?
    var canLoadMoreNotices: Bool
    var events: [CafeEventManagementItem]
    var notices: [CafeNoticeManagementItem]
    var isLoadingMoreReviews: Bool
    var reviewsNextCursor: String?
    var canLoadMoreReviews: Bool
    var reviews: [CafeDetailReview]
    var isFavorite: Bool
    var isLoggedIn: Bool
    var isVisitVerified: Bool
    var shouldScrollToTopOnReturn: Bool
    var currentUserId: String?
    var shouldShowFavoriteTooltip: Bool

    static let empty = CafeUiState(
        isLoading: false,
        isLoadingMoreCasts: false,
        errorMessage: nil,
        selectedTab: .info,
        detail: nil,
        casts: [],
        castsNextCursor: nil,
        canLoadMoreCasts: false,
        isLoadingMenuGoods: false,
        hasLoadedMenuGoods: false,
        isLoadingMoreNotices: false,
        noticesNextCursor: nil,
        canLoadMoreNotices: false,
        events: [],
        notices: [],
        isLoadingMoreReviews: false,
        reviewsNextCursor: nil,
        canLoadMoreReviews: false,
        reviews: [],
        isFavorite: false,
        isLoggedIn: false,
        isVisitVerified: false,
        shouldScrollToTopOnReturn: false,
        currentUserId: nil,
        shouldShowFavoriteTooltip: false
    )

    enum TabType: CaseIterable {
        case info
        case casts
        case menu
        case reviews
        case notices
    }
}
