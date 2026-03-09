package org.hhp227.concafe.presentation.main.checkin

sealed interface CheckInAction {
    data class ClickCafe(val id: String) : CheckInAction

    data class ClickCast(val id: String) : CheckInAction

    data object ClickCheckIn : CheckInAction

    data object ClickSignIn : CheckInAction

    data object ClickSignUp : CheckInAction

    data object DismissLoginPrompt : CheckInAction
}
