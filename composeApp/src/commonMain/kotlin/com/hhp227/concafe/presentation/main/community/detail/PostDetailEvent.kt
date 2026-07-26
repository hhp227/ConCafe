package com.hhp227.concafe.presentation.main.community.detail

sealed interface PostDetailEvent {
    data object NavigateBack : PostDetailEvent
    data class NavigateToPicture(val imageUrl: String) : PostDetailEvent
    data class NavigateToPostEdit(val postId: String) : PostDetailEvent
}
