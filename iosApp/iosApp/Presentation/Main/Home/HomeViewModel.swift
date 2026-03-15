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
                let result = try await getHomeFeedUseCase.invoke(popularCastCursor: nil, nearbyCafeCursor: nil)

                if let success = result as? AppResultSuccess<AnyObject> {
                    if let feed = success.data as? Shared.HomeFeed {
                        uiState = HomeUiState(
                            banners: feed.banners,
                            popularCasts: feed.popularCasts,
                            popularCastCafeNames: Self.dictionary(from: feed.popularCastCafeNames),
                            popularCastCursor: feed.popularCastsNextCursor,
                            canLoadMorePopularCasts: feed.hasMorePopularCasts,
                            isLoadingMorePopularCasts: false,
                            nearbyCafes: feed.nearbyCafes,
                            nearbyCafeCursor: feed.nearbyCafesNextCursor,
                            canLoadMoreNearbyCafes: feed.hasMoreNearbyCafes,
                            isLoadingMoreNearbyCafes: false,
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

    private func loadPopularCastPage(cursor: String?, append: Bool) {
        loadTask?.cancel()
        loadTask = Task {
            uiState = HomeUiState(
                banners: uiState.banners,
                popularCasts: uiState.popularCasts,
                popularCastCafeNames: uiState.popularCastCafeNames,
                popularCastCursor: uiState.popularCastCursor,
                canLoadMorePopularCasts: uiState.canLoadMorePopularCasts,
                isLoadingMorePopularCasts: append,
                nearbyCafes: uiState.nearbyCafes,
                nearbyCafeCursor: uiState.nearbyCafeCursor,
                canLoadMoreNearbyCafes: uiState.canLoadMoreNearbyCafes,
                isLoadingMoreNearbyCafes: uiState.isLoadingMoreNearbyCafes,
                birthdayCasts: uiState.birthdayCasts,
                notices: uiState.notices
            )

            do {
                let result = try await getHomeFeedUseCase.invoke(popularCastCursor: cursor, nearbyCafeCursor: nil)

                if let success = result as? AppResultSuccess<AnyObject>,
                   let feed = success.data as? Shared.HomeFeed {
                    uiState = HomeUiState(
                        banners: uiState.banners,
                        popularCasts: append ? (uiState.popularCasts + feed.popularCasts) : feed.popularCasts,
                        popularCastCafeNames: append
                            ? uiState.popularCastCafeNames.merging(Self.dictionary(from: feed.popularCastCafeNames)) { _, new in new }
                            : Self.dictionary(from: feed.popularCastCafeNames),
                        popularCastCursor: feed.popularCastsNextCursor,
                        canLoadMorePopularCasts: feed.hasMorePopularCasts,
                        isLoadingMorePopularCasts: false,
                        nearbyCafes: uiState.nearbyCafes,
                        nearbyCafeCursor: uiState.nearbyCafeCursor,
                        canLoadMoreNearbyCafes: uiState.canLoadMoreNearbyCafes,
                        isLoadingMoreNearbyCafes: uiState.isLoadingMoreNearbyCafes,
                        birthdayCasts: uiState.birthdayCasts,
                        notices: uiState.notices
                    )
                } else {
                    uiState = HomeUiState(
                        banners: uiState.banners,
                        popularCasts: uiState.popularCasts,
                        popularCastCafeNames: uiState.popularCastCafeNames,
                        popularCastCursor: uiState.popularCastCursor,
                        canLoadMorePopularCasts: uiState.canLoadMorePopularCasts,
                        isLoadingMorePopularCasts: false,
                        nearbyCafes: uiState.nearbyCafes,
                        nearbyCafeCursor: uiState.nearbyCafeCursor,
                        canLoadMoreNearbyCafes: uiState.canLoadMoreNearbyCafes,
                        isLoadingMoreNearbyCafes: uiState.isLoadingMoreNearbyCafes,
                        birthdayCasts: uiState.birthdayCasts,
                        notices: uiState.notices
                    )
                }
            } catch {
                if Task.isCancelled { return }
                uiState = HomeUiState(
                    banners: uiState.banners,
                    popularCasts: uiState.popularCasts,
                    popularCastCafeNames: uiState.popularCastCafeNames,
                    popularCastCursor: uiState.popularCastCursor,
                    canLoadMorePopularCasts: uiState.canLoadMorePopularCasts,
                    isLoadingMorePopularCasts: false,
                    nearbyCafes: uiState.nearbyCafes,
                    nearbyCafeCursor: uiState.nearbyCafeCursor,
                    canLoadMoreNearbyCafes: uiState.canLoadMoreNearbyCafes,
                    isLoadingMoreNearbyCafes: uiState.isLoadingMoreNearbyCafes,
                    birthdayCasts: uiState.birthdayCasts,
                    notices: uiState.notices
                )
            }
        }
    }

    private func loadMorePopularCasts() {
        guard uiState.canLoadMorePopularCasts,
              !uiState.isLoadingMorePopularCasts,
              let cursor = uiState.popularCastCursor else { return }
        loadPopularCastPage(cursor: cursor, append: true)
    }

    private func loadNearbyCafePage(cursor: String?, append: Bool) {
        loadTask?.cancel()
        loadTask = Task {
            uiState = HomeUiState(
                banners: uiState.banners,
                popularCasts: uiState.popularCasts,
                popularCastCafeNames: uiState.popularCastCafeNames,
                popularCastCursor: uiState.popularCastCursor,
                canLoadMorePopularCasts: uiState.canLoadMorePopularCasts,
                isLoadingMorePopularCasts: uiState.isLoadingMorePopularCasts,
                nearbyCafes: uiState.nearbyCafes,
                nearbyCafeCursor: uiState.nearbyCafeCursor,
                canLoadMoreNearbyCafes: uiState.canLoadMoreNearbyCafes,
                isLoadingMoreNearbyCafes: append,
                birthdayCasts: uiState.birthdayCasts,
                notices: uiState.notices
            )

            do {
                let result = try await getHomeFeedUseCase.invoke(popularCastCursor: nil, nearbyCafeCursor: cursor)

                if let success = result as? AppResultSuccess<AnyObject>,
                   let feed = success.data as? Shared.HomeFeed {
                    uiState = HomeUiState(
                        banners: uiState.banners,
                        popularCasts: uiState.popularCasts,
                        popularCastCafeNames: uiState.popularCastCafeNames,
                        popularCastCursor: uiState.popularCastCursor,
                        canLoadMorePopularCasts: uiState.canLoadMorePopularCasts,
                        isLoadingMorePopularCasts: uiState.isLoadingMorePopularCasts,
                        nearbyCafes: append ? (uiState.nearbyCafes + feed.nearbyCafes) : feed.nearbyCafes,
                        nearbyCafeCursor: feed.nearbyCafesNextCursor,
                        canLoadMoreNearbyCafes: feed.hasMoreNearbyCafes,
                        isLoadingMoreNearbyCafes: false,
                        birthdayCasts: uiState.birthdayCasts,
                        notices: uiState.notices
                    )
                } else {
                    uiState = HomeUiState(
                        banners: uiState.banners,
                        popularCasts: uiState.popularCasts,
                        popularCastCafeNames: uiState.popularCastCafeNames,
                        popularCastCursor: uiState.popularCastCursor,
                        canLoadMorePopularCasts: uiState.canLoadMorePopularCasts,
                        isLoadingMorePopularCasts: uiState.isLoadingMorePopularCasts,
                        nearbyCafes: uiState.nearbyCafes,
                        nearbyCafeCursor: uiState.nearbyCafeCursor,
                        canLoadMoreNearbyCafes: uiState.canLoadMoreNearbyCafes,
                        isLoadingMoreNearbyCafes: false,
                        birthdayCasts: uiState.birthdayCasts,
                        notices: uiState.notices
                    )
                }
            } catch {
                if Task.isCancelled { return }
                uiState = HomeUiState(
                    banners: uiState.banners,
                    popularCasts: uiState.popularCasts,
                    popularCastCafeNames: uiState.popularCastCafeNames,
                    popularCastCursor: uiState.popularCastCursor,
                    canLoadMorePopularCasts: uiState.canLoadMorePopularCasts,
                    isLoadingMorePopularCasts: uiState.isLoadingMorePopularCasts,
                    nearbyCafes: uiState.nearbyCafes,
                    nearbyCafeCursor: uiState.nearbyCafeCursor,
                    canLoadMoreNearbyCafes: uiState.canLoadMoreNearbyCafes,
                    isLoadingMoreNearbyCafes: false,
                    birthdayCasts: uiState.birthdayCasts,
                    notices: uiState.notices
                )
            }
        }
    }

    private func loadMoreNearbyCafes() {
        guard uiState.canLoadMoreNearbyCafes,
              !uiState.isLoadingMoreNearbyCafes,
              let cursor = uiState.nearbyCafeCursor else { return }
        loadNearbyCafePage(cursor: cursor, append: true)
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
            popularCastCafeNames: uiState.popularCastCafeNames.merging([cafe.id: cafe.name]) { _, new in new },
            popularCastCursor: uiState.popularCastCursor,
            canLoadMorePopularCasts: uiState.canLoadMorePopularCasts,
            isLoadingMorePopularCasts: uiState.isLoadingMorePopularCasts,
            nearbyCafes: uiState.nearbyCafes.map { item in
                item.id == cafe.id ? cafe : item
            },
            nearbyCafeCursor: uiState.nearbyCafeCursor,
            canLoadMoreNearbyCafes: uiState.canLoadMoreNearbyCafes,
            isLoadingMoreNearbyCafes: uiState.isLoadingMoreNearbyCafes,
            birthdayCasts: uiState.birthdayCasts,
            notices: uiState.notices
        )
    }

    private func patchCast(_ cast: Cast) {
        uiState = HomeUiState(
            banners: uiState.banners,
            popularCasts: uiState.popularCasts.map { $0.id == cast.id ? cast : $0 },
            popularCastCafeNames: uiState.popularCastCafeNames,
            popularCastCursor: uiState.popularCastCursor,
            canLoadMorePopularCasts: uiState.canLoadMorePopularCasts,
            isLoadingMorePopularCasts: uiState.isLoadingMorePopularCasts,
            nearbyCafes: uiState.nearbyCafes,
            nearbyCafeCursor: uiState.nearbyCafeCursor,
            canLoadMoreNearbyCafes: uiState.canLoadMoreNearbyCafes,
            isLoadingMoreNearbyCafes: uiState.isLoadingMoreNearbyCafes,
            birthdayCasts: uiState.birthdayCasts.map { $0.id == cast.id ? cast : $0 },
            notices: uiState.notices
        )
    }

    private func removeCast(_ castId: String) {
        uiState = HomeUiState(
            banners: uiState.banners,
            popularCasts: uiState.popularCasts.filter { $0.id != castId },
            popularCastCafeNames: uiState.popularCastCafeNames,
            popularCastCursor: uiState.popularCastCursor,
            canLoadMorePopularCasts: uiState.canLoadMorePopularCasts,
            isLoadingMorePopularCasts: uiState.isLoadingMorePopularCasts,
            nearbyCafes: uiState.nearbyCafes,
            nearbyCafeCursor: uiState.nearbyCafeCursor,
            canLoadMoreNearbyCafes: uiState.canLoadMoreNearbyCafes,
            isLoadingMoreNearbyCafes: uiState.isLoadingMoreNearbyCafes,
            birthdayCasts: uiState.birthdayCasts.filter { $0.id != castId },
            notices: uiState.notices
        )
    }

    func onAction(_ action: HomeAction) {
        switch action {
        case .bannerTapped(let banner):
            handleBannerTap(banner)
        case .maidTapped(let id):
            event.send(.navigateToCast(id: id))
        case .birthdayMaidTapped(let id):
            event.send(.navigateToCast(id: id))
        case .cafeTapped(let id):
            event.send(.navigateToCafe(id: id))
        case .loadMorePopularCasts:
            loadMorePopularCasts()
        case .loadMoreNearbyCafes:
            loadMoreNearbyCafes()
        }
    }

    private func handleBannerTap(_ banner: HomeBanner) {
        switch banner.targetType {
        case .externalLink:
            if !banner.targetValue.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                event.send(.navigateToExternalLink(title: banner.title, url: banner.targetValue))
            }
        case .cafeDetail, .eventDetail, .notice:
            let cafeId = banner.cafeId ?? (banner.targetType == .cafeDetail ? banner.targetValue : nil)
            if let cafeId, !cafeId.isEmpty {
                event.send(.navigateToCafe(id: cafeId))
            }
        default:
            break
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

    private static func dictionary(from source: [AnyHashable: Any]) -> [String: String] {
        source.reduce(into: [:]) { partialResult, entry in
            guard let key = entry.key as? String, let value = entry.value as? String else { return }
            partialResult[key] = value
        }
    }

    private enum WatchKey {
        case bannerEvent
        case cafeDetailEvent
        case castEvent
    }
}
