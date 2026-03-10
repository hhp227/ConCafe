package org.hhp227.concafe.presentation.main

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
import org.hhp227.concafe.domain.usecase.GetMainNavigationUseCase
import org.hhp227.concafe.domain.usecase.ObserveCurrentUserUseCase

class MainViewModel(
    private val getMainNavigationUseCase: GetMainNavigationUseCase,
    private val observeCurrentUserUseCase: ObserveCurrentUserUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState.empty())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<MainEvent>()
    val event = _event.asSharedFlow()

    private var observeSessionJob: Job? = null

    private fun refreshNavigation(preferredRoute: String? = null) {
        viewModelScope.launch {
            when (val result = getMainNavigationUseCase.invoke(preferredRoute)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            currentUser = result.data.currentUser,
                            tabs = result.data.tabs,
                            selectedTab = result.data.selectedTab,
                            thirdTab = result.data.thirdTab
                        )
                    }
                }
                is AppResult.Failure -> {
                    _event.emit(MainEvent.ShowError(result.error.toString()))
                }
            }
        }
    }

    private fun selectTab(route: String) {
        _uiState.update { state ->
            if (state.tabs.any { it.route == route }) {
                state.copy(selectedTab = route)
            } else {
                state
            }
        }
    }

    private fun observeSession() {
        observeSessionJob = viewModelScope.launch {
            observeCurrentUserUseCase.invoke().collectLatest {
                refreshNavigation(_uiState.value.selectedTab)
            }
        }
    }

    fun onAction(action: MainAction) {
        when (action) {
            is MainAction.Enter -> refreshNavigation(action.preferredRoute)
            is MainAction.RefreshNavigation -> refreshNavigation(action.preferredRoute)
            is MainAction.SelectTab -> selectTab(action.route)
        }
    }

    init {
        observeSession()
        onAction(MainAction.Enter())
    }
}
