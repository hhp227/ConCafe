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

    private let observeCafeDetailEventUseCase: ObserveCafeDetailEventUseCase

    private let observeCurrentUserUseCase: ObserveCurrentUserUseCase

    @Published private(set) var uiState = CafeManagementUiState()

    let event = PassthroughSubject<CafeManagementEvent, Never>()

    private var watchHandles: [WatchKey: WatchHandle] = [:]

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
        watchHandles[.session]?.cancel()
        watchHandles[.session] = observeCurrentUserUseCase.watch { [weak self] _ in
            guard let self else { return }

            Task { @MainActor in
                self.loadCafeManagement()
            }
        }
    }

    private func observeCafeDetailEvent() {
        watchHandles[.cafeDetailEvent]?.cancel()
        watchHandles[.cafeDetailEvent] = observeCafeDetailEventUseCase.watch { [weak self] event in
            guard let self else { return }
            Task { @MainActor in
                if let updated = event as? CafeDetailEvent.CafeInfoUpdated {
                    self.patchCafeInfo(updated.cafe)
                }
            }
        }
    }

    private func patchCafeInfo(_ cafe: Cafe) {
        uiState.ownedCafes = uiState.ownedCafes.map { item in
            guard item.id == cafe.id else { return item }
            return CafeManagementData.OwnedCafeSummary(
                id: item.id,
                name: cafe.name,
                city: cafe.region.city,
                isApproved: item.isApproved,
                todayVisitors: item.todayVisitors,
                todayCheckIns: item.todayCheckIns,
                todayReviews: item.todayReviews,
                rating: cafe.ratingAvg,
                castCount: item.castCount,
                noticeCount: item.noticeCount,
                externalLinkCount: item.externalLinkCount
            )
        }
        uiState.searchableCafes = uiState.searchableCafes.map { item in
            guard item.id == cafe.id else { return item }
            return CafeManagementData.SearchableCafeSummary(
                id: item.id,
                name: cafe.name,
                location: cafe.region.address
            )
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
        observeCafeDetailEventUseCase: ObserveCafeDetailEventUseCase = KoinInitializerKt.resolveObserveCafeDetailEventUseCase(),
        observeCurrentUserUseCase: ObserveCurrentUserUseCase = KoinInitializerKt.resolveObserveCurrentUserUseCase()
    ) {
        self.getCafeManagementUseCase = getCafeManagementUseCase
        self.observeCafeDetailEventUseCase = observeCafeDetailEventUseCase
        self.observeCurrentUserUseCase = observeCurrentUserUseCase

        observeSession()
        observeCafeDetailEvent()
        loadCafeManagement()
    }

    deinit {
        watchHandles.values.forEach { $0.cancel() }
        watchHandles.removeAll()
    }

    private enum WatchKey {
        case session
        case cafeDetailEvent
    }
}
