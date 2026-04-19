package com.hhp227.concafe.presentation.main.cafemanagement.castmanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.usecase.GetCafeScheduleCalendarUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*

class CastManagementViewModel(
    private val cafeId: String,
    private val cafeName: String,
    private val getCafeScheduleCalendarUseCase: GetCafeScheduleCalendarUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(CastManagementUiState(isLoading = true, cafeName = cafeName))
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<CastManagementEvent>(replay = 0)
    val event = _event.asSharedFlow()

    private var loadJob: Job? = null

    private fun loadCalendar() {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        loadJob = viewModelScope.launch {
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val mode = _uiState.value.viewMode
            val (fromDate, toDate) = computeDateRange(today, mode)
            when (val result = getCafeScheduleCalendarUseCase(cafeId, fromDate, toDate)) {
                is AppResult.Success -> {
                    val days = result.data
                    _uiState.update { state ->
                        if (mode == CastScheduleViewMode.WEEK) {
                            state.copy(
                                isLoading = false,
                                periodStart = fromDate,
                                periodEnd = toDate,
                                weekColumns = days.map { day ->
                                    val date = LocalDate.parse(day.date)
                                    CastManagementUiState.WeekColumn(
                                        dayLabelKey = date.toDayLabelKey(),
                                        dateLabel = "${date.monthNumber}/${date.dayOfMonth}",
                                        castNames = day.castNames
                                    )
                                },
                                monthOffset = 0,
                                monthCells = emptyList(),
                                errorMessage = null
                            )
                        } else {
                            val firstDate = LocalDate.parse(fromDate)
                            val offset = firstDate.dayOfWeek.isoDayNumber - 1
                            state.copy(
                                isLoading = false,
                                periodStart = fromDate,
                                periodEnd = toDate,
                                weekColumns = emptyList(),
                                monthOffset = offset,
                                monthCells = days.map { day ->
                                    val date = LocalDate.parse(day.date)
                                    CastManagementUiState.MonthCell(
                                        dayNumber = date.dayOfMonth,
                                        castNames = day.castNames
                                    )
                                },
                                errorMessage = null
                            )
                        }
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "cast_management_error_load_failed") }
                }
            }
        }
    }

    private fun computeDateRange(today: LocalDate, mode: CastScheduleViewMode): Pair<String, String> {
        return when (mode) {
            CastScheduleViewMode.WEEK -> {
                val daysFromMonday = today.dayOfWeek.isoDayNumber - 1
                val weekStart = today.minus(DatePeriod(days = daysFromMonday))
                val weekEnd = weekStart.plus(DatePeriod(days = 6))
                Pair(weekStart.toString(), weekEnd.toString())
            }
            CastScheduleViewMode.MONTH -> {
                val monthStart = LocalDate(today.year, today.month, 1)
                val monthEnd = monthStart.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))
                Pair(monthStart.toString(), monthEnd.toString())
            }
        }
    }

    fun onAction(action: CastManagementAction) {
        when (action) {
            CastManagementAction.ClickBack -> {
                viewModelScope.launch { _event.emit(CastManagementEvent.NavigateBack) }
            }
            is CastManagementAction.ChangeViewMode -> {
                if (_uiState.value.viewMode == action.mode) return
                _uiState.update { it.copy(viewMode = action.mode) }
                loadCalendar()
            }
        }
    }

    override fun onCleared() {
        loadJob?.cancel()
        super.onCleared()
    }

    init {
        loadCalendar()
    }
}

private fun LocalDate.toDayLabelKey(): String {
    return when (dayOfWeek) {
        DayOfWeek.MONDAY -> "day_mon"
        DayOfWeek.TUESDAY -> "day_tue"
        DayOfWeek.WEDNESDAY -> "day_wed"
        DayOfWeek.THURSDAY -> "day_thu"
        DayOfWeek.FRIDAY -> "day_fri"
        DayOfWeek.SATURDAY -> "day_sat"
        DayOfWeek.SUNDAY -> "day_sun"
        else -> ""
    }
}
