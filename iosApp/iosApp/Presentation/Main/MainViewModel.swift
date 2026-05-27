//
//  MainViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import Foundation
import Combine
import Shared
import KMPNativeCoroutinesAsync

@MainActor
final class MainViewModel: ObservableObject {
    private let getMainNavigationUseCase: GetMainNavigationUseCase

    private let observeCurrentUserUseCase: ObserveCurrentUserUseCase

    private let restoreSessionUseCase: RestoreSessionUseCase

    @Published private(set) var uiState = MainUiState.empty

    let event = PassthroughSubject<MainEvent, Never>()

    private var tasks: [TaskKey: Task<Void, Never>] = [:]

    private func refreshNavigation(preferredRoute: String?) {
        tasks[.refreshNavigation]?.cancel()
        tasks[.refreshNavigation] = Task {
            do {
                let result = try await getMainNavigationUseCase.invoke(preferredRoute: preferredRoute)
                guard !Task.isCancelled else { return }

                if let success = result as? AppResultSuccess<AnyObject>,
                   let state = success.data as? MainNavigationState {
                    if state.currentUser?.signupCompleted == false {
                        event.send(.navigateToSignUp)
                    }
                    uiState = MainUiState(
                        currentUser: state.currentUser,
                        tabs: state.tabs,
                        selectedTab: state.selectedTab,
                        thirdTab: state.thirdTab
                    )
                } else if let failure = result as? AppResultFailure {
                    event.send(.showError(message: "\(failure.error)"))
                }
            } catch {
                guard !Task.isCancelled else { return }
                event.send(.showError(message: error.localizedDescription))
            }
        }
    }

    private func selectTab(_ route: String) {
        guard uiState.tabs.contains(where: { $0.route == route }) || route == MainNavigationTab.community.route else { return }
        uiState.selectedTab = route
    }

    private func observeSession() {
        tasks[.observeSession]?.cancel()
        tasks[.observeSession] = Task {
            do {
                for try await newUser in asyncSequence(for: observeCurrentUserUseCase.invoke()) {
                    guard !Task.isCancelled else { return }
                    let currentUser = self.uiState.currentUser
                    let roleChanged = newUser?.role != currentUser?.role
                    let loginStateChanged = (newUser == nil) != (currentUser == nil)

                    if roleChanged || loginStateChanged {
                        // Only rebuild navigation when login state or role changes to avoid
                        // spurious selectedTab resets caused by Firestore/auth re-emissions.
                        self.refreshNavigation(preferredRoute: self.uiState.selectedTab)
                    } else {
                        // Same user/role — just sync the user object without touching the tab.
                        self.uiState.currentUser = newUser
                    }
                }
            } catch {
                print("Error: \(error)")
            }
        }
    }

    private func restoreSession() {
        Task {
            do {
                _ = try await restoreSessionUseCase.invoke()
                refreshNavigation(preferredRoute: uiState.selectedTab)
            } catch {
                refreshNavigation(preferredRoute: uiState.selectedTab)
            }
        }
    }

    func onAction(_ action: MainAction) {
        switch action {
        case .enter(let preferredRoute):
            refreshNavigation(preferredRoute: preferredRoute)
        case .refreshNavigation(let preferredRoute):
            refreshNavigation(preferredRoute: preferredRoute)
        case .selectTab(let route):
            selectTab(route)
        }
    }

    init(
        getMainNavigationUseCase: GetMainNavigationUseCase = KoinInitializerKt.resolveGetMainNavigationUseCase(),
        observeCurrentUserUseCase: ObserveCurrentUserUseCase = KoinInitializerKt.resolveObserveCurrentUserUseCase(),
        restoreSessionUseCase: RestoreSessionUseCase = KoinInitializerKt.resolveRestoreSessionUseCase()
    ) {
        self.getMainNavigationUseCase = getMainNavigationUseCase
        self.observeCurrentUserUseCase = observeCurrentUserUseCase
        self.restoreSessionUseCase = restoreSessionUseCase

        observeSession()
        restoreSession()
    }

    deinit {
        tasks.values.forEach { $0.cancel() }
        tasks.removeAll()
    }

    private enum TaskKey {
        case observeSession
        case refreshNavigation
    }
}
