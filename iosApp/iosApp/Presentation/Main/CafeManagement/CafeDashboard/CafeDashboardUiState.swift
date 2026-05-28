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
    var guestSchedules: [GuestCastSchedule] = []
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
    var isTableCountSheetVisible = false
    var isGuestSheetVisible = false
    var isGuestSaving = false
    var guestName = ""
    var guestProfileImage = ""
    var guestDate = ""
    var guestStartTime = CafeDashboardUiState.defaultGuestStartTime
    var guestEndTime = CafeDashboardUiState.defaultGuestEndTime
    var guestMemo = ""
    var guestDateOptions: [String] = []
    var guestTimeOptions: [String] = CafeDashboardUiState.defaultGuestTimeOptions()
    var currentTableCountInput = ""
    var totalTableCountInput = ""
    var isSavingTableCounts = false
    var isLoading = true
    var infoMessage: String?

    var isExternalLinkSubmitEnabled: Bool {
        !externalLinkTitle.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
        !externalLinkUrl.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    var isTableCountSubmitEnabled: Bool {
        guard let current = Int(currentTableCountInput), let total = Int(totalTableCountInput) else { return false }
        return total >= 0 && current >= 0 && current <= total
    }

    var isGuestSubmitEnabled: Bool {
        !guestName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
        !guestDate.isEmpty &&
        guestStartTime < guestEndTime
    }

    static let defaultGuestStartTime = "14:00"

    static let defaultGuestEndTime = "22:00"

    static func defaultGuestTimeOptions() -> [String] {
        (0...23).flatMap { hour in
            ["00", "30"].map { minute in String(format: "%02d:%@", hour, minute) }
        }
    }
}

struct CafeDashboardExternalLink: Identifiable {
    let id: String
    let title: String
    let url: String
}
