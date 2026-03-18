package com.hhp227.concafe.presentation.main.cafemanagement.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.event.publisher.CastEventPublisher
import com.hhp227.concafe.domain.event.publisher.ScheduleManagementEventPublisher
import com.hhp227.concafe.domain.model.CastScheduleStatus
import com.hhp227.concafe.domain.model.CastScheduleUpdate
import com.hhp227.concafe.domain.model.ScheduleManagementData
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
            castEventPublisher.observe().collectLatest { event ->
                when (event) {
                    is CastDomainEvent.Created -> if (event.cast.id == castId) {
                        loadSchedule()
                    }
                    is CastDomainEvent.Updated -> if (event.cast.id == castId) {
                        _uiState.update { state ->
                            val cafeName = state.castSummary.subtitle.substringAfter(" / ", "")
                            state.copy(
                                castSummary = state.castSummary.copy(
                                    title = event.cast.name,
                                    subtitle = "${event.cast.conceptRole.toDisplayConceptRole()} / $cafeName",
                                    initials = event.cast.name.toInitials()
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
            scheduleManagementEventPublisher.observe().collectLatest { event ->
                when (event) {
                    is ScheduleManagementDomainEvent.Updated -> if (event.castId == castId) {
                        if (_uiState.value.isSaving) {
                            return@collectLatest
                        }
                        loadSchedule(showLoading = false)
                        _event.emit(
                            ScheduleEvent.ShowMessage(
                                when (event.status) {
                                    CastScheduleStatus.WORK -> "근무 시간이 저장되었습니다."
                                    CastScheduleStatus.OFF -> "휴무로 변경되었습니다."
                                    CastScheduleStatus.VACATION -> "휴가 일정으로 변경되었습니다."
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
                    _uiState.value = result.data.toUiState()
                }
                is AppResult.Failure -> {
                    unbindCastEvent()
                    jobs.remove(TaskKey.OBSERVE_SCHEDULE_EVENT)?.cancel()
                    _uiState.value = ScheduleUiState(
                        isLoading = false,
                        errorMessage = "출근표 데이터를 불러오지 못했습니다.",
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
                _uiState.update { it.copy(infoMessage = "달력 보기 연결은 다음 단계에서 제공합니다.") }
            }
            is ScheduleAction.SelectDay -> {
                _uiState.update { state ->
                    state.copy(
                        selectedDayId = action.dayId,
                        weekDays = state.weekDays.map { day ->
                            day.copy(isSelected = day.id == action.dayId)
                        }
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
                        editStartTime = selected.timeLabel.substringBefore(" - ").takeIf { time -> ":" in time } ?: "10:00",
                        editEndTime = selected.timeLabel.substringAfter(" - ", "19:00").takeIf { time -> ":" in time } ?: "19:00",
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
                if (currentState.editStatus == ScheduleEditStatus.WORK && currentState.editStartTime >= currentState.editEndTime) {
                    _uiState.update { it.copy(errorMessage = "종료 시간은 시작 시간보다 늦어야 합니다.") }
                    return
                }
                _uiState.update { state ->
                    val pendingUpdate = ScheduleUiState.PendingScheduleUpdate(
                        date = editingId,
                        status = currentState.editStatus,
                        startTime = currentState.editStartTime.takeIf { currentState.editStatus == ScheduleEditStatus.WORK },
                        endTime = currentState.editEndTime.takeIf { currentState.editStatus == ScheduleEditStatus.WORK }
                    )
                    state.copy(
                        isEditSheetVisible = false,
                        editingScheduleId = null,
                        errorMessage = null,
                        infoMessage = "편집 내용을 화면에 반영했습니다. 하단 버튼으로 실제 저장을 완료하세요.",
                        schedules = state.schedules.map { schedule ->
                            if (schedule.id == editingId) {
                                pendingUpdate.toDaySchedule(title = schedule.title)
                            } else {
                                schedule
                            }
                        },
                        weekDays = state.weekDays.map { day ->
                            if (day.id == editingId) {
                                day.copy(isWorking = pendingUpdate.status == ScheduleEditStatus.WORK)
                            } else {
                                day
                            }
                        },
                        pendingUpdates = state.pendingUpdates
                            .filterNot { it.date == editingId } + pendingUpdate
                    )
                }
            }
            ScheduleAction.ClickMore -> {
                _uiState.update { it.copy(infoMessage = "추가 메뉴는 다음 단계에서 제공합니다.") }
            }
            ScheduleAction.ClickSave -> {
                val currentState = _uiState.value
                val managedCastId = currentState.managedCastId.ifBlank { return }
                if (currentState.pendingUpdates.isEmpty()) {
                    _uiState.update { it.copy(infoMessage = "저장할 변경사항이 없습니다.", errorMessage = null) }
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
                                    status = pendingUpdate.status.toDomainStatus(),
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
                                            is AppError.ValidationFailed ->
                                                error.reason.toScheduleValidationMessage()
                                            else -> "주간 시간표 저장에 실패했습니다."
                                        }
                                    )
                                }
                                return@launch
                            }
                        }
                    }
                    loadSchedule(showLoading = false)
                    _event.emit(ScheduleEvent.ShowMessage("주간 시간표를 저장했습니다."))
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
}

private fun ScheduleManagementData.toUiState(): ScheduleUiState {
    return ScheduleUiState(
        managedCastId = detail.cast.id,
        isLoading = false,
        isSaving = false,
        errorMessage = null,
        castSummary = ScheduleUiState.CastSummary(
            title = detail.cast.name,
            subtitle = "${detail.cast.conceptRole.toDisplayConceptRole()} / ${detail.cafe.name}",
            badge = "Cast Member",
            initials = detail.cast.name.toInitials()
        ),
        weekRangeLabel = weekRangeLabel,
        weekDays = weekDays.map { day ->
            ScheduleUiState.WeekDay(
                id = day.id,
                label = day.label,
                number = day.number,
                isSelected = day.id == selectedDayId,
                isWorking = day.isWorking
            )
        },
        schedules = daySchedules.map { schedule ->
            ScheduleUiState.DaySchedule(
                id = schedule.id,
                title = schedule.title,
                timeLabel = schedule.timeLabel,
                statusLabel = schedule.statusLabel,
                isWorking = schedule.isWorking,
                status = schedule.status.toUiStatus()
            )
        },
        selectedDayId = selectedDayId,
        infoMessage = null
    )
}

private fun String.toDisplayConceptRole(): String {
    return when (lowercase()) {
        "maid" -> "메이드"
        "butler" -> "버틀러"
        "idol" -> "아이돌"
        else -> replaceFirstChar { char -> char.uppercase() }
    }
}

private fun ScheduleUiState.PendingScheduleUpdate.toDaySchedule(title: String): ScheduleUiState.DaySchedule {
    val isWorking = status == ScheduleEditStatus.WORK
    return ScheduleUiState.DaySchedule(
        id = date,
        title = title,
        timeLabel = when (status) {
            ScheduleEditStatus.WORK -> "${startTime ?: "10:00"} - ${endTime ?: "19:00"}"
            ScheduleEditStatus.OFF -> "휴무"
            ScheduleEditStatus.VACATION -> "휴가"
        },
        statusLabel = status.label,
        isWorking = isWorking,
        status = status
    )
}

private fun String.toInitials(): String {
    return take(2).uppercase()
}

private enum class TaskKey {
    OBSERVE_SESSION,
    OBSERVE_CAST_EVENT,
    OBSERVE_SCHEDULE_EVENT,
    SUBMIT
}

private fun ScheduleEditStatus.toDomainStatus(): CastScheduleStatus {
    return when (this) {
        ScheduleEditStatus.WORK -> CastScheduleStatus.WORK
        ScheduleEditStatus.OFF -> CastScheduleStatus.OFF
        ScheduleEditStatus.VACATION -> CastScheduleStatus.VACATION
    }
}

private fun CastScheduleStatus.toUiStatus(): ScheduleEditStatus {
    return when (this) {
        CastScheduleStatus.WORK -> ScheduleEditStatus.WORK
        CastScheduleStatus.OFF -> ScheduleEditStatus.OFF
        CastScheduleStatus.VACATION -> ScheduleEditStatus.VACATION
    }
}

private fun String.toScheduleValidationMessage(): String {
    return when (this) {
        "start time is required" -> "시작 시간을 선택해주세요."
        "end time is required" -> "종료 시간을 선택해주세요."
        "end time must be after start time" -> "종료 시간은 시작 시간보다 늦어야 합니다."
        else -> "근무 시간 저장에 실패했습니다."
    }
}
