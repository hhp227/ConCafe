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
                guard let feed = try await getHomeFeedUseCase.getHomeFeedOrNull(limit: 10) else {
                    uiState = .empty
                    return
                }
                uiState = mapHomeFeed(feed)
            } catch {
                uiState = .empty
            }
        }
    }
    
    private func mapHomeFeed(_ feed: Shared.HomeFeed) -> HomeUiState {
        return HomeUiState(
            banners: feed.banners as? [Shared.HomeBanner] ?? [],
            popularCasts: feed.popularCasts as? [Shared.Cast] ?? [],
            nearbyCafes: feed.nearbyCafes as? [Shared.Cafe] ?? [],
            birthdayCasts: feed.birthdayCasts as? [Shared.Cast] ?? [],
            notices: feed.notices as? [Shared.Notice] ?? []
        )
    }

    func onAction(_ action: HomeAction) {
        switch action {
        case .maidTapped(let id):
            event.send(.navigateToCastDetail(id: id))
        case .birthdayMaidTapped(let id):
            event.send(.navigateToCastDetail(id: id))
        case .cafeTapped(let id):
            event.send(.navigateToCafeDetail(id: id))
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
