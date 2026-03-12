//
//  ExploreAction.swift
//  iosApp
//
//  Created by 홍희표 on 2026/03/06.
//

import Foundation

enum ExploreAction {
    case queryChanged(String)
    case regionChanged(ExploreUiState.RegionFilter)
    case sortChanged(ExploreUiState.SortFilter)
    case tabChanged(ExploreUiState.TabType)
    case cafeTapped(id: String)
    case maidTapped(id: String)
    case refresh
}
