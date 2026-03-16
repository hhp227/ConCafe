//
//  CafeCastView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/08.
//

import SwiftUI
import Shared

struct CafeCastView: View {
    let maids: [CafeDetailCast]

    let canLoadMore: Bool

    let isLoadingMore: Bool

    let onAction: (CafeAction) -> Void

    var body: some View {
        if maids.isEmpty {
            emptyCard("등록된 메이드가 없습니다.")
        } else {
            VStack(spacing: 12) {
                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                    ForEach(Array(maids.enumerated()), id: \.element.cast.id) { index, maid in
                        ConCafeCastCard(
                            name: maid.cast.name,
                            subtitle: maid.cast.desc,
                            subtitleLineLimit: 2,
                            isWorking: maid.isWorking,
                            onTap: { onAction(.maidTapped(id: maid.cast.id)) }
                        )
                        .onAppear {
                            guard index == maids.indices.last,
                                  canLoadMore,
                                  !isLoadingMore else { return }
                            onAction(.loadMoreCasts)
                        }
                    }
                }
                if canLoadMore {
                    Group {
                        if isLoadingMore {
                            ProgressView()
                                .frame(maxWidth: .infinity)
                        }
                    }
                    .padding(.top, 12)
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

struct CafeCastView_Previews: PreviewProvider {
    static var previews: some View {
        CafeCastView(maids: [], canLoadMore: false, isLoadingMore: false, onAction: { _ in })
    }
}
