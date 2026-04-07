package com.hhp227.concafe.presentation.main.cafemanagement.cafedashboard

import com.hhp227.concafe.domain.model.CafeCastPreview
import com.hhp227.concafe.domain.model.CafeDashboardData
import com.hhp227.concafe.domain.model.PendingCastClaimPreview

data class CafeDashboardUiState(
    val cafe: CafeDashboardData? = null,
    val castPreviews: List<CafeCastPreview> = emptyList(),
    val pendingCastClaims: List<PendingCastClaimPreview> = emptyList(),
    val externalLinks: List<CafeDashboardExternalLink> = emptyList(),
    val selectedCastId: String? = null,
    val nextCastCursor: String? = null,
    val hasMoreCasts: Boolean = false,
    val isLoadingMoreCasts: Boolean = false,
    val isDeleteCastDialogVisible: Boolean = false,
    val isExternalLinkSheetVisible: Boolean = false,
    val editingExternalLinkId: String? = null,
    val externalLinkTitle: String = "",
    val externalLinkUrl: String = "",
    val instagramId: String = "",
    val twitterId: String = "",
    val tiktokId: String = "",
    val youtubeId: String = "",
    val isSavingSocialMedia: Boolean = false,
    val isSocialMediaSheetVisible: Boolean = false,
    val reservationUrl: String = "",
    val isReservationSheetVisible: Boolean = false,
    val isSavingReservation: Boolean = false,
    val isLoading: Boolean = true,
    val infoMessage: String? = null
) {
    val isExternalLinkSubmitEnabled: Boolean
        get() = externalLinkTitle.isNotBlank() && externalLinkUrl.isNotBlank()
}

data class CafeDashboardExternalLink(
    val id: String,
    val title: String,
    val url: String
)
