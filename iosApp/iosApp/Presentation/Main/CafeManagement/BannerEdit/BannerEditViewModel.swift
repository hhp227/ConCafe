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

    private let createHomeBannerUseCase: CreateHomeBannerUseCase

    private let getCafeManagementUseCase: GetCafeManagementUseCase

    private let getCafeNoticePageUseCase: GetCafeNoticePageUseCase

    private let getCafeEventPageUseCase: GetCafeEventPageUseCase

    private let observeCurrentUserUseCase: ObserveCurrentUserUseCase

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
                    uiState.infoMessage = "운영 카페 목록을 불러오지 못했습니다."
                }
            } catch {
                uiState.ownedCafeOptions = []
                uiState.infoMessage = "운영 카페 목록을 불러오지 못했습니다."
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
            uiState.infoMessage = "먼저 운영 카페를 선택해주세요."
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
                    uiState.infoMessage = "공지사항 목록을 불러오지 못했습니다."
                }
            } catch {
                if Task.isCancelled { return }
                uiState.noticeSelectorOptions = []
                uiState.isSelectorLoading = false
                uiState.infoMessage = "공지사항 목록을 불러오지 못했습니다."
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
                    uiState.infoMessage = "이벤트 목록을 불러오지 못했습니다."
                }
            } catch {
                if Task.isCancelled { return }
                uiState.eventSelectorOptions = []
                uiState.isSelectorLoading = false
                uiState.infoMessage = "이벤트 목록을 불러오지 못했습니다."
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
            validationMessage = "배너 제목을 입력해주세요."
        } else if uiState.subtitle.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            validationMessage = "서브 문구를 입력해주세요."
        } else if uiState.selectedTarget == .externalLink &&
                    uiState.targetValue.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            validationMessage = "외부 URL을 입력해주세요."
        } else if uiState.selectedTarget != .externalLink &&
                    uiState.targetValue.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            validationMessage = "연결 대상을 선택해주세요."
        } else {
            validationMessage = nil
        }

        if let validationMessage {
            uiState.infoMessage = validationMessage
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

        let createInput = HomeBannerCreate(
            cafeId: uiState.selectedCafeId,
            title: uiState.title.trimmingCharacters(in: .whitespacesAndNewlines),
            subtitle: uiState.subtitle.trimmingCharacters(in: .whitespacesAndNewlines),
            imageUrl: uiState.selectedImageLabel,
            targetType: targetType,
            targetValue: uiState.targetValue.trimmingCharacters(in: .whitespacesAndNewlines),
            displayDays: Int32(uiState.displayDays)
        )

        uiState.isSaving = true
        uiState.infoMessage = nil
        Task {
            do {
                let result = try await createHomeBannerUseCase.invoke(input: createInput)
                if result is AppResultSuccess<AnyObject> {
                    uiState.isSaving = false
                    uiState.infoMessage = nil
                    event.send(.navigateBack)
                } else if let failure = result as? AppResultFailure {
                    let userMessage: String
                    if failure.error is AppErrorUnauthorized {
                        userMessage = "로그인 후 배너를 등록해주세요."
                    } else if failure.error is AppErrorPermissionDenied {
                        userMessage = "배너 등록 권한이 없습니다."
                    } else if failure.error is AppErrorNotFound {
                        userMessage = "연결 대상을 찾을 수 없습니다."
                    } else if let error = failure.error as? AppErrorValidationFailed {
                        userMessage = error.reason
                    } else if failure.error is AppErrorNetworkError {
                        userMessage = "배너를 등록하지 못했습니다."
                    } else {
                        userMessage = "배너 등록 중 오류가 발생했습니다."
                    }
                    uiState.isSaving = false
                    uiState.infoMessage = userMessage
                } else {
                    uiState.isSaving = false
                    uiState.infoMessage = "배너 등록 중 오류가 발생했습니다."
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isSaving = false
                uiState.infoMessage = "배너 등록 중 오류가 발생했습니다."
            }
        }
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
        case .clickSave:
            clickSave()
        case .dismissInfoMessage:
            uiState.infoMessage = nil
        }
    }

    init(
        initialCafeId: String? = nil,
        createHomeBannerUseCase: CreateHomeBannerUseCase = KoinInitializerKt.resolveCreateHomeBannerUseCase(),
        getCafeManagementUseCase: GetCafeManagementUseCase = KoinInitializerKt.resolveGetCafeManagementUseCase(),
        getCafeNoticePageUseCase: GetCafeNoticePageUseCase = KoinInitializerKt.resolveGetCafeNoticePageUseCase(),
        getCafeEventPageUseCase: GetCafeEventPageUseCase = KoinInitializerKt.resolveGetCafeEventPageUseCase(),
        observeCurrentUserUseCase: ObserveCurrentUserUseCase = KoinInitializerKt.resolveObserveCurrentUserUseCase()
    ) {
        self.initialCafeId = initialCafeId
        self.createHomeBannerUseCase = createHomeBannerUseCase
        self.getCafeManagementUseCase = getCafeManagementUseCase
        self.getCafeNoticePageUseCase = getCafeNoticePageUseCase
        self.getCafeEventPageUseCase = getCafeEventPageUseCase
        self.observeCurrentUserUseCase = observeCurrentUserUseCase
        observeSession()
        loadOwnedCafeOptions()
    }

    deinit {
        selectorTask?.cancel()
    }
}
