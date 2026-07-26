package com.hhp227.concafe.presentation.main.cafemanagement.castmanagement

sealed interface CastManagementEvent {
    data object NavigateBack : CastManagementEvent
}
