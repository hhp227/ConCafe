//
//  CafeReviewView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/08.
//

import SwiftUI
import Shared

struct CafeReviewView: View {
    let detail: CafeDetail

    let reviews: [CafeDetailReview]

    let canLoadMore: Bool

    let isLoadingMore: Bool

    let currentUserId: String?

    let onLoadMore: () -> Void
    
    let onPagingTriggerDisappear: () -> Void

    let onAction: (CafeAction) -> Void

    var body: some View {
        VStack(spacing: 12) {
            HStack(spacing: 10) {
                Image(systemName: "star.fill")
                    .font(.system(size: 28))
                    .foregroundStyle(Color.yellow)
                VStack(alignment: .leading, spacing: 2) {
                    Text(RatingUtils.formatOneDecimal(detail.cafe.ratingAvg))
                        .font(.title2.bold())
                    Text(
                        String(
                            format: String(localized: String.LocalizationValue("cafe_review_count"), table: "Localizable"),
                            locale: Locale.current,
                            detail.cafe.reviewCount
                        )
                    )
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
                Spacer()
            }
            .padding(16)
            .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
            .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
            if reviews.isEmpty {
                emptyCard(String(localized: String.LocalizationValue("cafe_review_empty"), table: "Localizable"))
            } else {
                LazyVStack(spacing: 12) {
                    ForEach(Array(reviews.enumerated()), id: \.element.id) { _, review in
                        VStack(alignment: .leading, spacing: 10) {
                            HStack {
                                HStack(spacing: 8) {
                                    Text(review.userNickname)
                                        .font(.subheadline.weight(.semibold))
                                    if review.verified {
                                        Text(String(localized: String.LocalizationValue("cafe_review_verified"), table: "Localizable"))
                                            .font(.caption2.weight(.semibold))
                                            .foregroundStyle(.white)
                                            .padding(.horizontal, 8)
                                            .padding(.vertical, 4)
                                            .background(Color(hex: "EF6797"))
                                            .clipShape(Capsule())
                                    }
                                }
                                Spacer()
                                HStack(spacing: 4) {
                                    Text(review.createdDate)
                                        .font(.caption)
                                        .foregroundStyle(.secondary)
                                    Menu {
                                        if review.userId == currentUserId {
                                            Button(String(localized: String.LocalizationValue("cafe_review_action_edit"), table: "Localizable")) {
                                                onAction(.editReview(reviewId: review.id))
                                            }
                                            Button(String(localized: String.LocalizationValue("cafe_review_action_delete"), table: "Localizable"), role: .destructive) {
                                                onAction(.deleteReview(reviewId: review.id))
                                            }
                                        } else {
                                            Button(String(localized: String.LocalizationValue("cafe_review_action_report"), table: "Localizable"), role: .destructive) {
                                                onAction(.reportReview(reviewId: review.id))
                                            }
                                        }
                                    } label: {
                                        Image(systemName: "ellipsis")
                                            .font(.caption)
                                            .foregroundStyle(.secondary)
                                            .frame(width: 24, height: 24)
                                    }
                                }
                            }
                            HStack(spacing: 2) {
                                ForEach(0..<5, id: \.self) { starIndex in
                                    Image(systemName: "star.fill")
                                        .font(.caption)
                                        .foregroundStyle(starIndex < Int(review.rating) ? Color.yellow : Color(hex: "E1E1E1"))
                                }
                            }
                            if !review.taggedCastNames.isEmpty {
                                ScrollView(.horizontal, showsIndicators: false) {
                                    HStack(spacing: 6) {
                                        ForEach(review.taggedCastNames, id: \.self) { castName in
                                            Text(castName)
                                                .font(.caption2.weight(.semibold))
                                                .foregroundStyle(Color(hex: "C9527E"))
                                                .padding(.horizontal, 10)
                                                .padding(.vertical, 5)
                                                .background(Color(hex: "FFD1DC").opacity(0.12))
                                                .clipShape(Capsule())
                                        }
                                    }
                                }
                            }
                            if let reviewImageUrl = review.imageUrls.first(where: { !$0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }),
                               let imageUrl = URL(string: reviewImageUrl.trimmingCharacters(in: .whitespacesAndNewlines)) {
                                let trimmedImageUrl = reviewImageUrl.trimmingCharacters(in: .whitespacesAndNewlines)

                                HStack(alignment: .top, spacing: 12) {
                                    Text(review.content)
                                        .font(.subheadline)
                                        .frame(maxWidth: .infinity, alignment: .leading)
                                    CachedAsyncImage(
                                        url: imageUrl,
                                        placeholder: Color(hex: "F4EFF2")
                                    )
                                    .frame(width: 96, height: 96)
                                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                                    .clipped()
                                    .contentShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                                    .onTapGesture {
                                        onAction(.reviewImageTapped(imageUrl: trimmedImageUrl))
                                    }
                                }
                            } else {
                                Text(review.content)
                                    .font(.subheadline)
                            }
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(16)
                        .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
                        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
                        .id(review.id)
                    }
                    if canLoadMore || isLoadingMore {
                        VStack(spacing: 0) {
                            Color.clear
                                .frame(height: 1)
                                .onAppear {
                                    guard canLoadMore, !isLoadingMore else { return }
                                    onLoadMore()
                                }
                            if isLoadingMore {
                                ProgressView()
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 12)
                            } else if canLoadMore {
                                Text(String(localized: String.LocalizationValue("cafe_review_load_more_hint"), table: "Localizable"))
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 6)
                            }
                            Color.clear
                                .frame(height: 1)
                                .onDisappear {
                                    onPagingTriggerDisappear()
                                }
                        }
                    }
                }
            }
        }
    }

    private func emptyCard(_ text: String) -> some View {
        Text(text)
            .font(.subheadline)
            .foregroundStyle(.secondary)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 28)
            .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
            .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
    }
}
