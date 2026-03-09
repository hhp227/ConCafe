//
//  CheckInUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/06.
//

import Foundation
import Shared

struct CheckInUiState {
    var isLoading: Bool
    var errorMessage: String?
    var currentUser: User?
    var currentLocationLabel: String
    var mapCafes: [CheckInCafeSummary]
    var popularCafes: [CheckInCafeSummary]
    var popularCasts: [CheckInCastSummary]
    var todayVisits: [CheckInVisitEntry]
    var recentVisits: [CheckInVisitEntry]
    var isLoginPromptVisible: Bool

    static let empty = CheckInUiState(
        isLoading: false,
        errorMessage: nil,
        currentUser: nil,
        currentLocationLabel: "",
        mapCafes: [],
        popularCafes: [],
        popularCasts: [],
        todayVisits: [],
        recentVisits: [],
        isLoginPromptVisible: false
    )
}
