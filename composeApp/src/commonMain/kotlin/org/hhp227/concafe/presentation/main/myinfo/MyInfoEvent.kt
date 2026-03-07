package org.hhp227.concafe.presentation.main.myinfo

sealed interface MyInfoEvent {
    data class NavigateToCafe(val id: String) : MyInfoEvent
    data class NavigateToCast(val id: String) : MyInfoEvent
    data object NavigateToSignIn : MyInfoEvent
}
