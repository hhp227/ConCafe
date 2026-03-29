package com.hhp227.concafe.presentation.auth.resetpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.usecase.RequestPasswordResetUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ResetPasswordViewModel(
    private val requestPasswordResetUseCase: RequestPasswordResetUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(ResetPasswordUiState.empty())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<ResetPasswordEvent>(replay = 0)
    val event = _event.asSharedFlow()

    private fun emitNavigateBack() {
        viewModelScope.launch {
            _event.emit(ResetPasswordEvent.NavigateBack)
        }
    }

    private fun changeEmail(value: String) {
        _uiState.update {
            it.copy(email = value)
        }
    }

    private fun submit() {
        val state = _uiState.value
        val email = state.email.trim()
        val validationMessage = validate(email)

        if (validationMessage == null) {
            _uiState.update {
                it.copy(isSubmitting = true)
            }

            viewModelScope.launch {
                val result = requestPasswordResetUseCase.invoke(email)

                if (result is AppResult.Success) {
                    _uiState.value = ResetPasswordUiState.empty()
                    _event.emit(ResetPasswordEvent.ShowMessage("비밀번호 재설정 메일을 발송했습니다."))
                } else if (result is AppResult.Failure) {
                    _uiState.update {
                        it.copy(isSubmitting = false)
                    }
                    _event.emit(ResetPasswordEvent.ShowMessage(mapFailureMessage(result)))
                } else {
                    _uiState.update {
                        it.copy(isSubmitting = false)
                    }
                    _event.emit(ResetPasswordEvent.ShowMessage("재설정 메일 발송에 실패했습니다. 잠시 후 다시 시도해 주세요."))
                }
            }
        } else {
            viewModelScope.launch {
                _event.emit(ResetPasswordEvent.ShowMessage(validationMessage))
            }
        }
    }

    private fun validate(email: String): String? {
        val isEmailPatternValid = EMAIL_REGEX.matches(email)
        return if (email.isBlank()) {
            "이메일을 입력해 주세요."
        } else if (!isEmailPatternValid) {
            "올바른 이메일 형식을 입력해 주세요."
        } else {
            null
        }
    }

    private fun mapFailureMessage(failure: AppResult.Failure): String {
        val rawError = failure.error.toString().uppercase()
        return if (rawError.contains("EMAIL_NOT_FOUND")) {
            "등록되지 않은 이메일입니다."
        } else if (rawError.contains("TOO_MANY_ATTEMPTS_TRY_LATER")) {
            "요청이 많습니다. 잠시 후 다시 시도해 주세요."
        } else {
            "재설정 메일 발송에 실패했습니다. 잠시 후 다시 시도해 주세요."
        }
    }

    fun onAction(action: ResetPasswordAction) {
        when (action) {
            ResetPasswordAction.ClickBack -> emitNavigateBack()
            is ResetPasswordAction.ChangeEmail -> changeEmail(action.value)
            ResetPasswordAction.ClickSubmit -> {
                if (_uiState.value.isSubmitting) {
                    Unit
                } else {
                    submit()
                }
            }
        }
    }

    private companion object {
        private val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()
    }
}
