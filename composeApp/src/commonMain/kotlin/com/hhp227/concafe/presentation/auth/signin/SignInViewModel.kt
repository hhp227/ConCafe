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
import com.hhp227.concafe.domain.usecase.SignInWithKakaoIdTokenUseCase
import com.hhp227.concafe.domain.usecase.SignInWithGoogleIdTokenUseCase

class SignInViewModel(
    private val signInUseCase: SignInUseCase,
    private val signInWithGoogleIdTokenUseCase: SignInWithGoogleIdTokenUseCase,
    private val signInWithKakaoIdTokenUseCase: SignInWithKakaoIdTokenUseCase,
    private val googleIdTokenProvider: GoogleIdTokenProvider,
    private val kakaoIdTokenProvider: KakaoIdTokenProvider
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
                    when (action.provider) {
                        SignInProvider.GOOGLE -> {
                            runCatching { googleIdTokenProvider.getGoogleIdToken() }
                                .onFailure {
                                    _uiState.update { state ->
                                        state.copy(
                                            isLoading = false,
                                            errorMessage = "구글 로그인에 실패했습니다. 다시 시도해주세요."
                                        )
                                    }
                                }
                                .onSuccess { idToken ->
                                    when (signInWithGoogleIdTokenUseCase.invoke(idToken)) {
                                        is AppResult.Success -> {
                                            _uiState.update { it.copy(isLoading = false, errorMessage = null) }
                                            _event.emit(SignInEvent.SignedIn)
                                        }
                                        is AppResult.Failure -> {
                                            _uiState.update {
                                                it.copy(
                                                    isLoading = false,
                                                    errorMessage = "구글 로그인에 실패했습니다. 다시 시도해주세요."
                                                )
                                            }
                                        }
                                    }
                                }
                        }
                        SignInProvider.APPLE -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = "애플 로그인은 iOS 앱에서 지원됩니다."
                                )
                            }
                        }
                        SignInProvider.KAKAO -> {
                            runCatching { kakaoIdTokenProvider.getKakaoIdToken() }
                                .onFailure {
                                    println("TEST, Kakao getKakaoIdToken failed in SignInViewModel: ${it.message}")
                                    _uiState.update { state ->
                                        state.copy(
                                            isLoading = false,
                                            errorMessage = "카카오 로그인에 실패했습니다. 다시 시도해주세요."
                                        )
                                    }
                                }
                                .onSuccess { idToken ->
                                    println("TEST, Kakao idToken acquired in SignInViewModel. length=${idToken.length}")
                                    val kakaoSignInResult = signInWithKakaoIdTokenUseCase.invoke(idToken)
                                    when (kakaoSignInResult) {
                                        is AppResult.Success -> {
                                            println("TEST, Kakao sign-in usecase success")
                                            _uiState.update { it.copy(isLoading = false, errorMessage = null) }
                                            _event.emit(SignInEvent.SignedIn)
                                        }
                                        is AppResult.Failure -> {
                                            println("TEST, Kakao sign-in usecase failure")
                                            println("TEST, Kakao sign-in failure detail: ${kakaoSignInResult.error}")
                                            _uiState.update {
                                                it.copy(
                                                    isLoading = false,
                                                    errorMessage = "카카오 로그인에 실패했습니다. 다시 시도해주세요."
                                                )
                                            }
                                        }
                                    }
                                }
                        }
                    }
                }
            }
        }
    }
}
