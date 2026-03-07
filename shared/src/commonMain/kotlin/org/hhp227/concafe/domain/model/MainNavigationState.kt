package org.hhp227.concafe.domain.model

data class MainNavigationState(
    val currentUser: User?,
    val tabs: List<MainNavigationTab>,
    val selectedTab: String,
    val thirdTab: MainNavigationTab
)
