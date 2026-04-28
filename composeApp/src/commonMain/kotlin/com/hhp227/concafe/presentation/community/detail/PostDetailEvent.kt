package com.hhp227.concafe.presentation.community.detail

sealed interface PostDetailEvent {
    data object NavigateBack : PostDetailEvent
    data class NavigateToPicture(val imageUrl: String) : PostDetailEvent
}
