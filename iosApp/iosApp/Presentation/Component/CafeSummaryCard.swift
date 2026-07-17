//
//  CafeSummaryCard.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/09.
//

import SwiftUI
import UIKit

struct CafeSummaryCard: View {
    @Environment(\.colorScheme) private var colorScheme

    let name: String

    let rating: String

    let conceptType: String?

    let location: String

    let thumbnailImage: String?

    let showLocationIcon: Bool

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
                    .foregroundStyle(colorScheme == .dark ? .white : .primary)
                    .lineLimit(1)
                if let conceptType = conceptType?.trimmingCharacters(in: .whitespacesAndNewlines), !conceptType.isEmpty {
                    Text(conceptType)
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(ConCafeColors.primary)
                        .lineLimit(1)
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
                            .foregroundStyle(colorScheme == .dark ? Color.white.opacity(0.78) : .secondary)
                            .lineLimit(1)
                    }
                    Spacer(minLength: 8)
                    if let trailingLabel {
                        Text(trailingLabel)
                            .font(.caption)
                            .foregroundColor(ConCafeColors.primary)
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

            ZStack(alignment: .topTrailing) {
                placeholderCafeImage
                CachedAsyncImage(
                    url: ImageUrlUtils.normalizedRemoteUrl(from: thumbnailImage),
                    placeholder: EmptyView()
                )
                .frame(width: imageSize.width, height: imageSize.height)
                .clipped()
                RatingBox(rating: rating)
                    .padding(.top, 8)
                    .padding(.trailing, 8)
            }
        }
    }

    private var placeholderCafeImage: some View {
        LinearGradient(
            colors: [ConCafeColors.warningContainer, ConCafeColors.warningContainer],
            startPoint: .top,
            endPoint: .bottom
        )
        .overlay(
            Image(systemName: "building.2.fill")
                .foregroundStyle(Color.white.opacity(0.85))
        )
    }
}
