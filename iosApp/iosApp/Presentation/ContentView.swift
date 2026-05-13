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
}

private extension View {
    func protectedFromScreenCapture() -> some View {
        ScreenCaptureProtectedView {
            self
        }
    }
}

private struct ScreenCaptureProtectedView<Content: View>: UIViewControllerRepresentable {
    private let content: Content

    func makeUIViewController(context: Context) -> ScreenCaptureProtectedHostingController<Content> {
        ScreenCaptureProtectedHostingController(rootView: content)
    }

    func updateUIViewController(
        _ uiViewController: ScreenCaptureProtectedHostingController<Content>,
        context: Context
    ) {
        uiViewController.rootView = content
    }

    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }
}

private final class ScreenCaptureProtectedHostingController<Content: View>: UIViewController {
    private let secureTextField = UITextField()

    private let hostingController: UIHostingController<Content>

    var rootView: Content {
        get { hostingController.rootView }
        set { hostingController.rootView = newValue }
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        configureSecureContainer()
    }

    private func configureSecureContainer() {
        view.backgroundColor = .clear
        secureTextField.isSecureTextEntry = true
        secureTextField.isUserInteractionEnabled = false
        hostingController.view.backgroundColor = .clear

        guard let secureContainerView = secureTextField.subviews.first else {
            addChild(hostingController)
            attach(hostingController.view, to: view)
            hostingController.didMove(toParent: self)
            return
        }

        secureContainerView.backgroundColor = .clear
        secureContainerView.isUserInteractionEnabled = true
        attach(secureContainerView, to: view)
        addChild(hostingController)
        attach(hostingController.view, to: secureContainerView)
        hostingController.didMove(toParent: self)
    }

    private func attach(_ childView: UIView, to parentView: UIView) {
        childView.translatesAutoresizingMaskIntoConstraints = false
        parentView.addSubview(childView)
        NSLayoutConstraint.activate([
            childView.leadingAnchor.constraint(equalTo: parentView.leadingAnchor),
            childView.trailingAnchor.constraint(equalTo: parentView.trailingAnchor),
            childView.topAnchor.constraint(equalTo: parentView.topAnchor),
            childView.bottomAnchor.constraint(equalTo: parentView.bottomAnchor)
        ])
    }

    init(rootView: Content) {
        self.hostingController = UIHostingController(rootView: rootView)
        super.init(nibName: nil, bundle: nil)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}


struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
