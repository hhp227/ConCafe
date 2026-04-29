package com.hhp227.concafe.presentation.main.fanmanagement

sealed interface FanManagementAction {
    data object ClickClaimProfile : FanManagementAction
    data object LoadMoreClaimCandidates : FanManagementAction
    data class SelectClaimCandidate(val castId: String) : FanManagementAction
    data object SubmitCastClaim : FanManagementAction
    data object DismissClaimSheet : FanManagementAction
    data object ClickEditProfile : FanManagementAction
    data object ClickPrimaryAnnouncement : FanManagementAction
    data class ChangeAnnouncementTitle(val value: String) : FanManagementAction
    data class ChangeAnnouncementBody(val value: String) : FanManagementAction
    data object SubmitAnnouncement : FanManagementAction
    data object DismissAnnouncementSheet : FanManagementAction
    data class ClickQuickAction(val quickAction: FanManagementUiState.QuickAction) : FanManagementAction
    data object ClickViewAllFollowers : FanManagementAction
    data class ClickRecentFollower(val followerId: String) : FanManagementAction
    data class ClickTopFan(val fanId: String) : FanManagementAction
    data object DismissInfoMessage : FanManagementAction
}
