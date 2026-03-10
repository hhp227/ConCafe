//
//  CafeManagementUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import Foundation
import Shared

struct CafeManagementUiState {
    var ownedCafes: [CafeManagementDataOwnedCafeSummary] = []

    var searchableCafes: [CafeManagementDataSearchableCafeSummary] = []

    var pendingClaims: [CafeManagementDataPendingClaimSummary] = []

    var isShowingAllCafes = false

    var cafeSearchQuery = ""

    var infoMessage: String?

    var featuredCafe: CafeManagementDataOwnedCafeSummary? {
        ownedCafes.first
    }

    var hasOwnedCafes: Bool {
        !ownedCafes.isEmpty
    }

    var visibleOwnedCafes: [CafeManagementDataOwnedCafeSummary] {
        isShowingAllCafes ? ownedCafes : Array(ownedCafes.prefix(Self.defaultVisibleCafeCount))
    }

    var hasHiddenOwnedCafes: Bool {
        ownedCafes.count > Self.defaultVisibleCafeCount
    }

    var filteredSearchableCafes: [CafeManagementDataSearchableCafeSummary] {
        searchableCafes.filter { cafe in
            cafeSearchQuery.isEmpty ||
            cafe.name.localizedCaseInsensitiveContains(cafeSearchQuery) ||
            cafe.location.localizedCaseInsensitiveContains(cafeSearchQuery)
        }
    }

    private static let defaultVisibleCafeCount = 2
}
