package com.hhp227.concafe.domain.repository

import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.Comment
import com.hhp227.concafe.domain.model.CommunityPost

interface CommunityPostRepository {
    suspend fun getCommunityPostPage(cursor: String?, pageSize: Int): PagedResult<CommunityPost>
    suspend fun getCommunityPost(postId: String): CommunityPost
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
    suspend fun getComments(postId: String): List<Comment>
    suspend fun addComment(postId: String, userId: String, content: String): Comment
    suspend fun getCommentPage(postId: String, beforeCursor: String?, pageSize: Int): PagedResult<Comment>
    suspend fun updateComment(postId: String, commentId: String, content: String): Comment
    suspend fun deleteComment(postId: String, commentId: String)
}
