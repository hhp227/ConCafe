package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.CommunityPostRemoteDataSource
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.CommunityPost
import com.hhp227.concafe.domain.repository.CommunityPostRepository

class CommunityPostRepositoryImpl(
    private val communityPostRemoteDataSource: CommunityPostRemoteDataSource
) : CommunityPostRepository {
    override suspend fun getCommunityPostPage(cursor: String?, pageSize: Int): PagedResult<CommunityPost> {
        return communityPostRemoteDataSource.fetchCommunityPostPage(cursor, pageSize)
    }

    override suspend fun createCommunityPost(
        userId: String,
        title: String,
        content: String,
        imageUrls: List<String>
    ): CommunityPost {
        return communityPostRemoteDataSource.createCommunityPost(userId, title, content, imageUrls)
    }
}
