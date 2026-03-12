package com.hhp227.concafe.presentation.main.home

sealed interface HomeEvent {
    data class NavigateToCast(val id: String) : HomeEvent
    data class NavigateToCafe(val id: String) : HomeEvent
}