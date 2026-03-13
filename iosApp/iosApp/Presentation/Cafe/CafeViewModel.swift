//
//  CafeViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/06.
//

import Foundation
import Combine
import Shared

@MainActor
final class CafeViewModel: ObservableObject {
    private let cafeId: String
    
    private let getCafeDetailUseCase: GetCafeDetailUseCase

    private let getCafeCastListPageUseCase: GetCafeCastListPageUseCase

    private let getCafeNoticePageUseCase: GetCafeNoticePageUseCase

    private let getCafeReviewPageUseCase: GetCafeReviewPageUseCase

    private let observeCafeDetailUseCase: ObserveCafeDetailUseCase

    private let observeReviewEventUseCase: ObserveReviewEventUseCase
    
    private let toggleFavoriteCafeUseCase: ToggleFavoriteCafeUseCase
    
    @Published private(set) var uiState = CafeUiState.empty
    
    let event = PassthroughSubject<CafeEvent, Never>()
    
    private var tasks: [TaskKey: Task<Void, Never>] = [:]

    private var watchHandles: [WatchKey: WatchHandle] = [:]

    private func bindCafeDetail() {
        watchHandles[.cafeDetail]?.cancel()
        var isInitialEmission = true
        watchHandles[.cafeDetail] = observeCafeDetailUseCase.watch(cafeId: cafeId) { [weak self] _ in
            guard let self else { return }
            if isInitialEmission {
                isInitialEmission = false
                return
            }
            self.loadCafeDetail()
        }
    }

    private func observeReviewEvent() {
        watchHandles[.reviewEvent]?.cancel()
        watchHandles[.reviewEvent] = observeReviewEventUseCase.watch { [weak self] event in
            guard let self else { return }
            if let created = event as? ReviewEvent.Created {
                if created.cafeId == self.cafeId, self.uiState.selectedTab == .reviews {
                    self.event.send(.scrollReviewsToTop)
                    self.loadCafeDetail(refreshReviews: false)
                    self.refreshReviewPage()
                }
            } else if let deleted = event as? ReviewEvent.Deleted {
                if deleted.cafeId == self.cafeId, self.uiState.selectedTab == .reviews {
                    self.uiState.reviews.removeAll { $0.id == deleted.reviewId }
                }
            }
        }
    }

    private func loadCafeDetail(refreshReviews: Bool = true) {
        uiState.isLoading = true
        uiState.errorMessage = nil

        tasks[.detail]?.cancel()
        tasks[.detail] = Task {
            do {
                let result = try await getCafeDetailUseCase.invoke(cafeId: cafeId)

                if let success = result as? AppResultSuccess<AnyObject>,
                   let feed = success.data as? Shared.CafeDetailFeed {
                    uiState = CafeUiState(
                        isLoading: false,
                        isLoadingMoreCasts: uiState.isLoadingMoreCasts,
                        isLoadingMoreNotices: uiState.isLoadingMoreNotices,
                        errorMessage: nil,
                        selectedTab: uiState.selectedTab,
                        detail: feed.detail,
                        casts: uiState.casts,
                        castsNextCursor: uiState.castsNextCursor,
                        canLoadMoreCasts: uiState.canLoadMoreCasts,
                        noticesNextCursor: uiState.noticesNextCursor,
                        canLoadMoreNotices: uiState.canLoadMoreNotices,
                        notices: uiState.notices,
                        isLoadingMoreReviews: uiState.isLoadingMoreReviews,
                        reviewsNextCursor: feed.reviewsNextCursor,
                        canLoadMoreReviews: feed.canLoadMoreReviews,
                        reviews: feed.reviews,
                        isFavorite: feed.isFavorite,
                        isLoggedIn: feed.isLoggedIn
                    )
                    refreshCastPage()
                    if uiState.selectedTab == .notices, uiState.notices.isEmpty {
                        refreshNoticePage()
                    }
                    if refreshReviews, uiState.selectedTab == .reviews {
                        refreshReviewPage()
                    }
                } else if result is AppResultFailure {
                    uiState.isLoading = false
                    uiState.errorMessage = "카페 상세 데이터를 불러오지 못했습니다."
                } else {
                    uiState.isLoading = false
                    uiState.errorMessage = "카페 상세 데이터를 불러오지 못했습니다."
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isLoading = false
                uiState.errorMessage = "카페 상세 데이터를 불러오지 못했습니다."
            }
        }
    }

    private func loadCastPage(cursor: String?, append: Bool) {
        tasks[.castPage]?.cancel()
        tasks[.castPage] = Task {
            uiState.isLoadingMoreCasts = append

            do {
                let result = try await getCafeCastListPageUseCase.invoke(cafeId: self.cafeId, cursor: cursor)

                if let success = result as? AppResultSuccess<AnyObject>,
                   let page = success.data as? PagedResult<CafeDetailCast> {
                    uiState.casts = append ? (uiState.casts + page.items as! [CafeDetailCast]) : page.items as! [CafeDetailCast]
                    uiState.castsNextCursor = page.nextCursor
                    uiState.canLoadMoreCasts = page.hasNext
                    uiState.isLoadingMoreCasts = false
                } else {
                    uiState.isLoadingMoreCasts = false
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isLoadingMoreCasts = false
            }
        }
    }

    private func refreshCastPage() {
        loadCastPage(cursor: nil, append: false)
    }

    private func loadMoreCasts() {
        guard uiState.canLoadMoreCasts,
              !uiState.isLoadingMoreCasts,
              let cursor = uiState.castsNextCursor else { return }
        loadCastPage(cursor: cursor, append: true)
    }

    private func loadNoticePage(cursor: String?, append: Bool) {
        tasks[.noticePage]?.cancel()
        tasks[.noticePage] = Task {
            uiState.isLoadingMoreNotices = append

            do {
                let result = try await getCafeNoticePageUseCase.invoke(cafeId: self.cafeId, query: "", cursor: cursor, pageSize: 15)

                if let success = result as? AppResultSuccess<AnyObject>,
                   let page = success.data as? PagedResult<CafeNoticeManagementItem> {
                    let items = (page.items as! [CafeNoticeManagementItem]).map(mapNotice)
                    uiState.notices = append ? (uiState.notices + items) : items
                    uiState.noticesNextCursor = page.nextCursor
                    uiState.canLoadMoreNotices = page.hasNext
                    uiState.isLoadingMoreNotices = false
                } else {
                    uiState.isLoadingMoreNotices = false
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isLoadingMoreNotices = false
            }
        }
    }

    private func refreshNoticePage() {
        loadNoticePage(cursor: nil, append: false)
    }

    private func loadMoreNotices() {
        guard uiState.canLoadMoreNotices,
              !uiState.isLoadingMoreNotices,
              let cursor = uiState.noticesNextCursor else { return }
        loadNoticePage(cursor: cursor, append: true)
    }

    private func loadReviewPage(cursor: String?, append: Bool) {
        tasks[.reviewPage]?.cancel()
        tasks[.reviewPage] = Task {
            uiState.isLoadingMoreReviews = append

            do {
                let result = try await getCafeReviewPageUseCase.invoke(cafeId: self.cafeId, cursor: cursor, pageSize: 15)

                if let success = result as? AppResultSuccess<AnyObject>,
                   let page = success.data as? PagedResult<CafeDetailReview> {
                    let items = page.items as! [CafeDetailReview]
                    uiState.reviews = append ? (uiState.reviews + items) : items
                    uiState.reviewsNextCursor = page.nextCursor
                    uiState.canLoadMoreReviews = page.hasNext
                    uiState.isLoadingMoreReviews = false
                } else {
                    uiState.isLoadingMoreReviews = false
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isLoadingMoreReviews = false
            }
        }
    }

    private func refreshReviewPage() {
        loadReviewPage(cursor: nil, append: false)
    }

    private func loadMoreReviews() {
        guard uiState.canLoadMoreReviews,
              !uiState.isLoadingMoreReviews,
              let cursor = uiState.reviewsNextCursor else { return }
        loadReviewPage(cursor: cursor, append: true)
    }

    private func toggleFavorite() {
        Task {
            do {
                let result = try await toggleFavoriteCafeUseCase.invoke(cafeId: cafeId)

                if let failure = result as? AppResultFailure {
                    if failure.error is AppErrorUnauthorized {
                        event.send(.navigateToSignIn)
                    }
                } else {
                    uiState.isLoggedIn = true
                    loadCafeDetail()
                }
            } catch {
                if Task.isCancelled { return }
            }
        }
    }

    private func writeReview() {
        event.send(.navigateToReviewEdit(cafeId: cafeId))
    }

    func onAction(_ action: CafeAction) {
        switch action {
        case .backTapped:
            event.send(.navigateBack)
        case .changeTab(let tab):
            uiState.selectedTab = tab
            if tab == .notices, uiState.notices.isEmpty {
                refreshNoticePage()
            }
            if tab == .reviews, uiState.reviews.isEmpty {
                refreshReviewPage()
            }
        case .maidTapped(let id):
            event.send(.navigateToCast(id: id))
        case .favoriteTapped:
            toggleFavorite()
        case .writeReviewTapped:
            writeReview()
        case .loadMoreCasts:
            loadMoreCasts()
        case .loadMoreNotices:
            loadMoreNotices()
        case .loadMoreReviews:
            loadMoreReviews()
        case .refresh:
            loadCafeDetail()
        }
    }

    init(
        cafeId: String,
        getCafeDetailUseCase: GetCafeDetailUseCase = KoinInitializerKt.resolveGetCafeDetailUseCase(),
        getCafeCastListPageUseCase: GetCafeCastListPageUseCase = KoinInitializerKt.resolveGetCafeCastListPageUseCase(),
        getCafeNoticePageUseCase: GetCafeNoticePageUseCase = KoinInitializerKt.resolveGetCafeNoticePageUseCase(),
        getCafeReviewPageUseCase: GetCafeReviewPageUseCase = KoinInitializerKt.resolveGetCafeReviewPageUseCase(),
        observeCafeDetailUseCase: ObserveCafeDetailUseCase = KoinInitializerKt.resolveObserveCafeDetailUseCase(),
        observeReviewEventUseCase: ObserveReviewEventUseCase = KoinInitializerKt.resolveObserveReviewEventUseCase(),
        toggleFavoriteCafeUseCase: ToggleFavoriteCafeUseCase = KoinInitializerKt.resolveToggleFavoriteCafeUseCase()
    ) {
        self.cafeId = cafeId
        self.getCafeDetailUseCase = getCafeDetailUseCase
        self.getCafeCastListPageUseCase = getCafeCastListPageUseCase
        self.getCafeNoticePageUseCase = getCafeNoticePageUseCase
        self.getCafeReviewPageUseCase = getCafeReviewPageUseCase
        self.observeCafeDetailUseCase = observeCafeDetailUseCase
        self.observeReviewEventUseCase = observeReviewEventUseCase
        self.toggleFavoriteCafeUseCase = toggleFavoriteCafeUseCase

        bindCafeDetail()
        observeReviewEvent()
        loadCafeDetail()
    }
    
    deinit {
        watchHandles.values.forEach { $0.cancel() }
        watchHandles.removeAll()
        tasks.values.forEach { $0.cancel() }
        tasks.removeAll()
    }

    private enum TaskKey {
        case detail
        case castPage
        case noticePage
        case reviewPage
    }

    private enum WatchKey {
        case cafeDetail
        case reviewEvent
    }

    private func mapNotice(_ item: CafeNoticeManagementItem) -> NoticeItem {
        let accent: NoticeStatusAccent
        switch item.statusAccent {
        case .published:
            accent = .published
        case .draft:
            accent = .draft
        case .ended:
            accent = .ended
        default:
            accent = .published
        }

        return NoticeItem(
            id: item.id,
            title: item.title,
            content: item.content,
            date: item.displayDate,
            isPinned: item.isPinned,
            statusLabel: item.statusLabel,
            statusAccent: accent
        )
    }
}
