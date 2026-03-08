package org.hhp227.concafe.presentation.cafe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.hhp227.concafe.domain.common.AppError
import org.hhp227.concafe.domain.common.AppResult
import org.hhp227.concafe.domain.usecase.GetCafeDetailUseCase
import org.hhp227.concafe.domain.usecase.ToggleFavoriteCafeUseCase

class CafeViewModel(
    private val cafeId: String,
    private val getCafeDetailUseCase: GetCafeDetailUseCase,
    private val toggleFavoriteCafeUseCase: ToggleFavoriteCafeUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(CafeUiState.empty())

    val uiState: StateFlow<CafeUiState> = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<CafeEvent>(replay = 0)

    val event = _event.asSharedFlow()

    private fun loadCafeDetail() {
        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            val result = getCafeDetailUseCase.invoke(cafeId)

            if (result is AppResult.Success) {
                _uiState.value = CafeUiState(
                    isLoading = false,
                    errorMessage = null,
                    selectedTab = _uiState.value.selectedTab,
                    detail = result.data.detail,
                    casts = result.data.casts,
                    reviews = result.data.reviews,
                    isFavorite = result.data.isFavorite,
                    isLoggedIn = result.data.isLoggedIn
                )
            } else if (result is AppResult.Failure) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "카페 상세 데이터를 불러오지 못했습니다."
                )
            }
        }
    }

    private fun toggleFavorite() {
        viewModelScope.launch {
            val result = toggleFavoriteCafeUseCase.invoke(cafeId)

            if (result is AppResult.Success) {
                _uiState.update { it.copy(isFavorite = result.data, isLoggedIn = true) }
            } else if (result is AppResult.Failure) {
                if (result.error is AppError.Unauthorized) {
                    _event.emit(CafeEvent.NavigateToSignIn)
                }
            }
        }
    }

    fun onAction(action: CafeAction) {
        viewModelScope.launch {
            when (action) {
                CafeAction.ClickBack -> {
                    _event.emit(CafeEvent.NavigateBack)
                }
                is CafeAction.ChangeTab -> {
                    _uiState.update { it.copy(selectedTab = action.tab) }
                }
                is CafeAction.ClickMaid -> {
                    _event.emit(CafeEvent.NavigateToCast(action.id))
                }
                CafeAction.ClickFavorite -> {
                    toggleFavorite()
                }
                CafeAction.Refresh -> {
                    loadCafeDetail()
                }
            }
        }
    }

    init {
        loadCafeDetail()
    }
}
