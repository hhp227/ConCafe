package org.hhp227.concafe.presentation.main.cafemanagement.menugoodsedit

data class MenuGoodsEditUiState(
    val isLoading: Boolean = true,
    val isEditMode: Boolean = false,
    val screenTitle: String = "새 항목 추가",
    val saveButtonLabel: String = "항목 생성",
    val itemName: String = "",
    val price: String = "",
    val selectedCategory: ItemCategory = ItemCategory.DRINK,
    val description: String = "",
    val isInStock: Boolean = true,
    val imageUrl: String? = null,
    val infoMessage: String? = null
) {
    enum class ItemCategory(
        val label: String,
        val iconKey: String
    ) {
        DRINK("Drink", "drink"),
        FOOD("Food", "food"),
        DESSERT("Dessert", "dessert"),
        GOODS("Goods", "goods")
    }
}
