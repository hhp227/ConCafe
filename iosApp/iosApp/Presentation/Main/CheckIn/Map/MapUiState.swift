//
//  MapUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 4/23/26.
//

import Foundation
import Shared

struct MapUiState {
    var isLoading: Bool = false
    var errorMessage: String? = nil
    var isLoggedIn: Bool = false
    var isLoginPromptVisible: Bool = false
    var currentLocationLabel: String = ""
    var userCityKey: String? = nil
    var mapCafes: [CheckInCafeSummary] = []
    var selectedRegion: ExploreUiState.RegionFilter = .all
    var searchQuery: String = ""

    static let empty = MapUiState()
}
