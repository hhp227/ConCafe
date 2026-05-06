import SwiftUI
import Shared

struct ContentView: View {
    @StateObject private var viewModel = ContentViewModel()

    var body: some View {
        ZStack(alignment: .top) {
            AppNavigationView(
                hasUnreadNotifications: viewModel.uiState.hasUnreadNotifications,
                onRefreshUnreadNotificationCount: { viewModel.onAction(.refreshUnreadNotificationCount) }
            )
            if let networkAlertState = viewModel.uiState.networkAlertState, networkAlertState.isVisible {
                NetworkStatusBannerView(
                    message: networkAlertState.message,
                    isConnected: networkAlertState.isConnected
                )
                .transition(.move(edge: .top).combined(with: .opacity))
                .zIndex(1)
            }
        }
        .preferredColorScheme(viewModel.uiState.themeMode.colorScheme)
        .animation(
            .easeInOut(duration: 0.2),
            value: viewModel.uiState.networkAlertState?.isVisible == true
        )
        .onReceive(viewModel.event) { event in
            switch event {
            case .syncPushToken:
                let token = PushTokenBridge.shared.currentToken()

                viewModel.onAction(.syncPushToken(token: token))
            }
        }
        .onReceive(NotificationCenter.default.publisher(for: .pushTokenUpdated)) { notification in
            let token = notification.userInfo?["token"] as? String ?? ""

            viewModel.onAction(.syncPushToken(token: token))
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
