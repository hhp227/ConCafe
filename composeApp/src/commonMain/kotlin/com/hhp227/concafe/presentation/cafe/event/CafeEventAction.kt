package com.hhp227.concafe.presentation.cafe.event

sealed interface CafeEventAction {
    data object ClickBack : CafeEventAction
    data object Retry : CafeEventAction
    data object ToggleLike : CafeEventAction
    data object GoToCafe : CafeEventAction
    data class ClickCast(val castId: String) : CafeEventAction
    data object ClickLoginPromptSignIn : CafeEventAction
    data object DismissLoginPrompt : CafeEventAction
}
