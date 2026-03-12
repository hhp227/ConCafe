//
//  ReviewEditUiState.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/12.
//

import Foundation

struct ReviewEditUiState {
    var screenTitle = "리뷰 작성"
    var topActionLabel = "등록"
    var submitButtonLabel = "리뷰 등록하기"
    var cafeName = "Starlight Melody Cafe"
    var cafeAddress = "서울 강남구 테헤란로 123"
    var isVisitVerified = true
    var rating = 4
    var reviewText = ""
    var photoItems = PhotoItem.defaultItems
    var atmosphereAnswer: Bool? = nil
    var isSubmitting = false
    var infoMessage: String? = nil

    var ratingMessage: String {
        Self.ratingMessage(for: rating)
    }

    var reviewLength: Int {
        reviewText.count
    }

    var isSubmitEnabled: Bool {
        rating > 0 && reviewText.trimmingCharacters(in: .whitespacesAndNewlines).count >= Self.minimumReviewLength && !isSubmitting
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
