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
    
    private let toggleFavoriteCafeUseCase: ToggleFavoriteCafeUseCase
    
    @Published private(set) var uiState = CafeUiState.empty
    
    let event = PassthroughSubject<CafeEvent, Never>()
    
    private var loadTask: Task<Void, Never>?

    private func loadCafeDetail() {
        loadTask?.cancel()
        uiState.isLoading = true
        uiState.errorMessage = nil

        loadTask = Task {
            do {
                let result = try await getCafeDetailUseCase.invoke(cafeId: cafeId)

                if let success = result as? AppResultSuccess<AnyObject>,
                   let feed = success.data as? Shared.CafeDetailFeed {
                    uiState = CafeUiState(
                        isLoading: false,
                        errorMessage: nil,
                        selectedTab: uiState.selectedTab,
                        detail: feed.detail,
                        casts: feed.casts,
                        reviews: feed.reviews,
                        isFavorite: feed.isFavorite,
                        isLoggedIn: feed.isLoggedIn
                    )
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

    private func toggleFavorite() {
        loadTask?.cancel()
        loadTask = Task {
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
        case .refresh:
            loadCafeDetail()
        }
    }

    init(
        cafeId: String,
        getCafeDetailUseCase: GetCafeDetailUseCase = KoinInitializerKt.resolveGetCafeDetailUseCase(),
        toggleFavoriteCafeUseCase: ToggleFavoriteCafeUseCase = KoinInitializerKt.resolveToggleFavoriteCafeUseCase()
    ) {
        self.cafeId = cafeId
        self.getCafeDetailUseCase = getCafeDetailUseCase
        self.toggleFavoriteCafeUseCase = toggleFavoriteCafeUseCase
        
        loadCafeDetail()
    }
    
    deinit {
        loadTask?.cancel()
    }
}
