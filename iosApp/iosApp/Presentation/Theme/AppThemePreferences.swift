//
//  AppThemePreferences.swift
//  ConCafe
//

import Foundation
import SwiftUI

@MainActor
final class AppThemePreferences {
    static let shared = AppThemePreferences()

    private(set) var themeMode: AppThemeMode

    private let themeModeKey = "concafe.theme.mode"
    
    private var continuations: [UUID: AsyncStream<AppThemeMode>.Continuation] = [:]

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
    
    private init() {
        let storedValue = UserDefaults.standard.string(forKey: themeModeKey) ?? ""
        themeMode = AppThemeMode(rawValue: storedValue) ?? .light
    }
}
