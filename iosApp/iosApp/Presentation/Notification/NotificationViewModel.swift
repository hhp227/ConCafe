//
//  NotificationViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/06.
//

import Foundation
import Combine
import Shared
import KMPNativeCoroutinesAsync

@MainActor
final class NotificationViewModel: ObservableObject {
    private let getNotificationFeedUseCase: GetNotificationFeedUseCase

    private let markNotificationReadUseCase: MarkNotificationReadUseCase

    private let observeCurrentUserUseCase: ObserveCurrentUserUseCase

    @Published private(set) var uiState = NotificationUiState.empty

    let event = PassthroughSubject<NotificationEvent, Never>()

    private func observeSession() {
        Task {
            do {
                for try await _ in asyncSequence(for: observeCurrentUserUseCase.invoke()) {
                    self.loadNotifications()
                }
            } catch {
                print("Error: \(error)")
            }
        }
    }

    private func loadNotifications() {
        uiState.isLoading = true
        uiState.errorMessage = nil

        Task {
            do {
                let result = try await getNotificationFeedUseCase.invoke(pageSize: 20)

                if let success = result as? AppResultSuccess<AnyObject>,
                   let feed = success.data as? NotificationFeed {
                    uiState = NotificationUiState(
                        isLoading: false,
                        errorMessage: nil,
                        isLoggedIn: feed.isLoggedIn,
                        unreadCount: feed.unreadCount,
                        sections: feed.sections
                    )
                } else {
                    uiState.isLoading = false
                    uiState.errorMessage = "알림을 불러오지 못했습니다."
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isLoading = false
                uiState.errorMessage = "알림을 불러오지 못했습니다."
            }
        }
    }

    private func handleNotificationTap(id: String, type: String, targetId: String?) {
        Task {
            do {
                let result = try await markNotificationReadUseCase.invoke(notificationId: id)

                if result is AppResultFailure {
                    uiState.errorMessage = "알림 상태를 업데이트하지 못했습니다."
                } else {
                    var updatedSections: [NotificationSection] = []
                    var unreadCount: Int32 = 0

                    for section in uiState.sections {
                        var items: [NotificationListItem] = []

                        for item in section.items {
                            if item.id == id {
                                let updatedItem = NotificationListItem(
                                    id: item.id,
                                    title: item.title,
                                    message: item.message,
                                    type: item.type,
                                    targetId: item.targetId,
                                    isRead: true,
                                    relativeTime: item.relativeTime
                                )
                                items.append(updatedItem)
                            } else {
                                items.append(item)
                            }
                        }
                        unreadCount += Int32(items.filter { !$0.isRead }.count)
                        updatedSections.append(
                            NotificationSection(
                                id: section.id,
                                title: section.title,
                                items: items
                            )
                        )
                    }
                    uiState.sections = updatedSections
                    uiState.unreadCount = unreadCount

                    if let targetId {
                        if type == "CAST_SHIFT" || type == "BIRTHDAY" {
                            event.send(.navigateToCast(id: targetId))
                        } else if type == "CAFE_NOTICE" || type == "CAFE_EVENT" || type == "CAFE_TABLE_COUNT_UPDATE" {
                            event.send(.navigateToCafe(id: targetId))
                        }
                    }
                }
            } catch {
                if Task.isCancelled { return }
                uiState.errorMessage = "알림 상태를 업데이트하지 못했습니다."
            }
        }
    }

    func onAction(_ action: NotificationAction) {
        switch action {
        case .backTapped:
            event.send(.navigateBack)
        case .notificationTapped(let id, let type, let targetId):
            handleNotificationTap(id: id, type: type, targetId: targetId)
        case .signInTapped:
            event.send(.navigateToSignIn)
        case .refresh:
            loadNotifications()
        }
    }

    init(
        getNotificationFeedUseCase: GetNotificationFeedUseCase = KoinInitializerKt.resolveGetNotificationFeedUseCase(),
        markNotificationReadUseCase: MarkNotificationReadUseCase = KoinInitializerKt.resolveMarkNotificationReadUseCase(),
        observeCurrentUserUseCase: ObserveCurrentUserUseCase = KoinInitializerKt.resolveObserveCurrentUserUseCase()
    ) {
        self.getNotificationFeedUseCase = getNotificationFeedUseCase
        self.markNotificationReadUseCase = markNotificationReadUseCase
        self.observeCurrentUserUseCase = observeCurrentUserUseCase

        observeSession()
        loadNotifications()
    }
}
