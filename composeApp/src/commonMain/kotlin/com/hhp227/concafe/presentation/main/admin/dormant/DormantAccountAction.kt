package com.hhp227.concafe.presentation.main.admin.dormant

import com.hhp227.concafe.domain.model.DormantAccountFilter
import com.hhp227.concafe.domain.model.User

sealed interface DormantAccountAction {
    data class SelectFilter(val filter: DormantAccountFilter) : DormantAccountAction
    data object LoadMore : DormantAccountAction
    data class RequestDormantChange(val user: User, val dormant: Boolean) : DormantAccountAction
    data object ConfirmDormantChange : DormantAccountAction
    data object CancelDormantChange : DormantAccountAction
    data object DismissInfoMessage : DormantAccountAction
}
