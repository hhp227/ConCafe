package com.hhp227.concafe.presentation.main.cafemanagement.noticeevent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.CafeEventCreate
import com.hhp227.concafe.domain.model.CafeEventManagementItem
import com.hhp227.concafe.domain.model.CafeEventUpdate
import com.hhp227.concafe.domain.model.CafeNoticeCreate
import com.hhp227.concafe.domain.model.CafeNoticeManagementItem
import com.hhp227.concafe.domain.model.CafeNoticeUpdate
import com.hhp227.concafe.domain.event.NoticeManagementEvent
import com.hhp227.concafe.domain.event.publisher.NoticeManagementEventPublisher
import com.hhp227.concafe.domain.model.NoticeStatusAccent as DomainNoticeStatusAccent
import com.hhp227.concafe.domain.usecase.CreateCafeEventUseCase
import com.hhp227.concafe.domain.usecase.CreateCafeNoticeUseCase
import com.hhp227.concafe.domain.usecase.DeleteCafeEventUseCase
import com.hhp227.concafe.domain.usecase.DeleteCafeNoticeUseCase
import com.hhp227.concafe.domain.usecase.GetCafeEventPageUseCase
import com.hhp227.concafe.domain.usecase.GetCafeNoticePageUseCase
import com.hhp227.concafe.domain.usecase.UpdateCafeEventUseCase
import com.hhp227.concafe.domain.usecase.UpdateCafeNoticeUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NoticeEventViewModel(
    private val cafeId: String,
    private val getCafeNoticePageUseCase: GetCafeNoticePageUseCase,
    private val getCafeEventPageUseCase: GetCafeEventPageUseCase,
    private val createCafeNoticeUseCase: CreateCafeNoticeUseCase,
    private val createCafeEventUseCase: CreateCafeEventUseCase,
    private val updateCafeNoticeUseCase: UpdateCafeNoticeUseCase,
    private val updateCafeEventUseCase: UpdateCafeEventUseCase,
    private val deleteCafeNoticeUseCase: DeleteCafeNoticeUseCase,
    private val deleteCafeEventUseCase: DeleteCafeEventUseCase,
    private val noticeManagementEventPublisher: NoticeManagementEventPublisher
) : ViewModel() {
    private val _uiState = MutableStateFlow(NoticeEventUiState())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<NoticeEventEvent>(replay = 0)
    val event = _event.asSharedFlow()

    private val jobs = mutableMapOf<JobKey, Job>()

    private fun setInfoMessage(message: String) {
        _uiState.update { it.copy(infoMessage = message) }
    }

    private fun openCreateForm() {
        _uiState.update {
            it.copy(
                isFormSheetVisible = true,
                isSubmittingForm = false,
                formEditingId = null,
                formTitle = "",
                formContent = "",
                formImageUrl = "",
                formPinned = false,
                formReservedAt = "",
                infoMessage = null
            )
        }
    }

    private fun openEditNoticeForm(id: String) {
        val target = _uiState.value.notices.firstOrNull { it.id == id } ?: run {
            setInfoMessage("수정할 공지사항을 찾지 못했습니다.")
            return
        }
        _uiState.update {
            it.copy(
                selectedTab = NoticeEventTab.NOTICE,
                isFormSheetVisible = true,
                isSubmittingForm = false,
                formEditingId = target.id,
                formTitle = target.title,
                formContent = target.content,
                formImageUrl = "",
                formPinned = target.isPinned,
                formReservedAt = if (target.statusAccent == NoticeStatusAccent.DRAFT) target.date else "",
                infoMessage = null
            )
        }
    }

    private fun openEditEventForm(id: String) {
        val target = _uiState.value.events.firstOrNull { it.id == id } ?: run {
            setInfoMessage("수정할 이벤트를 찾지 못했습니다.")
            return
        }
        _uiState.update {
            it.copy(
                selectedTab = NoticeEventTab.EVENT,
                isFormSheetVisible = true,
                isSubmittingForm = false,
                formEditingId = target.id,
                formTitle = target.title,
                formContent = target.content,
                formImageUrl = target.imageUrl,
                formPinned = false,
                formReservedAt = target.period,
                infoMessage = null
            )
        }
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
        jobs[JobKey.SUBMIT]?.cancel()
        jobs[JobKey.SUBMIT] = viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingForm = true, infoMessage = null) }

            val result = if (state.selectedTab == NoticeEventTab.NOTICE) {
                if (state.formEditingId == null) {
                    createCafeNoticeUseCase.invoke(
                        CafeNoticeCreate(
                            cafeId = cafeId,
                            title = state.formTitle,
                            content = state.formContent,
                            isPinned = state.formPinned,
                            reservedAt = state.formReservedAt.ifBlank { null }
                        )
                    )
                } else {
                    updateCafeNoticeUseCase.invoke(
                        CafeNoticeUpdate(
                            cafeId = cafeId,
                            noticeId = state.formEditingId,
                            title = state.formTitle,
                            content = state.formContent,
                            isPinned = state.formPinned,
                            reservedAt = state.formReservedAt.ifBlank { null }
                        )
                    )
                }
            } else {
                if (state.formEditingId == null) {
                    createCafeEventUseCase.invoke(
                        CafeEventCreate(
                            cafeId = cafeId,
                            title = state.formTitle,
                            content = state.formContent,
                            imageUrl = state.formImageUrl,
                            periodText = state.formReservedAt.ifBlank { null }
                        )
                    )
                } else {
                    updateCafeEventUseCase.invoke(
                        CafeEventUpdate(
                            cafeId = cafeId,
                            eventId = state.formEditingId,
                            title = state.formTitle,
                            content = state.formContent,
                            imageUrl = state.formImageUrl,
                            periodText = state.formReservedAt.ifBlank { null }
                        )
                    )
                }
            }

            when (result) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isFormSheetVisible = false,
                            isSubmittingForm = false,
                            formEditingId = null,
                            formTitle = "",
                            formContent = "",
                            formImageUrl = "",
                            formPinned = false,
                            formReservedAt = "",
                            infoMessage = if (state.selectedTab == NoticeEventTab.NOTICE) {
                                if (state.formEditingId == null) "공지사항이 등록되었습니다." else "공지사항이 수정되었습니다."
                            } else {
                                if (state.formEditingId == null) "이벤트가 등록되었습니다." else "이벤트가 수정되었습니다."
                            }
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isSubmittingForm = false,
                            infoMessage = when (val error = result.error) {
                                is AppError.ValidationFailed -> error.reason.toNoticeEventValidationMessage()
                                else -> if (state.selectedTab == NoticeEventTab.NOTICE) {
                                    if (state.formEditingId == null) "공지사항 등록에 실패했습니다." else "공지사항 수정에 실패했습니다."
                                } else {
                                    if (state.formEditingId == null) "이벤트 등록에 실패했습니다." else "이벤트 수정에 실패했습니다."
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun deleteNotice(id: String) {
        jobs[JobKey.DELETE]?.cancel()
        jobs[JobKey.DELETE] = viewModelScope.launch {
            when (deleteCafeNoticeUseCase.invoke(cafeId = cafeId, noticeId = id)) {
                is AppResult.Success -> setInfoMessage("공지사항이 삭제되었습니다.")
                is AppResult.Failure -> setInfoMessage("공지사항 삭제에 실패했습니다.")
            }
        }
    }

    private fun deleteEvent(id: String) {
        jobs[JobKey.DELETE]?.cancel()
        jobs[JobKey.DELETE] = viewModelScope.launch {
            when (deleteCafeEventUseCase.invoke(cafeId = cafeId, eventId = id)) {
                is AppResult.Success -> setInfoMessage("이벤트가 삭제되었습니다.")
                is AppResult.Failure -> setInfoMessage("이벤트 삭제에 실패했습니다.")
            }
        }
    }

    private fun patchNotice(item: CafeNoticeManagementItem) {
        val mapped = mapNotice(item)
        _uiState.update { state ->
            state.copy(notices = state.notices.map { if (it.id == mapped.id) mapped else it })
        }
    }

    private fun patchEvent(item: CafeEventManagementItem) {
        val mapped = mapEvent(item)
        _uiState.update { state ->
            state.copy(events = state.events.map { if (it.id == mapped.id) mapped else it })
        }
    }

    private fun removeNotice(id: String) {
        _uiState.update { state ->
            state.copy(notices = state.notices.filterNot { it.id == id })
        }
    }

    private fun removeEvent(id: String) {
        _uiState.update { state ->
            state.copy(events = state.events.filterNot { it.id == id })
        }
    }

    private fun observeNoticeManagementEvent() {
        jobs[JobKey.OBSERVE_EVENT]?.cancel()
        jobs[JobKey.OBSERVE_EVENT] = viewModelScope.launch {
            noticeManagementEventPublisher.events.collectLatest { event ->
                when (event) {
                    is NoticeManagementEvent.NoticeCreated -> if (event.cafeId == cafeId) loadNoticePage(cursor = null, append = false)
                    is NoticeManagementEvent.NoticeUpdated -> if (event.cafeId == cafeId) patchNotice(event.notice)
                    is NoticeManagementEvent.NoticeDeleted -> if (event.cafeId == cafeId) removeNotice(event.noticeId)
                    is NoticeManagementEvent.EventCreated -> if (event.cafeId == cafeId) loadEventPage(cursor = null, append = false)
                    is NoticeManagementEvent.EventUpdated -> if (event.cafeId == cafeId) patchEvent(event.event)
                    is NoticeManagementEvent.EventDeleted -> if (event.cafeId == cafeId) removeEvent(event.eventId)
                }
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
            NoticeEventAction.ClickRegister -> openCreateForm()
            NoticeEventAction.ClickMoreEvents -> setInfoMessage("이벤트 전체 목록 연결은 다음 단계에서 이어집니다.")
            is NoticeEventAction.ClickEditNotice -> openEditNoticeForm(action.id)
            is NoticeEventAction.ClickDeleteNotice -> deleteNotice(action.id)
            is NoticeEventAction.ClickEditEvent -> openEditEventForm(action.id)
            is NoticeEventAction.ClickDeleteEvent -> deleteEvent(action.id)
            NoticeEventAction.DismissFormSheet -> _uiState.update { it.copy(isFormSheetVisible = false, formEditingId = null) }
            is NoticeEventAction.ChangeFormTitle -> _uiState.update { it.copy(formTitle = action.value) }
            is NoticeEventAction.ChangeFormContent -> _uiState.update { it.copy(formContent = action.value) }
            is NoticeEventAction.ChangeFormImageUrl -> _uiState.update {
                it.copy(formImageUrl = action.imageUrl, infoMessage = null)
            }
            NoticeEventAction.ClickFormImage -> _uiState.update {
                if (it.formImageUrl.isNotBlank()) {
                    it.copy(infoMessage = "이미지는 한 장만 첨부할 수 있습니다.")
                } else {
                    it.copy(infoMessage = "이미지를 첨부하려면 이미지 선택 기능을 사용해 주세요.")
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
            content = item.content,
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
            content = item.content,
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
        observeNoticeManagementEvent()
        loadNoticePage(cursor = null, append = false)
    }

    private enum class JobKey {
        NOTICE_PAGE,
        EVENT_PAGE,
        SUBMIT,
        DELETE,
        OBSERVE_EVENT
    }
}

private fun String.toNoticeEventValidationMessage(): String {
    return when (this) {
        "cafeId is required" -> "카페 정보를 찾을 수 없습니다."
        "noticeId is required" -> "공지사항 정보를 찾을 수 없습니다."
        "eventId is required" -> "이벤트 정보를 찾을 수 없습니다."
        "notice title is required", "event title is required" -> "제목을 입력해 주세요."
        "notice content is required", "event content is required" -> "내용을 입력해 주세요."
        "event image is required" -> "이벤트 대표 이미지를 첨부해 주세요."
        else -> this
    }
}
