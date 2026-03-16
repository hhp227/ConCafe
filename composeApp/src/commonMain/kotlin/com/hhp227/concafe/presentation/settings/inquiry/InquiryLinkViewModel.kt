package com.hhp227.concafe.presentation.settings.inquiry

import com.hhp227.concafe.di.resolveCreateInquiryUseCase
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.InquiryCreate
import com.hhp227.concafe.domain.usecase.CreateInquiryUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class InquiryLinkViewModel : ViewModel() {
    private val createInquiryUseCase: CreateInquiryUseCase = resolveCreateInquiryUseCase()

    private val _uiState = MutableStateFlow(InquiryLinkUiState.empty())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<InquiryLinkEvent>(replay = 0)
    val event = _event.asSharedFlow()

    fun onAction(action: InquiryLinkAction) {
        when (action) {
            InquiryLinkAction.ClickBack -> emitNavigateBack()
            is InquiryLinkAction.ChangeInquiryType -> {
                _uiState.update { it.copy(inquiryType = action.value, errorMessage = null) }
            }
            is InquiryLinkAction.ChangeTitle -> {
                _uiState.update { it.copy(title = action.value, errorMessage = null) }
            }
            is InquiryLinkAction.ChangeMessage -> {
                _uiState.update { it.copy(message = action.value, errorMessage = null) }
            }
            InquiryLinkAction.ClickSubmit -> submitInquiry()
        }
    }

    private fun emitNavigateBack() {
        viewModelScope.launch {
            _event.emit(InquiryLinkEvent.NavigateBack)
        }
    }

    private fun submitInquiry() {
        val currentState = _uiState.value
        val errorMessage = validate(currentState)

        if (errorMessage != null) {
            _uiState.update { it.copy(errorMessage = errorMessage) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            when (
                val result = createInquiryUseCase(
                    InquiryCreate(
                        inquiryType = currentState.inquiryType.title,
                        title = currentState.title,
                        content = currentState.message
                    )
                )
            ) {
                is AppResult.Success -> {
                    _uiState.update {
                        InquiryLinkUiState(
                            inquiryType = currentState.inquiryType
                        )
                    }
                    _event.emit(InquiryLinkEvent.ShowMessage(SUBMIT_SUCCESS_MESSAGE))
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = result.error.toUserMessage()
                        )
                    }
                }
            }
        }
    }

    private fun validate(state: InquiryLinkUiState): String? {
        return when {
            state.title.isBlank() -> "문의 제목을 입력해 주세요."
            state.message.isBlank() -> "문의 내용을 입력해 주세요."
            else -> null
        }
    }

    private companion object {
        const val SUBMIT_SUCCESS_MESSAGE = "문의가 접수되었습니다. 검토 후 안내드릴게요."
    }
}

private fun com.hhp227.concafe.domain.common.AppError.toUserMessage(): String {
    return when (this) {
        is com.hhp227.concafe.domain.common.AppError.ValidationFailed -> reason
        com.hhp227.concafe.domain.common.AppError.Unauthorized -> "로그인 후 문의를 접수해 주세요."
        com.hhp227.concafe.domain.common.AppError.PermissionDenied -> "문의 작성 권한이 없습니다."
        com.hhp227.concafe.domain.common.AppError.NotFound -> "문의 저장 대상을 찾지 못했습니다."
        is com.hhp227.concafe.domain.common.AppError.NetworkError -> message ?: "네트워크 오류가 발생했습니다."
        is com.hhp227.concafe.domain.common.AppError.Unknown -> cause ?: "문의 접수에 실패했습니다."
    }
}
