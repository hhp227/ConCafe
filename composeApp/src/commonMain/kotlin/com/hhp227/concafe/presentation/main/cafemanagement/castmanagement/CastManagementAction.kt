package com.hhp227.concafe.presentation.main.cafemanagement.castmanagement

sealed interface CastManagementAction {
    data object ClickBack : CastManagementAction
    data class ChangeViewMode(val mode: CastScheduleViewMode) : CastManagementAction
}
