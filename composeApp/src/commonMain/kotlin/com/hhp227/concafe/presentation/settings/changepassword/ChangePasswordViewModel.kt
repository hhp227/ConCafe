package com.hhp227.concafe.presentation.settings.changepassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.usecase.ChangePasswordUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChangePasswordViewModel(
    private val changePasswordUseCase: ChangePasswordUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChangePasswordUiState.empty())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<ChangePasswordEvent>(replay = 0)
    val event = _event.asSharedFlow()

    private fun emitNavigateBack() {
        viewModelScope.launch {
            _event.emit(ChangePasswordEvent.NavigateBack)
        }
    }

    private fun changeCurrentPassword(value: String) {
        _uiState.update {
            it.copy(currentPassword = value)
        }
    }

    private fun changeNewPassword(value: String) {
        _uiState.update {
            it.copy(newPassword = value)
        }
    }

    private fun changeConfirmPassword(value: String) {
        _uiState.update {
            it.copy(confirmPassword = value)
        }
    }

    private fun submit() {
        val state = _uiState.value
        val currentPassword = state.currentPassword
        val newPassword = state.newPassword
        val confirmPassword = state.confirmPassword
        val validationMessage = validate(
            currentPassword = currentPassword,
            newPassword = newPassword,
            confirmPassword = confirmPassword
        )

        if (validationMessage == null) {
            _uiState.update {
                it.copy(isSubmitting = true)
            }

            viewModelScope.launch {
                val result = changePasswordUseCase.invoke(
                    currentPassword = currentPassword,
                    newPassword = newPassword
                )

                when (result) {
                    is AppResult.Success -> {
                        _uiState.value = ChangePasswordUiState.empty()
                        _event.emit(ChangePasswordEvent.ShowMessage("비밀번호가 안전하게 변경되었습니다."))
                    }
                    is AppResult.Failure -> {
                        _uiState.update {
                            it.copy(isSubmitting = false)
                        }
                        _event.emit(ChangePasswordEvent.ShowMessage(mapFailureMessage(result)))
                    }
                }
            }
        } else {
            viewModelScope.launch {
                _event.emit(ChangePasswordEvent.ShowMessage(validationMessage))
            }
        }
    }

    private fun validate(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String
    ): String? {
        return if (currentPassword.isBlank()) {
            "현재 비밀번호를 입력해 주세요."
        } else if (newPassword.length < MIN_PASSWORD_LENGTH) {
            "새 비밀번호는 8자 이상이어야 합니다."
        } else if (newPassword != confirmPassword) {
            "새 비밀번호 확인이 일치하지 않습니다."
        } else if (currentPassword == newPassword) {
            "현재 비밀번호와 다른 새 비밀번호를 입력해 주세요."
        } else {
            null
        }
    }

    private fun mapFailureMessage(failure: AppResult.Failure): String {
        val rawError = failure.error.toString().uppercase()
        return if (rawError.contains("INVALID PASSWORD")
            || rawError.contains("INVALID_LOGIN_CREDENTIALS")
            || rawError.contains("INVALID_PASSWORD")
            || rawError.contains("EMAIL_NOT_FOUND")
            || rawError.contains("PASSWORD DOES NOT MATCH CURRENT USER")
        ) {
            "현재 비밀번호가 올바르지 않습니다."
        } else if (rawError.contains("REQUIRES_RECENT_LOGIN")) {
            "보안을 위해 다시 로그인한 뒤 비밀번호를 변경해 주세요."
        } else {
            "비밀번호 변경에 실패했습니다. 잠시 후 다시 시도해 주세요."
        }
    }

    fun onAction(action: ChangePasswordAction) {
        when (action) {
            ChangePasswordAction.ClickBack -> emitNavigateBack()
            is ChangePasswordAction.ChangeCurrentPassword -> changeCurrentPassword(action.value)
            is ChangePasswordAction.ChangeNewPassword -> changeNewPassword(action.value)
            is ChangePasswordAction.ChangeConfirmPassword -> changeConfirmPassword(action.value)
            ChangePasswordAction.ClickSubmit -> {
                if (_uiState.value.isSubmitting) {
                    Unit
                } else {
                    submit()
                }
            }
        }
    }

    private companion object {
        private const val MIN_PASSWORD_LENGTH = 8
    }
}
