//
//  CafeSummaryCard.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/09.
//

import SwiftUI
import UIKit

struct CafeSummaryCard: View {
    let name: String

    let rating: String

    let location: String

    let thumbnailImage: String?

    let showLocationIcon: Bool = true

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
                HStack(spacing: 4) {
                    Image(systemName: "star.fill")
                        .font(.caption)
                        .foregroundStyle(Color(hex: "EF6797"))
                    Text(rating)
                        .font(.caption)
                }
                HStack(alignment: .center) {
                    HStack(spacing: 4) {
                        if showLocationIcon {
                            Image(systemName: "mappin.and.ellipse")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        Text(location)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                            .lineLimit(1)
                    }
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
        GeometryReader { geometry in
            let imageSize = geometry.size

            ZStack {
                placeholderCafeImage
                if let resolvedImageUrl = resolvedRemoteImageUrl(thumbnailImage) {
                    CachedAsyncImage(
                        url: resolvedImageUrl,
                        placeholder: EmptyView()
                    )
                    .frame(width: imageSize.width, height: imageSize.height)
                    .clipped()
                }
            }
        }
    }

    private func resolvedRemoteImageUrl(_ raw: String?) -> URL? {
        let trimmed = raw?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if trimmed.isEmpty {
            return nil
        }
        return URL(string: trimmed)
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
