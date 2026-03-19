package com.hhp227.concafe.presentation.auth.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.usecase.SignInUseCase
import com.hhp227.concafe.domain.usecase.SignInWithSocialProviderUseCase

class SignInViewModel(
    private val signInUseCase: SignInUseCase,
    private val signInWithSocialProviderUseCase: SignInWithSocialProviderUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(SignInUiState.empty())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<SignInEvent>()
    val event = _event.asSharedFlow()

    private fun changeEmail(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null) }
    }

    private fun changePassword(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    private fun signIn(email: String, password: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            when (val result = signInUseCase.invoke(email, password)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = null) }
                    _event.emit(SignInEvent.SignedIn)
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "로그인에 실패했습니다. 입력값을 확인해주세요."
                        )
                    }
                }
            }
        }
    }

    fun onAction(action: SignInAction) {
        when (action) {
            is SignInAction.ChangeEmail -> changeEmail(action.value)
            is SignInAction.ChangePassword -> changePassword(action.value)
            SignInAction.ClickEmailSignIn -> signIn(uiState.value.email, uiState.value.password)
            is SignInAction.ClickSocialSignIn -> {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                viewModelScope.launch {
                    when (signInWithSocialProviderUseCase.invoke(action.provider.name.lowercase())) {
                        is AppResult.Success -> {
                            _uiState.update { it.copy(isLoading = false, errorMessage = null) }
                            _event.emit(SignInEvent.SignedIn)
                        }
                        is AppResult.Failure -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = "소셜 로그인에 실패했습니다. 입력값을 확인해주세요."
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
