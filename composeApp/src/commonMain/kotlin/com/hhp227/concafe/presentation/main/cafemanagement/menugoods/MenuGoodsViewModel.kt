package com.hhp227.concafe.presentation.main.cafemanagement.menugoods

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.event.CafeDetailEvent
import com.hhp227.concafe.domain.event.publisher.CafeDetailEventPublisher
import com.hhp227.concafe.domain.model.CafeDetail
import com.hhp227.concafe.domain.model.CafeMenu
import com.hhp227.concafe.domain.model.Goods
import com.hhp227.concafe.domain.usecase.DeleteCafeMenuGoodsUseCase
import com.hhp227.concafe.domain.usecase.GetCafeDetailUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MenuGoodsViewModel(
    private val cafeId: String,
    private val getCafeDetailUseCase: GetCafeDetailUseCase,
    private val deleteCafeMenuGoodsUseCase: DeleteCafeMenuGoodsUseCase,
    private val cafeDetailEventPublisher: CafeDetailEventPublisher
) : ViewModel() {
    private val _uiState = MutableStateFlow(MenuGoodsUiState())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<MenuGoodsEvent>(replay = 0)
    val event = _event.asSharedFlow()

    private val jobs = mutableMapOf<JobKey, Job>()

    private fun loadMenuGoods() {
        jobs[JobKey.LOAD]?.cancel()
        jobs[JobKey.LOAD] = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, infoMessage = null) }
            when (val result = getCafeDetailUseCase.invoke(cafeId)) {
                is AppResult.Success -> applyDetail(result.data.detail)
                is AppResult.Failure -> {
                    _uiState.update { it.copy(isLoading = false, infoMessage = "항목 정보를 불러오지 못했습니다.") }
                }
            }
        }
    }

    private fun observeCafeDetailEvent() {
        jobs[JobKey.OBSERVE_EVENT]?.cancel()
        jobs[JobKey.OBSERVE_EVENT] = viewModelScope.launch {
            cafeDetailEventPublisher.observe().collect { event ->
                when (event) {
                    is CafeDetailEvent.CafeInfoUpdated -> if (event.cafeId == cafeId) {
                        loadMenuGoods()
                    }
                    is CafeDetailEvent.MenuCreated -> if (event.cafeId == cafeId) {
                        loadMenuGoods()
                    }
                    is CafeDetailEvent.MenuUpdated -> if (event.cafeId == cafeId) {
                        upsertLocalMenu(event.menu)
                    }
                    is CafeDetailEvent.MenuDeleted -> if (event.cafeId == cafeId) {
                        removeLocalMenu(event.itemId)
                    }
                    is CafeDetailEvent.GoodsCreated -> if (event.cafeId == cafeId) {
                        loadMenuGoods()
                    }
                    is CafeDetailEvent.GoodsUpdated -> if (event.cafeId == cafeId) {
                        upsertLocalGoods(event.goods)
                    }
                    is CafeDetailEvent.GoodsDeleted -> if (event.cafeId == cafeId) {
                        removeLocalGoods(event.itemId)
                    }
                }
            }
        }
    }

    private fun applyDetail(detail: CafeDetail) {
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
                goodsItems = goodsItems
            )
        }
    }

    private fun upsertLocalMenu(menu: CafeMenu) {
        _uiState.update { state ->
            val nextMenuItems = state.menuItems.filterNot { it.id == menu.id } + menu.toManageItem(state.menuItems.size)
            val nextGoodsItems = state.goodsItems.filterNot { it.id == menu.id }
            state.copy(
                isLoading = false,
                menuCategories = buildMenuCategories(nextMenuItems),
                goodsCategories = buildGoodsCategories(nextGoodsItems),
                menuItems = nextMenuItems,
                goodsItems = nextGoodsItems,
                pendingDeleteItem = state.pendingDeleteItem?.takeUnless { it.id == menu.id }
            )
        }
    }

    private fun upsertLocalGoods(goods: Goods) {
        _uiState.update { state ->
            val nextMenuItems = state.menuItems.filterNot { it.id == goods.id }
            val nextGoodsItems = state.goodsItems.filterNot { it.id == goods.id } + goods.toManageItem(state.goodsItems.size)
            state.copy(
                isLoading = false,
                menuCategories = buildMenuCategories(nextMenuItems),
                goodsCategories = buildGoodsCategories(nextGoodsItems),
                menuItems = nextMenuItems,
                goodsItems = nextGoodsItems,
                pendingDeleteItem = state.pendingDeleteItem?.takeUnless { it.id == goods.id }
            )
        }
    }

    private fun removeLocalMenu(itemId: String) {
        _uiState.update { state ->
            val nextMenuItems = state.menuItems.filterNot { it.id == itemId }
            state.copy(
                isLoading = false,
                menuCategories = buildMenuCategories(nextMenuItems),
                goodsCategories = buildGoodsCategories(state.goodsItems),
                menuItems = nextMenuItems,
                goodsItems = state.goodsItems,
                pendingDeleteItem = state.pendingDeleteItem?.takeUnless { it.id == itemId }
            )
        }
    }

    private fun removeLocalGoods(itemId: String) {
        _uiState.update { state ->
            val nextGoodsItems = state.goodsItems.filterNot { it.id == itemId }
            state.copy(
                isLoading = false,
                menuCategories = buildMenuCategories(state.menuItems),
                goodsCategories = buildGoodsCategories(nextGoodsItems),
                menuItems = state.menuItems,
                goodsItems = nextGoodsItems,
                pendingDeleteItem = state.pendingDeleteItem?.takeUnless { it.id == itemId }
            )
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

    private fun confirmDeleteItem(itemId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(infoMessage = null, pendingDeleteItem = null) }
            when (deleteCafeMenuGoodsUseCase.invoke(cafeId = cafeId, itemId = itemId)) {
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
            is MenuGoodsAction.ConfirmDeleteItem -> confirmDeleteItem(action.itemId)
            MenuGoodsAction.CancelDeleteItem -> cancelDeleteItem()
            MenuGoodsAction.ClickAddNewItem -> clickAddNewItem()
            MenuGoodsAction.DismissInfoMessage -> dismissInfoMessage()
        }
    }

    init {
        observeCafeDetailEvent()
        loadMenuGoods()
    }

    override fun onCleared() {
        jobs.values.forEach(Job::cancel)
        jobs.clear()
        super.onCleared()
    }

    private enum class JobKey {
        LOAD,
        OBSERVE_EVENT
    }
}
