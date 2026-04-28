package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.CommunityPostRemoteDataSource
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.Comment
import com.hhp227.concafe.domain.model.CommunityPost
import com.hhp227.concafe.domain.repository.CommunityPostRepository

class CommunityPostRepositoryImpl(
    private val communityPostRemoteDataSource: CommunityPostRemoteDataSource
) : CommunityPostRepository {
    override suspend fun getCommunityPostPage(cursor: String?, pageSize: Int): PagedResult<CommunityPost> {
        return communityPostRemoteDataSource.fetchCommunityPostPage(cursor, pageSize)
    }

    override suspend fun getCommunityPost(postId: String): CommunityPost {
        return communityPostRemoteDataSource.fetchCommunityPost(postId)
    }

    override suspend fun createCommunityPost(
        userId: String,
        title: String,
        content: String,
        imageUrls: List<String>
    ): CommunityPost {
        return communityPostRemoteDataSource.createCommunityPost(userId, title, content, imageUrls)
    }

    override suspend fun deleteCommunityPost(postId: String) {
        communityPostRemoteDataSource.deleteCommunityPost(postId)
    }

    override suspend fun updateCommunityPost(
        postId: String,
        title: String,
        content: String,
        imageUrls: List<String>
    ): CommunityPost {
        return communityPostRemoteDataSource.updateCommunityPost(postId, title, content, imageUrls)
    }

    override suspend fun isLikedByUser(postId: String, userId: String): Boolean {
        return communityPostRemoteDataSource.isLikedByUser(postId, userId)
    }

    override suspend fun toggleLike(postId: String, userId: String): Boolean {
        return communityPostRemoteDataSource.toggleLike(postId, userId)
    }

    override suspend fun getComments(postId: String): List<Comment> {
        return communityPostRemoteDataSource.fetchComments(postId)
    }

    override suspend fun addComment(postId: String, userId: String, content: String): Comment {
        return communityPostRemoteDataSource.addComment(postId, userId, content)
    }
}
