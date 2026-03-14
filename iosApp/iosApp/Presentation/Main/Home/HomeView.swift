//
//  HomeView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/05.
//

import Foundation
import SwiftUI
import Shared

struct HomeView: View {
    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel = HomeViewModel()
    
    @State private var currentBannerPage = 0
    
    private let bannerTimer = Timer.publish(every: 3.0, on: .main, in: .common).autoconnect()

    var body: some View {
        HomeContentView(
            uiState: viewModel.uiState,
            currentBannerPage: $currentBannerPage,
            onAction: viewModel.onAction
        )
        .onReceive(viewModel.event) { event in
            switch event {
            case .navigateToExternalLink(let title, let url):
                onNavigationAction(.navigateToExternalLink(title: title, url: url))
            case .navigateToCast(let id):
                onNavigationAction(.navigateToCast(id: id))
            case .navigateToCafe(let id):
                onNavigationAction(.navigateToCafe(id: id))
            }
        }
        .onAppear {
            if currentBannerPage >= viewModel.uiState.banners.count {
                currentBannerPage = 0
            }
        }
        .onReceive(bannerTimer) { _ in
            guard viewModel.uiState.banners.count > 1 else { return }
            withAnimation(.easeInOut(duration: 0.35)) {
                currentBannerPage = (currentBannerPage + 1) % viewModel.uiState.banners.count
            }
        }
    }
}

private struct HomeContentView: View {
    var uiState: HomeUiState
    
    @Binding var currentBannerPage: Int
    
    let onAction: (HomeAction) -> Void
    
    private var cafeNameById: [String: String] {
        Dictionary(uniqueKeysWithValues: uiState.nearbyCafes.map { ($0.id, $0.name) })
    }
    
    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                bannerSection
                popularCastSection
                nearbyCafeSection
                birthdaySection
                noticeSection
            }
            .padding(.vertical, 16)
        }
        .background(Color(hex: "FFF9FC"))
    }
    
    private var bannerSection: some View {
        VStack(spacing: 10) {
            TabView(selection: $currentBannerPage) {
                ForEach(Array(uiState.banners.enumerated()), id: \.element.id) { index, banner in
                    ZStack(alignment: .bottomLeading) {
                        LinearGradient(
                            colors: [Color(hex: banner.startColorHex), Color(hex: banner.endColorHex)],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                        Text(banner.title)
                            .font(.title3.weight(.bold))
                            .foregroundColor(.white)
                            .padding(16)
                    }
                    .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
                    .padding(.horizontal, 16)
                    .contentShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
                    .onTapGesture {
                        onAction(.bannerTapped(banner))
                    }
                    .tag(index)
                }
            }
            .frame(height: 190)
            .tabViewStyle(.page(indexDisplayMode: .never))
            if uiState.banners.count > 1 {
                HStack(spacing: 6) {
                    ForEach(Array(uiState.banners.enumerated()), id: \.element.id) { index, _ in
                        RoundedRectangle(cornerRadius: 999)
                            .fill(currentBannerPage == index ? Color(hex: "EF6797") : Color(hex: "D8D8D8"))
                            .frame(width: currentBannerPage == index ? 18 : 8, height: 8)
                    }
                }
            }
        }
    }

    private var popularCastSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            SectionTitle(icon: "❤", title: "인기 메이드")
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    ForEach(uiState.popularCasts, id: \.id) { maid in
                        ConCafeCastCard(
                            name: maid.name,
                            subtitle: cafeNameById[maid.cafeId] ?? maid.cafeId,
                            metaText: "👥 \(maid.followerCount)",
                            onTap: { onAction(.maidTapped(id: maid.id)) }
                        )
                        .frame(width: 132, alignment: .leading)
                    }
                }
                .padding(.horizontal, 16)
            }
        }
    }

    private var nearbyCafeSection: some View {
        GeometryReader { geometry in
            let contentWidth = geometry.size.width
            let itemWidth = nearbyCafeItemWidth(for: contentWidth)

            VStack(alignment: .leading, spacing: 10) {
                SectionTitle(
                    icon: "📍",
                    title: "근처 메이드카페",
                    actionTitle: uiState.canLoadMoreNearbyCafes ? "더보기" : nil,
                    onAction: { onAction(.loadMoreNearbyCafes) }
                )
                ScrollView(.horizontal, showsIndicators: false) {
                    LazyHGrid(
                        rows: Array(repeating: GridItem(.fixed(92), spacing: 12), count: 3),
                        alignment: .center,
                        spacing: 12
                    ) {
                        ForEach(uiState.nearbyCafes, id: \.id) { cafe in
                            NearByCafeItem(cafe: cafe)
                            .frame(width: itemWidth, height: 92, alignment: .leading)
                            .contentShape(Rectangle())
                            .onTapGesture {
                                onAction(.cafeTapped(id: cafe.id))
                            }
                        }
                    }
                    .padding(.horizontal, 16)
                }
                .frame(height: 300)
            }
        }
        .frame(height: 334)
    }

    private func nearbyCafeItemWidth(for contentWidth: CGFloat) -> CGFloat {
        let availableWidth = max(contentWidth, 320)
        let horizontalPadding: CGFloat = 16
        let itemSpacing: CGFloat = 12
        let nextItemPeekWidth: CGFloat = 16

        if availableWidth >= 840 {
            return (availableWidth - horizontalPadding - (itemSpacing * 2) - nextItemPeekWidth) / 2
        }
        return availableWidth - horizontalPadding - itemSpacing - nextItemPeekWidth
    }

    private var birthdaySection: some View {
        VStack(alignment: .leading, spacing: 10) {
            SectionTitle(icon: "🎂", title: "생일인 메이드")
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 16) {
                    ForEach(uiState.birthdayCasts, id: \.id) { maid in
                        VStack(spacing: 8) {
                            Circle()
                                .fill(LinearGradient(colors: [Color(hex: "FFD3E2"), Color(hex: "FFB6D0")], startPoint: .top, endPoint: .bottom))
                                .frame(width: 74, height: 74)
                            Text(maid.name)
                                .font(.caption)
                        }
                        .onTapGesture {
                            onAction(.birthdayMaidTapped(id: maid.id))
                        }
                    }
                }
                .padding(.horizontal, 16)
            }
        }
    }

    private var noticeSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            SectionTitle(icon: "📢", title: "최근 카페 공지")
            VStack(spacing: 10) {
                ForEach(uiState.notices, id: \.id) { notice in
                    HStack(alignment: .top, spacing: 8) {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(notice.cafeName)
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(Color(hex: "EF6797"))
                            Text(notice.content)
                                .font(.subheadline)
                        }
                        Spacer()
                        Text(notice.relativeTime)
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                    }
                    .padding(12)
                    .background(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                }
            }
            .padding(.horizontal, 16)
        }
    }
}

private struct SectionTitle: View {
    let icon: String
    
    let title: String

    var actionTitle: String? = nil

    var onAction: (() -> Void)? = nil

    private let actionSlotWidth: CGFloat = 44

    private let actionSlotHeight: CGFloat = 24

    var body: some View {
        HStack(spacing: 6) {
            Text(icon)
            Text(title)
                .font(.headline)
            Spacer()
            Group {
                if let actionTitle, let onAction {
                    Button(actionTitle, action: onAction)
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(Color(hex: "EF6797"))
                } else {
                    Color.clear
                }
            }
            .frame(width: actionSlotWidth, height: actionSlotHeight, alignment: .trailing)
        }
        .frame(minHeight: actionSlotHeight)
        .padding(.horizontal, 16)
    }
}

private struct NearByCafeItem: View {
    let cafe: Cafe

    var body: some View {
        HStack(spacing: 12) {
            RoundedRectangle(cornerRadius: 16)
            .fill(LinearGradient(colors: [Color(hex: "FFE1C7"), Color(hex: "FFCEAE")], startPoint: .top, endPoint: .bottom))
            .frame(width: 92, height: 92)
            VStack(alignment: .leading, spacing: 4) {
                Text(cafe.name)
                .font(.subheadline.weight(.semibold))
                .lineLimit(1)
                Text("⭐ \(cafe.ratingAvg)")
                .font(.caption)
                Text(cafe.region.city)
                .font(.caption)
                .foregroundStyle(.secondary)
                .lineLimit(1)
                Text("📍 \(cafe.region.address)")
                .font(.caption)
                .foregroundStyle(Color(hex: "EF6797"))
                .lineLimit(1)
            }
            Spacer(minLength: 0)
        }
    }
}

struct HomeView_Previews: PreviewProvider {
    static var previews: some View {
        HomeContentView(uiState: .empty, currentBannerPage: Binding(get: { 1 }, set: { _ in }), onAction: { _ in })
    }
}
