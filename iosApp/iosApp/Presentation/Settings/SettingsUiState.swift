//
//  SettingsUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import Foundation

struct SettingsUiState {
    var isLoading: Bool = false
    var errorMessage: String?

    static let empty = SettingsUiState()
}
