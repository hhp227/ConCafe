package com.hhp227.concafe.presentation.main

sealed interface MainAction {
    data class CheckAppUpdate(
        val storePlatform: String,
        val storeId: String,
        val currentVersion: String
    ) : MainAction
    data class Enter(val preferredRoute: String? = null) : MainAction
    data class RefreshNavigation(val preferredRoute: String? = null) : MainAction
    data class SelectTab(val route: String) : MainAction
}
