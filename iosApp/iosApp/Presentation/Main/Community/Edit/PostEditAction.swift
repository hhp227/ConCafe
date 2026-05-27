//
//  PostEditAction.swift
//  ConCafe
//
//  Created by 홍희표 on 4/27/26.
//

import Foundation

enum PostEditAction: Equatable {
    case clickBack
    case changeTitle(String)
    case changeContent(String)
    case clickAddImage
    case addImage(String)
    case removeImage(Int)
    case clickSubmit
    case dismissInfoMessage
}
