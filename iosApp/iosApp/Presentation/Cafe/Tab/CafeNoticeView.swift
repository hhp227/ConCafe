//
//  CafeNoticeView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/08.
//

import SwiftUI
import UIKit
import Shared

struct CafeNoticeView: View {
    let events: [CafeEventManagementItem]

    let notices: [CafeNoticeManagementItem]

    let canLoadMore: Bool

    let isLoadingMore: Bool

    let onLoadMore: () -> Void
    
    let onPagingTriggerDisappear: () -> Void

    let onEventTap: (String) -> Void

    @State private var expandedNoticeIds: Set<String> = []

    private let contentPadding: CGFloat = 16

    var body: some View {
        if events.isEmpty && notices.isEmpty {
            emptyCard(
                String(localized: String.LocalizationValue("cafe_notice_empty"), table: "Localizable")
            )
            .padding(.horizontal, contentPadding)
        } else {
            LazyVStack(spacing: 12) {
                Text(String(localized: String.LocalizationValue("noticeevent_tab_event"), table: "Localizable"))
                    .font(.headline.weight(.bold))
                    .foregroundStyle(UITraitCollection.current.userInterfaceStyle == .dark ? .white : ConCafeColors.textPrimary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, contentPadding)
                if !events.isEmpty {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 12) {
                            ForEach(events, id: \.id) { event in
                                eventCard(event)
                                    .frame(width: 276)
                                    .contentShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                                    .onTapGesture {
                                        onEventTap(event.id)
                                    }
                            }
                        }
                        .padding(.horizontal, contentPadding)
                    }
                } else {
                    emptyCard(String(localized: String.LocalizationValue("noticeevent_empty_event"), table: "Localizable"))
                        .padding(.horizontal, contentPadding)
                }
                Spacer()
                    .frame(height: 6)
                Text(String(localized: String.LocalizationValue("noticeevent_tab_notice"), table: "Localizable"))
                    .font(.headline.weight(.bold))
                    .foregroundStyle(UITraitCollection.current.userInterfaceStyle == .dark ? .white : ConCafeColors.textPrimary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, contentPadding)
                ForEach(notices, id: \.id) { notice in
                    VStack(alignment: .leading, spacing: 8) {
                        HStack(alignment: .top) {
                            Text(notice.title)
                                .font(.subheadline.weight(.semibold))
                                .foregroundStyle(.primary)
                            Spacer()
                            Text(notice.displayDate)
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        Text(notice.content)
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                            .lineLimit(expandedNoticeIds.contains(notice.id) ? nil : 3)
                            .truncationMode(.tail)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(16)
                    .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
                    .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
                    .contentShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
                    .padding(.horizontal, contentPadding)
                    .onTapGesture {
                        if expandedNoticeIds.contains(notice.id) {
                            expandedNoticeIds.remove(notice.id)
                        } else {
                            expandedNoticeIds.insert(notice.id)
                        }
                    }
                    .id(notice.id)
                }
                if notices.isEmpty {
                    emptyCard(String(localized: String.LocalizationValue("cafe_notice_empty"), table: "Localizable"))
                        .padding(.horizontal, contentPadding)
                } else if canLoadMore || isLoadingMore {
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
                                .padding(.vertical, 8)
                        }
                        Color.clear
                            .frame(height: 1)
                            .onDisappear {
                                onPagingTriggerDisappear()
                            }
                    }
                }
            }
            .padding(.horizontal, -contentPadding)
        }
    }

    private func eventCard(_ event: CafeEventManagementItem) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            ZStack {
                if let imageUrl = resolvedImageUrl(event.imageUrl) {
                    GeometryReader { proxy in
                        CachedAsyncImage(url: imageUrl, placeholder: Color.clear, displaySize: .medium)
                            .frame(width: proxy.size.width, height: proxy.size.height)
                            .clipped()
                    }
                } else {
                    LinearGradient(
                        colors: [ConCafeColors.surfaceTint, ConCafeColors.primaryContainer],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                }
            }
            .frame(height: 172)
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))

            VStack(alignment: .leading, spacing: 4) {
                Text(event.title)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.primary)
                    .lineLimit(2)
                HStack(spacing: 4) {
                    Image(systemName: "calendar")
                        .font(.caption2)
                        .foregroundStyle(ConCafeColors.textMuted)
                    Text(event.periodText)
                        .font(.caption2)
                        .foregroundStyle(ConCafeColors.textMuted)
                        .lineLimit(1)
                }
            }
            .padding(.horizontal, 4)
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
            .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
            .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
    }
}

struct CafeNoticeView_Previews: PreviewProvider {
    static var previews: some View {
        CafeNoticeView(events: [], notices: [], canLoadMore: false, isLoadingMore: false, onLoadMore: {}, onPagingTriggerDisappear: {}, onEventTap: { _ in })
    }
}
