//
//  RankingAction.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/06.
//

import Foundation
import Shared

enum RankingAction {
    case changeTab(RankingUiState.TabType)
    case changePeriod(RankingPeriod)
    case changeRegion(RankingUiState.RegionFilter)
    case selectAd(Int)
    case tapMaid(String)
    case tapCafe(String)
    case loginPromptSignInTapped
    case dismissLoginPrompt
    case updateBannerHeight(CGFloat)
}
