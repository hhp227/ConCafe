//
//  CafeRegionLabel.swift
//  ConCafe
//
//  Created by 홍희표 on 7/20/26.
//

import Foundation

func localizedRegionCity(_ rawCity: String) -> String {
    let normalized = rawCity.trimmingCharacters(in: .whitespacesAndNewlines)
    guard !normalized.isEmpty else {
        return normalized
    }
    switch normalized.lowercased() {
    case "seoul":
        return String(localized: String.LocalizationValue("region_seoul"), table: "Localizable")
    case "busan":
        return String(localized: String.LocalizationValue("region_busan"), table: "Localizable")
    case "daegu":
        return String(localized: String.LocalizationValue("region_daegu"), table: "Localizable")
    case "tokyo":
        return String(localized: String.LocalizationValue("region_tokyo"), table: "Localizable")
    case "osaka":
        return String(localized: String.LocalizationValue("region_osaka"), table: "Localizable")
    case "yokohama":
        return String(localized: String.LocalizationValue("region_yokohama"), table: "Localizable")
    default:
        return normalized
    }
}
