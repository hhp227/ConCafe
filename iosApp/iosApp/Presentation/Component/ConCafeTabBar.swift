//
//  ConCafeTabBar.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import SwiftUI

struct ConCafeTabItem: Identifiable, Equatable {
    let id: String
    let title: String
}

struct ConCafeTabBar: View {
    let items: [ConCafeTabItem]

    let selectedIndex: Int

    let backgroundColor: Color

    let onSelect: (Int) -> Void

    var body: some View {
        GeometryReader { geometry in
            let itemCount = max(items.count, 1)
            let itemWidth = geometry.size.width / CGFloat(itemCount)

            VStack(spacing: 0) {
                HStack(spacing: 0) {
                    ForEach(Array(items.enumerated()), id: \.element.id) { index, item in
                        Button {
                            onSelect(index)
                        } label: {
                            Text(item.title)
                                .font(.subheadline.weight(selectedIndex == index ? .bold : .regular))
                                .foregroundStyle(selectedIndex == index ? Color(hex: "EF6797") : Color(hex: "777777"))
                                .frame(maxWidth: .infinity)
                                .padding(.top, 10)
                                .padding(.bottom, 8)
                        }
                        .buttonStyle(.plain)
                    }
                }
                Rectangle()
                    .fill(Color.clear)
                    .frame(height: 2)
                    .overlay(alignment: .leading) {
                        Rectangle()
                            .fill(Color(hex: "EF6797"))
                            .frame(width: itemWidth, height: 2)
                            .offset(x: CGFloat(selectedIndex) * itemWidth)
                            .animation(.easeInOut(duration: 0.2), value: selectedIndex)
                    }
            }
            .background(backgroundColor)
        }
        .frame(height: 52)
    }
}
