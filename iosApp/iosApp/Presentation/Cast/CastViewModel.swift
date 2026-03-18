//
//  CastViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/06.
//

import Foundation
import Combine
import Shared

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
            
        }
        /*tasks[.castEvent] = observeCastEventUseCase.watch { [weak self] event in
            guard let self else { return }
            Task { @MainActor in
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
                            schedule: detail.schedule
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
        }*/
    }

    private func observeReviewEvent() {
        tasks[.reviewEvent]?.cancel()
        tasks[.reviewEvent] = Task {
            
        }
        /*tasks[.reviewEvent] = observeReviewEventUseCase.watch { [weak self] event in
            guard let self else { return }
            guard let currentCafeId = self.uiState.detail?.cafe.id else { return }
            if let created = event as? ReviewEvent.Created, created.cafeId == currentCafeId {
                self.loadCastDetail()
            } else if let deleted = event as? ReviewEvent.Deleted, deleted.cafeId == currentCafeId {
                self.loadCastDetail()
            }
        }*/
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
                        isLoggedIn: feed.isLoggedIn
                    )
                } else {
                    uiState.isLoading = false
                    uiState.errorMessage = "캐스트 상세 데이터를 불러오지 못했습니다."
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isLoading = false
                uiState.errorMessage = "캐스트 상세 데이터를 불러오지 못했습니다."
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
