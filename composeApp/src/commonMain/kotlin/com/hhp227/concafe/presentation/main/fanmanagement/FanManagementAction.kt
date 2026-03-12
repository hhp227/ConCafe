package com.hhp227.concafe.presentation.main.fanmanagement

sealed interface FanManagementAction {
    data object ClickEditProfile : FanManagementAction
    data object ClickPrimaryAnnouncement : FanManagementAction
    data class ClickQuickAction(val quickAction: FanManagementUiState.QuickAction) : FanManagementAction
    data object ClickViewAllFollowers : FanManagementAction
    data class ClickRecentFollower(val followerId: String) : FanManagementAction
    data class ClickTopFan(val fanId: String) : FanManagementAction
    data object DismissInfoMessage : FanManagementAction
}
