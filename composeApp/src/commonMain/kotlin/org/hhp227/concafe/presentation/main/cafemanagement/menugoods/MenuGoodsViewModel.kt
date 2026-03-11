package org.hhp227.concafe.presentation.main.cafemanagement.menugoods

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.hhp227.concafe.domain.common.AppResult
import org.hhp227.concafe.domain.model.CafeMenu
import org.hhp227.concafe.domain.model.Goods
import org.hhp227.concafe.domain.usecase.DeleteCafeMenuGoodsUseCase
import org.hhp227.concafe.domain.usecase.ObserveCafeDetailUseCase

class MenuGoodsViewModel(
    private val cafeId: String,
    private val observeCafeDetailUseCase: ObserveCafeDetailUseCase,
    private val deleteCafeMenuGoodsUseCase: DeleteCafeMenuGoodsUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(MenuGoodsUiState())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<MenuGoodsEvent>(replay = 0)
    val event = _event.asSharedFlow()

    private fun loadMenuGoods() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, infoMessage = null) }
            observeCafeDetailUseCase(cafeId).collectLatest { detail ->
                val menuItems = detail.menus.mapIndexed { index, menu ->
                    menu.toManageItem(index)
                }
                val goodsItems = detail.goods.mapIndexed { index, goods ->
                    goods.toManageItem(index)
                }
                _uiState.update {
                    it.copy(
                        cafeName = detail.cafe.name,
                        isLoading = false,
                        menuCategories = buildMenuCategories(menuItems),
                        goodsCategories = buildGoodsCategories(goodsItems),
                        menuItems = menuItems,
                        goodsItems = goodsItems,
                        infoMessage = null
                    )
                }
            }
        }
    }

    private fun clickBack() {
        viewModelScope.launch {
            _event.emit(MenuGoodsEvent.NavigateBack)
        }
    }

    private fun toggleSearch() {
        _uiState.update { state ->
            state.copy(
                isSearchVisible = !state.isSearchVisible,
                searchQuery = if (state.isSearchVisible) "" else state.searchQuery
            )
        }
    }

    private fun changeSearchQuery(value: String) {
        _uiState.update { it.copy(searchQuery = value) }
    }

    private fun selectCollection(collection: MenuGoodsUiState.CollectionTab) {
        _uiState.update { it.copy(selectedCollection = collection) }
    }

    private fun selectCategory(categoryId: String?) {
        _uiState.update { state ->
            when (state.selectedCollection) {
                MenuGoodsUiState.CollectionTab.MENU -> state.copy(selectedMenuCategoryId = categoryId)
                MenuGoodsUiState.CollectionTab.GOODS -> state.copy(selectedGoodsCategoryId = categoryId)
            }
        }
    }

    private fun toggleItemAvailability(itemId: String) {
        _uiState.update { state ->
            when (state.selectedCollection) {
                MenuGoodsUiState.CollectionTab.MENU -> state.copy(
                    menuItems = state.menuItems.toggleAvailability(itemId)
                )
                MenuGoodsUiState.CollectionTab.GOODS -> state.copy(
                    goodsItems = state.goodsItems.toggleAvailability(itemId)
                )
            }
        }
    }

    private fun clickDeleteItem(itemId: String) {
        val targetItem = _uiState.value.visibleItems.firstOrNull { it.id == itemId } ?: return
        _uiState.update { it.copy(pendingDeleteItem = targetItem) }
    }

    private fun confirmDeleteItem() {
        val targetItem = _uiState.value.pendingDeleteItem ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(infoMessage = null, pendingDeleteItem = null) }
            when (deleteCafeMenuGoodsUseCase.invoke(cafeId = cafeId, itemId = targetItem.id)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(infoMessage = "항목이 삭제되었습니다.") }
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(infoMessage = "항목 삭제에 실패했습니다.") }
                }
            }
        }
    }

    private fun cancelDeleteItem() {
        _uiState.update { it.copy(pendingDeleteItem = null) }
    }

    private fun dismissInfoMessage() {
        _uiState.update { it.copy(infoMessage = null) }
    }

    private fun clickAddNewItem() {
        viewModelScope.launch {
            _event.emit(MenuGoodsEvent.NavigateToEdit(cafeId = cafeId))
        }
    }

    private fun clickEditItem(itemId: String) {
        viewModelScope.launch {
            _event.emit(MenuGoodsEvent.NavigateToEdit(cafeId = cafeId, itemId = itemId))
        }
    }

    private fun buildMenuCategories(
        items: List<MenuGoodsUiState.ManageItem>
    ): List<MenuGoodsUiState.CategoryChip> {
        val preferredOrder = listOf(
            "food" to MenuGoodsUiState.CategoryChip("food", "Food", "food"),
            "drink" to MenuGoodsUiState.CategoryChip("drink", "Drinks", "drink"),
            "dessert" to MenuGoodsUiState.CategoryChip("dessert", "Dessert", "dessert")
        )
        return listOf(MenuGoodsUiState.CategoryChip(null, "All", "all")) +
            preferredOrder.mapNotNull { (id, chip) ->
                chip.takeIf { category -> items.any { it.categoryId == id } }
            }
    }

    private fun buildGoodsCategories(
        items: List<MenuGoodsUiState.ManageItem>
    ): List<MenuGoodsUiState.CategoryChip> {
        val dynamic = items
            .mapNotNull { item ->
                val categoryId = item.categoryId ?: return@mapNotNull null
                MenuGoodsUiState.CategoryChip(
                    id = categoryId,
                    label = item.categoryLabel,
                    iconKey = "goods"
                )
            }
            .distinctBy { it.id }
        return listOf(MenuGoodsUiState.CategoryChip(null, "All", "all")) + dynamic
    }

    private fun List<MenuGoodsUiState.ManageItem>.toggleAvailability(
        itemId: String
    ): List<MenuGoodsUiState.ManageItem> {
        return map { item ->
            if (item.id != itemId) {
                item
            } else {
                val nextAvailability = !item.isAvailable
                item.copy(
                    isAvailable = nextAvailability,
                    availabilityLabel = if (nextAvailability) "판매 중" else "품절"
                )
            }
        }
    }

    private fun CafeMenu.toManageItem(index: Int): MenuGoodsUiState.ManageItem {
        val normalizedCategoryId = category.lowercase()
        val categoryLabel = when (normalizedCategoryId) {
            "food" -> "Food"
            "drink" -> "Drinks"
            "dessert" -> "Dessert"
            else -> category.replaceFirstChar { it.uppercase() }
        }
        return MenuGoodsUiState.ManageItem(
            id = id,
            name = name,
            priceText = formatPrice(price),
            description = desc,
            imageUrl = image,
            badgeLabel = categoryLabel,
            categoryId = normalizedCategoryId,
            categoryLabel = categoryLabel,
            isAvailable = isAvailable,
            availabilityLabel = if (isAvailable) "판매 중" else "품절"
        )
    }

    private fun Goods.toManageItem(index: Int): MenuGoodsUiState.ManageItem {
        val category = when {
            name.contains("포토", ignoreCase = true) -> "collectible"
            name.contains("의상", ignoreCase = true) -> "apparel"
            else -> "goods"
        }
        val categoryLabel = when (category) {
            "collectible" -> "Collectible"
            "apparel" -> "Apparel"
            else -> "Goods"
        }
        val isAvailable = stock > 0
        return MenuGoodsUiState.ManageItem(
            id = id,
            name = name,
            priceText = formatPrice(price),
            description = "카페 굿즈 판매 항목",
            imageUrl = image,
            badgeLabel = categoryLabel,
            categoryId = category,
            categoryLabel = categoryLabel,
            isAvailable = isAvailable,
            availabilityLabel = if (isAvailable) "판매 중" else "품절",
            inventoryLabel = "재고 $stock"
        )
    }

    private fun formatPrice(price: Int): String {
        return buildString {
            append("KRW ")
            append(price.toString().reversed().chunked(3).joinToString(",").reversed())
        }
    }

    fun onAction(action: MenuGoodsAction) {
        when (action) {
            MenuGoodsAction.ClickBack -> clickBack()
            MenuGoodsAction.ClickSearch -> toggleSearch()
            is MenuGoodsAction.ChangeSearchQuery -> changeSearchQuery(action.value)
            is MenuGoodsAction.SelectCollection -> selectCollection(action.collection)
            is MenuGoodsAction.SelectCategory -> selectCategory(action.categoryId)
            is MenuGoodsAction.ToggleItemAvailability -> toggleItemAvailability(action.itemId)
            is MenuGoodsAction.ClickEditItem -> clickEditItem(action.itemId)
            is MenuGoodsAction.ClickDeleteItem -> clickDeleteItem(action.itemId)
            MenuGoodsAction.ConfirmDeleteItem -> confirmDeleteItem()
            MenuGoodsAction.CancelDeleteItem -> cancelDeleteItem()
            MenuGoodsAction.ClickAddNewItem -> clickAddNewItem()
            MenuGoodsAction.DismissInfoMessage -> dismissInfoMessage()
        }
    }

    init {
        loadMenuGoods()
    }
}
