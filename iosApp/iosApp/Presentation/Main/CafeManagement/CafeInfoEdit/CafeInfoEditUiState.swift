//
//  CafeInfoEditUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/11.
//

import Foundation
import Shared

struct CafeInfoEditUiState {
    let galleryMaxCount = 3

    var detail: CafeDetail?
    var isRegistrationMode = false
    var isLoading = true
    var isSaving = false
    var cafeName = ""
    var cafeDescription = ""
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
