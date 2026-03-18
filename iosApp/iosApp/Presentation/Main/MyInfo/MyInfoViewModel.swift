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

    private let observeCafeDetailEventUseCase: ObserveCafeDetailEventUseCase

    private let observeCastEventUseCase: ObserveCastEventUseCase

    @Published private(set) var uiState = MyInfoUiState.empty

    let event = PassthroughSubject<MyInfoEvent, Never>()

    private var loadTask: Task<Void, Never>?

    private var watchHandles: [WatchKey: WatchHandle] = [:]

    private func observeSession() {
        watchHandles[.session]?.cancel()
        watchHandles[.session] = observeCurrentUserUseCase.watch { [weak self] _ in
            guard let self else { return }

            Task { @MainActor in
                self.loadMyInfo()
            }
        }
    }

    private func observeCafeDetailEvent() {
        watchHandles[.cafeDetailEvent]?.cancel()
        watchHandles[.cafeDetailEvent] = observeCafeDetailEventUseCase.watch { [weak self] event in
            guard let self else { return }
            Task { @MainActor in
                if let updated = event as? CafeDetailEvent.CafeInfoUpdated {
                    self.patchCafe(updated.cafe)
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
                case let updated as Shared.CastEvent.Updated:
                    self.patchCast(updated.cast)
                case let deleted as Shared.CastEvent.Deleted:
                    self.removeCast(deleted.castId)
                default:
                    break
                }
            }
        }
    }

    private func loadMyInfo() {
        loadTask?.cancel()
        uiState.isLoading = true
        uiState.errorMessage = nil

        loadTask = Task {
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
        observeCafeDetailEventUseCase: ObserveCafeDetailEventUseCase = KoinInitializerKt.resolveObserveCafeDetailEventUseCase(),
        observeCastEventUseCase: ObserveCastEventUseCase = KoinInitializerKt.resolveObserveCastEventUseCase()
    ) {
        self.getMyInfoUseCase = getMyInfoUseCase
        self.observeCurrentUserUseCase = observeCurrentUserUseCase
        self.observeCafeDetailEventUseCase = observeCafeDetailEventUseCase
        self.observeCastEventUseCase = observeCastEventUseCase

        observeSession()
        observeCafeDetailEvent()
        observeCastEvent()
    }

    deinit {
        loadTask?.cancel()
        watchHandles.values.forEach { $0.cancel() }
        watchHandles.removeAll()
    }

    private enum WatchKey {
        case session
        case cafeDetailEvent
        case castEvent
    }
}
