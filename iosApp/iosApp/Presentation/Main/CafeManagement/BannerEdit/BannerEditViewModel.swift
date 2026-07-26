//
//  BannerEditViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/14.
//

import Foundation
import Combine
import Shared
import KMPNativeCoroutinesAsync

@MainActor
final class BannerEditViewModel: ObservableObject {
    private let initialCafeId: String?

    private let initialBannerId: String?

    private let createHomeBannerUseCase: CreateHomeBannerUseCase

    private let updateHomeBannerUseCase: UpdateHomeBannerUseCase

    private let getCafeManagementUseCase: GetCafeManagementUseCase

    private let getHomeBannerManagementUseCase: GetHomeBannerManagementUseCase

    private let getCafeNoticePageUseCase: GetCafeNoticePageUseCase

    private let getCafeEventPageUseCase: GetCafeEventPageUseCase

    private let observeCurrentUserUseCase: ObserveCurrentUserUseCase

    private let uploadImageUseCase: UploadImageUseCase

    @Published private(set) var uiState = BannerEditUiState()

    let event = PassthroughSubject<BannerEditEvent, Never>()

    private var selectorTask: Task<Void, Never>?

    private func observeSession() {
        Task {
            do {
                for try await user in asyncSequence(for: observeCurrentUserUseCase.invoke()) {
                    self.uiState.isAdmin = user?.role == .admin
                }
            } catch {
                print("Error: \(error)")
            }
        }
    }

    private func loadOwnedCafeOptions() {
        Task {
            do {
                let result = try await getCafeManagementUseCase.invoke()
                if let success = result as? AppResultSuccess<AnyObject>,
                   let data = success.data as? CafeManagementData {
                    let options = data.ownedCafes
                    uiState.ownedCafeOptions = options
                    if let initialCafeId {
                        uiState.selectedCafeId = options.first(where: { $0.id == initialCafeId })?.id
                    } else if uiState.selectedCafeOption == nil && !uiState.isAdmin {
                        uiState.selectedCafeId = options.first?.id
                    }
                    if uiState.selectedTarget == .cafeDetail {
                        uiState.targetValue = uiState.selectedCafeOption?.id ?? ""
                    }
                } else {
                    uiState.ownedCafeOptions = []
                    uiState.infoMessage = MessageKey.ownedCafeLoadFailed
                }
            } catch {
                uiState.ownedCafeOptions = []
                uiState.infoMessage = MessageKey.ownedCafeLoadFailed
            }
        }
    }

    private func loadEditingBanner() {
        guard let initialBannerId else { return }
        Task {
            do {
                let result = try await getHomeBannerManagementUseCase.invoke(cafeId: initialCafeId)
                if let success = result as? AppResultSuccess<AnyObject>,
                   let banners = success.data as? [HomeBanner] {
                    if let banner = banners.first(where: { $0.id == initialBannerId }) {
                        applyEditingBanner(banner)
                    } else {
                        uiState.infoMessage = MessageKey.editBannerNotFound
                    }
                } else {
                    uiState.infoMessage = MessageKey.editBannerLoadFailed
                }
            } catch {
                uiState.infoMessage = MessageKey.editBannerLoadFailed
            }
        }
    }

    private func applyEditingBanner(_ banner: HomeBanner) {
        let target: BannerTargetType
        switch banner.targetType {
        case .cafeDetail:
            target = .cafeDetail
        case .eventDetail:
            target = .eventDetail
        case .notice:
            target = .notice
        case .externalLink:
            target = .externalLink
        default:
            target = .externalLink
        }

        uiState.editingBannerId = banner.id
        uiState.selectedImageLabel = banner.imageUrl
        uiState.originalImageUrl = banner.imageUrl
        uiState.title = banner.title
        uiState.subtitle = banner.subtitle
        uiState.displayDays = min(max(Int(banner.displayDays), 1), 10)
        uiState.selectedCafeId = banner.cafeId
        uiState.selectedTarget = target
        uiState.targetValue = banner.targetValue
        uiState.selectedNoticeId = uiState.selectedTarget == .notice ? banner.targetValue : nil
        uiState.selectedEventId = uiState.selectedTarget == .eventDetail ? banner.targetValue : nil
        uiState.infoMessage = nil

        if let cafeId = banner.cafeId, !cafeId.isEmpty {
            if uiState.selectedTarget == .notice {
                loadNoticeOptions(cafeId: cafeId, query: "")
            } else if uiState.selectedTarget == .eventDetail {
                loadEventOptions(cafeId: cafeId, query: "")
            }
        }
    }

    private func selectTarget(_ target: BannerTargetType) {
        uiState.selectedTarget = target
        uiState.selectedNoticeId = nil
        uiState.selectedEventId = nil
        uiState.selectorType = nil
        uiState.selectorQuery = ""
        uiState.noticeSelectorOptions = []
        uiState.eventSelectorOptions = []
        uiState.isSelectorLoading = false
        switch target {
        case .cafeDetail:
            if let initialCafeId {
                uiState.selectedCafeId = uiState.ownedCafeOptions.first(where: { $0.id == initialCafeId })?.id
            } else if uiState.selectedCafeOption == nil && !uiState.isAdmin {
                uiState.selectedCafeId = uiState.ownedCafeOptions.first?.id
            }
            uiState.targetValue = uiState.selectedCafeOption?.id ?? ""
        case .externalLink:
            uiState.targetValue = ""
        case .notice, .eventDetail:
            uiState.targetValue = ""
        }
    }

    private func openTargetSelector() {
        guard let selectedCafe = uiState.selectedCafeOption else {
            uiState.infoMessage = MessageKey.selectCafeFirst
            return
        }
        switch uiState.selectedTarget {
        case .notice:
            uiState.selectorType = .notice
            uiState.selectorQuery = ""
            uiState.noticeSelectorOptions = []
            uiState.isSelectorLoading = true
            loadNoticeOptions(cafeId: selectedCafe.id, query: "")
        case .eventDetail:
            uiState.selectorType = .event
            uiState.selectorQuery = ""
            uiState.eventSelectorOptions = []
            uiState.isSelectorLoading = true
            loadEventOptions(cafeId: selectedCafe.id, query: "")
        default:
            break
        }
    }

    private func changeSelectorQuery(_ value: String) {
        uiState.selectorQuery = value
        switch uiState.selectorType {
        case .cafe:
            break
        case .notice:
            if let cafeId = uiState.selectedCafeOption?.id {
                loadNoticeOptions(cafeId: cafeId, query: value)
            }
        case .event:
            if let cafeId = uiState.selectedCafeOption?.id {
                loadEventOptions(cafeId: cafeId, query: value)
            }
        case .none:
            break
        }
    }

    private func loadNoticeOptions(cafeId: String, query: String) {
        selectorTask?.cancel()
        selectorTask = Task {
            do {
                let result = try await getCafeNoticePageUseCase.invoke(cafeId: cafeId, query: query, cursor: nil, pageSize: 50)
                if let success = result as? AppResultSuccess<AnyObject>,
                   let page = success.data as? PagedResult<CafeNoticeManagementItem> {
                    let items = page.items as! [CafeNoticeManagementItem]
                    uiState.noticeSelectorOptions = items
                    uiState.isSelectorLoading = false
                } else {
                    uiState.noticeSelectorOptions = []
                    uiState.isSelectorLoading = false
                    uiState.infoMessage = MessageKey.noticeListLoadFailed
                }
            } catch {
                if Task.isCancelled { return }
                uiState.noticeSelectorOptions = []
                uiState.isSelectorLoading = false
                uiState.infoMessage = MessageKey.noticeListLoadFailed
            }
        }
    }

    private func loadEventOptions(cafeId: String, query: String) {
        selectorTask?.cancel()
        selectorTask = Task {
            do {
                let result = try await getCafeEventPageUseCase.invoke(cafeId: cafeId, query: query, cursor: nil, pageSize: 50)
                if let success = result as? AppResultSuccess<AnyObject>,
                   let page = success.data as? PagedResult<CafeEventManagementItem> {
                    let items = page.items as! [CafeEventManagementItem]
                    uiState.eventSelectorOptions = items
                    uiState.isSelectorLoading = false
                } else {
                    uiState.eventSelectorOptions = []
                    uiState.isSelectorLoading = false
                    uiState.infoMessage = MessageKey.eventListLoadFailed
                }
            } catch {
                if Task.isCancelled { return }
                uiState.eventSelectorOptions = []
                uiState.isSelectorLoading = false
                uiState.infoMessage = MessageKey.eventListLoadFailed
            }
        }
    }

    private func selectSelectorItem(_ id: String) {
        switch uiState.selectorType {
        case .cafe:
            guard let selectedCafe = uiState.ownedCafeOptions.first(where: { $0.id == id }) else { return }
            let sameCafe = uiState.selectedCafeId == selectedCafe.id
            uiState.selectedCafeId = selectedCafe.id
            if uiState.selectedTarget == .cafeDetail {
                uiState.targetValue = selectedCafe.id
            } else if !sameCafe {
                uiState.selectedNoticeId = nil
                uiState.selectedEventId = nil
                uiState.targetValue = ""
            }
        case .notice:
            guard let selectedItem = uiState.noticeSelectorOptions.first(where: { $0.id == id }) else { return }
            uiState.selectedNoticeId = selectedItem.id
            uiState.selectedEventId = nil
            uiState.targetValue = selectedItem.id
        case .event:
            guard let selectedItem = uiState.eventSelectorOptions.first(where: { $0.id == id }) else { return }
            uiState.selectedEventId = selectedItem.id
            uiState.selectedNoticeId = nil
            uiState.targetValue = selectedItem.id
        case .none:
            return
        }
        dismissSelector()
    }

    private func dismissSelector() {
        selectorTask?.cancel()
        uiState.selectorType = nil
        uiState.selectorQuery = ""
        uiState.isSelectorLoading = false
    }

    private func clickSave() {
        let validationMessage: String?

        if uiState.title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            validationMessage = MessageKey.validationTitleRequired
        } else if uiState.selectedImageLabel?.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ?? true {
            validationMessage = MessageKey.validationImageRequired
        } else if uiState.subtitle.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            validationMessage = MessageKey.validationSubtitleRequired
        } else if uiState.selectedTarget == .externalLink &&
                    uiState.targetValue.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            validationMessage = MessageKey.validationExternalUrlRequired
        } else if uiState.selectedTarget != .externalLink &&
                    uiState.targetValue.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            validationMessage = MessageKey.validationTargetRequired
        } else {
            validationMessage = nil
        }

        if let validationMessage {
            if uiState.selectedImageLabel?.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ?? true {
                uiState.isImageRequiredAlertVisible = true
            } else {
                uiState.infoMessage = validationMessage
            }
            return
        }

        let targetType: BannerLinkTargetType
        switch uiState.selectedTarget {
        case .cafeDetail:
            targetType = .cafeDetail
        case .eventDetail:
            targetType = .eventDetail
        case .notice:
            targetType = .notice
        case .externalLink:
            targetType = .externalLink
        }

        uiState.isSaving = true
        uiState.infoMessage = nil
        Task {
            do {
                guard let imageUrl = try await resolveBannerImageUrl() else {
                    uiState.isSaving = false
                    uiState.infoMessage = MessageKey.imageUploadFailed
                    return
                }

                let createInput = HomeBannerCreate(
                    cafeId: uiState.selectedCafeId,
                    title: uiState.title.trimmingCharacters(in: .whitespacesAndNewlines),
                    subtitle: uiState.subtitle.trimmingCharacters(in: .whitespacesAndNewlines),
                    imageUrl: imageUrl,
                    targetType: targetType,
                    targetValue: uiState.targetValue.trimmingCharacters(in: .whitespacesAndNewlines),
                    displayDays: Int32(uiState.displayDays)
                )
                let result: Any
                if let editingBannerId = uiState.editingBannerId, !editingBannerId.isEmpty {
                    result = try await updateHomeBannerUseCase.invoke(
                        bannerId: editingBannerId,
                        input: createInput
                    )
                } else {
                    result = try await createHomeBannerUseCase.invoke(input: createInput)
                }
                if result is AppResultSuccess<AnyObject> {
                    uiState.isSaving = false
                    uiState.infoMessage = nil
                    event.send(.navigateBack)
                } else if let failure = result as? AppResultFailure {
                    let userMessage: String
                    if failure.error is AppErrorUnauthorized {
                        userMessage = MessageKey.unauthorized
                    } else if failure.error is AppErrorPermissionDenied {
                        userMessage = MessageKey.permissionDenied
                    } else if failure.error is AppErrorNotFound {
                        userMessage = MessageKey.targetNotFound
                    } else if let error = failure.error as? AppErrorValidationFailed {
                        userMessage = error.reason
                    } else if failure.error is AppErrorNetworkError {
                        userMessage = MessageKey.networkFailed
                    } else {
                        userMessage = MessageKey.saveUnknown
                    }
                    uiState.isSaving = false
                    uiState.infoMessage = userMessage
                } else {
                    uiState.isSaving = false
                    uiState.infoMessage = MessageKey.saveUnknown
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isSaving = false
                uiState.infoMessage = MessageKey.saveUnknown
            }
        }
    }

    private func resolveBannerImageUrl() async throws -> String? {
        let selectedImage = uiState.selectedImageLabel?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if selectedImage.isEmpty {
            return nil
        }

        if selectedImage.hasPrefix("http://") || selectedImage.hasPrefix("https://") {
            return selectedImage
        }
        if let originalImageUrl = uiState.originalImageUrl, originalImageUrl == selectedImage {
            return originalImageUrl
        }

        let uploadResult = try await uploadImageUseCase.invoke(
            localPath: selectedImage,
            folder: "banners"
        )
        if let uploadSuccess = uploadResult as? AppResultSuccess<AnyObject>,
           let uploadedImageUrl = uploadSuccess.data as? String {
            return uploadedImageUrl
        }
        return nil
    }
    
    func onAction(_ action: BannerEditAction) {
        switch action {
        case .clickBack:
            event.send(.navigateBack)
        case .clickImagePicker:
            uiState.infoMessage = nil
        case .selectImage(let imageUrl):
            uiState.selectedImageLabel = imageUrl
            uiState.infoMessage = nil
            uiState.isImageRequiredAlertVisible = false
        case .changeTitle(let value):
            uiState.title = value
        case .changeSubtitle(let value):
            uiState.subtitle = value
        case .selectTarget(let target):
            selectTarget(target)
        case .changeTargetValue(let value):
            uiState.targetValue = value
        case .changeDisplayDays(let value):
            uiState.displayDays = min(max(value, 1), 10)
        case .clickCafeSelector:
            uiState.selectorType = .cafe
            uiState.selectorQuery = ""
            uiState.isSelectorLoading = false
        case .clickTargetSelector:
            openTargetSelector()
        case .changeSelectorQuery(let value):
            changeSelectorQuery(value)
        case .selectSelectorItem(let id):
            selectSelectorItem(id)
        case .dismissSelector:
            dismissSelector()
        case .dismissImageRequiredAlert:
            uiState.isImageRequiredAlertVisible = false
        case .clickSave:
            clickSave()
        case .dismissInfoMessage:
            uiState.infoMessage = nil
        }
    }

    init(
        initialCafeId: String? = nil,
        initialBannerId: String? = nil,
        createHomeBannerUseCase: CreateHomeBannerUseCase = KoinInitializerKt.resolveCreateHomeBannerUseCase(),
        updateHomeBannerUseCase: UpdateHomeBannerUseCase = KoinInitializerKt.resolveUpdateHomeBannerUseCase(),
        getCafeManagementUseCase: GetCafeManagementUseCase = KoinInitializerKt.resolveGetCafeManagementUseCase(),
        getHomeBannerManagementUseCase: GetHomeBannerManagementUseCase = KoinInitializerKt.resolveGetHomeBannerManagementUseCase(),
        getCafeNoticePageUseCase: GetCafeNoticePageUseCase = KoinInitializerKt.resolveGetCafeNoticePageUseCase(),
        getCafeEventPageUseCase: GetCafeEventPageUseCase = KoinInitializerKt.resolveGetCafeEventPageUseCase(),
        observeCurrentUserUseCase: ObserveCurrentUserUseCase = KoinInitializerKt.resolveObserveCurrentUserUseCase(),
        uploadImageUseCase: UploadImageUseCase = KoinInitializerKt.resolveUploadImageUseCase()
    ) {
        self.initialCafeId = initialCafeId
        self.initialBannerId = initialBannerId
        self.createHomeBannerUseCase = createHomeBannerUseCase
        self.updateHomeBannerUseCase = updateHomeBannerUseCase
        self.getCafeManagementUseCase = getCafeManagementUseCase
        self.getHomeBannerManagementUseCase = getHomeBannerManagementUseCase
        self.getCafeNoticePageUseCase = getCafeNoticePageUseCase
        self.getCafeEventPageUseCase = getCafeEventPageUseCase
        self.observeCurrentUserUseCase = observeCurrentUserUseCase
        self.uploadImageUseCase = uploadImageUseCase

        observeSession()
        loadOwnedCafeOptions()
        loadEditingBanner()
    }

    deinit {
        selectorTask?.cancel()
    }

    private enum MessageKey {
        static let ownedCafeLoadFailed = "banneredit_info_owned_cafe_load_failed"
        static let editBannerNotFound = "banneredit_info_edit_banner_not_found"
        static let editBannerLoadFailed = "banneredit_info_edit_banner_load_failed"
        static let selectCafeFirst = "banneredit_info_select_cafe_first"
        static let noticeListLoadFailed = "banneredit_info_notice_list_load_failed"
        static let eventListLoadFailed = "banneredit_info_event_list_load_failed"
        static let validationTitleRequired = "banneredit_validation_title_required"
        static let validationImageRequired = "banneredit_validation_image_required"
        static let validationSubtitleRequired = "banneredit_validation_subtitle_required"
        static let validationExternalUrlRequired = "banneredit_validation_external_url_required"
        static let validationTargetRequired = "banneredit_validation_target_required"
        static let imageUploadFailed = "banneredit_info_image_upload_failed"
        static let unauthorized = "banneredit_error_unauthorized"
        static let permissionDenied = "banneredit_error_permission_denied"
        static let targetNotFound = "banneredit_error_target_not_found"
        static let networkFailed = "banneredit_error_network_failed"
        static let saveUnknown = "banneredit_error_save_unknown"
    }
}
