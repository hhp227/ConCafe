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
    var userId = "user-1"
    var visitId = "visit-1"
    var rating = 4
    var content = ""
    var images = PhotoItem.defaultItems
    var likeCount = 0
    var createdAt = ""
    var isLoading = false
    var screenTitle = "리뷰 작성"
    var topActionLabel = "등록"
    var submitButtonLabel = "리뷰 등록하기"
    var cafeName = ""
    var cafeAddress = ""
    var isVisitVerified = true
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

        static let defaultItems = [
            PhotoItem(id: "photo-1", label: "라떼 아트", accentColorHex: "A65A74", backgroundColorHex: "FFE3EC"),
            PhotoItem(id: "photo-2", label: "테이블 뷰", accentColorHex: "6D4C68", backgroundColorHex: "F8E4EC"),
            PhotoItem(id: "photo-3", label: "머신 존", accentColorHex: "7D5A4F", backgroundColorHex: "FFEBDD")
        ]
    }
}
