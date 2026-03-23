//
//  ConCafeCastCard.swift
//  ConCafe
//
//  Created by Codex on 2026/03/12.
//

import SwiftUI
import Foundation

struct ConCafeCastCard: View {
    let name: String

    let subtitle: String

    var imageUrl: String? = nil

    var containerColor = Color(hex: "FFF9FC")

    var containerCornerRadius: CGFloat = 18

    var imageCornerRadius: CGFloat = 16

    var contentPadding: CGFloat = 10

    var imageHeight: CGFloat = 130

    var subtitleLineLimit: Int = 1

    var metaText: String? = nil

    var conceptRole: String? = nil

    var isWorking = false

    let onTap: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            GeometryReader { proxy in
                ZStack {
                    if let raw = imageUrl?.trimmingCharacters(in: .whitespacesAndNewlines), !raw.isEmpty,
                       let url = URL(string: raw) {
                        CachedAsyncImage(
                            url: url,
                            placeholder: Color.clear
                        )
                    } else {
                        LinearGradient(
                            colors: [Color(hex: "FFDCE8"), Color(hex: "FFC4D8")],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                    }
                }
                .frame(width: proxy.size.width, height: proxy.size.height)
                .clipShape(RoundedRectangle(cornerRadius: imageCornerRadius, style: .continuous))
                .clipped()
            }
            .frame(height: imageHeight)
            .overlay(alignment: .topTrailing) {
                if isWorking || !(conceptRole?.isEmpty ?? true) {
                    HStack(spacing: 6) {
                        if isWorking {
                            Text("출근중")
                                .font(.caption2.weight(.semibold))
                                .foregroundStyle(.white)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 4)
                                .background(Color.green)
                                .clipShape(Capsule())
                        }
                        if let conceptRole, !conceptRole.isEmpty {
                            Text(conceptRole.uppercased())
                                .font(.caption2.weight(.bold))
                                .foregroundStyle(Color.white.opacity(0.9))
                        }
                    }
                    .padding(12)
                }
            }
            VStack(alignment: .leading, spacing: 4) {
                Text(name)
                    .font(.subheadline.weight(.semibold))
                    .lineLimit(1)
                Text(subtitle)
                    .font(.caption)
                    .foregroundStyle(Color(hex: "7E7E7E"))
                    .lineLimit(subtitleLineLimit)
                    .frame(minHeight: subtitleLineLimit == 2 ? 28 : nil, alignment: .topLeading)
                    .fixedSize(horizontal: false, vertical: true)
                if let metaText, !metaText.isEmpty {
                    Text(metaText)
                        .font(.caption)
                        .foregroundStyle(Color(hex: "EF6797"))
                }
            }
            .padding(contentPadding)
        }
        .background(containerColor)
        .clipShape(RoundedRectangle(cornerRadius: containerCornerRadius, style: .continuous))
        .contentShape(RoundedRectangle(cornerRadius: containerCornerRadius, style: .continuous))
        .onTapGesture(perform: onTap)
    }
}
