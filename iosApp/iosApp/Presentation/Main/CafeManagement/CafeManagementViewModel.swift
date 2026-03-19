//
//  CafeManagementViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import Foundation
import Combine
import Shared
import KMPNativeCoroutinesAsync

@MainActor
final class CafeManagementViewModel: ObservableObject {
    private let createCafeOwnerClaimUseCase: CreateCafeOwnerClaimUseCase

    private let getCafeManagementUseCase: GetCafeManagementUseCase

    private let observeCurrentUserUseCase: ObserveCurrentUserUseCase

    private let cafeRegistrationClaimEventPublisher: CafeRegistrationClaimEventPublisher

    private let cafeDetailEventPublisher: CafeDetailEventPublisher

    @Published private(set) var uiState = CafeManagementUiState()

    let event = PassthroughSubject<CafeManagementEvent, Never>()

    private var tasks: [TaskKey: Task<Void, Never>] = [:]
    private var currentUserId: String?

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
        Task {
            do {
                let result = try await createCafeOwnerClaimUseCase.invoke(cafeId: cafeId)

                if result is AppResultSuccess<AnyObject> {
                    loadCafeManagement()
                    uiState.infoMessage = "\(cafeName) 운영자 신청을 등록했습니다."
                } else if let failure = result as? AppResultFailure {
                    uiState.infoMessage = "\(failure.error)"
                }
            } catch {
                uiState.infoMessage = error.localizedDescription
            }
        }
    }

    private func toggleCafeListExpanded() {
        uiState.isShowingAllCafes.toggle()
    }

    private func clickCreateCafe() {
        event.send(.navigateToCafeInfoRegistration)
    }

    private func dismissInfoMessage() {
        uiState.infoMessage = nil
    }

    private func observeSession() {
        tasks[.session]?.cancel()
        tasks[.session] = Task {
            do {
                for try await user in asyncSequence(for: observeCurrentUserUseCase.invoke()) {
                    self.currentUserId = user?.id
                    self.loadCafeManagement()
                }
            } catch {
                print("Error: \(error)")
            }
        }
    }

    private func observeCafeDetailEvent() {
        tasks[.cafeDetailEvent]?.cancel()
        tasks[.cafeDetailEvent] = Task {
            do {
                for try await event in asyncSequence(for: cafeDetailEventPublisher.events) {
                    if let updated = event as? CafeDetailEvent.CafeInfoUpdated {
                        self.patchCafeInfo(updated.cafe)
                    }
                }
            } catch {
                print("Error: \(error)")
            }
        }
    }

    private func observeCafeRegistrationClaimEvent() {
        tasks[.cafeRegistrationClaimEvent]?.cancel()
        tasks[.cafeRegistrationClaimEvent] = Task {
            do {
                for try await event in asyncSequence(for: cafeRegistrationClaimEventPublisher.events) {
                    guard let currentUserId = self.currentUserId else { return }
                    let shouldRefresh: Bool
                    switch event {
                    case let created as CafeRegistrationClaimEvent.Created:
                        shouldRefresh = created.requesterUserId == currentUserId
                    case let approved as CafeRegistrationClaimEvent.Approved:
                        shouldRefresh = approved.requesterUserId == currentUserId
                    case let rejected as CafeRegistrationClaimEvent.Rejected:
                        shouldRefresh = rejected.requesterUserId == currentUserId
                    default:
                        shouldRefresh = false
                    }
                    if shouldRefresh {
                        self.loadCafeManagement()
                    }
                }
            } catch {
                print("Error: \(error)")
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
                location: "\(cafe.region.city) \(cafe.region.address)"
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
        createCafeOwnerClaimUseCase: CreateCafeOwnerClaimUseCase = KoinInitializerKt.resolveCreateCafeOwnerClaimUseCase(),
        getCafeManagementUseCase: GetCafeManagementUseCase = KoinInitializerKt.resolveGetCafeManagementUseCase(),
        observeCurrentUserUseCase: ObserveCurrentUserUseCase = KoinInitializerKt.resolveObserveCurrentUserUseCase(),
        cafeRegistrationClaimEventPublisher: CafeRegistrationClaimEventPublisher = KoinInitializerKt.resolveCafeRegistrationClaimEventPublisher(),
        cafeDetailEventPublisher: CafeDetailEventPublisher = KoinInitializerKt.resolveCafeDetailEventPublisher()
    ) {
        self.createCafeOwnerClaimUseCase = createCafeOwnerClaimUseCase
        self.getCafeManagementUseCase = getCafeManagementUseCase
        self.observeCurrentUserUseCase = observeCurrentUserUseCase
        self.cafeRegistrationClaimEventPublisher = cafeRegistrationClaimEventPublisher
        self.cafeDetailEventPublisher = cafeDetailEventPublisher

        observeSession()
        observeCafeDetailEvent()
        observeCafeRegistrationClaimEvent()
        loadCafeManagement()
    }

    deinit {
        tasks.values.forEach { $0.cancel() }
        tasks.removeAll()
    }

    private enum TaskKey {
        case session
        case cafeDetailEvent
        case cafeRegistrationClaimEvent
    }
}
