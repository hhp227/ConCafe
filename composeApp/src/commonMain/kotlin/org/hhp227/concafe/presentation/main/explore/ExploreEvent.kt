package org.hhp227.concafe.presentation.main.explore

sealed interface ExploreEvent {
    data class NavigateToCast(val id: String) : ExploreEvent
    data class NavigateToCafe(val id: String) : ExploreEvent
}
