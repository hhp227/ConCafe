//
//  CastViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/06.
//

import Foundation
import Combine
import Shared
import KMPNativeCoroutinesAsync

@MainActor
final class CastViewModel: ObservableObject {
    private let castId: String

    private let getCastDetailUseCase: GetCastDetailUseCase

    private let toggleFollowCastUseCase: ToggleFollowCastUseCase

    private let castEventPublisher: CastEventPublisher

    private let reviewEventPublisher: ReviewEventPublisher

    @Published private(set) var uiState = CastUiState.empty

    let event = PassthroughSubject<CastEvent, Never>()

    private var tasks: [TaskKey: Task<Void, Never>] = [:]

    private func observeCastEvent() {
        tasks[.castEvent]?.cancel()
        tasks[.castEvent] = Task {
            do {
                for try await event in asyncSequence(for: castEventPublisher.events) {
                    switch event {
                    case let event as Shared.CastEvent.Created:
                        if event.cast.id == self.castId {
                            self.loadCastDetail()
                        }
                    case let event as Shared.CastEvent.Updated:
                        if event.cast.id == self.castId, let detail = self.uiState.detail {
                            self.uiState.detail = CastDetail(
                                cast: event.cast,
                                cafe: detail.cafe,
                                images: detail.images,
                                schedule: detail.schedule,
                                visitCertificationCount: detail.visitCertificationCount
                            )
                        }
                    case let event as Shared.CastEvent.Deleted:
                        if event.castId == self.castId {
                            self.event.send(.navigateBack)
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

    private func observeReviewEvent() {
        tasks[.reviewEvent]?.cancel()
        tasks[.reviewEvent] = Task {
            do {
                for try await event in asyncSequence(for: reviewEventPublisher.events) {
                    guard let currentCafeId = self.uiState.detail?.cafe.id else { return }
                    if let created = event as? ReviewEvent.Created, created.cafeId == currentCafeId {
                        self.loadCastDetail()
                    } else if let deleted = event as? ReviewEvent.Deleted, deleted.cafeId == currentCafeId {
                        self.loadCastDetail()
                    }
                }
            } catch {
                print("Error: \(error)")
            }
        }
    }

    private func loadCastDetail() {
        uiState.isLoading = true
        uiState.errorMessage = nil

        Task {
            do {
                let result = try await getCastDetailUseCase.invoke(castId: castId)

                if let success = result as? AppResultSuccess<AnyObject>,
                   let feed = success.data as? Shared.CastDetailFeed {
                    uiState = CastUiState(
                        isLoading: false,
                        errorMessage: nil,
                        detail: feed.detail,
                        recentReviews: feed.recentReviews,
                        isFollowing: feed.isFollowing,
                        isLoggedIn: feed.isLoggedIn,
                        todayAttendanceStatus: feed.todayAttendanceStatus,
                        isSelfCast: feed.isSelfCast
                    )
                } else {
                    uiState.isLoading = false
                    uiState.errorMessage = nil
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isLoading = false
                uiState.errorMessage = nil
            }
        }
    }

    private func toggleFollow() {
        Task {
            do {
                let result = try await toggleFollowCastUseCase.invoke(castId: castId)

                if let success = result as? AppResultSuccess<AnyObject>,
                   let isFollowing = success.data as? NSNumber {
                    uiState.isFollowing = isFollowing.boolValue
                    uiState.isLoggedIn = true
                } else if let failure = result as? AppResultFailure {
                    if failure.error is AppErrorUnauthorized {
                        event.send(.navigateToSignIn)
                    }
                }
            } catch {
                if Task.isCancelled { return }
            }
        }
    }

    func onAction(_ action: CastAction) {
        switch action {
        case .backTapped:
            event.send(.navigateBack)
        case .followTapped:
            toggleFollow()
        case .refresh:
            loadCastDetail()
        case .cafeTapped:
            guard let cafeId = uiState.detail?.cafe.id else { return }
            event.send(.navigateToCafe(id: cafeId))
        }
    }

    init(
        castId: String,
        getCastDetailUseCase: GetCastDetailUseCase = KoinInitializerKt.resolveGetCastDetailUseCase(),
        toggleFollowCastUseCase: ToggleFollowCastUseCase = KoinInitializerKt.resolveToggleFollowCastUseCase(),
        castEventPublisher: CastEventPublisher = KoinInitializerKt.resolveCastEventPublisher(),
        reviewEventPublisher: ReviewEventPublisher = KoinInitializerKt.resolveReviewEventPublisher()
    ) {
        self.castId = castId
        self.getCastDetailUseCase = getCastDetailUseCase
        self.toggleFollowCastUseCase = toggleFollowCastUseCase
        self.castEventPublisher = castEventPublisher
        self.reviewEventPublisher = reviewEventPublisher

        observeCastEvent()
        observeReviewEvent()
        loadCastDetail()
    }

    deinit {
        tasks.values.forEach { $0.cancel() }
        tasks.removeAll()
    }

    private enum TaskKey {
        case castEvent
        case reviewEvent
    }
}
