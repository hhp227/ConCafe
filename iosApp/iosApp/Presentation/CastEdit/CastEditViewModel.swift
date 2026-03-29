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

    private let uploadImageUseCase: UploadImageUseCase

    private let deleteImageUseCase: DeleteImageUseCase

    private var pendingDeletedGalleryImageUrls: Set<String> = []

    private var pendingDeletedProfileImageUrl: String? = nil

    private var hasPendingLocalEdits = false

    private func clickProfilePhoto() {
        uiState.infoMessage = nil
    }

    private func toggleWorkingDay(_ day: CastEditUiState.WorkingDay) {
        if uiState.selectedWorkingDays.contains(day) {
            uiState.selectedWorkingDays.remove(day)
        } else {
            uiState.selectedWorkingDays.insert(day)
        }
    }

    private func clickAddGalleryPhoto() {
        uiState.infoMessage = nil
    }

    private func clickSave() {
        let removedGalleryImageUrls = Array(pendingDeletedGalleryImageUrls)
        let removedProfileImageUrl = pendingDeletedProfileImageUrl
        guard !uiState.castName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            uiState.infoMessage = "캐스트 이름을 입력해주세요."
            return
        }
        guard !uiState.conceptRole.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            uiState.infoMessage = "컨셉 역할을 입력해주세요."
            return
        }
        guard !(uiState.profileImageUrl?.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ?? true) ||
            uiState.galleryImages.contains(where: { !$0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }) else {
            uiState.isImageRequiredAlertVisible = true
            return
        }

        uiState.isSaving = true
        uiState.infoMessage = nil

        Task {
            do {
                let uploadedProfileImage = try await uploadImageIfNeeded(
                    uiState.profileImageUrl,
                    folder: "casts/profile"
                )
                let uploadedGalleryImages = try await uploadImagesIfNeeded(
                    uiState.galleryImages,
                    folder: "casts/gallery"
                )
                let result = try await upsertCastUseCase.invoke(
                    update: CastUpsert(
                        castId: castId,
                        cafeId: cafeId,
                        name: uiState.castName,
                        conceptRole: uiState.conceptRole,
                        birthday: uiState.birthday.isEmpty ? nil : uiState.birthday,
                        introduction: uiState.introduction,
                        profileImage: uploadedProfileImage,
                        galleryImages: uploadedGalleryImages,
                        workingDays: uiState.selectedWorkingDays.toWorkingDayKeys()
                    )
                )

                if result is AppResultSuccess<AnyObject> {
                    await deleteImagesIfNeeded((removedProfileImageUrl.map { [$0] } ?? []) + removedGalleryImageUrls)
                    self.pendingDeletedGalleryImageUrls.subtract(removedGalleryImageUrls)
                    if self.pendingDeletedProfileImageUrl == removedProfileImageUrl {
                        self.pendingDeletedProfileImageUrl = nil
                    }
                    uiState.isSaving = false
                    event.send(.navigateBack)
                } else {
                    print("TEST, CastEditViewModel save failure: \(String(describing: result))")
                    uiState.isSaving = false
                    uiState.infoMessage = "캐스트 정보를 저장하지 못했습니다."
                }
            } catch {
                if Task.isCancelled { return }
                print("TEST, CastEditViewModel save catch: \(error.localizedDescription)")
                uiState.isSaving = false
                uiState.infoMessage = "캐스트 정보를 저장하지 못했습니다."
            }
        }
    }

    private func loadCastDetail(_ castId: String) {
        hasPendingLocalEdits = false
        uiState.isLoading = true
        uiState.infoMessage = nil

        Task {
            do {
                let result = try await getCastDetailUseCase.invoke(castId: castId)

                if let success = result as? AppResultSuccess<AnyObject>,
                   let feed = success.data as? CastDetailFeed {
                    let detail = feed.detail
                    var nextState = uiState
                    if hasPendingLocalEdits {
                        uiState.isLoading = false
                    } else {
                        pendingDeletedGalleryImageUrls.removeAll()
                        pendingDeletedProfileImageUrl = nil
                        nextState.isLoading = false
                        nextState.profileImageUrl = detail.cast.profileImage
                        nextState.castName = detail.cast.name
                        nextState.conceptRole = detail.cast.conceptRole
                        nextState.birthday = detail.cast.birthday ?? ""
                        nextState.introduction = detail.cast.desc
                        nextState.selectedWorkingDays = workingDays(from: detail.schedule)
                        nextState.galleryImages = Array(
                            detail.images
                                .filter { !$0.isEmpty && $0 != detail.cast.profileImage }
                                .prefix(nextState.galleryMaxCount)
                        )
                        uiState = nextState
                    }
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
        case .selectProfilePhoto(let imageUrl):
            hasPendingLocalEdits = true
            let previousProfileImageUrl = uiState.profileImageUrl
            if let previousProfileImageUrl,
               !previousProfileImageUrl.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
               previousProfileImageUrl != imageUrl,
               (previousProfileImageUrl.hasPrefix("http://") || previousProfileImageUrl.hasPrefix("https://")),
               previousProfileImageUrl != pendingDeletedProfileImageUrl {
                pendingDeletedProfileImageUrl = previousProfileImageUrl
            }
            uiState.profileImageUrl = imageUrl
            uiState.infoMessage = nil
            uiState.isImageRequiredAlertVisible = false
        case .addGalleryImage(let imageUrl):
            hasPendingLocalEdits = true
            if uiState.galleryImages.count >= uiState.galleryMaxCount {
                uiState.infoMessage = "갤러리 사진은 최대 \(uiState.galleryMaxCount)장까지 등록할 수 있습니다."
                return
            }
            if imageUrl.isEmpty { return }
            uiState.galleryImages.append(imageUrl)
            uiState.infoMessage = nil
            uiState.isImageRequiredAlertVisible = false
        case .removeGalleryImage(let index):
            if uiState.galleryImages.indices.contains(index) {
                hasPendingLocalEdits = true
                let removedImageUrl = uiState.galleryImages[index]
                if removedImageUrl.hasPrefix("http://") || removedImageUrl.hasPrefix("https://") {
                    pendingDeletedGalleryImageUrls.insert(removedImageUrl)
                }
                uiState.galleryImages.remove(at: index)
                uiState.infoMessage = nil
            }
        case .changeCastName(let value):
            hasPendingLocalEdits = true
            uiState.castName = value
        case .changeConceptRole(let value):
            hasPendingLocalEdits = true
            uiState.conceptRole = value
        case .changeBirthday(let value):
            hasPendingLocalEdits = true
            uiState.birthday = value
        case .changeIntroduction(let value):
            hasPendingLocalEdits = true
            uiState.introduction = value
        case .toggleWorkingDay(let day):
            hasPendingLocalEdits = true
            toggleWorkingDay(day)
        case .clickAddGalleryPhoto:
            clickAddGalleryPhoto()
        case .dismissImageRequiredAlert:
            uiState.isImageRequiredAlertVisible = false
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
        upsertCastUseCase: UpsertCastUseCase = KoinInitializerKt.resolveUpsertCastUseCase(),
        uploadImageUseCase: UploadImageUseCase = KoinInitializerKt.resolveUploadImageUseCase(),
        deleteImageUseCase: DeleteImageUseCase = KoinInitializerKt.resolveDeleteImageUseCase()
    ) {
        self.cafeId = cafeId
        self.castId = castId
        self.getCastDetailUseCase = getCastDetailUseCase
        self.upsertCastUseCase = upsertCastUseCase
        self.uploadImageUseCase = uploadImageUseCase
        self.deleteImageUseCase = deleteImageUseCase

        if let castId, !castId.isEmpty {
            uiState.screenTitle = "캐스트 프로필 수정"
            uiState.saveButtonLabel = "프로필 저장"
            loadCastDetail(castId)
        } else {
            uiState.screenTitle = "캐스트 프로필 추가"
            uiState.saveButtonLabel = "프로필 추가"
        }
    }
}

private extension CastEditViewModel {
    func uploadImageIfNeeded(_ imageUrl: String?, folder: String) async throws -> String? {
        guard let imageUrl, !imageUrl.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            return nil
        }
        let result = try await uploadImageUseCase.invoke(localPath: imageUrl, folder: folder)
        guard let success = result as? AppResultSuccess<AnyObject>,
              let uploaded = success.data as? String else {
            print("TEST, CastEditViewModel upload failure: \(String(describing: result))")
            throw NSError(domain: "CastEditUpload", code: 1)
        }
        return uploaded
    }

    func uploadImagesIfNeeded(_ imageUrls: [String], folder: String) async throws -> [String] {
        var results: [String] = []
        for imageUrl in imageUrls where !imageUrl.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            if let uploaded = try await uploadImageIfNeeded(imageUrl, folder: folder) {
                results.append(uploaded)
            }
        }
        return results
    }

    func deleteImagesIfNeeded(_ imageUrls: [String]) async {
        for imageUrl in imageUrls where !imageUrl.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            _ = try? await deleteImageUseCase.invoke(imageUrl: imageUrl)
        }
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
