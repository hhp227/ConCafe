package com.hhp227.concafe.presentation.main.cafemanagement.cafedashboard

sealed interface CafeDashboardAction {
    data object ClickBack : CafeDashboardAction
    data class ClickShortcut(val shortcut: CafeDashboardShortcut) : CafeDashboardAction
    data object ClickCreateBanner : CafeDashboardAction
    data object DismissExternalLinkSheet : CafeDashboardAction
    data class ChangeExternalLinkTitle(val value: String) : CafeDashboardAction
    data class ChangeExternalLinkUrl(val value: String) : CafeDashboardAction
    data object SubmitExternalLink : CafeDashboardAction
    data class ClickExternalLinkItem(val linkId: String) : CafeDashboardAction
    data class ClickEditExternalLink(val linkId: String) : CafeDashboardAction
    data class ClickDeleteExternalLink(val linkId: String) : CafeDashboardAction
    data class ClickCastSchedule(val castId: String) : CafeDashboardAction
    data object ClickDeleteCast : CafeDashboardAction
    data object ConfirmDeleteCast : CafeDashboardAction
    data object DismissDeleteCastDialog : CafeDashboardAction
    data class ClickApproveCastClaim(val claimId: String) : CafeDashboardAction
    data class ClickRejectCastClaim(val claimId: String) : CafeDashboardAction
    data object ClickLoadMoreCasts : CafeDashboardAction
    data object DismissInfoMessage : CafeDashboardAction
    data class ChangeSocialMediaInstagram(val value: String) : CafeDashboardAction
    data class ChangeSocialMediaTwitter(val value: String) : CafeDashboardAction
    data class ChangeSocialMediaTiktok(val value: String) : CafeDashboardAction
    data class ChangeSocialMediaYoutube(val value: String) : CafeDashboardAction
    data object SubmitSocialMedia : CafeDashboardAction
    data object DismissSocialMediaSheet : CafeDashboardAction
    data object DismissReservationSheet : CafeDashboardAction
    data class ChangeReservationUrl(val value: String) : CafeDashboardAction
    data object SubmitReservation : CafeDashboardAction
    data object ClickTableCountMetric : CafeDashboardAction
    data object DismissTableCountSheet : CafeDashboardAction
    data class ChangeCurrentTableCount(val value: String) : CafeDashboardAction
    data class ChangeTotalTableCount(val value: String) : CafeDashboardAction
    data object SubmitTableCounts : CafeDashboardAction
    data object ClickAddGuest : CafeDashboardAction
    data object DismissGuestSheet : CafeDashboardAction
    data class ChangeGuestName(val value: String) : CafeDashboardAction
    data class ChangeGuestProfileImage(val value: String) : CafeDashboardAction
    data class ChangeGuestDate(val value: String) : CafeDashboardAction
    data class ChangeGuestStartTime(val value: String) : CafeDashboardAction
    data class ChangeGuestEndTime(val value: String) : CafeDashboardAction
    data class ChangeGuestMemo(val value: String) : CafeDashboardAction
    data object SubmitGuest : CafeDashboardAction
    data class DeleteGuest(val scheduleId: String) : CafeDashboardAction
}
