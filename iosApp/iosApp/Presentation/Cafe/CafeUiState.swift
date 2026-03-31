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
    var isLoadingMoreNotices: Bool
    var noticesNextCursor: String?
    var canLoadMoreNotices: Bool
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

    static let empty = CafeUiState(
        isLoading: false,
        isLoadingMoreCasts: false,
        errorMessage: nil,
        selectedTab: .info,
        detail: nil,
        casts: [],
        castsNextCursor: nil,
        canLoadMoreCasts: false,
        isLoadingMoreNotices: false,
        noticesNextCursor: nil,
        canLoadMoreNotices: false,
        notices: [],
        isLoadingMoreReviews: false,
        reviewsNextCursor: nil,
        canLoadMoreReviews: false,
        reviews: [],
        isFavorite: false,
        isLoggedIn: false,
        isVisitVerified: false,
        shouldScrollToTopOnReturn: false,
        currentUserId: nil
    )

    enum TabType: String, CaseIterable {
        case info = "정보"
        case casts = "캐스트"
        case menu = "메뉴"
        case reviews = "리뷰"
        case notices = "공지"
    }
}
