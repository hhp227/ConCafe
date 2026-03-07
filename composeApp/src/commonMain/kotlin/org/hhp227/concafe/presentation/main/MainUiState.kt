package org.hhp227.concafe.presentation.main

import org.hhp227.concafe.domain.model.MainNavigationTab
import org.hhp227.concafe.domain.model.User

data class MainUiState(
    val currentUser: User? = null,
    val tabs: List<MainNavigationTab> = listOf(
        MainNavigationTab.HOME,
        MainNavigationTab.EXPLORE,
        MainNavigationTab.CHECK_IN,
        MainNavigationTab.RANKING,
        MainNavigationTab.MY_INFO
    ),
    val selectedTab: String = MainNavigationTab.HOME.route,
    val thirdTab: MainNavigationTab = MainNavigationTab.CHECK_IN
) {
    companion object {
        fun empty(): MainUiState = MainUiState()
    }
}
