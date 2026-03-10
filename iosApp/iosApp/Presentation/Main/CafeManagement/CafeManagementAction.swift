//
//  CafeManagementAction.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import Foundation

enum CafeManagementAction {
    case selectCafe(String)
    case shortcutTapped(CafeManagementUiState.Shortcut)
    case searchCafeTapped
    case createCafeTapped
    case dismissInfoMessageTapped
}
