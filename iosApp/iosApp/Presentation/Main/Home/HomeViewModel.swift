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
    private let getHomeBannersUseCase: GetHomeBannersUseCase

    private let getHomeCafeEventsUseCase: GetHomeCafeEventsUseCase

    private let getNearbyCafePageUseCase: GetNearbyCafePageUseCase

    private let getPopularCastPageUseCase: GetPopularCastPageUseCase

    private let getBirthdayCastsUseCase: GetBirthdayCastsUseCase

    private let getRecentNoticesUseCase: GetRecentNoticesUseCase

    private let getCommunityPostPageUseCase: GetCommunityPostPageUseCase

    private let observeCurrentUserUseCase: ObserveCurrentUserUseCase

    private let observeContentLayoutUseCase: ObserveContentLayoutUseCase

    private let bannerEventPublisher: BannerEventPublisher

    private let cafeEventEventPublisher: CafeEventEventPublisher

    private let cafeRegistrationClaimEventPublisher: CafeRegistrationClaimEventPublisher

    private let cafeDetailEventPublisher: CafeDetailEventPublisher

    private let castEventPublisher: CastEventPublisher

    private let communityPostEventPublisher: CommunityPostEventPublisher

    @Published private(set) var uiState = HomeUiState.empty

    let event = PassthroughSubject<HomeEvent, Never>()

    private var tasks: [TaskKey: Task<Void, Never>] = [:]

    private func loadInitialHomeSections() {
        uiState.isLoading = true

        Task {
            do {
                async let bannersResult = getHomeBannersUseCase.invoke(limit: maxHomeFeedItems)
                async let popularCastPageResult = getPopularCastPageUseCase.invoke(cursor: nil)
                async let nearbyCafePageResult = getNearbyCafePageUseCase.invoke(cursor: nil)
                async let birthdayCastsResult = getBirthdayCastsUseCase.invoke(limit: maxHomeFeedItems)
                async let noticesResult = getRecentNoticesUseCase.invoke(limit: maxHomeFeedItems)
                async let cafeEventsResult = getHomeCafeEventsUseCase.invoke(cursor: nil, pageSize: Int32(maxHomeCafeEvents))
                async let communityPostsResult = getCommunityPostPageUseCase.invoke(cursor: nil, pageSize: maxHomeCommunityPosts)

                let loaded = try await (
                    bannersResult,
                    popularCastPageResult,
                    nearbyCafePageResult,
                    birthdayCastsResult,
                    noticesResult,
                    cafeEventsResult,
                    communityPostsResult
                )
                let popularCastPage = (loaded.1 as? AppResultSuccess<AnyObject>)?.data as? Shared.HomePopularCastPage
                let nearbyCafePage = (loaded.2 as? AppResultSuccess<AnyObject>)?.data as? Shared.PagedResult<Shared.Cafe>
                let birthdayCastPage = (loaded.3 as? AppResultSuccess<AnyObject>)?.data as? Shared.HomeBirthdayCastPage
                let cafeEventPage = (loaded.5 as? AppResultSuccess<AnyObject>)?.data as? Shared.PagedResult<Shared.HomeCafeEvent>
                let communityPostPage = (loaded.6 as? AppResultSuccess<AnyObject>)?.data as? Shared.PagedResult<Shared.CommunityPost>
                uiState = HomeUiState(
                    isLoading: false,
                    isLoggedIn: uiState.isLoggedIn,
                    isLoginPromptVisible: uiState.isLoginPromptVisible,
                    contentLayout: uiState.contentLayout,
                    banners: ((loaded.0 as? AppResultSuccess<AnyObject>)?.data as? [HomeBanner]) ?? uiState.banners,
                    popularCasts: popularCastPage?.casts ?? uiState.popularCasts,
                    popularCastCafeNames: popularCastPage.map { Self.dictionary(from: $0.cafeNames) } ?? uiState.popularCastCafeNames,
                    popularCastCafeRegions: popularCastPage.map { Self.dictionary(from: $0.cafeRegions) } ?? uiState.popularCastCafeRegions,
                    popularCastCursor: popularCastPage?.nextCursor ?? uiState.popularCastCursor,
                    canLoadMorePopularCasts: popularCastPage?.hasNext ?? uiState.canLoadMorePopularCasts,
                    isLoadingMorePopularCasts: false,
                    nearbyCafes: nearbyCafePage?.items as? [Cafe] ?? uiState.nearbyCafes,
                    nearbyCafeCursor: nearbyCafePage?.nextCursor ?? uiState.nearbyCafeCursor,
                    canLoadMoreNearbyCafes: nearbyCafePage?.hasNext ?? uiState.canLoadMoreNearbyCafes,
                    isLoadingMoreNearbyCafes: false,
                    birthdayCasts: birthdayCastPage?.casts ?? uiState.birthdayCasts,
                    birthdayCastCafeNames: birthdayCastPage.map { Self.dictionary(from: $0.cafeNames) } ?? uiState.birthdayCastCafeNames,
                    notices: ((loaded.4 as? AppResultSuccess<AnyObject>)?.data as? [Notice]) ?? uiState.notices,
                    cafeEvents: cafeEventPage?.items as? [HomeCafeEvent] ?? uiState.cafeEvents,
                    cafeEventCursor: cafeEventPage?.nextCursor ?? uiState.cafeEventCursor,
                    canLoadMoreCafeEvents: cafeEventPage?.hasNext ?? uiState.canLoadMoreCafeEvents,
                    isLoadingMoreCafeEvents: false,
                    communityPosts: communityPostPage?.items as? [CommunityPost] ?? uiState.communityPosts
                )
            } catch {
                uiState = HomeUiState(
                    isLoading: false,
                    isLoggedIn: uiState.isLoggedIn,
                    isLoginPromptVisible: uiState.isLoginPromptVisible,
                    contentLayout: uiState.contentLayout,
                    banners: uiState.banners,
                    popularCasts: uiState.popularCasts,
                    popularCastCafeNames: uiState.popularCastCafeNames,
                    popularCastCafeRegions: uiState.popularCastCafeRegions,
                    popularCastCursor: uiState.popularCastCursor,
                    canLoadMorePopularCasts: uiState.canLoadMorePopularCasts,
                    isLoadingMorePopularCasts: false,
                    nearbyCafes: uiState.nearbyCafes,
                    nearbyCafeCursor: uiState.nearbyCafeCursor,
                    canLoadMoreNearbyCafes: uiState.canLoadMoreNearbyCafes,
                    isLoadingMoreNearbyCafes: false,
                    birthdayCasts: uiState.birthdayCasts,
                    birthdayCastCafeNames: uiState.birthdayCastCafeNames,
                    notices: uiState.notices,
                    cafeEvents: uiState.cafeEvents,
                    cafeEventCursor: uiState.cafeEventCursor,
                    canLoadMoreCafeEvents: uiState.canLoadMoreCafeEvents,
                    isLoadingMoreCafeEvents: false,
                    communityPosts: uiState.communityPosts
                )
            }
        }
    }

    private func loadHomeBanners() {
        tasks[.homeBanners]?.cancel()
        tasks[.homeBanners] = Task {
            do {
                let result = try await getHomeBannersUseCase.invoke(limit: maxHomeFeedItems)
                if let success = result as? AppResultSuccess<AnyObject>,
                   let banners = success.data as? [HomeBanner] {
                    uiState = HomeUiState(
                        isLoading: uiState.isLoading,
                        isLoggedIn: uiState.isLoggedIn,
                        isLoginPromptVisible: uiState.isLoginPromptVisible,
                        contentLayout: uiState.contentLayout,
                        banners: banners,
                        popularCasts: uiState.popularCasts,
                        popularCastCafeNames: uiState.popularCastCafeNames,
                        popularCastCafeRegions: uiState.popularCastCafeRegions,
                        popularCastCursor: uiState.popularCastCursor,
                        canLoadMorePopularCasts: uiState.canLoadMorePopularCasts,
                        isLoadingMorePopularCasts: uiState.isLoadingMorePopularCasts,
                        nearbyCafes: uiState.nearbyCafes,
                        nearbyCafeCursor: uiState.nearbyCafeCursor,
                        canLoadMoreNearbyCafes: uiState.canLoadMoreNearbyCafes,
                        isLoadingMoreNearbyCafes: uiState.isLoadingMoreNearbyCafes,
                        birthdayCasts: uiState.birthdayCasts,
                        birthdayCastCafeNames: uiState.birthdayCastCafeNames,
                        notices: uiState.notices,
                        cafeEvents: uiState.cafeEvents,
                        cafeEventCursor: uiState.cafeEventCursor,
                        canLoadMoreCafeEvents: uiState.canLoadMoreCafeEvents,
                        isLoadingMoreCafeEvents: uiState.isLoadingMoreCafeEvents,
                        communityPosts: uiState.communityPosts
                    )
                }
            } catch {}
        }
    }

    private func loadBirthdayCasts() {
        tasks[.birthdayCasts]?.cancel()
        tasks[.birthdayCasts] = Task {
            do {
                let result = try await getBirthdayCastsUseCase.invoke(limit: maxHomeFeedItems)
                if let success = result as? AppResultSuccess<AnyObject>,
                   let page = success.data as? Shared.HomeBirthdayCastPage {
                    uiState = HomeUiState(
                        isLoading: uiState.isLoading,
                        isLoggedIn: uiState.isLoggedIn,
                        isLoginPromptVisible: uiState.isLoginPromptVisible,
                        contentLayout: uiState.contentLayout,
                        banners: uiState.banners,
                        popularCasts: uiState.popularCasts,
                        popularCastCafeNames: uiState.popularCastCafeNames,
                        popularCastCafeRegions: uiState.popularCastCafeRegions,
                        popularCastCursor: uiState.popularCastCursor,
                        canLoadMorePopularCasts: uiState.canLoadMorePopularCasts,
                        isLoadingMorePopularCasts: uiState.isLoadingMorePopularCasts,
                        nearbyCafes: uiState.nearbyCafes,
                        nearbyCafeCursor: uiState.nearbyCafeCursor,
                        canLoadMoreNearbyCafes: uiState.canLoadMoreNearbyCafes,
                        isLoadingMoreNearbyCafes: uiState.isLoadingMoreNearbyCafes,
                        birthdayCasts: page.casts,
                        birthdayCastCafeNames: Self.dictionary(from: page.cafeNames),
                        notices: uiState.notices,
                        cafeEvents: uiState.cafeEvents,
                        cafeEventCursor: uiState.cafeEventCursor,
                        canLoadMoreCafeEvents: uiState.canLoadMoreCafeEvents,
                        isLoadingMoreCafeEvents: uiState.isLoadingMoreCafeEvents,
                        communityPosts: uiState.communityPosts
                    )
                }
            } catch {}
        }
    }

    private func loadRecentNotices() {
        tasks[.recentNotices]?.cancel()
        tasks[.recentNotices] = Task {
            do {
                let result = try await getRecentNoticesUseCase.invoke(limit: maxHomeFeedItems)
                if let success = result as? AppResultSuccess<AnyObject>,
                   let notices = success.data as? [Notice] {
                    uiState = HomeUiState(
                        isLoading: uiState.isLoading,
                        isLoggedIn: uiState.isLoggedIn,
                        isLoginPromptVisible: uiState.isLoginPromptVisible,
                        contentLayout: uiState.contentLayout,
                        banners: uiState.banners,
                        popularCasts: uiState.popularCasts,
                        popularCastCafeNames: uiState.popularCastCafeNames,
                        popularCastCafeRegions: uiState.popularCastCafeRegions,
                        popularCastCursor: uiState.popularCastCursor,
                        canLoadMorePopularCasts: uiState.canLoadMorePopularCasts,
                        isLoadingMorePopularCasts: uiState.isLoadingMorePopularCasts,
                        nearbyCafes: uiState.nearbyCafes,
                        nearbyCafeCursor: uiState.nearbyCafeCursor,
                        canLoadMoreNearbyCafes: uiState.canLoadMoreNearbyCafes,
                        isLoadingMoreNearbyCafes: uiState.isLoadingMoreNearbyCafes,
                        birthdayCasts: uiState.birthdayCasts,
                        birthdayCastCafeNames: uiState.birthdayCastCafeNames,
                        notices: notices,
                        cafeEvents: uiState.cafeEvents,
                        cafeEventCursor: uiState.cafeEventCursor,
                        canLoadMoreCafeEvents: uiState.canLoadMoreCafeEvents,
                        isLoadingMoreCafeEvents: uiState.isLoadingMoreCafeEvents,
                        communityPosts: uiState.communityPosts
                    )
                }
            } catch {}
        }
    }

    private func loadCommunityPosts() {
        tasks[.communityPosts]?.cancel()
        tasks[.communityPosts] = Task {
            do {
                let result = try await getCommunityPostPageUseCase.invoke(cursor: nil, pageSize: maxHomeCommunityPosts)

                if let success = result as? AppResultSuccess<AnyObject>,
                   let page = success.data as? Shared.PagedResult<Shared.CommunityPost> {
                    uiState = HomeUiState(
                        isLoading: uiState.isLoading,
                        isLoggedIn: uiState.isLoggedIn,
                        isLoginPromptVisible: uiState.isLoginPromptVisible,
                        contentLayout: uiState.contentLayout,
                        banners: uiState.banners,
                        popularCasts: uiState.popularCasts,
                        popularCastCafeNames: uiState.popularCastCafeNames,
                        popularCastCafeRegions: uiState.popularCastCafeRegions,
                        popularCastCursor: uiState.popularCastCursor,
                        canLoadMorePopularCasts: uiState.canLoadMorePopularCasts,
                        isLoadingMorePopularCasts: uiState.isLoadingMorePopularCasts,
                        nearbyCafes: uiState.nearbyCafes,
                        nearbyCafeCursor: uiState.nearbyCafeCursor,
                        canLoadMoreNearbyCafes: uiState.canLoadMoreNearbyCafes,
                        isLoadingMoreNearbyCafes: uiState.isLoadingMoreNearbyCafes,
                        birthdayCasts: uiState.birthdayCasts,
                        birthdayCastCafeNames: uiState.birthdayCastCafeNames,
                        notices: uiState.notices,
                        cafeEvents: uiState.cafeEvents,
                        cafeEventCursor: uiState.cafeEventCursor,
                        canLoadMoreCafeEvents: uiState.canLoadMoreCafeEvents,
                        isLoadingMoreCafeEvents: uiState.isLoadingMoreCafeEvents,
                        communityPosts: page.items as? [CommunityPost] ?? []
                    )
                }
            } catch {
                // silently ignore - community posts section hidden when empty
            }
        }
    }

    private func loadHomeCafeEvents() {
        tasks[.homeCafeEvents]?.cancel()
        tasks[.homeCafeEvents] = Task {
            do {
                let result = try await getHomeCafeEventsUseCase.invoke(cursor: nil, pageSize: Int32(maxHomeCafeEvents))

                if let success = result as? AppResultSuccess<AnyObject>,
                   let page = success.data as? Shared.PagedResult<Shared.HomeCafeEvent> {
                    uiState = HomeUiState(
                        isLoading: uiState.isLoading,
                        isLoggedIn: uiState.isLoggedIn,
                        isLoginPromptVisible: uiState.isLoginPromptVisible,
                        contentLayout: uiState.contentLayout,
                        banners: uiState.banners,
                        popularCasts: uiState.popularCasts,
                        popularCastCafeNames: uiState.popularCastCafeNames,
                        popularCastCafeRegions: uiState.popularCastCafeRegions,
                        popularCastCursor: uiState.popularCastCursor,
                        canLoadMorePopularCasts: uiState.canLoadMorePopularCasts,
                        isLoadingMorePopularCasts: uiState.isLoadingMorePopularCasts,
                        nearbyCafes: uiState.nearbyCafes,
                        nearbyCafeCursor: uiState.nearbyCafeCursor,
                        canLoadMoreNearbyCafes: uiState.canLoadMoreNearbyCafes,
                        isLoadingMoreNearbyCafes: uiState.isLoadingMoreNearbyCafes,
                        birthdayCasts: uiState.birthdayCasts,
                        birthdayCastCafeNames: uiState.birthdayCastCafeNames,
                        notices: uiState.notices,
                        cafeEvents: Array((page.items as? [HomeCafeEvent] ?? []).filter { isDisplayableCafeEvent($0.statusLabel) }.prefix(maxHomeCafeEvents)),
                        cafeEventCursor: page.nextCursor,
                        canLoadMoreCafeEvents: page.hasNext,
                        isLoadingMoreCafeEvents: false,
                        communityPosts: uiState.communityPosts
                    )
                }
            } catch {
                // Keep the rest of the home feed visible when the event feed fails.
            }
        }
    }

    private func loadHomeCafeEventPage(cursor: String?, append: Bool) {
        tasks[.homeCafeEventPage]?.cancel()
        tasks[.homeCafeEventPage] = Task {
            uiState = HomeUiState(
                isLoggedIn: uiState.isLoggedIn,
                isLoginPromptVisible: uiState.isLoginPromptVisible,
                contentLayout: uiState.contentLayout,
                banners: uiState.banners,
                popularCasts: uiState.popularCasts,
                popularCastCafeNames: uiState.popularCastCafeNames,
                popularCastCafeRegions: uiState.popularCastCafeRegions,
                popularCastCursor: uiState.popularCastCursor,
                canLoadMorePopularCasts: uiState.canLoadMorePopularCasts,
                isLoadingMorePopularCasts: uiState.isLoadingMorePopularCasts,
                nearbyCafes: uiState.nearbyCafes,
                nearbyCafeCursor: uiState.nearbyCafeCursor,
                canLoadMoreNearbyCafes: uiState.canLoadMoreNearbyCafes,
                isLoadingMoreNearbyCafes: uiState.isLoadingMoreNearbyCafes,
                birthdayCasts: uiState.birthdayCasts,
                birthdayCastCafeNames: uiState.birthdayCastCafeNames,
                notices: uiState.notices,
                cafeEvents: uiState.cafeEvents,
                cafeEventCursor: uiState.cafeEventCursor,
                canLoadMoreCafeEvents: uiState.canLoadMoreCafeEvents,
                isLoadingMoreCafeEvents: append,
                communityPosts: uiState.communityPosts
            )

            do {
                if append {
                    try await Task.sleep(nanoseconds: Self.paginationDelayNanoseconds)
                    if Task.isCancelled { return }
                }
                let result = try await getHomeCafeEventsUseCase.invoke(cursor: cursor, pageSize: Int32(maxHomeCafeEvents))

                if let success = result as? AppResultSuccess<AnyObject>,
                   let page = success.data as? Shared.PagedResult<Shared.HomeCafeEvent> {
                    let events = (page.items as? [HomeCafeEvent] ?? []).filter { isDisplayableCafeEvent($0.statusLabel) }
                    uiState = HomeUiState(
                        isLoggedIn: uiState.isLoggedIn,
                        isLoginPromptVisible: uiState.isLoginPromptVisible,
                        contentLayout: uiState.contentLayout,
                        banners: uiState.banners,
                        popularCasts: uiState.popularCasts,
                        popularCastCafeNames: uiState.popularCastCafeNames,
                        popularCastCafeRegions: uiState.popularCastCafeRegions,
                        popularCastCursor: uiState.popularCastCursor,
                        canLoadMorePopularCasts: uiState.canLoadMorePopularCasts,
                        isLoadingMorePopularCasts: uiState.isLoadingMorePopularCasts,
                        nearbyCafes: uiState.nearbyCafes,
                        nearbyCafeCursor: uiState.nearbyCafeCursor,
                        canLoadMoreNearbyCafes: uiState.canLoadMoreNearbyCafes,
                        isLoadingMoreNearbyCafes: uiState.isLoadingMoreNearbyCafes,
                        birthdayCasts: uiState.birthdayCasts,
                        birthdayCastCafeNames: uiState.birthdayCastCafeNames,
                        notices: uiState.notices,
                        cafeEvents: append ? (uiState.cafeEvents + events) : events,
                        cafeEventCursor: page.nextCursor,
                        canLoadMoreCafeEvents: page.hasNext,
                        isLoadingMoreCafeEvents: false,
                        communityPosts: uiState.communityPosts
                    )
                } else {
                    uiState = HomeUiState(
                        isLoggedIn: uiState.isLoggedIn,
                        isLoginPromptVisible: uiState.isLoginPromptVisible,
                        contentLayout: uiState.contentLayout,
                        banners: uiState.banners,
                        popularCasts: uiState.popularCasts,
                        popularCastCafeNames: uiState.popularCastCafeNames,
                        popularCastCafeRegions: uiState.popularCastCafeRegions,
                        popularCastCursor: uiState.popularCastCursor,
                        canLoadMorePopularCasts: uiState.canLoadMorePopularCasts,
                        isLoadingMorePopularCasts: uiState.isLoadingMorePopularCasts,
                        nearbyCafes: uiState.nearbyCafes,
                        nearbyCafeCursor: uiState.nearbyCafeCursor,
                        canLoadMoreNearbyCafes: uiState.canLoadMoreNearbyCafes,
                        isLoadingMoreNearbyCafes: uiState.isLoadingMoreNearbyCafes,
                        birthdayCasts: uiState.birthdayCasts,
                        birthdayCastCafeNames: uiState.birthdayCastCafeNames,
                        notices: uiState.notices,
                        cafeEvents: uiState.cafeEvents,
                        cafeEventCursor: uiState.cafeEventCursor,
                        canLoadMoreCafeEvents: false,
                        isLoadingMoreCafeEvents: false,
                        communityPosts: uiState.communityPosts
                    )
                }
            } catch {
                if Task.isCancelled { return }
                uiState = HomeUiState(
                    isLoggedIn: uiState.isLoggedIn,
                    isLoginPromptVisible: uiState.isLoginPromptVisible,
                    contentLayout: uiState.contentLayout,
                    banners: uiState.banners,
                    popularCasts: uiState.popularCasts,
                    popularCastCafeNames: uiState.popularCastCafeNames,
                    popularCastCafeRegions: uiState.popularCastCafeRegions,
                    popularCastCursor: uiState.popularCastCursor,
                    canLoadMorePopularCasts: uiState.canLoadMorePopularCasts,
                    isLoadingMorePopularCasts: uiState.isLoadingMorePopularCasts,
                    nearbyCafes: uiState.nearbyCafes,
                    nearbyCafeCursor: uiState.nearbyCafeCursor,
                    canLoadMoreNearbyCafes: uiState.canLoadMoreNearbyCafes,
                    isLoadingMoreNearbyCafes: uiState.isLoadingMoreNearbyCafes,
                    birthdayCasts: uiState.birthdayCasts,
                    birthdayCastCafeNames: uiState.birthdayCastCafeNames,
                    notices: uiState.notices,
                    cafeEvents: uiState.cafeEvents,
                    cafeEventCursor: uiState.cafeEventCursor,
                    canLoadMoreCafeEvents: false,
                    isLoadingMoreCafeEvents: false,
                    communityPosts: uiState.communityPosts
                )
            }
        }
    }

    private func loadMoreCafeEvents() {
        guard uiState.canLoadMoreCafeEvents,
              !uiState.isLoadingMoreCafeEvents,
              let cursor = uiState.cafeEventCursor else { return }
        loadHomeCafeEventPage(cursor: cursor, append: true)
    }

    private func loadPopularCastPage(cursor: String?, append: Bool) {
        tasks[.popularCastPage]?.cancel()
        tasks[.popularCastPage] = Task {
            uiState = HomeUiState(
                isLoggedIn: uiState.isLoggedIn,
                isLoginPromptVisible: uiState.isLoginPromptVisible,
                contentLayout: uiState.contentLayout,
                banners: uiState.banners,
                popularCasts: uiState.popularCasts,
                popularCastCafeNames: uiState.popularCastCafeNames,
                popularCastCafeRegions: uiState.popularCastCafeRegions,
                popularCastCursor: uiState.popularCastCursor,
                canLoadMorePopularCasts: uiState.canLoadMorePopularCasts,
                isLoadingMorePopularCasts: append,
                nearbyCafes: uiState.nearbyCafes,
                nearbyCafeCursor: uiState.nearbyCafeCursor,
                canLoadMoreNearbyCafes: uiState.canLoadMoreNearbyCafes,
                isLoadingMoreNearbyCafes: uiState.isLoadingMoreNearbyCafes,
                birthdayCasts: uiState.birthdayCasts,
                birthdayCastCafeNames: uiState.birthdayCastCafeNames,
                notices: uiState.notices,
                cafeEvents: uiState.cafeEvents,
                cafeEventCursor: uiState.cafeEventCursor,
                canLoadMoreCafeEvents: uiState.canLoadMoreCafeEvents,
                isLoadingMoreCafeEvents: uiState.isLoadingMoreCafeEvents,
                communityPosts: uiState.communityPosts
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
                        contentLayout: uiState.contentLayout,
                        banners: uiState.banners,
                        popularCasts: append ? (uiState.popularCasts + page.casts) : page.casts,
                        popularCastCafeNames: append
                            ? uiState.popularCastCafeNames.merging(Self.dictionary(from: page.cafeNames)) { _, new in new }
                            : Self.dictionary(from: page.cafeNames),
                        popularCastCafeRegions: append
                            ? uiState.popularCastCafeRegions.merging(Self.dictionary(from: page.cafeRegions)) { _, new in new }
                            : Self.dictionary(from: page.cafeRegions),
                        popularCastCursor: page.nextCursor,
                        canLoadMorePopularCasts: page.hasNext,
                        isLoadingMorePopularCasts: false,
                        nearbyCafes: uiState.nearbyCafes,
                        nearbyCafeCursor: uiState.nearbyCafeCursor,
                        canLoadMoreNearbyCafes: uiState.canLoadMoreNearbyCafes,
                        isLoadingMoreNearbyCafes: uiState.isLoadingMoreNearbyCafes,
                        birthdayCasts: uiState.birthdayCasts,
                        birthdayCastCafeNames: uiState.birthdayCastCafeNames,
                        notices: uiState.notices,
                        cafeEvents: uiState.cafeEvents,
                        cafeEventCursor: uiState.cafeEventCursor,
                        canLoadMoreCafeEvents: uiState.canLoadMoreCafeEvents,
                        isLoadingMoreCafeEvents: uiState.isLoadingMoreCafeEvents,
                        communityPosts: uiState.communityPosts
                    )
                } else {
                    uiState = HomeUiState(
                        isLoggedIn: uiState.isLoggedIn,
                        isLoginPromptVisible: uiState.isLoginPromptVisible,
                        contentLayout: uiState.contentLayout,
                        banners: uiState.banners,
                        popularCasts: uiState.popularCasts,
                        popularCastCafeNames: uiState.popularCastCafeNames,
                        popularCastCafeRegions: uiState.popularCastCafeRegions,
                        popularCastCursor: uiState.popularCastCursor,
                        canLoadMorePopularCasts: uiState.canLoadMorePopularCasts,
                        isLoadingMorePopularCasts: false,
                        nearbyCafes: uiState.nearbyCafes,
                        nearbyCafeCursor: uiState.nearbyCafeCursor,
                        canLoadMoreNearbyCafes: uiState.canLoadMoreNearbyCafes,
                        isLoadingMoreNearbyCafes: uiState.isLoadingMoreNearbyCafes,
                        birthdayCasts: uiState.birthdayCasts,
                        birthdayCastCafeNames: uiState.birthdayCastCafeNames,
                        notices: uiState.notices,
                        cafeEvents: uiState.cafeEvents,
                        cafeEventCursor: uiState.cafeEventCursor,
                        canLoadMoreCafeEvents: uiState.canLoadMoreCafeEvents,
                        isLoadingMoreCafeEvents: uiState.isLoadingMoreCafeEvents,
                        communityPosts: uiState.communityPosts
                    )
                }
            } catch {
                if Task.isCancelled { return }
                uiState = HomeUiState(
                    isLoggedIn: uiState.isLoggedIn,
                    isLoginPromptVisible: uiState.isLoginPromptVisible,
                    contentLayout: uiState.contentLayout,
                    banners: uiState.banners,
                    popularCasts: uiState.popularCasts,
                    popularCastCafeNames: uiState.popularCastCafeNames,
                    popularCastCafeRegions: uiState.popularCastCafeRegions,
                    popularCastCursor: uiState.popularCastCursor,
                    canLoadMorePopularCasts: uiState.canLoadMorePopularCasts,
                    isLoadingMorePopularCasts: false,
                    nearbyCafes: uiState.nearbyCafes,
                    nearbyCafeCursor: uiState.nearbyCafeCursor,
                    canLoadMoreNearbyCafes: uiState.canLoadMoreNearbyCafes,
                    isLoadingMoreNearbyCafes: uiState.isLoadingMoreNearbyCafes,
                    birthdayCasts: uiState.birthdayCasts,
                    birthdayCastCafeNames: uiState.birthdayCastCafeNames,
                    notices: uiState.notices,
                    cafeEvents: uiState.cafeEvents,
                    cafeEventCursor: uiState.cafeEventCursor,
                    canLoadMoreCafeEvents: uiState.canLoadMoreCafeEvents,
                    isLoadingMoreCafeEvents: uiState.isLoadingMoreCafeEvents,
                    communityPosts: uiState.communityPosts
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
                contentLayout: uiState.contentLayout,
                banners: uiState.banners,
                popularCasts: uiState.popularCasts,
                popularCastCafeNames: uiState.popularCastCafeNames,
                popularCastCafeRegions: uiState.popularCastCafeRegions,
                popularCastCursor: uiState.popularCastCursor,
                canLoadMorePopularCasts: uiState.canLoadMorePopularCasts,
                isLoadingMorePopularCasts: uiState.isLoadingMorePopularCasts,
                nearbyCafes: uiState.nearbyCafes,
                nearbyCafeCursor: uiState.nearbyCafeCursor,
                canLoadMoreNearbyCafes: uiState.canLoadMoreNearbyCafes,
                isLoadingMoreNearbyCafes: append,
                birthdayCasts: uiState.birthdayCasts,
                birthdayCastCafeNames: uiState.birthdayCastCafeNames,
                notices: uiState.notices,
                cafeEvents: uiState.cafeEvents,
                cafeEventCursor: uiState.cafeEventCursor,
                canLoadMoreCafeEvents: uiState.canLoadMoreCafeEvents,
                isLoadingMoreCafeEvents: uiState.isLoadingMoreCafeEvents,
                communityPosts: uiState.communityPosts
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
                        contentLayout: uiState.contentLayout,
                        banners: uiState.banners,
                        popularCasts: uiState.popularCasts,
                        popularCastCafeNames: uiState.popularCastCafeNames,
                        popularCastCafeRegions: uiState.popularCastCafeRegions,
                        popularCastCursor: uiState.popularCastCursor,
                        canLoadMorePopularCasts: uiState.canLoadMorePopularCasts,
                        isLoadingMorePopularCasts: uiState.isLoadingMorePopularCasts,
                        nearbyCafes: append ? (uiState.nearbyCafes + (page.items as? [Cafe] ?? [])) : (page.items as? [Cafe] ?? []),
                        nearbyCafeCursor: page.nextCursor,
                        canLoadMoreNearbyCafes: page.hasNext,
                        isLoadingMoreNearbyCafes: false,
                        birthdayCasts: uiState.birthdayCasts,
                        birthdayCastCafeNames: uiState.birthdayCastCafeNames,
                        notices: uiState.notices,
                        cafeEvents: uiState.cafeEvents,
                        cafeEventCursor: uiState.cafeEventCursor,
                        canLoadMoreCafeEvents: uiState.canLoadMoreCafeEvents,
                        isLoadingMoreCafeEvents: uiState.isLoadingMoreCafeEvents,
                        communityPosts: uiState.communityPosts
                    )
                } else {
                    uiState = HomeUiState(
                        isLoggedIn: uiState.isLoggedIn,
                        isLoginPromptVisible: uiState.isLoginPromptVisible,
                        contentLayout: uiState.contentLayout,
                        banners: uiState.banners,
                        popularCasts: uiState.popularCasts,
                        popularCastCafeNames: uiState.popularCastCafeNames,
                        popularCastCafeRegions: uiState.popularCastCafeRegions,
                        popularCastCursor: uiState.popularCastCursor,
                        canLoadMorePopularCasts: uiState.canLoadMorePopularCasts,
                        isLoadingMorePopularCasts: uiState.isLoadingMorePopularCasts,
                        nearbyCafes: uiState.nearbyCafes,
                        nearbyCafeCursor: uiState.nearbyCafeCursor,
                        canLoadMoreNearbyCafes: false,
                        isLoadingMoreNearbyCafes: false,
                        birthdayCasts: uiState.birthdayCasts,
                        birthdayCastCafeNames: uiState.birthdayCastCafeNames,
                        notices: uiState.notices,
                        cafeEvents: uiState.cafeEvents,
                        cafeEventCursor: uiState.cafeEventCursor,
                        canLoadMoreCafeEvents: uiState.canLoadMoreCafeEvents,
                        isLoadingMoreCafeEvents: uiState.isLoadingMoreCafeEvents,
                        communityPosts: uiState.communityPosts
                    )
                }
            } catch {
                if Task.isCancelled { return }
                uiState = HomeUiState(
                    isLoggedIn: uiState.isLoggedIn,
                    isLoginPromptVisible: uiState.isLoginPromptVisible,
                    contentLayout: uiState.contentLayout,
                    banners: uiState.banners,
                    popularCasts: uiState.popularCasts,
                    popularCastCafeNames: uiState.popularCastCafeNames,
                    popularCastCafeRegions: uiState.popularCastCafeRegions,
                    popularCastCursor: uiState.popularCastCursor,
                    canLoadMorePopularCasts: uiState.canLoadMorePopularCasts,
                    isLoadingMorePopularCasts: uiState.isLoadingMorePopularCasts,
                    nearbyCafes: uiState.nearbyCafes,
                    nearbyCafeCursor: uiState.nearbyCafeCursor,
                    canLoadMoreNearbyCafes: false,
                    isLoadingMoreNearbyCafes: false,
                    birthdayCasts: uiState.birthdayCasts,
                    birthdayCastCafeNames: uiState.birthdayCastCafeNames,
                    notices: uiState.notices,
                    cafeEvents: uiState.cafeEvents,
                    cafeEventCursor: uiState.cafeEventCursor,
                    canLoadMoreCafeEvents: uiState.canLoadMoreCafeEvents,
                    isLoadingMoreCafeEvents: uiState.isLoadingMoreCafeEvents,
                    communityPosts: uiState.communityPosts
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
                        contentLayout: uiState.contentLayout,
                        banners: uiState.banners,
                        popularCasts: uiState.popularCasts,
                        popularCastCafeNames: uiState.popularCastCafeNames,
                        popularCastCafeRegions: uiState.popularCastCafeRegions,
                        popularCastCursor: uiState.popularCastCursor,
                        canLoadMorePopularCasts: uiState.canLoadMorePopularCasts,
                        isLoadingMorePopularCasts: uiState.isLoadingMorePopularCasts,
                        nearbyCafes: uiState.nearbyCafes,
                        nearbyCafeCursor: uiState.nearbyCafeCursor,
                        canLoadMoreNearbyCafes: uiState.canLoadMoreNearbyCafes,
                        isLoadingMoreNearbyCafes: uiState.isLoadingMoreNearbyCafes,
                        birthdayCasts: uiState.birthdayCasts,
                        birthdayCastCafeNames: uiState.birthdayCastCafeNames,
                        notices: uiState.notices,
                        cafeEvents: uiState.cafeEvents,
                        cafeEventCursor: uiState.cafeEventCursor,
                        canLoadMoreCafeEvents: uiState.canLoadMoreCafeEvents,
                        isLoadingMoreCafeEvents: uiState.isLoadingMoreCafeEvents,
                        communityPosts: uiState.communityPosts
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

    private func observeContentLayout() {
        tasks[.contentLayout]?.cancel()
        tasks[.contentLayout] = Task {
            do {
                for try await contentLayout in asyncSequence(for: observeContentLayoutUseCase.invoke()) {
                    uiState.contentLayout = AppContentLayout(contentLayout: contentLayout)
                }
            } catch {
                uiState.contentLayout = .fullBleed
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
                        self.loadHomeBanners()
                    case is Shared.BannerEvent.Updated:
                        self.loadHomeBanners()
                    case is Shared.BannerEvent.Deleted:
                        self.loadHomeBanners()
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
                    if event is CafeRegistrationClaimEvent.Approved {
                        self.loadNearbyCafePage(cursor: nil, append: false)
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
                        self.loadBirthdayCasts()
                    case is Shared.CastEvent.Updated:
                        self.loadPopularCastPage(cursor: nil, append: false)
                        self.loadBirthdayCasts()
                    case is Shared.CastEvent.Deleted:
                        self.loadPopularCastPage(cursor: nil, append: false)
                        self.loadBirthdayCasts()
                    default:
                        break
                    }
                }
            } catch {
                print("Error: \(error)")
            }
        }
    }

    private func observeCommunityPostEvents() {
        tasks[.communityPostEvent]?.cancel()
        tasks[.communityPostEvent] = Task {
            do {
                for try await event in asyncSequence(for: communityPostEventPublisher.events) {
                    if event is CommunityPostEvent.Created {
                        self.loadCommunityPosts()
                    } else if let deletedEvent = event as? CommunityPostEvent.Deleted {
                        self.uiState.communityPosts = self.uiState.communityPosts.filter { $0.id != deletedEvent.postId }
                    } else if let updatedEvent = event as? CommunityPostEvent.Updated {
                        self.uiState.communityPosts = self.uiState.communityPosts.map {
                            $0.id == updatedEvent.post.id ? updatedEvent.post : $0
                        }
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
                    case is Shared.CafeEventEvent.Created:
                        loadHomeCafeEvents()
                    case is Shared.CafeEventEvent.Updated:
                        loadHomeCafeEvents()
                    case is Shared.CafeEventEvent.Deleted:
                        loadHomeCafeEvents()
                    default:
                        break
                    }
                }
            } catch {
                print("Error: \(error)")
            }
        }
    }

    private func patchCafeInfo(_ cafe: Cafe) {
        uiState = HomeUiState(
            isLoggedIn: uiState.isLoggedIn,
            isLoginPromptVisible: uiState.isLoginPromptVisible,
            contentLayout: uiState.contentLayout,
            banners: uiState.banners,
            popularCasts: uiState.popularCasts,
            popularCastCafeNames: uiState.popularCastCafeNames.merging([cafe.id: cafe.name]) { _, new in new },
            popularCastCafeRegions: uiState.popularCastCafeRegions.merging([cafe.id: cafe.region.city]) { _, new in new },
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
            birthdayCastCafeNames: uiState.birthdayCastCafeNames.merging([cafe.id: cafe.name]) { _, new in new },
            notices: uiState.notices,
            cafeEvents: uiState.cafeEvents,
            cafeEventCursor: uiState.cafeEventCursor,
            canLoadMoreCafeEvents: uiState.canLoadMoreCafeEvents,
            isLoadingMoreCafeEvents: uiState.isLoadingMoreCafeEvents,
            communityPosts: uiState.communityPosts
        )
    }

    private func isDisplayableCafeEvent(_ statusLabel: String) -> Bool {
        let normalized = statusLabel.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        return normalized.contains("진행 중") ||
            normalized.contains("진행중") ||
            normalized.contains("ongoing") ||
            normalized.contains("예정") ||
            normalized.contains("upcoming") ||
            normalized.contains("scheduled")
    }

    private func requireSignedIn(onAuthenticated: @escaping () -> Void) {
        if uiState.isLoggedIn {
            onAuthenticated()
        } else {
            uiState = HomeUiState(
                isLoggedIn: uiState.isLoggedIn,
                isLoginPromptVisible: true,
                contentLayout: uiState.contentLayout,
                banners: uiState.banners,
                popularCasts: uiState.popularCasts,
                popularCastCafeNames: uiState.popularCastCafeNames,
                popularCastCafeRegions: uiState.popularCastCafeRegions,
                popularCastCursor: uiState.popularCastCursor,
                canLoadMorePopularCasts: uiState.canLoadMorePopularCasts,
                isLoadingMorePopularCasts: uiState.isLoadingMorePopularCasts,
                nearbyCafes: uiState.nearbyCafes,
                nearbyCafeCursor: uiState.nearbyCafeCursor,
                canLoadMoreNearbyCafes: uiState.canLoadMoreNearbyCafes,
                isLoadingMoreNearbyCafes: uiState.isLoadingMoreNearbyCafes,
                birthdayCasts: uiState.birthdayCasts,
                birthdayCastCafeNames: uiState.birthdayCastCafeNames,
                notices: uiState.notices,
                cafeEvents: uiState.cafeEvents,
                cafeEventCursor: uiState.cafeEventCursor,
                canLoadMoreCafeEvents: uiState.canLoadMoreCafeEvents,
                isLoadingMoreCafeEvents: uiState.isLoadingMoreCafeEvents,
                communityPosts: uiState.communityPosts
            )
        }
    }

    private func handleBannerTap(_ banner: HomeBanner) {
        switch banner.targetType {
        case .externalLink:
            if !banner.targetValue.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                event.send(.navigateToExternalLink(title: banner.title, url: banner.targetValue))
            }
        case .eventDetail:
            let eventId = banner.targetValue.trimmingCharacters(in: .whitespacesAndNewlines)
            if let cafeId = banner.cafeId,
               !cafeId.isEmpty,
               !eventId.isEmpty {
                event.send(.navigateToCafeEvent(cafeId: cafeId, eventId: eventId))
            }
        case .cafeDetail, .notice:
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
        case .cafeEventTapped(let cafeId, let eventId):
            event.send(.navigateToCafeEvent(cafeId: cafeId, eventId: eventId))
        case .loginPromptSignInTapped:
            uiState = HomeUiState(
                isLoggedIn: uiState.isLoggedIn,
                isLoginPromptVisible: false,
                contentLayout: uiState.contentLayout,
                banners: uiState.banners,
                popularCasts: uiState.popularCasts,
                popularCastCafeNames: uiState.popularCastCafeNames,
                popularCastCafeRegions: uiState.popularCastCafeRegions,
                popularCastCursor: uiState.popularCastCursor,
                canLoadMorePopularCasts: uiState.canLoadMorePopularCasts,
                isLoadingMorePopularCasts: uiState.isLoadingMorePopularCasts,
                nearbyCafes: uiState.nearbyCafes,
                nearbyCafeCursor: uiState.nearbyCafeCursor,
                canLoadMoreNearbyCafes: uiState.canLoadMoreNearbyCafes,
                isLoadingMoreNearbyCafes: uiState.isLoadingMoreNearbyCafes,
                birthdayCasts: uiState.birthdayCasts,
                birthdayCastCafeNames: uiState.birthdayCastCafeNames,
                notices: uiState.notices,
                cafeEvents: uiState.cafeEvents,
                cafeEventCursor: uiState.cafeEventCursor,
                canLoadMoreCafeEvents: uiState.canLoadMoreCafeEvents,
                isLoadingMoreCafeEvents: uiState.isLoadingMoreCafeEvents,
                communityPosts: uiState.communityPosts
            )
            event.send(.navigateToSignIn)
        case .dismissLoginPrompt:
            uiState = HomeUiState(
                isLoggedIn: uiState.isLoggedIn,
                isLoginPromptVisible: false,
                contentLayout: uiState.contentLayout,
                banners: uiState.banners,
                popularCasts: uiState.popularCasts,
                popularCastCafeNames: uiState.popularCastCafeNames,
                popularCastCafeRegions: uiState.popularCastCafeRegions,
                popularCastCursor: uiState.popularCastCursor,
                canLoadMorePopularCasts: uiState.canLoadMorePopularCasts,
                isLoadingMorePopularCasts: uiState.isLoadingMorePopularCasts,
                nearbyCafes: uiState.nearbyCafes,
                nearbyCafeCursor: uiState.nearbyCafeCursor,
                canLoadMoreNearbyCafes: uiState.canLoadMoreNearbyCafes,
                isLoadingMoreNearbyCafes: uiState.isLoadingMoreNearbyCafes,
                birthdayCasts: uiState.birthdayCasts,
                birthdayCastCafeNames: uiState.birthdayCastCafeNames,
                notices: uiState.notices,
                cafeEvents: uiState.cafeEvents,
                cafeEventCursor: uiState.cafeEventCursor,
                canLoadMoreCafeEvents: uiState.canLoadMoreCafeEvents,
                isLoadingMoreCafeEvents: uiState.isLoadingMoreCafeEvents,
                communityPosts: uiState.communityPosts
            )
        case .loadMoreCafeEvents:
            loadMoreCafeEvents()
        case .loadMorePopularCasts:
            loadMorePopularCasts()
        case .loadMoreNearbyCafes:
            loadMoreNearbyCafes()
        case .communityTapped:
            requireSignedIn { [weak self] in
                self?.event.send(.navigateToCommunity)
            }
        case .communityPostTapped(let postId):
            requireSignedIn { [weak self] in
                self?.event.send(.navigateToPostDetail(postId: postId))
            }
        }
    }

    init(
        getHomeBannersUseCase: GetHomeBannersUseCase = KoinInitializerKt.resolveGetHomeBannersUseCase(),
        getHomeCafeEventsUseCase: GetHomeCafeEventsUseCase = KoinInitializerKt.resolveGetHomeCafeEventsUseCase(),
        getNearbyCafePageUseCase: GetNearbyCafePageUseCase = KoinInitializerKt.resolveGetNearbyCafePageUseCase(),
        getPopularCastPageUseCase: GetPopularCastPageUseCase = KoinInitializerKt.resolveGetPopularCastPageUseCase(),
        getBirthdayCastsUseCase: GetBirthdayCastsUseCase = KoinInitializerKt.resolveGetBirthdayCastsUseCase(),
        getRecentNoticesUseCase: GetRecentNoticesUseCase = KoinInitializerKt.resolveGetRecentNoticesUseCase(),
        getCommunityPostPageUseCase: GetCommunityPostPageUseCase = KoinInitializerKt.resolveGetCommunityPostPageUseCase(),
        observeCurrentUserUseCase: ObserveCurrentUserUseCase = KoinInitializerKt.resolveObserveCurrentUserUseCase(),
        observeContentLayoutUseCase: ObserveContentLayoutUseCase = KoinInitializerKt.resolveObserveContentLayoutUseCase(),
        bannerEventPublisher: BannerEventPublisher = KoinInitializerKt.resolveBannerEventPublisher(),
        cafeEventEventPublisher: CafeEventEventPublisher = KoinInitializerKt.resolveCafeEventEventPublisher(),
        cafeRegistrationClaimEventPublisher: CafeRegistrationClaimEventPublisher = KoinInitializerKt.resolveCafeRegistrationClaimEventPublisher(),
        cafeDetailEventPublisher: CafeDetailEventPublisher = KoinInitializerKt.resolveCafeDetailEventPublisher(),
        castEventPublisher: CastEventPublisher = KoinInitializerKt.resolveCastEventPublisher(),
        communityPostEventPublisher: CommunityPostEventPublisher = KoinInitializerKt.resolveCommunityPostEventPublisher()
    ) {
        self.getHomeBannersUseCase = getHomeBannersUseCase
        self.getHomeCafeEventsUseCase = getHomeCafeEventsUseCase
        self.getNearbyCafePageUseCase = getNearbyCafePageUseCase
        self.getPopularCastPageUseCase = getPopularCastPageUseCase
        self.getBirthdayCastsUseCase = getBirthdayCastsUseCase
        self.getRecentNoticesUseCase = getRecentNoticesUseCase
        self.getCommunityPostPageUseCase = getCommunityPostPageUseCase
        self.observeCurrentUserUseCase = observeCurrentUserUseCase
        self.observeContentLayoutUseCase = observeContentLayoutUseCase
        self.bannerEventPublisher = bannerEventPublisher
        self.cafeEventEventPublisher = cafeEventEventPublisher
        self.cafeRegistrationClaimEventPublisher = cafeRegistrationClaimEventPublisher
        self.cafeDetailEventPublisher = cafeDetailEventPublisher
        self.castEventPublisher = castEventPublisher
        self.communityPostEventPublisher = communityPostEventPublisher

        observeSession()
        observeContentLayout()
        observeBannerEvent()
        observeCafeEventEvent()
        observeCafeRegistrationClaimEvent()
        observeCafeDetailEvent()
        observeCastEvent()
        observeCommunityPostEvents()
        loadInitialHomeSections()
    }

    deinit {
        tasks.values.forEach { $0.cancel() }
        tasks.removeAll()
    }

    private let maxHomeFeedItems: Int32 = 6
    private let maxHomeCafeEvents = 8
    private let maxHomeCommunityPosts: Int32 = 8
    private static let paginationDelayNanoseconds: UInt64 = 1_000_000_000

    private static func dictionary(from source: [AnyHashable: Any]) -> [String: String] {
        source.reduce(into: [:]) { partialResult, entry in
            guard let key = entry.key as? String, let value = entry.value as? String else { return }
            partialResult[key] = value
        }
    }

    private enum TaskKey {
        case homeBanners
        case birthdayCasts
        case recentNotices
        case homeCafeEvents
        case homeCafeEventPage
        case session
        case contentLayout
        case bannerEvent
        case cafeEventEvent
        case cafeRegistrationClaimEvent
        case cafeDetailEvent
        case castEvent
        case communityPostEvent
        case popularCastPage
        case nearbyCafePage
        case communityPosts
    }
}
