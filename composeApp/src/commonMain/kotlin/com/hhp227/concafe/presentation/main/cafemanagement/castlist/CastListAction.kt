package com.hhp227.concafe.presentation.main.cafemanagement.castlist

sealed interface CastListAction {
    data object ClickBack : CastListAction
    data object ClickAddCast : CastListAction
    data class ChangeSearchQuery(val value: String) : CastListAction
    data class ClickCast(val castId: String) : CastListAction
    data class ClickCastSchedule(val castId: String) : CastListAction
    data class ClickDeleteCast(val castId: String) : CastListAction
    data object ConfirmDeleteCast : CastListAction
    data object DismissDeleteCastDialog : CastListAction
    data object ClickLoadMoreCasts : CastListAction
    data object DismissInfoMessage : CastListAction
}
