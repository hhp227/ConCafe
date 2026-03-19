package com.hhp227.concafe.presentation.main.cafemanagement.menugoods

import com.hhp227.concafe.domain.model.CafeMenu
import com.hhp227.concafe.domain.model.Goods

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
    val menuItems: List<CafeMenu> = emptyList(),
    val goodsItems: List<Goods> = emptyList(),
    val menuAvailabilityOverrides: Map<String, Boolean> = emptyMap(),
    val goodsAvailabilityOverrides: Map<String, Boolean> = emptyMap(),
    val infoMessage: String? = null,
    val pendingDeleteItemId: String? = null
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

    fun isMenuAvailable(menu: CafeMenu): Boolean {
        return menuAvailabilityOverrides[menu.id] ?: menu.isAvailable
    }

    fun isGoodsAvailable(goods: Goods): Boolean {
        return goodsAvailabilityOverrides[goods.id] ?: (goods.stock > 0)
    }

    fun menuCategoryId(menu: CafeMenu): String = menu.category.lowercase()

    fun menuCategoryLabel(menu: CafeMenu): String {
        return when (menuCategoryId(menu)) {
            "food" -> "Food"
            "drink" -> "Drinks"
            "dessert" -> "Dessert"
            else -> menu.category.replaceFirstChar { it.uppercase() }
        }
    }

    fun goodsCategoryId(goods: Goods): String {
        return when {
            goods.name.contains("포토", ignoreCase = true) -> "collectible"
            goods.name.contains("의상", ignoreCase = true) -> "apparel"
            else -> "goods"
        }
    }

    fun goodsCategoryLabel(goods: Goods): String {
        return when (goodsCategoryId(goods)) {
            "collectible" -> "Collectible"
            "apparel" -> "Apparel"
            else -> "Goods"
        }
    }

    val filteredMenuItems: List<CafeMenu>
        get() {
            val normalizedQuery = searchQuery.trim()
            return menuItems.filter { item ->
                val matchesCategory = selectedMenuCategoryId == null || menuCategoryId(item) == selectedMenuCategoryId
                val matchesQuery = normalizedQuery.isBlank() ||
                    item.name.contains(normalizedQuery, ignoreCase = true) ||
                    item.desc.contains(normalizedQuery, ignoreCase = true) ||
                    menuCategoryLabel(item).contains(normalizedQuery, ignoreCase = true)
                matchesCategory && matchesQuery
            }
        }

    val filteredGoodsItems: List<Goods>
        get() {
            val normalizedQuery = searchQuery.trim()
            return goodsItems.filter { item ->
                val matchesCategory = selectedGoodsCategoryId == null || goodsCategoryId(item) == selectedGoodsCategoryId
                val matchesQuery = normalizedQuery.isBlank() ||
                    item.name.contains(normalizedQuery, ignoreCase = true) ||
                    "카페 굿즈 판매 항목".contains(normalizedQuery, ignoreCase = true) ||
                    goodsCategoryLabel(item).contains(normalizedQuery, ignoreCase = true)
                matchesCategory && matchesQuery
            }
        }
}
