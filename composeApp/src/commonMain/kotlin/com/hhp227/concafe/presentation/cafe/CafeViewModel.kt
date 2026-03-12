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
import com.hhp227.concafe.domain.usecase.GetCafeCastListPageUseCase
import com.hhp227.concafe.domain.usecase.GetCafeDetailUseCase
import com.hhp227.concafe.domain.usecase.GetCafeReviewPageUseCase
import com.hhp227.concafe.domain.usecase.ObserveCafeDetailUseCase
import com.hhp227.concafe.domain.usecase.ToggleFavoriteCafeUseCase

class CafeViewModel(
    private val cafeId: String,
    private val getCafeDetailUseCase: GetCafeDetailUseCase,
    private val getCafeCastListPageUseCase: GetCafeCastListPageUseCase,
    private val getCafeReviewPageUseCase: GetCafeReviewPageUseCase,
    private val observeCafeDetailUseCase: ObserveCafeDetailUseCase,
    private val toggleFavoriteCafeUseCase: ToggleFavoriteCafeUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(CafeUiState.empty())

    val uiState: StateFlow<CafeUiState> = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<CafeEvent>(replay = 0)

    val event = _event.asSharedFlow()

    private val jobs = mutableMapOf<JobKey, Job>()

    private fun bindCafeDetail() {
        jobs[JobKey.OBSERVE_DETAIL]?.cancel()
        jobs[JobKey.OBSERVE_DETAIL] = viewModelScope.launch {
            var isInitialEmission = true
            observeCafeDetailUseCase.invoke(cafeId).collectLatest {
                if (isInitialEmission) {
                    isInitialEmission = false
                    return@collectLatest
                }
                loadCafeDetail()
            }
        }
    }

    private fun loadCafeDetail() {
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
                    isLoadingMoreReviews = _uiState.value.isLoadingMoreReviews,
                    errorMessage = null,
                    selectedTab = _uiState.value.selectedTab,
                    detail = result.data.detail,
                    casts = _uiState.value.casts,
                    castsNextCursor = _uiState.value.castsNextCursor,
                    canLoadMoreCasts = _uiState.value.canLoadMoreCasts,
                    reviews = result.data.reviews,
                    reviewsNextCursor = result.data.reviewsNextCursor,
                    canLoadMoreReviews = result.data.canLoadMoreReviews,
                    isFavorite = result.data.isFavorite,
                    isLoggedIn = result.data.isLoggedIn
                )
                refreshCastPage()
                if (_uiState.value.selectedTab == CafeUiState.TabType.REVIEWS) {
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
            _event.emit(CafeEvent.NavigateToReviewEdit(cafeId))
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
                    if (action.tab == CafeUiState.TabType.REVIEWS && _uiState.value.reviews.isEmpty()) {
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
                CafeAction.LoadMoreReviews -> {
                    loadMoreReviews()
                }
                CafeAction.Refresh -> {
                    loadCafeDetail()
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
        bindCafeDetail()
        loadCafeDetail()
    }

    private enum class JobKey {
        DETAIL,
        CAST_PAGE,
        REVIEW_PAGE,
        OBSERVE_DETAIL
    }
}
