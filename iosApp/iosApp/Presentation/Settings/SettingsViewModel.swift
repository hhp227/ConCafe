//
//  SettingsViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import Foundation
import Combine
import Shared
import KMPNativeCoroutinesAsync

@MainActor
final class SettingsViewModel: ObservableObject {
    private let signOutUseCase: SignOutUseCase

    private let observeThemeModeUseCase: ObserveThemeModeUseCase

    private let setThemeModeUseCase: SetThemeModeUseCase

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
        setThemeModeUseCase.invoke(themeMode: themeMode.sharedThemeMode)
        uiState.themeMode = themeMode
    }

    private func observeThemeMode() {
        tasks[.observeTheme]?.cancel()
        tasks[.observeTheme] = Task {
            do {
                for try await themeMode in asyncSequence(for: observeThemeModeUseCase.invoke()) {
                    uiState.themeMode = AppThemeMode(themeMode: themeMode)
                }
            } catch {
                uiState.themeMode = .light
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
        observeThemeModeUseCase: ObserveThemeModeUseCase = KoinInitializerKt.resolveObserveThemeModeUseCase(),
        setThemeModeUseCase: SetThemeModeUseCase = KoinInitializerKt.resolveSetThemeModeUseCase()
    ) {
        self.signOutUseCase = signOutUseCase
        self.observeThemeModeUseCase = observeThemeModeUseCase
        self.setThemeModeUseCase = setThemeModeUseCase

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
