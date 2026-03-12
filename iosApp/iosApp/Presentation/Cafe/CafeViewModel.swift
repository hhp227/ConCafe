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

    private let observeCafeDetailUseCase: ObserveCafeDetailUseCase
    
    private let toggleFavoriteCafeUseCase: ToggleFavoriteCafeUseCase
    
    @Published private(set) var uiState = CafeUiState.empty
    
    let event = PassthroughSubject<CafeEvent, Never>()
    
    private var tasks: [TaskKey: Task<Void, Never>] = [:]

    private var cafeDetailWatchHandle: WatchHandle?

    private func bindCafeDetail() {
        cafeDetailWatchHandle?.cancel()
        var isInitialEmission = true
        cafeDetailWatchHandle = observeCafeDetailUseCase.watch(cafeId: cafeId) { [weak self] _ in
            guard let self else { return }
            if isInitialEmission {
                isInitialEmission = false
                return
            }
            self.loadCafeDetail()
        }
    }

    private func loadCafeDetail() {
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
                        reviews: feed.reviews,
                        isFavorite: feed.isFavorite,
                        isLoggedIn: feed.isLoggedIn
                    )
                    refreshCastPage()
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
        case .maidTapped(let id):
            event.send(.navigateToCast(id: id))
        case .favoriteTapped:
            toggleFavorite()
        case .writeReviewTapped:
            writeReview()
        case .loadMoreCasts:
            loadMoreCasts()
        case .refresh:
            loadCafeDetail()
        }
    }

    init(
        cafeId: String,
        getCafeDetailUseCase: GetCafeDetailUseCase = KoinInitializerKt.resolveGetCafeDetailUseCase(),
        getCafeCastListPageUseCase: GetCafeCastListPageUseCase = KoinInitializerKt.resolveGetCafeCastListPageUseCase(),
        observeCafeDetailUseCase: ObserveCafeDetailUseCase = KoinInitializerKt.resolveObserveCafeDetailUseCase(),
        toggleFavoriteCafeUseCase: ToggleFavoriteCafeUseCase = KoinInitializerKt.resolveToggleFavoriteCafeUseCase()
    ) {
        self.cafeId = cafeId
        self.getCafeDetailUseCase = getCafeDetailUseCase
        self.getCafeCastListPageUseCase = getCafeCastListPageUseCase
        self.observeCafeDetailUseCase = observeCafeDetailUseCase
        self.toggleFavoriteCafeUseCase = toggleFavoriteCafeUseCase

        bindCafeDetail()
        loadCafeDetail()
    }
    
    deinit {
        cafeDetailWatchHandle?.cancel()
        tasks.values.forEach { $0.cancel() }
        tasks.removeAll()
    }

    private enum TaskKey {
        case detail
        case castPage
    }
}
