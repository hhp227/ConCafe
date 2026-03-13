//
//  CafeDashboardUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/10.
//

import Foundation
import Shared

struct CafeDashboardUiState {
    var cafe: CafeDashboardData?
    var castPreviews: [CafeCastPreview] = []
    var pendingCastClaims: [PendingCastClaimPreview] = []
    var selectedCastId: String?
    var nextCastCursor: String?
    var hasMoreCasts = false
    var isLoadingMoreCasts = false
    var isLoading = true
    var infoMessage: String?
}
