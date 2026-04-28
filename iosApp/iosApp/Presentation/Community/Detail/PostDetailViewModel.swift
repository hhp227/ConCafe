//
//  PostDetailViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 4/28/26.
//

import Foundation
import Combine
import Shared
import KMPNativeCoroutinesAsync

@MainActor
final class PostDetailViewModel: ObservableObject {
    private let postId: String
    private let getCommunityPostUseCase: GetCommunityPostUseCase
    private let checkCommunityPostLikedUseCase: CheckCommunityPostLikedUseCase
    private let deleteCommunityPostUseCase: DeleteCommunityPostUseCase
    private let toggleCommunityPostLikeUseCase: ToggleCommunityPostLikeUseCase
    private let getCommunityCommentsUseCase: GetCommunityCommentsUseCase
    private let addCommunityCommentUseCase: AddCommunityCommentUseCase
    private let observeCurrentUserUseCase: ObserveCurrentUserUseCase

    @Published private(set) var uiState = PostDetailUiState()

    let event = PassthroughSubject<PostDetailEvent, Never>()

    private var tasks: [TaskKey: Task<Void, Never>] = [:]

    private func loadPost() {
        tasks[.loadPost]?.cancel()
        tasks[.loadPost] = Task {
            uiState.isLoading = true
            uiState.errorMessage = nil
            do {
                let result = try await getCommunityPostUseCase.invoke(postId: postId)
                if let success = result as? AppResultSuccess<AnyObject>,
                   let post = success.data as? CommunityPost {
                    uiState.post = post
                    uiState.isLoading = false
                    checkIsOwner(post: post)
                    checkLikeStatus()
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

    private func checkIsOwner(post: CommunityPost) {
        tasks[.checkOwner]?.cancel()
        tasks[.checkOwner] = Task {
            do {
                for try await user in asyncSequence(for: observeCurrentUserUseCase.invoke()) {
                    uiState.isOwner = user?.id == post.userId
                    break
                }
            } catch { }
        }
    }

    private func checkLikeStatus() {
        tasks[.checkLike]?.cancel()
        tasks[.checkLike] = Task {
            do {
                let result = try await checkCommunityPostLikedUseCase.invoke(postId: postId)
                if let success = result as? AppResultSuccess<AnyObject>,
                   let isLiked = success.data as? Bool {
                    uiState.isLiked = isLiked
                }
            } catch { }
        }
    }

    private func loadComments() {
        tasks[.loadComments]?.cancel()
        tasks[.loadComments] = Task {
            uiState.isLoadingComments = true
            do {
                let result = try await getCommunityCommentsUseCase.invoke(postId: postId)
                if let success = result as? AppResultSuccess<AnyObject>,
                   let comments = success.data as? [Comment] {
                    uiState.comments = comments
                }
            } catch { }
            uiState.isLoadingComments = false
        }
    }

    private func toggleLike() {
        let wasLiked = uiState.isLiked
        let currentCount = uiState.likeCount
        uiState.isLiked = !wasLiked
        if let post = uiState.post {
            let newCount = wasLiked ? max(0, Int(post.likeCount) - 1) : Int(post.likeCount) + 1
            uiState.post = CommunityPost(
                id: post.id,
                userId: post.userId,
                userNickname: post.userNickname,
                title: post.title,
                content: post.content,
                imageUrls: post.imageUrls,
                likeCount: Int32(newCount),
                commentCount: post.commentCount,
                createdAt: post.createdAt,
                displayDate: post.displayDate
            )
        }
        tasks[.toggleLike]?.cancel()
        tasks[.toggleLike] = Task {
            do {
                let result = try await toggleCommunityPostLikeUseCase.invoke(postId: postId)
                if result is AppResultFailure {
                    uiState.isLiked = wasLiked
                    if let post = uiState.post {
                        uiState.post = CommunityPost(
                            id: post.id,
                            userId: post.userId,
                            userNickname: post.userNickname,
                            title: post.title,
                            content: post.content,
                            imageUrls: post.imageUrls,
                            likeCount: Int32(currentCount),
                            commentCount: post.commentCount,
                            createdAt: post.createdAt,
                            displayDate: post.displayDate
                        )
                    }
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isLiked = wasLiked
            }
        }
    }

    private func deletePost() {
        uiState.isDeleting = true
        uiState.isDeleteConfirmVisible = false
        tasks[.deletePost]?.cancel()
        tasks[.deletePost] = Task {
            do {
                let result = try await deleteCommunityPostUseCase.invoke(postId: postId)
                if result is AppResultSuccess<AnyObject> {
                    event.send(.navigateBack)
                } else {
                    uiState.isDeleting = false
                    uiState.errorMessage = "게시글을 삭제하지 못했습니다."
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isDeleting = false
                uiState.errorMessage = "게시글을 삭제하지 못했습니다."
            }
        }
    }

    private func sendComment() {
        let text = uiState.commentText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty else { return }
        uiState.isSendingComment = true
        tasks[.sendComment]?.cancel()
        tasks[.sendComment] = Task {
            do {
                let result = try await addCommunityCommentUseCase.invoke(postId: postId, content: text)
                if let success = result as? AppResultSuccess<AnyObject>,
                   let comment = success.data as? Comment {
                    uiState.isSendingComment = false
                    uiState.commentText = ""
                    uiState.comments.append(comment)
                    if let post = uiState.post {
                        uiState.post = CommunityPost(
                            id: post.id,
                            userId: post.userId,
                            userNickname: post.userNickname,
                            title: post.title,
                            content: post.content,
                            imageUrls: post.imageUrls,
                            likeCount: post.likeCount,
                            commentCount: post.commentCount + 1,
                            createdAt: post.createdAt,
                            displayDate: post.displayDate
                        )
                    }
                } else {
                    uiState.isSendingComment = false
                    uiState.errorMessage = "댓글을 등록하지 못했습니다."
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isSendingComment = false
                uiState.errorMessage = "댓글을 등록하지 못했습니다."
            }
        }
    }

    func onAction(_ action: PostDetailAction) {
        switch action {
        case .clickBack:
            event.send(.navigateBack)
        case .clickLike:
            toggleLike()
        case .clickMoreMenu:
            uiState.isMenuVisible = true
        case .dismissMoreMenu:
            uiState.isMenuVisible = false
        case .clickEdit:
            uiState.isMenuVisible = false
        case .clickDelete:
            uiState.isMenuVisible = false
            uiState.isDeleteConfirmVisible = true
        case .confirmDelete:
            deletePost()
        case .dismissDeleteConfirm:
            uiState.isDeleteConfirmVisible = false
        case .clickReport:
            uiState.isMenuVisible = false
        case .changeCommentText(let text):
            uiState.commentText = text
        case .clickSendComment:
            sendComment()
        case .dismissError:
            uiState.errorMessage = nil
        case .clickImage(let imageUrl):
            event.send(.navigateToPicture(imageUrl: imageUrl))
        }
    }

    init(
        postId: String,
        getCommunityPostUseCase: GetCommunityPostUseCase = KoinInitializerKt.resolveGetCommunityPostUseCase(),
        checkCommunityPostLikedUseCase: CheckCommunityPostLikedUseCase = KoinInitializerKt.resolveCheckCommunityPostLikedUseCase(),
        deleteCommunityPostUseCase: DeleteCommunityPostUseCase = KoinInitializerKt.resolveDeleteCommunityPostUseCase(),
        toggleCommunityPostLikeUseCase: ToggleCommunityPostLikeUseCase = KoinInitializerKt.resolveToggleCommunityPostLikeUseCase(),
        getCommunityCommentsUseCase: GetCommunityCommentsUseCase = KoinInitializerKt.resolveGetCommunityCommentsUseCase(),
        addCommunityCommentUseCase: AddCommunityCommentUseCase = KoinInitializerKt.resolveAddCommunityCommentUseCase(),
        observeCurrentUserUseCase: ObserveCurrentUserUseCase = KoinInitializerKt.resolveObserveCurrentUserUseCase()
    ) {
        self.postId = postId
        self.getCommunityPostUseCase = getCommunityPostUseCase
        self.checkCommunityPostLikedUseCase = checkCommunityPostLikedUseCase
        self.deleteCommunityPostUseCase = deleteCommunityPostUseCase
        self.toggleCommunityPostLikeUseCase = toggleCommunityPostLikeUseCase
        self.getCommunityCommentsUseCase = getCommunityCommentsUseCase
        self.addCommunityCommentUseCase = addCommunityCommentUseCase
        self.observeCurrentUserUseCase = observeCurrentUserUseCase

        loadPost()
        loadComments()
    }

    deinit {
        tasks.values.forEach { $0.cancel() }
        tasks.removeAll()
    }

    private enum TaskKey {
        case loadPost
        case checkOwner
        case checkLike
        case loadComments
        case toggleLike
        case deletePost
        case sendComment
    }
}
