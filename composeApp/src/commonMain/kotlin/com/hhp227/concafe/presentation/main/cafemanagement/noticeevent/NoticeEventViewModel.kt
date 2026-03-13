package com.hhp227.concafe.presentation.main.cafemanagement.noticeevent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NoticeEventViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(NoticeEventUiState())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<NoticeEventEvent>(replay = 0)
    val event = _event.asSharedFlow()

    private fun setInfoMessage(message: String) {
        _uiState.update { it.copy(infoMessage = message) }
    }

    private fun submitForm() {
        val state = _uiState.value
        if (!state.isFormSubmitEnabled) return

        if (state.selectedTab == NoticeEventTab.NOTICE) {
            val nextNotice = NoticeItem(
                id = "notice-${state.notices.size + 1}",
                title = state.formTitle.trim(),
                date = "2026.03.13",
                isPinned = state.formPinned,
                statusLabel = if (state.formReservedAt.isBlank()) "게시 중" else "임시 저장",
                statusAccent = if (state.formReservedAt.isBlank()) NoticeStatusAccent.PUBLISHED else NoticeStatusAccent.DRAFT
            )

            _uiState.update {
                it.copy(
                    notices = listOf(nextNotice) + it.notices,
                    isFormSheetVisible = false,
                    formTitle = "",
                    formContent = "",
                    formImageUrl = "",
                    formPinned = false,
                    formReservedAt = "",
                    infoMessage = "공지사항이 목록에 추가되었습니다."
                )
            }
        } else {
            val nextEvent = EventItem(
                id = "event-${state.events.size + 1}",
                title = state.formTitle.trim(),
                period = state.formReservedAt.ifBlank { "게시 일정 선택 필요" },
                statusLabel = if (state.formReservedAt.isBlank()) "진행 예정" else "진행 중",
                imageUrl = state.formImageUrl,
                isDimmed = false
            )

            _uiState.update {
                it.copy(
                    events = listOf(nextEvent) + it.events,
                    selectedTab = NoticeEventTab.EVENT,
                    isFormSheetVisible = false,
                    formTitle = "",
                    formContent = "",
                    formImageUrl = "",
                    formPinned = false,
                    formReservedAt = "",
                    infoMessage = "이벤트가 목록에 추가되었습니다."
                )
            }
        }
    }

    fun onAction(action: NoticeEventAction) {
        when (action) {
            NoticeEventAction.ClickBack -> viewModelScope.launch {
                _event.emit(NoticeEventEvent.NavigateBack)
            }
            is NoticeEventAction.SelectTab -> _uiState.update { it.copy(selectedTab = action.tab) }
            is NoticeEventAction.ChangeQuery -> _uiState.update { it.copy(query = action.value) }
            NoticeEventAction.ClickRegister -> _uiState.update {
                it.copy(
                    isFormSheetVisible = true,
                    formTitle = "",
                    formContent = "",
                    formImageUrl = "",
                    formPinned = false,
                    formReservedAt = "",
                    infoMessage = null
                )
            }
            NoticeEventAction.ClickMoreEvents -> setInfoMessage("이벤트 전체 목록 연결은 다음 단계에서 이어집니다.")
            is NoticeEventAction.ClickEditNotice -> setInfoMessage("편집 기능은 다음 단계에서 연결됩니다.")
            is NoticeEventAction.ClickDeleteNotice -> setInfoMessage("삭제 기능은 다음 단계에서 연결됩니다.")
            is NoticeEventAction.ClickEventMenu -> setInfoMessage("이벤트 상세 메뉴는 다음 단계에서 연결됩니다.")
            NoticeEventAction.DismissFormSheet -> _uiState.update { it.copy(isFormSheetVisible = false) }
            is NoticeEventAction.ChangeFormTitle -> _uiState.update { it.copy(formTitle = action.value) }
            is NoticeEventAction.ChangeFormContent -> _uiState.update { it.copy(formContent = action.value) }
            NoticeEventAction.ClickFormImage -> _uiState.update {
                if (it.formImageUrl.isNotBlank()) {
                    it.copy(infoMessage = "이미지는 한 장만 첨부할 수 있습니다.")
                } else {
                    it.copy(formImageUrl = SAMPLE_EVENT_IMAGE_URL, infoMessage = null)
                }
            }
            NoticeEventAction.ClickRemoveFormImage -> _uiState.update { it.copy(formImageUrl = "", infoMessage = null) }
            is NoticeEventAction.ChangeFormPinned -> _uiState.update { it.copy(formPinned = action.value) }
            NoticeEventAction.ClickReserveSchedule -> setInfoMessage("게시 예약 기능은 다음 단계에서 연결됩니다.")
            NoticeEventAction.ClickSubmitForm -> submitForm()
            NoticeEventAction.DismissInfoMessage -> _uiState.update { it.copy(infoMessage = null) }
        }
    }
}
