//
//  BannerEditUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/14.
//

import Foundation

struct BannerEditUiState {
    var screenTitle = "새 배너 등록"
    var submitButtonText = "배너 등록하기"
    var imageSectionTitle = "배너 이미지 업로드"
    var imageGuideText = "권장 비율 16:9 (1080x600px)"
    var imageButtonText = "이미지 선택"
    var selectedImageLabel: String? = nil
    var title = ""
    var subtitle = ""
    var selectedTarget: BannerTargetType = .cafeDetail
    var targetValue = ""
    var displayDays = 5
    var isSaving = false
    var infoMessage: String? = "현재 활성화된 배너 슬롯이 가득 찬 경우, 등록된 배너는 예약 상태(SCHEDULED)로 대기하며 기존 배너 종료 시 자동으로 노출됩니다."

    var displayDaysLabel: String {
        "\(displayDays)일"
    }

    var targetFieldPlaceholder: String {
        selectedTarget.placeholder
    }

    var isSaveEnabled: Bool {
        !title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
        !subtitle.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
        !targetValue.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
        !isSaving
    }
}

enum BannerTargetType: String, CaseIterable, Identifiable {
    case cafeDetail = "카페 상세"
    case eventDetail = "이벤트 상세"
    case notice = "공지사항"
    case externalLink = "외부 링크"

    var id: String { rawValue }

    var placeholder: String {
        switch self {
        case .cafeDetail:
            return "대상 카페 ID를 입력해주세요"
        case .eventDetail:
            return "대상 이벤트 ID를 입력해주세요"
        case .notice:
            return "대상 공지 ID를 입력해주세요"
        case .externalLink:
            return "외부 URL을 입력해주세요"
        }
    }
}
