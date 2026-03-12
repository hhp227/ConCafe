package com.hhp227.concafe.presentation.cafe

sealed interface CafeAction {
    data object ClickBack : CafeAction

    data class ChangeTab(val tab: CafeUiState.TabType) : CafeAction

    data class ClickMaid(val id: String) : CafeAction

    data object ClickFavorite : CafeAction

    data object LoadMoreCasts : CafeAction

    data object Refresh : CafeAction
}
