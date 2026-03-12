package com.hhp227.concafe.presentation.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.usecase.GetCafeDetailUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReviewEditViewModel(
    private val cafeId: String? = null,
    private val getCafeDetailUseCase: GetCafeDetailUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        ReviewEditUiState(
            cafeId = cafeId.orEmpty()
        )
    )
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<ReviewEditEvent>(replay = 0)
    val event = _event.asSharedFlow()

    private fun loadCafeInfo() {
        if (cafeId.isNullOrBlank()) {
            _uiState.update { it.copy(infoMessage = "카페 정보를 찾을 수 없습니다.") }
        } else {
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
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                cafeId = cafeId,
                                cafeName = detail.cafe.name,
                                cafeAddress = detail.cafe.region.address,
                                infoMessage = null
                            )
                        }
                    }
                    is AppResult.Failure -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                infoMessage = "카페 정보를 불러오지 못했습니다."
                            )
                        }
                    }
                }
            }
        }
    }

    private fun clickBack() {
        viewModelScope.launch {
            _event.emit(ReviewEditEvent.NavigateBack)
        }
    }

    private fun clickAddPhoto() {
        val currentState = _uiState.value
        if (currentState.images.size >= ReviewEditUiState.maximumPhotoCount) {
            _uiState.update { it.copy(infoMessage = "사진은 최대 10장까지 등록할 수 있습니다.") }
        } else {
            val nextIndex = currentState.images.size + 1
            val nextPhoto = placeholderPhotoItem(nextIndex)
            _uiState.update {
                it.copy(
                    images = it.images + nextPhoto,
                    infoMessage = "사진 업로드는 다음 단계에서 연결됩니다."
                )
            }
        }
    }

    private fun removePhoto(photoId: String) {
        _uiState.update { state ->
            state.copy(
                images = state.images.filterNot { it.id == photoId }
            )
        }
    }

    private fun clickSubmit() {
        val currentState = _uiState.value

        if (currentState.rating <= 0) {
            _uiState.update { it.copy(infoMessage = "평점을 선택해주세요.") }
        } else if (currentState.content.trim().length < ReviewEditUiState.minimumReviewLength) {
            _uiState.update { it.copy(infoMessage = "상세 리뷰는 최소 10자 이상 입력해주세요.") }
        } else {
            _uiState.update {
                it.copy(
                    isSubmitting = true,
                    infoMessage = "리뷰 등록은 다음 단계에서 연결됩니다."
                )
            }
            _uiState.update { it.copy(isSubmitting = false) }
        }
    }

    fun onAction(action: ReviewEditAction) {
        when (action) {
            ReviewEditAction.ClickBack -> clickBack()
            is ReviewEditAction.SelectRating -> _uiState.update {
                it.copy(
                    rating = action.rating.coerceIn(0, ReviewEditUiState.maximumRating)
                )
            }
            ReviewEditAction.ClickAddPhoto -> clickAddPhoto()
            is ReviewEditAction.RemovePhoto -> removePhoto(action.photoId)
            is ReviewEditAction.ChangeReviewText -> _uiState.update {
                it.copy(content = action.value)
            }
            is ReviewEditAction.SelectAtmosphereAnswer -> _uiState.update {
                it.copy(atmosphereAnswer = action.isPositive)
            }
            ReviewEditAction.ClickSubmit -> clickSubmit()
            ReviewEditAction.DismissInfoMessage -> _uiState.update { it.copy(infoMessage = null) }
        }
    }

    init {
        loadCafeInfo()
    }
}

private fun placeholderPhotoItem(index: Int): ReviewEditUiState.PhotoItem {
    val accentColors = listOf(0xFFA65A74, 0xFF6D4C68, 0xFF7D5A4F, 0xFF8E5E78, 0xFF8A6557)
    val backgroundColors = listOf(0xFFFFE3EC, 0xFFF8E4EC, 0xFFFFEBDD, 0xFFFFF1F5, 0xFFF9ECE6)
    val colorIndex = (index - 1) % accentColors.size

    return ReviewEditUiState.PhotoItem(
        id = "photo-$index",
        label = "사진 $index",
        accentColorHex = accentColors[colorIndex],
        backgroundColorHex = backgroundColors[colorIndex]
    )
}
