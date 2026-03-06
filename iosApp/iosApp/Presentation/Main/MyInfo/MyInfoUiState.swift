//
//  MyInfoUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/06.
//

import Foundation
import Shared

struct MyInfoUiState {
    var isLoading: Bool
    var errorMessage: String?
    var isLoggedIn: Bool
    var user: User?
    var summary: MyPageSummary?
    var badges: [ProfileBadge]
    var popularCafes: [Cafe]
    var recentVisits: [Cafe]
    var favorites: [Cafe]
    var followedMaids: [Cast]

    static let empty = MyInfoUiState(
        isLoading: false,
        errorMessage: nil,
        isLoggedIn: false,
        user: nil,
        summary: nil,
        badges: [],
        popularCafes: [],
        recentVisits: [],
        favorites: [],
        followedMaids: []
    )
}
