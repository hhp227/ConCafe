package com.hhp227.concafe.data.source

import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.Comment
import com.hhp227.concafe.domain.model.CommunityPost

interface CommunityPostRemoteDataSource {
    suspend fun fetchCommunityPostPage(cursor: String?, pageSize: Int): PagedResult<CommunityPost>
    suspend fun fetchCommunityPost(postId: String): CommunityPost
    suspend fun createCommunityPost(
        userId: String,
        title: String,
        content: String,
        imageUrls: List<String>
    ): CommunityPost
    suspend fun updateCommunityPost(
        postId: String,
        title: String,
        content: String,
        imageUrls: List<String>
    ): CommunityPost
    suspend fun deleteCommunityPost(postId: String)
    suspend fun isLikedByUser(postId: String, userId: String): Boolean
    suspend fun toggleLike(postId: String, userId: String): Boolean
    suspend fun fetchComments(postId: String): List<Comment>
    suspend fun addComment(postId: String, userId: String, content: String): Comment
}
