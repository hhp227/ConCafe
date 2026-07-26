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
import com.hhp227.concafe.domain.model.ConceptType
import com.hhp227.concafe.domain.model.GeoPoint
import com.hhp227.concafe.domain.model.Region
import com.hhp227.concafe.core.util.TimeUtils
import com.hhp227.concafe.domain.usecase.CreateCafeRegistrationClaimUseCase
import com.hhp227.concafe.domain.usecase.GetCafeDetailUseCase
import com.hhp227.concafe.domain.usecase.UploadImageUseCase
import com.hhp227.concafe.domain.usecase.UpdateCafeInfoUseCase

class CafeInfoEditViewModel(
    private val cafeId: String?,
    private val isRegistrationMode: Boolean,
    private val createCafeRegistrationClaimUseCase: CreateCafeRegistrationClaimUseCase,
    private val getCafeDetailUseCase: GetCafeDetailUseCase,
    private val updateCafeInfoUseCase: UpdateCafeInfoUseCase,
    private val uploadImageUseCase: UploadImageUseCase
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
                            conceptType = normalizeConceptType(detail.cafe.conceptType),
                            representativeImageUrl = cafeImages.firstOrNull() ?: detail.cafe.thumbnailImage,
                            galleryImages = cafeImages.drop(1).take(_uiState.value.galleryMaxCount),
                            address = detail.cafe.region.address,
                            mapLatitude = detail.cafe.region.location.latitude,
                            mapLongitude = detail.cafe.region.location.longitude,
                            contactNumber = detail.phoneNumber
                                .takeUnless { phone -> phone == CONTACT_PLACEHOLDER }
                                .orEmpty(),
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
                            infoMessage = MSG_LOAD_FAILED
                        )
                    }
                }
            }
        }
    }

    private fun saveCafeInfo() {
        val currentState = _uiState.value
        if (currentState.representativeImageUrl.isNullOrBlank() && currentState.galleryImages.none { it.isNotBlank() }) {
            _uiState.update {
                it.copy(
                    isImageRequiredAlertVisible = true,
                    infoMessage = MSG_IMAGE_REQUIRED_ONE_OR_MORE
                )
            }
            return
        }

        if (isRegistrationMode) {
            submitCafeRegistration()
            return
        }

        val targetCafeId = cafeId ?: return
        _uiState.update { it.copy(isSaving = true, infoMessage = null) }

        viewModelScope.launch {
            val uploadedRepresentativeImage = if (currentState.representativeImageUrl.isNullOrBlank()) {
                null
            } else {
                uploadImage(currentState.representativeImageUrl, "cafes/representative") ?: return@launch
            }
            val uploadedGalleryImages = buildList {
                for (image in currentState.galleryImages.filter { it.isNotBlank() }) {
                    val uploaded = uploadImage(image, "cafes/gallery") ?: return@launch
                    add(uploaded)
                }
            }
            when (
                val result = updateCafeInfoUseCase.invoke(
                    CafeInfoUpdate(
                        cafeId = targetCafeId,
                        name = currentState.cafeName,
                        description = currentState.cafeDescription,
                        conceptType = normalizeConceptType(currentState.conceptType),
                        representativeImageUrl = uploadedRepresentativeImage,
                        galleryImages = uploadedGalleryImages,
                        location = GeoPoint(
                            latitude = currentState.mapLatitude,
                            longitude = currentState.mapLongitude
                        ),
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
                            conceptType = normalizeConceptType(detail.cafe.conceptType),
                            representativeImageUrl = cafeImages.firstOrNull() ?: detail.cafe.thumbnailImage,
                            galleryImages = cafeImages.drop(1).take(_uiState.value.galleryMaxCount),
                            address = detail.cafe.region.address,
                            mapLatitude = detail.cafe.region.location.latitude,
                            mapLongitude = detail.cafe.region.location.longitude,
                            contactNumber = detail.phoneNumber
                                .takeUnless { phone -> phone == CONTACT_PLACEHOLDER }
                                .orEmpty(),
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
                            infoMessage = MSG_SAVE_FAILED
                        )
                    }
                }
            }
        }
    }

    private fun submitCafeRegistration() {
        val currentState = _uiState.value
        if (currentState.representativeImageUrl.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    isImageRequiredAlertVisible = true,
                    infoMessage = MSG_REGISTRATION_REP_REQUIRED
                )
            }
            return
        }
        _uiState.update { it.copy(isSaving = true, infoMessage = null) }

        viewModelScope.launch {
            val uploadedRepresentativeImage = uploadImage(currentState.representativeImageUrl, "cafes/representative")
                ?: return@launch
            when (
                val result = createCafeRegistrationClaimUseCase.invoke(
                    CafeRegistrationDraft(
                        name = currentState.cafeName.trim(),
                        description = currentState.cafeDescription.trim(),
                        region = Region(
                            country = "KR",
                            city = "Seoul",
                            address = currentState.address.trim(),
                            location = GeoPoint(
                                latitude = currentState.mapLatitude,
                                longitude = currentState.mapLongitude
                            )
                        ),
                        thumbnailImage = uploadedRepresentativeImage,
                        conceptType = normalizeConceptType(currentState.conceptType),
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
            is CafeInfoEditAction.ChangeConceptType -> _uiState.update { it.copy(conceptType = normalizeConceptType(action.value)) }
            is CafeInfoEditAction.ChangeAddress -> _uiState.update { it.copy(address = action.value) }
            is CafeInfoEditAction.SetPinnedLocation -> _uiState.update {
                it.copy(
                    mapLatitude = action.latitude,
                    mapLongitude = action.longitude
                )
            }
            is CafeInfoEditAction.ChangeContactNumber -> _uiState.update { it.copy(contactNumber = action.value) }
            is CafeInfoEditAction.ChangeWeekdayOpen -> _uiState.update { it.copy(weekdayOpen = action.value) }
            is CafeInfoEditAction.ChangeWeekdayClose -> _uiState.update { it.copy(weekdayClose = action.value) }
            is CafeInfoEditAction.ChangeWeekendOpen -> _uiState.update { it.copy(weekendOpen = action.value) }
            is CafeInfoEditAction.ChangeWeekendClose -> _uiState.update { it.copy(weekendClose = action.value) }
            is CafeInfoEditAction.SelectRepresentativeImage -> {
                _uiState.update { it.copy(representativeImageUrl = action.imageUrl, isImageRequiredAlertVisible = false) }
            }
            is CafeInfoEditAction.AddGalleryImage -> {
                if (action.imageUrl.isBlank()) {
                    return
                }
                val maxCount = _uiState.value.galleryMaxCount
                if (_uiState.value.galleryImages.size >= maxCount) {
                    showInfo("$MSG_GALLERY_MAX_EXCEEDED:$maxCount")
                    return
                }
                _uiState.update { state ->
                    state.copy(
                        galleryImages = state.galleryImages + action.imageUrl,
                        isImageRequiredAlertVisible = false
                    )
                }
            }
            is CafeInfoEditAction.RemoveGalleryImage -> {
                _uiState.update { state ->
                    val updated = state.galleryImages.toMutableList().also { it.removeAt(action.index) }

                    state.copy(galleryImages = updated)
                }
            }
            CafeInfoEditAction.ClickRepresentativeImage -> showInfo(MSG_REP_UPLOAD_NEXT_STEP)
            CafeInfoEditAction.ClickAddGalleryImage -> showInfo(MSG_GALLERY_ADD_NEXT_STEP)
            CafeInfoEditAction.ClickPinLocation -> showInfo(MSG_PIN_LOCATION_HINT)
            CafeInfoEditAction.ClickManageExceptionDates -> showInfo(MSG_EXCEPTION_NEXT_STEP)
            CafeInfoEditAction.DismissImageRequiredAlert -> _uiState.update { it.copy(isImageRequiredAlertVisible = false) }
            CafeInfoEditAction.ClickSave -> saveCafeInfo()
            CafeInfoEditAction.DismissInfoMessage -> _uiState.update { it.copy(infoMessage = null) }
        }
    }

    private suspend fun uploadImage(imageUrl: String?, folder: String): String? {
        if (imageUrl.isNullOrBlank()) return null
        return when (val result = uploadImageUseCase.invoke(imageUrl, folder)) {
            is AppResult.Success -> result.data
            is AppResult.Failure -> {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        infoMessage = MSG_IMAGE_UPLOAD_FAILED
                    )
                }
                null
            }
        }
    }

    private fun showInfo(message: String) {
        _uiState.update { it.copy(infoMessage = message) }
    }

    private fun parseBusinessHours(businessHours: String): ParsedBusinessHours {
        val times = TimeUtils.extractNormalizedHourMinuteList(businessHours)
        val open = times.getOrNull(0).orEmpty()
        val close = times.getOrNull(1).orEmpty()
        val weekendOpen = times.getOrNull(2) ?: open
        val weekendClose = times.getOrNull(3) ?: close
        return ParsedBusinessHours(
            weekdayOpen = open,
            weekdayClose = close,
            weekendOpen = weekendOpen,
            weekendClose = weekendClose
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

    companion object {
        private const val CONTACT_PLACEHOLDER = "연락처 정보 준비중"
        private const val MSG_LOAD_FAILED = "cafeinfo_info_load_failed"
        private const val MSG_IMAGE_REQUIRED_ONE_OR_MORE = "cafeinfo_info_image_required_one_or_more"
        private const val MSG_SAVE_FAILED = "cafeinfo_info_save_failed"
        private const val MSG_REGISTRATION_REP_REQUIRED = "cafeinfo_info_registration_rep_required"
        private const val MSG_GALLERY_MAX_EXCEEDED = "cafeinfo_info_gallery_max_exceeded"
        private const val MSG_REP_UPLOAD_NEXT_STEP = "cafeinfo_info_rep_upload_next_step"
        private const val MSG_GALLERY_ADD_NEXT_STEP = "cafeinfo_info_gallery_add_next_step"
        private const val MSG_PIN_LOCATION_HINT = "cafeinfo_info_pin_location_hint"
        private const val MSG_EXCEPTION_NEXT_STEP = "cafeinfo_info_exception_next_step"
        private const val MSG_IMAGE_UPLOAD_FAILED = "cafeinfo_info_image_upload_failed"

        private fun normalizeConceptType(value: String): String {
            return ConceptType.fromRaw(value)?.name ?: ConceptType.MAID.name
        }
    }
}
