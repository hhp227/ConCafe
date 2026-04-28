package com.hhp227.concafe.presentation.community

sealed interface CommunityEvent {
    data object NavigateToPostEdit : CommunityEvent
    data class NavigateToPost(val postId: String) : CommunityEvent
}
