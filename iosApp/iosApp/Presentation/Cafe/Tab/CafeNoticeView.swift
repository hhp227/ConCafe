//
//  CafeNoticeView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/08.
//

import SwiftUI
import Shared

struct CafeNoticeView: View {
    let notices: [CafeNoticeManagementItem]

    let canLoadMore: Bool

    let isLoadingMore: Bool

    let onLoadMore: () -> Void

    @State private var expandedNoticeIds: Set<String> = []

    var body: some View {
        if notices.isEmpty {
            emptyCard("등록된 공지가 없습니다.")
        } else {
            LazyVStack(spacing: 12) {
                ForEach(notices, id: \.id) { notice in
                    VStack(alignment: .leading, spacing: 8) {
                        HStack(alignment: .top) {
                            Text(notice.title)
                                .font(.subheadline.weight(.semibold))
                            Spacer()
                            Text(notice.displayDate)
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        Text(notice.content)
                            .font(.subheadline)
                            .foregroundStyle(Color(hex: "666666"))
                            .lineLimit(expandedNoticeIds.contains(notice.id) ? nil : 3)
                            .truncationMode(.tail)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(16)
                    .background(Color.white)
                    .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
                    .contentShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
                    .onTapGesture {
                        if expandedNoticeIds.contains(notice.id) {
                            expandedNoticeIds.remove(notice.id)
                        } else {
                            expandedNoticeIds.insert(notice.id)
                        }
                    }
                }
                if isLoadingMore {
                    ProgressView()
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 8)
                } else if canLoadMore {
                    Color.clear
                        .frame(height: 1)
                        .onAppear {
                            onLoadMore()
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
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
    }
}

struct CafeNoticeView_Previews: PreviewProvider {
    static var previews: some View {
        CafeNoticeView(notices: [], canLoadMore: false, isLoadingMore: false, onLoadMore: {})
    }
}
