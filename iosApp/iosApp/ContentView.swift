import SwiftUI
import Shared
import KMPNativeCoroutinesAsync

struct ContentView: View {
    @StateObject private var viewModel = ContentViewModel()

    var body: some View {
        ZStack(alignment: .top) {
            AppNavigationView()
            if let networkAlertState = viewModel.uiState.networkAlertState,
               networkAlertState.isVisible {
                NetworkStatusBannerView(
                    message: networkAlertState.message,
                    isConnected: networkAlertState.isConnected
                )
                .transition(.move(edge: .top).combined(with: .opacity))
                .zIndex(1)
            }
        }
        .animation(
            .easeInOut(duration: 0.2),
            value: viewModel.uiState.networkAlertState?.isVisible == true
        )
    }
}

@MainActor
final class ContentViewModel: ObservableObject {
    private let observeNetworkAlertStateUseCase: ObserveNetworkAlertStateUseCase

    @Published private(set) var uiState = ContentUiState()

    private var tasks: [TaskKey: Task<Void, Never>] = [:]

    private func observeNetworkAlertState() {
        tasks[.observeNetworkAlert]?.cancel()
        tasks[.observeNetworkAlert] = Task {
            do {
                for try await networkAlertState in asyncSequence(for: observeNetworkAlertStateUseCase.invoke()) {
                    await MainActor.run {
                        uiState.networkAlertState = networkAlertState
                    }
                }
            } catch {
                await MainActor.run {
                    uiState.networkAlertState = nil
                }
            }
        }
    }

    init(
        observeNetworkAlertStateUseCase: ObserveNetworkAlertStateUseCase = KoinInitializerKt.resolveObserveNetworkAlertStateUseCase()
    ) {
        self.observeNetworkAlertStateUseCase = observeNetworkAlertStateUseCase

        observeNetworkAlertState()
    }

    deinit {
        tasks.values.forEach { $0.cancel() }
        tasks.removeAll()
    }

    private enum TaskKey {
        case observeNetworkAlert
    }
}

struct ContentUiState {
    var networkAlertState: NetworkAlertState? = nil
}

private struct NetworkStatusBannerView: View {
    let message: String

    let isConnected: Bool

    var body: some View {
        Text(message)
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(isConnected ? Color(hex: "166534") : Color(hex: "991B1B"))
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .background(isConnected ? Color(hex: "DCFCE7") : Color(hex: "FEE2E2"))
            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
            .padding(.horizontal, 16)
            .padding(.top, 8)
            .accessibilityLabel(message)
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
