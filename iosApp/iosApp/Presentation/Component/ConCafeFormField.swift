//
//  ConCafeFormField.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/11.
//

import SwiftUI
import UIKit

struct ConCafeFormField<Trailing: View>: View {
    let label: String

    @Binding var text: String

    @ViewBuilder let leadingContent: () -> Trailing

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

extension ConCafeFormField where Trailing == EmptyView {
    init(
        label: String,
        text: Binding<String>
    ) {
        self.label = label
        self._text = text
        self.leadingContent = { EmptyView() }
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
            TextEditor(text: $text)
                .frame(minHeight: 120)
                .background(TextEditorClearBackgroundView())
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

private struct TextEditorClearBackgroundView: UIViewRepresentable {
    func makeUIView(context: Context) -> UIView {
        let view = UIView()
        DispatchQueue.main.async {
            UITextView.appearance().backgroundColor = .clear
        }
        return view
    }

    func updateUIView(_ uiView: UIView, context: Context) {}
}
