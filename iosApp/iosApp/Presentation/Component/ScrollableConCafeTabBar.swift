//
//  ScrollableConCafeTabBar.swift
//  ConCafe
//
//  Created by Codex on 2026/03/08.
//

import SwiftUI

struct ScrollableConCafeTabBar: View {
    let labels: [String]
    
    let selectedIndex: Int
    
    let backgroundColor: Color
    
    let onSelect: (Int) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 0) {
                ForEach(Array(labels.enumerated()), id: \.offset) { index, label in
                    let isSelected = index == selectedIndex

                    Button {
                        onSelect(index)
                    } label: {
                        VStack(spacing: 0) {
                            Spacer(minLength: 0)
                            Text(label)
                                .font(.subheadline.weight(isSelected ? .bold : .regular))
                                .foregroundStyle(
                                    isSelected
                                    ? Color(hex: "EF6797")
                                    : Color(hex: "777777")
                                )
                                .frame(maxWidth: .infinity)
                            Spacer(minLength: 0)
                            Rectangle()
                                .fill(isSelected ? Color(hex: "EF6797") : .clear)
                                .frame(maxWidth: .infinity)
                                .frame(height: 2)
                        }
                        .frame(height: 48)
                        .padding(.horizontal, 16)
                        .background(backgroundColor)
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, 12)
        }
        .background(backgroundColor)
    }
}
