import Foundation
import Combine
import Shared

struct MainUiState {
    var currentUser: User?

    var selectedTab: String

    var thirdTabRoute: String

    static let empty = MainUiState(
        currentUser: nil,
        selectedTab: "home",
        thirdTabRoute: "checkin"
    )
}

enum MainAction {
    case enter(preferredRoute: String? = nil)
    case refreshNavigation(preferredRoute: String? = nil)
}

enum MainEvent {
    case showError(message: String)
}

@MainActor
final class MainViewModel: ObservableObject {
    private let getMainNavigationUseCase: GetMainNavigationUseCase

    @Published private(set) var uiState = MainUiState.empty

    let event = PassthroughSubject<MainEvent, Never>()

    private func refreshNavigation(preferredRoute: String?) {
        Task {
            do {
                let result = try await getMainNavigationUseCase.invoke(preferredRoute: preferredRoute)

                if let success = result as? AppResultSuccess<AnyObject>,
                   let state = success.data as? MainNavigationState {
                    uiState = MainUiState(
                        currentUser: state.currentUser,
                        selectedTab: state.selectedTab,
                        thirdTabRoute: state.thirdTab.route
                    )
                } else if let failure = result as? AppResultFailure {
                    event.send(.showError(message: "\(failure.error)"))
                }
            } catch {
                event.send(.showError(message: error.localizedDescription))
            }
        }
    }

    func onAction(_ action: MainAction) {
        switch action {
        case .enter(let preferredRoute):
            refreshNavigation(preferredRoute: preferredRoute)
        case .refreshNavigation(let preferredRoute):
            refreshNavigation(preferredRoute: preferredRoute)
        }
    }

    init(
        getMainNavigationUseCase: GetMainNavigationUseCase = KoinInitializerKt.resolveGetMainNavigationUseCase()
    ) {
        self.getMainNavigationUseCase = getMainNavigationUseCase

        onAction(.enter())
    }
}
