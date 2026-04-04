package com.hhp227.concafe.presentation.main.ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.Cafe
import com.hhp227.concafe.domain.event.CafeDetailEvent
import com.hhp227.concafe.domain.model.Cast
import com.hhp227.concafe.domain.event.CastEvent
import com.hhp227.concafe.domain.event.publisher.CafeDetailEventPublisher
import com.hhp227.concafe.domain.event.publisher.CastEventPublisher
import com.hhp227.concafe.domain.usecase.ClearNativeAdUseCase
import com.hhp227.concafe.domain.usecase.GetRankingFeedUseCase
import com.hhp227.concafe.domain.usecase.LoadNativeAdUseCase
import com.hhp227.concafe.domain.usecase.ObserveCurrentUserUseCase
import com.hhp227.concafe.presentation.main.ranking.RankingEvent.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RankingViewModel(
    private val getRankingFeedUseCase: GetRankingFeedUseCase,
    private val observeCurrentUserUseCase: ObserveCurrentUserUseCase,
    private val loadNativeAdUseCase: LoadNativeAdUseCase,
    private val clearNativeAdUseCase: ClearNativeAdUseCase,
    private val cafeDetailEventPublisher: CafeDetailEventPublisher,
    private val castEventPublisher: CastEventPublisher
) : ViewModel() {
    private val _uiState = MutableStateFlow(RankingUiState.empty)
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<RankingEvent>()
    val event = _event.asSharedFlow()

    private val jobs = mutableMapOf<TaskKey, Job>()

    private fun loadRankingFeed() {
        val currentState = _uiState.value
        val country = currentState.selectedRegion.country
        val city = currentState.selectedRegion.city
        val period = currentState.selectedPeriod

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            when (val result = getRankingFeedUseCase.invoke(period, country, city)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            selectedAdIndex = 0,
                            ads = result.data.ads,
                            maidRankings = result.data.castRankings,
                            cafeRankings = result.data.cafeRankings
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "랭킹 데이터를 불러오지 못했습니다."
                        )
                    }
                }
            }
        }
    }

    private fun observeCafeDetailEvent() {
        jobs[TaskKey.OBSERVE_CAFE_DETAIL_EVENT]?.cancel()
        jobs[TaskKey.OBSERVE_CAFE_DETAIL_EVENT] = viewModelScope.launch {
            cafeDetailEventPublisher.events.collectLatest { event ->
                if (event is CafeDetailEvent.CafeInfoUpdated) {
                    patchCafeRanking(event.cafe)
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
                    is CastEvent.Updated -> patchCastRanking(event.cast)
                    is CastEvent.Deleted -> removeCastRanking(event.castId)
                }
            }
        }
    }

    private fun observeSession() {
        jobs[TaskKey.OBSERVE_SESSION]?.cancel()
        jobs[TaskKey.OBSERVE_SESSION] = viewModelScope.launch {
            observeCurrentUserUseCase.invoke().collectLatest { user ->
                _uiState.update {
                    it.copy(
                        isLoggedIn = user != null,
                        isLoginPromptVisible = if (user == null) it.isLoginPromptVisible else false
                    )
                }
            }
        }
    }

    private fun patchCafeRanking(cafe: Cafe) {
        val subtitle = cafe.region.address.substringBefore("구").substringBefore("로").ifBlank { cafe.region.city }
        _uiState.update { state ->
            state.copy(
                cafeRankings = state.cafeRankings.map { entry ->
                    if (entry.id == cafe.id) {
                        entry.copy(name = cafe.name, subtitle = subtitle)
                    } else {
                        entry
                    }
                }
            )
        }
    }

    private fun patchCastRanking(cast: Cast) {
        _uiState.update { state ->
            state.copy(
                maidRankings = state.maidRankings.map { entry ->
                    if (entry.id == cast.id) {
                        entry.copy(name = cast.name)
                    } else {
                        entry
                    }
                }
            )
        }
    }

    private fun removeCastRanking(castId: String) {
        _uiState.update { state ->
            state.copy(maidRankings = state.maidRankings.filterNot { it.id == castId })
        }
    }

    fun loadNativeAdIfNeeded() {
        if (_uiState.value.nativeAd != null) return
        viewModelScope.launch {
            val ad = loadNativeAdUseCase.invoke()

            _uiState.update {
                it.copy(nativeAd = ad)
            }
        }
    }

    fun clearAd() {
        clearNativeAdUseCase.invoke(uiState.value.nativeAd)
        _uiState.update {
            it.copy(nativeAd = null)
        }
    }

    fun onAction(action: RankingAction) {
        when (action) {
            is RankingAction.ChangePeriod -> {
                _uiState.update { it.copy(selectedPeriod = action.period) }
                loadRankingFeed()
            }
            is RankingAction.ChangeRegion -> {
                _uiState.update { it.copy(selectedRegion = action.region) }
                loadRankingFeed()
            }
            is RankingAction.ChangeTab -> _uiState.update { it.copy(selectedTab = action.tab) }
            is RankingAction.SelectAd -> _uiState.update {
                val lastIndex = (it.ads.size - 1).coerceAtLeast(0)
                it.copy(selectedAdIndex = action.index.coerceIn(0, lastIndex))
            }
            is RankingAction.ClickMaid -> viewModelScope.launch {
                requireSignedIn { _event.emit(NavigateToCast(action.id)) }
            }
            is RankingAction.ClickCafe -> viewModelScope.launch {
                requireSignedIn { _event.emit(NavigateToCafe(action.id)) }
            }
            RankingAction.ClickLoginPromptSignIn -> viewModelScope.launch {
                _uiState.update { it.copy(isLoginPromptVisible = false) }
                _event.emit(NavigateToSignIn)
            }
            RankingAction.DismissLoginPrompt -> _uiState.update { it.copy(isLoginPromptVisible = false) }
            is RankingAction.UpdateBannerHeight -> {
                _uiState.update { state ->
                    if (action.heightPx > state.bannerHeightPx) {
                        state.copy(bannerHeightPx = action.heightPx)
                    } else {
                        state
                    }
                }
            }
        }
    }

    private suspend fun requireSignedIn(onAuthenticated: suspend () -> Unit) {
        if (_uiState.value.isLoggedIn) {
            onAuthenticated()
        } else {
            _uiState.update { it.copy(isLoginPromptVisible = true) }
        }
    }

    override fun onCleared() {
        jobs.values.forEach(Job::cancel)
        jobs.clear()
        clearAd()
        super.onCleared()
    }

    init {
        observeSession()
        observeCafeDetailEvent()
        observeCastEvent()
        loadRankingFeed()
        loadNativeAdIfNeeded()
    }

    private enum class TaskKey {
        OBSERVE_CAFE_DETAIL_EVENT,
        OBSERVE_CAST_EVENT,
        OBSERVE_SESSION
    }
}
