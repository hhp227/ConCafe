//
//  CafeManagementUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import Foundation
import Shared

struct CafeManagementUiState {
    var ownedCafes: [CafeManagementData.OwnedCafeSummary] = []
    var searchableCafes: [CafeManagementData.SearchableCafeSummary] = []
    var pendingClaims: [CafeManagementData.PendingClaimSummary] = []
    var isLoading = true
    var isShowingAllCafes = false
    var cafeSearchQuery = ""
    var infoMessage: String?

    var featuredCafe: CafeManagementData.OwnedCafeSummary? {
        ownedCafes.first
    }

    var hasOwnedCafes: Bool {
        !ownedCafes.isEmpty
    }

    var visibleOwnedCafes: [CafeManagementData.OwnedCafeSummary] {
        isShowingAllCafes ? ownedCafes : Array(ownedCafes.prefix(Self.defaultVisibleCafeCount))
    }

    var hasHiddenOwnedCafes: Bool {
        ownedCafes.count > Self.defaultVisibleCafeCount
    }

    var filteredSearchableCafes: [CafeManagementData.SearchableCafeSummary] {
        searchableCafes.filter { cafe in
            cafeSearchQuery.isEmpty ||
            cafe.name.localizedCaseInsensitiveContains(cafeSearchQuery) ||
            cafe.location.localizedCaseInsensitiveContains(cafeSearchQuery)
        }
    }

    private static let defaultVisibleCafeCount = 2
}
