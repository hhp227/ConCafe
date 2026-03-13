package com.hhp227.concafe.presentation.main.cafemanagement.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hhp227.concafe.di.resolveGetScheduleManagementDataUseCase
import com.hhp227.concafe.di.resolveObserveCastEventUseCase
import com.hhp227.concafe.di.resolveObserveCurrentUserUseCase
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.CastEvent as CastDomainEvent
import com.hhp227.concafe.domain.model.ScheduleManagementData
import com.hhp227.concafe.domain.usecase.GetScheduleManagementDataUseCase
import com.hhp227.concafe.domain.usecase.ObserveCastEventUseCase
import com.hhp227.concafe.domain.usecase.ObserveCurrentUserUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ScheduleViewModel(
    private val castId: String? = null,
    private val getScheduleManagementDataUseCase: GetScheduleManagementDataUseCase = resolveGetScheduleManagementDataUseCase(),
    private val observeCastEventUseCase: ObserveCastEventUseCase = resolveObserveCastEventUseCase(),
    private val observeCurrentUserUseCase: ObserveCurrentUserUseCase = resolveObserveCurrentUserUseCase()
) : ViewModel() {
    private val _uiState = MutableStateFlow(ScheduleUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<ScheduleEvent>(replay = 0)
    val event = _event.asSharedFlow()

    private val jobs = mutableMapOf<TaskKey, Job>()

    init {
        observeSession()
    }

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
            observeCastEventUseCase.invoke().collectLatest { event ->
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

    private fun unbindCastEvent() {
        jobs.remove(TaskKey.OBSERVE_CAST_EVENT)?.cancel()
    }

    private fun loadSchedule() {
        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                infoMessage = null
            )
        }
        viewModelScope.launch {
            when (val result = getScheduleManagementDataUseCase.invoke(castId)) {
                is AppResult.Success -> {
                    bindCastEvent(result.data.detail.cast.id)
                    _uiState.value = result.data.toUiState()
                }
                is AppResult.Failure -> {
                    unbindCastEvent()
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
                    it.copy(infoMessage = "${selected.title} 수정은 다음 단계에서 제공합니다.")
                }
            }
            ScheduleAction.ClickMore -> {
                _uiState.update { it.copy(infoMessage = "추가 메뉴는 다음 단계에서 제공합니다.") }
            }
            ScheduleAction.ClickSave -> {
                _uiState.update { it.copy(isSaving = true, infoMessage = "주간 시간표를 저장했습니다.") }
                _uiState.update { it.copy(isSaving = false) }
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
}

private fun ScheduleManagementData.toUiState(): ScheduleUiState {
    return ScheduleUiState(
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
                isWorking = schedule.isWorking
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

private fun String.toInitials(): String {
    return take(2).uppercase()
}

private enum class TaskKey {
    OBSERVE_SESSION,
    OBSERVE_CAST_EVENT
}
