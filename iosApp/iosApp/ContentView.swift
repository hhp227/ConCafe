import SwiftUI
import Shared

struct ContentView: View {
    @StateObject private var viewModel = ContentViewModel()

    var body: some View {
        ZStack(alignment: .top) {
            AppNavigationView()
            if let networkAlertState = viewModel.uiState.networkAlertState, networkAlertState.isVisible {
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

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
