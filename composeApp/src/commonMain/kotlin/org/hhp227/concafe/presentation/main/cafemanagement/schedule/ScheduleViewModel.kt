package org.hhp227.concafe.presentation.main.cafemanagement.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ScheduleViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<ScheduleEvent>(replay = 0)
    val event = _event.asSharedFlow()

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
                _uiState.update { it.copy(infoMessage = null) }
            }
        }
    }
}
