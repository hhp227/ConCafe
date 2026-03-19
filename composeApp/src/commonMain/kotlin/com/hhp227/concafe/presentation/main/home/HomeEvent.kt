package com.hhp227.concafe.presentation.main.home

sealed interface HomeEvent {
    data class NavigateToExternalLink(val title: String, val url: String) : HomeEvent
    data class NavigateToCast(val id: String) : HomeEvent
    data class NavigateToCafe(val id: String) : HomeEvent
    data object NavigateToSignIn : HomeEvent
}
