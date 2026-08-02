import Foundation
import Combine
import Shared

@MainActor
final class DormantAccountViewModel: ObservableObject {
    private let getDormantAccountPageUseCase: GetDormantAccountPageUseCase

    private let updateUserDormantStatusUseCase: UpdateUserDormantStatusUseCase

    @Published private(set) var uiState = DormantAccountUiState()

    let event = PassthroughSubject<DormantAccountEvent, Never>()

    private var pageTask: Task<Void, Never>?

    func onAction(_ action: DormantAccountAction) {
        switch action {
        case .selectFilter(let filter):
            guard uiState.selectedFilter != filter else { return }
            uiState.selectedFilter = filter
            uiState.users = []
            uiState.nextCursor = nil
            uiState.canLoadMore = false
            uiState.confirmTarget = nil
            uiState.infoMessage = nil
            loadPage(cursor: nil, append: false)
        case .loadMore:
            loadMore()
        case .requestDormantChange(let user, let dormant):
            uiState.confirmTarget = DormantChangeRequest(user: user, dormant: dormant)
        case .confirmDormantChange:
            confirmDormantChange()
        case .cancelDormantChange:
            uiState.confirmTarget = nil
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
                    try await Task.sleep(nanoseconds: dormantPaginationDelayNanoseconds)
                    if Task.isCancelled { return }
                } catch {
                    return
                }
            } else {
                uiState.isLoading = true
                uiState.infoMessage = nil
            }

            do {
                let result = try await getDormantAccountPageUseCase.invoke(
                    filter: uiState.selectedFilter,
                    cursor: cursor,
                    pageSize: dormantAccountPageSize
                )
                if let success = result as? AppResultSuccess<AnyObject>,
                   let page = success.data as? Shared.PagedResult<User> {
                    let items = page.items as? [User] ?? []
                    let pageItems = uiState.selectedFilter == .dormant
                        ? items.sorted { $0.createdAt > $1.createdAt }
                        : items.sorted { ($0.lastLoginAt ?? "") < ($1.lastLoginAt ?? "") }
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

    private func confirmDormantChange() {
        guard let request = uiState.confirmTarget, !uiState.isUpdating else { return }

        uiState.confirmTarget = nil
        Task { @MainActor in
            uiState.isUpdating = true

            do {
                let result = try await updateUserDormantStatusUseCase.invoke(
                    userId: request.user.id,
                    dormant: request.dormant
                )
                if result is AppResultSuccess<AnyObject> {
                    uiState.users = uiState.users.filter { $0.id != request.user.id }
                    uiState.isUpdating = false
                    uiState.infoMessage = request.dormant
                        ? "\(request.user.nickname) 계정을 휴면 전환했습니다."
                        : "\(request.user.nickname) 계정의 휴면을 해제했습니다."
                } else if let failure = result as? AppResultFailure {
                    uiState.isUpdating = false
                    uiState.infoMessage = "\(failure.error)"
                } else {
                    uiState.isUpdating = false
                }
            } catch {
                uiState.isUpdating = false
                uiState.infoMessage = error.localizedDescription
            }
        }
    }

    init(
        getDormantAccountPageUseCase: GetDormantAccountPageUseCase = KoinInitializerKt.resolveGetDormantAccountPageUseCase(),
        updateUserDormantStatusUseCase: UpdateUserDormantStatusUseCase = KoinInitializerKt.resolveUpdateUserDormantStatusUseCase()
    ) {
        self.getDormantAccountPageUseCase = getDormantAccountPageUseCase
        self.updateUserDormantStatusUseCase = updateUserDormantStatusUseCase

        loadPage(cursor: nil, append: false)
    }

    deinit {
        pageTask?.cancel()
    }
}

private let dormantAccountPageSize: Int32 = 15
private let dormantPaginationDelayNanoseconds: UInt64 = 1_000_000_000
