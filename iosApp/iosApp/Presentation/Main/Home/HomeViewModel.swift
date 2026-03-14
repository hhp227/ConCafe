//
//  HomeViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/06.
//

import Foundation
import Combine
import Shared

@MainActor
final class HomeViewModel: ObservableObject {
    private let getHomeFeedUseCase: GetHomeFeedUseCase

    private let observeBannerEventUseCase: ObserveBannerEventUseCase

    private let observeCafeDetailEventUseCase: ObserveCafeDetailEventUseCase

    private let observeCastEventUseCase: ObserveCastEventUseCase
    
    @Published private(set) var uiState = HomeUiState.empty
    
    let event = PassthroughSubject<HomeEvent, Never>()
    
    private var loadTask: Task<Void, Never>?

    private var watchHandles: [WatchKey: WatchHandle] = [:]

    private func loadHomeFeed() {
        loadTask?.cancel()
        loadTask = Task {
            do {
                let result = try await getHomeFeedUseCase.invoke(nearbyCafeCursor: nil)

                if let success = result as? AppResultSuccess<AnyObject> {
                    if let feed = success.data as? Shared.HomeFeed {
                        uiState = HomeUiState(
                            banners: feed.banners,
                            popularCasts: feed.popularCasts,
                            nearbyCafes: feed.nearbyCafes,
                            nearbyCafeCursor: feed.nearbyCafesNextCursor,
                            canLoadMoreNearbyCafes: feed.hasMoreNearbyCafes,
                            birthdayCasts: feed.birthdayCasts,
                            notices: feed.notices
                        )
                    } else {
                        uiState = .empty
                    }
                } else if result is AppResultFailure {
                    uiState = .empty
                } else {
                    uiState = .empty
                }
            } catch {
                uiState = .empty
            }
        }
    }

    private func loadMoreNearbyCafes() async {
        let cursor = uiState.nearbyCafeCursor
        let canLoadMoreNearbyCafes = uiState.canLoadMoreNearbyCafes

        if let cursor, canLoadMoreNearbyCafes {
            do {
                let result = try await getHomeFeedUseCase.invoke(nearbyCafeCursor: cursor)

                if let success = result as? AppResultSuccess<AnyObject>,
                   let feed = success.data as? Shared.HomeFeed {
                    uiState = HomeUiState(
                        banners: uiState.banners,
                        popularCasts: uiState.popularCasts,
                        nearbyCafes: uiState.nearbyCafes + feed.nearbyCafes,
                        nearbyCafeCursor: feed.nearbyCafesNextCursor,
                        canLoadMoreNearbyCafes: feed.hasMoreNearbyCafes,
                        birthdayCasts: uiState.birthdayCasts,
                        notices: uiState.notices
                    )
                }
            } catch {
                if Task.isCancelled { return }
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

    private func observeBannerEvent() {
        watchHandles[.bannerEvent]?.cancel()
        watchHandles[.bannerEvent] = observeBannerEventUseCase.watch { [weak self] _ in
            guard let self else { return }
            Task { @MainActor in
                self.loadHomeFeed()
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

    private func patchCafeInfo(_ cafe: Cafe) {
        uiState = HomeUiState(
            banners: uiState.banners,
            popularCasts: uiState.popularCasts,
            nearbyCafes: uiState.nearbyCafes.map { item in
                item.id == cafe.id ? cafe : item
            },
            nearbyCafeCursor: uiState.nearbyCafeCursor,
            canLoadMoreNearbyCafes: uiState.canLoadMoreNearbyCafes,
            birthdayCasts: uiState.birthdayCasts,
            notices: uiState.notices
        )
    }

    private func patchCast(_ cast: Cast) {
        uiState = HomeUiState(
            banners: uiState.banners,
            popularCasts: uiState.popularCasts.map { $0.id == cast.id ? cast : $0 },
            nearbyCafes: uiState.nearbyCafes,
            nearbyCafeCursor: uiState.nearbyCafeCursor,
            canLoadMoreNearbyCafes: uiState.canLoadMoreNearbyCafes,
            birthdayCasts: uiState.birthdayCasts.map { $0.id == cast.id ? cast : $0 },
            notices: uiState.notices
        )
    }

    private func removeCast(_ castId: String) {
        uiState = HomeUiState(
            banners: uiState.banners,
            popularCasts: uiState.popularCasts.filter { $0.id != castId },
            nearbyCafes: uiState.nearbyCafes,
            nearbyCafeCursor: uiState.nearbyCafeCursor,
            canLoadMoreNearbyCafes: uiState.canLoadMoreNearbyCafes,
            birthdayCasts: uiState.birthdayCasts.filter { $0.id != castId },
            notices: uiState.notices
        )
    }

    func onAction(_ action: HomeAction) {
        switch action {
        case .maidTapped(let id):
            event.send(.navigateToCast(id: id))
        case .birthdayMaidTapped(let id):
            event.send(.navigateToCast(id: id))
        case .cafeTapped(let id):
            event.send(.navigateToCafe(id: id))
        case .loadMoreNearbyCafes:
            if uiState.canLoadMoreNearbyCafes {
                loadTask?.cancel()
                loadTask = Task {
                    await loadMoreNearbyCafes()
                }
            }
        }
    }

    init(
        getHomeFeedUseCase: GetHomeFeedUseCase = KoinInitializerKt.resolveGetHomeFeedUseCase(),
        observeBannerEventUseCase: ObserveBannerEventUseCase = KoinInitializerKt.resolveObserveBannerEventUseCase(),
        observeCafeDetailEventUseCase: ObserveCafeDetailEventUseCase = KoinInitializerKt.resolveObserveCafeDetailEventUseCase(),
        observeCastEventUseCase: ObserveCastEventUseCase = KoinInitializerKt.resolveObserveCastEventUseCase()
    ) {
        self.getHomeFeedUseCase = getHomeFeedUseCase
        self.observeBannerEventUseCase = observeBannerEventUseCase
        self.observeCafeDetailEventUseCase = observeCafeDetailEventUseCase
        self.observeCastEventUseCase = observeCastEventUseCase
        
        observeBannerEvent()
        observeCafeDetailEvent()
        observeCastEvent()
        loadHomeFeed()
    }
    
    deinit {
        loadTask?.cancel()
        watchHandles.values.forEach { $0.cancel() }
        watchHandles.removeAll()
    }

    private enum WatchKey {
        case bannerEvent
        case cafeDetailEvent
        case castEvent
    }
}
