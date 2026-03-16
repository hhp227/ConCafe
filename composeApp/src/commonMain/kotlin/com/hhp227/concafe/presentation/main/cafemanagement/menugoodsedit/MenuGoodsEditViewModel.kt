package com.hhp227.concafe.presentation.main.cafemanagement.menugoodsedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.CafeMenuGoodsUpsert
import com.hhp227.concafe.domain.model.CafeMenu
import com.hhp227.concafe.domain.model.Goods
import com.hhp227.concafe.domain.usecase.GetCafeDetailUseCase
import com.hhp227.concafe.domain.usecase.UpsertCafeMenuGoodsUseCase

class MenuGoodsEditViewModel(
    private val cafeId: String,
    private val itemId: String?,
    private val getCafeDetailUseCase: GetCafeDetailUseCase,
    private val upsertCafeMenuGoodsUseCase: UpsertCafeMenuGoodsUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(MenuGoodsEditUiState())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<MenuGoodsEditEvent>(replay = 0)
    val event = _event.asSharedFlow()

    private fun loadInitialValue() {
        if (itemId == null) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isEditMode = false,
                    screenTitle = "새 항목 추가",
                    saveButtonLabel = "항목 생성"
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, infoMessage = null) }
            when (val result = getCafeDetailUseCase.invoke(cafeId)) {
                is AppResult.Success -> {
                    val detail = result.data.detail
                    val menu = detail.menus.firstOrNull { it.id == itemId }
                    val goods = detail.goods.firstOrNull { it.id == itemId }

                    when {
                        menu != null -> applyMenu(menu)
                        goods != null -> applyGoods(goods)
                        else -> showInfoAndStop("편집할 항목 정보를 찾을 수 없습니다.")
                    }
                }
                is AppResult.Failure -> showInfoAndStop("항목 정보를 불러오지 못했습니다.")
            }
        }
    }

    private fun clickBack() {
        viewModelScope.launch {
            _event.emit(MenuGoodsEditEvent.NavigateBack)
        }
    }

    private fun clickSave() {
        val currentState = _uiState.value
        when {
            currentState.itemName.isBlank() -> showInfo("항목 이름을 입력해주세요.")
            currentState.price.isBlank() -> showInfo("가격을 입력해주세요.")
            currentState.price.toIntOrNull() == null -> showInfo("가격은 숫자로 입력해주세요.")
            else -> {
                _uiState.update { it.copy(infoMessage = null) }
                viewModelScope.launch {
                    when (
                        val result = upsertCafeMenuGoodsUseCase.invoke(
                            CafeMenuGoodsUpsert(
                                cafeId = cafeId,
                                itemId = itemId,
                                name = currentState.itemName.trim(),
                                price = currentState.price.toInt(),
                                category = currentState.selectedCategory.toCategoryId(),
                                description = currentState.description.trim(),
                                isInStock = currentState.isInStock,
                                imageUrl = currentState.imageUrl
                            )
                        )
                    ) {
                        is AppResult.Success -> {
                            _event.emit(MenuGoodsEditEvent.NavigateBack)
                        }
                        is AppResult.Failure -> showInfo("항목 저장에 실패했습니다.")
                    }
                }
            }
        }
    }

    private fun showInfo(message: String) {
        _uiState.update { it.copy(infoMessage = message) }
    }

    private fun applyMenu(menu: CafeMenu) {
        _uiState.update {
            it.copy(
                isLoading = false,
                isEditMode = true,
                screenTitle = "항목 편집",
                saveButtonLabel = "항목 저장",
                itemName = menu.name,
                price = menu.price.toString(),
                selectedCategory = menu.category.toItemCategory(),
                description = menu.desc,
                isInStock = menu.isAvailable,
                imageUrl = menu.image
            )
        }
    }

    private fun applyGoods(goods: Goods) {
        _uiState.update {
            it.copy(
                isLoading = false,
                isEditMode = true,
                screenTitle = "항목 편집",
                saveButtonLabel = "항목 저장",
                itemName = goods.name,
                price = goods.price.toString(),
                selectedCategory = MenuGoodsEditUiState.ItemCategory.GOODS,
                description = "카페 굿즈 판매 항목",
                isInStock = goods.stock > 0,
                imageUrl = goods.image
            )
        }
    }

    private fun showInfoAndStop(message: String) {
        _uiState.update {
            it.copy(
                isLoading = false,
                infoMessage = message
            )
        }
    }

    private fun String.toItemCategory(): MenuGoodsEditUiState.ItemCategory {
        return when (lowercase()) {
            "food" -> MenuGoodsEditUiState.ItemCategory.FOOD
            "dessert" -> MenuGoodsEditUiState.ItemCategory.DESSERT
            "goods" -> MenuGoodsEditUiState.ItemCategory.GOODS
            else -> MenuGoodsEditUiState.ItemCategory.DRINK
        }
    }

    private fun MenuGoodsEditUiState.ItemCategory.toCategoryId(): String {
        return when (this) {
            MenuGoodsEditUiState.ItemCategory.DRINK -> "drink"
            MenuGoodsEditUiState.ItemCategory.FOOD -> "food"
            MenuGoodsEditUiState.ItemCategory.DESSERT -> "dessert"
            MenuGoodsEditUiState.ItemCategory.GOODS -> "goods"
        }
    }

    fun onAction(action: MenuGoodsEditAction) {
        when (action) {
            MenuGoodsEditAction.ClickBack -> clickBack()
            MenuGoodsEditAction.ClickPhotoUpload -> showInfo("이미지 업로드는 다음 단계에서 연결됩니다.")
            is MenuGoodsEditAction.ChangeName -> _uiState.update { it.copy(itemName = action.value) }
            is MenuGoodsEditAction.ChangePrice -> _uiState.update { it.copy(price = action.value.filter(Char::isDigit)) }
            is MenuGoodsEditAction.SelectPhoto -> _uiState.update { it.copy(imageUrl = action.imageUrl) }
            is MenuGoodsEditAction.SelectCategory -> _uiState.update { it.copy(selectedCategory = action.category) }
            is MenuGoodsEditAction.ChangeDescription -> _uiState.update { it.copy(description = action.value) }
            is MenuGoodsEditAction.ToggleStock -> _uiState.update { it.copy(isInStock = action.isInStock) }
            MenuGoodsEditAction.ClickSave -> clickSave()
            MenuGoodsEditAction.DismissInfoMessage -> _uiState.update { it.copy(infoMessage = null) }
        }
    }

    init {
        loadInitialValue()
    }
}
