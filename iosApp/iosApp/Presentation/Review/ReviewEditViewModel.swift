//
//  ReviewEditViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/12.
//

import Foundation
import Combine

@MainActor
final class ReviewEditViewModel: ObservableObject {
    @Published private(set) var uiState = ReviewEditUiState()

    let event = PassthroughSubject<ReviewEditEvent, Never>()

    private func clickAddPhoto() {
        if uiState.photoItems.count >= ReviewEditUiState.maximumPhotoCount {
            uiState.infoMessage = "사진은 최대 10장까지 등록할 수 있습니다."
        } else {
            let nextIndex = uiState.photoItems.count + 1
            uiState.photoItems.append(Self.placeholderPhotoItem(index: nextIndex))
            uiState.infoMessage = "사진 업로드는 다음 단계에서 연결됩니다."
        }
    }

    private func removePhoto(_ photoId: String) {
        uiState.photoItems.removeAll { item in
            item.id == photoId
        }
    }

    private func clickSubmit() {
        let reviewLength = uiState.reviewText.trimmingCharacters(in: .whitespacesAndNewlines).count

        if uiState.rating <= 0 {
            uiState.infoMessage = "평점을 선택해주세요."
        } else if reviewLength < ReviewEditUiState.minimumReviewLength {
            uiState.infoMessage = "상세 리뷰는 최소 10자 이상 입력해주세요."
        } else {
            uiState.isSubmitting = true
            uiState.infoMessage = "리뷰 등록은 다음 단계에서 연결됩니다."
            uiState.isSubmitting = false
        }
    }

    func onAction(_ action: ReviewEditAction) {
        switch action {
        case .clickBack:
            event.send(.navigateBack)
        case .selectRating(let rating):
            uiState.rating = min(max(rating, 0), ReviewEditUiState.maximumRating)
        case .clickAddPhoto:
            clickAddPhoto()
        case .removePhoto(let photoId):
            removePhoto(photoId)
        case .changeReviewText(let value):
            uiState.reviewText = value
        case .selectAtmosphereAnswer(let isPositive):
            uiState.atmosphereAnswer = isPositive
        case .clickSubmit:
            clickSubmit()
        case .dismissInfoMessage:
            uiState.infoMessage = nil
        }
    }

    private static func placeholderPhotoItem(index: Int) -> ReviewEditUiState.PhotoItem {
        let accentColors = ["A65A74", "6D4C68", "7D5A4F", "8E5E78", "8A6557"]
        let backgroundColors = ["FFE3EC", "F8E4EC", "FFEBDD", "FFF1F5", "F9ECE6"]
        let colorIndex = (index - 1) % accentColors.count
        return ReviewEditUiState.PhotoItem(
            id: "photo-\(index)",
            label: "사진 \(index)",
            accentColorHex: accentColors[colorIndex],
            backgroundColorHex: backgroundColors[colorIndex]
        )
    }
}
