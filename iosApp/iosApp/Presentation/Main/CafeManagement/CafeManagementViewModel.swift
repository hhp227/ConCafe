//
//  CafeManagementViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import Foundation
import Combine
import Shared

@MainActor
final class CafeManagementViewModel: ObservableObject {
    private let getCafeManagementUseCase: GetCafeManagementUseCase

    private let observeCurrentUserUseCase: ObserveCurrentUserUseCase

    @Published private(set) var uiState = CafeManagementUiState()

    let event = PassthroughSubject<CafeManagementEvent, Never>()

    private var sessionWatchHandle: WatchHandle?

    private func loadCafeManagement() {
        Task {
            do {
                let result = try await getCafeManagementUseCase.invoke()

                if let success = result as? AppResultSuccess<AnyObject>,
                   let data = success.data as? CafeManagementData {
                    uiState.ownedCafes = data.ownedCafes
                    uiState.searchableCafes = data.searchableCafes
                    uiState.pendingClaims = data.pendingClaims
                } else if let failure = result as? AppResultFailure {
                    uiState.ownedCafes = []
                    uiState.searchableCafes = []
                    uiState.pendingClaims = []
                    uiState.infoMessage = "\(failure.error)"
                }
            } catch {
                uiState.ownedCafes = []
                uiState.searchableCafes = []
                uiState.pendingClaims = []
                uiState.infoMessage = error.localizedDescription
            }
        }
    }

    private func clickCafe(_ cafeId: String) {
        event.send(.navigateToCafeDashboard(cafeId: cafeId))
    }

    private func clickCafeDetail(_ cafeId: String) {
        event.send(.navigateToCafe(cafeId: cafeId))
    }

    private func changeCafeSearchQuery(_ query: String) {
        uiState.cafeSearchQuery = query
        uiState.infoMessage = nil
    }

    private func clickClaimCafe(_ cafeId: String) {
        let cafeName = uiState.searchableCafes.first(where: { $0.id == cafeId })?.name ?? "선택한 카페"
        uiState.infoMessage = "\(cafeName) 운영자 신청 연결은 다음 단계에서 이어집니다."
    }

    private func toggleCafeListExpanded() {
        uiState.isShowingAllCafes.toggle()
    }

    private func clickCreateCafe() {
        uiState.infoMessage = "새 카페 등록 플로우는 다음 단계에서 연결됩니다."
    }

    private func dismissInfoMessage() {
        uiState.infoMessage = nil
    }

    private func observeSession() {
        sessionWatchHandle = observeCurrentUserUseCase.watch { [weak self] _ in
            guard let self else { return }

            Task { @MainActor in
                self.loadCafeManagement()
            }
        }
    }

    func onAction(_ action: CafeManagementAction) {
        switch action {
        case .clickCafe(let cafeId):
            clickCafe(cafeId)
        case .clickCafeDetail(let cafeId):
            clickCafeDetail(cafeId)
        case .changeCafeSearchQuery(let query):
            changeCafeSearchQuery(query)
        case .clickClaimCafe(let cafeId):
            clickClaimCafe(cafeId)
        case .toggleCafeListExpanded:
            toggleCafeListExpanded()
        case .clickCreateCafe:
            clickCreateCafe()
        case .dismissInfoMessage:
            dismissInfoMessage()
        }
    }

    init(
        getCafeManagementUseCase: GetCafeManagementUseCase = KoinInitializerKt.resolveGetCafeManagementUseCase(),
        observeCurrentUserUseCase: ObserveCurrentUserUseCase = KoinInitializerKt.resolveObserveCurrentUserUseCase()
    ) {
        self.getCafeManagementUseCase = getCafeManagementUseCase
        self.observeCurrentUserUseCase = observeCurrentUserUseCase

        observeSession()
        loadCafeManagement()
    }

    deinit {
        sessionWatchHandle?.cancel()
    }
}
