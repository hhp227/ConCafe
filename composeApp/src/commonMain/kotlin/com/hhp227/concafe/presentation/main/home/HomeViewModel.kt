package com.hhp227.concafe.presentation.main.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.BannerLinkTargetType
import com.hhp227.concafe.domain.event.BannerEvent
import com.hhp227.concafe.domain.model.Cafe
import com.hhp227.concafe.domain.event.CafeDetailEvent
import com.hhp227.concafe.domain.model.Cast
import com.hhp227.concafe.domain.event.CastEvent
import com.hhp227.concafe.domain.event.publisher.BannerEventPublisher
import com.hhp227.concafe.domain.event.publisher.CafeDetailEventPublisher
import com.hhp227.concafe.domain.event.publisher.CastEventPublisher
import com.hhp227.concafe.domain.model.HomeBanner
import com.hhp227.concafe.domain.usecase.GetHomeFeedUseCase
import com.hhp227.concafe.presentation.main.home.HomeUiState.Companion.empty

class HomeViewModel(
    private val getHomeFeedUseCase: GetHomeFeedUseCase,
    private val bannerEventPublisher: BannerEventPublisher,
    private val cafeDetailEventPublisher: CafeDetailEventPublisher,
    private val castEventPublisher: CastEventPublisher
) : ViewModel() {
    private val _uiState = MutableStateFlow(empty())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<HomeEvent>(replay = 0)
    val event = _event.asSharedFlow()

    private val jobs = mutableMapOf<TaskKey, Job>()

    private fun loadHomeFeed() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = getHomeFeedUseCase.invoke(
                popularCastCursor = null,
                nearbyCafeCursor = null
            )

            if (result is AppResult.Success) {
                _uiState.value = HomeUiState(
                    isLoading = false,
                    errorMessage = null,
                    banners = result.data.banners,
                    popularCasts = result.data.popularCasts,
                    popularCastCafeNames = result.data.popularCastCafeNames,
                    popularCastCursor = result.data.popularCastsNextCursor,
                    canLoadMorePopularCasts = result.data.hasMorePopularCasts,
                    nearbyCafes = result.data.nearbyCafes,
                    nearbyCafeCursor = result.data.nearbyCafesNextCursor,
                    canLoadMoreNearbyCafes = result.data.hasMoreNearbyCafes,
                    birthdayCasts = result.data.birthdayCasts,
                    notices = result.data.notices
                )
            } else if (result is AppResult.Failure) {
                _uiState.value = empty().copy(
                    isLoading = false,
                    errorMessage = result.error.toString()
                )
            } else {
                _uiState.value = empty().copy(
                    isLoading = false,
                    errorMessage = "unknown"
                )
            }
        }
    }

    private fun loadPopularCastPage(cursor: String?, append: Boolean) {
        jobs[TaskKey.POPULAR_CAST_PAGE]?.cancel()
        jobs[TaskKey.POPULAR_CAST_PAGE] = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMorePopularCasts = append) }

            when (val result = getHomeFeedUseCase.invoke(popularCastCursor = cursor, nearbyCafeCursor = null)) {
                is AppResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            popularCasts = if (append) state.popularCasts + result.data.popularCasts else result.data.popularCasts,
                            popularCastCafeNames = if (append) {
                                state.popularCastCafeNames + result.data.popularCastCafeNames
                            } else {
                                result.data.popularCastCafeNames
                            },
                            popularCastCursor = result.data.popularCastsNextCursor,
                            canLoadMorePopularCasts = result.data.hasMorePopularCasts,
                            isLoadingMorePopularCasts = false
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(isLoadingMorePopularCasts = false) }
                }
            }
        }
    }

    private fun loadMorePopularCasts() {
        val currentState = _uiState.value
        val cursor = currentState.popularCastCursor
        if (currentState.isLoadingMorePopularCasts || !currentState.canLoadMorePopularCasts || cursor == null) return
        loadPopularCastPage(cursor = cursor, append = true)
    }

    private fun loadNearbyCafePage(cursor: String?, append: Boolean) {
        jobs[TaskKey.NEARBY_CAFE_PAGE]?.cancel()
        jobs[TaskKey.NEARBY_CAFE_PAGE] = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMoreNearbyCafes = append) }

            when (val result = getHomeFeedUseCase.invoke(popularCastCursor = null, nearbyCafeCursor = cursor)) {
                is AppResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            nearbyCafes = if (append) state.nearbyCafes + result.data.nearbyCafes else result.data.nearbyCafes,
                            nearbyCafeCursor = result.data.nearbyCafesNextCursor,
                            canLoadMoreNearbyCafes = result.data.hasMoreNearbyCafes,
                            isLoadingMoreNearbyCafes = false
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(isLoadingMoreNearbyCafes = false) }
                }
            }
        }
    }

    private fun loadMoreNearbyCafes() {
        val currentState = _uiState.value
        val cursor = currentState.nearbyCafeCursor
        if (currentState.isLoadingMoreNearbyCafes || !currentState.canLoadMoreNearbyCafes || cursor == null) return
        loadNearbyCafePage(cursor = cursor, append = true)
    }

    private fun observeCafeDetailEvent() {
        jobs[TaskKey.OBSERVE_CAFE_DETAIL_EVENT]?.cancel()
        jobs[TaskKey.OBSERVE_CAFE_DETAIL_EVENT] = viewModelScope.launch {
            cafeDetailEventPublisher.observe().collectLatest { event ->
                if (event is CafeDetailEvent.CafeInfoUpdated) {
                    patchCafeInfo(event.cafe)
                }
            }
        }
    }

    private fun observeBannerEvent() {
        jobs[TaskKey.OBSERVE_BANNER_EVENT]?.cancel()
        jobs[TaskKey.OBSERVE_BANNER_EVENT] = viewModelScope.launch {
            bannerEventPublisher.observe().collectLatest { event ->
                when (event) {
                    is BannerEvent.Created -> loadHomeFeed()
                }
            }
        }
    }

    private fun patchCafeInfo(cafe: Cafe) {
        _uiState.update { state ->
            state.copy(
                popularCastCafeNames = state.popularCastCafeNames + (cafe.id to cafe.name),
                nearbyCafes = state.nearbyCafes.map { item ->
                    if (item.id == cafe.id) cafe else item
                }
            )
        }
    }

    private fun observeCastEvent() {
        jobs[TaskKey.OBSERVE_CAST_EVENT]?.cancel()
        jobs[TaskKey.OBSERVE_CAST_EVENT] = viewModelScope.launch {
            castEventPublisher.observe().collectLatest { event ->
                when (event) {
                    is CastEvent.Created -> Unit
                    is CastEvent.Updated -> patchCast(event.cast)
                    is CastEvent.Deleted -> removeCast(event.castId)
                }
            }
        }
    }

    private fun patchCast(cast: Cast) {
        _uiState.update { state ->
            state.copy(
                popularCasts = state.popularCasts.map { item ->
                    if (item.id == cast.id) cast else item
                },
                birthdayCasts = state.birthdayCasts.map { item ->
                    if (item.id == cast.id) cast else item
                }
            )
        }
    }

    private fun removeCast(castId: String) {
        _uiState.update { state ->
            state.copy(
                popularCasts = state.popularCasts.filterNot { it.id == castId },
                birthdayCasts = state.birthdayCasts.filterNot { it.id == castId }
            )
        }
    }

    fun onAction(action: HomeAction) {
        viewModelScope.launch {
            when (action) {
                is HomeAction.ClickBanner -> handleBannerClick(action.banner)
                is HomeAction.ClickMaid -> _event.emit(HomeEvent.NavigateToCast(action.id))
                is HomeAction.ClickBirthdayMaid -> _event.emit(HomeEvent.NavigateToCast(action.id))
                is HomeAction.ClickCafe -> _event.emit(HomeEvent.NavigateToCafe(action.id))
                HomeAction.LoadMorePopularCasts -> loadMorePopularCasts()
                HomeAction.LoadMoreNearbyCafes -> loadMoreNearbyCafes()
            }
        }
    }

    private suspend fun handleBannerClick(banner: HomeBanner) {
        when (banner.targetType) {
            BannerLinkTargetType.EXTERNAL_LINK -> {
                if (banner.targetValue.isNotBlank()) {
                    _event.emit(
                        HomeEvent.NavigateToExternalLink(
                            title = banner.title,
                            url = banner.targetValue
                        )
                    )
                }
            }
            BannerLinkTargetType.CAFE_DETAIL,
            BannerLinkTargetType.EVENT_DETAIL,
            BannerLinkTargetType.NOTICE -> {
                val cafeId = banner.cafeId ?: banner.targetValue.takeIf { banner.targetType == BannerLinkTargetType.CAFE_DETAIL }
                if (!cafeId.isNullOrBlank()) {
                    _event.emit(HomeEvent.NavigateToCafe(cafeId))
                }
            }
        }
    }

    override fun onCleared() {
        jobs.values.forEach(Job::cancel)
        jobs.clear()
        super.onCleared()
    }

    init {
        observeBannerEvent()
        observeCafeDetailEvent()
        observeCastEvent()
        loadHomeFeed()
    }

    private enum class TaskKey {
        OBSERVE_BANNER_EVENT,
        OBSERVE_CAFE_DETAIL_EVENT,
        OBSERVE_CAST_EVENT,
        POPULAR_CAST_PAGE,
        NEARBY_CAFE_PAGE
    }
}
