package org.hhp227.concafe.presentation.main.myinfo

sealed interface MyInfoEvent {
    data class NavigateToCafeDetail(val id: String) : MyInfoEvent
    data class NavigateToCastDetail(val id: String) : MyInfoEvent
}
