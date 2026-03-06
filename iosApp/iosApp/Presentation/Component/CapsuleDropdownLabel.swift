//
//  CapsuleDropdownLabel.swift
//  iosApp
//
//  Created by 홍희표 on 2026/03/07.
//

import SwiftUI

struct CapsuleDropdownLabel: View {
    let text: String

    var body: some View {
        HStack(spacing: 6) {
            Text(text)
                .font(.caption.weight(.semibold))
                .foregroundStyle(Color(hex: "666666"))
            Image(systemName: "chevron.down")
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 8)
        .background(Color(hex: "F3F3F3"))
        .clipShape(Capsule())
    }
}

private extension Color {
    init(hex: String) {
        let value = Int(hex, radix: 16) ?? 0
        let red = Double((value >> 16) & 0xFF) / 255.0
        let green = Double((value >> 8) & 0xFF) / 255.0
        let blue = Double(value & 0xFF) / 255.0
        self.init(.sRGB, red: red, green: green, blue: blue, opacity: 1)
    }
}

struct CapsuleDropdownLabel_Previews: PreviewProvider {
    static var previews: some View {
        CapsuleDropdownLabel(text: "텍스트")
    }
}
