package com.hhp227.concafe.presentation.main

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
import com.hhp227.concafe.domain.model.MainNavigationTab
import com.hhp227.concafe.domain.usecase.CheckAppUpdateUseCase
import com.hhp227.concafe.domain.usecase.GetMainNavigationUseCase
import com.hhp227.concafe.domain.usecase.ObserveCurrentUserUseCase
import com.hhp227.concafe.domain.usecase.RestoreSessionUseCase

class MainViewModel(
    private val checkAppUpdateUseCase: CheckAppUpdateUseCase,
    private val getMainNavigationUseCase: GetMainNavigationUseCase,
    private val observeCurrentUserUseCase: ObserveCurrentUserUseCase,
    private val restoreSessionUseCase: RestoreSessionUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState.empty())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<MainEvent>()
    val event = _event.asSharedFlow()

    private val jobs = mutableMapOf<JobKey, Job>()

    private fun refreshNavigation(preferredRoute: String? = null) {
        jobs[JobKey.REFRESH_NAVIGATION]?.cancel()
        jobs[JobKey.REFRESH_NAVIGATION] = viewModelScope.launch {
            when (val result = getMainNavigationUseCase.invoke(preferredRoute)) {
                is AppResult.Success -> {
                    if (result.data.currentUser?.signupCompleted == false) {
                        _event.emit(MainEvent.NavigateToSignUp)
                    }
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
            if (state.tabs.any { it.route == route } || route == MainNavigationTab.COMMUNITY.route) {
                state.copy(selectedTab = route)
            } else {
                state
            }
        }
    }

    private fun observeSession() {
        jobs[JobKey.OBSERVE_SESSION]?.cancel()
        jobs[JobKey.OBSERVE_SESSION] = viewModelScope.launch {
            observeCurrentUserUseCase.invoke().collectLatest { newUser ->
                val currentState = _uiState.value
                val roleChanged = newUser?.role != currentState.currentUser?.role
                val loginStateChanged = (newUser == null) != (currentState.currentUser == null)

                if (roleChanged || loginStateChanged) {
                    // Only rebuild navigation when login state or role changes to avoid
                    // spurious selectedTab resets caused by Firestore/auth re-emissions.
                    refreshNavigation(currentState.selectedTab)
                } else {
                    // Same user/role — just sync the user object without touching the tab.
                    _uiState.update { it.copy(currentUser = newUser) }
                }
            }
        }
    }

    private fun restoreSession() {
        viewModelScope.launch {
            when (restoreSessionUseCase.invoke()) {
                is AppResult.Success -> refreshNavigation(_uiState.value.selectedTab)
                is AppResult.Failure -> refreshNavigation(_uiState.value.selectedTab)
            }
        }
    }

    private fun checkAppUpdate(storePlatform: String, storeId: String, currentVersion: String) {
        jobs[JobKey.CHECK_APP_UPDATE]?.cancel()
        jobs[JobKey.CHECK_APP_UPDATE] = viewModelScope.launch {
            when (
                val result = checkAppUpdateUseCase.invoke(
                    storePlatform = storePlatform,
                    storeId = storeId,
                    currentVersion = currentVersion
                )
            ) {
                is AppResult.Success -> result.data?.let { _event.emit(MainEvent.ShowAppUpdate(it)) }
                is AppResult.Failure -> Unit
            }
        }
    }

    fun onAction(action: MainAction) {
        when (action) {
            is MainAction.CheckAppUpdate -> checkAppUpdate(
                storePlatform = action.storePlatform,
                storeId = action.storeId,
                currentVersion = action.currentVersion
            )
            is MainAction.Enter -> refreshNavigation(action.preferredRoute)
            is MainAction.RefreshNavigation -> refreshNavigation(action.preferredRoute)
            is MainAction.SelectTab -> selectTab(action.route)
        }
    }

    override fun onCleared() {
        jobs.values.forEach(Job::cancel)
        jobs.clear()
        super.onCleared()
    }

    init {
        observeSession()
        restoreSession()
    }

    private enum class JobKey {
        CHECK_APP_UPDATE,
        OBSERVE_SESSION,
        REFRESH_NAVIGATION
    }
}
