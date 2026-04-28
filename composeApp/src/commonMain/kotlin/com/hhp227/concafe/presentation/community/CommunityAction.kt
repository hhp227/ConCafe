package com.hhp227.concafe.presentation.community

sealed interface CommunityAction {
    data object Refresh : CommunityAction
    data object LoadMore : CommunityAction
    data object ClickWritePost : CommunityAction
    data class ClickPost(val postId: String) : CommunityAction
    data object DismissError : CommunityAction
}
