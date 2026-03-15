//
//  ReviewEditUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/12.
//

import Foundation

struct ReviewEditUiState {
    var reviewId: String? = nil
    var cafeId = ""
    var userId = ""
    var visitId = ""
    var rating = 0
    var content = ""
    var images: [PhotoItem] = []
    var taggedCastIds: [String] = []
    var availableCastTags: [CastTag] = []
    var likeCount = 0
    var createdAt = ""
    var isLoading = false
    var screenTitle = "리뷰 작성"
    var topActionLabel = "등록"
    var submitButtonLabel = "리뷰 등록하기"
    var cafeName = ""
    var cafeAddress = ""
    var isLoggedIn = false
    var isVisitVerified = false
    var atmosphereAnswer: Bool? = nil
    var isSubmitting = false
    var infoMessage: String? = nil

    var ratingMessage: String {
        Self.ratingMessage(for: rating)
    }

    var reviewLength: Int {
        content.count
    }

    var isSubmitEnabled: Bool {
        rating > 0 && content.trimmingCharacters(in: .whitespacesAndNewlines).count >= Self.minimumReviewLength && !isSubmitting
    }

    static let maximumRating = 5
    static let maximumPhotoCount = 10
    static let minimumReviewLength = 10

    static func ratingMessage(for rating: Int) -> String {
        switch rating {
        case 5:
            return "최고예요!"
        case 4:
            return "추천해요!"
        case 3:
            return "무난했어요"
        case 2:
            return "조금 아쉬워요"
        case 1:
            return "추천하지 않아요"
        default:
            return "평점을 선택해주세요"
        }
    }

    struct PhotoItem: Identifiable, Hashable {
        let id: String
        let label: String
        let accentColorHex: String
        let backgroundColorHex: String

    }

    struct CastTag: Identifiable, Hashable {
        let id: String
        let name: String
    }
}
