//
//  AppBrandTheme.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/07/08.
//

import Foundation
import Shared
import SwiftUI

enum AppBrandTheme: String, CaseIterable, Identifiable {
    case maidCafe
    case mensConCafe

    var id: String { rawValue }

    var title: String {
        switch self {
        case .maidCafe:
            return String(localized: String.LocalizationValue("settings_brand_theme_maid"), table: "Localizable")
        case .mensConCafe:
            return String(localized: String.LocalizationValue("settings_brand_theme_mens"), table: "Localizable")
        }
    }

    init(brandTheme: BrandTheme) {
        switch brandTheme {
        case .mensConCafe:
            self = .mensConCafe
        default:
            self = .maidCafe
        }
    }

    var sharedBrandTheme: BrandTheme {
        switch self {
        case .maidCafe:
            return .maidCafe
        case .mensConCafe:
            return .mensConCafe
        }
    }
}
