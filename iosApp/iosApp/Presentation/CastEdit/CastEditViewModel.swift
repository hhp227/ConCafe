//
//  CastEditViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/11.
//

import Foundation
import Combine
import Shared

@MainActor
final class CastEditViewModel: ObservableObject {
    @Published private(set) var uiState = CastEditUiState()

    let event = PassthroughSubject<CastEditEvent, Never>()

    private let cafeId: String?

    private let castId: String?

    private let getCastDetailUseCase: GetCastDetailUseCase

    private let upsertCastUseCase: UpsertCastUseCase

    private var requestTask: Task<Void, Never>?

    private func clickProfilePhoto() {
        uiState.infoMessage = "프로필 사진 업로드는 다음 단계에서 연결됩니다."
    }

    private func toggleWorkingDay(_ day: CastEditUiState.WorkingDay) {
        if uiState.selectedWorkingDays.contains(day) {
            uiState.selectedWorkingDays.remove(day)
        } else {
            uiState.selectedWorkingDays.insert(day)
        }
    }

    private func clickAddGalleryPhoto() {
        uiState.infoMessage = "갤러리 사진 업로드는 다음 단계에서 연결됩니다."
    }

    private func clickSave() {
        guard !uiState.castName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            uiState.infoMessage = "캐스트 이름을 입력해주세요."
            return
        }
        guard !uiState.conceptRole.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            uiState.infoMessage = "컨셉 역할을 입력해주세요."
            return
        }

        uiState.isSaving = true
        uiState.infoMessage = nil
        requestTask?.cancel()

        requestTask = Task {
            do {
                let result = try await upsertCastUseCase.invoke(
                    update: CastUpsert(
                        castId: castId,
                        cafeId: cafeId,
                        name: uiState.castName,
                        conceptRole: uiState.conceptRole,
                        birthday: uiState.birthday.isEmpty ? nil : uiState.birthday,
                        introduction: uiState.introduction,
                        workingDays: uiState.selectedWorkingDays.toWorkingDayKeys()
                    )
                )

                if result is AppResultSuccess<AnyObject> {
                    uiState.isSaving = false
                    event.send(.navigateBack)
                } else {
                    uiState.isSaving = false
                    uiState.infoMessage = "캐스트 정보를 저장하지 못했습니다."
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isSaving = false
                uiState.infoMessage = "캐스트 정보를 저장하지 못했습니다."
            }
        }
    }

    private func loadCastDetail(_ castId: String) {
        requestTask?.cancel()
        uiState.isLoading = true
        uiState.infoMessage = nil

        requestTask = Task {
            do {
                let result = try await getCastDetailUseCase.invoke(castId: castId)

                if let success = result as? AppResultSuccess<AnyObject>,
                   let feed = success.data as? CastDetailFeed {
                    let detail = feed.detail
                    var nextState = uiState
                    nextState.isLoading = false
                    nextState.castName = detail.cast.name
                    nextState.conceptRole = detail.cast.conceptRole
                    nextState.birthday = detail.cast.birthday ?? ""
                    nextState.introduction = detail.cast.desc
                    nextState.selectedWorkingDays = workingDays(from: detail.schedule)
                    nextState.galleryItems = galleryItems(from: detail)
                    uiState = nextState
                } else {
                    uiState.isLoading = false
                    uiState.infoMessage = "캐스트 정보를 불러오지 못했습니다."
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isLoading = false
                uiState.infoMessage = "캐스트 정보를 불러오지 못했습니다."
            }
        }
    }

    func onAction(_ action: CastEditAction) {
        switch action {
        case .clickBack:
            event.send(.navigateBack)
        case .clickProfilePhoto:
            clickProfilePhoto()
        case .changeCastName(let value):
            uiState.castName = value
        case .changeConceptRole(let value):
            uiState.conceptRole = value
        case .changeBirthday(let value):
            uiState.birthday = value
        case .changeIntroduction(let value):
            uiState.introduction = value
        case .toggleWorkingDay(let day):
            toggleWorkingDay(day)
        case .clickAddGalleryPhoto:
            clickAddGalleryPhoto()
        case .clickSave:
            clickSave()
        case .dismissInfoMessage:
            uiState.infoMessage = nil
        }
    }

    init(
        cafeId: String? = nil,
        castId: String? = nil,
        getCastDetailUseCase: GetCastDetailUseCase = KoinInitializerKt.resolveGetCastDetailUseCase(),
        upsertCastUseCase: UpsertCastUseCase = KoinInitializerKt.resolveUpsertCastUseCase()
    ) {
        self.cafeId = cafeId
        self.castId = castId
        self.getCastDetailUseCase = getCastDetailUseCase
        self.upsertCastUseCase = upsertCastUseCase

        if let castId, !castId.isEmpty {
            uiState.screenTitle = "캐스트 프로필 수정"
            uiState.saveButtonLabel = "프로필 저장"
            loadCastDetail(castId)
        } else {
            uiState.screenTitle = "캐스트 프로필 추가"
            uiState.saveButtonLabel = "프로필 추가"
        }
    }

    deinit {
        requestTask?.cancel()
    }
}

private func workingDays(from schedules: [CastSchedule]) -> Set<CastEditUiState.WorkingDay> {
    Set(schedules.compactMap { workingDay(from: $0.date) })
}

private extension Set where Element == CastEditUiState.WorkingDay {
    func toWorkingDayKeys() -> [String] {
        return map {
            switch $0 {
            case .monday: return "MONDAY"
            case .tuesday: return "TUESDAY"
            case .wednesday: return "WEDNESDAY"
            case .thursday: return "THURSDAY"
            case .friday: return "FRIDAY"
            case .saturday: return "SATURDAY"
            case .sunday: return "SUNDAY"
            }
        }
    }
}

private func galleryItems(from detail: CastDetail) -> [CastEditUiState.GalleryItem] {
    let images = detail.images.filter { !$0.isEmpty }
    guard !images.isEmpty else { return [] }

    let visibleImages = Array(images.prefix(3))
    let remainingCount = max(images.count - visibleImages.count, 0)

    return visibleImages.enumerated().map { index, image in
        CastEditUiState.GalleryItem(
            id: image.isEmpty ? "gallery-\(index)" : image,
            label: "갤러리 \(index + 1)",
            overlayCount: index == visibleImages.count - 1 && remainingCount > 0 ? remainingCount : nil
        )
    }
}

private func workingDay(from date: String) -> CastEditUiState.WorkingDay? {
    let parts = date.split(separator: "-")
    guard parts.count == 3,
          let year = Int(parts[0]),
          let month = Int(parts[1]),
          let day = Int(parts[2]) else {
        return nil
    }
    let orderedDays = CastEditUiState.WorkingDay.allCases
    let index = dayOfWeekIndex(year: year, month: month, day: day)
    guard orderedDays.indices.contains(index) else { return nil }
    return orderedDays[index]
}

private func dayOfWeekIndex(year: Int, month: Int, day: Int) -> Int {
    var adjustedYear = year
    var adjustedMonth = month
    if adjustedMonth < 3 {
        adjustedMonth += 12
        adjustedYear -= 1
    }
    let k = adjustedYear % 100
    let j = adjustedYear / 100
    let h = (day + (13 * (adjustedMonth + 1)) / 5 + k + (k / 4) + (j / 4) + (5 * j)) % 7
    switch h {
    case 2: return 0
    case 3: return 1
    case 4: return 2
    case 5: return 3
    case 6: return 4
    case 0: return 5
    default: return 6
    }
}
