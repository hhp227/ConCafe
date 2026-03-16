//
//  BannerEditViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/14.
//

import Foundation
import Combine
import Shared

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

    private var sessionWatchHandle: WatchHandle?

    private func observeSession() {
        sessionWatchHandle = observeCurrentUserUseCase.watch { [weak self] user in
            guard let self else { return }
            Task { @MainActor in
                self.uiState.isAdmin = user?.role == .admin
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
            uiState.selectorOptions = uiState.ownedCafeOptions
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

    private func loadOwnedCafeOptions() {
        Task {
            do {
                let result = try await getCafeManagementUseCase.invoke()
                if let success = result as? AppResultSuccess<AnyObject>,
                   let data = success.data as? CafeManagementData {
                    let options = data.ownedCafes.map { item in
                        BannerSelectableItem(id: item.id, title: item.name, subtitle: item.city)
                    }
                    uiState.ownedCafeOptions = options
                    if let initialCafeId {
                        uiState.selectedCafeOption = options.first(where: { $0.id == initialCafeId })
                    } else if uiState.selectedCafeOption == nil && !uiState.isAdmin {
                        uiState.selectedCafeOption = options.first
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
        uiState.selectedContentOption = nil
        uiState.selectorType = nil
        uiState.selectorQuery = ""
        uiState.selectorOptions = []
        uiState.isSelectorLoading = false
        switch target {
        case .cafeDetail:
            if let initialCafeId {
                uiState.selectedCafeOption = uiState.ownedCafeOptions.first(where: { $0.id == initialCafeId })
            } else if uiState.selectedCafeOption == nil && !uiState.isAdmin {
                uiState.selectedCafeOption = uiState.ownedCafeOptions.first
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
            uiState.selectorOptions = []
            uiState.isSelectorLoading = true
            loadNoticeOptions(cafeId: selectedCafe.id, query: "")
        case .eventDetail:
            uiState.selectorType = .event
            uiState.selectorQuery = ""
            uiState.selectorOptions = []
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
            uiState.selectorOptions = uiState.ownedCafeOptions.filter { option in
                option.title.localizedCaseInsensitiveContains(value) ||
                option.subtitle.localizedCaseInsensitiveContains(value)
            }
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
                    uiState.selectorOptions = items.map {
                        BannerSelectableItem(id: $0.id, title: $0.title, subtitle: $0.displayDate)
                    }
                    uiState.isSelectorLoading = false
                } else {
                    uiState.selectorOptions = []
                    uiState.isSelectorLoading = false
                    uiState.infoMessage = "공지사항 목록을 불러오지 못했습니다."
                }
            } catch {
                if Task.isCancelled { return }
                uiState.selectorOptions = []
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
                    uiState.selectorOptions = items.map {
                        BannerSelectableItem(id: $0.id, title: $0.title, subtitle: $0.periodText)
                    }
                    uiState.isSelectorLoading = false
                } else {
                    uiState.selectorOptions = []
                    uiState.isSelectorLoading = false
                    uiState.infoMessage = "이벤트 목록을 불러오지 못했습니다."
                }
            } catch {
                if Task.isCancelled { return }
                uiState.selectorOptions = []
                uiState.isSelectorLoading = false
                uiState.infoMessage = "이벤트 목록을 불러오지 못했습니다."
            }
        }
    }

    private func selectSelectorItem(_ id: String) {
        switch uiState.selectorType {
        case .cafe:
            guard let selectedCafe = uiState.ownedCafeOptions.first(where: { $0.id == id }) else { return }
            let sameCafe = uiState.selectedCafeOption?.id == selectedCafe.id
            uiState.selectedCafeOption = selectedCafe
            if uiState.selectedTarget == .cafeDetail {
                uiState.targetValue = selectedCafe.id
            } else if !sameCafe {
                uiState.selectedContentOption = nil
                uiState.targetValue = ""
            }
        case .notice, .event:
            guard let selectedItem = uiState.selectorOptions.first(where: { $0.id == id }) else { return }
            uiState.selectedContentOption = selectedItem
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
        uiState.selectorOptions = []
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

        uiState.isSaving = true
        uiState.infoMessage = nil
        Task {
            do {
                let result = try await createHomeBannerUseCase.invoke(input: uiState.toCreateInput())
                if result is AppResultSuccess<AnyObject> {
                    uiState.isSaving = false
                    uiState.infoMessage = nil
                    event.send(.navigateBack)
                } else if let failure = result as? AppResultFailure {
                    uiState.isSaving = false
                    uiState.infoMessage = failure.error.toUserMessage()
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
        sessionWatchHandle?.cancel()
    }
}

private extension BannerEditUiState {
    func toCreateInput() -> HomeBannerCreate {
        HomeBannerCreate(
            cafeId: selectedCafeOption?.id,
            title: title.trimmingCharacters(in: .whitespacesAndNewlines),
            subtitle: subtitle.trimmingCharacters(in: .whitespacesAndNewlines),
            imageUrl: selectedImageLabel,
            targetType: selectedTarget.toDomainType(),
            targetValue: targetValue.trimmingCharacters(in: .whitespacesAndNewlines),
            displayDays: Int32(displayDays)
        )
    }
}

private extension BannerTargetType {
    func toDomainType() -> BannerLinkTargetType {
        switch self {
        case .cafeDetail:
            return .cafeDetail
        case .eventDetail:
            return .eventDetail
        case .notice:
            return .notice
        case .externalLink:
            return .externalLink
        }
    }
}

private extension AppError {
    func toUserMessage() -> String {
        if self is AppErrorUnauthorized {
            return "로그인 후 배너를 등록해주세요."
        }
        if self is AppErrorPermissionDenied {
            return "배너 등록 권한이 없습니다."
        }
        if self is AppErrorNotFound {
            return "연결 대상을 찾을 수 없습니다."
        }
        if let error = self as? AppErrorValidationFailed {
            return error.reason
        }
        if self is AppErrorNetworkError {
            return "배너를 등록하지 못했습니다."
        }
        return "배너 등록 중 오류가 발생했습니다."
    }
}
