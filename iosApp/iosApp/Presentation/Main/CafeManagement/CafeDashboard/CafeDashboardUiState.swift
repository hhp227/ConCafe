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
    var externalLinks: [CafeDashboardExternalLink] = []
    var selectedCastId: String?
    var nextCastCursor: String?
    var hasMoreCasts = false
    var isLoadingMoreCasts = false
    var isDeleteCastDialogVisible = false
    var isExternalLinkSheetVisible = false
    var editingExternalLinkId: String?
    var externalLinkTitle = ""
    var externalLinkUrl = ""
    var instagramId = ""
    var twitterId = ""
    var tiktokId = ""
    var youtubeId = ""
    var isSavingSocialMedia = false
    var isSocialMediaSheetVisible = false
    var reservationUrl = ""
    var isReservationSheetVisible = false
    var isSavingReservation = false
    var isLoading = true
    var infoMessage: String?

    var isExternalLinkSubmitEnabled: Bool {
        !externalLinkTitle.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
        !externalLinkUrl.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }
}

struct CafeDashboardExternalLink: Identifiable {
    let id: String
    let title: String
    let url: String
}
