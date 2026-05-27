//
//  PostDetailEvent.swift
//  ConCafe
//
//  Created by 홍희표 on 4/28/26.
//

import Foundation

enum PostDetailViewEvent {
    case navigateBack
    case navigateToPicture(imageUrl: String)
    case navigateToPostEdit(postId: String)
}
