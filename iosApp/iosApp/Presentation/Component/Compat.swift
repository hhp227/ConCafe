//
//  Compat.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/08.
//

import Foundation
import SwiftUI
import UIKit

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

extension View {
    func compatLargeSheetDetent() -> some View {
        modifier(CompatLargeSheetDetentModifier())
    }

    func compatFractionSheetDetent(_ fraction: CGFloat) -> some View {
        modifier(CompatFractionSheetDetentModifier(fraction: fraction))
    }
}

func compatSystemImageName(iOS16: String, fallback: String) -> String {
    if #available(iOS 16.0, *) {
        return iOS16
    } else {
        return fallback
    }
}
