//
//  CafeSummaryCard.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/09.
//

import SwiftUI

struct CafeSummaryCard: View {
    let name: String

    let rating: String

    let location: String

    let thumbnailImage: String?

    let trailingLabel: String?

    let onTap: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            cafeImage
                .frame(height: 120)
                .clipped()
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            VStack(alignment: .leading, spacing: 4) {
                Text(name)
                    .font(.subheadline.weight(.semibold))
                    .lineLimit(1)
                Text("⭐ \(rating)")
                    .font(.caption)
                HStack(alignment: .center) {
                    Text("📍 \(location)")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                    Spacer(minLength: 8)
                    if let trailingLabel {
                        Text(trailingLabel)
                            .font(.caption)
                            .foregroundColor(Color(hex: "EF6797"))
                            .fontWeight(.semibold)
                            .lineLimit(1)
                    }
                }
            }
            .padding(.horizontal, 4)
        }
        .contentShape(Rectangle())
        .onTapGesture {
            onTap()
        }
    }

    @ViewBuilder
    private var cafeImage: some View {
        if let thumbnailImage,
           let url = URL(string: thumbnailImage) {
            AsyncImage(url: url) { phase in
                switch phase {
                case .empty:
                    placeholderCafeImage
                case .success(let image):
                    image
                        .resizable()
                        .scaledToFill()
                case .failure:
                    placeholderCafeImage
                @unknown default:
                    placeholderCafeImage
                }
            }
        } else {
            placeholderCafeImage
        }
    }

    private var placeholderCafeImage: some View {
        LinearGradient(
            colors: [Color(hex: "FFE2D2"), Color(hex: "FFC9A9")],
            startPoint: .top,
            endPoint: .bottom
        )
        .overlay(
            Image(systemName: "building.2.fill")
                .foregroundStyle(Color.white.opacity(0.85))
        )
    }
}
