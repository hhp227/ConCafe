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
    case changeCommentText(String)
    case clickSendComment
    case dismissError
    case clickImage(String)
}
