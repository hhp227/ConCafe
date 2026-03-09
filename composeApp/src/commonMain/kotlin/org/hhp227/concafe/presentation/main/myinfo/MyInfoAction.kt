package org.hhp227.concafe.presentation.main.myinfo

sealed interface MyInfoAction {
    data class ClickCafe(val id: String) : MyInfoAction
    data class ClickMaid(val id: String) : MyInfoAction
    data object ClickSignIn : MyInfoAction
    data object Refresh : MyInfoAction
}
