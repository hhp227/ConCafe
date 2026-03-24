package com.hhp227.concafe.presentation.cafe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.event.CafeDetailEvent
import com.hhp227.concafe.domain.usecase.GetCafeCastListPageUseCase
import com.hhp227.concafe.domain.usecase.GetCafeDetailUseCase
import com.hhp227.concafe.domain.usecase.GetCafeNoticePageUseCase
import com.hhp227.concafe.domain.usecase.GetCafeReviewPageUseCase
import com.hhp227.concafe.domain.event.ReviewEvent
import com.hhp227.concafe.domain.event.publisher.CafeDetailEventPublisher
import com.hhp227.concafe.domain.event.publisher.ReviewEventPublisher
import com.hhp227.concafe.domain.usecase.ToggleFavoriteCafeUseCase

class CafeViewModel(
    private val cafeId: String,
    private val getCafeDetailUseCase: GetCafeDetailUseCase,
    private val getCafeCastListPageUseCase: GetCafeCastListPageUseCase,
    private val getCafeNoticePageUseCase: GetCafeNoticePageUseCase,
    private val getCafeReviewPageUseCase: GetCafeReviewPageUseCase,
    private val toggleFavoriteCafeUseCase: ToggleFavoriteCafeUseCase,
    private val cafeDetailEventPublisher: CafeDetailEventPublisher,
    private val reviewEventPublisher: ReviewEventPublisher
) : ViewModel() {
    private val _uiState = MutableStateFlow(CafeUiState.empty())

    val uiState: StateFlow<CafeUiState> = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<CafeEvent>(replay = 0)

    val event = _event.asSharedFlow()

    private val jobs = mutableMapOf<JobKey, Job>()

    private fun observeCafeDetailEvent() {
        jobs[JobKey.OBSERVE_DETAIL_EVENT]?.cancel()
        jobs[JobKey.OBSERVE_DETAIL_EVENT] = viewModelScope.launch {
            cafeDetailEventPublisher.events.collectLatest { event ->
                when (event) {
                    is CafeDetailEvent.CafeInfoUpdated -> loadCafeDetail()
                    is CafeDetailEvent.GoodsCreated,
                    is CafeDetailEvent.GoodsDeleted,
                    is CafeDetailEvent.GoodsUpdated,
                    is CafeDetailEvent.MenuCreated,
                    is CafeDetailEvent.MenuDeleted,
                    is CafeDetailEvent.MenuUpdated -> Unit
                }
            }
        }
    }

    private fun observeReviewEvent() {
        jobs[JobKey.OBSERVE_REVIEW_EVENT]?.cancel()
        jobs[JobKey.OBSERVE_REVIEW_EVENT] = viewModelScope.launch {
            reviewEventPublisher.events.collect { event ->
                when (event) {
                    is ReviewEvent.Created -> {
                        if (event.cafeId == cafeId && _uiState.value.selectedTab == CafeUiState.TabType.REVIEWS) {
                            _uiState.update { it.copy(shouldScrollToTopOnReturn = true) }
                            loadCafeDetail()
                        }
                    }
                    is ReviewEvent.Deleted -> {
                        if (event.cafeId == cafeId && _uiState.value.selectedTab == CafeUiState.TabType.REVIEWS) {
                            _uiState.update { state ->
                                state.copy(
                                    reviews = state.reviews.filterNot { review -> review.id == event.reviewId }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun loadCafeDetail(refreshReviews: Boolean = true) {
        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null
            )
        }

        jobs[JobKey.DETAIL]?.cancel()
        jobs[JobKey.DETAIL] = viewModelScope.launch {
            val result = getCafeDetailUseCase.invoke(cafeId)

            if (result is AppResult.Success) {
                _uiState.value = CafeUiState(
                    isLoading = false,
                    isLoadingMoreCasts = _uiState.value.isLoadingMoreCasts,
                    isLoadingMoreNotices = _uiState.value.isLoadingMoreNotices,
                    isLoadingMoreReviews = _uiState.value.isLoadingMoreReviews,
                    errorMessage = null,
                    selectedTab = _uiState.value.selectedTab,
                    detail = result.data.detail,
                    casts = _uiState.value.casts,
                    castsNextCursor = _uiState.value.castsNextCursor,
                    canLoadMoreCasts = _uiState.value.canLoadMoreCasts,
                    notices = _uiState.value.notices,
                    noticesNextCursor = _uiState.value.noticesNextCursor,
                    canLoadMoreNotices = _uiState.value.canLoadMoreNotices,
                    reviews = result.data.reviews,
                    reviewsNextCursor = result.data.reviewsNextCursor,
                    canLoadMoreReviews = result.data.canLoadMoreReviews,
                    isFavorite = result.data.isFavorite,
                    isLoggedIn = result.data.isLoggedIn,
                    isVisitVerified = result.data.isVisitVerified,
                    shouldScrollToTopOnReturn = _uiState.value.shouldScrollToTopOnReturn
                )
                refreshCastPage()
                if (_uiState.value.selectedTab == CafeUiState.TabType.NOTICES && _uiState.value.notices.isEmpty()) {
                    refreshNoticePage()
                }
                if (refreshReviews && _uiState.value.selectedTab == CafeUiState.TabType.REVIEWS) {
                    refreshReviewPage()
                }
            } else if (result is AppResult.Failure) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "카페 상세 데이터를 불러오지 못했습니다."
                )
            }
        }
    }

    private fun loadCastPage(cursor: String?, append: Boolean) {
        jobs[JobKey.CAST_PAGE]?.cancel()
        jobs[JobKey.CAST_PAGE] = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMoreCasts = append) }

            when (val result = getCafeCastListPageUseCase.invoke(cafeId, cursor)) {
                is AppResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            casts = if (append) state.casts + result.data.items else result.data.items,
                            castsNextCursor = result.data.nextCursor,
                            canLoadMoreCasts = result.data.hasNext,
                            isLoadingMoreCasts = false
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(isLoadingMoreCasts = false) }
                }
            }
        }
    }

    private fun refreshCastPage() {
        loadCastPage(cursor = null, append = false)
    }

    private fun loadMoreCasts() {
        val currentState = _uiState.value
        val cursor = currentState.castsNextCursor
        if (currentState.isLoadingMoreCasts || !currentState.canLoadMoreCasts || cursor == null) return
        loadCastPage(cursor = cursor, append = true)
    }

    private fun loadNoticePage(cursor: String?, append: Boolean) {
        jobs[JobKey.NOTICE_PAGE]?.cancel()
        jobs[JobKey.NOTICE_PAGE] = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMoreNotices = append) }

            when (val result = getCafeNoticePageUseCase.invoke(cafeId = cafeId, query = "", cursor = cursor)) {
                is AppResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            notices = if (append) state.notices + result.data.items else result.data.items,
                            noticesNextCursor = result.data.nextCursor,
                            canLoadMoreNotices = result.data.hasNext,
                            isLoadingMoreNotices = false
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(isLoadingMoreNotices = false) }
                }
            }
        }
    }

    private fun refreshNoticePage() {
        loadNoticePage(cursor = null, append = false)
    }

    private fun loadMoreNotices() {
        val currentState = _uiState.value
        val cursor = currentState.noticesNextCursor
        if (currentState.isLoadingMoreNotices || !currentState.canLoadMoreNotices || cursor == null) return
        loadNoticePage(cursor = cursor, append = true)
    }

    private fun loadReviewPage(cursor: String?, append: Boolean) {
        jobs[JobKey.REVIEW_PAGE]?.cancel()
        jobs[JobKey.REVIEW_PAGE] = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMoreReviews = append) }

            when (val result = getCafeReviewPageUseCase.invoke(cafeId = cafeId, cursor = cursor)) {
                is AppResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            reviews = if (append) state.reviews + result.data.items else result.data.items,
                            reviewsNextCursor = result.data.nextCursor,
                            canLoadMoreReviews = result.data.hasNext,
                            isLoadingMoreReviews = false
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(isLoadingMoreReviews = false) }
                }
            }
        }
    }

    private fun refreshReviewPage() {
        loadReviewPage(cursor = null, append = false)
    }

    private fun loadMoreReviews() {
        val currentState = _uiState.value
        val cursor = currentState.reviewsNextCursor
        if (currentState.isLoadingMoreReviews || !currentState.canLoadMoreReviews || cursor == null) return
        loadReviewPage(cursor = cursor, append = true)
    }

    private fun toggleFavorite() {
        viewModelScope.launch {
            val result = toggleFavoriteCafeUseCase.invoke(cafeId)

            if (result is AppResult.Success) {
                _uiState.update { it.copy(isFavorite = result.data, isLoggedIn = true) }
            } else if (result is AppResult.Failure) {
                if (result.error is AppError.Unauthorized) {
                    _event.emit(CafeEvent.NavigateToSignIn)
                }
            }
        }
    }

    private fun clickWriteReview() {
        viewModelScope.launch {
            if (!_uiState.value.isLoggedIn) {
                _event.emit(CafeEvent.NavigateToSignIn)
            } else {
                _event.emit(CafeEvent.NavigateToReviewEdit(cafeId))
            }
        }
    }

    fun onAction(action: CafeAction) {
        viewModelScope.launch {
            when (action) {
                CafeAction.ClickBack -> {
                    _event.emit(CafeEvent.NavigateBack)
                }
                is CafeAction.ChangeTab -> {
                    _uiState.update { it.copy(selectedTab = action.tab) }
                    if (action.tab == CafeUiState.TabType.NOTICES && _uiState.value.notices.isEmpty()) {
                        refreshNoticePage()
                    }
                    if (action.tab == CafeUiState.TabType.REVIEWS) {
                        refreshReviewPage()
                    }
                }
                is CafeAction.ClickMaid -> {
                    _event.emit(CafeEvent.NavigateToCast(action.id))
                }
                CafeAction.ClickFavorite -> {
                    toggleFavorite()
                }
                CafeAction.ClickWriteReview -> {
                    clickWriteReview()
                }
                CafeAction.LoadMoreCasts -> {
                    loadMoreCasts()
                }
                CafeAction.LoadMoreNotices -> {
                    loadMoreNotices()
                }
                CafeAction.LoadMoreReviews -> {
                    loadMoreReviews()
                }
                CafeAction.Refresh -> {
                    loadCafeDetail()
                }
                CafeAction.ConsumeScrollToTopOnReturn -> {
                    _uiState.update { it.copy(shouldScrollToTopOnReturn = false) }
                }
            }
        }
    }

    override fun onCleared() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        super.onCleared()
    }

    init {
        observeCafeDetailEvent()
        observeReviewEvent()
        loadCafeDetail()
    }

    private enum class JobKey {
        DETAIL,
        CAST_PAGE,
        NOTICE_PAGE,
        REVIEW_PAGE,
        OBSERVE_DETAIL_EVENT,
        OBSERVE_REVIEW_EVENT
    }
}
