//
//  CommunityViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 4/27/26.
//

import Foundation
import Combine
import Shared
import KMPNativeCoroutinesAsync

@MainActor
final class CommunityViewModel: ObservableObject {
    private let getCommunityPostPageUseCase: GetCommunityPostPageUseCase

    private let communityPostEventPublisher: CommunityPostEventPublisher

    @Published private(set) var uiState = CommunityUiState()

    let event = PassthroughSubject<CommunityEvent, Never>()

    private var cancellables = Set<AnyCancellable>()

    private func refresh() {
        uiState.isLoading = true
        uiState.posts = []
        uiState.nextCursor = nil
        uiState.hasNext = false
        uiState.errorMessage = nil

        Task {
            do {
                let result = try await getCommunityPostPageUseCase.invoke(cursor: nil, pageSize: 20)
                if let success = result as? AppResultSuccess<AnyObject>,
                   let paged = success.data as? PagedResult<CommunityPost> {
                    uiState.isLoading = false
                    uiState.posts = paged.items as? [CommunityPost] ?? []
                    uiState.nextCursor = paged.nextCursor
                    uiState.hasNext = paged.hasNext
                } else {
                    uiState.isLoading = false
                    uiState.errorMessage = "게시글을 불러오지 못했습니다."
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isLoading = false
                uiState.errorMessage = "게시글을 불러오지 못했습니다."
            }
        }
    }

    private func loadMore() {
        guard uiState.hasNext, !uiState.isLoadingMore else { return }
        let cursor = uiState.nextCursor
        uiState.isLoadingMore = true

        Task {
            do {
                let result = try await getCommunityPostPageUseCase.invoke(cursor: cursor, pageSize: 20)
                if let success = result as? AppResultSuccess<AnyObject>,
                   let paged = success.data as? PagedResult<CommunityPost> {
                    uiState.isLoadingMore = false
                    uiState.posts += paged.items as? [CommunityPost] ?? []
                    uiState.nextCursor = paged.nextCursor
                    uiState.hasNext = paged.hasNext
                } else {
                    uiState.isLoadingMore = false
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isLoadingMore = false
            }
        }
    }

    private func observeCommunityPostEvents() {
        Task {
            do {
                for try await _ in asyncSequence(for: communityPostEventPublisher.events) {
                    refresh()
                }
            } catch {
                print("Error: \(error)")
            }
        }
    }

    func onAction(_ action: CommunityAction) {
        switch action {
        case .refresh:
            refresh()
        case .loadMore:
            loadMore()
        case .clickWritePost:
            event.send(.navigateToPostEdit)
        case .clickPost(let postId):
            event.send(.navigateToPost(postId: postId))
        case .dismissError:
            uiState.errorMessage = nil
        }
    }

    init(
        getCommunityPostPageUseCase: GetCommunityPostPageUseCase = KoinInitializerKt.resolveGetCommunityPostPageUseCase(),
        communityPostEventPublisher: CommunityPostEventPublisher = KoinInitializerKt.resolveCommunityPostEventPublisher()
    ) {
        self.getCommunityPostPageUseCase = getCommunityPostPageUseCase
        self.communityPostEventPublisher = communityPostEventPublisher
        observeCommunityPostEvents()
        refresh()
    }
}
