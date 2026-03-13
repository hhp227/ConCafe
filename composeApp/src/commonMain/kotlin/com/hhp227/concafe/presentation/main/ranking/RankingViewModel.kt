package com.hhp227.concafe.presentation.main.ranking

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
import com.hhp227.concafe.di.resolveGetRankingFeedUseCase
import com.hhp227.concafe.di.resolveObserveCafeDetailEventUseCase
import com.hhp227.concafe.di.resolveObserveCastEventUseCase
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.Cafe
import com.hhp227.concafe.domain.model.CafeDetailEvent
import com.hhp227.concafe.domain.model.Cast
import com.hhp227.concafe.domain.model.CastEvent
import com.hhp227.concafe.domain.usecase.GetRankingFeedUseCase
import com.hhp227.concafe.domain.usecase.ObserveCafeDetailEventUseCase
import com.hhp227.concafe.domain.usecase.ObserveCastEventUseCase

class RankingViewModel(
    private val getRankingFeedUseCase: GetRankingFeedUseCase = resolveGetRankingFeedUseCase(),
    private val observeCafeDetailEventUseCase: ObserveCafeDetailEventUseCase = resolveObserveCafeDetailEventUseCase(),
    private val observeCastEventUseCase: ObserveCastEventUseCase = resolveObserveCastEventUseCase()
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
            observeCafeDetailEventUseCase.invoke().collectLatest { event ->
                if (event is CafeDetailEvent.CafeInfoUpdated) {
                    patchCafeRanking(event.cafe)
                }
            }
        }
    }

    private fun observeCastEvent() {
        jobs[TaskKey.OBSERVE_CAST_EVENT]?.cancel()
        jobs[TaskKey.OBSERVE_CAST_EVENT] = viewModelScope.launch {
            observeCastEventUseCase.invoke().collectLatest { event ->
                when (event) {
                    is CastEvent.Created -> Unit
                    is CastEvent.Updated -> patchCastRanking(event.cast)
                    is CastEvent.Deleted -> removeCastRanking(event.castId)
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
                _event.emit(RankingEvent.NavigateToCast(action.id))
            }
            is RankingAction.ClickCafe -> viewModelScope.launch {
                _event.emit(RankingEvent.NavigateToCafe(action.id))
            }
        }
    }

    init {
        observeCafeDetailEvent()
        observeCastEvent()
        loadRankingFeed()
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
