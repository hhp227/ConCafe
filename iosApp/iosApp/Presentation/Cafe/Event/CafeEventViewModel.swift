//
//  CafeEventViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 4/23/26.
//

import Foundation
import Combine
import Shared
import KMPNativeCoroutinesAsync

@MainActor
class CafeEventViewModel: ObservableObject {
    private let cafeId: String

    private let eventId: String

    private let getCafeEventPageUseCase: GetCafeEventPageUseCase

    private let cafeEventEventPublisher: CafeEventEventPublisher

    @Published private(set) var uiState = CafeEventUiState.empty

    let event = PassthroughSubject<CafeEventEvent, Never>()

    private var tasks: [TaskKey: Task<Void, Never>] = [:]

    private func loadEvent() {
        tasks[.loadEvent]?.cancel()
        tasks[.loadEvent] = Task {
            uiState.isLoading = true
            uiState.errorMessage = nil
            do {
                var cursor: String? = nil
                var selected: CafeEventManagementItem? = nil
                var failed = false

                repeat {
                    let result = try await getCafeEventPageUseCase.invoke(
                        cafeId: cafeId,
                        query: "",
                        cursor: cursor,
                        pageSize: Self.eventPageSize
                    )
                    if let success = result as? AppResultSuccess<AnyObject>,
                       let page = success.data as? PagedResult<CafeEventManagementItem> {
                        let items = page.items as! [CafeEventManagementItem]
                        selected = items.first { $0.id == eventId }
                        cursor = page.nextCursor
                        if selected != nil || !page.hasNext {
                            break
                        }
                    } else {
                        failed = true
                        break
                    }
                } while cursor != nil

                if failed {
                    uiState.isLoading = false
                    uiState.errorMessage = Self.eventLoadFailedMessage
                } else {
                    uiState.isLoading = false
                    uiState.event = selected
                    uiState.errorMessage = selected == nil ? Self.eventNotFoundMessage : nil
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isLoading = false
                uiState.errorMessage = Self.eventLoadFailedMessage
            }
        }
    }

    private func observeCafeEventEvent() {
        tasks[.observeEvent]?.cancel()
        tasks[.observeEvent] = Task {
            do {
                for try await event in asyncSequence(for: cafeEventEventPublisher.events) {
                    switch event {
                    case let updated as Shared.CafeEventEvent.Updated:
                        if updated.cafeId == cafeId && updated.event.id == eventId {
                            uiState.event = updated.event
                            uiState.errorMessage = nil
                        }
                    case let deleted as Shared.CafeEventEvent.Deleted:
                        if deleted.cafeId == cafeId && deleted.eventId == eventId {
                            uiState.event = nil
                            uiState.errorMessage = Self.eventNotFoundMessage
                        }
                    default:
                        break
                    }
                }
            } catch {
                print("Error: \(error)")
            }
        }
    }

    func onAction(_ action: CafeEventAction) {
        switch action {
        case .backTapped:
            event.send(.navigateBack)
        case .retry:
            loadEvent()
        }
    }

    init(
        cafeId: String,
        eventId: String,
        getCafeEventPageUseCase: GetCafeEventPageUseCase = KoinInitializerKt.resolveGetCafeEventPageUseCase(),
        cafeEventEventPublisher: CafeEventEventPublisher = KoinInitializerKt.resolveCafeEventEventPublisher()
    ) {
        self.cafeId = cafeId
        self.eventId = eventId
        self.getCafeEventPageUseCase = getCafeEventPageUseCase
        self.cafeEventEventPublisher = cafeEventEventPublisher

        observeCafeEventEvent()
        loadEvent()
    }

    deinit {
        tasks.values.forEach { $0.cancel() }
        tasks.removeAll()
    }

    private enum TaskKey {
        case loadEvent
        case observeEvent
    }

    private static let eventPageSize = 100
    private static let eventLoadFailedMessage = "Failed to load event."
    private static let eventNotFoundMessage = "Event not found."
}
