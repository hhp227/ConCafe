package com.hhp227.concafe.presentation.settings.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

class AccountSettingsViewModel(
    private val getMyInfoUseCase: GetMyInfoUseCase,
    private val observeCurrentUserUseCase: ObserveCurrentUserUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(AccountSettingsUiState.empty())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<AccountSettingsEvent>(replay = 0)
    val event = _event.asSharedFlow()

    private fun observeSession() {
        viewModelScope.launch {
            observeCurrentUserUseCase.invoke().collectLatest {
                loadAccountSettings()
            }
        }
    }

    private fun loadAccountSettings() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
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
        when {
            state.nicknameInput.isBlank() -> emitMessage("닉네임을 입력해 주세요.")
            else -> emitMessage("계정 기본 정보를 저장했어요. 현재 단계에서는 로컬 상태에 반영됩니다.")
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
                deletePassword = ""
            )
        }
    }

    private fun clickDismissDeleteDialog() {
        _uiState.update {
            it.copy(
                isDeleteDialogVisible = false,
                deletePassword = ""
            )
        }
    }

    private fun clickDeleteAccount() {
        val state = _uiState.value

        if (state.deletePassword.isBlank()) {
            emitMessage("회원 비밀번호를 입력해 주세요.")
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
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
                    _event.emit(AccountSettingsEvent.NavigateBack)
                }

                is AppResult.Failure -> {
                    val message = mapDeleteFailureMessage(result)

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = message
                        )
                    }
                    _event.emit(AccountSettingsEvent.ShowMessage(message))
                }
            }
        }
    }

    private fun emitMessage(message: String) {
        viewModelScope.launch {
            _event.emit(AccountSettingsEvent.ShowMessage(message))
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
                it.copy(deletePassword = action.value)
            }
            AccountSettingsAction.ClickDeleteAccount -> clickDeleteAccount()
        }
    }

    init {
        loadAccountSettings()
        observeSession()
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
}
