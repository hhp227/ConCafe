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
    private let checkAppUpdateUseCase: CheckAppUpdateUseCase

    private let observeNetworkAlertStateUseCase: ObserveNetworkAlertStateUseCase

    private let observeCurrentUserUseCase: ObserveCurrentUserUseCase

    private let registerPushTokenUseCase: RegisterPushTokenUseCase

    private let getNotificationFeedUseCase: GetNotificationFeedUseCase

    private let observeThemeModeUseCase: ObserveThemeModeUseCase

    private let observeBrandThemeUseCase: ObserveBrandThemeUseCase

    @Published private(set) var uiState = ContentUiState()

    let event = PassthroughSubject<ContentEvent, Never>()

    private var tasks: [TaskKey: Task<Void, Never>] = [:]

    private func observeNetworkAlertState() {
        tasks[.observeNetworkAlert]?.cancel()
        tasks[.observeNetworkAlert] = Task {
            do {
                for try await networkAlertState in asyncSequence(for: observeNetworkAlertStateUseCase.invoke()) {
                    uiState.networkAlertState = networkAlertState
                }
            } catch {
                uiState.networkAlertState = nil
            }
        }
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

    private func observeBrandTheme() {
        tasks[.observeBrandTheme]?.cancel()
        tasks[.observeBrandTheme] = Task {
            do {
                for try await brandTheme in asyncSequence(for: observeBrandThemeUseCase.invoke()) {
                    let presentationBrandTheme = AppBrandTheme(brandTheme: brandTheme)
                    ConCafeColors.brandTheme = presentationBrandTheme
                    uiState.brandTheme = presentationBrandTheme
                }
            } catch {
                ConCafeColors.brandTheme = .maidCafe
                uiState.brandTheme = .maidCafe
            }
        }
    }

    private func observeSessionAndSyncPushToken() {
        tasks[.observeCurrentUser]?.cancel()
        tasks[.observeCurrentUser] = Task {
            do {
                for try await user in asyncSequence(for: observeCurrentUserUseCase.invoke()) {
                    if user != nil {
                        event.send(.syncPushToken)
                        startUnreadNotificationPolling()
                    } else {
                        stopUnreadNotificationPolling()
                        uiState.hasUnreadNotifications = false
                    }
                }
            } catch {}
        }
    }

    private func refreshUnreadNotificationCount() async {
        do {
            let result = try await getNotificationFeedUseCase.invoke(pageSize: 20)

            if let success = result as? AppResultSuccess<AnyObject>,
               let feed = success.data as? NotificationFeed {
                let hasUnread = feed.unreadCount > 0

                if uiState.hasUnreadNotifications != hasUnread {
                    uiState.hasUnreadNotifications = hasUnread
                }
            }
        } catch {}
    }

    private func startUnreadNotificationPolling() {
        tasks[.unreadNotificationPoll]?.cancel()
        tasks[.unreadNotificationPoll] = Task { [weak self] in
            while !Task.isCancelled {
                await self?.refreshUnreadNotificationCount()
                do {
                    try await Task.sleep(nanoseconds: unreadNotificationPollIntervalNanoseconds)
                } catch {
                    break
                }
            }
        }
    }

    private func stopUnreadNotificationPolling() {
        tasks[.unreadNotificationPoll]?.cancel()
        tasks.removeValue(forKey: .unreadNotificationPoll)
    }

    private func checkAppUpdate(storePlatform: String, storeId: String, currentVersion: String) {
        tasks[.checkAppUpdate]?.cancel()
        tasks[.checkAppUpdate] = Task {
            do {
                let result = try await checkAppUpdateUseCase.invoke(
                    storePlatform: storePlatform,
                    storeId: storeId,
                    currentVersion: currentVersion
                )

                if let success = result as? AppResultSuccess<AnyObject>,
                   let updateInfo = success.data as? AppUpdateInfo {
                    event.send(.showAppUpdate(updateInfo))
                }
            } catch {}
        }
    }

    private func syncPushToken(_ token: String) {
        let normalizedToken = token.trimmingCharacters(in: .whitespacesAndNewlines)
        if normalizedToken.isEmpty { return }
        Task {
            _ = try? await registerPushTokenUseCase.invoke(platform: "IOS", token: normalizedToken)
        }
    }

    func onAction(_ action: ContentAction) {
        switch action {
        case .checkAppUpdate(let storePlatform, let storeId, let currentVersion):
            checkAppUpdate(storePlatform: storePlatform, storeId: storeId, currentVersion: currentVersion)
        case .syncPushToken(let token):
            syncPushToken(token)
        case .refreshUnreadNotificationCount:
            Task { await refreshUnreadNotificationCount() }
        }
    }

    init(
        checkAppUpdateUseCase: CheckAppUpdateUseCase = KoinInitializerKt.resolveCheckAppUpdateUseCase(),
        observeNetworkAlertStateUseCase: ObserveNetworkAlertStateUseCase = KoinInitializerKt.resolveObserveNetworkAlertStateUseCase(),
        observeCurrentUserUseCase: ObserveCurrentUserUseCase = KoinInitializerKt.resolveObserveCurrentUserUseCase(),
        registerPushTokenUseCase: RegisterPushTokenUseCase = KoinInitializerKt.resolveRegisterPushTokenUseCase(),
        getNotificationFeedUseCase: GetNotificationFeedUseCase = KoinInitializerKt.resolveGetNotificationFeedUseCase(),
        observeThemeModeUseCase: ObserveThemeModeUseCase = KoinInitializerKt.resolveObserveThemeModeUseCase(),
        observeBrandThemeUseCase: ObserveBrandThemeUseCase = KoinInitializerKt.resolveObserveBrandThemeUseCase()
    ) {
        self.checkAppUpdateUseCase = checkAppUpdateUseCase
        self.observeNetworkAlertStateUseCase = observeNetworkAlertStateUseCase
        self.observeCurrentUserUseCase = observeCurrentUserUseCase
        self.registerPushTokenUseCase = registerPushTokenUseCase
        self.getNotificationFeedUseCase = getNotificationFeedUseCase
        self.observeThemeModeUseCase = observeThemeModeUseCase
        self.observeBrandThemeUseCase = observeBrandThemeUseCase

        observeNetworkAlertState()
        observeThemeMode()
        observeBrandTheme()
        observeSessionAndSyncPushToken()
    }

    deinit {
        tasks.values.forEach { $0.cancel() }
        tasks.removeAll()
    }

    private enum TaskKey {
        case checkAppUpdate
        case observeNetworkAlert
        case observeTheme
        case observeBrandTheme
        case observeCurrentUser
        case unreadNotificationPoll
    }
}

private let unreadNotificationPollIntervalNanoseconds: UInt64 = 60_000_000_000
