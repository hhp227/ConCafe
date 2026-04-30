package com.hhp227.concafe.presentation.community.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.event.CommunityPostEvent
import com.hhp227.concafe.domain.event.publisher.CommunityPostEventPublisher
import com.hhp227.concafe.domain.usecase.AddCommunityCommentUseCase
import com.hhp227.concafe.domain.usecase.CheckCommunityPostLikedUseCase
import com.hhp227.concafe.domain.usecase.DeleteCommunityCommentUseCase
import com.hhp227.concafe.domain.usecase.DeleteCommunityPostUseCase
import com.hhp227.concafe.domain.usecase.GetCommunityCommentPageUseCase
import com.hhp227.concafe.domain.usecase.GetCommunityPostUseCase
import com.hhp227.concafe.domain.usecase.ObserveCurrentUserUseCase
import com.hhp227.concafe.domain.usecase.ToggleCommunityPostLikeUseCase
import com.hhp227.concafe.domain.usecase.UpdateCommunityCommentUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val COMMENT_PAGE_SIZE = 5

class PostDetailViewModel(
    private val postId: String,
    private val getCommunityPostUseCase: GetCommunityPostUseCase,
    private val checkCommunityPostLikedUseCase: CheckCommunityPostLikedUseCase,
    private val deleteCommunityPostUseCase: DeleteCommunityPostUseCase,
    private val toggleCommunityPostLikeUseCase: ToggleCommunityPostLikeUseCase,
    private val getCommunityCommentPageUseCase: GetCommunityCommentPageUseCase,
    private val addCommunityCommentUseCase: AddCommunityCommentUseCase,
    private val updateCommunityCommentUseCase: UpdateCommunityCommentUseCase,
    private val deleteCommunityCommentUseCase: DeleteCommunityCommentUseCase,
    private val observeCurrentUserUseCase: ObserveCurrentUserUseCase,
    private val communityPostEventPublisher: CommunityPostEventPublisher
) : ViewModel() {
    private val _uiState = MutableStateFlow(PostDetailUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<PostDetailEvent>(replay = 0)
    val event = _event.asSharedFlow()

    private val jobs = mutableMapOf<JobKey, Job>()

    private fun loadPost() {
        jobs[JobKey.LOAD_POST]?.cancel()
        jobs[JobKey.LOAD_POST] = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = getCommunityPostUseCase(postId)) {
                is AppResult.Success -> {
                    val post = result.data
                    _uiState.update { it.copy(post = post, isLoading = false) }
                    checkIsOwner(postUserId = post.userId)
                    checkLikeStatus()
                }
                is AppResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = "게시글을 불러오지 못했습니다.")
                }
            }
        }
    }

    private fun checkIsOwner(postUserId: String) {
        jobs[JobKey.CHECK_OWNER]?.cancel()
        jobs[JobKey.CHECK_OWNER] = viewModelScope.launch {
            val currentUser = observeCurrentUserUseCase.invoke().first()
            _uiState.update {
                it.copy(currentUserId = currentUser?.id, isOwner = currentUser?.id == postUserId)
            }
        }
    }

    private fun checkLikeStatus() {
        jobs[JobKey.CHECK_LIKE]?.cancel()
        jobs[JobKey.CHECK_LIKE] = viewModelScope.launch {
            when (val liked = checkCommunityPostLikedUseCase(postId)) {
                is AppResult.Success -> _uiState.update { it.copy(isLiked = liked.data) }
                is AppResult.Failure -> Unit
            }
        }
    }

    private fun loadInitialCommentPage() {
        jobs[JobKey.LOAD_COMMENTS]?.cancel()
        jobs[JobKey.LOAD_COMMENTS] = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingComments = true) }
            when (val result = getCommunityCommentPageUseCase(postId, null, COMMENT_PAGE_SIZE)) {
                is AppResult.Success -> {
                    val page = result.data
                    _uiState.update {
                        it.copy(
                            comments = page.items,
                            isLoadingComments = false,
                            hasMoreComments = page.hasNext,
                            oldestCommentCursor = page.nextCursor
                        )
                    }
                }
                is AppResult.Failure -> _uiState.update { it.copy(isLoadingComments = false) }
            }
        }
    }

    private fun loadMoreComments() {
        if (_uiState.value.isLoadingMoreComments || !_uiState.value.hasMoreComments) return
        val cursor = _uiState.value.oldestCommentCursor ?: return
        jobs[JobKey.LOAD_MORE_COMMENTS]?.cancel()
        jobs[JobKey.LOAD_MORE_COMMENTS] = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMoreComments = true) }
            when (val result = getCommunityCommentPageUseCase(postId, cursor, COMMENT_PAGE_SIZE)) {
                is AppResult.Success -> {
                    val page = result.data
                    _uiState.update { state ->
                        state.copy(
                            comments = page.items + state.comments,
                            isLoadingMoreComments = false,
                            hasMoreComments = page.hasNext,
                            oldestCommentCursor = page.nextCursor
                        )
                    }
                }
                is AppResult.Failure -> _uiState.update { it.copy(isLoadingMoreComments = false) }
            }
        }
    }

    private fun toggleLike() {
        val wasLiked = _uiState.value.isLiked
        val currentCount = _uiState.value.likeCount
        _uiState.update { state ->
            state.copy(
                isLiked = !wasLiked,
                post = state.post?.copy(likeCount = if (wasLiked) maxOf(0, currentCount - 1) else currentCount + 1)
            )
        }
        jobs[JobKey.TOGGLE_LIKE]?.cancel()
        jobs[JobKey.TOGGLE_LIKE] = viewModelScope.launch {
            when (toggleCommunityPostLikeUseCase(postId)) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> _uiState.update { state ->
                    state.copy(
                        isLiked = wasLiked,
                        post = state.post?.copy(likeCount = currentCount)
                    )
                }
            }
        }
    }

    private fun deletePost() {
        _uiState.update { it.copy(isDeleting = true, isDeleteConfirmVisible = false) }
        jobs[JobKey.DELETE_POST]?.cancel()
        jobs[JobKey.DELETE_POST] = viewModelScope.launch {
            when (deleteCommunityPostUseCase(postId)) {
                is AppResult.Success -> _event.emit(PostDetailEvent.NavigateBack)
                is AppResult.Failure -> _uiState.update {
                    it.copy(isDeleting = false, errorMessage = "게시글을 삭제하지 못했습니다.")
                }
            }
        }
    }

    private fun sendComment() {
        val text = _uiState.value.commentText.trim()
        if (text.isBlank()) return
        _uiState.update { it.copy(isSendingComment = true) }
        jobs[JobKey.SEND_COMMENT]?.cancel()
        jobs[JobKey.SEND_COMMENT] = viewModelScope.launch {
            when (val result = addCommunityCommentUseCase(postId, text)) {
                is AppResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isSendingComment = false,
                            commentText = "",
                            comments = state.comments + result.data,
                            post = state.post?.copy(commentCount = state.post.commentCount + 1)
                        )
                    }
                }
                is AppResult.Failure -> _uiState.update {
                    it.copy(isSendingComment = false, errorMessage = "댓글을 등록하지 못했습니다.")
                }
            }
        }
    }

    private fun confirmEditComment(content: String) {
        val commentId = _uiState.value.editingCommentId ?: return
        if (content.isBlank()) return
        _uiState.update { it.copy(isUpdatingComment = true) }
        jobs[JobKey.UPDATE_COMMENT]?.cancel()
        jobs[JobKey.UPDATE_COMMENT] = viewModelScope.launch {
            when (val result = updateCommunityCommentUseCase(postId, commentId, content)) {
                is AppResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isUpdatingComment = false,
                            editingCommentId = null,
                            editCommentText = "",
                            comments = state.comments.map { c ->
                                if (c.id == commentId) result.data else c
                            }
                        )
                    }
                }
                is AppResult.Failure -> _uiState.update {
                    it.copy(isUpdatingComment = false, errorMessage = "댓글을 수정하지 못했습니다.")
                }
            }
        }
    }

    private fun deleteComment(commentId: String) {
        jobs[JobKey.DELETE_COMMENT]?.cancel()
        jobs[JobKey.DELETE_COMMENT] = viewModelScope.launch {
            when (deleteCommunityCommentUseCase(postId, commentId)) {
                is AppResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            comments = state.comments.filter { it.id != commentId },
                            post = state.post?.copy(commentCount = maxOf(0, state.post.commentCount - 1))
                        )
                    }
                }
                is AppResult.Failure -> _uiState.update {
                    it.copy(errorMessage = "댓글을 삭제하지 못했습니다.")
                }
            }
        }
    }

    private fun observeCommunityPostEvents() {
        jobs[JobKey.OBSERVE_COMMUNITY_EVENT]?.cancel()
        jobs[JobKey.OBSERVE_COMMUNITY_EVENT] = viewModelScope.launch {
            communityPostEventPublisher.events.collect { event ->
                if (event is CommunityPostEvent.Updated && event.post.id == postId) {
                    _uiState.update { it.copy(post = event.post) }
                }
            }
        }
    }

    fun onAction(action: PostDetailAction) {
        when (action) {
            PostDetailAction.ClickBack -> {
                jobs[JobKey.EMIT_EVENT]?.cancel()
                jobs[JobKey.EMIT_EVENT] = viewModelScope.launch {
                    _event.emit(PostDetailEvent.NavigateBack)
                }
            }
            PostDetailAction.ClickLike -> toggleLike()
            PostDetailAction.ClickMoreMenu -> _uiState.update { it.copy(isMenuVisible = true) }
            PostDetailAction.DismissMoreMenu -> _uiState.update { it.copy(isMenuVisible = false) }
            PostDetailAction.ClickEdit -> {
                _uiState.update { it.copy(isMenuVisible = false) }
                jobs[JobKey.EMIT_EVENT]?.cancel()
                jobs[JobKey.EMIT_EVENT] = viewModelScope.launch {
                    _event.emit(PostDetailEvent.NavigateToPostEdit(postId))
                }
            }
            PostDetailAction.ClickDelete -> _uiState.update { it.copy(isMenuVisible = false, isDeleteConfirmVisible = true) }
            PostDetailAction.ConfirmDelete -> deletePost()
            PostDetailAction.DismissDeleteConfirm -> _uiState.update { it.copy(isDeleteConfirmVisible = false) }
            PostDetailAction.ClickReport -> _uiState.update { it.copy(isMenuVisible = false) }
            is PostDetailAction.ClickEditComment -> {
                val comment = _uiState.value.comments.find { it.id == action.commentId }
                _uiState.update { it.copy(editingCommentId = action.commentId, editCommentText = comment?.content.orEmpty()) }
            }
            is PostDetailAction.ConfirmEditComment -> confirmEditComment(action.content)
            PostDetailAction.DismissEditComment -> _uiState.update { it.copy(editingCommentId = null, editCommentText = "") }
            is PostDetailAction.ClickDeleteComment -> deleteComment(action.commentId)
            is PostDetailAction.ClickReportComment -> _uiState.update { it.copy(errorMessage = "신고가 접수되었습니다.") }
            is PostDetailAction.ChangeCommentText -> _uiState.update { it.copy(commentText = action.text) }
            PostDetailAction.ClickSendComment -> sendComment()
            PostDetailAction.DismissError -> _uiState.update { it.copy(errorMessage = null) }
            is PostDetailAction.ClickImage -> {
                jobs[JobKey.EMIT_EVENT]?.cancel()
                jobs[JobKey.EMIT_EVENT] = viewModelScope.launch {
                    _event.emit(PostDetailEvent.NavigateToPicture(action.imageUrl))
                }
            }
            PostDetailAction.LoadMoreComments -> loadMoreComments()
        }
    }

    override fun onCleared() {
        jobs.values.forEach(Job::cancel)
        jobs.clear()
        super.onCleared()
    }

    init {
        loadPost()
        loadInitialCommentPage()
        observeCommunityPostEvents()
    }

    private enum class JobKey {
        LOAD_POST, CHECK_OWNER, CHECK_LIKE, LOAD_COMMENTS, LOAD_MORE_COMMENTS,
        TOGGLE_LIKE, DELETE_POST, SEND_COMMENT, UPDATE_COMMENT, DELETE_COMMENT,
        EMIT_EVENT, OBSERVE_COMMUNITY_EVENT
    }
}
