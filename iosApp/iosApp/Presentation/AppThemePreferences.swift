//
//  AppThemePreferences.swift
//  ConCafe
//

import Foundation
import SwiftUI

enum AppThemeMode: String, CaseIterable, Identifiable {
    case light
    case dark

    var id: String { rawValue }

    var title: String {
        switch self {
        case .light:
            return String(localized: String.LocalizationValue("settings_theme_light"), table: "Localizable")
        case .dark:
            return String(localized: String.LocalizationValue("settings_theme_dark"), table: "Localizable")
        }
    }

    var colorScheme: ColorScheme {
        switch self {
        case .light:
            return .light
        case .dark:
            return .dark
        }
    }
}

@MainActor
final class AppThemePreferences {
    static let shared = AppThemePreferences()

    private(set) var themeMode: AppThemeMode

    private let themeModeKey = "concafe.theme.mode"
    private var continuations: [UUID: AsyncStream<AppThemeMode>.Continuation] = [:]

    private init() {
        let storedValue = UserDefaults.standard.string(forKey: themeModeKey) ?? ""
        themeMode = AppThemeMode(rawValue: storedValue) ?? .light
    }

    func setThemeMode(_ themeMode: AppThemeMode) {
        UserDefaults.standard.set(themeMode.rawValue, forKey: themeModeKey)
        self.themeMode = themeMode
        continuations.values.forEach { $0.yield(themeMode) }
    }

    func observeThemeMode() -> AsyncStream<AppThemeMode> {
        AsyncStream { continuation in
            let id = UUID()

            continuation.yield(themeMode)
            continuations[id] = continuation
            continuation.onTermination = { [weak self] _ in
                Task { @MainActor in
                    self?.continuations.removeValue(forKey: id)
                }
            }
        }
    }
}
