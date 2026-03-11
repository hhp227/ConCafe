//
//  ConCafeFormField.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/11.
//

import SwiftUI
import UIKit

struct ConCafeFormField<Leading: View, Trailing: View>: View {
    let label: String

    @Binding var text: String

    @ViewBuilder let leadingContent: () -> Leading

    @ViewBuilder let trailingContent: () -> Trailing

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(label)
                .font(.subheadline.weight(.medium))
                .foregroundStyle(Color(hex: "665A63"))
            HStack(spacing: 8) {
                leadingContent()
                TextField("", text: $text)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                trailingContent()
            }
            .padding(.horizontal, 16)
            .frame(height: 52)
            .background(Color(hex: "F8F5F6"))
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .stroke(Color(hex: "FFD1DC").opacity(0.3), lineWidth: 1)
            )
        }
    }
}

extension ConCafeFormField where Leading == EmptyView, Trailing == EmptyView {
    init(
        label: String,
        text: Binding<String>
    ) {
        self.label = label
        self._text = text
        self.leadingContent = { EmptyView() }
        self.trailingContent = { EmptyView() }
    }
}

extension ConCafeFormField where Trailing == EmptyView {
    init(
        label: String,
        text: Binding<String>,
        @ViewBuilder leadingContent: @escaping () -> Leading
    ) {
        self.label = label
        self._text = text
        self.leadingContent = leadingContent
        self.trailingContent = { EmptyView() }
    }
}

extension ConCafeFormField where Leading == EmptyView {
    init(
        label: String,
        text: Binding<String>,
        @ViewBuilder trailingContent: @escaping () -> Trailing
    ) {
        self.label = label
        self._text = text
        self.leadingContent = { EmptyView() }
        self.trailingContent = trailingContent
    }
}

struct ConCafeFormEditor: View {
    let label: String

    @Binding var text: String

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(label)
                .font(.subheadline.weight(.medium))
                .foregroundStyle(Color(hex: "665A63"))
            ConCafeMultilineTextView(text: $text)
                .frame(minHeight: 120)
                .padding(12)
                .background(Color(hex: "F8F5F6"))
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .stroke(Color(hex: "FFD1DC").opacity(0.3), lineWidth: 1)
                )
        }
    }
}

private struct ConCafeMultilineTextView: UIViewRepresentable {
    @Binding var text: String

    func makeCoordinator() -> Coordinator {
        Coordinator(text: $text)
    }

    func makeUIView(context: Context) -> UITextView {
        let textView = UITextView()
        textView.delegate = context.coordinator
        textView.backgroundColor = .clear
        textView.textContainerInset = .zero
        textView.textContainer.lineFragmentPadding = 0
        textView.font = UIFont.preferredFont(forTextStyle: .body)
        textView.textColor = UIColor.label
        textView.autocapitalizationType = .none
        textView.autocorrectionType = .no
        textView.isScrollEnabled = true
        return textView
    }

    func updateUIView(_ uiView: UITextView, context: Context) {
        if uiView.text != text {
            uiView.text = text
        }
    }

    final class Coordinator: NSObject, UITextViewDelegate {
        @Binding private var text: String

        init(text: Binding<String>) {
            self._text = text
        }

        func textViewDidChange(_ textView: UITextView) {
            text = textView.text
        }
    }
}
