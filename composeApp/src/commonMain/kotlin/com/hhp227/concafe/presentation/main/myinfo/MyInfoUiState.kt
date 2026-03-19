package com.hhp227.concafe.presentation.main.myinfo

import com.hhp227.concafe.domain.model.Cafe
import com.hhp227.concafe.domain.model.CafeManagementData
import com.hhp227.concafe.domain.model.Cast
import com.hhp227.concafe.domain.model.CastDetail
import com.hhp227.concafe.domain.model.MyPageSummary
import com.hhp227.concafe.domain.model.ProfileBadge
import com.hhp227.concafe.domain.model.User

data class MyInfoUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false,
    val user: User? = null,
    val summary: MyPageSummary? = null,
    val castDetail: CastDetail? = null,
    val ownedCafes: List<CafeManagementData.OwnedCafeSummary> = emptyList(),
    val badges: List<ProfileBadge> = emptyList(),
    val popularCafes: List<Cafe> = emptyList(),
    val recentVisits: List<Cafe> = emptyList(),
    val favorites: List<Cafe> = emptyList(),
    val followedMaids: List<Cast> = emptyList(),
    val isLoginPromptVisible: Boolean = false
) {
    companion object {
        fun empty() = MyInfoUiState()
    }
}
