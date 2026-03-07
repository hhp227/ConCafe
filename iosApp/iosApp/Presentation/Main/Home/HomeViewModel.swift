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
                let result = try await getHomeFeedUseCase.invoke(limit: 10)

                if let success = result as? AppResultSuccess<AnyObject> {
                    guard let feed = success.data as? Shared.HomeFeed else {
                        uiState = .empty
                        return
                    }
                    uiState = HomeUiState(
                        banners: feed.banners,
                        popularCasts: feed.popularCasts,
                        nearbyCafes: feed.nearbyCafes,
                        birthdayCasts: feed.birthdayCasts,
                        notices: feed.notices
                    )
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

    func onAction(_ action: HomeAction) {
        switch action {
        case .maidTapped(let id):
            event.send(.navigateToCast(id: id))
        case .birthdayMaidTapped(let id):
            event.send(.navigateToCast(id: id))
        case .cafeTapped(let id):
            event.send(.navigateToCafe(id: id))
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
