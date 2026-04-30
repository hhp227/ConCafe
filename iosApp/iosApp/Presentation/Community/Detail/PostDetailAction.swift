//
//  PostDetailAction.swift
//  ConCafe
//
//  Created by 홍희표 on 4/28/26.
//

import Foundation

enum PostDetailAction {
    case clickBack
    case clickLike
    case clickMoreMenu
    case dismissMoreMenu
    case clickEdit
    case clickDelete
    case confirmDelete
    case dismissDeleteConfirm
    case clickReport
    case clickEditComment(commentId: String)
    case confirmEditComment(content: String)
    case dismissEditComment
    case clickDeleteComment(commentId: String)
    case clickReportComment(commentId: String)
    case changeCommentText(String)
    case clickSendComment
    case dismissError
    case clickImage(String)
    case loadMoreComments
}
