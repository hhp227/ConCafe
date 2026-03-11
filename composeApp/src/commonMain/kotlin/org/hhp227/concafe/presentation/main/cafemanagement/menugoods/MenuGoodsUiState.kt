package org.hhp227.concafe.presentation.main.cafemanagement.menugoods

data class MenuGoodsUiState(
    val cafeName: String = "",
    val isLoading: Boolean = true,
    val isSearchVisible: Boolean = false,
    val searchQuery: String = "",
    val selectedCollection: CollectionTab = CollectionTab.MENU,
    val menuCategories: List<CategoryChip> = emptyList(),
    val goodsCategories: List<CategoryChip> = emptyList(),
    val selectedMenuCategoryId: String? = null,
    val selectedGoodsCategoryId: String? = null,
    val menuItems: List<ManageItem> = emptyList(),
    val goodsItems: List<ManageItem> = emptyList(),
    val infoMessage: String? = null,
    val pendingDeleteItem: ManageItem? = null
) {
    enum class CollectionTab {
        MENU,
        GOODS
    }

    data class CategoryChip(
        val id: String?,
        val label: String,
        val iconKey: String
    )

    data class ManageItem(
        val id: String,
        val name: String,
        val priceText: String,
        val description: String,
        val imageUrl: String?,
        val badgeLabel: String,
        val categoryId: String?,
        val categoryLabel: String,
        val isAvailable: Boolean,
        val availabilityLabel: String,
        val inventoryLabel: String? = null
    )

    val visibleCategories: List<CategoryChip>
        get() = when (selectedCollection) {
            CollectionTab.MENU -> menuCategories
            CollectionTab.GOODS -> goodsCategories
        }

    val selectedCategoryId: String?
        get() = when (selectedCollection) {
            CollectionTab.MENU -> selectedMenuCategoryId
            CollectionTab.GOODS -> selectedGoodsCategoryId
        }

    val visibleItems: List<ManageItem>
        get() = when (selectedCollection) {
            CollectionTab.MENU -> menuItems
            CollectionTab.GOODS -> goodsItems
        }

    val filteredVisibleItems: List<ManageItem>
        get() = visibleItems.filter { item ->
            val matchesCategory = selectedCategoryId == null || item.categoryId == selectedCategoryId
            val normalizedQuery = searchQuery.trim()
            val matchesQuery = normalizedQuery.isBlank() ||
                item.name.contains(normalizedQuery, ignoreCase = true) ||
                item.description.contains(normalizedQuery, ignoreCase = true) ||
                item.categoryLabel.contains(normalizedQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
}
