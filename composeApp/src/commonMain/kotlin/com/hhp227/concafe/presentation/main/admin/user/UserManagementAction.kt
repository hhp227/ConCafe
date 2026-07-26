package com.hhp227.concafe.presentation.main.admin.user

import com.hhp227.concafe.domain.model.AdminUserFilter

sealed interface UserManagementAction {
    data class SelectFilter(val filter: AdminUserFilter) : UserManagementAction
    data object LoadMore : UserManagementAction
    data object DismissInfoMessage : UserManagementAction
}
