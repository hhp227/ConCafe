//
//  CafeInfoEditUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/11.
//

import Foundation
import Shared

enum CafeTypeOption: String, CaseIterable, Identifiable {
    case maid = "MAID"
    case butler = "BUTLER"
    case idol = "IDOL"
    case devil = "DEVIL"
    case doll = "DOLL"
    case cosplay = "COSPLAY"
    case namjang = "NAMJANG"
    case yokai = "YOKAI"
    case cat = "CAT"
    case other = "OTHER"

    var id: String { rawValue }

    var title: String {
        switch self {
        case .maid:
            return String(localized: String.LocalizationValue("home_nearby_cafe_type_maid"), table: "Localizable")
        case .butler:
            return String(localized: String.LocalizationValue("home_nearby_cafe_type_butler"), table: "Localizable")
        case .idol:
            return String(localized: String.LocalizationValue("home_nearby_cafe_type_idol"), table: "Localizable")
        case .devil:
            return String(localized: String.LocalizationValue("home_nearby_cafe_type_devil"), table: "Localizable")
        case .doll:
            return String(localized: String.LocalizationValue("home_nearby_cafe_type_doll"), table: "Localizable")
        case .cosplay:
            return String(localized: String.LocalizationValue("home_nearby_cafe_type_cosplay"), table: "Localizable")
        case .namjang:
            return String(localized: String.LocalizationValue("home_nearby_cafe_type_namjang"), table: "Localizable")
        case .yokai:
            return String(localized: String.LocalizationValue("home_nearby_cafe_type_yokai"), table: "Localizable")
        case .cat:
            return String(localized: String.LocalizationValue("home_nearby_cafe_type_cat"), table: "Localizable")
        case .other:
            return String(localized: String.LocalizationValue("home_nearby_cafe_type_other"), table: "Localizable")
        }
    }
}

struct CafeInfoEditUiState {
    let galleryMaxCount = 3

    var detail: CafeDetail?
    var isRegistrationMode = false
    var isLoading = true
    var isSaving = false
    var cafeName = ""
    var cafeDescription = ""
    var conceptType = "MAID"
    var representativeImageUrl: String?
    var galleryImages: [String] = []
    var address = ""
    var mapLatitude = 37.5665
    var mapLongitude = 126.9780
    var contactNumber = ""
    var weekdayOpen = ""
    var weekdayClose = ""
    var weekendOpen = ""
    var weekendClose = ""
    var isImageRequiredAlertVisible = false
    var infoMessage: String?

    var galleryLimitText: String {
        "\(galleryImages.count) / \(galleryMaxCount)"
    }

    var galleryLimitCount: Int {
        galleryImages.count
    }
}
