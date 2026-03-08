//
//  ScrollableConCafeTabBar.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/08.
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
                    Button {
                        onSelect(index)
                    } label: {
                        ZStack {
                            backgroundColor.opacity(0.001)
                            Text(label)
                                .font(.subheadline.weight(selectedIndex == index ? .bold : .regular))
                                .foregroundStyle(
                                    selectedIndex == index
                                    ? Color(hex: "EF6797")
                                    : Color(hex: "777777")
                                )
                                .padding(.horizontal, 16)
                        }
                        .frame(minWidth: 56, maxHeight: .infinity, alignment: .center)
                        .overlay(alignment: .bottom) {
                            Rectangle()
                                .fill(selectedIndex == index ? Color(hex: "EF6797") : .clear)
                                .frame(maxWidth: .infinity)
                                .frame(height: 2)
                        }
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .frame(height: 48)
                }
            }
            .frame(height: 48)
            .background(backgroundColor)
        }
        .background(backgroundColor)
        .frame(height: 48)
    }
}
