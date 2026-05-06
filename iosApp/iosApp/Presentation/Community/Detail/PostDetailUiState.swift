//
//  PostDetailUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 4/28/26.
//

import Foundation
import Shared

struct PostDetailUiState {
    var post: CommunityPost? = nil
    var isLoading: Bool = true
    var isLiked: Bool = false
    var isOwner: Bool = false
    var currentUserId: String? = nil
    var isMenuVisible: Bool = false
    var isDeleteConfirmVisible: Bool = false
    var isReportSheetVisible: Bool = false
    var reportingCommentId: String? = nil
    var selectedReportType: String? = nil
    var isSubmittingReport: Bool = false
    var isDeleting: Bool = false
    var comments: [Comment] = []
    var isLoadingComments: Bool = false
    var hasMoreComments: Bool = false
    var isLoadingMoreComments: Bool = false
    var oldestCommentCursor: String? = nil
    var editingCommentId: String? = nil
    var editCommentText: String = ""
    var isUpdatingComment: Bool = false
    var commentText: String = ""
    var isSendingComment: Bool = false
    var errorMessage: String? = nil

    var likeCount: Int { Int(post?.likeCount ?? 0) }
    var commentCount: Int { Int(post?.commentCount ?? 0) }
    var canSendComment: Bool { !commentText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && !isSendingComment }
    var canUpdateComment: Bool { !editCommentText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && !isUpdatingComment }
}
