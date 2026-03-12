package com.hhp227.concafe.presentation.main.checkin

sealed interface CheckInEvent {
    data class NavigateToCafe(val id: String) : CheckInEvent

    data class NavigateToCast(val id: String) : CheckInEvent

    data object NavigateToSignIn : CheckInEvent
}
