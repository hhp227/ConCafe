//
//  CafeDashboardViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/10.
//

import Foundation
import Combine
import Shared

@MainActor
final class CafeDashboardViewModel: ObservableObject {
    private let cafeId: String

    private let getCafeCastPageUseCase: GetCafeCastPageUseCase

    private let getCafeDashboardUseCase: GetCafeDashboardUseCase

    private let observeCafeDetailEventUseCase: ObserveCafeDetailEventUseCase

    private let observeCastEventUseCase: ObserveCastEventUseCase

    private let observeCurrentUserUseCase: ObserveCurrentUserUseCase

    @Published private(set) var uiState = CafeDashboardUiState()

    let event = PassthroughSubject<CafeDashboardEvent, Never>()

    private var watchHandles: [WatchKey: WatchHandle] = [:]

    private func loadCafeDashboard() {
        Task {
            uiState.isLoading = true
            uiState.infoMessage = nil

            do {
                let result = try await getCafeDashboardUseCase.invoke(cafeId: cafeId)

                if let success = result as? AppResultSuccess<AnyObject>,
                   let data = success.data as? CafeDashboardData {
                    uiState.cafe = data
                    uiState.isLoading = false
                    refreshCastPreviews(resetMessage: false)
                } else if let failure = result as? AppResultFailure {
                    uiState.cafe = nil
                    uiState.castPreviews = []
                    uiState.nextCastCursor = nil
                    uiState.hasMoreCasts = false
                    uiState.isLoading = false
                    uiState.infoMessage = "\(failure.error)"
                }
            } catch {
                uiState.cafe = nil
                uiState.castPreviews = []
                uiState.nextCastCursor = nil
                uiState.hasMoreCasts = false
                uiState.isLoading = false
                uiState.infoMessage = error.localizedDescription
            }
        }
    }

    private func loadCastPage(cursor: String?, pageSize: Int, append: Bool) {
        Task {
            uiState.isLoadingMoreCasts = append

            do {
                let result = try await self.getCafeCastPageUseCase.invoke(
                    cafeId: cafeId,
                    cursor: cursor,
                    pageSize: Int32(pageSize)
                )

                if let success = result as? AppResultSuccess<AnyObject>,
                   let page = success.data as? PagedResult<CafeCastPreview> {
                    let mergedItems = append ? (uiState.castPreviews + (page.items as! [CafeCastPreview])) : (page.items as! [CafeCastPreview])
                    uiState.castPreviews = mergedItems
                    if let selectedCastId = uiState.selectedCastId,
                       !mergedItems.contains(where: { $0.id == selectedCastId }) {
                        uiState.selectedCastId = nil
                    }
                    uiState.nextCastCursor = page.nextCursor
                    uiState.hasMoreCasts = page.hasNext
                    uiState.isLoadingMoreCasts = false
                } else {
                    uiState.isLoadingMoreCasts = false
                    uiState.infoMessage = "소속 캐스트 목록을 불러오지 못했습니다."
                }
            } catch {
                uiState.isLoadingMoreCasts = false
                uiState.infoMessage = "소속 캐스트 목록을 불러오지 못했습니다."
            }
        }
    }

    private func refreshCastPreviews(resetMessage: Bool = true) {
        if resetMessage {
            uiState.infoMessage = nil
        }
        loadCastPage(
            cursor: nil,
            pageSize: Int(getCafeCastPageUseCase.defaultPageSize()),
            append: false
        )
    }

    private func clickLoadMoreCasts() {
        guard uiState.hasMoreCasts, !uiState.isLoadingMoreCasts else { return }
        loadCastPage(
            cursor: uiState.nextCastCursor,
            pageSize: Int(getCafeCastPageUseCase.defaultPageSize()),
            append: true
        )
    }

    private func clickBack() {
        event.send(.navigateBack)
    }

    private func clickShortcut(_ shortcut: CafeDashboardShortcut) {
        switch shortcut {
        case .cafeSettings:
            event.send(.navigateToCafeInfoEdit(cafeId: cafeId))
        case .menuGoods:
            event.send(.navigateToMenuGoods(cafeId: cafeId))
        case .castSchedule:
            guard let selectedCastId = uiState.selectedCastId else {
                uiState.infoMessage = "출근표를 관리할 캐스트를 목록에서 선택해 주세요."
                return
            }
            event.send(.navigateToSchedule(castId: selectedCastId))
        case .castManagement:
            event.send(.navigateToCastEdit(cafeId: cafeId, castId: nil))
        default:
            uiState.infoMessage = "\(shortcut.title) 연결은 다음 단계에서 이어집니다."
        }
    }

    private func dismissInfoMessage() {
        uiState.infoMessage = nil
    }

    private func clickCastSchedule(_ castId: String) {
        uiState.selectedCastId = uiState.selectedCastId == castId ? nil : castId
        uiState.infoMessage = nil
    }

    private func observeSession() {
        watchHandles[.session]?.cancel()
        watchHandles[.session] = observeCurrentUserUseCase.watch { [weak self] _ in
            guard let self else { return }

            Task { @MainActor in
                self.loadCafeDashboard()
            }
        }
    }

    private func observeCafeDetailEvent() {
        watchHandles[.cafeDetailEvent]?.cancel()
        watchHandles[.cafeDetailEvent] = observeCafeDetailEventUseCase.watch { [weak self] event in
            guard let self else { return }
            Task { @MainActor in
                if event.matches(cafeId: self.cafeId) {
                    self.loadCafeDashboard()
                }
            }
        }
    }

    private func observeCastEvent() {
        watchHandles[.castEvent]?.cancel()
        watchHandles[.castEvent] = observeCastEventUseCase.watch { [weak self] event in
            guard let self else { return }

            Task { @MainActor in
                switch event {
                case let event as Shared.CastEventCreated:
                    if event.cafeId == self.cafeId {
                        self.loadCafeDashboard()
                    }
                case let event as Shared.CastEventUpdated:
                    if event.cafeId == self.cafeId {
                        self.loadCafeDashboard()
                    }
                case let event as Shared.CastEventDeleted:
                    if event.cafeId == self.cafeId {
                        self.loadCafeDashboard()
                    }
                default:
                    break
                }
            }
        }
    }

    func onAction(_ action: CafeDashboardAction) {
        switch action {
        case .clickBack:
            clickBack()
        case .clickShortcut(let shortcut):
            clickShortcut(shortcut)
        case .clickCastSchedule(let castId):
            clickCastSchedule(castId)
        case .clickLoadMoreCasts:
            clickLoadMoreCasts()
        case .dismissInfoMessage:
            dismissInfoMessage()
        }
    }

    init(
        cafeId: String,
        getCafeCastPageUseCase: GetCafeCastPageUseCase = KoinInitializerKt.resolveGetCafeCastPageUseCase(),
        getCafeDashboardUseCase: GetCafeDashboardUseCase = KoinInitializerKt.resolveGetCafeDashboardUseCase(),
        observeCafeDetailEventUseCase: ObserveCafeDetailEventUseCase = KoinInitializerKt.resolveObserveCafeDetailEventUseCase(),
        observeCastEventUseCase: ObserveCastEventUseCase = KoinInitializerKt.resolveObserveCastEventUseCase(),
        observeCurrentUserUseCase: ObserveCurrentUserUseCase = KoinInitializerKt.resolveObserveCurrentUserUseCase()
    ) {
        self.cafeId = cafeId
        self.getCafeCastPageUseCase = getCafeCastPageUseCase
        self.getCafeDashboardUseCase = getCafeDashboardUseCase
        self.observeCafeDetailEventUseCase = observeCafeDetailEventUseCase
        self.observeCastEventUseCase = observeCastEventUseCase
        self.observeCurrentUserUseCase = observeCurrentUserUseCase

        observeSession()
        observeCafeDetailEvent()
        observeCastEvent()
        loadCafeDashboard()
    }

    deinit {
        watchHandles.values.forEach { $0.cancel() }
        watchHandles.removeAll()
    }

    private enum WatchKey {
        case session
        case cafeDetailEvent
        case castEvent
    }
}

private extension CafeDetailEvent {
    func matches(cafeId: String) -> Bool {
        switch self {
        case let event as CafeDetailEventCafeInfoUpdated:
            return event.cafeId == cafeId
        case let event as CafeDetailEventMenuGoodsCreated:
            return event.cafeId == cafeId
        case let event as CafeDetailEventMenuGoodsUpdated:
            return event.cafeId == cafeId
        case let event as CafeDetailEventMenuGoodsDeleted:
            return event.cafeId == cafeId
        default:
            return false
        }
    }
}
