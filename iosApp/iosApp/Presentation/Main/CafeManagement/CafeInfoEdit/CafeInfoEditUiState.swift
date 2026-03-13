//
//  CafeInfoEditUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/11.
//

import Foundation
import Shared

struct CafeInfoEditUiState {
    var detail: CafeDetail?
    var isRegistrationMode = false
    var isLoading = true
    var isSaving = false
    var cafeName = ""
    var cafeDescription = ""
    var representativeImageTitle = "대표 이미지"
    var representativeImageUrl: String?
    var galleryImages: [String] = []
    var address = ""
    var contactNumber = ""
    var weekdayOpen = ""
    var weekdayClose = ""
    var weekendOpen = ""
    var weekendClose = ""
    var infoMessage: String?

    var galleryLimitText: String {
        "\(galleryImages.count) / 10"
    }

    var screenTitle: String {
        isRegistrationMode ? "새 카페 등록" : "카페 정보 관리"
    }

    var submitButtonText: String {
        isRegistrationMode ? "등록 신청하기" : "변경사항 저장"
    }
}
