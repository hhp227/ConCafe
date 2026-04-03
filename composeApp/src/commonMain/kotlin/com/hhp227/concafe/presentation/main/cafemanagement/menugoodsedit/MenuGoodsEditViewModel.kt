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
import com.hhp227.concafe.domain.usecase.UploadImageUseCase
import com.hhp227.concafe.domain.usecase.UpsertCafeMenuGoodsUseCase

class MenuGoodsEditViewModel(
    private val cafeId: String,
    private val itemId: String?,
    private val getCafeDetailUseCase: GetCafeDetailUseCase,
    private val upsertCafeMenuGoodsUseCase: UpsertCafeMenuGoodsUseCase,
    private val uploadImageUseCase: UploadImageUseCase
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
                    isEditMode = false
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, infoMessageKey = null) }
            when (val result = getCafeDetailUseCase.invoke(cafeId)) {
                is AppResult.Success -> {
                    val detail = result.data.detail
                    val menu = detail.menus.firstOrNull { it.id == itemId }
                    val goods = detail.goods.firstOrNull { it.id == itemId }

                    when {
                        menu != null -> applyMenu(menu)
                        goods != null -> applyGoods(goods)
                        else -> showInfoAndStop(MSG_ITEM_NOT_FOUND)
                    }
                }
                is AppResult.Failure -> showInfoAndStop(MSG_LOAD_FAILED)
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
            currentState.itemName.isBlank() -> showInfo(MSG_ENTER_NAME)
            currentState.price.isBlank() -> showInfo(MSG_ENTER_PRICE)
            currentState.price.toIntOrNull() == null -> showInfo(MSG_PRICE_NUMBER_ONLY)
            else -> {
                _uiState.update { it.copy(infoMessageKey = null) }
                viewModelScope.launch {
                    val uploadedImageUrl = uploadImage(currentState.imageUrl, "cafe-items")
                    if (!currentState.imageUrl.isNullOrBlank() && uploadedImageUrl == null) {
                        return@launch
                    }
                    when (
                        val result = upsertCafeMenuGoodsUseCase.invoke(
                            CafeMenuGoodsUpsert(
                                cafeId = cafeId,
                                itemId = itemId,
                                name = currentState.itemName.trim(),
                                price = currentState.price.toInt(),
                                category = currentState.selectedCategoryId,
                                description = currentState.description.trim(),
                                isInStock = currentState.isInStock,
                                imageUrl = uploadedImageUrl
                            )
                        )
                    ) {
                        is AppResult.Success -> {
                            _event.emit(MenuGoodsEditEvent.NavigateBack)
                        }
                        is AppResult.Failure -> showInfo(MSG_SAVE_FAILED)
                    }
                }
            }
        }
    }

    private fun showInfo(messageKey: String) {
        _uiState.update { it.copy(infoMessageKey = messageKey) }
    }

    private suspend fun uploadImage(imageUrl: String?, folder: String): String? {
        if (imageUrl.isNullOrBlank()) return null
        return when (val result = uploadImageUseCase.invoke(imageUrl, folder)) {
            is AppResult.Success -> result.data
            is AppResult.Failure -> {
                _uiState.update {
                    it.copy(
                        infoMessageKey = MSG_IMAGE_UPLOAD_FAILED
                    )
                }
                null
            }
        }
    }

    private fun applyMenu(menu: CafeMenu) {
        _uiState.update {
            it.copy(
                isLoading = false,
                isEditMode = true,
                itemName = menu.name,
                price = menu.price.toString(),
                selectedCategoryId = when (menu.category.lowercase()) {
                    "food", "dessert", "goods", "drink" -> menu.category.lowercase()
                    else -> "drink"
                },
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
                itemName = goods.name,
                price = goods.price.toString(),
                selectedCategoryId = "goods",
                description = "카페 굿즈 판매 항목",
                isInStock = goods.stock > 0,
                imageUrl = goods.image
            )
        }
    }

    private fun showInfoAndStop(messageKey: String) {
        _uiState.update {
            it.copy(
                isLoading = false,
                infoMessageKey = messageKey
            )
        }
    }

    fun onAction(action: MenuGoodsEditAction) {
        when (action) {
            MenuGoodsEditAction.ClickBack -> clickBack()
            MenuGoodsEditAction.ClickPhotoUpload -> showInfo(MSG_IMAGE_UPLOAD_NEXT_STEP)
            is MenuGoodsEditAction.ChangeName -> _uiState.update { it.copy(itemName = action.value) }
            is MenuGoodsEditAction.ChangePrice -> _uiState.update { it.copy(price = action.value.filter(Char::isDigit)) }
            is MenuGoodsEditAction.SelectPhoto -> _uiState.update { it.copy(imageUrl = action.imageUrl) }
            is MenuGoodsEditAction.SelectCategory -> _uiState.update { it.copy(selectedCategoryId = action.categoryId) }
            is MenuGoodsEditAction.ChangeDescription -> _uiState.update { it.copy(description = action.value) }
            is MenuGoodsEditAction.ToggleStock -> _uiState.update { it.copy(isInStock = action.isInStock) }
            MenuGoodsEditAction.ClickSave -> clickSave()
            MenuGoodsEditAction.DismissInfoMessage -> _uiState.update { it.copy(infoMessageKey = null) }
        }
    }

    init {
        loadInitialValue()
    }

    companion object {
        private const val MSG_ITEM_NOT_FOUND = "menugoods_edit_info_item_not_found"
        private const val MSG_LOAD_FAILED = "menugoods_edit_info_load_failed"
        private const val MSG_ENTER_NAME = "menugoods_edit_info_enter_name"
        private const val MSG_ENTER_PRICE = "menugoods_edit_info_enter_price"
        private const val MSG_PRICE_NUMBER_ONLY = "menugoods_edit_info_price_number_only"
        private const val MSG_SAVE_FAILED = "menugoods_edit_info_save_failed"
        private const val MSG_IMAGE_UPLOAD_FAILED = "menugoods_edit_info_image_upload_failed"
        private const val MSG_IMAGE_UPLOAD_NEXT_STEP = "menugoods_edit_info_image_upload_next_step"
    }
}
