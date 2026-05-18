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

    private let getCommunityCommentPageUseCase: GetCommunityCommentPageUseCase

    private let addCommunityCommentUseCase: AddCommunityCommentUseCase

    private let updateCommunityCommentUseCase: UpdateCommunityCommentUseCase

    private let deleteCommunityCommentUseCase: DeleteCommunityCommentUseCase

    private let createCommunityPostReportUseCase: CreateCommunityPostReportUseCase

    private let createUserBlockUseCase: CreateUserBlockUseCase

    private let observeCurrentUserUseCase: ObserveCurrentUserUseCase

    private let communityPostEventPublisher: CommunityPostEventPublisher

    @Published private(set) var uiState = PostDetailUiState()

    private let eventSubject = PassthroughSubject<PostDetailViewEvent, Never>()

    var eventPublisher: AnyPublisher<PostDetailViewEvent, Never> {
        eventSubject.eraseToAnyPublisher()
    }

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
                    uiState.currentUserId = user?.id
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

    private func loadInitialCommentPage() {
        tasks[.loadComments]?.cancel()
        tasks[.loadComments] = Task {
            uiState.isLoadingComments = true
            do {
                let result = try await getCommunityCommentPageUseCase.invoke(postId: postId, beforeCursor: nil, pageSize: 5)
                if let success = result as? AppResultSuccess<AnyObject>,
                   let page = success.data as? PagedResult<Comment> {
                    let comments = page.items as! [Comment]
                    uiState.comments = comments.filter { !uiState.blockedUserIds.contains($0.userId) }
                    uiState.hasMoreComments = page.hasNext
                    uiState.oldestCommentCursor = page.nextCursor
                }
            } catch { }
            uiState.isLoadingComments = false
        }
    }

    private func loadMoreComments() {
        guard !uiState.isLoadingMoreComments, uiState.hasMoreComments,
              let cursor = uiState.oldestCommentCursor else { return }
        tasks[.loadMoreComments]?.cancel()
        tasks[.loadMoreComments] = Task {
            uiState.isLoadingMoreComments = true
            do {
                let result = try await getCommunityCommentPageUseCase.invoke(postId: postId, beforeCursor: cursor, pageSize: 5)
                if let success = result as? AppResultSuccess<AnyObject>,
                   let page = success.data as? PagedResult<Comment> {
                    let newComments = (page.items as! [Comment]).filter { !uiState.blockedUserIds.contains($0.userId) }
                    uiState.comments = newComments + uiState.comments
                    uiState.hasMoreComments = page.hasNext
                    uiState.oldestCommentCursor = page.nextCursor
                }
            } catch { }
            uiState.isLoadingMoreComments = false
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
                    eventSubject.send(.navigateBack)
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

    private func confirmEditComment(content: String) {
        guard let commentId = uiState.editingCommentId, !content.isEmpty else { return }
        uiState.isUpdatingComment = true
        tasks[.updateComment]?.cancel()
        tasks[.updateComment] = Task {
            do {
                let result = try await updateCommunityCommentUseCase.invoke(postId: postId, commentId: commentId, content: content)
                if let success = result as? AppResultSuccess<AnyObject>,
                   let updated = success.data as? Comment {
                    uiState.isUpdatingComment = false
                    uiState.editingCommentId = nil
                    uiState.editCommentText = ""
                    uiState.comments = uiState.comments.map { c in c.id == commentId ? updated : c }
                } else {
                    uiState.isUpdatingComment = false
                    uiState.errorMessage = "댓글을 수정하지 못했습니다."
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isUpdatingComment = false
                uiState.errorMessage = "댓글을 수정하지 못했습니다."
            }
        }
    }

    private func deleteComment(commentId: String) {
        tasks[.deleteComment]?.cancel()
        tasks[.deleteComment] = Task {
            do {
                let result = try await deleteCommunityCommentUseCase.invoke(postId: postId, commentId: commentId)
                if result is AppResultSuccess<AnyObject> {
                    uiState.comments = uiState.comments.filter { $0.id != commentId }
                    if let post = uiState.post {
                        uiState.post = CommunityPost(
                            id: post.id,
                            userId: post.userId,
                            userNickname: post.userNickname,
                            title: post.title,
                            content: post.content,
                            imageUrls: post.imageUrls,
                            likeCount: post.likeCount,
                            commentCount: max(0, post.commentCount - 1),
                            createdAt: post.createdAt,
                            displayDate: post.displayDate
                        )
                    }
                } else {
                    uiState.errorMessage = "댓글을 삭제하지 못했습니다."
                }
            } catch {
                if Task.isCancelled { return }
                uiState.errorMessage = "댓글을 삭제하지 못했습니다."
            }
        }
    }

    private func submitReport() {
        guard let reportType = uiState.selectedReportType else { return }
        let reportingCommentId = uiState.reportingCommentId
        uiState.isSubmittingReport = true
        tasks[.submitReport]?.cancel()
        tasks[.submitReport] = Task {
            do {
                let result: Any
                if let reportingCommentId {
                    result = try await createCommunityPostReportUseCase.createCommentReport(
                        postId: postId,
                        commentId: reportingCommentId,
                        reportType: reportType
                    )
                } else {
                    result = try await createCommunityPostReportUseCase.invoke(postId: postId, reportType: reportType)
                }
                if result is AppResultSuccess<AnyObject> {
                    uiState.isSubmittingReport = false
                    uiState.isReportSheetVisible = false
                    uiState.reportingCommentId = nil
                    uiState.selectedReportType = nil
                    uiState.errorMessage = "신고가 접수되었습니다."
                } else {
                    uiState.isSubmittingReport = false
                    uiState.errorMessage = "신고를 접수하지 못했습니다."
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isSubmittingReport = false
                uiState.errorMessage = "신고를 접수하지 못했습니다."
            }
        }
    }

    private func blockUser(blockedUserId: String, blockedNickname: String, closeAfterSuccess: Bool) {
        guard !blockedUserId.isEmpty, !uiState.isBlockingUser else { return }

        uiState.isMenuVisible = false
        uiState.isBlockingUser = true
        tasks[.blockUser]?.cancel()
        tasks[.blockUser] = Task {
            do {
                let result = try await createUserBlockUseCase.invoke(
                    blockedUserId: blockedUserId,
                    blockedNickname: blockedNickname
                )

                if result is AppResultSuccess<AnyObject> {
                    if closeAfterSuccess {
                        eventSubject.send(.navigateBack)
                    } else {
                        uiState.isBlockingUser = false
                        uiState.blockedUserIds.insert(blockedUserId)
                        uiState.comments = uiState.comments.filter { $0.userId != blockedUserId }
                        uiState.errorMessage = "사용자를 차단했습니다."
                    }
                } else {
                    uiState.isBlockingUser = false
                    uiState.errorMessage = "사용자를 차단하지 못했습니다."
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isBlockingUser = false
                uiState.errorMessage = "사용자를 차단하지 못했습니다."
            }
        }
    }

    private func observeCommunityPostEvents() {
        tasks[.observeCommunityEvent]?.cancel()
        tasks[.observeCommunityEvent] = Task {
            do {
                for try await event in asyncSequence(for: communityPostEventPublisher.events) {
                    if let updatedEvent = event as? CommunityPostEvent.Updated,
                       updatedEvent.post.id == postId {
                        uiState.post = updatedEvent.post
                    }
                }
            } catch { }
        }
    }

    func onAction(_ action: PostDetailAction) {
        switch action {
        case .clickBack:
            eventSubject.send(.navigateBack)
        case .clickLike:
            toggleLike()
        case .clickMoreMenu:
            uiState.isMenuVisible = true
        case .dismissMoreMenu:
            uiState.isMenuVisible = false
        case .clickEdit:
            uiState.isMenuVisible = false
            eventSubject.send(.navigateToPostEdit(postId: postId))
        case .clickDelete:
            uiState.isMenuVisible = false
            uiState.isDeleteConfirmVisible = true
        case .confirmDelete:
            deletePost()
        case .dismissDeleteConfirm:
            uiState.isDeleteConfirmVisible = false
        case .clickReport:
            uiState.isMenuVisible = false
            uiState.reportingCommentId = nil
            uiState.selectedReportType = nil
            uiState.isReportSheetVisible = true
        case .clickBlock:
            guard let post = uiState.post else { return }
            blockUser(
                blockedUserId: post.userId,
                blockedNickname: post.userNickname,
                closeAfterSuccess: true
            )
        case .selectReportType(let type):
            uiState.selectedReportType = type
        case .dismissReportSheet:
            uiState.isReportSheetVisible = false
            uiState.reportingCommentId = nil
            uiState.selectedReportType = nil
            uiState.isSubmittingReport = false
        case .submitReport:
            submitReport()
        case .clickEditComment(let commentId):
            let comment = uiState.comments.first { $0.id == commentId }
            uiState.editingCommentId = commentId
            uiState.editCommentText = comment?.content ?? ""
        case .confirmEditComment(let content):
            confirmEditComment(content: content)
        case .dismissEditComment:
            uiState.editingCommentId = nil
            uiState.editCommentText = ""
        case .clickDeleteComment(let commentId):
            deleteComment(commentId: commentId)
        case .clickReportComment(let commentId):
            uiState.reportingCommentId = commentId
            uiState.selectedReportType = nil
            uiState.isReportSheetVisible = true
        case .clickBlockComment(let commentId):
            guard let comment = uiState.comments.first(where: { $0.id == commentId }) else { return }
            blockUser(
                blockedUserId: comment.userId,
                blockedNickname: comment.userNickname,
                closeAfterSuccess: false
            )
        case .changeCommentText(let text):
            uiState.commentText = text
        case .clickSendComment:
            sendComment()
        case .dismissError:
            uiState.errorMessage = nil
        case .clickImage(let imageUrl):
            eventSubject.send(.navigateToPicture(imageUrl: imageUrl))
        case .loadMoreComments:
            loadMoreComments()
        }
    }

    init(
        postId: String,
        getCommunityPostUseCase: GetCommunityPostUseCase = KoinInitializerKt.resolveGetCommunityPostUseCase(),
        checkCommunityPostLikedUseCase: CheckCommunityPostLikedUseCase = KoinInitializerKt.resolveCheckCommunityPostLikedUseCase(),
        deleteCommunityPostUseCase: DeleteCommunityPostUseCase = KoinInitializerKt.resolveDeleteCommunityPostUseCase(),
        toggleCommunityPostLikeUseCase: ToggleCommunityPostLikeUseCase = KoinInitializerKt.resolveToggleCommunityPostLikeUseCase(),
        getCommunityCommentPageUseCase: GetCommunityCommentPageUseCase = KoinInitializerKt.resolveGetCommunityCommentPageUseCase(),
        addCommunityCommentUseCase: AddCommunityCommentUseCase = KoinInitializerKt.resolveAddCommunityCommentUseCase(),
        updateCommunityCommentUseCase: UpdateCommunityCommentUseCase = KoinInitializerKt.resolveUpdateCommunityCommentUseCase(),
        deleteCommunityCommentUseCase: DeleteCommunityCommentUseCase = KoinInitializerKt.resolveDeleteCommunityCommentUseCase(),
        createCommunityPostReportUseCase: CreateCommunityPostReportUseCase = KoinInitializerKt.resolveCreateCommunityPostReportUseCase(),
        createUserBlockUseCase: CreateUserBlockUseCase = KoinInitializerKt.resolveCreateUserBlockUseCase(),
        observeCurrentUserUseCase: ObserveCurrentUserUseCase = KoinInitializerKt.resolveObserveCurrentUserUseCase(),
        communityPostEventPublisher: CommunityPostEventPublisher = KoinInitializerKt.resolveCommunityPostEventPublisher()
    ) {
        self.postId = postId
        self.getCommunityPostUseCase = getCommunityPostUseCase
        self.checkCommunityPostLikedUseCase = checkCommunityPostLikedUseCase
        self.deleteCommunityPostUseCase = deleteCommunityPostUseCase
        self.toggleCommunityPostLikeUseCase = toggleCommunityPostLikeUseCase
        self.getCommunityCommentPageUseCase = getCommunityCommentPageUseCase
        self.addCommunityCommentUseCase = addCommunityCommentUseCase
        self.updateCommunityCommentUseCase = updateCommunityCommentUseCase
        self.deleteCommunityCommentUseCase = deleteCommunityCommentUseCase
        self.createCommunityPostReportUseCase = createCommunityPostReportUseCase
        self.createUserBlockUseCase = createUserBlockUseCase
        self.observeCurrentUserUseCase = observeCurrentUserUseCase
        self.communityPostEventPublisher = communityPostEventPublisher

        loadPost()
        loadInitialCommentPage()
        observeCommunityPostEvents()
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
        case loadMoreComments
        case toggleLike
        case deletePost
        case sendComment
        case updateComment
        case deleteComment
        case observeCommunityEvent
        case submitReport
        case blockUser
    }
}
