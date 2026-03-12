package com.hhp227.concafe.presentation.main.cafemanagement.cafeinfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.CafeInfoUpdate
import com.hhp227.concafe.domain.usecase.GetCafeDetailUseCase
import com.hhp227.concafe.domain.usecase.UpdateCafeInfoUseCase

class CafeInfoEditViewModel(
    private val cafeId: String,
    private val getCafeDetailUseCase: GetCafeDetailUseCase,
    private val updateCafeInfoUseCase: UpdateCafeInfoUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(CafeInfoEditUiState())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<CafeInfoEvent>(replay = 0)
    val event = _event.asSharedFlow()

    private fun loadCafeInfo() {
        _uiState.update {
            it.copy(
                isLoading = true,
                infoMessage = null
            )
        }
        viewModelScope.launch {
            when (val result = getCafeDetailUseCase.invoke(cafeId)) {
                is AppResult.Success -> {
                    val detail = result.data.detail
                    val parsedHours = parseBusinessHours(detail.businessHours)
                    _uiState.update {
                        it.copy(
                            detail = detail,
                            isLoading = false,
                            cafeName = detail.cafe.name,
                            cafeDescription = detail.cafe.desc,
                            representativeImageUrl = detail.images.firstOrNull()?.takeIf { image -> image.isNotBlank() }
                                ?: detail.cafe.thumbnailImage,
                            galleryImages = detail.images.filter { image -> image.isNotBlank() },
                            address = detail.cafe.region.address,
                            contactNumber = detail.phoneNumber,
                            weekdayOpen = parsedHours.weekdayOpen,
                            weekdayClose = parsedHours.weekdayClose,
                            weekendOpen = parsedHours.weekendOpen,
                            weekendClose = parsedHours.weekendClose,
                            infoMessage = null
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            detail = null,
                            isLoading = false,
                            infoMessage = "카페 정보를 불러오지 못했습니다."
                        )
                    }
                }
            }
        }
    }

    private fun saveCafeInfo() {
        val currentState = _uiState.value
        _uiState.update { it.copy(isSaving = true, infoMessage = null) }

        viewModelScope.launch {
            when (
                val result = updateCafeInfoUseCase.invoke(
                    CafeInfoUpdate(
                        cafeId = cafeId,
                        name = currentState.cafeName,
                        description = currentState.cafeDescription,
                        address = currentState.address,
                        contactNumber = currentState.contactNumber,
                        weekdayOpen = currentState.weekdayOpen,
                        weekdayClose = currentState.weekdayClose,
                        weekendOpen = currentState.weekendOpen,
                        weekendClose = currentState.weekendClose
                    )
                )
            ) {
                is AppResult.Success -> {
                    val detail = result.data
                    val parsedHours = parseBusinessHours(detail.businessHours)
                    _uiState.update {
                        it.copy(
                            detail = detail,
                            isSaving = false,
                            cafeName = detail.cafe.name,
                            cafeDescription = detail.cafe.desc,
                            representativeImageUrl = detail.images.firstOrNull()?.takeIf { image -> image.isNotBlank() }
                                ?: detail.cafe.thumbnailImage,
                            galleryImages = detail.images.filter { image -> image.isNotBlank() },
                            address = detail.cafe.region.address,
                            contactNumber = detail.phoneNumber,
                            weekdayOpen = parsedHours.weekdayOpen,
                            weekdayClose = parsedHours.weekdayClose,
                            weekendOpen = parsedHours.weekendOpen,
                            weekendClose = parsedHours.weekendClose,
                            infoMessage = null
                        )
                    }
                    _event.emit(CafeInfoEvent.ShowSaveSuccessMessage)
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            infoMessage = "카페 정보 저장에 실패했습니다."
                        )
                    }
                }
            }
        }
    }

    fun onAction(action: CafeInfoEditAction) {
        when (action) {
            CafeInfoEditAction.ClickBack -> {
                viewModelScope.launch { _event.emit(CafeInfoEvent.NavigateBack) }
            }
            is CafeInfoEditAction.ChangeCafeName -> _uiState.update { it.copy(cafeName = action.value) }
            is CafeInfoEditAction.ChangeCafeDescription -> _uiState.update { it.copy(cafeDescription = action.value) }
            is CafeInfoEditAction.ChangeAddress -> _uiState.update { it.copy(address = action.value) }
            is CafeInfoEditAction.ChangeContactNumber -> _uiState.update { it.copy(contactNumber = action.value) }
            is CafeInfoEditAction.ChangeWeekdayOpen -> _uiState.update { it.copy(weekdayOpen = action.value) }
            is CafeInfoEditAction.ChangeWeekdayClose -> _uiState.update { it.copy(weekdayClose = action.value) }
            is CafeInfoEditAction.ChangeWeekendOpen -> _uiState.update { it.copy(weekendOpen = action.value) }
            is CafeInfoEditAction.ChangeWeekendClose -> _uiState.update { it.copy(weekendClose = action.value) }
            CafeInfoEditAction.ClickRepresentativeImage -> showInfo("대표 이미지 업로드는 다음 단계에서 연결됩니다.")
            CafeInfoEditAction.ClickAddGalleryImage -> showInfo("갤러리 이미지 추가는 다음 단계에서 연결됩니다.")
            CafeInfoEditAction.ClickPinLocation -> showInfo("지도 핀 위치 조정은 다음 단계에서 연결됩니다.")
            CafeInfoEditAction.ClickManageExceptionDates -> showInfo("예외 영업일 관리는 다음 단계에서 연결됩니다.")
            CafeInfoEditAction.ClickSave -> saveCafeInfo()
            CafeInfoEditAction.DismissInfoMessage -> _uiState.update { it.copy(infoMessage = null) }
        }
    }

    private fun showInfo(message: String) {
        _uiState.update { it.copy(infoMessage = message) }
    }

    private fun parseBusinessHours(businessHours: String): ParsedBusinessHours {
        val timeRange = Regex("""(\d{1,2}:\d{2})\s*-\s*(\d{1,2}:\d{2})""")
            .find(businessHours)
        val open = timeRange?.groupValues?.getOrNull(1).orEmpty()
        val close = timeRange?.groupValues?.getOrNull(2).orEmpty()
        return ParsedBusinessHours(
            weekdayOpen = open,
            weekdayClose = close,
            weekendOpen = open,
            weekendClose = close
        )
    }

    private data class ParsedBusinessHours(
        val weekdayOpen: String,
        val weekdayClose: String,
        val weekendOpen: String,
        val weekendClose: String
    )

    init {
        loadCafeInfo()
    }
}
