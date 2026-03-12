//
//  SettingsViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import Foundation
import Combine
import Shared

@MainActor
final class SettingsViewModel: ObservableObject {
    private let signOutUseCase: SignOutUseCase

    @Published private(set) var uiState = SettingsUiState.empty

    let event = PassthroughSubject<SettingsEvent, Never>()

    private var signOutTask: Task<Void, Never>?

    private func signOut() {
        uiState.isLoading = true
        uiState.errorMessage = nil

        signOutTask?.cancel()
        signOutTask = Task {
            do {
                let result = try await signOutUseCase.invoke()

                if result is AppResultFailure {
                    uiState.isLoading = false
                    uiState.errorMessage = "로그아웃에 실패했습니다."
                } else {
                    uiState.isLoading = false
                    event.send(.navigateBack)
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isLoading = false
                uiState.errorMessage = "로그아웃에 실패했습니다."
            }
        }
    }

    func onAction(_ action: SettingsAction) {
        switch action {
        case .backTapped:
            event.send(.navigateBack)
        case .signOutTapped:
            signOut()
        }
    }

    init(
        signOutUseCase: SignOutUseCase = KoinInitializerKt.resolveSignOutUseCase()
    ) {
        self.signOutUseCase = signOutUseCase
    }

    deinit {
        signOutTask?.cancel()
    }
}
