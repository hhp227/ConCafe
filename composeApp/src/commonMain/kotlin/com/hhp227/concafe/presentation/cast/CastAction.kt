package com.hhp227.concafe.presentation.cast

sealed interface CastAction {
    data object ClickBack : CastAction

    data object ClickFollow : CastAction

    data object MarkFollowTooltipShown : CastAction

    data object DismissFollowTooltip : CastAction

    data object ClickCafe : CastAction

    data object Refresh : CastAction

    data class ClickImage(val imageUrl: String) : CastAction
}
