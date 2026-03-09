//
//  MyInfoViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/06.
//

import Foundation
import Combine
import Shared

@MainActor
final class MyInfoViewModel: ObservableObject {
    private let getMyInfoUseCase: GetMyInfoUseCase
    
    private let observeCurrentUserUseCase: ObserveCurrentUserUseCase

    @Published private(set) var uiState = MyInfoUiState.empty

    let event = PassthroughSubject<MyInfoEvent, Never>()

    private var loadTask: Task<Void, Never>?
    
    private var sessionWatchHandle: WatchHandle?

    private func observeSession() {
        sessionWatchHandle = observeCurrentUserUseCase.watch { [weak self] _ in
            guard let self else { return }

            Task { @MainActor in
                self.loadMyInfo()
            }
        }
    }

    private func loadMyInfo() {
        loadTask?.cancel()
        uiState.isLoading = true
        uiState.errorMessage = nil

        loadTask = Task {
            do {
                let result = try await getMyInfoUseCase.invoke()

                if let success = result as? AppResultSuccess<AnyObject>,
                let feed = success.data as? Shared.MyInfoFeed {
                    uiState = MyInfoUiState(
                        isLoading: false,
                        errorMessage: nil,
                        isLoggedIn: feed.isLoggedIn,
                        user: feed.user,
                        summary: feed.summary,
                        badges: feed.badges,
                        popularCafes: feed.popularCafes,
                        recentVisits: feed.recentVisits,
                        favorites: feed.favorites,
                        followedMaids: feed.followedMaids
                    )
                } else if let failure = result as? AppResultFailure {
                    uiState = .empty
                    uiState.errorMessage = "\(failure.error)"
                } else {
                    uiState = .empty
                }
            } catch {
                if Task.isCancelled { return }
                uiState = .empty
                uiState.errorMessage = error.localizedDescription
            }
        }
    }

    func onAction(_ action: MyInfoAction) {
        switch action {
        case .cafeTapped(let id):
            event.send(.navigateToCafe(id: id))
        case .maidTapped(let id):
            event.send(.navigateToCast(id: id))
        case .signInTapped:
            event.send(.navigateToSignIn)
        case .refresh:
            loadMyInfo()
        }
    }

    init(
        getMyInfoUseCase: GetMyInfoUseCase = KoinInitializerKt.resolveGetMyInfoUseCase(),
        observeCurrentUserUseCase: ObserveCurrentUserUseCase = KoinInitializerKt.resolveObserveCurrentUserUseCase()
    ) {
        self.getMyInfoUseCase = getMyInfoUseCase
        self.observeCurrentUserUseCase = observeCurrentUserUseCase
        
        observeSession()
    }

    deinit {
        loadTask?.cancel()
        sessionWatchHandle?.cancel()
    }
}
