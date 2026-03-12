package com.hhp227.concafe.presentation.main.ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.hhp227.concafe.di.resolveGetRankingFeedUseCase
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.usecase.GetRankingFeedUseCase

class RankingViewModel(
    private val getRankingFeedUseCase: GetRankingFeedUseCase = resolveGetRankingFeedUseCase()
) : ViewModel() {
    private val _uiState = MutableStateFlow(RankingUiState.empty)
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<RankingEvent>()
    val event = _event.asSharedFlow()

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
        loadRankingFeed()
    }
}
