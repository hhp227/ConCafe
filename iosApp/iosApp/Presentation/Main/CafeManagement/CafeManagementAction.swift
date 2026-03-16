//
//  CafeManagementAction.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import Foundation

enum CafeManagementAction {
    case clickCafe(String)
    case clickCafeDetail(String)
    case changeCafeSearchQuery(String)
    case clickClaimCafe(String)
    case toggleCafeListExpanded
    case clickCreateCafe
    case dismissInfoMessage
}
