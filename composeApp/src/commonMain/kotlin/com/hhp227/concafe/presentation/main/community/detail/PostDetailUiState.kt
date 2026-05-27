package com.hhp227.concafe.presentation.main.community.detail

import com.hhp227.concafe.domain.model.Comment
import com.hhp227.concafe.domain.model.CommunityPost

data class PostDetailUiState(
    val post: CommunityPost? = null,
    val isLoading: Boolean = false,
    val isLiked: Boolean = false,
    val isOwner: Boolean = false,
    val currentUserId: String? = null,
    val isMenuVisible: Boolean = false,
    val isDeleteConfirmVisible: Boolean = false,
    val isReportSheetVisible: Boolean = false,
    val reportingCommentId: String? = null,
    val selectedReportType: String? = null,
    val isSubmittingReport: Boolean = false,
    val isBlockingUser: Boolean = false,
    val blockedUserIds: Set<String> = emptySet(),
    val isDeleting: Boolean = false,
    val comments: List<Comment> = emptyList(),
    val isLoadingComments: Boolean = false,
    val hasMoreComments: Boolean = false,
    val isLoadingMoreComments: Boolean = false,
    val oldestCommentCursor: String? = null,
    val editingCommentId: String? = null,
    val editCommentText: String = "",
    val isUpdatingComment: Boolean = false,
    val commentText: String = "",
    val isSendingComment: Boolean = false,
    val errorMessage: String? = null
) {
    val likeCount: Int get() = post?.likeCount ?: 0
    val commentCount: Int get() = post?.commentCount ?: 0
    val canSendComment: Boolean get() = commentText.isNotBlank() && !isSendingComment
    val canUpdateComment: Boolean get() = editCommentText.isNotBlank() && !isUpdatingComment
}
