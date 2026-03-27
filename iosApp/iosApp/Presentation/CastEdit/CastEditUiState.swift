//
//  CastEditUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/11.
//

import Foundation

struct CastEditUiState {
    let galleryMaxCount = 4

    var isLoading = false
    var isSaving = false
    var screenTitle = "캐스트 프로필 수정"
    var saveButtonLabel = "프로필 저장"
    var profileImageUrl: String? = nil
    var castName = ""
    var conceptRole = ""
    var birthday = ""
    var introduction = ""
    var selectedWorkingDays: Set<WorkingDay> = []
    var galleryImages: [String] = []
    var isImageRequiredAlertVisible = false
    var infoMessage: String? = nil

    var galleryLimitText: String {
        "\(galleryImages.count) / \(galleryMaxCount)"
    }

    enum WorkingDay: String, CaseIterable, Identifiable, Hashable {
        case monday = "Mon"
        case tuesday = "Tue"
        case wednesday = "Wed"
        case thursday = "Thu"
        case friday = "Fri"
        case saturday = "Sat"
        case sunday = "Sun"

        var id: String { rawValue }
        var shortLabel: String { rawValue }
    }

}
