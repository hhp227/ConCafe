package com.hhp227.concafe.presentation.main.cafemanagement.noticeevent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.CafeEventManagementItem
import com.hhp227.concafe.domain.model.CafeNoticeManagementItem
import com.hhp227.concafe.domain.model.NoticeStatusAccent as DomainNoticeStatusAccent
import com.hhp227.concafe.domain.usecase.GetCafeEventPageUseCase
import com.hhp227.concafe.domain.usecase.GetCafeNoticePageUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NoticeEventViewModel(
    private val cafeId: String,
    private val getCafeNoticePageUseCase: GetCafeNoticePageUseCase,
    private val getCafeEventPageUseCase: GetCafeEventPageUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(NoticeEventUiState())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<NoticeEventEvent>(replay = 0)
    val event = _event.asSharedFlow()

    private val jobs = mutableMapOf<JobKey, Job>()

    private fun setInfoMessage(message: String) {
        _uiState.update { it.copy(infoMessage = message) }
    }

    private fun loadNoticePage(cursor: String?, append: Boolean) {
        jobs[JobKey.NOTICE_PAGE]?.cancel()
        jobs[JobKey.NOTICE_PAGE] = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingNotices = !append,
                    isLoadingMoreNotices = append,
                    infoMessage = if (append) it.infoMessage else null
                )
            }

            when (val result = getCafeNoticePageUseCase.invoke(cafeId = cafeId, query = _uiState.value.query, cursor = cursor)) {
                is AppResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            notices = if (append) state.notices + result.data.items.map(::mapNotice) else result.data.items.map(::mapNotice),
                            noticeNextCursor = result.data.nextCursor,
                            canLoadMoreNotices = result.data.hasNext,
                            isLoadingNotices = false,
                            isLoadingMoreNotices = false
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoadingNotices = false,
                            isLoadingMoreNotices = false,
                            infoMessage = "공지사항을 불러오지 못했습니다."
                        )
                    }
                }
            }
        }
    }

    private fun loadEventPage(cursor: String?, append: Boolean) {
        jobs[JobKey.EVENT_PAGE]?.cancel()
        jobs[JobKey.EVENT_PAGE] = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingEvents = !append,
                    isLoadingMoreEvents = append,
                    infoMessage = if (append) it.infoMessage else null
                )
            }

            when (val result = getCafeEventPageUseCase.invoke(cafeId = cafeId, query = _uiState.value.query, cursor = cursor)) {
                is AppResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            events = if (append) state.events + result.data.items.map(::mapEvent) else result.data.items.map(::mapEvent),
                            eventNextCursor = result.data.nextCursor,
                            canLoadMoreEvents = result.data.hasNext,
                            isLoadingEvents = false,
                            isLoadingMoreEvents = false
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoadingEvents = false,
                            isLoadingMoreEvents = false,
                            infoMessage = "이벤트를 불러오지 못했습니다."
                        )
                    }
                }
            }
        }
    }

    private fun refreshCurrentTab() {
        when (_uiState.value.selectedTab) {
            NoticeEventTab.NOTICE -> loadNoticePage(cursor = null, append = false)
            NoticeEventTab.EVENT -> loadEventPage(cursor = null, append = false)
        }
    }

    private fun loadMoreNotices() {
        val state = _uiState.value
        val cursor = state.noticeNextCursor ?: return
        if (state.isLoadingNotices || state.isLoadingMoreNotices || !state.canLoadMoreNotices) return
        loadNoticePage(cursor = cursor, append = true)
    }

    private fun loadMoreEvents() {
        val state = _uiState.value
        val cursor = state.eventNextCursor ?: return
        if (state.isLoadingEvents || state.isLoadingMoreEvents || !state.canLoadMoreEvents) return
        loadEventPage(cursor = cursor, append = true)
    }

    private fun submitForm() {
        val state = _uiState.value
        if (!state.isFormSubmitEnabled) return

        if (state.selectedTab == NoticeEventTab.NOTICE) {
            val nextNotice = NoticeItem(
                id = "local-notice-${state.notices.size + 1}",
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
                id = "local-event-${state.events.size + 1}",
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
            is NoticeEventAction.SelectTab -> {
                _uiState.update { it.copy(selectedTab = action.tab) }
                when (action.tab) {
                    NoticeEventTab.NOTICE -> if (_uiState.value.notices.isEmpty()) loadNoticePage(cursor = null, append = false)
                    NoticeEventTab.EVENT -> if (_uiState.value.events.isEmpty()) loadEventPage(cursor = null, append = false)
                }
            }
            is NoticeEventAction.ChangeQuery -> {
                _uiState.update { it.copy(query = action.value) }
                refreshCurrentTab()
            }
            NoticeEventAction.LoadMoreNotices -> loadMoreNotices()
            NoticeEventAction.LoadMoreEvents -> loadMoreEvents()
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

    private fun mapNotice(item: CafeNoticeManagementItem): NoticeItem {
        return NoticeItem(
            id = item.id,
            title = item.title,
            date = item.displayDate,
            isPinned = item.isPinned,
            statusLabel = item.statusLabel,
            statusAccent = when (item.statusAccent) {
                DomainNoticeStatusAccent.PUBLISHED -> NoticeStatusAccent.PUBLISHED
                DomainNoticeStatusAccent.DRAFT -> NoticeStatusAccent.DRAFT
                DomainNoticeStatusAccent.ENDED -> NoticeStatusAccent.ENDED
            }
        )
    }

    private fun mapEvent(item: CafeEventManagementItem): EventItem {
        return EventItem(
            id = item.id,
            title = item.title,
            period = item.periodText,
            statusLabel = item.statusLabel,
            imageUrl = item.imageUrl,
            isDimmed = item.isDimmed
        )
    }

    override fun onCleared() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        super.onCleared()
    }

    init {
        loadNoticePage(cursor = null, append = false)
    }

    private enum class JobKey {
        NOTICE_PAGE,
        EVENT_PAGE
    }
}
