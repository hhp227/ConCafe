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
    private let cafeId: String

    private let getCafeDetailUseCase: GetCafeDetailUseCase

    private let updateCafeInfoUseCase: UpdateCafeInfoUseCase

    @Published private(set) var uiState = CafeInfoEditUiState()

    let event = PassthroughSubject<CafeInfoEditEvent, Never>()

    private var loadTask: Task<Void, Never>?

    private func loadCafeInfo() {
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

                    uiState.detail = detail
                    uiState.isLoading = false
                    uiState.cafeName = detail.cafe.name
                    uiState.cafeDescription = detail.cafe.desc
                    uiState.representativeImageUrl = detail.images.first(where: { !$0.isEmpty }) ?? detail.cafe.thumbnailImage
                    uiState.galleryImages = detail.images.filter { !$0.isEmpty }
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
        uiState.isSaving = true
        uiState.infoMessage = nil

        loadTask?.cancel()
        loadTask = Task {
            do {
                let result = try await updateCafeInfoUseCase.invoke(
                    update: CafeInfoUpdate(
                        cafeId: cafeId,
                        name: uiState.cafeName,
                        description: uiState.cafeDescription,
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
                    uiState.detail = detail
                    uiState.isSaving = false
                    uiState.cafeName = detail.cafe.name
                    uiState.cafeDescription = detail.cafe.desc
                    uiState.representativeImageUrl = detail.images.first(where: { !$0.isEmpty }) ?? detail.cafe.thumbnailImage
                    uiState.galleryImages = detail.images.filter { !$0.isEmpty }
                    uiState.address = detail.cafe.region.address
                    uiState.contactNumber = detail.phoneNumber
                    uiState.weekdayOpen = parsedHours.weekdayOpen
                    uiState.weekdayClose = parsedHours.weekdayClose
                    uiState.weekendOpen = parsedHours.weekendOpen
                    uiState.weekendClose = parsedHours.weekendClose
                    uiState.infoMessage = nil
                    event.send(.showSaveSuccessAlert)
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
        case .clickRepresentativeImage:
            showInfo("대표 이미지 업로드는 다음 단계에서 연결됩니다.")
        case .clickAddGalleryImage:
            showInfo("갤러리 이미지 추가는 다음 단계에서 연결됩니다.")
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
        cafeId: String,
        getCafeDetailUseCase: GetCafeDetailUseCase = KoinInitializerKt.resolveGetCafeDetailUseCase(),
        updateCafeInfoUseCase: UpdateCafeInfoUseCase = KoinInitializerKt.resolveUpdateCafeInfoUseCase()
    ) {
        self.cafeId = cafeId
        self.getCafeDetailUseCase = getCafeDetailUseCase
        self.updateCafeInfoUseCase = updateCafeInfoUseCase

        loadCafeInfo()
    }

    deinit {
        loadTask?.cancel()
    }
}
