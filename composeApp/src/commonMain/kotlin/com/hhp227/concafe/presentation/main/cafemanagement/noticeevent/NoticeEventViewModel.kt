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
import com.hhp227.concafe.domain.model.NoticeStatusAccent
import com.hhp227.concafe.domain.usecase.CreateCafeEventUseCase
import com.hhp227.concafe.domain.usecase.CreateCafeNoticeUseCase
import com.hhp227.concafe.domain.usecase.DeleteCafeEventUseCase
import com.hhp227.concafe.domain.usecase.DeleteCafeNoticeUseCase
import com.hhp227.concafe.domain.usecase.GetCafeEventPageUseCase
import com.hhp227.concafe.domain.usecase.GetCafeNoticePageUseCase
import com.hhp227.concafe.domain.usecase.UploadImageUseCase
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
    private val noticeManagementEventPublisher: NoticeManagementEventPublisher,
    private val uploadImageUseCase: UploadImageUseCase
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
            setInfoMessage(MSG_NOTICE_EDIT_TARGET_NOT_FOUND)
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
                formReservedAt = if (target.statusAccent == NoticeStatusAccent.DRAFT) target.displayDate else "",
                infoMessage = null
            )
        }
    }

    private fun openEditEventForm(id: String) {
        val target = _uiState.value.events.firstOrNull { it.id == id } ?: run {
            setInfoMessage(MSG_EVENT_EDIT_TARGET_NOT_FOUND)
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
                formReservedAt = target.periodText,
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
                            notices = if (append) state.notices + result.data.items else result.data.items,
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
                            infoMessage = MSG_NOTICE_LOAD_FAILED
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
                            events = if (append) state.events + result.data.items else result.data.items,
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
                            infoMessage = MSG_EVENT_LOAD_FAILED
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
                val uploadedImageUrl = uploadEventImage(state.formImageUrl) ?: return@launch
                if (state.formEditingId == null) {
                    createCafeEventUseCase.invoke(
                        CafeEventCreate(
                            cafeId = cafeId,
                            title = state.formTitle,
                            content = state.formContent,
                            imageUrl = uploadedImageUrl,
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
                            imageUrl = uploadedImageUrl,
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
                                if (state.formEditingId == null) MSG_NOTICE_CREATED else MSG_NOTICE_UPDATED
                            } else {
                                if (state.formEditingId == null) MSG_EVENT_CREATED else MSG_EVENT_UPDATED
                            }
                        )
                    }
                    refreshCurrentTab()
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isSubmittingForm = false,
                            infoMessage = when (val error = result.error) {
                                is AppError.ValidationFailed -> when (error.reason) {
                                    "cafeId is required" -> "noticeevent_validation_cafe_required"
                                    "noticeId is required" -> "noticeevent_validation_notice_required"
                                    "eventId is required" -> "noticeevent_validation_event_required"
                                    "notice title is required", "event title is required" -> "noticeevent_validation_title_required"
                                    "notice content is required", "event content is required" -> "noticeevent_validation_content_required"
                                    "event image is required" -> "noticeevent_validation_event_image_required"
                                    else -> error.reason
                                }
                                else -> if (state.selectedTab == NoticeEventTab.NOTICE) {
                                    if (state.formEditingId == null) MSG_NOTICE_CREATE_FAILED else MSG_NOTICE_UPDATE_FAILED
                                } else {
                                    if (state.formEditingId == null) MSG_EVENT_CREATE_FAILED else MSG_EVENT_UPDATE_FAILED
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
                is AppResult.Success -> setInfoMessage(MSG_NOTICE_DELETE_SUCCESS)
                is AppResult.Failure -> setInfoMessage(MSG_NOTICE_DELETE_FAILED)
            }
        }
    }

    private fun deleteEvent(id: String) {
        jobs[JobKey.DELETE]?.cancel()
        jobs[JobKey.DELETE] = viewModelScope.launch {
            when (deleteCafeEventUseCase.invoke(cafeId = cafeId, eventId = id)) {
                is AppResult.Success -> setInfoMessage(MSG_EVENT_DELETE_SUCCESS)
                is AppResult.Failure -> setInfoMessage(MSG_EVENT_DELETE_FAILED)
            }
        }
    }

    private fun patchNotice(item: CafeNoticeManagementItem) {
        _uiState.update { state ->
            state.copy(notices = state.notices.map { if (it.id == item.id) item else it })
        }
    }

    private fun patchEvent(item: CafeEventManagementItem) {
        _uiState.update { state ->
            state.copy(events = state.events.map { if (it.id == item.id) item else it })
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

    private suspend fun uploadEventImage(imageUrl: String): String? {
        if (imageUrl.isBlank()) return imageUrl
        return when (val result = uploadImageUseCase.invoke(imageUrl, "events")) {
            is AppResult.Success -> result.data
            is AppResult.Failure -> {
                _uiState.update {
                    it.copy(
                        isSubmittingForm = false,
                        infoMessage = MSG_IMAGE_UPLOAD_FAILED
                    )
                }
                null
            }
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
            NoticeEventAction.ClickMoreEvents -> setInfoMessage(MSG_MORE_EVENTS_NEXT_STEP)
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
                    it.copy(infoMessage = MSG_IMAGE_ONE_ONLY)
                } else {
                    it.copy(infoMessage = MSG_IMAGE_PICK_REQUIRED)
                }
            }
            NoticeEventAction.ClickRemoveFormImage -> _uiState.update { it.copy(formImageUrl = "", infoMessage = null) }
            is NoticeEventAction.ChangeFormPinned -> _uiState.update { it.copy(formPinned = action.value) }
            is NoticeEventAction.ChangeFormReservedAt -> _uiState.update { it.copy(formReservedAt = action.value, infoMessage = null) }
            NoticeEventAction.ClickReserveSchedule -> setInfoMessage(MSG_RESERVE_SCHEDULE_NEXT_STEP)
            NoticeEventAction.ClickSubmitForm -> submitForm()
            NoticeEventAction.DismissInfoMessage -> _uiState.update { it.copy(infoMessage = null) }
        }
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

    companion object {
        private const val MSG_NOTICE_EDIT_TARGET_NOT_FOUND = "noticeevent_info_notice_edit_target_not_found"
        private const val MSG_EVENT_EDIT_TARGET_NOT_FOUND = "noticeevent_info_event_edit_target_not_found"
        private const val MSG_NOTICE_LOAD_FAILED = "noticeevent_info_notice_load_failed"
        private const val MSG_EVENT_LOAD_FAILED = "noticeevent_info_event_load_failed"
        private const val MSG_NOTICE_CREATED = "noticeevent_info_notice_created"
        private const val MSG_NOTICE_UPDATED = "noticeevent_info_notice_updated"
        private const val MSG_EVENT_CREATED = "noticeevent_info_event_created"
        private const val MSG_EVENT_UPDATED = "noticeevent_info_event_updated"
        private const val MSG_NOTICE_CREATE_FAILED = "noticeevent_info_notice_create_failed"
        private const val MSG_NOTICE_UPDATE_FAILED = "noticeevent_info_notice_update_failed"
        private const val MSG_EVENT_CREATE_FAILED = "noticeevent_info_event_create_failed"
        private const val MSG_EVENT_UPDATE_FAILED = "noticeevent_info_event_update_failed"
        private const val MSG_NOTICE_DELETE_SUCCESS = "noticeevent_info_notice_delete_success"
        private const val MSG_NOTICE_DELETE_FAILED = "noticeevent_info_notice_delete_failed"
        private const val MSG_EVENT_DELETE_SUCCESS = "noticeevent_info_event_delete_success"
        private const val MSG_EVENT_DELETE_FAILED = "noticeevent_info_event_delete_failed"
        private const val MSG_IMAGE_UPLOAD_FAILED = "noticeevent_info_image_upload_failed"
        private const val MSG_MORE_EVENTS_NEXT_STEP = "noticeevent_info_more_events_next_step"
        private const val MSG_IMAGE_ONE_ONLY = "noticeevent_info_image_one_only"
        private const val MSG_IMAGE_PICK_REQUIRED = "noticeevent_info_image_pick_required"
        private const val MSG_RESERVE_SCHEDULE_NEXT_STEP = "noticeevent_info_reserve_schedule_next_step"
    }
}
