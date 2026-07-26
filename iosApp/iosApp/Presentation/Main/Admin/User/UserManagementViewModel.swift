import Foundation
import Combine
import Shared

@MainActor
final class UserManagementViewModel: ObservableObject {
    private let getAdminUserPageUseCase: GetAdminUserPageUseCase

    @Published private(set) var uiState = UserManagementUiState()

    let event = PassthroughSubject<UserManagementEvent, Never>()

    private var pageTask: Task<Void, Never>?

    func onAction(_ action: UserManagementAction) {
        switch action {
        case .selectFilter(let filter):
            guard uiState.selectedFilter != filter else { return }
            uiState.selectedFilter = filter
            uiState.users = []
            uiState.nextCursor = nil
            uiState.canLoadMore = false
            uiState.infoMessage = nil
            loadPage(cursor: nil, append: false)
        case .loadMore:
            loadMore()
        case .dismissInfoMessage:
            uiState.infoMessage = nil
        }
    }

    private func loadMore() {
        guard uiState.canLoadMore, !uiState.isLoadingMore, let cursor = uiState.nextCursor else { return }
        loadPage(cursor: cursor, append: true)
    }

    private func loadPage(cursor: String?, append: Bool) {
        pageTask?.cancel()
        pageTask = Task { @MainActor in
            if append {
                uiState.isLoadingMore = true
                do {
                    try await Task.sleep(nanoseconds: userPaginationDelayNanoseconds)
                    if Task.isCancelled { return }
                } catch {
                    return
                }
            } else {
                uiState.isLoading = true
                uiState.infoMessage = nil
            }

            do {
                let result = try await getAdminUserPageUseCase.invoke(
                    filter: uiState.selectedFilter,
                    cursor: cursor,
                    pageSize: userManagementPageSize
                )
                if let success = result as? AppResultSuccess<AnyObject>,
                   let page = success.data as? Shared.PagedResult<User> {
                    let pageItems = (page.items as? [User] ?? []).sorted { $0.createdAt > $1.createdAt }
                    uiState.users = append ? (uiState.users + pageItems) : pageItems
                    uiState.nextCursor = page.nextCursor
                    uiState.canLoadMore = page.hasNext
                    uiState.isLoading = false
                    uiState.isLoadingMore = false
                    uiState.infoMessage = nil
                } else if let failure = result as? AppResultFailure {
                    uiState.isLoading = false
                    uiState.isLoadingMore = false
                    uiState.infoMessage = "\(failure.error)"
                } else {
                    uiState.isLoading = false
                    uiState.isLoadingMore = false
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isLoading = false
                uiState.isLoadingMore = false
                uiState.infoMessage = error.localizedDescription
            }
        }
    }

    init(
        getAdminUserPageUseCase: GetAdminUserPageUseCase = KoinInitializerKt.resolveGetAdminUserPageUseCase()
    ) {
        self.getAdminUserPageUseCase = getAdminUserPageUseCase

        loadPage(cursor: nil, append: false)
    }

    deinit {
        pageTask?.cancel()
    }
}

private let userManagementPageSize: Int32 = 15
private let userPaginationDelayNanoseconds: UInt64 = 1_000_000_000
