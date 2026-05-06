//
//  AppThemeMode.swift
//  ConCafe
//
//  Created by 홍희표 on 5/6/26.
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
