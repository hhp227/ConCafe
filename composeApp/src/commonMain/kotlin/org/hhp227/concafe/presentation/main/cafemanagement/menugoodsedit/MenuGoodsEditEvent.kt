package com.hhp227.concafe.presentation.main.cafemanagement.menugoodsedit

sealed interface MenuGoodsEditEvent {
    data object NavigateBack : MenuGoodsEditEvent
}
