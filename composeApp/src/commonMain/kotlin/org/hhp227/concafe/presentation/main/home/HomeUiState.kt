package org.hhp227.concafe.presentation.main.home

data class HomeUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val banners: List<HomeBannerUi>,
    val popularMaids: List<PopularMaidUi>,
    val nearbyCafes: List<NearbyCafeUi>,
    val birthdayMaids: List<BirthdayMaidUi>,
    val notices: List<NoticeUi>
) {
    companion object {
        fun empty() = HomeUiState(
            banners = emptyList(),
            popularMaids = emptyList(),
            nearbyCafes = emptyList(),
            birthdayMaids = emptyList(),
            notices = emptyList()
        )
    }
}
