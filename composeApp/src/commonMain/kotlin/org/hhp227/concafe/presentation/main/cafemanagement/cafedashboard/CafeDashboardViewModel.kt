package org.hhp227.concafe.presentation.main.cafemanagement.cafedashboard

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
import org.hhp227.concafe.domain.common.AppResult
import org.hhp227.concafe.domain.usecase.GetCafeDashboardUseCase
import org.hhp227.concafe.domain.usecase.ObserveCurrentUserUseCase

class CafeDashboardViewModel(
    private val cafeId: String,
    private val getCafeDashboardUseCase: GetCafeDashboardUseCase,
    private val observeCurrentUserUseCase: ObserveCurrentUserUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(CafeDashboardUiState())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<CafeDashboardEvent>(replay = 0)
    val event = _event.asSharedFlow()

    private var observeSessionJob: Job? = null

    private fun loadCafeDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, infoMessage = null) }

            when (val result = getCafeDashboardUseCase.invoke(cafeId)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            cafe = result.data,
                            isLoading = false
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            cafe = null,
                            isLoading = false,
                            infoMessage = result.error.toString()
                        )
                    }
                }
            }
        }
    }

    private fun clickBack() {
        viewModelScope.launch {
            _event.emit(CafeDashboardEvent.NavigateBack)
        }
    }

    private fun clickShortcut(shortcut: CafeDashboardShortcut) {
        when (shortcut) {
            CafeDashboardShortcut.CAFE_SETTINGS -> {
                viewModelScope.launch {
                    _event.emit(CafeDashboardEvent.NavigateToCafeInfoEdit(cafeId))
                }
            }
            CafeDashboardShortcut.MENU_GOODS -> {
                viewModelScope.launch {
                    _event.emit(CafeDashboardEvent.NavigateToMenuGoods(cafeId))
                }
            }
            else -> {
                _uiState.update {
                    it.copy(infoMessage = "${shortcut.title} 연결은 다음 단계에서 이어집니다.")
                }
            }
        }
    }

    private fun dismissInfoMessage() {
        _uiState.update {
            it.copy(infoMessage = null)
        }
    }

    private fun observeSession() {
        observeSessionJob = viewModelScope.launch {
            observeCurrentUserUseCase.invoke().collectLatest {
                loadCafeDashboard()
            }
        }
    }

    fun onAction(action: CafeDashboardAction) {
        when (action) {
            CafeDashboardAction.ClickBack -> clickBack()
            is CafeDashboardAction.ClickShortcut -> clickShortcut(action.shortcut)
            CafeDashboardAction.DismissInfoMessage -> dismissInfoMessage()
        }
    }

    init {
        observeSession()
        loadCafeDashboard()
    }
}
