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
    case birthdayMaidTapped(id: String)
    case loadMorePopularCasts
    case loadMoreNearbyCafes
}
