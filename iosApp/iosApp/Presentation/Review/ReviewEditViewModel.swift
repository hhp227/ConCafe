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

    private let createReviewUseCase: CreateReviewUseCase

    private let uploadImageUseCase: UploadImageUseCase

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
                        uiState.isLoggedIn = feed.isLoggedIn
                        uiState.isVisitVerified = feed.isVisitVerified
                        uiState.availableCastTags = detail.casts.map { cast in
                            ReviewEditUiState.CastTag(id: cast.id, name: cast.name)
                        }
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
        uiState.infoMessage = nil
    }

    private func removePhoto() {
        uiState.photoImageUrl = nil
    }

    private func toggleCastTag(_ castId: String) {
        if uiState.taggedCastIds.contains(castId) {
            uiState.taggedCastIds.removeAll { currentId in
                currentId == castId
            }
        } else {
            uiState.taggedCastIds.append(castId)
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
            uiState.infoMessage = nil

            loadTask?.cancel()
            loadTask = Task {
                do {
                    let uploadedPhoto = try await uploadImageIfNeeded(uiState.photoImageUrl, folder: "reviews")
                    let result = try await createReviewUseCase.invoke(
                        cafeId: uiState.cafeId,
                        rating: Float(uiState.rating),
                        content: uiState.content,
                        imageUrls: uploadedPhoto.map { [$0] } ?? [],
                        taggedCastIds: uiState.taggedCastIds
                    )

                    if let success = result as? AppResultSuccess<AnyObject> {
                        uiState.isSubmitting = false
                        if let review = success.data as? Review {
                            uiState.reviewId = review.id
                            uiState.userId = review.userId
                            uiState.visitId = review.visitId
                            uiState.createdAt = review.createdAt
                        }
                        event.send(.navigateBack)
                    } else if let failure = result as? AppResultFailure {
                        uiState.isSubmitting = false
                        if failure.error is AppErrorUnauthorized {
                            uiState.infoMessage = "리뷰 작성은 로그인 후 가능해요."
                        } else if let error = failure.error as? AppErrorValidationFailed {
                            uiState.infoMessage = Self.reviewValidationMessage(for: error.reason)
                        } else {
                            uiState.infoMessage = "리뷰 등록에 실패했습니다."
                        }
                    } else {
                        uiState.isSubmitting = false
                        uiState.infoMessage = "리뷰 등록에 실패했습니다."
                    }
                } catch {
                    if Task.isCancelled { return }
                    uiState.isSubmitting = false
                    uiState.infoMessage = "리뷰 등록에 실패했습니다."
                }
            }
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
        case .selectPhoto(let imageUrl):
            uiState.photoImageUrl = imageUrl
            uiState.infoMessage = nil
        case .removePhoto:
            removePhoto()
        case .changeReviewText(let value):
            uiState.content = value
        case .toggleCastTag(let castId):
            toggleCastTag(castId)
        case .selectAtmosphereAnswer(let isPositive):
            uiState.atmosphereAnswer = isPositive
        case .clickSubmit:
            clickSubmit()
        case .dismissInfoMessage:
            uiState.infoMessage = nil
        }
    }

    private static func reviewValidationMessage(for reason: String) -> String {
        switch reason {
        case "cafeId is required":
            return "카페 정보를 찾을 수 없습니다."
        case "rating is required":
            return "평점을 선택해주세요."
        case "review content is required":
            return "상세 리뷰를 입력해주세요."
        default:
            return reason
        }
    }

    init(
        cafeId: String? = nil,
        getCafeDetailUseCase: GetCafeDetailUseCase = KoinInitializerKt.resolveGetCafeDetailUseCase(),
        createReviewUseCase: CreateReviewUseCase = KoinInitializerKt.resolveCreateReviewUseCase(),
        uploadImageUseCase: UploadImageUseCase = KoinInitializerKt.resolveUploadImageUseCase()
    ) {
        self.cafeId = cafeId
        self.getCafeDetailUseCase = getCafeDetailUseCase
        self.createReviewUseCase = createReviewUseCase
        self.uploadImageUseCase = uploadImageUseCase
        uiState.cafeId = cafeId ?? ""

        loadCafeInfo()
    }

    deinit {
        loadTask?.cancel()
    }

    private func uploadImageIfNeeded(_ imageUrl: String?, folder: String) async throws -> String? {
        guard let imageUrl, !imageUrl.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            return nil
        }
        let result = try await uploadImageUseCase.invoke(localPath: imageUrl, folder: folder)
        if let success = result as? AppResultSuccess<AnyObject>, let data = success.data as? String {
            return data
        }
        if let failure = result as? AppResultFailure, let validation = failure.error as? AppErrorValidationFailed {
            throw NSError(domain: "ReviewEdit", code: 1, userInfo: [NSLocalizedDescriptionKey: validation.reason])
        }
        throw NSError(domain: "ReviewEdit", code: 1, userInfo: [NSLocalizedDescriptionKey: "이미지를 업로드하지 못했습니다."])
    }
}
