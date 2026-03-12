package org.hhp227.concafe.presentation.main.myinfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.hhp227.concafe.domain.common.AppResult
import org.hhp227.concafe.domain.usecase.GetMyInfoUseCase
import org.hhp227.concafe.domain.usecase.ObserveCurrentUserUseCase
import org.hhp227.concafe.presentation.main.myinfo.MyInfoEvent.*
import org.hhp227.concafe.presentation.main.myinfo.MyInfoUiState.Companion.empty

class MyInfoViewModel(
    private val getMyInfoUseCase: GetMyInfoUseCase,
    private val observeCurrentUserUseCase: ObserveCurrentUserUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(empty())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<MyInfoEvent>()
    val event = _event.asSharedFlow()

    private fun observeSession() {
        viewModelScope.launch {
            observeCurrentUserUseCase.invoke().collectLatest {
                loadMyInfo()
            }
        }
    }

    private fun loadMyInfo() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            when (val result = getMyInfoUseCase.invoke()) {
                is AppResult.Success -> {
                    _uiState.value = MyInfoUiState(
                        isLoading = false,
                        errorMessage = null,
                        isLoggedIn = result.data.isLoggedIn,
                        user = result.data.user,
                        summary = result.data.summary,
                        castDetail = result.data.castDetail,
                        ownedCafes = result.data.ownedCafes,
                        badges = result.data.badges,
                        popularCafes = result.data.popularCafes,
                        recentVisits = result.data.recentVisits,
                        favorites = result.data.favorites,
                        followedMaids = result.data.followedMaids
                    )
                }
                is AppResult.Failure -> {
                    _uiState.value = empty().copy(
                        isLoading = false,
                        errorMessage = result.error.toString()
                    )
                }
            }
        }
    }

    fun onAction(action: MyInfoAction) {
        when (action) {
            is MyInfoAction.ClickCafe -> viewModelScope.launch {
                _event.emit(NavigateToCafe(action.id))
            }
            is MyInfoAction.ClickMaid -> viewModelScope.launch {
                _event.emit(NavigateToCast(action.id))
            }
            MyInfoAction.ClickSignIn -> viewModelScope.launch {
                _event.emit(NavigateToSignIn)
            }
            MyInfoAction.Refresh -> loadMyInfo()
        }
    }

    init {
        observeSession()
    }
}
