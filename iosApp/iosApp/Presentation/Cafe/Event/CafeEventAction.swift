//
//  CafeEventAction.swift
//  ConCafe
//
//  Created by 홍희표 on 4/23/26.
//

import Foundation

enum CafeEventAction {
    case backTapped
    case retry
    case toggleLike
    case goToCafe
    case castTapped(id: String)
    case loginPromptSignInTapped
    case dismissLoginPrompt
}
