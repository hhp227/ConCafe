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

    private let getCafeEventLikeStatusUseCase: GetCafeEventLikeStatusUseCase

    private let toggleCafeEventLikeUseCase: ToggleCafeEventLikeUseCase

    private let getCafeEventParticipantCastsUseCase: GetCafeEventParticipantCastsUseCase

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
                        pageSize: Int32(Self.eventPageSize)
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
                } else if let foundEvent = selected {
                    let likeResult = try? await getCafeEventLikeStatusUseCase.invoke(cafeId: cafeId, eventId: eventId)
                    let isLiked = (likeResult as? AppResultSuccess<AnyObject>)?.data as? Bool ?? false
                    let casts = await loadParticipantCasts(foundEvent.participantCastIds as? [String] ?? [])
                    uiState.isLoading = false
                    uiState.event = foundEvent
                    uiState.likeCount = Int(foundEvent.likeCount)
                    uiState.isLikedByMe = isLiked
                    uiState.participantCasts = casts
                    uiState.errorMessage = nil
                } else {
                    uiState.isLoading = false
                    uiState.event = nil
                    uiState.errorMessage = Self.eventNotFoundMessage
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
                for try await domainEvent in asyncSequence(for: cafeEventEventPublisher.events) {
                    switch domainEvent {
                    case let updated as Shared.CafeEventEvent.Updated:
                        if updated.cafeId == cafeId && updated.event.id == eventId {
                            let casts = await loadParticipantCasts(updated.event.participantCastIds as? [String] ?? [])
                            uiState.event = updated.event
                            uiState.likeCount = Int(updated.event.likeCount)
                            uiState.participantCasts = casts
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

    private func loadParticipantCasts(_ castIds: [String]) async -> [Cast] {
        do {
            let result = try await getCafeEventParticipantCastsUseCase.invoke(castIds: castIds)
            if let success = result as? AppResultSuccess<AnyObject>,
               let casts = success.data as? [Cast] {
                return casts
            }
        } catch {
            if Task.isCancelled { return [] }
        }
        return []
    }

    private func toggleLike() {
        guard !uiState.isTogglingLike else { return }
        tasks[.toggleLike]?.cancel()
        tasks[.toggleLike] = Task {
            let prevLiked = uiState.isLikedByMe
            let prevCount = uiState.likeCount
            uiState.isLikedByMe = !prevLiked
            uiState.likeCount = !prevLiked ? prevCount + 1 : max(0, prevCount - 1)
            uiState.isTogglingLike = true
            do {
                let result = try await toggleCafeEventLikeUseCase.invoke(cafeId: cafeId, eventId: eventId)
                if let success = result as? AppResultSuccess<AnyObject>, let isLiked = success.data as? Bool {
                    uiState.isLikedByMe = isLiked
                } else {
                    uiState.isLikedByMe = prevLiked
                    uiState.likeCount = prevCount
                    if let failure = result as? AppResultFailure,
                       failure.error is AppErrorUnauthorized {
                        event.send(.navigateToSignIn)
                    }
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isLikedByMe = prevLiked
                uiState.likeCount = prevCount
            }
            uiState.isTogglingLike = false
        }
    }

    func onAction(_ action: CafeEventAction) {
        switch action {
        case .backTapped:
            event.send(.navigateBack)
        case .retry:
            loadEvent()
        case .toggleLike:
            toggleLike()
        case .goToCafe:
            event.send(.navigateToCafe)
        case .castTapped(let id):
            event.send(.navigateToCast(id: id))
        }
    }

    init(
        cafeId: String,
        eventId: String,
        getCafeEventPageUseCase: GetCafeEventPageUseCase = KoinInitializerKt.resolveGetCafeEventPageUseCase(),
        cafeEventEventPublisher: CafeEventEventPublisher = KoinInitializerKt.resolveCafeEventEventPublisher(),
        getCafeEventLikeStatusUseCase: GetCafeEventLikeStatusUseCase = KoinInitializerKt.resolveGetCafeEventLikeStatusUseCase(),
        toggleCafeEventLikeUseCase: ToggleCafeEventLikeUseCase = KoinInitializerKt.resolveToggleCafeEventLikeUseCase(),
        getCafeEventParticipantCastsUseCase: GetCafeEventParticipantCastsUseCase = KoinInitializerKt.resolveGetCafeEventParticipantCastsUseCase()
    ) {
        self.cafeId = cafeId
        self.eventId = eventId
        self.getCafeEventPageUseCase = getCafeEventPageUseCase
        self.cafeEventEventPublisher = cafeEventEventPublisher
        self.getCafeEventLikeStatusUseCase = getCafeEventLikeStatusUseCase
        self.toggleCafeEventLikeUseCase = toggleCafeEventLikeUseCase
        self.getCafeEventParticipantCastsUseCase = getCafeEventParticipantCastsUseCase

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
        case toggleLike
    }

    private static let eventPageSize = 100
    private static let eventLoadFailedMessage = "Failed to load event."
    private static let eventNotFoundMessage = "Event not found."
}
