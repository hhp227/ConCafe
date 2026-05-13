import SwiftUI
import UIKit

struct ScreenCaptureProtectedView<Content: View>: UIViewControllerRepresentable {
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

final class ScreenCaptureProtectedHostingController<Content: View>: UIViewController {
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
