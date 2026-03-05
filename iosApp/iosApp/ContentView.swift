import SwiftUI

private enum Route: Hashable {
    case entry
    case main(initialTab: String?)
    case detail(param: String)
}

private enum NavigationAction {
    case navigateToMain(initialTab: String? = nil)
    case navigateToDetail(id: String)
    case navigateBack
}

private enum NavigationEvent {
    case navigateTo(Route)
    case navigateBack
}

@MainActor
private final class NavigationViewModel: ObservableObject {
    @Published var currentRoute: Route = .entry
    @Published var path: [Route] = []
    @Published var currentMainTab: String = "home"

    func onAction(_ action: NavigationAction) {
        let event: NavigationEvent
        switch action {
        case .navigateToMain(let initialTab):
            event = .navigateTo(.main(initialTab: initialTab))
        case .navigateToDetail(let id):
            event = .navigateTo(.detail(param: id))
        case .navigateBack:
            event = .navigateBack
        }
        onEvent(event)
    }

    private func onEvent(_ event: NavigationEvent) {
        switch event {
        case .navigateTo(let route):
            switch route {
            case .main(let initialTab):
                currentMainTab = initialTab ?? "home"
                currentRoute = .main(initialTab: currentMainTab)
                path.removeAll()
            case .detail(let param):
                currentRoute = .detail(param: param)
                path.append(route)
            case .entry:
                currentRoute = .entry
            }
        case .navigateBack:
            if !path.isEmpty {
                path.removeLast()
                currentRoute = .main(initialTab: currentMainTab)
            }
        }
    }
}

struct ContentView: View {
    @StateObject private var viewModel = NavigationViewModel()

    var body: some View {
        NavigationStack(path: $viewModel.path) {
            rootContent
                .navigationDestination(for: Route.self) { route in
                    switch route {
                    case .detail:
                        DetailScreenView(
                            onNavigationAction: viewModel.onAction
                        )
                    case .main(let initialTab):
                        MainScreenView(
                            initialTab: initialTab,
                            onNavigationAction: viewModel.onAction
                        )
                    case .entry:
                        EmptyView()
                    }
                }
                .onAppear {
                    if case .entry = viewModel.currentRoute {
                        viewModel.onAction(.navigateToMain())
                    }
                }
        }
    }

    @ViewBuilder
    private var rootContent: some View {
        switch viewModel.currentRoute {
        case .main(let initialTab):
            MainScreenView(
                initialTab: initialTab,
                onNavigationAction: viewModel.onAction
            )
        case .entry:
            ProgressView()
        case .detail:
            // Detail is pushed through NavigationStack path.
            MainScreenView(
                initialTab: viewModel.currentMainTab,
                onNavigationAction: viewModel.onAction
            )
        }
    }
}

private struct MainScreenView: View {
    let initialTab: String?
    let onNavigationAction: (NavigationAction) -> Void

    @State private var selectedTab = "home"

    var body: some View {
        VStack(spacing: 0) {
            Text("ConCafe")
                .font(.headline)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)

            TabView(selection: $selectedTab) {
                HomeScreenView(onNavigationAction: onNavigationAction)
                    .tabItem { Label("홈", systemImage: "house.fill") }
                    .tag("home")

                ExploreScreenView()
                    .tabItem { Label("탐색", systemImage: "magnifyingglass") }
                    .tag("explore")
            }
        }
        .onAppear {
            selectedTab = initialTab ?? "home"
        }
    }
}

private struct HomeScreenView: View {
    let onNavigationAction: (NavigationAction) -> Void

    var body: some View {
        VStack(spacing: 12) {
            Text("홈 화면")
            Button("상세화면 이동") {
                onNavigationAction(.navigateToDetail(id: "id"))
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private struct ExploreScreenView: View {
    var body: some View {
        Text("탐색")
            .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private struct DetailScreenView: View {
    let onNavigationAction: (NavigationAction) -> Void

    var body: some View {
        VStack {
            Text("상세")
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button {
                    onNavigationAction(.navigateBack)
                } label: {
                    Image(systemName: "arrow.backward")
                }
            }
            ToolbarItem(placement: .principal) {
                Text("Detail")
            }
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
