import Foundation
import Shared

struct UserManagementUiState {
    var selectedFilter: AdminUserFilter = .cafeOwner
    var users: [User] = []
    var nextCursor: String? = nil
    var canLoadMore: Bool = false
    var isLoading: Bool = false
    var isLoadingMore: Bool = false
    var infoMessage: String? = nil

    var filterChips: [UserManagementFilterChip] {
        [AdminUserFilter.cafeOwner, AdminUserFilter.banned].map { filter in
            UserManagementFilterChip(
                filter: filter,
                label: filter.label,
                isSelected: selectedFilter == filter
            )
        }
    }
}

struct UserManagementFilterChip: Identifiable {
    var id: AdminUserFilter { filter }
    let filter: AdminUserFilter
    let label: String
    let isSelected: Bool
}

extension AdminUserFilter {
    var label: String {
        switch self {
        case .cafeOwner: return "카페 운영자"
        case .banned: return "차단 사용자"
        default: return "사용자"
        }
    }
}
