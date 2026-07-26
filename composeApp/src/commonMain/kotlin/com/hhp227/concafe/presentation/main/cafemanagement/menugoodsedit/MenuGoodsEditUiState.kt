package com.hhp227.concafe.presentation.main.cafemanagement.menugoodsedit

data class MenuGoodsEditUiState(
    val isLoading: Boolean = true,
    val isEditMode: Boolean = false,
    val itemName: String = "",
    val price: String = "",
    val selectedCategoryId: String = "drink",
    val description: String = "",
    val isInStock: Boolean = true,
    val imageUrl: String? = null,
    val infoMessageKey: String? = null
)
