//
//  CastUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/06.
//

import Foundation
import Shared

enum CastAttendanceStatus {
    case upcoming   // 출근 예정
    case onShift    // 출근 중
    case completed  // 근무 완료
    case off        // 비근무
}

struct CastUiState {
    var isLoading: Bool
    var errorMessage: String?
    var detail: CastDetail?
    var recentReviews: [CastRecentReview]
    var isFollowing: Bool
    var isLoggedIn: Bool
    var todayAttendanceStatus: CastAttendanceStatus
    var isSelfCast: Bool

    static let empty = CastUiState(
        isLoading: false,
        errorMessage: nil,
        detail: nil,
        recentReviews: [],
        isFollowing: false,
        isLoggedIn: false,
        todayAttendanceStatus: .off,
        isSelfCast: false
    )
}
