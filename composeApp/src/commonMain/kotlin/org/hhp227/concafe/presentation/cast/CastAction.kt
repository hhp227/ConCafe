package org.hhp227.concafe.presentation.cast

sealed interface CastAction {
    data object ClickBack : CastAction

    data object ClickFollow : CastAction

    data object ClickCafe : CastAction

    data object Refresh : CastAction
}
