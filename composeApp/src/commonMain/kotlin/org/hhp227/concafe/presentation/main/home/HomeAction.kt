package org.hhp227.concafe.presentation.main.home

sealed interface HomeAction {
    data class ClickMaid(val id: String) : HomeAction
    data class ClickCafe(val id: String) : HomeAction
    data class ClickBirthdayMaid(val id: String) : HomeAction
}