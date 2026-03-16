package com.hhp227.concafe.presentation.review

data class ReviewEditUiState(
    val reviewId: String? = null,
    val cafeId: String = "",
    val userId: String = "",
    val visitId: String = "",
    val rating: Int = 0,
    val content: String = "",
    val photoImageUrl: String? = null,
    val taggedCastIds: List<String> = emptyList(),
    val availableCastTags: List<CastTag> = emptyList(),
    val likeCount: Int = 0,
    val createdAt: String = "",
    val isLoading: Boolean = false,
    val screenTitle: String = "리뷰 작성",
    val topActionLabel: String = "등록",
    val submitButtonLabel: String = "리뷰 등록하기",
    val cafeName: String = "",
    val cafeAddress: String = "",
    val isLoggedIn: Boolean = false,
    val isVisitVerified: Boolean = false,
    val atmosphereAnswer: Boolean? = null,
    val isSubmitting: Boolean = false,
    val infoMessage: String? = null
) {
    val ratingMessage: String
        get() = ratingToMessage(rating)

    val reviewLength: Int
        get() = content.length

    val isSubmitEnabled: Boolean
        get() = rating > 0 && content.trim().length >= minimumReviewLength && !isSubmitting

    data class CastTag(
        val id: String,
        val name: String
    )

    companion object {
        const val maximumRating = 5
        const val minimumReviewLength = 10

        fun ratingToMessage(rating: Int): String {
            return when (rating) {
                5 -> "최고예요!"
                4 -> "추천해요!"
                3 -> "무난했어요"
                2 -> "조금 아쉬워요"
                1 -> "추천하지 않아요"
                else -> "평점을 선택해주세요"
            }
        }
    }
}
