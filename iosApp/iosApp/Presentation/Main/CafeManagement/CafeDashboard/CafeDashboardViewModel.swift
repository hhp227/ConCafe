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

    private let observeCafeCastVersionUseCase: ObserveCafeCastVersionUseCase

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
                    uiState.castPreviews = append ? (uiState.castPreviews + page.items as! [CafeCastPreview]) : page.items as! [CafeCastPreview]
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
        case .castManagement:
            event.send(.navigateToCastEdit(cafeId: cafeId, castId: nil))
        default:
            uiState.infoMessage = "\(shortcut.title) 연결은 다음 단계에서 이어집니다."
        }
    }

    private func dismissInfoMessage() {
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

    private func observeCastVersion() {
        watchHandles[.castVersion]?.cancel()
        watchHandles[.castVersion] = observeCafeCastVersionUseCase.watch(cafeId: cafeId) { [weak self] _ in
            guard let self else { return }

            Task { @MainActor in
                self.refreshCastPreviews()
            }
        }
    }

    func onAction(_ action: CafeDashboardAction) {
        switch action {
        case .clickBack:
            clickBack()
        case .clickShortcut(let shortcut):
            clickShortcut(shortcut)
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
        observeCafeCastVersionUseCase: ObserveCafeCastVersionUseCase = KoinInitializerKt.resolveObserveCafeCastVersionUseCase(),
        observeCurrentUserUseCase: ObserveCurrentUserUseCase = KoinInitializerKt.resolveObserveCurrentUserUseCase()
    ) {
        self.cafeId = cafeId
        self.getCafeCastPageUseCase = getCafeCastPageUseCase
        self.getCafeDashboardUseCase = getCafeDashboardUseCase
        self.observeCafeCastVersionUseCase = observeCafeCastVersionUseCase
        self.observeCurrentUserUseCase = observeCurrentUserUseCase

        observeSession()
        observeCastVersion()
        loadCafeDashboard()
    }

    deinit {
        watchHandles.values.forEach { $0.cancel() }
        watchHandles.removeAll()
    }

    private enum WatchKey {
        case session
        case castVersion
    }
}
