//
//  HomeAction.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/06.
//

import Foundation
import Shared

enum HomeAction {
    case bannerTapped(HomeBanner)
    case maidTapped(id: String)
    case cafeTapped(id: String)
    case cafeEventTapped(cafeId: String, eventId: String)
    case birthdayMaidTapped(id: String)
    case loginPromptSignInTapped
    case dismissLoginPrompt
    case loadMorePopularCasts
    case loadMoreNearbyCafes
    case communityTapped
    case communityPostTapped(postId: String)
}
