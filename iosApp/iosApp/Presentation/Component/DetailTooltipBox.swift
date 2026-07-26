//
//  DetailTooltipBox.swift
//  ConCafe
//
//  Created by Codex on 2026/06/01.
//

import SwiftUI

private let detailTooltipDurationNanoseconds: UInt64 = 5_000_000_000

struct DetailTooltipBox<Content: View>: View {
    let visible: Bool

    let text: String

    let offset: CGSize

    let onShown: () -> Void

    let onDismiss: () -> Void

    let content: Content

    var body: some View {
        ZStack(alignment: .topTrailing) {
            content
            if visible {
                DetailTooltipBubble(text: text)
                    .offset(offset)
                    .onAppear {
                        onShown()
                    }
                    .task(id: visible) {
                        try? await Task.sleep(nanoseconds: detailTooltipDurationNanoseconds)
                        if !Task.isCancelled {
                            onDismiss()
                        }
                    }
            }
        }
    }

    init(
        visible: Bool,
        text: String,
        offset: CGSize = CGSize(width: 0, height: 0),
        onShown: @escaping () -> Void,
        onDismiss: @escaping () -> Void,
        @ViewBuilder content: () -> Content
    ) {
        self.visible = visible
        self.text = text
        self.offset = offset
        self.onShown = onShown
        self.onDismiss = onDismiss
        self.content = content()
    }
}

private struct DetailTooltipBubble: View {
    let text: String

    var body: some View {
        VStack(alignment: .trailing, spacing: 0) {
            DetailTooltipTriangle()
                .fill(Color(uiColor: .label))
                .frame(width: 14, height: 8)
                .padding(.trailing, 18)
            Text(text)
                .font(.caption.weight(.semibold))
                .foregroundStyle(Color(uiColor: .systemBackground))
                .multilineTextAlignment(.leading)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.horizontal, 12)
                .padding(.vertical, 10)
                .frame(maxWidth: 280, alignment: .leading)
                .background(
                    RoundedRectangle(cornerRadius: 10)
                        .fill(Color(uiColor: .label))
                )
                .shadow(color: .black.opacity(0.18), radius: 8, x: 0, y: 4)
        }
    }
}

private struct DetailTooltipTriangle: Shape {
    func path(in rect: CGRect) -> Path {
        var path = Path()
        path.move(to: CGPoint(x: rect.midX, y: rect.minY))
        path.addLine(to: CGPoint(x: rect.maxX, y: rect.maxY))
        path.addLine(to: CGPoint(x: rect.minX, y: rect.maxY))
        path.closeSubpath()
        return path
    }
}
