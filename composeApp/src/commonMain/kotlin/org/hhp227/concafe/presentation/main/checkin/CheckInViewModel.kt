package org.hhp227.concafe.presentation.main.checkin

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
import org.hhp227.concafe.domain.usecase.GetCheckInGuestFeedUseCase
import org.hhp227.concafe.domain.usecase.GetCheckInUserFeedUseCase
import org.hhp227.concafe.domain.usecase.ObserveCurrentUserUseCase

class CheckInViewModel(
    private val getCheckInGuestFeedUseCase: GetCheckInGuestFeedUseCase,
    private val getCheckInUserFeedUseCase: GetCheckInUserFeedUseCase,
    private val observeCurrentUserUseCase: ObserveCurrentUserUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(CheckInUiState.empty())

    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<CheckInEvent>(replay = 0)

    val event = _event.asSharedFlow()

    private var observeSessionJob: Job? = null

    private fun loadGuestFeed() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            when (val result = getCheckInGuestFeedUseCase.invoke()) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            currentLocationLabel = result.data.currentLocationLabel,
                            mapCafes = result.data.mapCafes,
                            popularCafes = result.data.popularCafes,
                            popularCasts = result.data.popularCasts
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.error.toString()
                        )
                    }
                }
            }
        }
    }

    private fun observeSession() {
        observeSessionJob = viewModelScope.launch {
            observeCurrentUserUseCase.invoke().collectLatest { user ->
                _uiState.update {
                    it.copy(
                        currentUser = user,
                        isLoginPromptVisible = if (user == null) it.isLoginPromptVisible else false
                    )
                }

                if (user == null) {
                    _uiState.update {
                        it.copy(
                            todayVisits = emptyList(),
                            recentVisits = emptyList()
                        )
                    }
                } else {
                    loadUserFeed()
                }
            }
        }
    }

    private fun loadUserFeed() {
        viewModelScope.launch {
            when (val result = getCheckInUserFeedUseCase.invoke()) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            todayVisits = result.data.todayVisits,
                            recentVisits = result.data.recentVisits
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            todayVisits = emptyList(),
                            recentVisits = emptyList(),
                            errorMessage = result.error.toString()
                        )
                    }
                }
            }
        }
    }

    fun onAction(action: CheckInAction) {
        viewModelScope.launch {
            when (action) {
                is CheckInAction.ClickCafe -> _event.emit(CheckInEvent.NavigateToCafe(action.id))
                is CheckInAction.ClickCast -> _event.emit(CheckInEvent.NavigateToCast(action.id))
                CheckInAction.ClickCheckIn -> {
                    val currentUser = _uiState.value.currentUser

                    if (currentUser == null) {
                        _uiState.update { it.copy(isLoginPromptVisible = true) }
                    }
                }
                CheckInAction.ClickSignIn -> {
                    _uiState.update { it.copy(isLoginPromptVisible = false) }
                    _event.emit(CheckInEvent.NavigateToSignIn)
                }
                CheckInAction.ClickSignUp -> {
                    _uiState.update { it.copy(isLoginPromptVisible = false) }
                    _event.emit(CheckInEvent.NavigateToSignIn)
                }
                CheckInAction.DismissLoginPrompt -> {
                    _uiState.update { it.copy(isLoginPromptVisible = false) }
                }
            }
        }
    }

    init {
        observeSession()
        loadGuestFeed()
    }
}
