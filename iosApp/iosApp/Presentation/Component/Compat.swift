//
//  Compat.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/08.
//

import Foundation
import SwiftUI

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
