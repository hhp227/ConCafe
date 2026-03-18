package com.hhp227.concafe.presentation.main.myinfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.Cafe
import com.hhp227.concafe.domain.event.CafeDetailEvent
import com.hhp227.concafe.domain.model.Cast
import com.hhp227.concafe.domain.model.CastDetail
import com.hhp227.concafe.domain.event.CastEvent
import com.hhp227.concafe.domain.event.publisher.CafeDetailEventPublisher
import com.hhp227.concafe.domain.event.publisher.CastEventPublisher
import com.hhp227.concafe.domain.usecase.GetMyInfoUseCase
import com.hhp227.concafe.domain.usecase.ObserveCurrentUserUseCase
import com.hhp227.concafe.presentation.main.myinfo.MyInfoEvent.*
import com.hhp227.concafe.presentation.main.myinfo.MyInfoUiState.Companion.empty

class MyInfoViewModel(
    private val getMyInfoUseCase: GetMyInfoUseCase,
    private val observeCurrentUserUseCase: ObserveCurrentUserUseCase,
    private val cafeDetailEventPublisher: CafeDetailEventPublisher,
    private val castEventPublisher: CastEventPublisher
) : ViewModel() {
    private val _uiState = MutableStateFlow(empty())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<MyInfoEvent>()
    val event = _event.asSharedFlow()

    private val jobs = mutableMapOf<TaskKey, Job>()

    private fun observeSession() {
        viewModelScope.launch {
            observeCurrentUserUseCase.invoke().collectLatest {
                loadMyInfo()
            }
        }
    }

    private fun loadMyInfo() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            when (val result = getMyInfoUseCase.invoke()) {
                is AppResult.Success -> {
                    _uiState.value = MyInfoUiState(
                        isLoading = false,
                        errorMessage = null,
                        isLoggedIn = result.data.isLoggedIn,
                        user = result.data.user,
                        summary = result.data.summary,
                        castDetail = result.data.castDetail,
                        ownedCafes = result.data.ownedCafes,
                        badges = result.data.badges,
                        popularCafes = result.data.popularCafes,
                        recentVisits = result.data.recentVisits,
                        favorites = result.data.favorites,
                        followedMaids = result.data.followedMaids
                    )
                }
                is AppResult.Failure -> {
                    _uiState.value = empty().copy(
                        isLoading = false,
                        errorMessage = result.error.toString()
                    )
                }
            }
        }
    }

    private fun observeCafeDetailEvent() {
        jobs[TaskKey.OBSERVE_CAFE_DETAIL_EVENT]?.cancel()
        jobs[TaskKey.OBSERVE_CAFE_DETAIL_EVENT] = viewModelScope.launch {
            cafeDetailEventPublisher.events.collectLatest { event ->
                if (event is CafeDetailEvent.CafeInfoUpdated) {
                    patchCafe(event.cafe)
                }
            }
        }
    }

    private fun observeCastEvent() {
        jobs[TaskKey.OBSERVE_CAST_EVENT]?.cancel()
        jobs[TaskKey.OBSERVE_CAST_EVENT] = viewModelScope.launch {
            castEventPublisher.events.collectLatest { event ->
                when (event) {
                    is CastEvent.Created -> Unit
                    is CastEvent.Updated -> patchCast(event.cast)
                    is CastEvent.Deleted -> removeCast(event.castId)
                }
            }
        }
    }

    private fun patchCafe(cafe: Cafe) {
        _uiState.update { state ->
            state.copy(
                castDetail = state.castDetail?.takeIf { it.cafe.id == cafe.id }?.copy(cafe = cafe) ?: state.castDetail,
                ownedCafes = state.ownedCafes.map { item ->
                    if (item.id == cafe.id) {
                        item.copy(name = cafe.name, city = cafe.region.city, rating = cafe.ratingAvg)
                    } else {
                        item
                    }
                },
                popularCafes = state.popularCafes.map { item -> if (item.id == cafe.id) cafe else item },
                recentVisits = state.recentVisits.map { item -> if (item.id == cafe.id) cafe else item },
                favorites = state.favorites.map { item -> if (item.id == cafe.id) cafe else item }
            )
        }
    }

    private fun patchCast(cast: Cast) {
        _uiState.update { state ->
            state.copy(
                castDetail = state.castDetail?.takeIf { it.cast.id == cast.id }?.updatedCast(cast) ?: state.castDetail,
                followedMaids = state.followedMaids.map { item -> if (item.id == cast.id) cast else item }
            )
        }
    }

    private fun removeCast(castId: String) {
        _uiState.update { state ->
            state.copy(
                castDetail = state.castDetail?.takeUnless { it.cast.id == castId },
                followedMaids = state.followedMaids.filterNot { it.id == castId }
            )
        }
    }

    fun onAction(action: MyInfoAction) {
        when (action) {
            is MyInfoAction.ClickCafe -> viewModelScope.launch {
                _event.emit(NavigateToCafe(action.id))
            }
            is MyInfoAction.ClickMaid -> viewModelScope.launch {
                _event.emit(NavigateToCast(action.id))
            }
            MyInfoAction.ClickSignIn -> viewModelScope.launch {
                _event.emit(NavigateToSignIn)
            }
            MyInfoAction.Refresh -> loadMyInfo()
        }
    }

    init {
        observeCafeDetailEvent()
        observeCastEvent()
        observeSession()
    }

    override fun onCleared() {
        jobs.values.forEach(Job::cancel)
        jobs.clear()
        super.onCleared()
    }

    private enum class TaskKey {
        OBSERVE_CAFE_DETAIL_EVENT,
        OBSERVE_CAST_EVENT
    }
}

private fun CastDetail.updatedCast(cast: Cast): CastDetail = copy(cast = cast)
