//
//  CapsuleDropdownLabel.swift
//  ConCafe
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
                .foregroundStyle(ConCafeColors.textSecondary)
            Image(systemName: "chevron.down")
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 8)
        .background(ConCafeColors.surfaceVariant)
        .clipShape(Capsule())
    }
}

struct CapsuleDropdownLabel_Previews: PreviewProvider {
    static var previews: some View {
        CapsuleDropdownLabel(text: "텍스트")
    }
}
