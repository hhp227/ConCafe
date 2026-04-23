//
//  MapAction.swift
//  ConCafe
//
//  Created by 홍희표 on 4/23/26.
//

import Foundation

enum MapAction {
    case backTapped
    case cafeTapped(id: String)
    case loginPromptSignInTapped
    case dismissLoginPrompt
    case regionChanged(region: ExploreUiState.RegionFilter)
    case searchQueryChanged(query: String)
}
