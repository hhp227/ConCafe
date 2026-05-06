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

    private let themePreferences: AppThemePreferences

    @Published private(set) var uiState = SettingsUiState.empty

    let event = PassthroughSubject<SettingsEvent, Never>()

    private var tasks: [TaskKey: Task<Void, Never>] = [:]

    private func clickAccountSettings() {
        event.send(.navigateToAccountSettings)
    }

    private func clickNotificationSettings() {
        event.send(.navigateToNotificationSettings)
    }

    private func clickInquiry() {
        event.send(.navigateToInquiry)
    }

    private func clickPrivacyPolicy() {
        event.send(
            .navigateToExternalLink(
                title: Self.privacyPolicyTitle,
                url: Self.privacyPolicyUrl
            )
        )
    }

    private func signOut() {
        uiState.isLoading = true
        uiState.errorMessage = nil

        tasks[.signOut]?.cancel()
        tasks[.signOut] = Task {
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

    private func selectThemeMode(_ themeMode: AppThemeMode) {
        themePreferences.setThemeMode(themeMode)
        uiState.themeMode = themeMode
    }

    private func observeThemeMode() {
        tasks[.observeTheme]?.cancel()
        tasks[.observeTheme] = Task {
            for await themeMode in themePreferences.observeThemeMode() {
                uiState.themeMode = themeMode
            }
        }
    }

    func onAction(_ action: SettingsAction) {
        switch action {
        case .backTapped:
            event.send(.navigateBack)
        case .accountSettingsTapped:
            clickAccountSettings()
        case .notificationSettingsTapped:
            clickNotificationSettings()
        case .customerSupportTapped:
            clickInquiry()
        case .inquiryTapped:
            clickInquiry()
        case .privacyPolicyTapped:
            clickPrivacyPolicy()
        case .signOutTapped:
            signOut()
        case .themeModeSelected(let themeMode):
            selectThemeMode(themeMode)
        }
    }

    init(
        signOutUseCase: SignOutUseCase = KoinInitializerKt.resolveSignOutUseCase(),
        themePreferences: AppThemePreferences = .shared
    ) {
        self.signOutUseCase = signOutUseCase
        self.themePreferences = themePreferences

        observeThemeMode()
    }

    deinit {
        tasks.values.forEach { $0.cancel() }
        tasks.removeAll()
    }

    private enum TaskKey {
        case signOut
        case observeTheme
    }

    private static let privacyPolicyTitle = "개인정보 처리방침"
    private static let privacyPolicyUrl = "https://concafe-5f7fd.firebaseapp.com/privacy"
}
