package com.hhp227.concafe.presentation.auth.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import concafe.composeapp.generated.resources.Res
import concafe.composeapp.generated.resources.signin_default_kakao_nickname
import concafe.composeapp.generated.resources.signin_error_apple_ios_only
import concafe.composeapp.generated.resources.signin_error_google_failed
import concafe.composeapp.generated.resources.signin_error_invalid_credentials
import concafe.composeapp.generated.resources.signin_error_kakao_failed
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.usecase.CompleteSignUpForCurrentUserUseCase
import com.hhp227.concafe.domain.usecase.SignInUseCase
import com.hhp227.concafe.domain.usecase.SignInWithKakaoIdTokenUseCase
import com.hhp227.concafe.domain.usecase.SignInWithGoogleIdTokenUseCase
import com.hhp227.concafe.domain.usecase.UpdateUserProfileUseCase
import org.jetbrains.compose.resources.getString

class SignInViewModel(
    private val signInUseCase: SignInUseCase,
    private val signInWithGoogleIdTokenUseCase: SignInWithGoogleIdTokenUseCase,
    private val signInWithKakaoIdTokenUseCase: SignInWithKakaoIdTokenUseCase,
    private val completeSignUpForCurrentUserUseCase: CompleteSignUpForCurrentUserUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase,
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
                            errorMessage = getString(Res.string.signin_error_invalid_credentials)
                        )
                    }
                }
            }
        }
    }

    private suspend fun ensureVisitorAccountCompleted(
        email: String,
        nickname: String,
        signupCompleted: Boolean
    ): Boolean {
        if (signupCompleted) return true
        return when (
            completeSignUpForCurrentUserUseCase.invoke(
                email = email,
                nickname = nickname,
                role = UserRole.VISITOR
            )
        ) {
            is AppResult.Success -> true
            is AppResult.Failure -> false
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
                                            errorMessage = getString(Res.string.signin_error_google_failed)
                                        )
                                    }
                                }
                                .onSuccess { idToken ->
                                    when (val result = signInWithGoogleIdTokenUseCase.invoke(idToken)) {
                                        is AppResult.Success -> {
                                            if (ensureVisitorAccountCompleted(
                                                    email = result.data.email,
                                                    nickname = result.data.nickname,
                                                    signupCompleted = result.data.signupCompleted
                                                )
                                            ) {
                                                _uiState.update { it.copy(isLoading = false, errorMessage = null) }
                                                _event.emit(SignInEvent.SignedIn)
                                            } else {
                                                _uiState.update {
                                                    it.copy(
                                                        isLoading = false,
                                                        errorMessage = getString(Res.string.signin_error_google_failed)
                                                    )
                                                }
                                            }
                                        }
                                        is AppResult.Failure -> {
                                            _uiState.update {
                                                it.copy(
                                                    isLoading = false,
                                                    errorMessage = getString(Res.string.signin_error_google_failed)
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
                                    errorMessage = getString(Res.string.signin_error_apple_ios_only)
                                )
                            }
                        }
                        SignInProvider.KAKAO -> {
                            runCatching { kakaoIdTokenProvider.getKakaoAuthPayload() }
                                .onFailure {
                                    _uiState.update { state ->
                                        state.copy(
                                            isLoading = false,
                                            errorMessage = getString(Res.string.signin_error_kakao_failed)
                                        )
                                    }
                                }
                                .onSuccess { payload ->
                                    val email = payload.email?.trim()
                                    val nickname = payload.nickname?.trim()
                                    when (
                                        val kakaoSignInResult = signInWithKakaoIdTokenUseCase.invoke(
                                            idToken = payload.idToken,
                                            email = email,
                                            nickname = nickname
                                        )
                                    ) {
                                        is AppResult.Success -> {
                                            val resolvedNickname = nickname.orEmpty().ifBlank { kakaoSignInResult.data.nickname }
                                            if (resolvedNickname.isNotBlank()) {
                                                updateUserProfileUseCase.invoke(
                                                    nickname = resolvedNickname,
                                                    profileImage = null
                                                )
                                            }
                                            if (ensureVisitorAccountCompleted(
                                                    email = email.orEmpty().ifBlank { kakaoSignInResult.data.email },
                                                    nickname = resolvedNickname.ifBlank { getString(Res.string.signin_default_kakao_nickname) },
                                                    signupCompleted = kakaoSignInResult.data.signupCompleted
                                                )
                                            ) {
                                                _uiState.update { it.copy(isLoading = false, errorMessage = null) }
                                                _event.emit(SignInEvent.SignedIn)
                                            } else {
                                                _uiState.update {
                                                    it.copy(
                                                        isLoading = false,
                                                        errorMessage = getString(Res.string.signin_error_kakao_failed)
                                                    )
                                                }
                                            }
                                        }
                                        is AppResult.Failure -> {
                                            _uiState.update {
                                                it.copy(
                                                    isLoading = false,
                                                    errorMessage = getString(Res.string.signin_error_kakao_failed)
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
