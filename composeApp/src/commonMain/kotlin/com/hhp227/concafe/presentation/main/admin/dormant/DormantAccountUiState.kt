package com.hhp227.concafe.presentation.main.admin.dormant

import com.hhp227.concafe.domain.model.DormantAccountFilter
import com.hhp227.concafe.domain.model.User

data class DormantAccountUiState(
    val selectedFilter: DormantAccountFilter = DormantAccountFilter.DORMANT,
    val users: List<User> = emptyList(),
    val nextCursor: String? = null,
    val canLoadMore: Boolean = false,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isUpdating: Boolean = false,
    val confirmTarget: DormantChangeRequest? = null,
    val infoMessage: String? = null
) {
    val filterChips: List<DormantAccountFilterChip>
        get() = DormantAccountFilter.entries.map { filter ->
            DormantAccountFilterChip(
                filter = filter,
                label = filter.label,
                isSelected = selectedFilter == filter
            )
        }
}

data class DormantAccountFilterChip(
    val filter: DormantAccountFilter,
    val label: String,
    val isSelected: Boolean
)

data class DormantChangeRequest(
    val user: User,
    val dormant: Boolean
)

val DormantAccountFilter.label: String
    get() = when (this) {
        DormantAccountFilter.DORMANT -> "휴면 계정"
        DormantAccountFilter.CANDIDATE -> "휴면 예정"
    }
