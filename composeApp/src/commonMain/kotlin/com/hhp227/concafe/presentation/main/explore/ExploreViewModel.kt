package com.hhp227.concafe.presentation.main.explore

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
import com.hhp227.concafe.domain.model.CafeDetailEvent
import com.hhp227.concafe.domain.model.Cast
import com.hhp227.concafe.domain.model.CastEvent
import com.hhp227.concafe.domain.usecase.GetExploreFeedUseCase
import com.hhp227.concafe.domain.usecase.ObserveCafeDetailEventUseCase
import com.hhp227.concafe.domain.usecase.ObserveCastEventUseCase
import com.hhp227.concafe.presentation.main.explore.ExploreUiState.Companion.empty

class ExploreViewModel(
    private val getExploreFeedUseCase: GetExploreFeedUseCase,
    private val observeCafeDetailEventUseCase: ObserveCafeDetailEventUseCase,
    private val observeCastEventUseCase: ObserveCastEventUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(empty())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<ExploreEvent>()
    val event = _event.asSharedFlow()

    private val jobs = mutableMapOf<TaskKey, Job>()

    private fun loadExploreFeed() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val state = _uiState.value
            when (
                val result = getExploreFeedUseCase.invoke(
                    query = state.query,
                    regionKey = state.selectedRegion.key,
                    sortKey = state.selectedSort.key,
                    pageSize = 50
                )
            ) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            cafes = result.data.cafes,
                            maids = result.data.maids
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.error.toString(),
                            cafes = emptyList(),
                            maids = emptyList()
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
                    patchCafe(event.cafe)
                }
            }
        }
    }

    private fun observeCastEvent() {
        jobs[TaskKey.OBSERVE_CAST_EVENT]?.cancel()
        jobs[TaskKey.OBSERVE_CAST_EVENT] = viewModelScope.launch {
            observeCastEventUseCase.invoke().collectLatest { event ->
                when (event) {
                    is CastEvent.Created -> addCastIfVisible(event.cast)
                    is CastEvent.Updated -> patchCast(event.cast)
                    is CastEvent.Deleted -> removeCast(event.castId)
                }
            }
        }
    }

    private fun patchCafe(cafe: Cafe) {
        _uiState.update { state ->
            val cafeMatches = matchesCafeFilters(state, cafe)
            val nextCafes = state.cafes.mapNotNull { item ->
                when {
                    item.id != cafe.id -> item
                    cafeMatches -> cafe
                    else -> null
                }
            }.sortedCafes(state)
            val nextMaids = state.maids.filter { maid ->
                if (maid.cafeId != cafe.id) {
                    true
                } else {
                    matchesCastFilters(state, maid, cafeMatches)
                }
            }.sortedCasts(state)
            state.copy(cafes = nextCafes, maids = nextMaids)
        }
    }

    private fun addCastIfVisible(cast: Cast) {
        _uiState.update { state ->
            if (state.maids.any { it.id == cast.id } || !matchesCastFilters(state, cast)) {
                state
            } else {
                state.copy(maids = (state.maids + cast).sortedCasts(state))
            }
        }
    }

    private fun patchCast(cast: Cast) {
        _uiState.update { state ->
            val nextMaids = state.maids.mapNotNull { item ->
                when {
                    item.id != cast.id -> item
                    matchesCastFilters(state, cast) -> cast
                    else -> null
                }
            }.sortedCasts(state)
            state.copy(maids = nextMaids)
        }
    }

    private fun removeCast(castId: String) {
        _uiState.update { state ->
            state.copy(maids = state.maids.filterNot { it.id == castId })
        }
    }

    fun onAction(action: ExploreAction) {
        when (action) {
            is ExploreAction.QueryChanged -> {
                _uiState.update { it.copy(query = action.query) }
                loadExploreFeed()
            }
            is ExploreAction.RegionChanged -> {
                _uiState.update { it.copy(selectedRegion = action.region) }
                loadExploreFeed()
            }
            is ExploreAction.SortChanged -> {
                _uiState.update { it.copy(selectedSort = action.sort) }
                loadExploreFeed()
            }
            is ExploreAction.TabChanged -> {
                _uiState.update { it.copy(selectedTab = action.tab) }
            }
            is ExploreAction.ClickCafe -> {
                viewModelScope.launch {
                    _event.emit(ExploreEvent.NavigateToCafe(action.id))
                }
            }
            is ExploreAction.ClickMaid -> {
                viewModelScope.launch {
                    _event.emit(ExploreEvent.NavigateToCast(action.id))
                }
            }
            is ExploreAction.Refresh -> loadExploreFeed()
        }
    }

    init {
        observeCafeDetailEvent()
        observeCastEvent()
        loadExploreFeed()
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

private fun matchesCafeFilters(state: ExploreUiState, cafe: Cafe): Boolean {
    val query = state.query.trim()
    val matchesQuery = query.isBlank() || cafe.name.contains(query, ignoreCase = true)
    val matchesRegion = when (state.selectedRegion) {
        ExploreUiState.RegionFilter.ALL -> true
        ExploreUiState.RegionFilter.SEOUL -> cafe.region.country.equals("KR", ignoreCase = true) &&
            cafe.region.city.equals("Seoul", ignoreCase = true)
        ExploreUiState.RegionFilter.TOKYO -> cafe.region.country.equals("JP", ignoreCase = true) &&
            cafe.region.city.equals("Tokyo", ignoreCase = true)
        ExploreUiState.RegionFilter.OSAKA -> cafe.region.country.equals("JP", ignoreCase = true) &&
            cafe.region.city.equals("Osaka", ignoreCase = true)
    }
    return matchesQuery && matchesRegion
}

private fun matchesCastFilters(state: ExploreUiState, cast: Cast, cafeVisible: Boolean? = null): Boolean {
    val query = state.query.trim()
    val matchesQuery = query.isBlank() || cast.name.contains(query, ignoreCase = true)
    val matchesRegion = cafeVisible ?: when (state.selectedRegion) {
        ExploreUiState.RegionFilter.ALL -> true
        else -> state.cafes.any { it.id == cast.cafeId }
    }
    return matchesQuery && matchesRegion
}

private fun List<Cafe>.sortedCafes(state: ExploreUiState): List<Cafe> {
    return when (state.selectedSort) {
        ExploreUiState.SortFilter.POPULAR -> sortedByDescending { it.reviewCount }
        ExploreUiState.SortFilter.LATEST -> sortedByDescending { it.id }
        ExploreUiState.SortFilter.RATING -> sortedByDescending { it.ratingAvg }
    }
}

private fun List<Cast>.sortedCasts(state: ExploreUiState): List<Cast> {
    return when (state.selectedSort) {
        ExploreUiState.SortFilter.POPULAR,
        ExploreUiState.SortFilter.RATING -> sortedByDescending { it.followerCount }
        ExploreUiState.SortFilter.LATEST -> sortedByDescending { it.id }
    }
}
