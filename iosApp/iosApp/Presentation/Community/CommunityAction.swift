//
//  CommunityAction.swift
//  ConCafe
//
//  Created by 홍희표 on 4/27/26.
//

import Foundation

enum CommunityAction {
    case refresh
    case loadMore
    case clickWritePost
    case clickPost(postId: String)
    case dismissError
}
