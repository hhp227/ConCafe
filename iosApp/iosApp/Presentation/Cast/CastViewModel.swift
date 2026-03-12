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

    @Published private(set) var uiState = CastUiState.empty

    let event = PassthroughSubject<CastEvent, Never>()

    private var loadTask: Task<Void, Never>?

    private func loadCastDetail() {
        loadTask?.cancel()
        uiState.isLoading = true
        uiState.errorMessage = nil

        loadTask = Task {
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
        loadTask?.cancel()
        loadTask = Task {
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
        toggleFollowCastUseCase: ToggleFollowCastUseCase = KoinInitializerKt.resolveToggleFollowCastUseCase()
    ) {
        self.castId = castId
        self.getCastDetailUseCase = getCastDetailUseCase
        self.toggleFollowCastUseCase = toggleFollowCastUseCase

        loadCastDetail()
    }

    deinit {
        loadTask?.cancel()
    }
}
