package org.hhp227.concafe.presentation.main.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.hhp227.concafe.domain.common.AppResult
import org.hhp227.concafe.domain.usecase.GetHomeFeedUseCase
import org.hhp227.concafe.presentation.main.home.HomeUiState.Companion.empty

class HomeViewModel(
    private val getHomeFeedUseCase: GetHomeFeedUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(empty())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<HomeEvent>()
    val event = _event.asSharedFlow()

    private fun loadHomeFeed() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        
        viewModelScope.launch {
            val result = getHomeFeedUseCase.invoke(limit = 10)

            if (result is AppResult.Success) {
                _uiState.value = HomeUiState(
                    isLoading = false,
                    errorMessage = null,
                    banners = result.data.banners,
                    popularCasts = result.data.popularCasts,
                    nearbyCafes = result.data.nearbyCafes,
                    birthdayCasts = result.data.birthdayCasts,
                    notices = result.data.notices
                )
            } else if (result is AppResult.Failure) {
                _uiState.value = empty().copy(
                    isLoading = false,
                    errorMessage = result.error.toString()
                )
            }
        }
    }

    fun onAction(action: HomeAction) {
        when (action) {
            is HomeAction.ClickMaid -> _event.tryEmit(HomeEvent.NavigateToCastDetail(action.id))
            is HomeAction.ClickBirthdayMaid -> _event.tryEmit(HomeEvent.NavigateToCastDetail(action.id))
            is HomeAction.ClickCafe -> _event.tryEmit(HomeEvent.NavigateToCafeDetail(action.id))
        }
    }

    init {
        loadHomeFeed()
    }
}
