package com.hhp227.concafe.presentation.settings.changepassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChangePasswordViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ChangePasswordUiState.empty())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<ChangePasswordEvent>(replay = 0)
    val event = _event.asSharedFlow()

    private fun emitNavigateBack() {
        viewModelScope.launch {
            _event.emit(ChangePasswordEvent.NavigateBack)
        }
    }

    private fun submit() {
        val state = _uiState.value
        val message = when {
            state.currentPassword.isBlank() -> "현재 비밀번호를 입력해 주세요."
            state.newPassword.length < MIN_PASSWORD_LENGTH -> "새 비밀번호는 8자 이상이어야 합니다."
            state.newPassword != state.confirmPassword -> "새 비밀번호 확인이 일치하지 않습니다."
            else -> {
                _uiState.value = ChangePasswordUiState.empty()
                "비밀번호 변경 요청을 처리했어요. 현재 단계에서는 확인 피드백만 제공됩니다."
            }
        }
        viewModelScope.launch {
            _event.emit(ChangePasswordEvent.ShowMessage(message))
        }
    }

    fun onAction(action: ChangePasswordAction) {
        when (action) {
            ChangePasswordAction.ClickBack -> emitNavigateBack()
            is ChangePasswordAction.ChangeCurrentPassword -> {
                _uiState.update { it.copy(currentPassword = action.value) }
            }
            is ChangePasswordAction.ChangeNewPassword -> {
                _uiState.update { it.copy(newPassword = action.value) }
            }
            is ChangePasswordAction.ChangeConfirmPassword -> {
                _uiState.update { it.copy(confirmPassword = action.value) }
            }
            ChangePasswordAction.ClickSubmit -> submit()
        }
    }

    private companion object {
        private const val MIN_PASSWORD_LENGTH = 8
    }
}
