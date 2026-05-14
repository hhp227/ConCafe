package com.hhp227.concafe.presentation.main.cafemanagement.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.event.publisher.CastEventPublisher
import com.hhp227.concafe.domain.event.publisher.ScheduleManagementEventPublisher
import com.hhp227.concafe.domain.model.CastScheduleStatus
import com.hhp227.concafe.domain.model.CastScheduleUpdate
import com.hhp227.concafe.domain.model.ScheduleManagementDaySchedule
import com.hhp227.concafe.domain.usecase.GetScheduleManagementDataUseCase
import com.hhp227.concafe.domain.usecase.ObserveCurrentUserUseCase
import com.hhp227.concafe.domain.usecase.UpdateCastScheduleUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.hhp227.concafe.domain.event.CastEvent as CastDomainEvent
import com.hhp227.concafe.domain.event.ScheduleManagementEvent as ScheduleManagementDomainEvent

class ScheduleViewModel(
    private val castId: String? = null,
    private val getScheduleManagementDataUseCase: GetScheduleManagementDataUseCase,
    private val observeCurrentUserUseCase: ObserveCurrentUserUseCase,
    private val updateCastScheduleUseCase: UpdateCastScheduleUseCase,
    private val castEventPublisher: CastEventPublisher,
    private val scheduleManagementEventPublisher: ScheduleManagementEventPublisher
) : ViewModel() {
    private val _uiState = MutableStateFlow(ScheduleUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<ScheduleEvent>(replay = 0)
    val event = _event.asSharedFlow()

    private val jobs = mutableMapOf<TaskKey, Job>()

    private fun observeSession() {
        jobs[TaskKey.OBSERVE_SESSION]?.cancel()
        jobs[TaskKey.OBSERVE_SESSION] = viewModelScope.launch {
            observeCurrentUserUseCase.invoke().collectLatest {
                unbindCastEvent()
                loadSchedule()
            }
        }
    }

    private fun bindCastEvent(castId: String) {
        jobs[TaskKey.OBSERVE_CAST_EVENT]?.cancel()
        jobs[TaskKey.OBSERVE_CAST_EVENT] = viewModelScope.launch {
            castEventPublisher.events.collectLatest { event ->
                when (event) {
                    is CastDomainEvent.Created -> if (event.cast.id == castId) {
                        loadSchedule()
                    }
                    is CastDomainEvent.Updated -> if (event.cast.id == castId) {
                        _uiState.update { state ->
                            val cafeName = state.castSummary.subtitle.substringAfter(" / ", "")
                            val conceptRole = when (event.cast.conceptRole.lowercase()) {
                                "maid" -> SCHEDULE_CONCEPT_MAID
                                "butler" -> SCHEDULE_CONCEPT_BUTLER
                                "idol" -> SCHEDULE_CONCEPT_IDOL
                                else -> event.cast.conceptRole.replaceFirstChar { char -> char.uppercase() }
                            }
                            state.copy(
                                castSummary = state.castSummary.copy(
                                    title = event.cast.name,
                                    subtitle = "$conceptRole / $cafeName",
                                    initials = event.cast.name.take(2).uppercase(),
                                    profileImageUrl = event.cast.profileImage
                                )
                            )
                        }
                    }
                    is CastDomainEvent.Deleted -> if (event.castId == castId) {
                        loadSchedule()
                    }
                }
            }
        }
    }

    private fun bindScheduleManagementEvent(castId: String) {
        jobs[TaskKey.OBSERVE_SCHEDULE_EVENT]?.cancel()
        jobs[TaskKey.OBSERVE_SCHEDULE_EVENT] = viewModelScope.launch {
            scheduleManagementEventPublisher.events.collectLatest { event ->
                when (event) {
                    is ScheduleManagementDomainEvent.Updated -> if (event.castId == castId) {
                        if (_uiState.value.isSaving) {
                            return@collectLatest
                        }
                        loadSchedule(showLoading = false)
                        _event.emit(
                            ScheduleEvent.ShowMessage(
                                when (event.status) {
                                    CastScheduleStatus.WORK -> "schedule_info_saved_work"
                                    CastScheduleStatus.OFF -> "schedule_info_saved_off"
                                    CastScheduleStatus.VACATION -> "schedule_info_saved_vacation"
                                }
                            )
                        )
                    }
                }
            }
        }
    }

    private fun unbindCastEvent() {
        jobs.remove(TaskKey.OBSERVE_CAST_EVENT)?.cancel()
        jobs.remove(TaskKey.OBSERVE_SCHEDULE_EVENT)?.cancel()
    }

    private fun loadSchedule(showLoading: Boolean = true) {
        val currentPeriod = _uiState.value.schedulePeriod
        val currentSelectedDayId = _uiState.value.selectedDayId

        _uiState.update {
            it.copy(
                isLoading = showLoading,
                isSaving = false,
                errorMessage = null,
                infoMessage = null,
                pendingUpdates = emptyList()
            )
        }
        viewModelScope.launch {
            when (val result = getScheduleManagementDataUseCase.invoke(castId)) {
                is AppResult.Success -> {
                    bindCastEvent(result.data.detail.cast.id)
                    bindScheduleManagementEvent(result.data.detail.cast.id)
                    val data = result.data
                    val conceptRole = when (data.detail.cast.conceptRole.lowercase()) {
                        "maid" -> SCHEDULE_CONCEPT_MAID
                        "butler" -> SCHEDULE_CONCEPT_BUTLER
                        "idol" -> SCHEDULE_CONCEPT_IDOL
                        else -> data.detail.cast.conceptRole.replaceFirstChar { char -> char.uppercase() }
                    }
                    _uiState.value = ScheduleUiState(
                        managedCastId = data.detail.cast.id,
                        managedCafeId = data.detail.cafe.id,
                        managedCafeName = data.detail.cafe.name,
                        isLoading = false,
                        isSaving = false,
                        errorMessage = null,
                        castSummary = ScheduleUiState.CastSummary(
                            title = data.detail.cast.name,
                            subtitle = "$conceptRole / ${data.detail.cafe.name}",
                            badge = "schedule_badge_cast_member",
                            initials = data.detail.cast.name.take(2).uppercase(),
                            profileImageUrl = data.detail.cast.profileImage
                        ),
                        allWeekDays = data.weekDays,
                        allSchedules = data.daySchedules,
                        selectedDayId = currentSelectedDayId.ifBlank { data.selectedDayId },
                        infoMessage = null
                    ).withSchedulePeriod(currentPeriod)
                }
                is AppResult.Failure -> {
                    unbindCastEvent()
                    jobs.remove(TaskKey.OBSERVE_SCHEDULE_EVENT)?.cancel()
                    _uiState.value = ScheduleUiState(
                        isLoading = false,
                        errorMessage = "schedule_info_load_failed",
                        infoMessage = null
                    )
                }
            }
        }
    }

    fun onAction(action: ScheduleAction) {
        when (action) {
            ScheduleAction.ClickBack -> {
                viewModelScope.launch {
                    _event.emit(ScheduleEvent.NavigateBack)
                }
            }
            ScheduleAction.ClickCalendar -> {
                val state = _uiState.value
                val cafeId = state.managedCafeId
                val cafeName = state.managedCafeName
                if (cafeId.isBlank()) {
                    _uiState.update { it.copy(infoMessage = "schedule_info_calendar_next_step") }
                } else {
                    viewModelScope.launch {
                        _event.emit(ScheduleEvent.NavigateToCastManagement(cafeId, cafeName))
                    }
                }
            }
            is ScheduleAction.SelectPeriod -> {
                _uiState.update { state -> state.withSchedulePeriod(action.period) }
            }
            is ScheduleAction.SelectDay -> {
                _uiState.update { state ->
                    state.copy(
                        selectedDayId = action.dayId
                    )
                }
            }
            is ScheduleAction.ClickEditDay -> {
                val selected = _uiState.value.schedules.firstOrNull { it.id == action.dayId } ?: return
                _uiState.update {
                    it.copy(
                        isEditSheetVisible = true,
                        editingScheduleId = selected.id,
                        editingScheduleTitle = selected.title,
                        editStatus = selected.status,
                        editStartTime = selected.timeLabel.substringBefore(" - ").takeIf { time -> ":" in time } ?: ScheduleUiState.DEFAULT_START_TIME,
                        editEndTime = selected.timeLabel.substringAfter(" - ", ScheduleUiState.DEFAULT_END_TIME).substringBefore(" ").takeIf { time -> ":" in time } ?: ScheduleUiState.DEFAULT_END_TIME,
                        infoMessage = null
                    )
                }
            }
            ScheduleAction.DismissEditSheet -> {
                _uiState.update {
                    it.copy(
                        isEditSheetVisible = false,
                        editingScheduleId = null
                    )
                }
            }
            is ScheduleAction.ChangeEditStatus -> {
                _uiState.update { it.copy(editStatus = action.status) }
            }
            is ScheduleAction.ChangeEditStartTime -> {
                _uiState.update { it.copy(editStartTime = action.value) }
            }
            is ScheduleAction.ChangeEditEndTime -> {
                _uiState.update { it.copy(editEndTime = action.value) }
            }
            ScheduleAction.SubmitEditDay -> {
                val currentState = _uiState.value
                val editingId = currentState.editingScheduleId ?: return
                if (currentState.editStatus == CastScheduleStatus.WORK && currentState.editStartTime >= currentState.editEndTime) {
                    _uiState.update { it.copy(errorMessage = "schedule_error_end_after_start") }
                    return
                }
                _uiState.update { state ->
                    val pendingUpdate = ScheduleUiState.PendingScheduleUpdate(
                        date = editingId,
                        status = currentState.editStatus,
                        startTime = currentState.editStartTime.takeIf { currentState.editStatus == CastScheduleStatus.WORK },
                        endTime = currentState.editEndTime.takeIf { currentState.editStatus == CastScheduleStatus.WORK }
                    )
                    state.copy(
                        isEditSheetVisible = false,
                        editingScheduleId = null,
                        errorMessage = null,
                        infoMessage = "schedule_info_edit_applied",
                        allSchedules = state.allSchedules.map { schedule ->
                            if (schedule.id == editingId) {
                                val isWorking = pendingUpdate.status == CastScheduleStatus.WORK
                                val timeLabel = when (pendingUpdate.status) {
                                    CastScheduleStatus.WORK -> "${pendingUpdate.startTime ?: ScheduleUiState.DEFAULT_START_TIME} - ${pendingUpdate.endTime ?: ScheduleUiState.DEFAULT_END_TIME}"
                                    CastScheduleStatus.OFF -> "schedule_status_off"
                                    CastScheduleStatus.VACATION -> "schedule_status_vacation"
                                }
                                val statusLabel = when (pendingUpdate.status) {
                                    CastScheduleStatus.WORK -> "schedule_status_work"
                                    CastScheduleStatus.OFF -> "schedule_status_off"
                                    CastScheduleStatus.VACATION -> "schedule_status_vacation"
                                }
                                ScheduleManagementDaySchedule(
                                    id = pendingUpdate.date,
                                    title = schedule.title,
                                    timeLabel = timeLabel,
                                    statusLabel = statusLabel,
                                    isWorking = isWorking,
                                    status = pendingUpdate.status
                                )
                            } else {
                                schedule
                            }
                        },
                        allWeekDays = state.allWeekDays.map { day ->
                            if (day.id == editingId) {
                                day.copy(isWorking = pendingUpdate.status == CastScheduleStatus.WORK)
                            } else {
                                day
                            }
                        },
                        pendingUpdates = state.pendingUpdates
                            .filterNot { it.date == editingId } + pendingUpdate
                    ).withSchedulePeriod(state.schedulePeriod)
                }
            }
            ScheduleAction.ClickMore -> {
                _uiState.update { it.copy(infoMessage = "schedule_info_more_next_step") }
            }
            ScheduleAction.ClickSave -> {
                val currentState = _uiState.value
                val managedCastId = currentState.managedCastId.ifBlank { return }
                if (currentState.pendingUpdates.isEmpty()) {
                    _uiState.update { it.copy(infoMessage = "schedule_info_no_changes", errorMessage = null) }
                    return
                }
                _uiState.update { it.copy(isSaving = true, errorMessage = null, infoMessage = null) }
                jobs[TaskKey.SUBMIT]?.cancel()
                jobs[TaskKey.SUBMIT] = viewModelScope.launch {
                    val pendingUpdates = currentState.pendingUpdates
                    for (pendingUpdate in pendingUpdates) {
                        when (
                            val result = updateCastScheduleUseCase.invoke(
                                CastScheduleUpdate(
                                    castId = managedCastId,
                                    date = pendingUpdate.date,
                                    status = pendingUpdate.status,
                                    startTime = pendingUpdate.startTime,
                                    endTime = pendingUpdate.endTime
                                )
                            )
                        ) {
                            is AppResult.Success -> Unit
                            is AppResult.Failure -> {
                                _uiState.update {
                                    val error = result.error
                                    it.copy(
                                        isSaving = false,
                                        errorMessage = when (error) {
                                            is AppError.ValidationFailed -> when (error.reason) {
                                                "start time is required" -> "schedule_error_start_required"
                                                "end time is required" -> "schedule_error_end_required"
                                                "end time must be after start time" -> "schedule_error_end_after_start"
                                                else -> "schedule_error_save_failed"
                                            }
                                            else -> "schedule_error_week_save_failed"
                                        }
                                    )
                                }
                                return@launch
                            }
                        }
                    }
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = null,
                            infoMessage = null,
                            pendingUpdates = emptyList()
                        )
                    }
                    _event.emit(ScheduleEvent.ShowMessage("schedule_event_week_saved"))
                }
            }
            ScheduleAction.DismissInfoMessage -> {
                _uiState.update { it.copy(infoMessage = null, errorMessage = null) }
            }
        }
    }

    override fun onCleared() {
        jobs.values.forEach(Job::cancel)
        jobs.clear()
        super.onCleared()
    }

    init {
        observeSession()
    }

    companion object {
        private const val SCHEDULE_CONCEPT_MAID = "schedule_concept_maid"
        private const val SCHEDULE_CONCEPT_BUTLER = "schedule_concept_butler"
        private const val SCHEDULE_CONCEPT_IDOL = "schedule_concept_idol"
    }
}

private enum class TaskKey {
    OBSERVE_SESSION,
    OBSERVE_CAST_EVENT,
    OBSERVE_SCHEDULE_EVENT,
    SUBMIT
}
