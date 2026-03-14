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
import com.hhp227.concafe.domain.model.BannerEvent
import com.hhp227.concafe.domain.model.Cafe
import com.hhp227.concafe.domain.model.CafeDetailEvent
import com.hhp227.concafe.domain.model.Cast
import com.hhp227.concafe.domain.model.CastEvent
import com.hhp227.concafe.domain.model.HomeBanner
import com.hhp227.concafe.domain.usecase.GetHomeFeedUseCase
import com.hhp227.concafe.domain.usecase.ObserveBannerEventUseCase
import com.hhp227.concafe.domain.usecase.ObserveCafeDetailEventUseCase
import com.hhp227.concafe.domain.usecase.ObserveCastEventUseCase
import com.hhp227.concafe.presentation.main.home.HomeUiState.Companion.empty

class HomeViewModel(
    private val getHomeFeedUseCase: GetHomeFeedUseCase,
    private val observeBannerEventUseCase: ObserveBannerEventUseCase,
    private val observeCafeDetailEventUseCase: ObserveCafeDetailEventUseCase,
    private val observeCastEventUseCase: ObserveCastEventUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(empty())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<HomeEvent>(replay = 0)
    val event = _event.asSharedFlow()

    private val jobs = mutableMapOf<TaskKey, Job>()

    private fun loadHomeFeed() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = getHomeFeedUseCase.invoke(nearbyCafeCursor = null)

            if (result is AppResult.Success) {
                _uiState.value = HomeUiState(
                    isLoading = false,
                    errorMessage = null,
                    banners = result.data.banners,
                    popularCasts = result.data.popularCasts,
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

    private suspend fun loadMoreNearbyCafes() {
        val cursor = _uiState.value.nearbyCafeCursor
        val canLoadMoreNearbyCafes = _uiState.value.canLoadMoreNearbyCafes

        if (cursor != null && canLoadMoreNearbyCafes) {
            val result = getHomeFeedUseCase.invoke(nearbyCafeCursor = cursor)

            if (result is AppResult.Success) {
                _uiState.update {
                    it.copy(
                        nearbyCafes = it.nearbyCafes + result.data.nearbyCafes,
                        nearbyCafeCursor = result.data.nearbyCafesNextCursor,
                        canLoadMoreNearbyCafes = result.data.hasMoreNearbyCafes
                    )
                }
            }
        }
    }

    private fun observeCafeDetailEvent() {
        jobs[TaskKey.OBSERVE_CAFE_DETAIL_EVENT]?.cancel()
        jobs[TaskKey.OBSERVE_CAFE_DETAIL_EVENT] = viewModelScope.launch {
            observeCafeDetailEventUseCase.invoke().collectLatest { event ->
                if (event is CafeDetailEvent.CafeInfoUpdated) {
                    patchCafeInfo(event.cafe)
                }
            }
        }
    }

    private fun observeBannerEvent() {
        jobs[TaskKey.OBSERVE_BANNER_EVENT]?.cancel()
        jobs[TaskKey.OBSERVE_BANNER_EVENT] = viewModelScope.launch {
            observeBannerEventUseCase.invoke().collectLatest { event ->
                when (event) {
                    is BannerEvent.Created -> loadHomeFeed()
                }
            }
        }
    }

    private fun patchCafeInfo(cafe: Cafe) {
        _uiState.update { state ->
            state.copy(
                nearbyCafes = state.nearbyCafes.map { item ->
                    if (item.id == cafe.id) cafe else item
                }
            )
        }
    }

    private fun observeCastEvent() {
        jobs[TaskKey.OBSERVE_CAST_EVENT]?.cancel()
        jobs[TaskKey.OBSERVE_CAST_EVENT] = viewModelScope.launch {
            observeCastEventUseCase.invoke().collectLatest { event ->
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
                HomeAction.LoadMoreNearbyCafes -> loadMoreNearbyCafes()
            }
        }
    }

    private suspend fun handleBannerClick(banner: HomeBanner) {
        when (banner.targetType) {
            BannerLinkTargetType.EXTERNAL_LINK -> {
                if (banner.targetValue.isNotBlank()) {
                    _event.emit(HomeEvent.OpenExternalLink(banner.targetValue))
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
        OBSERVE_CAST_EVENT
    }
}
