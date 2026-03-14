package com.hhp227.concafe.presentation.banner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BannerEditViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(BannerEditUiState())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<BannerEditEvent>(replay = 0)
    val event = _event.asSharedFlow()

    private fun clickBack() {
        viewModelScope.launch {
            _event.emit(BannerEditEvent.NavigateBack)
        }
    }

    private fun clickImagePicker() {
        _uiState.update {
            it.copy(
                selectedImageLabel = "banner_cover_mock.png",
                infoMessage = "이미지 업로드 연결은 다음 단계에서 구현됩니다."
            )
        }
    }

    private fun clickSave() {
        val currentState = _uiState.value

        val validationMessage = when {
            currentState.title.isBlank() -> "배너 제목을 입력해주세요."
            currentState.subtitle.isBlank() -> "서브 문구를 입력해주세요."
            currentState.targetValue.isBlank() -> "연결 대상 값을 입력해주세요."
            else -> null
        }

        if (validationMessage != null) {
            _uiState.update { it.copy(infoMessage = validationMessage) }
            return
        }

        _uiState.update {
            it.copy(
                isSaving = true,
                infoMessage = null
            )
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSaving = false,
                    infoMessage = "배너 초안이 저장되었습니다. 실제 업로드 연동은 다음 단계에서 연결됩니다."
                )
            }
            _event.emit(BannerEditEvent.ShowSaveSuccessMessage)
        }
    }

    fun onAction(action: BannerEditAction) {
        when (action) {
            BannerEditAction.ClickBack -> clickBack()
            BannerEditAction.ClickImagePicker -> clickImagePicker()
            is BannerEditAction.ChangeTitle -> _uiState.update { it.copy(title = action.value) }
            is BannerEditAction.ChangeSubtitle -> _uiState.update { it.copy(subtitle = action.value) }
            is BannerEditAction.SelectTarget -> _uiState.update {
                it.copy(
                    selectedTarget = action.target,
                    targetValue = ""
                )
            }
            is BannerEditAction.ChangeTargetValue -> _uiState.update { it.copy(targetValue = action.value) }
            is BannerEditAction.ChangeDisplayDays -> _uiState.update {
                it.copy(displayDays = action.value.coerceIn(1, 10))
            }
            BannerEditAction.ClickSave -> clickSave()
            BannerEditAction.DismissInfoMessage -> _uiState.update { it.copy(infoMessage = null) }
        }
    }
}
