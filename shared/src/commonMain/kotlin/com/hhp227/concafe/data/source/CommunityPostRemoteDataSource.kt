package com.hhp227.concafe.data.source

import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.CommunityPost

interface CommunityPostRemoteDataSource {
    suspend fun fetchCommunityPostPage(cursor: String?, pageSize: Int): PagedResult<CommunityPost>
    suspend fun createCommunityPost(
        userId: String,
        title: String,
        content: String,
        imageUrls: List<String>
    ): CommunityPost
}
