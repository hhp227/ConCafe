//
//  ContentViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/20.
//

import Foundation
import Combine
import Shared
import KMPNativeCoroutinesAsync

@MainActor
final class ContentViewModel: ObservableObject {
    private let observeNetworkAlertStateUseCase: ObserveNetworkAlertStateUseCase

    private let observeCurrentUserUseCase: ObserveCurrentUserUseCase

    private let registerPushTokenUseCase: RegisterPushTokenUseCase

    @Published private(set) var uiState = ContentUiState()

    let event = PassthroughSubject<ContentEvent, Never>()

    private var tasks: [TaskKey: Task<Void, Never>] = [:]

    private func observeNetworkAlertState() {
        tasks[.observeNetworkAlert]?.cancel()
        tasks[.observeNetworkAlert] = Task {
            do {
                for try await networkAlertState in asyncSequence(for: observeNetworkAlertStateUseCase.invoke()) {
                    await MainActor.run {
                        uiState.networkAlertState = networkAlertState
                    }
                }
            } catch {
                await MainActor.run {
                    uiState.networkAlertState = nil
                }
            }
        }
    }

    private func observeSessionAndSyncPushToken() {
        tasks[.observeCurrentUser]?.cancel()
        tasks[.observeCurrentUser] = Task {
            do {
                for try await _ in asyncSequence(for: observeCurrentUserUseCase.invoke()) {
                    event.send(.syncPushToken)
                }
            } catch {
            }
        }
    }

    private func syncPushToken(_ token: String) {
        let normalizedToken = token.trimmingCharacters(in: .whitespacesAndNewlines)

        if normalizedToken.isEmpty {
            return
        } else {
            Task {
                _ = try? await registerPushTokenUseCase.invoke(platform: "IOS", token: normalizedToken)
            }
        }
    }

    func onAction(_ action: ContentAction) {
        switch action {
        case .syncPushToken(let token):
            syncPushToken(token)
        }
    }

    init(
        observeNetworkAlertStateUseCase: ObserveNetworkAlertStateUseCase = KoinInitializerKt.resolveObserveNetworkAlertStateUseCase(),
        observeCurrentUserUseCase: ObserveCurrentUserUseCase = KoinInitializerKt.resolveObserveCurrentUserUseCase(),
        registerPushTokenUseCase: RegisterPushTokenUseCase = KoinInitializerKt.resolveRegisterPushTokenUseCase()
    ) {
        self.observeNetworkAlertStateUseCase = observeNetworkAlertStateUseCase
        self.observeCurrentUserUseCase = observeCurrentUserUseCase
        self.registerPushTokenUseCase = registerPushTokenUseCase

        observeNetworkAlertState()
        observeSessionAndSyncPushToken()
    }

    deinit {
        tasks.values.forEach { $0.cancel() }
        tasks.removeAll()
    }

    private enum TaskKey {
        case observeNetworkAlert
        case observeCurrentUser
    }
}
