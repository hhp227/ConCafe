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
    
    var body: some View {
        VStack(spacing: 12) {
            HStack(spacing: 10) {
                Image(systemName: "star.fill")
                    .font(.system(size: 28))
                    .foregroundStyle(Color.yellow)
                VStack(alignment: .leading, spacing: 2) {
                    Text(String(format: "%.1f", detail.cafe.ratingAvg))
                        .font(.title2.bold())
                    Text("\(detail.cafe.reviewCount)개 리뷰")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
                Spacer()
            }
            .padding(16)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
            if reviews.isEmpty {
                emptyCard("아직 등록된 리뷰가 없습니다.")
            } else {
                ForEach(reviews, id: \.id) { review in
                    VStack(alignment: .leading, spacing: 10) {
                        HStack {
                            HStack(spacing: 8) {
                                Text(review.userNickname)
                                    .font(.subheadline.weight(.semibold))
                                if review.verified {
                                    Text("방문인증")
                                        .font(.caption2.weight(.semibold))
                                        .foregroundStyle(.white)
                                        .padding(.horizontal, 8)
                                        .padding(.vertical, 4)
                                        .background(Color(hex: "EF6797"))
                                        .clipShape(Capsule())
                                }
                            }
                            Spacer()
                            Text(review.createdDate)
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        HStack(spacing: 2) {
                            ForEach(0..<5, id: \.self) { index in
                                Image(systemName: "star.fill")
                                    .font(.caption)
                                    .foregroundStyle(index < Int(review.rating) ? Color.yellow : Color(hex: "E1E1E1"))
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
                        Text(review.content)
                            .font(.subheadline)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(16)
                    .background(Color.white)
                    .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
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
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
    }
}
