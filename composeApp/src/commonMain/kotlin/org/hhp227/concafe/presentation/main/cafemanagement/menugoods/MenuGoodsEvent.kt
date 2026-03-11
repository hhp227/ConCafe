package org.hhp227.concafe.presentation.main.cafemanagement.menugoods

sealed interface MenuGoodsEvent {
    data object NavigateBack : MenuGoodsEvent
    data class NavigateToEdit(
        val cafeId: String,
        val itemId: String? = null
    ) : MenuGoodsEvent
}
