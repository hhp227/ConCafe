//
//  CafeInfoEditViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/11.
//

import Foundation
import Combine
import Shared

@MainActor
final class CafeInfoEditViewModel: ObservableObject {
    private let cafeId: String?

    private let isRegistrationMode: Bool

    private let getCafeDetailUseCase: GetCafeDetailUseCase

    private let createCafeRegistrationClaimUseCase: CreateCafeRegistrationClaimUseCase

    private let updateCafeInfoUseCase: UpdateCafeInfoUseCase

    private let uploadImageUseCase: UploadImageUseCase

    @Published private(set) var uiState = CafeInfoEditUiState()

    let event = PassthroughSubject<CafeInfoEditEvent, Never>()

    private var loadTask: Task<Void, Never>?

    private func loadCafeInfo() {
        guard let cafeId else { return }
        loadTask?.cancel()
        uiState.isLoading = true
        uiState.infoMessage = nil

        loadTask = Task {
            do {
                let result = try await getCafeDetailUseCase.invoke(cafeId: cafeId)

                if let success = result as? AppResultSuccess<AnyObject>,
                   let feed = success.data as? CafeDetailFeed {
                    let detail = feed.detail
                    let parsedHours = parseBusinessHours(detail.businessHours)
                    let cafeImages = detail.images.filter { !$0.isEmpty }

                    uiState.detail = detail
                    uiState.isLoading = false
                    uiState.cafeName = detail.cafe.name
                    uiState.cafeDescription = detail.cafe.desc
                    uiState.representativeImageUrl = cafeImages.first ?? detail.cafe.thumbnailImage
                    uiState.galleryImages = Array(cafeImages.dropFirst().prefix(uiState.galleryMaxCount))
                    uiState.address = detail.cafe.region.address
                    uiState.contactNumber = detail.phoneNumber
                    uiState.weekdayOpen = parsedHours.weekdayOpen
                    uiState.weekdayClose = parsedHours.weekdayClose
                    uiState.weekendOpen = parsedHours.weekendOpen
                    uiState.weekendClose = parsedHours.weekendClose
                    uiState.infoMessage = nil
                } else {
                    uiState.detail = nil
                    uiState.isLoading = false
                    uiState.infoMessage = "카페 정보를 불러오지 못했습니다."
                }
            } catch {
                if Task.isCancelled { return }
                uiState.detail = nil
                uiState.isLoading = false
                uiState.infoMessage = "카페 정보를 불러오지 못했습니다."
            }
        }
    }

    private func saveCafeInfo() {
        if isRegistrationMode {
            submitCafeRegistration()
            return
        }

        guard let cafeId else { return }
        uiState.isSaving = true
        uiState.infoMessage = nil

        loadTask?.cancel()
        loadTask = Task {
            do {
                let uploadedRepresentativeImage = try await uploadImageIfNeeded(
                    uiState.representativeImageUrl,
                    folder: "cafes/representative"
                )
                let uploadedGalleryImages = try await uploadImagesIfNeeded(
                    uiState.galleryImages,
                    folder: "cafes/gallery"
                )
                let result = try await updateCafeInfoUseCase.invoke(
                    update: CafeInfoUpdate(
                        cafeId: cafeId,
                        name: uiState.cafeName,
                        description: uiState.cafeDescription,
                        representativeImageUrl: uploadedRepresentativeImage,
                        galleryImages: uploadedGalleryImages,
                        address: uiState.address,
                        contactNumber: uiState.contactNumber,
                        weekdayOpen: uiState.weekdayOpen,
                        weekdayClose: uiState.weekdayClose,
                        weekendOpen: uiState.weekendOpen,
                        weekendClose: uiState.weekendClose
                    )
                )

                if let success = result as? AppResultSuccess<AnyObject>,
                   let detail = success.data as? CafeDetail {
                    let parsedHours = parseBusinessHours(detail.businessHours)
                    let cafeImages = detail.images.filter { !$0.isEmpty }

                    uiState.detail = detail
                    uiState.isSaving = false
                    uiState.cafeName = detail.cafe.name
                    uiState.cafeDescription = detail.cafe.desc
                    uiState.representativeImageUrl = cafeImages.first ?? detail.cafe.thumbnailImage
                    uiState.galleryImages = Array(cafeImages.dropFirst().prefix(uiState.galleryMaxCount))
                    uiState.address = detail.cafe.region.address
                    uiState.contactNumber = detail.phoneNumber
                    uiState.weekdayOpen = parsedHours.weekdayOpen
                    uiState.weekdayClose = parsedHours.weekdayClose
                    uiState.weekendOpen = parsedHours.weekendOpen
                    uiState.weekendClose = parsedHours.weekendClose
                    uiState.infoMessage = nil
                    event.send(.navigateBack)
                } else {
                    uiState.isSaving = false
                    uiState.infoMessage = "카페 정보 저장에 실패했습니다."
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isSaving = false
                uiState.infoMessage = "카페 정보 저장에 실패했습니다."
            }
        }
    }

    private func submitCafeRegistration() {
        uiState.isSaving = true
        uiState.infoMessage = nil

        loadTask?.cancel()
        loadTask = Task {
            do {
                let uploadedRepresentativeImage = try await uploadImageIfNeeded(
                    uiState.representativeImageUrl,
                    folder: "cafes/representative"
                )
                let result = try await createCafeRegistrationClaimUseCase.invoke(
                    draft: CafeRegistrationDraft(
                        name: uiState.cafeName.trimmingCharacters(in: .whitespacesAndNewlines),
                        description: uiState.cafeDescription.trimmingCharacters(in: .whitespacesAndNewlines),
                        region: Region(
                            country: "KR",
                            city: "Seoul",
                            address: uiState.address.trimmingCharacters(in: .whitespacesAndNewlines),
                            location: GeoPoint(latitude: 37.5665, longitude: 126.9780)
                        ),
                        thumbnailImage: uploadedRepresentativeImage,
                        conceptType: "MAID",
                        businessHours: formatBusinessHours(),
                        phoneNumber: uiState.contactNumber.trimmingCharacters(in: .whitespacesAndNewlines)
                    )
                )

                if result is AppResultSuccess<AnyObject> {
                    uiState.isSaving = false
                    uiState.infoMessage = nil
                    event.send(.navigateBack)
                } else if let failure = result as? AppResultFailure {
                    uiState.isSaving = false
                    uiState.infoMessage = "\(failure.error)"
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isSaving = false
                uiState.infoMessage = error.localizedDescription
            }
        }
    }

    private func formatBusinessHours() -> String {
        let weekday = !uiState.weekdayOpen.isEmpty && !uiState.weekdayClose.isEmpty
        let weekend = !uiState.weekendOpen.isEmpty && !uiState.weekendClose.isEmpty

        if weekday && weekend && uiState.weekdayOpen == uiState.weekendOpen && uiState.weekdayClose == uiState.weekendClose {
            return "매일 \(uiState.weekdayOpen) - \(uiState.weekdayClose)"
        }
        if weekday && weekend {
            return "평일 \(uiState.weekdayOpen) - \(uiState.weekdayClose) / 주말 \(uiState.weekendOpen) - \(uiState.weekendClose)"
        }
        if weekday {
            return "평일 \(uiState.weekdayOpen) - \(uiState.weekdayClose)"
        }
        if weekend {
            return "주말 \(uiState.weekendOpen) - \(uiState.weekendClose)"
        }
        return ""
    }

    func onAction(_ action: CafeInfoEditAction) {
        switch action {
        case .clickBack:
            event.send(.navigateBack)
        case .changeCafeName(let value):
            uiState.cafeName = value
        case .changeCafeDescription(let value):
            uiState.cafeDescription = value
        case .changeAddress(let value):
            uiState.address = value
        case .changeContactNumber(let value):
            uiState.contactNumber = value
        case .changeWeekdayOpen(let value):
            uiState.weekdayOpen = value
        case .changeWeekdayClose(let value):
            uiState.weekdayClose = value
        case .changeWeekendOpen(let value):
            uiState.weekendOpen = value
        case .changeWeekendClose(let value):
            uiState.weekendClose = value
        case .selectRepresentativeImage(let imageUrl):
            uiState.representativeImageUrl = imageUrl
        case .addGalleryImage(let imageUrl):
            if uiState.galleryImages.count >= uiState.galleryMaxCount {
                showInfo("카페 갤러리는 최대 \(uiState.galleryMaxCount)장까지 등록할 수 있습니다.")
                return
            }
            if imageUrl.isEmpty { return }
            uiState.galleryImages.append(imageUrl)
        case .clickRepresentativeImage:
            break
        case .clickAddGalleryImage:
            break
        case .clickPinLocation:
            showInfo("지도 핀 위치 조정은 다음 단계에서 연결됩니다.")
        case .clickManageExceptionDates:
            showInfo("예외 영업일 관리는 다음 단계에서 연결됩니다.")
        case .clickSave:
            saveCafeInfo()
        case .dismissInfoMessage:
            uiState.infoMessage = nil
        }
    }

    private func showInfo(_ message: String) {
        uiState.infoMessage = message
    }

    private func parseBusinessHours(_ businessHours: String) -> (weekdayOpen: String, weekdayClose: String, weekendOpen: String, weekendClose: String) {
        let pattern = #"(\d{1,2}:\d{2})\s*-\s*(\d{1,2}:\d{2})"#
        let regex = try? NSRegularExpression(pattern: pattern)
        let range = NSRange(location: 0, length: businessHours.utf16.count)

        guard
            let match = regex?.firstMatch(in: businessHours, range: range),
            let openRange = Range(match.range(at: 1), in: businessHours),
            let closeRange = Range(match.range(at: 2), in: businessHours)
        else {
            return ("", "", "", "")
        }

        let open = String(businessHours[openRange])
        let close = String(businessHours[closeRange])
        return (open, close, open, close)
    }

    init(
        cafeId: String? = nil,
        isRegistrationMode: Bool = false,
        createCafeRegistrationClaimUseCase: CreateCafeRegistrationClaimUseCase = KoinInitializerKt.resolveCreateCafeRegistrationClaimUseCase(),
        getCafeDetailUseCase: GetCafeDetailUseCase = KoinInitializerKt.resolveGetCafeDetailUseCase(),
        updateCafeInfoUseCase: UpdateCafeInfoUseCase = KoinInitializerKt.resolveUpdateCafeInfoUseCase(),
        uploadImageUseCase: UploadImageUseCase = KoinInitializerKt.resolveUploadImageUseCase()
    ) {
        self.cafeId = cafeId
        self.isRegistrationMode = isRegistrationMode
        self.createCafeRegistrationClaimUseCase = createCafeRegistrationClaimUseCase
        self.getCafeDetailUseCase = getCafeDetailUseCase
        self.updateCafeInfoUseCase = updateCafeInfoUseCase
        self.uploadImageUseCase = uploadImageUseCase
        self.uiState.isRegistrationMode = isRegistrationMode

        if isRegistrationMode {
            uiState.isLoading = false
        } else {
            loadCafeInfo()
        }
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
            throw NSError(domain: "CafeInfoEdit", code: 1, userInfo: [NSLocalizedDescriptionKey: validation.reason])
        }
        throw NSError(domain: "CafeInfoEdit", code: 1, userInfo: [NSLocalizedDescriptionKey: "이미지를 업로드하지 못했습니다."])
    }

    private func uploadImagesIfNeeded(_ imageUrls: [String], folder: String) async throws -> [String] {
        var uploaded: [String] = []
        for imageUrl in imageUrls where !imageUrl.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            if let result = try await uploadImageIfNeeded(imageUrl, folder: folder) {
                uploaded.append(result)
            }
        }
        return uploaded
    }
}
