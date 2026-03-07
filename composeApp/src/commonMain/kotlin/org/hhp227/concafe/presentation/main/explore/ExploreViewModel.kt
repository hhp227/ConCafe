package org.hhp227.concafe.presentation.main.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.hhp227.concafe.domain.common.AppResult
import org.hhp227.concafe.domain.usecase.GetExploreFeedUseCase
import org.hhp227.concafe.presentation.main.explore.ExploreUiState.Companion.empty

class ExploreViewModel(
    private val getExploreFeedUseCase: GetExploreFeedUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(empty())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<ExploreEvent>()
    val event = _event.asSharedFlow()

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
        loadExploreFeed()
    }
}
