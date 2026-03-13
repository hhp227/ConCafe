//
//  CafeNoticeView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/08.
//

import SwiftUI
import Shared
import UIKit

struct CafeNoticeView: View {
    let notices: [NoticeItem]

    let canLoadMore: Bool

    let isLoadingMore: Bool

    let onLoadMore: () -> Void

    @State private var expandedNoticeIds: Set<String> = []
    
    var body: some View {
        if notices.isEmpty {
            emptyCard("등록된 공지가 없습니다.")
        } else {
            VStack(spacing: 12) {
                ForEach(notices, id: \.id) { notice in
                    let isExpandable = isNoticeExpandable(notice.content)
                    VStack(alignment: .leading, spacing: 8) {
                        HStack(alignment: .top) {
                            Text(notice.title)
                                .font(.subheadline.weight(.semibold))
                            Spacer()
                            Text(notice.date)
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
                        guard isExpandable else { return }
                        if expandedNoticeIds.contains(notice.id) {
                            expandedNoticeIds.remove(notice.id)
                        } else {
                            expandedNoticeIds.insert(notice.id)
                        }
                    }
                    .onAppear {
                        if notice.id == notices.last?.id {
                            onLoadMore()
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

    private func isNoticeExpandable(_ content: String) -> Bool {
        let availableWidth = UIScreen.main.bounds.width - 64
        guard availableWidth > 0 else { return false }

        let font = UIFont.preferredFont(forTextStyle: .subheadline)
        let attributes: [NSAttributedString.Key: Any] = [.font: font]
        let boundingRect = (content as NSString).boundingRect(
            with: CGSize(width: availableWidth, height: .greatestFiniteMagnitude),
            options: [.usesLineFragmentOrigin, .usesFontLeading],
            attributes: attributes,
            context: nil
        )
        return boundingRect.height > font.lineHeight * 3.05
    }
}

struct CafeNoticeView_Previews: PreviewProvider {
    static var previews: some View {
        CafeNoticeView(notices: [], canLoadMore: false, isLoadingMore: false, onLoadMore: {})
    }
}
