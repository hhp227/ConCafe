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
    var appVersion: String = "1.0.0"

    static let empty = SettingsUiState()
}
