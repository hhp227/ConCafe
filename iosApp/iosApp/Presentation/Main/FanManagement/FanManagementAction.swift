//
//  FanManagementAction.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import Foundation

enum FanManagementAction {
    case refresh
    case clickClaimProfile
    case loadMoreClaimCandidates
    case selectClaimCandidate(String)
    case submitCastClaim
    case dismissClaimSheet
    case clickEditProfile
    case clickPrimaryAnnouncement
    case clickQuickAction(FanManagementUiState.QuickAction)
    case clickViewAllFollowers
    case clickRecentFollower(id: String)
    case clickTopFan(id: String)
    case dismissInfoMessage
}
