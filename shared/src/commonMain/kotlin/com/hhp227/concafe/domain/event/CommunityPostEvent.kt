package com.hhp227.concafe.domain.event

import com.hhp227.concafe.domain.model.CommunityPost

sealed class CommunityPostEvent {
    data class Created(val postId: String) : CommunityPostEvent()
    data class Deleted(val postId: String) : CommunityPostEvent()
    data class Updated(val post: CommunityPost) : CommunityPostEvent()
}
