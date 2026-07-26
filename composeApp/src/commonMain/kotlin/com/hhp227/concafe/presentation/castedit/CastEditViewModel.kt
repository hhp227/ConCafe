package com.hhp227.concafe.presentation.castedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.CastSchedule
import com.hhp227.concafe.domain.model.CastUpsert
import com.hhp227.concafe.domain.usecase.DeleteImageUseCase
import com.hhp227.concafe.domain.usecase.GetCastDetailUseCase
import com.hhp227.concafe.domain.usecase.UploadImageUseCase
import com.hhp227.concafe.domain.usecase.UpsertCastUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CastEditViewModel(
    private val cafeId: String? = null,
    private val castId: String? = null,
    private val getCastDetailUseCase: GetCastDetailUseCase,
    private val upsertCastUseCase: UpsertCastUseCase,
    private val uploadImageUseCase: UploadImageUseCase,
    private val deleteImageUseCase: DeleteImageUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(CastEditUiState())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<CastEditEvent>(replay = 0)
    val event = _event.asSharedFlow()

    private val pendingDeletedGalleryImageUrls = mutableSetOf<String>()

    private var pendingDeletedProfileImageUrl: String? = null

    private var hasPendingLocalEdits = false

    private fun clickBack() {
        viewModelScope.launch {
            _event.emit(CastEditEvent.NavigateBack)
        }
    }

    private fun clickProfilePhoto() {
        _uiState.update { it.copy(infoMessage = null) }
    }

    private fun toggleWorkingDay(day: CastEditUiState.WorkingDay) {
        _uiState.update { state ->
            val nextDays = state.selectedWorkingDays.toMutableSet().apply {
                if (!add(day)) {
                    remove(day)
                }
            }
            state.copy(selectedWorkingDays = nextDays)
        }
    }

    private fun clickAddGalleryPhoto() {
        _uiState.update { it.copy(infoMessage = null) }
    }

    private fun clickSave() {
        val currentState = _uiState.value
        val removedGalleryImageUrls = pendingDeletedGalleryImageUrls.toList()
        val removedProfileImageUrl = pendingDeletedProfileImageUrl

        if (currentState.castName.isBlank()) {
            _uiState.update { it.copy(infoMessage = "캐스트 이름을 입력해주세요.") }
            return
        }
        if (currentState.conceptRole.isBlank()) {
            _uiState.update { it.copy(infoMessage = "컨셉 역할을 입력해주세요.") }
            return
        }
        if (currentState.profileImageUrl.isNullOrBlank() && currentState.galleryImages.none { it.isNotBlank() }) {
            _uiState.update { it.copy(isImageRequiredAlertVisible = true) }
            return
        }
        _uiState.update {
            it.copy(
                isSaving = true,
                infoMessage = null
            )
        }
        viewModelScope.launch {
            val uploadedProfileImage = uploadImage(currentState.profileImageUrl, folder = "casts/profile")
                ?: return@launch
            val uploadedGalleryImages = buildList {
                for (image in currentState.galleryImages) {
                    val uploaded = uploadImage(image, folder = "casts/gallery") ?: return@launch
                    add(uploaded)
                }
            }
            when (
                val result = upsertCastUseCase.invoke(
                    CastUpsert(
                        cafeId = cafeId,
                        castId = castId,
                        name = currentState.castName,
                        conceptRole = currentState.conceptRole,
                        birthday = currentState.birthday,
                        introduction = currentState.introduction,
                        profileImage = uploadedProfileImage,
                        galleryImages = uploadedGalleryImages,
                        workingDays = currentState.selectedWorkingDays.toWorkingDayKeys()
                    )
                )
            ) {
                is AppResult.Success -> {
                    deleteImages(listOfNotNull(removedProfileImageUrl) + removedGalleryImageUrls)
                    pendingDeletedGalleryImageUrls.removeAll(removedGalleryImageUrls.toSet())
                    if (pendingDeletedProfileImageUrl == removedProfileImageUrl) {
                        pendingDeletedProfileImageUrl = null
                    }
                    _uiState.update { it.copy(isSaving = false) }
                    _event.emit(CastEditEvent.NavigateBack)
                }
                is AppResult.Failure -> {
                    println("TEST, CastEditViewModel save failure: ${result.error}")
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            infoMessage = when (val error = result.error) {
                                is AppError.ValidationFailed -> error.reason
                                else -> "캐스트 정보를 저장하지 못했습니다."
                            }
                        )
                    }
                }
            }
        }
    }

    private fun loadCastDetail(targetCastId: String) {
        hasPendingLocalEdits = false
        _uiState.update {
            it.copy(
                isLoading = true,
                infoMessage = null
            )
        }

        viewModelScope.launch {
            when (val result = getCastDetailUseCase.invoke(targetCastId)) {
                is AppResult.Success -> {
                    val detail = result.data.detail
                    if (hasPendingLocalEdits) {
                        _uiState.update { state ->
                            state.copy(isLoading = false)
                        }
                    } else {
                        pendingDeletedGalleryImageUrls.clear()
                        pendingDeletedProfileImageUrl = null
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                profileImageUrl = detail.cast.profileImage,
                                castName = detail.cast.name,
                                conceptRole = detail.cast.conceptRole,
                                birthday = detail.cast.birthday.orEmpty(),
                                introduction = detail.cast.desc,
                                selectedWorkingDays = detail.schedule.toWorkingDays(),
                                galleryImages = detail.images
                                    .filter { image -> image.isNotBlank() && image != detail.cast.profileImage }
                                    .take(state.galleryMaxCount)
                            )
                        }
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            infoMessage = "캐스트 정보를 불러오지 못했습니다."
                        )
                    }
                }
            }
        }
    }

    fun onAction(action: CastEditAction) {
        when (action) {
            CastEditAction.ClickBack -> clickBack()
            CastEditAction.ClickProfilePhoto -> clickProfilePhoto()
            is CastEditAction.SelectProfilePhoto -> {
                hasPendingLocalEdits = true
                val nextImageUrl = action.imageUrl
                val previousProfileImageUrl = _uiState.value.profileImageUrl

                if (
                    !previousProfileImageUrl.isNullOrBlank() &&
                    previousProfileImageUrl != nextImageUrl &&
                    (previousProfileImageUrl.startsWith("http://") || previousProfileImageUrl.startsWith("https://")) &&
                    previousProfileImageUrl != pendingDeletedProfileImageUrl
                ) {
                    pendingDeletedProfileImageUrl = previousProfileImageUrl
                }
                _uiState.update {
                    it.copy(
                        profileImageUrl = nextImageUrl,
                        infoMessage = null,
                        isImageRequiredAlertVisible = false
                    )
                }
            }
            is CastEditAction.AddGalleryImage -> {
                hasPendingLocalEdits = true
                val imageUrl = action.imageUrl
                if (imageUrl.isBlank()) return
                val galleryImages = _uiState.value.galleryImages
                val galleryMaxCount = _uiState.value.galleryMaxCount
                if (galleryImages.size >= galleryMaxCount) {
                    _uiState.update { it.copy(infoMessage = "갤러리 사진은 최대 ${galleryMaxCount}장까지 등록할 수 있습니다.") }
                } else {
                    _uiState.update {
                        it.copy(
                            galleryImages = galleryImages + imageUrl,
                            infoMessage = null,
                            isImageRequiredAlertVisible = false
                        )
                    }
                }
            }
            is CastEditAction.RemoveGalleryImage -> {
                val index = action.index
                val galleryImages = _uiState.value.galleryImages
                if (index in galleryImages.indices) {
                    hasPendingLocalEdits = true
                    val removedImageUrl = galleryImages[index]
                    if (removedImageUrl.startsWith("http://") || removedImageUrl.startsWith("https://")) {
                        pendingDeletedGalleryImageUrls.add(removedImageUrl)
                    }
                    _uiState.update {
                        it.copy(
                            galleryImages = galleryImages.filterIndexed { imageIndex, _ -> imageIndex != index },
                            infoMessage = null
                        )
                    }
                }
            }
            is CastEditAction.ChangeCastName -> {
                hasPendingLocalEdits = true
                _uiState.update { it.copy(castName = action.value) }
            }
            is CastEditAction.ChangeConceptRole -> {
                hasPendingLocalEdits = true
                _uiState.update { it.copy(conceptRole = action.value) }
            }
            is CastEditAction.ChangeBirthday -> {
                hasPendingLocalEdits = true
                _uiState.update { it.copy(birthday = action.value) }
            }
            is CastEditAction.ChangeIntroduction -> {
                hasPendingLocalEdits = true
                _uiState.update { it.copy(introduction = action.value) }
            }
            is CastEditAction.ToggleWorkingDay -> {
                hasPendingLocalEdits = true
                toggleWorkingDay(action.day)
            }
            CastEditAction.ClickAddGalleryPhoto -> clickAddGalleryPhoto()
            CastEditAction.DismissImageRequiredAlert -> _uiState.update { it.copy(isImageRequiredAlertVisible = false) }
            CastEditAction.ClickSave -> clickSave()
            CastEditAction.DismissInfoMessage -> _uiState.update { it.copy(infoMessage = null) }
        }
    }

    private suspend fun uploadImage(imageUrl: String?, folder: String): String? {
        if (imageUrl.isNullOrBlank()) return null
        return when (val result = uploadImageUseCase.invoke(imageUrl, folder)) {
            is AppResult.Success -> result.data
            is AppResult.Failure -> {
                println("TEST, CastEditViewModel upload failure: ${result.error}")
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        infoMessage = "이미지를 업로드하지 못했습니다."
                    )
                }
                null
            }
        }
    }

    private suspend fun deleteImages(imageUrls: List<String>) {
        imageUrls.forEach { imageUrl ->
            deleteImageUseCase.invoke(imageUrl)
        }
    }

    init {
        _uiState.update {
            if (castId.isNullOrBlank()) {
                it.copy(
                    screenTitle = "캐스트 프로필 추가",
                    saveButtonLabel = "프로필 추가"
                )
            } else {
                it.copy(
                    screenTitle = "캐스트 프로필 수정",
                    saveButtonLabel = "프로필 저장"
                )
            }
        }

        if (!castId.isNullOrBlank()) {
            loadCastDetail(castId)
        }
    }
}

private fun List<CastSchedule>.toWorkingDays(): Set<CastEditUiState.WorkingDay> {
    return mapNotNull { it.date.toWorkingDayOrNull() }.toSet()
}

private fun Set<CastEditUiState.WorkingDay>.toWorkingDayKeys(): List<String> {
    return map { it.name }
}

private fun String.toWorkingDayOrNull(): CastEditUiState.WorkingDay? {
    val parts = split("-")
    if (parts.size != 3) return null
    val year = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val day = parts[2].toIntOrNull() ?: return null
    return CastEditUiState.WorkingDay.entries.getOrNull(dayOfWeekIndex(year, month, day))
}

private fun dayOfWeekIndex(year: Int, month: Int, day: Int): Int {
    var adjustedYear = year
    var adjustedMonth = month
    if (adjustedMonth < 3) {
        adjustedMonth += 12
        adjustedYear -= 1
    }
    val k = adjustedYear % 100
    val j = adjustedYear / 100
    val h = (day + (13 * (adjustedMonth + 1)) / 5 + k + (k / 4) + (j / 4) + (5 * j)) % 7
    return when (h) {
        2 -> 0
        3 -> 1
        4 -> 2
        5 -> 3
        6 -> 4
        0 -> 5
        else -> 6
    }
}
