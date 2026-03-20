//
//  ContentViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/20.
//

import Foundation
import Shared
import KMPNativeCoroutinesAsync

@MainActor
final class ContentViewModel: ObservableObject {
    private let observeNetworkAlertStateUseCase: ObserveNetworkAlertStateUseCase

    @Published private(set) var uiState = ContentUiState()

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

    init(
        observeNetworkAlertStateUseCase: ObserveNetworkAlertStateUseCase = KoinInitializerKt.resolveObserveNetworkAlertStateUseCase()
    ) {
        self.observeNetworkAlertStateUseCase = observeNetworkAlertStateUseCase

        observeNetworkAlertState()
    }

    deinit {
        tasks.values.forEach { $0.cancel() }
        tasks.removeAll()
    }

    private enum TaskKey {
        case observeNetworkAlert
    }
}
