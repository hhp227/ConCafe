//
//  CafeViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/06.
//

import Foundation
import Combine
import Shared
import KMPNativeCoroutinesAsync

@MainActor
final class CafeViewModel: ObservableObject {
    private let cafeId: String
    
    private let getCafeDetailUseCase: GetCafeDetailUseCase

    private let getCafeCastListPageUseCase: GetCafeCastListPageUseCase

    private let getCafeNoticePageUseCase: GetCafeNoticePageUseCase

    private let getCafeReviewPageUseCase: GetCafeReviewPageUseCase

    private let toggleFavoriteCafeUseCase: ToggleFavoriteCafeUseCase

    private let deleteReviewUseCase: DeleteReviewUseCase

    private let cafeDetailEventPublisher: CafeDetailEventPublisher

    private let reviewEventPublisher: ReviewEventPublisher
    
    @Published private(set) var uiState = CafeUiState.empty
    
    let event = PassthroughSubject<CafeEvent, Never>()
    
    private var tasks: [TaskKey: Task<Void, Never>] = [:]

    private func observeCafeDetailEvent() {
        tasks[.cafeDetail]?.cancel()
        tasks[.cafeDetail] = Task {
            do {
                for try await event in asyncSequence(for: cafeDetailEventPublisher.events) {
                    switch event {
                    case is CafeDetailEvent.CafeInfoUpdated:
                        self.loadCafeDetail()
                    case let favorite as CafeDetailEvent.FavoriteToggled:
                        if favorite.cafeId == self.cafeId {
                            self.uiState.isFavorite = favorite.isFavorite
                        }
                    case is CafeDetailEvent.GoodsCreated,
                        is CafeDetailEvent.GoodsDeleted,
                        is CafeDetailEvent.GoodsUpdated,
                        is CafeDetailEvent.MenuCreated,
                        is CafeDetailEvent.MenuDeleted,
                        is CafeDetailEvent.MenuUpdated:
                        break
                    default:
                        break
                    }
                }
            } catch {
                print("Error: \(error)")
            }
        }
    }
    
    private func observeReviewEvent() {
        tasks[.reviewEvent]?.cancel()
        tasks[.reviewEvent] = Task {
            do {
                for try await event in asyncSequence(for: reviewEventPublisher.events) {
                    switch event {
                    case let created as ReviewEvent.Created:
                        if created.cafeId == cafeId && self.uiState.selectedTab == .reviews {
                            await MainActor.run {
                                self.uiState.shouldScrollToTopOnReturn = true
                            }
                            self.refreshReviewPage()
                        }
                    case let deleted as ReviewEvent.Deleted:
                        if deleted.cafeId == cafeId && self.uiState.selectedTab == .reviews {
                            await MainActor.run {
                                self.uiState.reviews.removeAll {
                                    $0.id == deleted.reviewId
                                }
                            }
                        }
                    default:
                        break
                    }
                }
            } catch {
                print("Error: \(error)")
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
                        errorMessage: nil,
                        selectedTab: uiState.selectedTab,
                        detail: feed.detail,
                        casts: uiState.casts,
                        castsNextCursor: uiState.castsNextCursor,
                        canLoadMoreCasts: uiState.canLoadMoreCasts,
                        isLoadingMoreNotices: uiState.isLoadingMoreNotices,
                        noticesNextCursor: uiState.noticesNextCursor,
                        canLoadMoreNotices: uiState.canLoadMoreNotices,
                        notices: uiState.notices,
                        isLoadingMoreReviews: uiState.isLoadingMoreReviews,
                        reviewsNextCursor: feed.reviewsNextCursor,
                        canLoadMoreReviews: feed.canLoadMoreReviews,
                        reviews: feed.reviews,
                        isFavorite: feed.isFavorite,
                        isLoggedIn: feed.isLoggedIn,
                        isVisitVerified: feed.isVisitVerified,
                        shouldScrollToTopOnReturn: uiState.shouldScrollToTopOnReturn,
                        currentUserId: feed.currentUserId
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
                    uiState.errorMessage = nil
                } else {
                    uiState.isLoading = false
                    uiState.errorMessage = nil
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isLoading = false
                uiState.errorMessage = nil
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
                    let items = page.items as! [CafeNoticeManagementItem]
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

                if let success = result as? AppResultSuccess<AnyObject>,
                   let isFavorite = success.data as? NSNumber {
                    uiState.isFavorite = isFavorite.boolValue
                    uiState.isLoggedIn = true
                } else if let failure = result as? AppResultFailure {
                    if failure.error is AppErrorUnauthorized {
                        event.send(.navigateToSignIn)
                    }
                } else {
                    uiState.isLoggedIn = true
                }
            } catch {
                if Task.isCancelled { return }
            }
        }
    }

    private func writeReview() {
        if !uiState.isLoggedIn {
            event.send(.navigateToSignIn)
        } else {
            event.send(.navigateToReviewEdit(cafeId: cafeId, reviewId: nil))
        }
    }

    private func deleteReview(reviewId: String) {
        Task {
            do {
                let result = try await deleteReviewUseCase.invoke(cafeId: cafeId, reviewId: reviewId)

                if result is AppResultFailure {
                    event.send(.showReviewDeleteFailedMessage)
                }
            } catch {
                if Task.isCancelled { return }
                event.send(.showReviewDeleteFailedMessage)
            }
        }
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
        case .consumeScrollToTopOnReturn:
            uiState.shouldScrollToTopOnReturn = false
        case .editReview(let reviewId):
            event.send(.navigateToReviewEdit(cafeId: cafeId, reviewId: reviewId))
        case .deleteReview(let reviewId):
            deleteReview(reviewId: reviewId)
        case .reportReview:
            event.send(.showReviewReportedMessage)
        }
    }

    init(
        cafeId: String,
        getCafeDetailUseCase: GetCafeDetailUseCase = KoinInitializerKt.resolveGetCafeDetailUseCase(),
        getCafeCastListPageUseCase: GetCafeCastListPageUseCase = KoinInitializerKt.resolveGetCafeCastListPageUseCase(),
        getCafeNoticePageUseCase: GetCafeNoticePageUseCase = KoinInitializerKt.resolveGetCafeNoticePageUseCase(),
        getCafeReviewPageUseCase: GetCafeReviewPageUseCase = KoinInitializerKt.resolveGetCafeReviewPageUseCase(),
        toggleFavoriteCafeUseCase: ToggleFavoriteCafeUseCase = KoinInitializerKt.resolveToggleFavoriteCafeUseCase(),
        deleteReviewUseCase: DeleteReviewUseCase = KoinInitializerKt.resolveDeleteReviewUseCase(),
        cafeDetailEventPublisher: CafeDetailEventPublisher = KoinInitializerKt.resolveCafeDetailEventPublisher(),
        reviewEventPublisher: ReviewEventPublisher = KoinInitializerKt.resolveReviewEventPublisher()
    ) {
        self.cafeId = cafeId
        self.getCafeDetailUseCase = getCafeDetailUseCase
        self.getCafeCastListPageUseCase = getCafeCastListPageUseCase
        self.getCafeNoticePageUseCase = getCafeNoticePageUseCase
        self.getCafeReviewPageUseCase = getCafeReviewPageUseCase
        self.toggleFavoriteCafeUseCase = toggleFavoriteCafeUseCase
        self.deleteReviewUseCase = deleteReviewUseCase
        self.cafeDetailEventPublisher = cafeDetailEventPublisher
        self.reviewEventPublisher = reviewEventPublisher

        observeCafeDetailEvent()
        observeReviewEvent()
        loadCafeDetail()
    }
    
    deinit {
        tasks.values.forEach { $0.cancel() }
        tasks.removeAll()
    }

    private enum TaskKey {
        case detail
        case castPage
        case noticePage
        case reviewPage
        case cafeDetail
        case reviewEvent
    }
}
