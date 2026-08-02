import Foundation
import Shared

struct DormantAccountUiState {
    var selectedFilter: DormantAccountFilter = .dormant
    var users: [User] = []
    var nextCursor: String? = nil
    var canLoadMore: Bool = false
    var isLoading: Bool = false
    var isLoadingMore: Bool = false
    var isUpdating: Bool = false
    var confirmTarget: DormantChangeRequest? = nil
    var infoMessage: String? = nil

    var filterChips: [DormantAccountFilterChip] {
        [DormantAccountFilter.dormant, DormantAccountFilter.candidate].map { filter in
            DormantAccountFilterChip(
                filter: filter,
                label: filter.label,
                isSelected: selectedFilter == filter
            )
        }
    }
}

struct DormantAccountFilterChip: Identifiable {
    var id: DormantAccountFilter { filter }
    let filter: DormantAccountFilter
    let label: String
    let isSelected: Bool
}

struct DormantChangeRequest {
    let user: User
    let dormant: Bool
}

extension DormantAccountFilter {
    var label: String {
        switch self {
        case .dormant: return "휴면 계정"
        case .candidate: return "휴면 예정"
        default: return "계정"
        }
    }
}
