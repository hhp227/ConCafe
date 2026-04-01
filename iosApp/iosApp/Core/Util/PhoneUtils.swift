//
//  PhoneUtils.swift
//  ConCafe
//

import SwiftUI
import UIKit

func formatKoreanPhoneNumber(_ input: String) -> String {
    let digits = String(input.filter { $0.isNumber }.prefix(11))
    if digits.count <= 3 { return digits }
    if digits.count <= 7 { return "\(digits.prefix(3))-\(digits.dropFirst(3))" }
    return "\(digits.prefix(3))-\(digits.dropFirst(3).prefix(4))-\(digits.dropFirst(7))"
}

/// 전화번호 전용 UITextField wrapper.
/// 숫자만 허용하며 010-XXXX-XXXX 포맷으로 자동 변환,
/// 대시 삽입 후에도 커서를 항상 끝에 유지한다.
struct PhoneTextField: UIViewRepresentable {
    @Binding var text: String

    var placeholder: String = "010-1234-5678"

    func makeUIView(context: Context) -> UITextField {
        let tf = UITextField()
        tf.keyboardType = .phonePad
        tf.placeholder = placeholder
        tf.font = .systemFont(ofSize: 16)
        tf.borderStyle = .none
        tf.addTarget(
            context.coordinator,
            action: #selector(Coordinator.editingChanged(_:)),
            for: .editingChanged
        )
        return tf
    }

    func updateUIView(_ uiView: UITextField, context: Context) {
        guard uiView.text != text else { return }
        uiView.text = text
        DispatchQueue.main.async {
            let end = uiView.endOfDocument
            uiView.selectedTextRange = uiView.textRange(from: end, to: end)
        }
    }

    func makeCoordinator() -> Coordinator { Coordinator(text: $text) }

    final class Coordinator: NSObject {
        @Binding var text: String

        @objc func editingChanged(_ sender: UITextField) {
            let formatted = formatKoreanPhoneNumber(sender.text ?? "")
            if sender.text != formatted {
                sender.text = formatted
            }
            text = formatted
            let end = sender.endOfDocument
            sender.selectedTextRange = sender.textRange(from: end, to: end)
        }

        init(text: Binding<String>) { _text = text }
    }
}
