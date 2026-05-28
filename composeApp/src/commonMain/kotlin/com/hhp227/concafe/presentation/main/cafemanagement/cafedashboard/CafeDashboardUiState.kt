package com.hhp227.concafe.presentation.main.cafemanagement.cafedashboard

import com.hhp227.concafe.domain.model.CafeCastPreview
import com.hhp227.concafe.domain.model.CafeDashboardData
import com.hhp227.concafe.domain.model.GuestCastSchedule
import com.hhp227.concafe.domain.model.PendingCastClaimPreview

data class CafeDashboardUiState(
    val cafe: CafeDashboardData? = null,
    val castPreviews: List<CafeCastPreview> = emptyList(),
    val pendingCastClaims: List<PendingCastClaimPreview> = emptyList(),
    val guestSchedules: List<GuestCastSchedule> = emptyList(),
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
    val isTableCountSheetVisible: Boolean = false,
    val isGuestSheetVisible: Boolean = false,
    val isGuestSaving: Boolean = false,
    val guestName: String = "",
    val guestProfileImage: String = "",
    val guestDate: String = "",
    val guestStartTime: String = DEFAULT_GUEST_START_TIME,
    val guestEndTime: String = DEFAULT_GUEST_END_TIME,
    val guestMemo: String = "",
    val guestDateOptions: List<String> = emptyList(),
    val guestTimeOptions: List<String> = defaultGuestTimeOptions(),
    val currentTableCountInput: String = "",
    val totalTableCountInput: String = "",
    val isSavingTableCounts: Boolean = false,
    val isLoading: Boolean = true,
    val infoMessage: String? = null
) {
    val isExternalLinkSubmitEnabled: Boolean
        get() = externalLinkTitle.isNotBlank() && externalLinkUrl.isNotBlank()

    val isTableCountSubmitEnabled: Boolean
        get() {
            val current = currentTableCountInput.toIntOrNull() ?: return false
            val total = totalTableCountInput.toIntOrNull() ?: return false
            return total >= 0 && current >= 0 && current <= total
        }

    val isGuestSubmitEnabled: Boolean
        get() = guestName.isNotBlank() && guestDate.isNotBlank() && guestStartTime < guestEndTime

    companion object {
        const val DEFAULT_GUEST_START_TIME = "14:00"
        const val DEFAULT_GUEST_END_TIME = "22:00"
    }
}

data class CafeDashboardExternalLink(
    val id: String,
    val title: String,
    val url: String
)

private fun defaultGuestTimeOptions(): List<String> {
    return (0..23).flatMap { hour ->
        listOf("00", "30").map { minute -> hour.toString().padStart(2, '0') + ":$minute" }
    }
}
