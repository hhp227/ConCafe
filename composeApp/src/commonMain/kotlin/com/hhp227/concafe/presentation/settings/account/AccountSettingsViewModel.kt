package com.hhp227.concafe.presentation.settings.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import concafe.composeapp.generated.resources.Res
import concafe.composeapp.generated.resources.account_settings_delete_password_required
import concafe.composeapp.generated.resources.account_settings_error_delete_apple_ios_only
import concafe.composeapp.generated.resources.account_settings_error_delete_failed
import concafe.composeapp.generated.resources.account_settings_error_delete_google_reauth_failed
import concafe.composeapp.generated.resources.account_settings_error_delete_invalid_password
import concafe.composeapp.generated.resources.account_settings_error_delete_kakao_reauth_failed
import concafe.composeapp.generated.resources.account_settings_error_load_failed
import concafe.composeapp.generated.resources.account_settings_error_profile_save_failed
import concafe.composeapp.generated.resources.account_settings_error_session_expired
import concafe.composeapp.generated.resources.account_settings_message_cast_profile_missing
import concafe.composeapp.generated.resources.account_settings_message_enter_nickname
import concafe.composeapp.generated.resources.account_settings_message_saved
import com.hhp227.concafe.domain.common.AppError
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.AuthProvider
import com.hhp227.concafe.domain.model.DeleteAccountRequest
import com.hhp227.concafe.domain.usecase.DeleteAccountUseCase
import com.hhp227.concafe.domain.usecase.GetMyInfoUseCase
import com.hhp227.concafe.domain.usecase.ObserveCurrentUserUseCase
import com.hhp227.concafe.domain.usecase.UpdateUserProfileUseCase
import com.hhp227.concafe.presentation.auth.signin.GoogleIdTokenProvider
import com.hhp227.concafe.presentation.auth.signin.KakaoIdTokenProvider
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

class AccountSettingsViewModel(
    private val getMyInfoUseCase: GetMyInfoUseCase,
    private val observeCurrentUserUseCase: ObserveCurrentUserUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase,
    private val googleIdTokenProvider: GoogleIdTokenProvider,
    private val kakaoIdTokenProvider: KakaoIdTokenProvider
) : ViewModel() {
    private val _uiState = MutableStateFlow(AccountSettingsUiState.empty())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<AccountSettingsEvent>(replay = 0)
    val event = _event.asSharedFlow()

    private val jobs = mutableMapOf<TaskKey, Job>()

    private fun observeSession() {
        jobs[TaskKey.ObserveSession]?.cancel()
        jobs[TaskKey.ObserveSession] = viewModelScope.launch {
            observeCurrentUserUseCase.invoke().collectLatest {
                loadAccountSettings()
            }
        }
    }

    private fun loadAccountSettings() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        jobs[TaskKey.LoadAccountSettings]?.cancel()
        jobs[TaskKey.LoadAccountSettings] = viewModelScope.launch {
            when (val result = getMyInfoUseCase.invoke()) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = null,
                        myInfoFeed = result.data,
                        nicknameInput = result.data.user?.nickname.orEmpty()
                    )
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = getString(Res.string.account_settings_error_load_failed)
                        )
                    }
                }
            }
        }
    }

    private fun clickBack() {
        viewModelScope.launch {
            _event.emit(AccountSettingsEvent.NavigateBack)
        }
    }

    private fun clickSaveUserInfo() {
        val state = _uiState.value
        val nicknameInput = state.nicknameInput
        val profileImage = state.myInfoFeed?.user?.profileImage

        if (nicknameInput.isBlank()) {
            emitMessage(Res.string.account_settings_message_enter_nickname)
        } else {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null
                )
            }

            jobs[TaskKey.SaveUserInfo]?.cancel()
            jobs[TaskKey.SaveUserInfo] = viewModelScope.launch {
                when (val result = updateUserProfileUseCase.invoke(nicknameInput, profileImage)) {
                    is AppResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = null,
                                nicknameInput = result.data.nickname,
                                myInfoFeed = it.myInfoFeed?.copy(user = result.data)
                            )
                        }
                        emitMessage(Res.string.account_settings_message_saved)
                    }

                    is AppResult.Failure -> {
                        val message = mapProfileUpdateFailureMessage(result)

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = null
                            )
                        }
                        emitMessage(message)
                    }
                }
            }
        }
    }

    private fun clickOpenCastEdit() {
        val state = _uiState.value
        val cast = state.myInfoFeed?.castDetail?.cast
        if (cast?.id.isNullOrBlank()) {
            emitMessage(Res.string.account_settings_message_cast_profile_missing)
            return
        }
        viewModelScope.launch {
            _event.emit(
                AccountSettingsEvent.NavigateToCastEdit(
                    cafeId = cast!!.cafeId,
                    castId = cast.id
                )
            )
        }
    }

    private fun clickOpenChangePassword() {
        viewModelScope.launch {
            _event.emit(AccountSettingsEvent.NavigateToChangePassword)
        }
    }

    private fun clickShowDeleteDialog() {
        _uiState.update {
            it.copy(
                isDeleteDialogVisible = true,
                deletePassword = "",
                deletePasswordErrorMessage = null
            )
        }
    }

    private fun clickDismissDeleteDialog() {
        _uiState.update {
            it.copy(
                isDeleteDialogVisible = false,
                deletePassword = "",
                deletePasswordErrorMessage = null
            )
        }
    }

    private fun clickDeleteAccount() {
        val state = _uiState.value
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        jobs[TaskKey.DeleteAccount]?.cancel()
        jobs[TaskKey.DeleteAccount] = viewModelScope.launch {
            val result = when (state.authProvider) {
                AuthProvider.EMAIL, AuthProvider.UNKNOWN -> {
                    if (state.deletePassword.isBlank()) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                deletePasswordErrorMessage = getString(Res.string.account_settings_delete_password_required)
                            )
                        }
                        return@launch
                    }
                    deleteAccountUseCase.invoke(
                        DeleteAccountRequest(
                            provider = AuthProvider.EMAIL,
                            password = state.deletePassword
                        )
                    )
                }
                AuthProvider.GOOGLE -> {
                    runCatching { googleIdTokenProvider.getGoogleIdToken() }
                        .fold(
                            onSuccess = { token ->
                                deleteAccountUseCase.invoke(
                                    DeleteAccountRequest(
                                        provider = AuthProvider.GOOGLE,
                                        idToken = token
                                    )
                                )
                            },
                            onFailure = {
                                AppResult.Failure(
                                    AppError.ValidationFailed("google reauth failed")
                                )
                            }
                        )
                }
                AuthProvider.KAKAO -> {
                    runCatching { kakaoIdTokenProvider.getKakaoAuthPayload().idToken }
                        .fold(
                            onSuccess = { token ->
                                deleteAccountUseCase.invoke(
                                    DeleteAccountRequest(
                                        provider = AuthProvider.KAKAO,
                                        idToken = token
                                    )
                                )
                            },
                            onFailure = {
                                AppResult.Failure(
                                    AppError.ValidationFailed("kakao reauth failed")
                                )
                            }
                        )
                }
                AuthProvider.APPLE -> {
                    AppResult.Failure(
                        AppError.ValidationFailed("apple delete not supported on compose")
                    )
                }
            }
            when (result) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            isDeleteRequested = true,
                            isDeleteDialogVisible = false,
                            deletePassword = ""
                        )
                    }
                    _event.emit(AccountSettingsEvent.NavigateToMain)
                }

                is AppResult.Failure -> {
                    val message = mapDeleteFailureMessage(result)

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            deletePasswordErrorMessage = message
                        )
                    }
                }
            }
        }
    }

    private fun emitMessage(message: String) {
        viewModelScope.launch {
            _event.emit(AccountSettingsEvent.ShowMessage(message))
        }
    }

    private fun emitMessage(message: StringResource) {
        viewModelScope.launch {
            _event.emit(AccountSettingsEvent.ShowMessage(getString(message)))
        }
    }

    private suspend fun mapDeleteFailureMessage(failure: AppResult.Failure): String {
        val rawError = failure.error.toString()
        val normalized = rawError.uppercase()

        return if (normalized.contains("INVALID PASSWORD")
            || normalized.contains("INVALID_LOGIN_CREDENTIALS")
            || normalized.contains("INVALID_PASSWORD")
            || normalized.contains("EMAIL_NOT_FOUND")
        ) {
            getString(Res.string.account_settings_error_delete_invalid_password)
        } else if (normalized.contains("GOOGLE REAUTH FAILED")) {
            getString(Res.string.account_settings_error_delete_google_reauth_failed)
        } else if (normalized.contains("KAKAO REAUTH FAILED")) {
            getString(Res.string.account_settings_error_delete_kakao_reauth_failed)
        } else if (normalized.contains("APPLE DELETE NOT SUPPORTED")) {
            getString(Res.string.account_settings_error_delete_apple_ios_only)
        } else {
            getString(Res.string.account_settings_error_delete_failed)
        }
    }

    private suspend fun mapProfileUpdateFailureMessage(failure: AppResult.Failure): String {
        val rawError = failure.error.toString().uppercase()

        return if (rawError.contains("UNAUTHORIZED")) {
            getString(Res.string.account_settings_error_session_expired)
        } else if (rawError.contains("VALIDATIONFAILED")) {
            getString(Res.string.account_settings_message_enter_nickname)
        } else {
            getString(Res.string.account_settings_error_profile_save_failed)
        }
    }

    fun onAction(action: AccountSettingsAction) {
        when (action) {
            AccountSettingsAction.ClickBack -> clickBack()
            is AccountSettingsAction.ChangeNickname -> _uiState.update { it.copy(nicknameInput = action.value) }
            AccountSettingsAction.ClickSaveUserInfo -> clickSaveUserInfo()
            AccountSettingsAction.ClickOpenCastEdit -> clickOpenCastEdit()
            AccountSettingsAction.ClickOpenChangePassword -> clickOpenChangePassword()
            AccountSettingsAction.ClickShowDeleteDialog -> clickShowDeleteDialog()
            AccountSettingsAction.ClickDismissDeleteDialog -> clickDismissDeleteDialog()
            is AccountSettingsAction.ChangeDeletePassword -> _uiState.update {
                it.copy(
                    deletePassword = action.value,
                    deletePasswordErrorMessage = null
                )
            }
            AccountSettingsAction.ClickDeleteAccount -> clickDeleteAccount()
        }
    }

    override fun onCleared() {
        jobs.values.forEach(Job::cancel)
        jobs.clear()
        super.onCleared()
    }

    init {
        loadAccountSettings()
        observeSession()
    }

    private enum class TaskKey {
        ObserveSession,
        LoadAccountSettings,
        SaveUserInfo,
        DeleteAccount
    }
}
