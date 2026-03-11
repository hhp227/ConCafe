package org.hhp227.concafe.presentation.main.cafemanagement.menugoods

sealed interface MenuGoodsAction {
    data object ClickBack : MenuGoodsAction
    data object ClickSearch : MenuGoodsAction
    data class ChangeSearchQuery(val value: String) : MenuGoodsAction
    data class SelectCollection(val collection: MenuGoodsUiState.CollectionTab) : MenuGoodsAction
    data class SelectCategory(val categoryId: String?) : MenuGoodsAction
    data class ToggleItemAvailability(val itemId: String) : MenuGoodsAction
    data class ClickEditItem(val itemId: String) : MenuGoodsAction
    data class ClickDeleteItem(val itemId: String) : MenuGoodsAction
    data class ConfirmDeleteItem(val itemId: String) : MenuGoodsAction
    data object CancelDeleteItem : MenuGoodsAction
    data object ClickAddNewItem : MenuGoodsAction
    data object DismissInfoMessage : MenuGoodsAction
}
