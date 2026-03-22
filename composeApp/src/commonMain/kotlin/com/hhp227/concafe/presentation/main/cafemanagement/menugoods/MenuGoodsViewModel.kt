package com.hhp227.concafe.presentation.main.cafemanagement.menugoods

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.event.CafeDetailEvent
import com.hhp227.concafe.domain.event.publisher.CafeDetailEventPublisher
import com.hhp227.concafe.domain.model.CafeDetail
import com.hhp227.concafe.domain.model.CafeMenu
import com.hhp227.concafe.domain.model.CafeMenuGoodsUpsert
import com.hhp227.concafe.domain.model.Goods
import com.hhp227.concafe.domain.usecase.DeleteCafeMenuGoodsUseCase
import com.hhp227.concafe.domain.usecase.GetCafeDetailUseCase
import com.hhp227.concafe.domain.usecase.UpsertCafeMenuGoodsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MenuGoodsViewModel(
    private val cafeId: String,
    private val getCafeDetailUseCase: GetCafeDetailUseCase,
    private val upsertCafeMenuGoodsUseCase: UpsertCafeMenuGoodsUseCase,
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
            cafeDetailEventPublisher.events.collect { event ->
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
        _uiState.update {
            it.copy(
                cafeName = detail.cafe.name,
                isLoading = false,
                menuCategories = buildMenuCategories(detail.menus),
                goodsCategories = buildGoodsCategories(detail.goods),
                menuItems = detail.menus,
                goodsItems = detail.goods,
                menuAvailabilityOverrides = emptyMap(),
                goodsAvailabilityOverrides = emptyMap(),
                infoMessage = null
            )
        }
    }

    private fun upsertLocalMenu(menu: CafeMenu) {
        _uiState.update { state ->
            val nextMenuItems = state.menuItems.filterNot { it.id == menu.id } + menu
            val nextGoodsItems = state.goodsItems.filterNot { it.id == menu.id }
            state.copy(
                isLoading = false,
                menuCategories = buildMenuCategories(nextMenuItems),
                goodsCategories = buildGoodsCategories(nextGoodsItems),
                menuItems = nextMenuItems,
                goodsItems = nextGoodsItems,
                menuAvailabilityOverrides = state.menuAvailabilityOverrides - menu.id,
                goodsAvailabilityOverrides = state.goodsAvailabilityOverrides - menu.id,
                pendingDeleteItemId = state.pendingDeleteItemId.takeUnless { it == menu.id }
            )
        }
    }

    private fun upsertLocalGoods(goods: Goods) {
        _uiState.update { state ->
            val nextMenuItems = state.menuItems.filterNot { it.id == goods.id }
            val nextGoodsItems = state.goodsItems.filterNot { it.id == goods.id } + goods
            state.copy(
                isLoading = false,
                menuCategories = buildMenuCategories(nextMenuItems),
                goodsCategories = buildGoodsCategories(nextGoodsItems),
                menuItems = nextMenuItems,
                goodsItems = nextGoodsItems,
                menuAvailabilityOverrides = state.menuAvailabilityOverrides - goods.id,
                goodsAvailabilityOverrides = state.goodsAvailabilityOverrides - goods.id,
                pendingDeleteItemId = state.pendingDeleteItemId.takeUnless { it == goods.id }
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
                menuAvailabilityOverrides = state.menuAvailabilityOverrides - itemId,
                goodsAvailabilityOverrides = state.goodsAvailabilityOverrides - itemId,
                pendingDeleteItemId = state.pendingDeleteItemId.takeUnless { it == itemId }
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
                menuAvailabilityOverrides = state.menuAvailabilityOverrides - itemId,
                goodsAvailabilityOverrides = state.goodsAvailabilityOverrides - itemId,
                pendingDeleteItemId = state.pendingDeleteItemId.takeUnless { it == itemId }
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
        when (_uiState.value.selectedCollection) {
            MenuGoodsUiState.CollectionTab.MENU -> {
                val target = _uiState.value.menuItems.firstOrNull { it.id == itemId } ?: return
                val nextAvailability = !_uiState.value.isMenuAvailable(target)
                _uiState.update { state ->
                    state.copy(
                        menuAvailabilityOverrides = state.menuAvailabilityOverrides + (itemId to nextAvailability),
                        infoMessage = null
                    )
                }
                jobs[JobKey.TOGGLE_AVAILABILITY]?.cancel()
                jobs[JobKey.TOGGLE_AVAILABILITY] = viewModelScope.launch {
                    when (
                        upsertCafeMenuGoodsUseCase.invoke(
                            CafeMenuGoodsUpsert(
                                cafeId = cafeId,
                                itemId = target.id,
                                name = target.name,
                                price = target.price,
                                category = target.category,
                                description = target.desc,
                                isInStock = nextAvailability,
                                imageUrl = target.image
                            )
                        )
                    ) {
                        is AppResult.Success -> Unit
                        is AppResult.Failure -> {
                            _uiState.update { state ->
                                state.copy(
                                    menuAvailabilityOverrides = state.menuAvailabilityOverrides - itemId,
                                    infoMessage = "판매 상태 저장에 실패했습니다."
                                )
                            }
                        }
                    }
                }
            }
            MenuGoodsUiState.CollectionTab.GOODS -> {
                val target = _uiState.value.goodsItems.firstOrNull { it.id == itemId } ?: return
                val nextAvailability = !_uiState.value.isGoodsAvailable(target)
                _uiState.update { state ->
                    state.copy(
                        goodsAvailabilityOverrides = state.goodsAvailabilityOverrides + (itemId to nextAvailability),
                        infoMessage = null
                    )
                }
                jobs[JobKey.TOGGLE_AVAILABILITY]?.cancel()
                jobs[JobKey.TOGGLE_AVAILABILITY] = viewModelScope.launch {
                    when (
                        upsertCafeMenuGoodsUseCase.invoke(
                            CafeMenuGoodsUpsert(
                                cafeId = cafeId,
                                itemId = target.id,
                                name = target.name,
                                price = target.price,
                                category = "goods",
                                description = "카페 굿즈 판매 항목",
                                isInStock = nextAvailability,
                                imageUrl = target.image
                            )
                        )
                    ) {
                        is AppResult.Success -> Unit
                        is AppResult.Failure -> {
                            _uiState.update { state ->
                                state.copy(
                                    goodsAvailabilityOverrides = state.goodsAvailabilityOverrides - itemId,
                                    infoMessage = "판매 상태 저장에 실패했습니다."
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun clickDeleteItem(itemId: String) {
        _uiState.update { it.copy(pendingDeleteItemId = itemId) }
    }

    private fun confirmDeleteItem(itemId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(infoMessage = null, pendingDeleteItemId = null) }
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
        _uiState.update { it.copy(pendingDeleteItemId = null) }
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
        items: List<CafeMenu>
    ): List<MenuGoodsUiState.CategoryChip> {
        val preferredOrder = listOf(
            "food" to MenuGoodsUiState.CategoryChip("food", "Food", "food"),
            "drink" to MenuGoodsUiState.CategoryChip("drink", "Drinks", "drink"),
            "dessert" to MenuGoodsUiState.CategoryChip("dessert", "Dessert", "dessert")
        )
        return listOf(MenuGoodsUiState.CategoryChip(null, "All", "all")) +
            preferredOrder.mapNotNull { (id, chip) ->
                chip.takeIf { items.any { menu -> menu.category.lowercase() == id } }
            }
    }

    private fun buildGoodsCategories(
        items: List<Goods>
    ): List<MenuGoodsUiState.CategoryChip> {
        val dynamic = items
            .map { item ->
                val categoryId = when {
                    item.name.contains("포토", ignoreCase = true) -> "collectible"
                    item.name.contains("의상", ignoreCase = true) -> "apparel"
                    else -> "goods"
                }
                MenuGoodsUiState.CategoryChip(
                    id = categoryId,
                    label = when (categoryId) {
                        "collectible" -> "Collectible"
                        "apparel" -> "Apparel"
                        else -> "Goods"
                    },
                    iconKey = "goods"
                )
            }
            .distinctBy { it.id }
        return listOf(MenuGoodsUiState.CategoryChip(null, "All", "all")) + dynamic
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
        OBSERVE_EVENT,
        TOGGLE_AVAILABILITY
    }
}
