//
//  MainUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import Foundation
import Shared

struct MainUiState {
    var currentUser: User?
    
    var tabs: [MainNavigationTab] = [
        .home,
        .explore,
        .checkIn,
        .ranking,
        .myInfo
    ]

    var selectedTab: String = MainNavigationTab.home.route

    var thirdTab: MainNavigationTab = .checkIn

    static let empty = MainUiState()
}
