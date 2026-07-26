package com.hhp227.concafe.presentation.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.usecase.CreateReviewUseCase
import com.hhp227.concafe.domain.usecase.GetCafeDetailUseCase
import com.hhp227.concafe.domain.usecase.GetReviewUseCase
import com.hhp227.concafe.domain.usecase.UpdateReviewUseCase
import com.hhp227.concafe.domain.usecase.UploadImageUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReviewEditViewModel(
    private val cafeId: String? = null,
    private val reviewId: String? = null,
    private val getCafeDetailUseCase: GetCafeDetailUseCase,
    private val createReviewUseCase: CreateReviewUseCase,
    private val updateReviewUseCase: UpdateReviewUseCase,
    private val getReviewUseCase: GetReviewUseCase,
    private val uploadImageUseCase: UploadImageUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        ReviewEditUiState(
            cafeId = cafeId.orEmpty(),
            reviewId = reviewId,
            screenTitle = if (reviewId != null) "리뷰 수정" else "리뷰 작성",
            topActionLabel = if (reviewId != null) "수정" else "등록",
            submitButtonLabel = if (reviewId != null) "리뷰 수정하기" else "리뷰 등록하기"
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
                                isLoggedIn = result.data.isLoggedIn,
                                isVisitVerified = result.data.isVisitVerified,
                                availableCastTags = detail.casts.map { cast ->
                                    ReviewEditUiState.CastTag(
                                        id = cast.id,
                                        name = cast.name
                                    )
                                },
                                infoMessage = null
                            )
                        }
                        if (reviewId != null) loadExistingReview()
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

    private fun loadExistingReview() {
        val id = reviewId ?: return
        viewModelScope.launch {
            when (val result = getReviewUseCase.invoke(id)) {
                is AppResult.Success -> {
                    val review = result.data
                    _uiState.update {
                        it.copy(
                            rating = review.rating.toInt(),
                            content = review.content,
                            taggedCastIds = review.taggedCastIds,
                            photoImageUrl = review.imageUrls.firstOrNull()
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update { it.copy(infoMessage = "기존 리뷰를 불러오지 못했습니다.") }
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
        _uiState.update { it.copy(infoMessage = null) }
    }

    private fun removePhoto() {
        _uiState.update { it.copy(photoImageUrl = null) }
    }

    private fun toggleCastTag(castId: String) {
        _uiState.update { state ->
            val nextIds = state.taggedCastIds.toMutableList().apply {
                if (contains(castId)) {
                    remove(castId)
                } else {
                    add(castId)
                }
            }
            state.copy(taggedCastIds = nextIds)
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
                    infoMessage = null
                )
            }
            viewModelScope.launch {
                val uploadedPhoto = uploadImage(currentState.photoImageUrl, "reviews")
                if (!currentState.photoImageUrl.isNullOrBlank() && uploadedPhoto == null) {
                    return@launch
                }
                val imageUrls = uploadedPhoto?.let { listOf(it) } ?: emptyList()

                if (reviewId != null) {
                    when (
                        val result = updateReviewUseCase.invoke(
                            reviewId = reviewId,
                            rating = currentState.rating.toFloat(),
                            content = currentState.content,
                            imageUrls = imageUrls,
                            taggedCastIds = currentState.taggedCastIds
                        )
                    ) {
                        is AppResult.Success -> {
                            _uiState.update { it.copy(isSubmitting = false) }
                            _event.emit(ReviewEditEvent.NavigateBack)
                        }
                        is AppResult.Failure -> {
                            _uiState.update {
                                it.copy(
                                    isSubmitting = false,
                                    infoMessage = when (val error = result.error) {
                                        is AppError.Unauthorized -> "리뷰 수정은 로그인 후 가능해요."
                                        is AppError.ValidationFailed -> error.reason.toReviewValidationMessage()
                                        else -> "리뷰 수정에 실패했습니다."
                                    }
                                )
                            }
                        }
                    }
                } else {
                    when (
                        val result = createReviewUseCase.invoke(
                            cafeId = currentState.cafeId,
                            rating = currentState.rating.toFloat(),
                            content = currentState.content,
                            imageUrls = imageUrls,
                            taggedCastIds = currentState.taggedCastIds
                        )
                    ) {
                        is AppResult.Success -> {
                            _uiState.update {
                                it.copy(
                                    isSubmitting = false,
                                    reviewId = result.data.id,
                                    userId = result.data.userId,
                                    visitId = result.data.visitId,
                                    createdAt = result.data.createdAt
                                )
                            }
                            _event.emit(ReviewEditEvent.NavigateBack)
                        }
                        is AppResult.Failure -> {
                            _uiState.update {
                                it.copy(
                                    isSubmitting = false,
                                    infoMessage = when (val error = result.error) {
                                        is AppError.Unauthorized -> "리뷰 작성은 로그인 후 가능해요."
                                        is AppError.ValidationFailed -> error.reason.toReviewValidationMessage()
                                        else -> "리뷰 등록에 실패했습니다."
                                    }
                                )
                            }
                        }
                    }
                }
            }
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
            is ReviewEditAction.SelectPhoto -> _uiState.update { it.copy(photoImageUrl = action.imageUrl, infoMessage = null) }
            ReviewEditAction.RemovePhoto -> removePhoto()
            is ReviewEditAction.ChangeReviewText -> _uiState.update {
                it.copy(content = action.value)
            }
            is ReviewEditAction.ToggleCastTag -> toggleCastTag(action.castId)
            is ReviewEditAction.SelectAtmosphereAnswer -> _uiState.update {
                it.copy(atmosphereAnswer = action.isPositive)
            }
            ReviewEditAction.ClickSubmit -> clickSubmit()
            ReviewEditAction.DismissInfoMessage -> _uiState.update { it.copy(infoMessage = null) }
        }
    }

    private suspend fun uploadImage(imageUrl: String?, folder: String): String? {
        if (imageUrl.isNullOrBlank()) return null
        return when (val result = uploadImageUseCase.invoke(imageUrl, folder)) {
            is AppResult.Success -> result.data
            is AppResult.Failure -> {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        infoMessage = "이미지를 업로드하지 못했습니다."
                    )
                }
                null
            }
        }
    }

    init {
        loadCafeInfo()
    }
}

private fun String.toReviewValidationMessage(): String {
    return when (this) {
        "cafeId is required" -> "카페 정보를 찾을 수 없습니다."
        "rating is required" -> "평점을 선택해주세요."
        "review content is required" -> "상세 리뷰를 입력해주세요."
        else -> this
    }
}
