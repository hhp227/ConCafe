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
                    uiState.mapLatitude = detail.cafe.region.location.latitude
                    uiState.mapLongitude = detail.cafe.region.location.longitude
                    uiState.contactNumber = detail.phoneNumber == MessageKey.contactPlaceholder ? "" : detail.phoneNumber
                    uiState.weekdayOpen = parsedHours.weekdayOpen
                    uiState.weekdayClose = parsedHours.weekdayClose
                    uiState.weekendOpen = parsedHours.weekendOpen
                    uiState.weekendClose = parsedHours.weekendClose
                    uiState.infoMessage = nil
                } else {
                    uiState.detail = nil
                    uiState.isLoading = false
                    uiState.infoMessage = MessageKey.loadFailed
                }
            } catch {
                if Task.isCancelled { return }
                uiState.detail = nil
                uiState.isLoading = false
                uiState.infoMessage = MessageKey.loadFailed
            }
        }
    }

    private func saveCafeInfo() {
        if (uiState.representativeImageUrl?.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ?? true)
            && uiState.galleryImages.allSatisfy({ $0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }) {
            uiState.isImageRequiredAlertVisible = true
            uiState.infoMessage = MessageKey.imageRequiredOneOrMore
            return
        }

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
                        location: GeoPoint(
                            latitude: uiState.mapLatitude,
                            longitude: uiState.mapLongitude
                        ),
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
                    uiState.mapLatitude = detail.cafe.region.location.latitude
                    uiState.mapLongitude = detail.cafe.region.location.longitude
                    uiState.contactNumber = detail.phoneNumber == MessageKey.contactPlaceholder ? "" : detail.phoneNumber
                    uiState.weekdayOpen = parsedHours.weekdayOpen
                    uiState.weekdayClose = parsedHours.weekdayClose
                    uiState.weekendOpen = parsedHours.weekendOpen
                    uiState.weekendClose = parsedHours.weekendClose
                    uiState.infoMessage = nil
                    event.send(.navigateBack)
                } else {
                    uiState.isSaving = false
                    uiState.infoMessage = MessageKey.saveFailed
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isSaving = false
                uiState.infoMessage = MessageKey.saveFailed
            }
        }
    }

    private func submitCafeRegistration() {
        if uiState.representativeImageUrl?.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ?? true {
            uiState.isImageRequiredAlertVisible = true
            uiState.infoMessage = MessageKey.registrationRepRequired
            return
        }

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
                            location: GeoPoint(
                                latitude: uiState.mapLatitude,
                                longitude: uiState.mapLongitude
                            )
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
                showInfo("\(MessageKey.galleryMaxExceeded):\(uiState.galleryMaxCount)")
                return
            }
            if imageUrl.isEmpty { return }
            uiState.galleryImages.append(imageUrl)
        case .clickRepresentativeImage:
            break
        case .clickAddGalleryImage:
            break
        case .clickPinLocation:
            showInfo(MessageKey.pinLocationHint)
        case .setPinnedLocation(let latitude, let longitude):
            uiState.mapLatitude = latitude
            uiState.mapLongitude = longitude
        case .clickManageExceptionDates:
            showInfo(MessageKey.exceptionNextStep)
        case .dismissImageRequiredAlert:
            uiState.isImageRequiredAlertVisible = false
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
        let times = TimeUtils.extractNormalizedHourMinuteList(from: businessHours)
        let weekdayOpen = times.indices.contains(0) ? times[0] : ""
        let weekdayClose = times.indices.contains(1) ? times[1] : ""
        let weekendOpen = times.indices.contains(2) ? times[2] : weekdayOpen
        let weekendClose = times.indices.contains(3) ? times[3] : weekdayClose
        return (weekdayOpen, weekdayClose, weekendOpen, weekendClose)
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
        throw NSError(domain: "CafeInfoEdit", code: 1, userInfo: [NSLocalizedDescriptionKey: MessageKey.imageUploadFailed])
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

    private enum MessageKey {
        static let loadFailed = "cafeinfo_info_load_failed"
        static let imageRequiredOneOrMore = "cafeinfo_info_image_required_one_or_more"
        static let saveFailed = "cafeinfo_info_save_failed"
        static let registrationRepRequired = "cafeinfo_info_registration_rep_required"
        static let galleryMaxExceeded = "cafeinfo_info_gallery_max_exceeded"
        static let pinLocationHint = "cafeinfo_info_pin_location_hint"
        static let exceptionNextStep = "cafeinfo_info_exception_next_step"
        static let imageUploadFailed = "cafeinfo_info_image_upload_failed"
        static let contactPlaceholder = "연락처 정보 준비중"
    }
}
