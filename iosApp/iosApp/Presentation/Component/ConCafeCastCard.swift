//
//  ConCafeCastCard.swift
//  ConCafe
//
//  Created by Codex on 2026/03/12.
//

import SwiftUI
import Foundation

struct ConCafeCastCard: View {
    @Environment(\.colorScheme) private var colorScheme

    let name: String

    let subtitle: String

    var imageUrl: String? = nil

    var fallbackImageUrl: String? = nil

    var containerColor = ConCafeColors.background

    var containerCornerRadius: CGFloat = 18

    var imageCornerRadius: CGFloat = 16

    var contentPadding: CGFloat = 10

    var imageHeight: CGFloat = 130

    var subtitleLineLimit: Int = 1

    var metaText: String? = nil

    var conceptRole: String? = nil

    var attendanceStatusText: String? = nil

    var isWorking = false

    let onTap: () -> Void

    var body: some View {
        let statusText = attendanceStatusText ?? (isWorking ? "출근중" : nil)

        VStack(alignment: .leading, spacing: 0) {
            GeometryReader { proxy in
                ZStack {
                    CastProfileImage(
                        profileImageUrl: imageUrl,
                        cafeImageUrl: fallbackImageUrl
                    )
                }
                .frame(width: proxy.size.width, height: proxy.size.height)
                .clipShape(RoundedRectangle(cornerRadius: imageCornerRadius, style: .continuous))
                .clipped()
            }
            .frame(height: imageHeight)
            .overlay(alignment: .topTrailing) {
                if !(statusText?.isEmpty ?? true) || !(conceptRole?.isEmpty ?? true) {
                    HStack(spacing: 6) {
                        if let statusText, !statusText.isEmpty {
                            Text(statusText)
                                .font(.caption2.weight(.semibold))
                                .foregroundStyle(.white)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 4)
                                .background(ConCafeColors.success)
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
                    .foregroundStyle(colorScheme == .dark ? .white : .primary)
                    .lineLimit(1)
                Text(subtitle)
                    .font(.caption)
                    .foregroundStyle(colorScheme == .dark ? Color.white.opacity(0.78) : .secondary)
                    .lineLimit(subtitleLineLimit)
                    .frame(minHeight: subtitleLineLimit == 2 ? 28 : nil, alignment: .topLeading)
                    .fixedSize(horizontal: false, vertical: true)
                if let metaText, !metaText.isEmpty {
                    Text(metaText)
                        .font(.caption)
                        .foregroundStyle(ConCafeColors.primary)
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

/// 캐스트 목록용 프로필 이미지.
/// 프로필 이미지 → 소속 카페 썸네일 → 기본 로고(maid_logo) 순서로 표시한다.
struct CastProfileImage: View {
    let profileImageUrl: String?

    let cafeImageUrl: String?

    var displaySize: ImageDisplaySize = .thumbnail

    var body: some View {
        if let url = ImageUrlUtils.normalizedRemoteUrl(from: profileImageUrl)
            ?? ImageUrlUtils.normalizedRemoteUrl(from: cafeImageUrl) {
            CachedAsyncImage(
                url: url,
                placeholder: Color.clear,
                displaySize: displaySize
            )
        } else {
            Image("maid_logo")
                .resizable()
                .scaledToFill()
        }
    }

    static func resolvedImageUrl(profileImageUrl: String?, cafeImageUrl: String?) -> String? {
        let profile = profileImageUrl?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if !profile.isEmpty { return profile }
        let cafe = cafeImageUrl?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        return cafe.isEmpty ? nil : cafe
    }
}
