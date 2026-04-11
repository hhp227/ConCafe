//
//  CafeNoticeView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/08.
//

import SwiftUI
import Shared

struct CafeNoticeView: View {
    let events: [CafeEventManagementItem]

    let notices: [CafeNoticeManagementItem]

    let canLoadMore: Bool

    let isLoadingMore: Bool

    let onLoadMore: () -> Void

    @State private var expandedNoticeIds: Set<String> = []

    var body: some View {
        if events.isEmpty && notices.isEmpty {
            emptyCard(String(localized: String.LocalizationValue("cafe_notice_empty"), table: "Localizable"))
        } else {
            LazyVStack(spacing: 12) {
                Text(String(localized: String.LocalizationValue("noticeevent_tab_event"), table: "Localizable"))
                    .font(.headline.weight(.bold))
                    .frame(maxWidth: .infinity, alignment: .leading)
                if !events.isEmpty {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 12) {
                            ForEach(events, id: \.id) { event in
                                eventCard(event)
                                    .frame(width: 248)
                            }
                        }
                    }
                } else {
                    emptyCard(String(localized: String.LocalizationValue("noticeevent_empty_event"), table: "Localizable"))
                }
                Spacer()
                    .frame(height: 6)
                Text(String(localized: String.LocalizationValue("noticeevent_tab_notice"), table: "Localizable"))
                    .font(.headline.weight(.bold))
                    .frame(maxWidth: .infinity, alignment: .leading)
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
                if notices.isEmpty {
                    emptyCard(String(localized: String.LocalizationValue("cafe_notice_empty"), table: "Localizable"))
                } else if isLoadingMore {
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

    private func eventCard(_ event: CafeEventManagementItem) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            ZStack {
                LinearGradient(
                    colors: [Color(hex: "FDE7EF"), Color(hex: "FCCFDF")],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                if let imageUrl = resolvedImageUrl(event.imageUrl) {
                    CachedAsyncImage(url: imageUrl, placeholder: Color.clear)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .clipped()
                }
            }
            .frame(height: 148)
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))

            VStack(alignment: .leading, spacing: 4) {
                Text(event.title)
                    .font(.subheadline.weight(.semibold))
                    .lineLimit(2)
                HStack(spacing: 4) {
                    Image(systemName: "calendar")
                        .font(.caption2)
                        .foregroundStyle(Color(hex: "8A7F8B"))
                    Text(event.periodText)
                        .font(.caption2)
                        .foregroundStyle(Color(hex: "8A7F8B"))
                        .lineLimit(1)
                }
                Text(event.statusLabel)
                    .font(.caption2)
                    .foregroundStyle(Color(hex: "7A707A"))
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 10)
        }
    }

    private func resolvedImageUrl(_ raw: String?) -> URL? {
        let trimmed = raw?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        return trimmed.isEmpty ? nil : URL(string: trimmed)
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
        CafeNoticeView(events: [], notices: [], canLoadMore: false, isLoadingMore: false, onLoadMore: {})
    }
}
