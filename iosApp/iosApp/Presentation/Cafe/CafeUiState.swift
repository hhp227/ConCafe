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
    var errorMessage: String?
    var selectedTab: TabType
    var detail: CafeDetail?
    var casts: [CafeDetailCast]
    var reviews: [CafeDetailReview]
    var isFavorite: Bool
    var isLoggedIn: Bool

    static let empty = CafeUiState(
        isLoading: false,
        errorMessage: nil,
        selectedTab: .info,
        detail: nil,
        casts: [],
        reviews: [],
        isFavorite: false,
        isLoggedIn: false
    )

    enum TabType: String, CaseIterable {
        case info = "정보"
        case maids = "메이드"
        case menu = "메뉴"
        case reviews = "리뷰"
        case notices = "공지"
    }
}
