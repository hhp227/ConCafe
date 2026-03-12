package com.hhp227.concafe.presentation.main.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.usecase.GetHomeFeedUseCase
import com.hhp227.concafe.presentation.main.home.HomeUiState.Companion.empty

class HomeViewModel(
    private val getHomeFeedUseCase: GetHomeFeedUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(empty())

    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<HomeEvent>(replay = 0)

    val event = _event.asSharedFlow()

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

    fun onAction(action: HomeAction) {
        viewModelScope.launch {
            when (action) {
                is HomeAction.ClickMaid -> _event.emit(HomeEvent.NavigateToCast(action.id))
                is HomeAction.ClickBirthdayMaid -> _event.emit(HomeEvent.NavigateToCast(action.id))
                is HomeAction.ClickCafe -> _event.emit(HomeEvent.NavigateToCafe(action.id))
                HomeAction.LoadMoreNearbyCafes -> loadMoreNearbyCafes()
            }
        }
    }

    init {
        loadHomeFeed()
    }
}
