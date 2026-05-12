package com.hhp227.concafe.presentation.main.admin.user

import com.hhp227.concafe.domain.model.AdminUserFilter
import com.hhp227.concafe.domain.model.User

data class UserManagementUiState(
    val selectedFilter: AdminUserFilter = AdminUserFilter.CAFE_OWNER,
    val users: List<User> = emptyList(),
    val nextCursor: String? = null,
    val canLoadMore: Boolean = false,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val infoMessage: String? = null
) {
    val filterChips: List<UserManagementFilterChip>
        get() = AdminUserFilter.entries.map { filter ->
            UserManagementFilterChip(
                filter = filter,
                label = filter.label,
                isSelected = selectedFilter == filter
            )
        }
}

data class UserManagementFilterChip(
    val filter: AdminUserFilter,
    val label: String,
    val isSelected: Boolean
)

val AdminUserFilter.label: String
    get() = when (this) {
        AdminUserFilter.CAFE_OWNER -> "카페 운영자"
        AdminUserFilter.BANNED -> "차단 사용자"
    }
