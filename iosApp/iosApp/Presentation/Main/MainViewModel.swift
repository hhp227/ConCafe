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

    @Published private(set) var uiState = MainUiState.empty

    let event = PassthroughSubject<MainEvent, Never>()
    
    private var sessionTask: Task<Void, Never>?

    private func refreshNavigation(preferredRoute: String?) {
        Task {
            do {
                let result = try await getMainNavigationUseCase.invoke(preferredRoute: preferredRoute)

                if let success = result as? AppResultSuccess<AnyObject>,
                   let state = success.data as? MainNavigationState {
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
                event.send(.showError(message: error.localizedDescription))
            }
        }
    }

    private func selectTab(_ route: String) {
        guard uiState.tabs.contains(where: { $0.route == route }) else { return }
        uiState.selectedTab = route
    }
    
    private func observeSession() {
        sessionTask = Task {
            do {
                for try await _ in asyncSequence(for: observeCurrentUserUseCase.invoke()) {
                    self.refreshNavigation(preferredRoute: self.uiState.selectedTab)
                }
            } catch {
                print("Error: \(error)")
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
        observeCurrentUserUseCase: ObserveCurrentUserUseCase = KoinInitializerKt.resolveObserveCurrentUserUseCase()
    ) {
        self.getMainNavigationUseCase = getMainNavigationUseCase
        self.observeCurrentUserUseCase = observeCurrentUserUseCase

        observeSession()
        onAction(.enter())
    }
    
    deinit {
        sessionTask?.cancel()
    }
}
