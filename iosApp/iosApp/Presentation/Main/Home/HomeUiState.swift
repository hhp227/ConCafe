//
//  HomeUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/06.
//

import Foundation
import Shared

struct HomeUiState {
    let banners: [HomeBanner]
    let popularCasts: [Cast]
    let nearbyCafes: [Cafe]
    let nearbyCafeCursor: String?
    let canLoadMoreNearbyCafes: Bool
    let birthdayCasts: [Cast]
    let notices: [Notice]

    static let empty = HomeUiState(
        banners: [],
        popularCasts: [],
        nearbyCafes: [],
        nearbyCafeCursor: nil,
        canLoadMoreNearbyCafes: false,
        birthdayCasts: [],
        notices: []
    )
}
