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
            uiState.selectedImageLabel = "banner_cover_mock.png"
            uiState.infoMessage = "이미지 업로드 연결은 다음 단계에서 구현됩니다."
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
                    if uiState.selectedCafeOption == nil {
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
            if uiState.selectedCafeOption == nil {
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
        uiState.isSaving = false
        uiState.infoMessage = "배너 초안이 저장되었습니다. 실제 업로드 연동은 다음 단계에서 연결됩니다."
        event.send(.showSaveSuccessAlert)
    }

    init(
        getCafeManagementUseCase: GetCafeManagementUseCase = KoinInitializerKt.resolveGetCafeManagementUseCase(),
        getCafeNoticePageUseCase: GetCafeNoticePageUseCase = KoinInitializerKt.resolveGetCafeNoticePageUseCase(),
        getCafeEventPageUseCase: GetCafeEventPageUseCase = KoinInitializerKt.resolveGetCafeEventPageUseCase(),
        observeCurrentUserUseCase: ObserveCurrentUserUseCase = KoinInitializerKt.resolveObserveCurrentUserUseCase()
    ) {
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
