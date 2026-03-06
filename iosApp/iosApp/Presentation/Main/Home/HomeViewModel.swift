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
    private let homeUseCaseWrapper: HomeUseCaseWrapper
    
    @Published private(set) var uiState = HomeUiState.empty

    let event = PassthroughSubject<HomeEvent, Never>()
    
    private var loadTask: Task<Void, Never>?

    private func loadHomeFeed() {
        loadTask?.cancel()
        loadTask = Task {
            do {
                guard let feed = try await homeUseCaseWrapper.getHomeFeedOrNull(limit: 10) else {
                    uiState = .empty
                    return
                }
                uiState = HomeUiState(
                    banners: feed.banners as? [Shared.HomeBanner] ?? [],
                    popularCasts: feed.popularCasts as? [Shared.Cast] ?? [],
                    nearbyCafes: feed.nearbyCafes as? [Shared.Cafe] ?? [],
                    birthdayCasts: feed.birthdayCasts as? [Shared.Cast] ?? [],
                    notices: feed.notices as? [Shared.Notice] ?? []
                )
            } catch {
                uiState = .empty
            }
        }
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
        homeUseCaseWrapper: HomeUseCaseWrapper = KoinInitializerKt.resolveHomeUseCaseWrapper()
    ) {
        self.homeUseCaseWrapper = homeUseCaseWrapper
        
        loadHomeFeed()
    }
    
    deinit {
        loadTask?.cancel()
    }
}
