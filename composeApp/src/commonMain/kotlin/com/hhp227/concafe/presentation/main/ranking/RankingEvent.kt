package com.hhp227.concafe.presentation.main.ranking

sealed interface RankingEvent {
    data class NavigateToCast(val id: String) : RankingEvent
    data class NavigateToCafe(val id: String) : RankingEvent
}
