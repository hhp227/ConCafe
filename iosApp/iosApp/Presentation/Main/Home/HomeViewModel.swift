//
//  HomeViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/06.
//

import Foundation
import Combine
import Shared
import KMPNativeCoroutinesAsync

@MainActor
final class HomeViewModel: ObservableObject {
    private let getHomeFeedUseCase: GetHomeFeedUseCase

    private let getNearbyCafePageUseCase: GetNearbyCafePageUseCase

    private let getPopularCastPageUseCase: GetPopularCastPageUseCase

    private let observeCurrentUserUseCase: ObserveCurrentUserUseCase

    private let bannerEventPublisher: BannerEventPublisher

    private let cafeEventEventPublisher: CafeEventEventPublisher

    private let cafeRegistrationClaimEventPublisher: CafeRegistrationClaimEventPublisher

    private let cafeDetailEventPublisher: CafeDetailEventPublisher

    private let castEventPublisher: CastEventPublisher

    @Published private(set) var uiState = HomeUiState.empty

    let event = PassthroughSubject<HomeEvent, Never>()

    private var tasks: [TaskKey: Task<Void, Never>] = [:]

    private func loadHomeFeed() {
        uiState.isLoading = true

        Task {
            do {
                let result = try await getHomeFeedUseCase.invoke()

                if let success = result as? AppResultSuccess<AnyObject> {
                    if let feed = success.data as? Shared.HomeFeed {
                        uiState = HomeUiState(
                            isLoading: false,
                            isLoggedIn: uiState.isLoggedIn,
                            isLoginPromptVisible: uiState.isLoginPromptVisible,
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
                            notices: feed.notices,
                            cafeEvents: Array(feed.cafeEvents.filter { isOngoingCafeEvent($0.statusLabel) }.prefix(maxHomeCafeEvents))
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
        tasks[.popularCastPage]?.cancel()
        tasks[.popularCastPage] = Task {
            uiState = HomeUiState(
                isLoggedIn: uiState.isLoggedIn,
                isLoginPromptVisible: uiState.isLoginPromptVisible,
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
                notices: uiState.notices,
                cafeEvents: uiState.cafeEvents
            )

            do {
                if append {
                    try await Task.sleep(nanoseconds: Self.paginationDelayNanoseconds)
                    if Task.isCancelled { return }
                }
                let result = try await getPopularCastPageUseCase.invoke(cursor: cursor)

                if let success = result as? AppResultSuccess<AnyObject>,
                   let page = success.data as? Shared.HomePopularCastPage {
                    uiState = HomeUiState(
                        isLoggedIn: uiState.isLoggedIn,
                        isLoginPromptVisible: uiState.isLoginPromptVisible,
                        banners: uiState.banners,
                        popularCasts: append ? (uiState.popularCasts + page.casts) : page.casts,
                        popularCastCafeNames: append
                            ? uiState.popularCastCafeNames.merging(Self.dictionary(from: page.cafeNames)) { _, new in new }
                            : Self.dictionary(from: page.cafeNames),
                        popularCastCursor: page.nextCursor,
                        canLoadMorePopularCasts: page.hasNext,
                        isLoadingMorePopularCasts: false,
                        nearbyCafes: uiState.nearbyCafes,
                        nearbyCafeCursor: uiState.nearbyCafeCursor,
                        canLoadMoreNearbyCafes: uiState.canLoadMoreNearbyCafes,
                        isLoadingMoreNearbyCafes: uiState.isLoadingMoreNearbyCafes,
                        birthdayCasts: uiState.birthdayCasts,
                        notices: uiState.notices,
                        cafeEvents: uiState.cafeEvents
                    )
                } else {
                    uiState = HomeUiState(
                        isLoggedIn: uiState.isLoggedIn,
                        isLoginPromptVisible: uiState.isLoginPromptVisible,
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
                        notices: uiState.notices,
                        cafeEvents: uiState.cafeEvents
                    )
                }
            } catch {
                if Task.isCancelled { return }
                uiState = HomeUiState(
                    isLoggedIn: uiState.isLoggedIn,
                    isLoginPromptVisible: uiState.isLoginPromptVisible,
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
                    notices: uiState.notices,
                    cafeEvents: uiState.cafeEvents
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
        tasks[.nearbyCafePage]?.cancel()
        tasks[.nearbyCafePage] = Task {
            uiState = HomeUiState(
                isLoggedIn: uiState.isLoggedIn,
                isLoginPromptVisible: uiState.isLoginPromptVisible,
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
                notices: uiState.notices,
                cafeEvents: uiState.cafeEvents
            )

            do {
                if append {
                    try await Task.sleep(nanoseconds: Self.paginationDelayNanoseconds)
                    if Task.isCancelled { return }
                }
                let result = try await getNearbyCafePageUseCase.invoke(cursor: cursor)

                if let success = result as? AppResultSuccess<AnyObject>,
                   let page = success.data as? Shared.PagedResult<Shared.Cafe> {
                    uiState = HomeUiState(
                        isLoggedIn: uiState.isLoggedIn,
                        isLoginPromptVisible: uiState.isLoginPromptVisible,
                        banners: uiState.banners,
                        popularCasts: uiState.popularCasts,
                        popularCastCafeNames: uiState.popularCastCafeNames,
                        popularCastCursor: uiState.popularCastCursor,
                        canLoadMorePopularCasts: uiState.canLoadMorePopularCasts,
                        isLoadingMorePopularCasts: uiState.isLoadingMorePopularCasts,
                        nearbyCafes: append ? (uiState.nearbyCafes + (page.items as? [Cafe] ?? [])) : (page.items as? [Cafe] ?? []),
                        nearbyCafeCursor: page.nextCursor,
                        canLoadMoreNearbyCafes: page.hasNext,
                        isLoadingMoreNearbyCafes: false,
                        birthdayCasts: uiState.birthdayCasts,
                        notices: uiState.notices,
                        cafeEvents: uiState.cafeEvents
                    )
                } else {
                    uiState = HomeUiState(
                        isLoggedIn: uiState.isLoggedIn,
                        isLoginPromptVisible: uiState.isLoginPromptVisible,
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
                        notices: uiState.notices,
                        cafeEvents: uiState.cafeEvents
                    )
                }
            } catch {
                if Task.isCancelled { return }
                uiState = HomeUiState(
                    isLoggedIn: uiState.isLoggedIn,
                    isLoginPromptVisible: uiState.isLoginPromptVisible,
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
                    notices: uiState.notices,
                    cafeEvents: uiState.cafeEvents
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

    private func observeSession() {
        tasks[.session]?.cancel()
        tasks[.session] = Task {
            do {
                for try await user in asyncSequence(for: observeCurrentUserUseCase.invoke()) {
                    uiState = HomeUiState(
                        isLoading: uiState.isLoading,
                        isLoggedIn: user != nil,
                        isLoginPromptVisible: user == nil ? uiState.isLoginPromptVisible : false,
                        banners: uiState.banners,
                        popularCasts: uiState.popularCasts,
                        popularCastCafeNames: uiState.popularCastCafeNames,
                        popularCastCursor: uiState.popularCastCursor,
                        canLoadMorePopularCasts: uiState.canLoadMorePopularCasts,
                        isLoadingMorePopularCasts: uiState.isLoadingMorePopularCasts,
                        nearbyCafes: uiState.nearbyCafes,
                        nearbyCafeCursor: uiState.nearbyCafeCursor,
                        canLoadMoreNearbyCafes: uiState.canLoadMoreNearbyCafes,
                        isLoadingMoreNearbyCafes: uiState.isLoadingMoreNearbyCafes,
                        birthdayCasts: uiState.birthdayCasts,
                        notices: uiState.notices,
                        cafeEvents: uiState.cafeEvents
                    )
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

    private func observeBannerEvent() {
        tasks[.bannerEvent]?.cancel()
        tasks[.bannerEvent] = Task {
            do {
                for try await event in asyncSequence(for: bannerEventPublisher.events) {
                    switch event {
                    case is Shared.BannerEvent.Created:
                        self.loadHomeFeed()
                    case let updated as Shared.BannerEvent.Updated:
                        self.patchBanner(updated.banner)
                    case let deleted as Shared.BannerEvent.Deleted:
                        self.removeBanner(id: deleted.banner.id)
                    default:
                        break
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
                    if let approved = event as? CafeRegistrationClaimEvent.Approved {
                        let approvedCafeId = approved.approvedCafeId?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
                        let alreadyVisible = !approvedCafeId.isEmpty &&
                            self.uiState.nearbyCafes.contains(where: { $0.id == approvedCafeId })

                        if !alreadyVisible {
                            self.loadNearbyCafePage(cursor: nil, append: false)
                        }
                    }
                }
            } catch {
                print("Error: \(error)")
            }
        }
    }

    private func observeCastEvent() {
        tasks[.castEvent]?.cancel()
        tasks[.castEvent] = Task {
            do {
                for try await event in asyncSequence(for: castEventPublisher.events) {
                    switch event {
                    case is Shared.CastEvent.Created:
                        self.loadPopularCastPage(cursor: nil, append: false)
                    case let updated as Shared.CastEvent.Updated:
                        self.patchCast(updated.cast)
                    case let deleted as Shared.CastEvent.Deleted:
                        self.removeCast(deleted.castId)
                    default:
                        break
                    }
                }
            } catch {
                print("Error: \(error)")
            }
        }
    }

    private func observeCafeEventEvent() {
        tasks[.cafeEventEvent]?.cancel()
        tasks[.cafeEventEvent] = Task {
            do {
                for try await event in asyncSequence(for: cafeEventEventPublisher.events) {
                    switch event {
                    case let created as Shared.CafeEventEvent.Created:
                        upsertCafeEvent(created.event)
                    case let updated as Shared.CafeEventEvent.Updated:
                        upsertCafeEvent(updated.event)
                    case let deleted as Shared.CafeEventEvent.Deleted:
                        removeCafeEvent(deleted.eventId)
                    default:
                        break
                    }
                }
            } catch {
                print("Error: \(error)")
            }
        }
    }

    private func patchBanner(_ updatedBanner: HomeBanner) {
        uiState = HomeUiState(
            isLoggedIn: uiState.isLoggedIn,
            isLoginPromptVisible: uiState.isLoginPromptVisible,
            banners: uiState.banners.map { banner in
                banner.id == updatedBanner.id ? updatedBanner : banner
            },
            popularCasts: uiState.popularCasts,
            popularCastCafeNames: uiState.popularCastCafeNames,
            popularCastCursor: uiState.popularCastCursor,
            canLoadMorePopularCasts: uiState.canLoadMorePopularCasts,
            isLoadingMorePopularCasts: uiState.isLoadingMorePopularCasts,
            nearbyCafes: uiState.nearbyCafes,
            nearbyCafeCursor: uiState.nearbyCafeCursor,
            canLoadMoreNearbyCafes: uiState.canLoadMoreNearbyCafes,
            isLoadingMoreNearbyCafes: uiState.isLoadingMoreNearbyCafes,
            birthdayCasts: uiState.birthdayCasts,
            notices: uiState.notices,
            cafeEvents: uiState.cafeEvents
        )
    }

    private func removeBanner(id: String) {
        uiState = HomeUiState(
            isLoggedIn: uiState.isLoggedIn,
            isLoginPromptVisible: uiState.isLoginPromptVisible,
            banners: uiState.banners.filter { $0.id != id },
            popularCasts: uiState.popularCasts,
            popularCastCafeNames: uiState.popularCastCafeNames,
            popularCastCursor: uiState.popularCastCursor,
            canLoadMorePopularCasts: uiState.canLoadMorePopularCasts,
            isLoadingMorePopularCasts: uiState.isLoadingMorePopularCasts,
            nearbyCafes: uiState.nearbyCafes,
            nearbyCafeCursor: uiState.nearbyCafeCursor,
            canLoadMoreNearbyCafes: uiState.canLoadMoreNearbyCafes,
            isLoadingMoreNearbyCafes: uiState.isLoadingMoreNearbyCafes,
            birthdayCasts: uiState.birthdayCasts,
            notices: uiState.notices,
            cafeEvents: uiState.cafeEvents
        )
    }

    private func patchCafeInfo(_ cafe: Cafe) {
        uiState = HomeUiState(
            isLoggedIn: uiState.isLoggedIn,
            isLoginPromptVisible: uiState.isLoginPromptVisible,
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
            notices: uiState.notices,
            cafeEvents: uiState.cafeEvents
        )
    }

    private func patchCast(_ cast: Cast) {
        uiState = HomeUiState(
            isLoggedIn: uiState.isLoggedIn,
            isLoginPromptVisible: uiState.isLoginPromptVisible,
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
            notices: uiState.notices,
            cafeEvents: uiState.cafeEvents
        )
    }

    private func removeCast(_ castId: String) {
        uiState = HomeUiState(
            isLoggedIn: uiState.isLoggedIn,
            isLoginPromptVisible: uiState.isLoginPromptVisible,
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
            notices: uiState.notices,
            cafeEvents: uiState.cafeEvents
        )
    }

    private func upsertCafeEvent(_ event: Shared.CafeEventManagementItem) {
        guard let homeEvent = toHomeCafeEvent(event) else {
            removeCafeEvent(event.id)
            return
        }
        let merged = (uiState.cafeEvents.filter { $0.id != homeEvent.id } + [homeEvent])
            .sorted { $0.periodText > $1.periodText }
        let limited = Array(merged.prefix(maxHomeCafeEvents))
        uiState = HomeUiState(
            isLoading: uiState.isLoading,
            isLoggedIn: uiState.isLoggedIn,
            isLoginPromptVisible: uiState.isLoginPromptVisible,
            banners: uiState.banners,
            popularCasts: uiState.popularCasts,
            popularCastCafeNames: uiState.popularCastCafeNames,
            popularCastCursor: uiState.popularCastCursor,
            canLoadMorePopularCasts: uiState.canLoadMorePopularCasts,
            isLoadingMorePopularCasts: uiState.isLoadingMorePopularCasts,
            nearbyCafes: uiState.nearbyCafes,
            nearbyCafeCursor: uiState.nearbyCafeCursor,
            canLoadMoreNearbyCafes: uiState.canLoadMoreNearbyCafes,
            isLoadingMoreNearbyCafes: uiState.isLoadingMoreNearbyCafes,
            birthdayCasts: uiState.birthdayCasts,
            notices: uiState.notices,
            cafeEvents: limited
        )
    }

    private func removeCafeEvent(_ eventId: String) {
        let filtered = uiState.cafeEvents.filter { $0.id != eventId }
        uiState = HomeUiState(
            isLoading: uiState.isLoading,
            isLoggedIn: uiState.isLoggedIn,
            isLoginPromptVisible: uiState.isLoginPromptVisible,
            banners: uiState.banners,
            popularCasts: uiState.popularCasts,
            popularCastCafeNames: uiState.popularCastCafeNames,
            popularCastCursor: uiState.popularCastCursor,
            canLoadMorePopularCasts: uiState.canLoadMorePopularCasts,
            isLoadingMorePopularCasts: uiState.isLoadingMorePopularCasts,
            nearbyCafes: uiState.nearbyCafes,
            nearbyCafeCursor: uiState.nearbyCafeCursor,
            canLoadMoreNearbyCafes: uiState.canLoadMoreNearbyCafes,
            isLoadingMoreNearbyCafes: uiState.isLoadingMoreNearbyCafes,
            birthdayCasts: uiState.birthdayCasts,
            notices: uiState.notices,
            cafeEvents: filtered
        )
    }

    private func toHomeCafeEvent(_ event: Shared.CafeEventManagementItem) -> Shared.HomeCafeEvent? {
        if !isOngoingCafeEvent(event.statusLabel) || event.isDimmed {
            return nil
        }
        let cafeName = uiState.nearbyCafes.first(where: { $0.id == event.cafeId })?.name
            ?? uiState.popularCastCafeNames[event.cafeId]
            ?? event.cafeId
        return Shared.HomeCafeEvent(
            id: event.id,
            cafeId: event.cafeId,
            cafeName: cafeName,
            title: event.title,
            content: event.content,
            imageUrl: event.imageUrl,
            periodText: event.periodText,
            statusLabel: event.statusLabel
        )
    }

    private func isOngoingCafeEvent(_ statusLabel: String) -> Bool {
        let normalized = statusLabel.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        return normalized.contains("진행 중") || normalized.contains("진행중") || normalized.contains("ongoing")
    }

    private func requireSignedIn(onAuthenticated: @escaping () -> Void) {
        if uiState.isLoggedIn {
            onAuthenticated()
        } else {
            uiState = HomeUiState(
                isLoggedIn: uiState.isLoggedIn,
                isLoginPromptVisible: true,
                banners: uiState.banners,
                popularCasts: uiState.popularCasts,
                popularCastCafeNames: uiState.popularCastCafeNames,
                popularCastCursor: uiState.popularCastCursor,
                canLoadMorePopularCasts: uiState.canLoadMorePopularCasts,
                isLoadingMorePopularCasts: uiState.isLoadingMorePopularCasts,
                nearbyCafes: uiState.nearbyCafes,
                nearbyCafeCursor: uiState.nearbyCafeCursor,
                canLoadMoreNearbyCafes: uiState.canLoadMoreNearbyCafes,
                isLoadingMoreNearbyCafes: uiState.isLoadingMoreNearbyCafes,
                birthdayCasts: uiState.birthdayCasts,
                notices: uiState.notices,
                cafeEvents: uiState.cafeEvents
            )
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
                requireSignedIn { [weak self] in
                    self?.event.send(.navigateToCafe(id: cafeId))
                }
            }
        default:
            break
        }
    }

    func onAction(_ action: HomeAction) {
        switch action {
        case .bannerTapped(let banner):
            handleBannerTap(banner)
        case .maidTapped(let id):
            requireSignedIn { [weak self] in
                self?.event.send(.navigateToCast(id: id))
            }
        case .birthdayMaidTapped(let id):
            requireSignedIn { [weak self] in
                self?.event.send(.navigateToCast(id: id))
            }
        case .cafeTapped(let id):
            requireSignedIn { [weak self] in
                self?.event.send(.navigateToCafe(id: id))
            }
        case .loginPromptSignInTapped:
            uiState = HomeUiState(
                isLoggedIn: uiState.isLoggedIn,
                isLoginPromptVisible: false,
                banners: uiState.banners,
                popularCasts: uiState.popularCasts,
                popularCastCafeNames: uiState.popularCastCafeNames,
                popularCastCursor: uiState.popularCastCursor,
                canLoadMorePopularCasts: uiState.canLoadMorePopularCasts,
                isLoadingMorePopularCasts: uiState.isLoadingMorePopularCasts,
                nearbyCafes: uiState.nearbyCafes,
                nearbyCafeCursor: uiState.nearbyCafeCursor,
                canLoadMoreNearbyCafes: uiState.canLoadMoreNearbyCafes,
                isLoadingMoreNearbyCafes: uiState.isLoadingMoreNearbyCafes,
                birthdayCasts: uiState.birthdayCasts,
                notices: uiState.notices,
                cafeEvents: uiState.cafeEvents
            )
            event.send(.navigateToSignIn)
        case .dismissLoginPrompt:
            uiState = HomeUiState(
                isLoggedIn: uiState.isLoggedIn,
                isLoginPromptVisible: false,
                banners: uiState.banners,
                popularCasts: uiState.popularCasts,
                popularCastCafeNames: uiState.popularCastCafeNames,
                popularCastCursor: uiState.popularCastCursor,
                canLoadMorePopularCasts: uiState.canLoadMorePopularCasts,
                isLoadingMorePopularCasts: uiState.isLoadingMorePopularCasts,
                nearbyCafes: uiState.nearbyCafes,
                nearbyCafeCursor: uiState.nearbyCafeCursor,
                canLoadMoreNearbyCafes: uiState.canLoadMoreNearbyCafes,
                isLoadingMoreNearbyCafes: uiState.isLoadingMoreNearbyCafes,
                birthdayCasts: uiState.birthdayCasts,
                notices: uiState.notices,
                cafeEvents: uiState.cafeEvents
            )
        case .loadMorePopularCasts:
            loadMorePopularCasts()
        case .loadMoreNearbyCafes:
            loadMoreNearbyCafes()
        }
    }

    init(
        getHomeFeedUseCase: GetHomeFeedUseCase = KoinInitializerKt.resolveGetHomeFeedUseCase(),
        getNearbyCafePageUseCase: GetNearbyCafePageUseCase = KoinInitializerKt.resolveGetNearbyCafePageUseCase(),
        getPopularCastPageUseCase: GetPopularCastPageUseCase = KoinInitializerKt.resolveGetPopularCastPageUseCase(),
        observeCurrentUserUseCase: ObserveCurrentUserUseCase = KoinInitializerKt.resolveObserveCurrentUserUseCase(),
        bannerEventPublisher: BannerEventPublisher = KoinInitializerKt.resolveBannerEventPublisher(),
        cafeEventEventPublisher: CafeEventEventPublisher = KoinInitializerKt.resolveCafeEventEventPublisher(),
        cafeRegistrationClaimEventPublisher: CafeRegistrationClaimEventPublisher = KoinInitializerKt.resolveCafeRegistrationClaimEventPublisher(),
        cafeDetailEventPublisher: CafeDetailEventPublisher = KoinInitializerKt.resolveCafeDetailEventPublisher(),
        castEventPublisher: CastEventPublisher = KoinInitializerKt.resolveCastEventPublisher()
    ) {
        self.getHomeFeedUseCase = getHomeFeedUseCase
        self.getNearbyCafePageUseCase = getNearbyCafePageUseCase
        self.getPopularCastPageUseCase = getPopularCastPageUseCase
        self.observeCurrentUserUseCase = observeCurrentUserUseCase
        self.bannerEventPublisher = bannerEventPublisher
        self.cafeEventEventPublisher = cafeEventEventPublisher
        self.cafeRegistrationClaimEventPublisher = cafeRegistrationClaimEventPublisher
        self.cafeDetailEventPublisher = cafeDetailEventPublisher
        self.castEventPublisher = castEventPublisher

        observeSession()
        observeBannerEvent()
        observeCafeEventEvent()
        observeCafeRegistrationClaimEvent()
        observeCafeDetailEvent()
        observeCastEvent()
        loadHomeFeed()
    }

    deinit {
        tasks.values.forEach { $0.cancel() }
        tasks.removeAll()
    }

    private let maxHomeCafeEvents = 8
    private static let paginationDelayNanoseconds: UInt64 = 1_000_000_000

    private static func dictionary(from source: [AnyHashable: Any]) -> [String: String] {
        source.reduce(into: [:]) { partialResult, entry in
            guard let key = entry.key as? String, let value = entry.value as? String else { return }
            partialResult[key] = value
        }
    }

    private enum TaskKey {
        case session
        case bannerEvent
        case cafeEventEvent
        case cafeRegistrationClaimEvent
        case cafeDetailEvent
        case castEvent
        case popularCastPage
        case nearbyCafePage
    }
}
