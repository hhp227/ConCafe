//
//  HomeUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/06.
//

import Foundation
import Shared

struct HomeUiState {
    let isLoggedIn: Bool = false
    let isLoginPromptVisible: Bool = false
    let banners: [HomeBanner]
    let popularCasts: [Cast]
    let popularCastCafeNames: [String: String]
    let popularCastCursor: String?
    let canLoadMorePopularCasts: Bool
    let isLoadingMorePopularCasts: Bool
    let nearbyCafes: [Cafe]
    let nearbyCafeCursor: String?
    let canLoadMoreNearbyCafes: Bool
    let isLoadingMoreNearbyCafes: Bool
    let birthdayCasts: [Cast]
    let notices: [Notice]

    static let empty = HomeUiState(
        banners: [],
        popularCasts: [],
        popularCastCafeNames: [:],
        popularCastCursor: nil,
        canLoadMorePopularCasts: false,
        isLoadingMorePopularCasts: false,
        nearbyCafes: [],
        nearbyCafeCursor: nil,
        canLoadMoreNearbyCafes: false,
        isLoadingMoreNearbyCafes: false,
        birthdayCasts: [],
        notices: []
    )
}
