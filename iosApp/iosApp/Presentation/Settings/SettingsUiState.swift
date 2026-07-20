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
    var appVersion: String = Self.resolvedAppVersion()
    var themeMode: AppThemeMode = .light
    var brandTheme: AppBrandTheme = .maidCafe

    static let empty = SettingsUiState()

    private static func resolvedAppVersion() -> String {
        guard let version = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String else {
            preconditionFailure("CFBundleShortVersionString is missing")
        }
        let trimmedVersion = version.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedVersion.isEmpty else {
            preconditionFailure("CFBundleShortVersionString is empty")
        }
        return trimmedVersion
    }
}
