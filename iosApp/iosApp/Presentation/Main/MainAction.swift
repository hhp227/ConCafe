//
//  MainAction.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import Foundation

enum MainAction {
    case enter(preferredRoute: String? = nil)
    case refreshNavigation(preferredRoute: String? = nil)
    case selectTab(route: String)
}
