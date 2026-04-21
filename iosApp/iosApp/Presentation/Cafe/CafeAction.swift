//
//  CafeAction.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/06.
//

import Foundation

enum CafeAction {
    case backTapped
    case changeTab(CafeUiState.TabType)
    case maidTapped(id: String)
    case favoriteTapped
    case writeReviewTapped
    case loadMoreCasts
    case loadMoreNotices
    case loadMoreReviews
    case refresh
    case pagingTriggerDisappeared(CafeUiState.TabType)
    case consumeScrollToTopOnReturn
    case editReview(reviewId: String)
    case deleteReview(reviewId: String)
    case reviewImageTapped(imageUrl: String)
    case reportReview(reviewId: String)
}
