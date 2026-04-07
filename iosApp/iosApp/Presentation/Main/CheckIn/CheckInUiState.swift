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
    var userCityKey: String?
    var mapCafes: [CheckInCafeSummary]
    var popularCafes: [CheckInCafeSummary]
    var popularCasts: [CheckInCastSummary]
    var todayVisits: [CheckInVisitEntry]
    var recentVisits: [CheckInVisitEntry]
    var recentVisitsNextCursor: String?
    var canLoadMoreRecentVisits: Bool
    var isLoadingMoreRecentVisits: Bool
    var isLoginPromptVisible: Bool
    var isNewVisitSheetVisible: Bool
    var reviewPrompt: ReviewPrompt?

    static let empty = CheckInUiState(
        isLoading: false,
        errorMessage: nil,
        currentUser: nil,
        currentLocationLabel: "",
        userCityKey: nil,
        mapCafes: [],
        popularCafes: [],
        popularCasts: [],
        todayVisits: [],
        recentVisits: [],
        recentVisitsNextCursor: nil,
        canLoadMoreRecentVisits: false,
        isLoadingMoreRecentVisits: false,
        isLoginPromptVisible: false,
        isNewVisitSheetVisible: false,
        reviewPrompt: nil
    )

    struct ReviewPrompt {
        let visitId: String
        let cafeId: String
        let cafeName: String
    }
}
