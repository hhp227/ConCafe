package com.hhp227.concafe.presentation.settings.account

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
import com.hhp227.concafe.domain.usecase.DeleteAccountUseCase
import com.hhp227.concafe.domain.usecase.GetMyInfoUseCase
import com.hhp227.concafe.domain.usecase.ObserveCurrentUserUseCase
import com.hhp227.concafe.domain.usecase.UpdateUserProfileUseCase

class AccountSettingsViewModel(
    private val getMyInfoUseCase: GetMyInfoUseCase,
    private val observeCurrentUserUseCase: ObserveCurrentUserUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase
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
                            errorMessage = "계정 정보를 불러오지 못했습니다."
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
            emitMessage("닉네임을 입력해 주세요.")
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
                        emitMessage("계정 기본 정보를 원격 데이터에 저장했어요.")
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
            emitMessage("연결된 캐스트 프로필이 아직 없습니다.")
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

        if (state.deletePassword.isBlank()) {
            _uiState.update {
                it.copy(
                    deletePasswordErrorMessage = "회원 비밀번호를 입력해 주세요."
                )
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        jobs[TaskKey.DeleteAccount]?.cancel()
        jobs[TaskKey.DeleteAccount] = viewModelScope.launch {
            when (val result = deleteAccountUseCase.invoke(state.deletePassword)) {
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

    private fun mapDeleteFailureMessage(failure: AppResult.Failure): String {
        val rawError = failure.error.toString()
        val normalized = rawError.uppercase()

        return if (normalized.contains("INVALID PASSWORD")
            || normalized.contains("INVALID_LOGIN_CREDENTIALS")
            || normalized.contains("INVALID_PASSWORD")
            || normalized.contains("EMAIL_NOT_FOUND")
        ) {
            "비밀번호가 올바르지 않습니다."
        } else {
            "회원탈퇴에 실패했습니다. 다시 시도해 주세요."
        }
    }

    private fun mapProfileUpdateFailureMessage(failure: AppResult.Failure): String {
        val rawError = failure.error.toString().uppercase()

        return if (rawError.contains("UNAUTHORIZED")) {
            "로그인이 만료되었습니다. 다시 로그인해 주세요."
        } else if (rawError.contains("VALIDATIONFAILED")) {
            "닉네임을 입력해 주세요."
        } else {
            "프로필 저장에 실패했습니다. 잠시 후 다시 시도해 주세요."
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
