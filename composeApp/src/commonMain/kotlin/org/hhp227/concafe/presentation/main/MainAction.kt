package org.hhp227.concafe.presentation.main

sealed interface MainAction {
    data class Enter(val preferredRoute: String? = null) : MainAction
    data class RefreshNavigation(val preferredRoute: String? = null) : MainAction
}
