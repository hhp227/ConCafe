//
//  Compat.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/08.
//

import Foundation
import SwiftUI
import UIKit

enum CompatNavigationBarStyle {
    case opaque
    case transparentScrollEdge
}

struct ExploreKeyboardDismissModifier: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 16.0, *) {
            content.scrollDismissesKeyboard(.immediately)
        } else {
            content
        }
    }
}

struct ScrollViewKeyboardDismissConfigurator: UIViewRepresentable {
    func makeUIView(context: Context) -> UIView {
        let view = UIView(frame: .zero)

        DispatchQueue.main.async {
            updateKeyboardDismissMode(from: view)
        }

        return view
    }

    func updateUIView(_ uiView: UIView, context: Context) {
        DispatchQueue.main.async {
            updateKeyboardDismissMode(from: uiView)
        }
    }

    private func updateKeyboardDismissMode(from view: UIView) {
        var currentView: UIView? = view.superview

        while let unwrappedView = currentView {
            if let scrollView = unwrappedView as? UIScrollView {
                scrollView.keyboardDismissMode = .onDrag
                break
            } else {
                currentView = unwrappedView.superview
            }
        }
    }
}

struct CompatNavigationContainer<Content: View>: View {
    private let title: String?

    private let content: Content

    var body: some View {
        if #available(iOS 16.0, *) {
            NavigationStack {
                wrappedContent
            }
        } else {
            NavigationView {
                wrappedContent
            }
            .navigationViewStyle(StackNavigationViewStyle())
        }
    }

    @ViewBuilder
    private var wrappedContent: some View {
        if let title {
            content
                .navigationTitle(title)
                .navigationBarTitleDisplayMode(.inline)
        } else {
            content
        }
    }

    init(
        title: String? = nil,
        @ViewBuilder content: () -> Content
    ) {
        self.title = title
        self.content = content()
    }
}

struct CompatLargeSheetDetentModifier: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 16.0, *) {
            content.presentationDetents([.large])
        } else {
            content
        }
    }
}

struct CompatFractionSheetDetentModifier: ViewModifier {
    let fraction: CGFloat

    func body(content: Content) -> some View {
        if #available(iOS 16.0, *) {
            content.presentationDetents([.fraction(fraction)])
        } else {
            content
        }
    }
}

final class NavigationBarAppearanceHostingController: UIViewController {
    var style: CompatNavigationBarStyle = .opaque

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        applyAppearanceIfNeeded()
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        applyAppearanceIfNeeded()
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        restoreOpaqueAppearance()
    }

    func applyAppearanceIfNeeded() {
        guard let navigationBar = navigationController?.navigationBar else { return }

        let standardAppearance = Self.makeOpaqueAppearance()
        let scrollEdgeAppearance: UINavigationBarAppearance

        switch style {
        case .opaque:
            scrollEdgeAppearance = standardAppearance
        case .transparentScrollEdge:
            scrollEdgeAppearance = Self.makeTransparentAppearance()
        }

        navigationBar.standardAppearance = standardAppearance
        navigationBar.scrollEdgeAppearance = scrollEdgeAppearance
        navigationBar.compactAppearance = standardAppearance
    }

    private func restoreOpaqueAppearance() {
        guard let navigationBar = navigationController?.navigationBar else { return }

        let opaqueAppearance = Self.makeOpaqueAppearance()
        navigationBar.standardAppearance = opaqueAppearance
        navigationBar.scrollEdgeAppearance = opaqueAppearance
        navigationBar.compactAppearance = opaqueAppearance
    }

    private static func makeOpaqueAppearance() -> UINavigationBarAppearance {
        let appearance = UINavigationBarAppearance()
        appearance.configureWithOpaqueBackground()
        appearance.backgroundColor = UIColor.systemBackground
        appearance.shadowColor = UIColor.separator
        return appearance
    }

    private static func makeTransparentAppearance() -> UINavigationBarAppearance {
        let appearance = UINavigationBarAppearance()
        appearance.configureWithTransparentBackground()
        appearance.backgroundColor = .clear
        appearance.shadowColor = .clear
        return appearance
    }
}

struct NavigationBarAppearanceConfigurator: UIViewControllerRepresentable {
    let style: CompatNavigationBarStyle

    func makeUIViewController(context: Context) -> NavigationBarAppearanceHostingController {
        let controller = NavigationBarAppearanceHostingController()
        controller.view.isHidden = true
        return controller
    }

    func updateUIViewController(_ uiViewController: NavigationBarAppearanceHostingController, context: Context) {
        uiViewController.style = style
        DispatchQueue.main.async {
            uiViewController.applyAppearanceIfNeeded()
        }
    }
}

final class NavigationBarVisibilityHostingController: UIViewController {
    var hideOnDisappear: Bool = false

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        navigationController?.setNavigationBarHidden(false, animated: animated)
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)

        guard hideOnDisappear else { return }
        navigationController?.setNavigationBarHidden(true, animated: animated)
    }
}

struct NavigationBarVisibilityConfigurator: UIViewControllerRepresentable {
    let hideOnDisappear: Bool

    func makeUIViewController(context: Context) -> NavigationBarVisibilityHostingController {
        let controller = NavigationBarVisibilityHostingController()
        controller.view.isHidden = true
        return controller
    }

    func updateUIViewController(_ uiViewController: NavigationBarVisibilityHostingController, context: Context) {
        uiViewController.hideOnDisappear = hideOnDisappear
    }
}

extension View {
    func compatLargeSheetDetent() -> some View {
        modifier(CompatLargeSheetDetentModifier())
    }

    func compatFractionSheetDetent(_ fraction: CGFloat) -> some View {
        modifier(CompatFractionSheetDetentModifier(fraction: fraction))
    }

    func compatNavigationBarStyle(_ style: CompatNavigationBarStyle) -> some View {
        background(NavigationBarAppearanceConfigurator(style: style))
    }

    @ViewBuilder
    func compatNavigationBarHidden(_ hidden: Bool) -> some View {
        if #available(iOS 16.0, *) {
            toolbar(hidden ? .hidden : .visible, for: .navigationBar)
        } else {
            navigationBarHidden(hidden)
        }
    }

    func compatNavigationBarTransition(hideOnDisappear: Bool) -> some View {
        background(NavigationBarVisibilityConfigurator(hideOnDisappear: hideOnDisappear))
    }
}

func compatSystemImageName(iOS16: String, fallback: String) -> String {
    if #available(iOS 16.0, *) {
        return iOS16
    } else {
        return fallback
    }
}
