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

    private let getCafeDashboardUseCase: GetCafeDashboardUseCase

    private let observeCurrentUserUseCase: ObserveCurrentUserUseCase

    @Published private(set) var uiState = CafeDashboardUiState()

    let event = PassthroughSubject<CafeDashboardEvent, Never>()

    private var sessionWatchHandle: WatchHandle?

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
                } else if let failure = result as? AppResultFailure {
                    uiState.cafe = nil
                    uiState.isLoading = false
                    uiState.infoMessage = "\(failure.error)"
                }
            } catch {
                uiState.cafe = nil
                uiState.isLoading = false
                uiState.infoMessage = error.localizedDescription
            }
        }
    }

    private func clickBack() {
        event.send(.navigateBack)
    }

    private func clickShortcut(_ shortcut: CafeDashboardShortcut) {
        if shortcut == .cafeSettings {
            event.send(.navigateToCafeInfoEdit(cafeId: cafeId))
        } else {
            uiState.infoMessage = "\(shortcut.title) 연결은 다음 단계에서 이어집니다."
        }
    }

    private func dismissInfoMessage() {
        uiState.infoMessage = nil
    }

    private func observeSession() {
        sessionWatchHandle = observeCurrentUserUseCase.watch { [weak self] _ in
            guard let self else { return }

            Task { @MainActor in
                self.loadCafeDashboard()
            }
        }
    }

    func onAction(_ action: CafeDashboardAction) {
        switch action {
        case .clickBack:
            clickBack()
        case .clickShortcut(let shortcut):
            clickShortcut(shortcut)
        case .dismissInfoMessage:
            dismissInfoMessage()
        }
    }

    init(
        cafeId: String,
        getCafeDashboardUseCase: GetCafeDashboardUseCase = KoinInitializerKt.resolveGetCafeDashboardUseCase(),
        observeCurrentUserUseCase: ObserveCurrentUserUseCase = KoinInitializerKt.resolveObserveCurrentUserUseCase()
    ) {
        self.cafeId = cafeId
        self.getCafeDashboardUseCase = getCafeDashboardUseCase
        self.observeCurrentUserUseCase = observeCurrentUserUseCase

        observeSession()
        loadCafeDashboard()
    }

    deinit {
        sessionWatchHandle?.cancel()
    }
}
