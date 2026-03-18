//
//  MyInfoViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/06.
//

import Foundation
import Combine
import Shared

@MainActor
final class MyInfoViewModel: ObservableObject {
    private let getMyInfoUseCase: GetMyInfoUseCase

    private let observeCurrentUserUseCase: ObserveCurrentUserUseCase

    private let cafeDetailEventPublisher: CafeDetailEventPublisher

    private let castEventPublisher: CastEventPublisher

    @Published private(set) var uiState = MyInfoUiState.empty

    let event = PassthroughSubject<MyInfoEvent, Never>()

    private var tasks: [TaskKey: Task<Void, Never>] = [:]

    private func observeSession() {
        tasks[.session]?.cancel()
        tasks[.session] = Task {
            
        }
        /*tasks[.session] = observeCurrentUserUseCase.watch { [weak self] _ in
            guard let self else { return }

            Task { @MainActor in
                self.loadMyInfo()
            }
        }*/
    }
    
    private func loadMyInfo() {
        uiState.isLoading = true
        uiState.errorMessage = nil

        Task {
            do {
                let result = try await getMyInfoUseCase.invoke()

                if let success = result as? AppResultSuccess<AnyObject>,
                let feed = success.data as? Shared.MyInfoFeed {
                    uiState = MyInfoUiState(
                        isLoading: false,
                        errorMessage: nil,
                        isLoggedIn: feed.isLoggedIn,
                        user: feed.user,
                        summary: feed.summary,
                        castDetail: feed.castDetail,
                        ownedCafes: feed.ownedCafes,
                        badges: feed.badges,
                        popularCafes: feed.popularCafes,
                        recentVisits: feed.recentVisits,
                        favorites: feed.favorites,
                        followedMaids: feed.followedMaids
                    )
                } else if let failure = result as? AppResultFailure {
                    uiState = .empty
                    uiState.errorMessage = "\(failure.error)"
                } else {
                    uiState = .empty
                }
            } catch {
                if Task.isCancelled { return }
                uiState = .empty
                uiState.errorMessage = error.localizedDescription
            }
        }
    }

    private func observeCafeDetailEvent() {
        tasks[.cafeDetailEvent]?.cancel()
        tasks[.cafeDetailEvent] = Task {
            
        }
        /*tasks[.cafeDetailEvent] = observeCafeDetailEventUseCase.watch { [weak self] event in
            guard let self else { return }
            Task { @MainActor in
                if let updated = event as? CafeDetailEvent.CafeInfoUpdated {
                    self.patchCafe(updated.cafe)
                }
            }
        }*/
    }

    private func observeCastEvent() {
        tasks[.castEvent]?.cancel()
        tasks[.castEvent] = Task {
            
        }
        /*tasks[.castEvent] = observeCastEventUseCase.watch { [weak self] event in
            guard let self else { return }
            Task { @MainActor in
                switch event {
                case let updated as Shared.CastEvent.Updated:
                    self.patchCast(updated.cast)
                case let deleted as Shared.CastEvent.Deleted:
                    self.removeCast(deleted.castId)
                default:
                    break
                }
            }
        }*/
    }

    private func patchCafe(_ cafe: Cafe) {
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
        uiState.popularCafes = uiState.popularCafes.map { $0.id == cafe.id ? cafe : $0 }
        uiState.recentVisits = uiState.recentVisits.map { $0.id == cafe.id ? cafe : $0 }
        uiState.favorites = uiState.favorites.map { $0.id == cafe.id ? cafe : $0 }
        if let detail = uiState.castDetail, detail.cafe.id == cafe.id {
            uiState.castDetail = CastDetail(
                cast: detail.cast,
                cafe: cafe,
                images: detail.images,
                schedule: detail.schedule
            )
        }
    }

    private func patchCast(_ cast: Cast) {
        uiState.followedMaids = uiState.followedMaids.map { $0.id == cast.id ? cast : $0 }
        if let detail = uiState.castDetail, detail.cast.id == cast.id {
            uiState.castDetail = CastDetail(
                cast: cast,
                cafe: detail.cafe,
                images: detail.images,
                schedule: detail.schedule
            )
        }
    }

    private func removeCast(_ castId: String) {
        uiState.followedMaids.removeAll { $0.id == castId }
        if uiState.castDetail?.cast.id == castId {
            uiState.castDetail = nil
        }
    }

    func onAction(_ action: MyInfoAction) {
        switch action {
        case .cafeTapped(let id):
            event.send(.navigateToCafe(id: id))
        case .maidTapped(let id):
            event.send(.navigateToCast(id: id))
        case .signInTapped:
            event.send(.navigateToSignIn)
        case .refresh:
            loadMyInfo()
        }
    }

    init(
        getMyInfoUseCase: GetMyInfoUseCase = KoinInitializerKt.resolveGetMyInfoUseCase(),
        observeCurrentUserUseCase: ObserveCurrentUserUseCase = KoinInitializerKt.resolveObserveCurrentUserUseCase(),
        cafeDetailEventPublisher: CafeDetailEventPublisher = KoinInitializerKt.resolveCafeDetailEventPublisher(),
        castEventPublisher: CastEventPublisher = KoinInitializerKt.resolveCastEventPublisher()
    ) {
        self.getMyInfoUseCase = getMyInfoUseCase
        self.observeCurrentUserUseCase = observeCurrentUserUseCase
        self.cafeDetailEventPublisher = cafeDetailEventPublisher
        self.castEventPublisher = castEventPublisher

        observeSession()
        observeCafeDetailEvent()
        observeCastEvent()
    }

    deinit {
        tasks.values.forEach { $0.cancel() }
        tasks.removeAll()
    }

    private enum TaskKey {
        case session
        case cafeDetailEvent
        case castEvent
    }
}
