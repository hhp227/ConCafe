package org.hhp227.concafe.presentation.cafe

import org.hhp227.concafe.domain.model.CafeDetail
import org.hhp227.concafe.domain.model.CafeDetailCast
import org.hhp227.concafe.domain.model.CafeDetailReview

data class CafeUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedTab: TabType = TabType.INFO,
    val detail: CafeDetail? = null,
    val casts: List<CafeDetailCast> = emptyList(),
    val reviews: List<CafeDetailReview> = emptyList(),
    val isFavorite: Boolean = false,
    val isLoggedIn: Boolean = false
) {
    enum class TabType(val label: String) {
        INFO("정보"),
        MAIDS("메이드"),
        MENU("메뉴"),
        REVIEWS("리뷰"),
        NOTICES("공지")
    }

    companion object {
        fun empty(): CafeUiState {
            return CafeUiState()
        }
    }
}
