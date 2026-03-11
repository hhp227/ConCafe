package org.hhp227.concafe.presentation.castedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.hhp227.concafe.domain.common.AppError
import org.hhp227.concafe.domain.common.AppResult
import org.hhp227.concafe.domain.model.CastDetail
import org.hhp227.concafe.domain.model.CastSchedule
import org.hhp227.concafe.domain.model.CastUpsert
import org.hhp227.concafe.domain.usecase.GetCastDetailUseCase
import org.hhp227.concafe.domain.usecase.UpsertCastUseCase

class CastEditViewModel(
    private val cafeId: String? = null,
    private val castId: String? = null,
    private val getCastDetailUseCase: GetCastDetailUseCase,
    private val upsertCastUseCase: UpsertCastUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(CastEditUiState())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<CastEditEvent>(replay = 0)
    val event = _event.asSharedFlow()

    private fun clickBack() {
        viewModelScope.launch {
            _event.emit(CastEditEvent.NavigateBack)
        }
    }

    private fun clickProfilePhoto() {
        _uiState.update { it.copy(infoMessage = "프로필 사진 업로드는 다음 단계에서 연결됩니다.") }
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
        _uiState.update { it.copy(infoMessage = "갤러리 사진 업로드는 다음 단계에서 연결됩니다.") }
    }

    private fun clickSave() {
        val currentState = _uiState.value
        if (currentState.castName.isBlank()) {
            _uiState.update { it.copy(infoMessage = "캐스트 이름을 입력해주세요.") }
            return
        }
        if (currentState.conceptRole.isBlank()) {
            _uiState.update { it.copy(infoMessage = "컨셉 역할을 입력해주세요.") }
            return
        }

        _uiState.update {
            it.copy(
                isSaving = true,
                infoMessage = null
            )
        }

        viewModelScope.launch {
            when (
                val result = upsertCastUseCase.invoke(
                    CastUpsert(
                        cafeId = cafeId,
                        castId = castId,
                        name = currentState.castName,
                        conceptRole = currentState.conceptRole,
                        birthday = currentState.birthday,
                        introduction = currentState.introduction,
                        workingDays = currentState.selectedWorkingDays.toWorkingDayKeys()
                    )
                )
            ) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isSaving = false) }
                    _event.emit(CastEditEvent.NavigateBack)
                }
                is AppResult.Failure -> {
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
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            castName = detail.cast.name,
                            conceptRole = detail.cast.conceptRole,
                            birthday = detail.cast.birthday.orEmpty(),
                            introduction = detail.cast.desc,
                            selectedWorkingDays = detail.schedule.toWorkingDays(),
                            galleryItems = detail.toGalleryItems()
                        )
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
            is CastEditAction.ChangeCastName -> _uiState.update { it.copy(castName = action.value) }
            is CastEditAction.ChangeConceptRole -> _uiState.update { it.copy(conceptRole = action.value) }
            is CastEditAction.ChangeBirthday -> _uiState.update { it.copy(birthday = action.value) }
            is CastEditAction.ChangeIntroduction -> _uiState.update { it.copy(introduction = action.value) }
            is CastEditAction.ToggleWorkingDay -> toggleWorkingDay(action.day)
            CastEditAction.ClickAddGalleryPhoto -> clickAddGalleryPhoto()
            CastEditAction.ClickSave -> clickSave()
            CastEditAction.DismissInfoMessage -> _uiState.update { it.copy(infoMessage = null) }
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

private fun CastDetail.toGalleryItems(): List<CastEditUiState.GalleryItem> {
    val images = images.filter { it.isNotBlank() }
    if (images.isEmpty()) return emptyList()

    val visibleImages = images.take(3)
    val remainingCount = (images.size - visibleImages.size).coerceAtLeast(0)

    return visibleImages.mapIndexed { index, imageUrl ->
        CastEditUiState.GalleryItem(
            id = imageUrl.ifBlank { "gallery-$index" },
            label = "갤러리 ${index + 1}",
            overlayCount = if (index == visibleImages.lastIndex && remainingCount > 0) remainingCount else null
        )
    }
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
