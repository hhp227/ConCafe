package com.hhp227.concafe.presentation.cafe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
import com.hhp227.concafe.domain.model.CafeDetailCast
import com.hhp227.concafe.domain.usecase.DeleteReviewUseCase
import com.hhp227.concafe.domain.usecase.GetCafeCastListPageUseCase
import com.hhp227.concafe.domain.usecase.GetCafeDetailUseCase
import com.hhp227.concafe.domain.usecase.GetCafeMenuGoodsUseCase
import com.hhp227.concafe.domain.usecase.GetCafeEventPageUseCase
import com.hhp227.concafe.domain.usecase.GetCafeNoticePageUseCase
import com.hhp227.concafe.domain.usecase.GetCafeReviewPageUseCase
import com.hhp227.concafe.domain.event.ReviewEvent
import com.hhp227.concafe.domain.event.publisher.CafeDetailEventPublisher
import com.hhp227.concafe.domain.event.publisher.ReviewEventPublisher
import com.hhp227.concafe.domain.model.DetailTooltipType
import com.hhp227.concafe.domain.usecase.MarkDetailTooltipShownUseCase
import com.hhp227.concafe.domain.usecase.ShouldShowDetailTooltipUseCase
import com.hhp227.concafe.domain.usecase.ToggleFavoriteCafeUseCase

class CafeViewModel(
    private val cafeId: String,
    private val getCafeDetailUseCase: GetCafeDetailUseCase,
    private val getCafeMenuGoodsUseCase: GetCafeMenuGoodsUseCase,
    private val getCafeCastListPageUseCase: GetCafeCastListPageUseCase,
    private val getCafeEventPageUseCase: GetCafeEventPageUseCase,
    private val getCafeNoticePageUseCase: GetCafeNoticePageUseCase,
    private val getCafeReviewPageUseCase: GetCafeReviewPageUseCase,
    private val toggleFavoriteCafeUseCase: ToggleFavoriteCafeUseCase,
    private val deleteReviewUseCase: DeleteReviewUseCase,
    private val shouldShowDetailTooltipUseCase: ShouldShowDetailTooltipUseCase,
    private val markDetailTooltipShownUseCase: MarkDetailTooltipShownUseCase,
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
                    is CafeDetailEvent.FavoriteToggled -> {
                        if (event.cafeId == cafeId) {
                            _uiState.update { state -> state.copy(isFavorite = event.isFavorite) }
                        }
                    }
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
                            _uiState.update { state ->
                                state.copy(
                                    shouldScrollToTopOnReturn = true
                                )
                            }
                            refreshReviewPage()
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
                    isLoadingMenuGoods = _uiState.value.isLoadingMenuGoods,
                    hasLoadedMenuGoods = _uiState.value.hasLoadedMenuGoods,
                    isLoadingMoreNotices = _uiState.value.isLoadingMoreNotices,
                    isLoadingMoreReviews = _uiState.value.isLoadingMoreReviews,
                    errorMessage = null,
                    selectedTab = _uiState.value.selectedTab,
                    detail = mergeLoadedMenuGoods(result.data.detail),
                    casts = _uiState.value.casts,
                    castsNextCursor = _uiState.value.castsNextCursor,
                    canLoadMoreCasts = _uiState.value.canLoadMoreCasts,
                    notices = _uiState.value.notices,
                    events = _uiState.value.events,
                    noticesNextCursor = _uiState.value.noticesNextCursor,
                    canLoadMoreNotices = _uiState.value.canLoadMoreNotices,
                    reviews = result.data.reviews,
                    reviewsNextCursor = result.data.reviewsNextCursor,
                    canLoadMoreReviews = result.data.canLoadMoreReviews,
                    isFavorite = result.data.isFavorite,
                    isLoggedIn = result.data.isLoggedIn,
                    isVisitVerified = result.data.isVisitVerified,
                    shouldScrollToTopOnReturn = _uiState.value.shouldScrollToTopOnReturn,
                    currentUserId = result.data.currentUserId,
                    shouldShowFavoriteTooltip = _uiState.value.shouldShowFavoriteTooltip ||
                            shouldShowDetailTooltipUseCase.invoke(DetailTooltipType.CAFE_FAVORITE)
                )
                refreshCastPage()
                if (_uiState.value.selectedTab == CafeUiState.TabType.NOTICES && _uiState.value.notices.isEmpty()) {
                    refreshNoticePage()
                }
                if (_uiState.value.selectedTab == CafeUiState.TabType.NOTICES && _uiState.value.events.isEmpty()) {
                    refreshEventPage()
                }
                if (_uiState.value.selectedTab == CafeUiState.TabType.MENU && !_uiState.value.hasLoadedMenuGoods) {
                    loadMenuGoods()
                }
                if (refreshReviews && _uiState.value.selectedTab == CafeUiState.TabType.REVIEWS) {
                    refreshReviewPage()
                }
            } else if (result is AppResult.Failure) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = null
                )
            }
        }
    }

    private fun loadCastPage(cursor: String?, append: Boolean) {
        jobs[JobKey.CAST_PAGE]?.cancel()
        jobs[JobKey.CAST_PAGE] = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMoreCasts = append) }
            if (append) delay(PAGINATION_DELAY_MILLIS)
            when (val result = getCafeCastListPageUseCase.invoke(cafeId, cursor)) {
                is AppResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            casts = (if (append) state.casts + result.data.items else result.data.items).sortedByTodayWorkFirst(),
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

    private fun loadMenuGoods() {
        val currentDetail = _uiState.value.detail ?: return
        if (_uiState.value.isLoadingMenuGoods || _uiState.value.hasLoadedMenuGoods) {
            return
        }
        jobs[JobKey.MENU_GOODS]?.cancel()
        jobs[JobKey.MENU_GOODS] = viewModelScope.launch {
            _uiState.update { state -> state.copy(isLoadingMenuGoods = true) }

            when (val result = getCafeMenuGoodsUseCase.invoke(currentDetail.cafe.id)) {
                is AppResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isLoadingMenuGoods = false,
                            hasLoadedMenuGoods = true,
                            detail = state.detail?.copy(
                                menus = result.data.menus,
                                goods = result.data.goods
                            )
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update { state -> state.copy(isLoadingMenuGoods = false) }
                }
            }
        }
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
            if (append) delay(PAGINATION_DELAY_MILLIS)
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

    private fun loadEventPage() {
        jobs[JobKey.EVENT_PAGE]?.cancel()
        jobs[JobKey.EVENT_PAGE] = viewModelScope.launch {
            when (val result = getCafeEventPageUseCase.invoke(cafeId = cafeId, query = "", cursor = null, pageSize = 30)) {
                is AppResult.Success -> {
                    _uiState.update { state ->
                        state.copy(events = result.data.items.sortedBy { event -> event.statusPriority() })
                    }
                }
                is AppResult.Failure -> Unit
            }
        }
    }

    private fun refreshEventPage() {
        loadEventPage()
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
            if (append) delay(PAGINATION_DELAY_MILLIS)
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

    private fun deleteReview(reviewId: String) {
        viewModelScope.launch {
            val result = deleteReviewUseCase.invoke(cafeId, reviewId)

            if (result is AppResult.Failure) {
                _event.emit(CafeEvent.ShowReviewDeleteFailedMessage)
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
                    if (action.tab == CafeUiState.TabType.MENU && !_uiState.value.hasLoadedMenuGoods) {
                        loadMenuGoods()
                    }
                    if (action.tab == CafeUiState.TabType.NOTICES && _uiState.value.notices.isEmpty()) {
                        refreshNoticePage()
                    }
                    if (action.tab == CafeUiState.TabType.NOTICES && _uiState.value.events.isEmpty()) {
                        refreshEventPage()
                    }
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
                CafeAction.MarkFavoriteTooltipShown -> {
                    markDetailTooltipShownUseCase.invoke(DetailTooltipType.CAFE_FAVORITE)
                }
                CafeAction.DismissFavoriteTooltip -> {
                    _uiState.update { it.copy(shouldShowFavoriteTooltip = false) }
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
                is CafeAction.ClickEvent -> {
                    _event.emit(CafeEvent.NavigateToCafeEvent(cafeId, action.eventId))
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
                is CafeAction.EditReview -> {
                    _event.emit(CafeEvent.NavigateToReviewEdit(cafeId, action.reviewId))
                }
                is CafeAction.DeleteReview -> {
                    deleteReview(action.reviewId)
                }
                is CafeAction.ClickReviewImage -> {
                    _event.emit(CafeEvent.NavigateToPicture(action.imageUrl))
                }
                is CafeAction.ReportReview -> {
                    _event.emit(CafeEvent.ShowReviewReportedMessage)
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
        EVENT_PAGE,
        REVIEW_PAGE,
        MENU_GOODS,
        OBSERVE_DETAIL_EVENT,
        OBSERVE_REVIEW_EVENT
    }

    private companion object {
        const val PAGINATION_DELAY_MILLIS = 1_000L
    }

    private fun mergeLoadedMenuGoods(detail: com.hhp227.concafe.domain.model.CafeDetail): com.hhp227.concafe.domain.model.CafeDetail {
        val currentDetail = _uiState.value.detail ?: return detail
        if (!_uiState.value.hasLoadedMenuGoods) {
            return detail
        }
        return detail.copy(
            menus = currentDetail.menus,
            goods = currentDetail.goods
        )
    }
}

private fun com.hhp227.concafe.domain.model.CafeEventManagementItem.statusPriority(): Int {
    val normalized = statusLabel.trim().lowercase()
    return when {
        normalized.contains("진행 중") || normalized.contains("진행중") || normalized.contains("ongoing") -> 0
        normalized.contains("예정") || normalized.contains("upcoming") || normalized.contains("scheduled") -> 1
        normalized.contains("종료") || normalized.contains("ended") || normalized.contains("end") -> 2
        else -> 3
    }
}

private fun List<CafeDetailCast>.sortedByTodayWorkFirst(): List<CafeDetailCast> {
    return sortedWith(
        compareByDescending<CafeDetailCast> { it.todaySchedule != null }
            .thenBy { it.cast.name }
    )
}
