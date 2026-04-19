package com.hhp227.concafe.presentation.main.cafemanagement.castmanagement

enum class CastScheduleViewMode { WEEK, MONTH }

data class CastManagementUiState(
    val isLoading: Boolean = true,
    val cafeName: String = "",
    val viewMode: CastScheduleViewMode = CastScheduleViewMode.WEEK,
    val periodStart: String = "",
    val periodEnd: String = "",
    val weekColumns: List<WeekColumn> = emptyList(),
    val monthOffset: Int = 0,
    val monthCells: List<MonthCell> = emptyList(),
    val errorMessage: String? = null
) {
    data class WeekColumn(
        val dayLabelKey: String,
        val dateLabel: String,
        val castNames: List<String>
    )

    data class MonthCell(
        val dayNumber: Int,
        val castNames: List<String>
    )
}
