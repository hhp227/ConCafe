package org.hhp227.concafe.presentation.main.myinfo

import org.hhp227.concafe.domain.model.Cafe
import org.hhp227.concafe.domain.model.Cast
import org.hhp227.concafe.domain.model.MyPageSummary
import org.hhp227.concafe.domain.model.ProfileBadge
import org.hhp227.concafe.domain.model.User

data class MyInfoUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false,
    val user: User? = null,
    val summary: MyPageSummary? = null,
    val badges: List<ProfileBadge> = emptyList(),
    val popularCafes: List<Cafe> = emptyList(),
    val recentVisits: List<Cafe> = emptyList(),
    val favorites: List<Cafe> = emptyList(),
    val followedMaids: List<Cast> = emptyList()
) {
    companion object {
        fun empty() = MyInfoUiState()
    }
}
