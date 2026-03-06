package org.hhp227.concafe.presentation.main.explore

sealed interface ExploreEvent {
    data class NavigateToCastDetail(val id: String) : ExploreEvent
    data class NavigateToCafeDetail(val id: String) : ExploreEvent
}
