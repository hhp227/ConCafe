package org.hhp227.concafe.presentation.main.cafemanagement.menugoods

sealed interface MenuGoodsEvent {
    data object NavigateBack : MenuGoodsEvent
}
