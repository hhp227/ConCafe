import SwiftUI
import UIKit
import Shared

struct ContentView: View {
    @StateObject private var viewModel = ContentViewModel()

    @State private var availableUpdate: AppUpdateInfo?

    var body: some View {
        ZStack(alignment: .top) {
            AppNavigationView(
                hasUnreadNotifications: viewModel.uiState.hasUnreadNotifications,
                onRefreshUnreadNotificationCount: { viewModel.onAction(.refreshUnreadNotificationCount) }
            )
            .id(viewModel.uiState.brandTheme)
            if let networkAlertState = viewModel.uiState.networkAlertState, networkAlertState.isVisible {
                NetworkStatusBannerView(
                    message: networkAlertState.message,
                    isConnected: networkAlertState.isConnected
                )
                .transition(.move(edge: .top).combined(with: .opacity))
                .zIndex(1)
            }
        }
        .tint(ConCafeColors.primary)
        .preferredColorScheme(viewModel.uiState.themeMode.colorScheme)
        .animation(
            .easeInOut(duration: 0.2),
            value: viewModel.uiState.networkAlertState?.isVisible == true
        )
        .task {
            viewModel.onAction(
                .checkAppUpdate(
                    storePlatform: storePlatformIos,
                    storeId: Bundle.main.bundleIdentifier ?? appStoreBundleId,
                    currentVersion: currentAppVersion()
                )
            )
        }
        .alert("업데이트 안내", isPresented: appUpdateAlertPresented) {
            Button("취소", role: .cancel) {}
            Button("업데이트") {
                if let urlString = availableUpdate?.storeUrl,
                   let url = URL(string: urlString) {
                    UIApplication.shared.open(url)
                }
            }
        } message: {
            Text("새 버전 \(availableUpdate?.latestVersion ?? "")이 출시되었습니다. App Store에서 업데이트할 수 있습니다.")
        }
        .onReceive(viewModel.event) { event in
            switch event {
            case .showAppUpdate(let updateInfo):
                availableUpdate = updateInfo
            case .syncPushToken:
                let token = PushTokenBridge.shared.currentToken()

                viewModel.onAction(.syncPushToken(token: token))
            }
        }
        .onReceive(NotificationCenter.default.publisher(for: .pushTokenUpdated)) { notification in
            let token = notification.userInfo?["token"] as? String ?? ""

            viewModel.onAction(.syncPushToken(token: token))
        }
        .protectedFromScreenCapture()
    }

    private var appUpdateAlertPresented: Binding<Bool> {
        Binding(
            get: { availableUpdate != nil },
            set: { isPresented in
                if !isPresented {
                    availableUpdate = nil
                }
            }
        )
    }

    private func currentAppVersion() -> String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "0"
    }

    private var appStoreBundleId: String {
        "com.hhp227.ConCafe"
    }

    private var storePlatformIos: String {
        "IOS"
    }

    init() {
        AppBarAppearance.configureDefaultAppearance()
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
