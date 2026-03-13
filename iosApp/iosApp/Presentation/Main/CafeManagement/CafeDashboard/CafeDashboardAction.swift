//
//  CafeDashboardAction.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/10.
//

import Foundation

enum CafeDashboardAction {
    case clickBack
    case clickShortcut(CafeDashboardShortcut)
    case clickCastSchedule(String)
    case clickApproveCastClaim(String)
    case clickRejectCastClaim(String)
    case clickLoadMoreCasts
    case dismissInfoMessage
}
