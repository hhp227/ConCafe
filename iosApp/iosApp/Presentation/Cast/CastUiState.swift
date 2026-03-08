//
//  CastUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/06.
//

import Foundation
import Shared

struct CastUiState {
    var isLoading: Bool
    var errorMessage: String?
    var detail: CastDetail?
    var recentReviews: [CastRecentReview]
    var isFollowing: Bool
    var isLoggedIn: Bool

    static let empty = CastUiState(
        isLoading: false,
        errorMessage: nil,
        detail: nil,
        recentReviews: [],
        isFollowing: false,
        isLoggedIn: false
    )
}
