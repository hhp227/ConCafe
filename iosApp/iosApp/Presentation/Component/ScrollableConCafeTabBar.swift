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
                                    ? ConCafeColors.primary
                                    : ConCafeColors.textSecondary
                                )
                                .padding(.horizontal, 16)
                        }
                        .frame(minWidth: 56, maxHeight: .infinity, alignment: .center)
                        .overlay(alignment: .bottom) {
                            Rectangle()
                                .fill(selectedIndex == index ? ConCafeColors.primary : .clear)
                                .frame(maxWidth: .infinity)
                                .frame(height: 2)
                        }
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .frame(height: 48)
                }
            }
            .padding(.horizontal, 12)
            .frame(height: 48)
            .background(backgroundColor, ignoresSafeAreaEdges: [])
        }
        .background(backgroundColor, ignoresSafeAreaEdges: [])
        .frame(height: 48)
    }
}
