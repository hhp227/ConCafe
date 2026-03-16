//
//  ConCafeTabBar.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import SwiftUI

struct ConCafeTabBar: View {
    let labels: [String]
    
    let selectedIndex: Int
    
    let backgroundColor: Color
    
    let onSelect: (Int) -> Void

    var body: some View {
        GeometryReader { geometry in
            let itemCount = max(labels.count, 1)
            let itemWidth = geometry.size.width / CGFloat(itemCount)

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
                        }
                        .frame(
                            maxWidth: .infinity,
                            maxHeight: .infinity,
                            alignment: .center
                        )
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                }
            }
            .overlay(alignment: .bottomLeading) {
                Rectangle()
                    .fill(Color(hex: "EF6797"))
                    .frame(width: itemWidth, height: 2)
                    .offset(x: CGFloat(selectedIndex) * itemWidth)
                    .animation(.easeInOut(duration: 0.2), value: selectedIndex)
            }
            .background(backgroundColor)
        }
        .frame(height: 48)
        .background(backgroundColor)
    }
}
