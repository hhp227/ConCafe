//
//  HomeUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/06.
//

import Foundation

struct HomeUiState {
    let banners: [HomeBanner]
    let popularMaids: [PopularMaid]
    let nearbyCafes: [NearbyCafe]
    let birthdayMaids: [BirthdayMaid]
    let notices: [NoticeItem]

    static let empty = HomeUiState(
        banners: [],
        popularMaids: [],
        nearbyCafes: [],
        birthdayMaids: [],
        notices: []
    )
}
