package org.hhp227.concafe.presentation.main.cafemanagement.menugoodsedit

sealed interface MenuGoodsEditAction {
    data object ClickBack : MenuGoodsEditAction
    data object ClickPhotoUpload : MenuGoodsEditAction
    data class ChangeName(val value: String) : MenuGoodsEditAction
    data class ChangePrice(val value: String) : MenuGoodsEditAction
    data class SelectCategory(val category: MenuGoodsEditUiState.ItemCategory) : MenuGoodsEditAction
    data class ChangeDescription(val value: String) : MenuGoodsEditAction
    data class ToggleStock(val isInStock: Boolean) : MenuGoodsEditAction
    data object ClickSave : MenuGoodsEditAction
    data object DismissInfoMessage : MenuGoodsEditAction
}
