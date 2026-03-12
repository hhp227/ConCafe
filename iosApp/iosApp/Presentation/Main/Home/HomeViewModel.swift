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
    
    @Published private(set) var uiState = HomeUiState.empty
    
    let event = PassthroughSubject<HomeEvent, Never>()
    
    private var loadTask: Task<Void, Never>?

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
        getHomeFeedUseCase: GetHomeFeedUseCase = KoinInitializerKt.resolveGetHomeFeedUseCase()
    ) {
        self.getHomeFeedUseCase = getHomeFeedUseCase
        
        loadHomeFeed()
    }
    
    deinit {
        loadTask?.cancel()
    }
}
