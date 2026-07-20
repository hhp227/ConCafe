//
//  CastListAction.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/07/05.
//

import Foundation

enum CastListAction {
    case clickBack
    case clickAddCast
    case changeSearchQuery(String)
    case clickCast(String)
    case clickCastSchedule(String)
    case clickDeleteCast(String)
    case confirmDeleteCast
    case dismissDeleteCastDialog
    case clickLoadMoreCasts
    case dismissInfoMessage
}
