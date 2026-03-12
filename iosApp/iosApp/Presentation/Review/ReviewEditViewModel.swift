//
//  ReviewEditViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/12.
//

import Foundation
import Combine
import Shared

@MainActor
final class ReviewEditViewModel: ObservableObject {
    @Published private(set) var uiState = ReviewEditUiState()

    let event = PassthroughSubject<ReviewEditEvent, Never>()

    private let cafeId: String?

    private let getCafeDetailUseCase: GetCafeDetailUseCase

    private var loadTask: Task<Void, Never>?

    private func loadCafeInfo() {
        if let cafeId, !cafeId.isEmpty {
            loadTask?.cancel()
            uiState.isLoading = true
            uiState.infoMessage = nil
            uiState.cafeId = cafeId

            loadTask = Task {
                do {
                    let result = try await getCafeDetailUseCase.invoke(cafeId: cafeId)

                    if let success = result as? AppResultSuccess<AnyObject>,
                       let feed = success.data as? CafeDetailFeed {
                        let detail = feed.detail
                        uiState.isLoading = false
                        uiState.cafeId = cafeId
                        uiState.cafeName = detail.cafe.name
                        uiState.cafeAddress = detail.cafe.region.address
                        uiState.infoMessage = nil
                    } else {
                        uiState.isLoading = false
                        uiState.infoMessage = "카페 정보를 불러오지 못했습니다."
                    }
                } catch {
                    if Task.isCancelled { return }
                    uiState.isLoading = false
                    uiState.infoMessage = "카페 정보를 불러오지 못했습니다."
                }
            }
        } else {
            uiState.infoMessage = "카페 정보를 찾을 수 없습니다."
        }
    }

    private func clickAddPhoto() {
        if uiState.images.count >= ReviewEditUiState.maximumPhotoCount {
            uiState.infoMessage = "사진은 최대 10장까지 등록할 수 있습니다."
        } else {
            let nextIndex = uiState.images.count + 1
            uiState.images.append(Self.placeholderPhotoItem(index: nextIndex))
            uiState.infoMessage = "사진 업로드는 다음 단계에서 연결됩니다."
        }
    }

    private func removePhoto(_ photoId: String) {
        uiState.images.removeAll { item in
            item.id == photoId
        }
    }

    private func clickSubmit() {
        let reviewLength = uiState.content.trimmingCharacters(in: .whitespacesAndNewlines).count

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
            uiState.content = value
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

    init(
        cafeId: String? = nil,
        getCafeDetailUseCase: GetCafeDetailUseCase = KoinInitializerKt.resolveGetCafeDetailUseCase()
    ) {
        self.cafeId = cafeId
        self.getCafeDetailUseCase = getCafeDetailUseCase
        uiState.cafeId = cafeId ?? ""

        loadCafeInfo()
    }

    deinit {
        loadTask?.cancel()
    }
}
