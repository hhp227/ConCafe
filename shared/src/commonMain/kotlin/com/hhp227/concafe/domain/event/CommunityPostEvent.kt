package com.hhp227.concafe.domain.event

sealed class CommunityPostEvent {
    data class Created(val postId: String) : CommunityPostEvent()
}
