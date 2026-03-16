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
import com.hhp227.concafe.domain.model.CafeRegistrationDraft
import com.hhp227.concafe.domain.model.CafeInfoUpdate
import com.hhp227.concafe.domain.model.GeoPoint
import com.hhp227.concafe.domain.model.Region
import com.hhp227.concafe.domain.usecase.CreateCafeRegistrationClaimUseCase
import com.hhp227.concafe.domain.usecase.GetCafeDetailUseCase
import com.hhp227.concafe.domain.usecase.UpdateCafeInfoUseCase

class CafeInfoEditViewModel(
    private val cafeId: String?,
    private val isRegistrationMode: Boolean,
    private val createCafeRegistrationClaimUseCase: CreateCafeRegistrationClaimUseCase,
    private val getCafeDetailUseCase: GetCafeDetailUseCase,
    private val updateCafeInfoUseCase: UpdateCafeInfoUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(CafeInfoEditUiState(isRegistrationMode = isRegistrationMode))
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<CafeInfoEvent>(replay = 0)
    val event = _event.asSharedFlow()

    private fun loadCafeInfo() {
        val targetCafeId = cafeId ?: return
        _uiState.update {
            it.copy(
                isLoading = true,
                infoMessage = null
            )
        }
        viewModelScope.launch {
            when (val result = getCafeDetailUseCase.invoke(targetCafeId)) {
                is AppResult.Success -> {
                    val detail = result.data.detail
                    val parsedHours = parseBusinessHours(detail.businessHours)
                    val cafeImages = detail.images.filter { image -> image.isNotBlank() }
                    _uiState.update {
                        it.copy(
                            detail = detail,
                            isLoading = false,
                            cafeName = detail.cafe.name,
                            cafeDescription = detail.cafe.desc,
                            representativeImageUrl = cafeImages.firstOrNull() ?: detail.cafe.thumbnailImage,
                            galleryImages = cafeImages.drop(1).take(_uiState.value.galleryMaxCount),
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
        if (isRegistrationMode) {
            submitCafeRegistration()
            return
        }

        val targetCafeId = cafeId ?: return
        val currentState = _uiState.value
        _uiState.update { it.copy(isSaving = true, infoMessage = null) }

        viewModelScope.launch {
            when (
                val result = updateCafeInfoUseCase.invoke(
                    CafeInfoUpdate(
                        cafeId = targetCafeId,
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
                    val cafeImages = detail.images.filter { image -> image.isNotBlank() }
                    _uiState.update {
                        it.copy(
                            detail = detail,
                            isSaving = false,
                            cafeName = detail.cafe.name,
                            cafeDescription = detail.cafe.desc,
                            representativeImageUrl = cafeImages.firstOrNull() ?: detail.cafe.thumbnailImage,
                            galleryImages = cafeImages.drop(1).take(_uiState.value.galleryMaxCount),
                            address = detail.cafe.region.address,
                            contactNumber = detail.phoneNumber,
                            weekdayOpen = parsedHours.weekdayOpen,
                            weekdayClose = parsedHours.weekdayClose,
                            weekendOpen = parsedHours.weekendOpen,
                            weekendClose = parsedHours.weekendClose,
                            infoMessage = null
                        )
                    }
                    _event.emit(CafeInfoEvent.NavigateBack)
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

    private fun submitCafeRegistration() {
        val currentState = _uiState.value
        _uiState.update { it.copy(isSaving = true, infoMessage = null) }

        viewModelScope.launch {
            when (
                val result = createCafeRegistrationClaimUseCase.invoke(
                    CafeRegistrationDraft(
                        name = currentState.cafeName.trim(),
                        description = currentState.cafeDescription.trim(),
                        region = Region(
                            country = "KR",
                            city = "Seoul",
                            address = currentState.address.trim(),
                            location = GeoPoint(37.5665, 126.9780)
                        ),
                        thumbnailImage = currentState.representativeImageUrl,
                        conceptType = "MAID",
                        businessHours = formatBusinessHours(currentState),
                        phoneNumber = currentState.contactNumber.trim()
                    )
                )
            ) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isSaving = false, infoMessage = null) }
                    _event.emit(CafeInfoEvent.NavigateBack)
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            infoMessage = result.error.toString()
                        )
                    }
                }
            }
        }
    }

    private fun formatBusinessHours(state: CafeInfoEditUiState): String {
        val weekday = listOf(state.weekdayOpen, state.weekdayClose).all { it.isNotBlank() }
        val weekend = listOf(state.weekendOpen, state.weekendClose).all { it.isNotBlank() }
        return when {
            weekday && weekend && state.weekdayOpen == state.weekendOpen && state.weekdayClose == state.weekendClose ->
                "매일 ${state.weekdayOpen} - ${state.weekdayClose}"
            weekday && weekend ->
                "평일 ${state.weekdayOpen} - ${state.weekdayClose} / 주말 ${state.weekendOpen} - ${state.weekendClose}"
            weekday ->
                "평일 ${state.weekdayOpen} - ${state.weekdayClose}"
            weekend ->
                "주말 ${state.weekendOpen} - ${state.weekendClose}"
            else -> ""
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
            is CafeInfoEditAction.SelectRepresentativeImage -> {
                _uiState.update { it.copy(representativeImageUrl = action.imageUrl) }
            }
            is CafeInfoEditAction.AddGalleryImage -> {
                if (action.imageUrl.isBlank()) {
                    return
                }
                val maxCount = _uiState.value.galleryMaxCount
                if (_uiState.value.galleryImages.size >= maxCount) {
                    showInfo("카페 갤러리는 최대 ${maxCount}장까지 등록할 수 있습니다.")
                    return
                }
                _uiState.update { state ->
                    state.copy(galleryImages = state.galleryImages + action.imageUrl)
                }
            }
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
        if (isRegistrationMode) {
            _uiState.update { it.copy(isLoading = false, infoMessage = null) }
        } else {
            loadCafeInfo()
        }
    }
}
