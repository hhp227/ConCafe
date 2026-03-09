//
//  CheckInViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/06.
//

import Foundation
import Combine
import Shared

@MainActor
final class CheckInViewModel: ObservableObject {
    private let getCheckInGuestFeedUseCase: GetCheckInGuestFeedUseCase

    private let getCheckInUserFeedUseCase: GetCheckInUserFeedUseCase

    private let observeCurrentUserUseCase: ObserveCurrentUserUseCase

    @Published private(set) var uiState = CheckInUiState.empty

    let event = PassthroughSubject<CheckInEvent, Never>()

    private var tasks: [TaskKey: Task<Void, Never>] = [:]

    private var sessionWatchHandle: WatchHandle?

    private func loadGuestFeed() {
        uiState.isLoading = true
        uiState.errorMessage = nil

        tasks[.guestFeed]?.cancel()
        tasks[.guestFeed] = Task {
            do {
                let result = try await getCheckInGuestFeedUseCase.invoke()

                if let success = result as? AppResultSuccess<AnyObject>,
                   let feed = success.data as? Shared.CheckInGuestFeed {
                    uiState.isLoading = false
                    uiState.errorMessage = nil
                    uiState.currentLocationLabel = feed.currentLocationLabel
                    uiState.mapCafes = feed.mapCafes
                    uiState.popularCafes = feed.popularCafes
                    uiState.popularCasts = feed.popularCasts
                } else if let failure = result as? AppResultFailure {
                    uiState.isLoading = false
                    uiState.errorMessage = "\(failure.error)"
                } else {
                    uiState.isLoading = false
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isLoading = false
                uiState.errorMessage = error.localizedDescription
            }
        }
    }

    private func loadUserFeed() {
        tasks[.userFeed]?.cancel()
        tasks[.userFeed] = Task {
            do {
                let result = try await getCheckInUserFeedUseCase.invoke()

                if let success = result as? AppResultSuccess<AnyObject>,
                   let feed = success.data as? Shared.CheckInUserFeed {
                    uiState.todayVisits = feed.todayVisits
                    uiState.recentVisits = feed.recentVisits
                } else if let failure = result as? AppResultFailure {
                    uiState.todayVisits = []
                    uiState.recentVisits = []
                    uiState.errorMessage = "\(failure.error)"
                } else {
                    uiState.todayVisits = []
                    uiState.recentVisits = []
                }
            } catch {
                if Task.isCancelled { return }
                uiState.todayVisits = []
                uiState.recentVisits = []
                uiState.errorMessage = error.localizedDescription
            }
        }
    }

    private func observeSession() {
        sessionWatchHandle = observeCurrentUserUseCase.watch { [weak self] user in
            guard let self else { return }

            Task { @MainActor in
                self.uiState.currentUser = user
                self.uiState.isLoginPromptVisible = user == nil ? self.uiState.isLoginPromptVisible : false

                if user == nil {
                    self.uiState.todayVisits = []
                    self.uiState.recentVisits = []
                } else {
                    self.loadUserFeed()
                }
            }
        }
    }

    func onAction(_ action: CheckInAction) {
        switch action {
        case .cafeTapped(let id):
            event.send(.navigateToCafe(id: id))
        case .castTapped(let id):
            event.send(.navigateToCast(id: id))
        case .checkInTapped:
            if uiState.currentUser == nil {
                uiState.isLoginPromptVisible = true
            }
        case .signInTapped, .signUpTapped:
            uiState.isLoginPromptVisible = false
            event.send(.navigateToSignIn)
        case .dismissLoginPrompt:
            uiState.isLoginPromptVisible = false
        }
    }

    init(
        getCheckInGuestFeedUseCase: GetCheckInGuestFeedUseCase = KoinInitializerKt.resolveGetCheckInGuestFeedUseCase(),
        getCheckInUserFeedUseCase: GetCheckInUserFeedUseCase = KoinInitializerKt.resolveGetCheckInUserFeedUseCase(),
        observeCurrentUserUseCase: ObserveCurrentUserUseCase = KoinInitializerKt.resolveObserveCurrentUserUseCase()
    ) {
        self.getCheckInGuestFeedUseCase = getCheckInGuestFeedUseCase
        self.getCheckInUserFeedUseCase = getCheckInUserFeedUseCase
        self.observeCurrentUserUseCase = observeCurrentUserUseCase

        observeSession()
        loadGuestFeed()
    }

    deinit {
        tasks.values.forEach { $0.cancel() }
        tasks.removeAll()
        sessionWatchHandle?.cancel()
    }

    private enum TaskKey {
        case guestFeed
        case userFeed
    }
}
