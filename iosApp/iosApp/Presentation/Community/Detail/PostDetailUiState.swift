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
    var isMenuVisible: Bool = false
    var isDeleteConfirmVisible: Bool = false
    var isDeleting: Bool = false
    var comments: [Comment] = []
    var isLoadingComments: Bool = false
    var commentText: String = ""
    var isSendingComment: Bool = false
    var errorMessage: String? = nil

    var likeCount: Int { Int(post?.likeCount ?? 0) }
    var commentCount: Int { Int(post?.commentCount ?? 0) }
    var canSendComment: Bool { !commentText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && !isSendingComment }
}
