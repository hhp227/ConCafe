//
//  CafeAction.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/06.
//

import Foundation

enum CafeAction {
    case backTapped
    case changeTab(CafeUiState.TabType)
    case maidTapped(id: String)
    case favoriteTapped
    case loadMoreCasts
    case refresh
}
