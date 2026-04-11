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

    private let bannerTimer = Timer.publish(every: 5.0, on: .main, in: .common).autoconnect()

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
            case .navigateToSignIn:
                onNavigationAction(.navigateToSignIn)
            }
        }
        .alert(
            String(localized: String.LocalizationValue("auth_login_required_title"), table: "Localizable"),
            isPresented: Binding(
                get: { viewModel.uiState.isLoginPromptVisible },
                set: { presented in
                    if !presented {
                        viewModel.onAction(.dismissLoginPrompt)
                    }
                }
            )
        ) {
            Button(String(localized: String.LocalizationValue("common_cancel"), table: "Localizable"), role: .cancel) {
                viewModel.onAction(.dismissLoginPrompt)
            }
            Button(String(localized: String.LocalizationValue("signin_submit"), table: "Localizable")) {
                viewModel.onAction(.loginPromptSignInTapped)
            }
        } message: {
            Text(String(localized: String.LocalizationValue("auth_login_required_message"), table: "Localizable"))
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

    var body: some View {
        if !uiState.isLoading {
            ScrollView {
                VStack(spacing: 24) {
                    bannerSection
                    popularCastSection
                    nearbyCafeSection
                    if !uiState.birthdayCasts.isEmpty {
                        birthdaySection
                    }
                    noticeSection
                }
                .padding(.vertical, 16)
            }
            .background(Color(hex: "FFF9FC"))
        } else {
            ZStack {
                Color(hex: "FFF9FC")
                    .ignoresSafeArea()
                ProgressView()
                    .tint(Color(hex: "EF6797"))
                    .controlSize(.regular)
            }
        }
    }
    
    private var bannerSection: some View {
        VStack(spacing: 10) {
            if !uiState.banners.isEmpty {
                TabView(selection: $currentBannerPage) {
                    ForEach(Array(uiState.banners.enumerated()), id: \.element.id) { index, banner in
                        HomeBannerItem(
                            banner: banner,
                            height: bannerHeight
                        )
                            .frame(maxWidth: .infinity)
                            .padding(.horizontal, 16)
                            .onTapGesture {
                                onAction(.bannerTapped(banner))
                            }
                            .tag(index)
                    }
                }
                .frame(height: bannerTabViewHeight)
                .tabViewStyle(.page(indexDisplayMode: .never))
            } else {
                HomeBannerPlaceholderCard(height: bannerHeight)
            }
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

    private var bannerHeight: CGFloat {
        let horizontalPadding: CGFloat = 32
        let contentWidth = max(UIScreen.main.bounds.width - horizontalPadding, 0)
        let calculatedHeight = contentWidth * (10.0 / 16.0)
        return min(calculatedHeight, 360)
    }

    private var bannerTabViewHeight: CGFloat {
        bannerHeight + 10
    }

    private var popularCastSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            SectionTitle(
                title: String(localized: String.LocalizationValue("home_section_popular_cast"), table: "Localizable"),
                actionTitle: uiState.canLoadMorePopularCasts ? String(localized: String.LocalizationValue("home_show_more"), table: "Localizable") : nil,
                onAction: { onAction(.loadMorePopularCasts) }
            )
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    if !uiState.popularCasts.isEmpty {
                        ForEach(uiState.popularCasts, id: \.id) { maid in
                            ConCafeCastCard(
                                name: maid.name,
                                subtitle: uiState.popularCastCafeNames[maid.cafeId] ?? maid.cafeId,
                                imageUrl: maid.profileImage,
                                metaText: String(format: String(localized: String.LocalizationValue("home_cast_followers"), table: "Localizable"), locale: Locale.current, maid.followerCount),
                                onTap: { onAction(.maidTapped(id: maid.id)) }
                            )
                            .frame(width: 132, alignment: .leading)
                        }
                    } else {
                        HomeSectionPlaceholderCard(
                            title: String(localized: String.LocalizationValue("home_popular_cast_empty_title"), table: "Localizable"),
                            description: String(localized: String.LocalizationValue("home_popular_cast_empty_desc"), table: "Localizable")
                        )
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
                    title: String(localized: String.LocalizationValue("home_section_nearby_cafe"), table: "Localizable"),
                    actionTitle: uiState.canLoadMoreNearbyCafes ? String(localized: String.LocalizationValue("home_show_more"), table: "Localizable") : nil,
                    onAction: { onAction(.loadMoreNearbyCafes) }
                )
                ScrollView(.horizontal, showsIndicators: false) {
                    Group {
                        if !uiState.nearbyCafes.isEmpty {
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
                        } else {
                            HomeSectionPlaceholderCard(
                                title: String(localized: String.LocalizationValue("home_nearby_cafe_empty_title"), table: "Localizable"),
                                description: String(localized: String.LocalizationValue("home_nearby_cafe_empty_desc"), table: "Localizable")
                            )
                            .frame(width: max(contentWidth - 32, 0), alignment: .leading)
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
            SectionTitle(title: String(localized: String.LocalizationValue("home_section_birthday_cast"), table: "Localizable"))
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 16) {
                    ForEach(uiState.birthdayCasts, id: \.id) { maid in
                        VStack(spacing: 8) {
                            GeometryReader { proxy in
                                ZStack {
                                    Circle()
                                        .fill(LinearGradient(colors: [Color(hex: "FFD3E2"), Color(hex: "FFB6D0")], startPoint: .top, endPoint: .bottom))
                                    if let rawImageUrl = maid.profileImage?.trimmingCharacters(in: .whitespacesAndNewlines),
                                       !rawImageUrl.isEmpty,
                                       let imageUrl = URL(string: rawImageUrl) {
                                        CachedAsyncImage(
                                            url: imageUrl,
                                            placeholder: Color.clear
                                        )
                                    }
                                }
                                .frame(width: proxy.size.width, height: proxy.size.height)
                                .clipShape(Circle())
                                .clipped()
                            }
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
            SectionTitle(title: String(localized: String.LocalizationValue("home_section_notice"), table: "Localizable"))
            VStack(spacing: 10) {
                if !uiState.notices.isEmpty {
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
                } else {
                    HomeSectionPlaceholderCard(
                        title: String(localized: String.LocalizationValue("home_notice_empty_title"), table: "Localizable"),
                        description: String(localized: String.LocalizationValue("home_notice_empty_desc"), table: "Localizable")
                    )
                }
            }
            .padding(.horizontal, 16)
        }
    }
}

private struct HomeBannerItem: View {
    let banner: Shared.HomeBanner

    let height: CGFloat

    private let cardShape = RoundedRectangle(cornerRadius: 20, style: .continuous)

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [Color(hex: banner.startColorHex), Color(hex: banner.endColorHex)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            if let imageUrl = resolvedRemoteImageUrl(banner.imageUrl) {
                CachedAsyncImage(
                    url: imageUrl,
                    placeholder: Color.clear
                )
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .clipped()
                LinearGradient(
                    colors: [Color.black.opacity(0.04), Color.black.opacity(0.34)],
                    startPoint: .top,
                    endPoint: .bottom
                )
            }
        }
        .frame(maxWidth: .infinity)
        .frame(height: height)
        .overlay(alignment: .bottomLeading) {
            VStack(alignment: .leading, spacing: 4) {
                Text(banner.title)
                    .font(.title3.weight(.bold))
                    .foregroundStyle(.white)
                    .shadow(color: Color.black.opacity(0.35), radius: 2, x: 0, y: 1)
                if let subtitle = trimmedSubtitle, !subtitle.isEmpty {
                    Text(subtitle)
                        .font(.subheadline.weight(.medium))
                        .foregroundStyle(.white.opacity(0.92))
                        .lineLimit(2)
                        .shadow(color: Color.black.opacity(0.3), radius: 1.5, x: 0, y: 1)
                }
            }
            .padding(18)
        }
        .clipShape(cardShape)
        .contentShape(cardShape)
        .clipped()
    }

    private var trimmedSubtitle: String? {
        banner.subtitle.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private func resolvedRemoteImageUrl(_ raw: String?) -> URL? {
        let trimmed = raw?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""

        if trimmed.isEmpty {
            return nil
        } else {
            return URL(string: trimmed)
        }
    }
}

private struct HomeBannerPlaceholderCard: View {
    let height: CGFloat

    var body: some View {
        ZStack(alignment: .leading) {
            LinearGradient(
                colors: [Color(hex: "EDE7EA"), Color(hex: "F6F2F4")],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            VStack(alignment: .leading, spacing: 6) {
                Text(String(localized: String.LocalizationValue("home_banner_placeholder_title"), table: "Localizable"))
                    .font(.headline.weight(.bold))
                    .foregroundStyle(Color(hex: "6E6671"))
                Text(String(localized: String.LocalizationValue("home_banner_placeholder_desc"), table: "Localizable"))
                    .font(.subheadline)
                    .foregroundStyle(Color(hex: "8E8794"))
            }
            .padding(16)
        }
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .padding(.horizontal, 16)
        .frame(height: height)
    }
}

private struct HomeSectionPlaceholderCard: View {
    let title: String

    let description: String

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Color(hex: "5C525D"))
            Text(description)
                .font(.caption)
                .foregroundStyle(Color(hex: "8A7F8B"))
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 14)
        .padding(.vertical, 16)
        .background(.white)
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
    }
}

private struct SectionTitle: View {
    let title: String

    var actionTitle: String? = nil

    var onAction: (() -> Void)? = nil

    private let actionSlotWidth: CGFloat = 44

    private let actionSlotHeight: CGFloat = 24

    var body: some View {
        HStack(spacing: 6) {
            Text(title)
                .font(.headline.weight(.bold))
                .foregroundStyle(Color(hex: "2B2330"))
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
            GeometryReader { geometry in
                let imageSize = geometry.size

                ZStack {
                    RoundedRectangle(cornerRadius: 16)
                        .fill(
                            LinearGradient(
                                colors: [Color(hex: "FFE1C7"), Color(hex: "FFCEAE")],
                                startPoint: .top,
                                endPoint: .bottom
                            )
                        )
                    if let resolvedImageUrl = resolvedRemoteImageUrl(cafe.thumbnailImage) {
                        CachedAsyncImage(
                            url: resolvedImageUrl,
                            placeholder: EmptyView()
                        )
                        .frame(width: imageSize.width, height: imageSize.height)
                        .clipped()
                    }
                }
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            }
            .frame(width: 92, height: 92)
            VStack(alignment: .leading, spacing: 4) {
                Text(cafe.name)
                .font(.subheadline.weight(.semibold))
                .lineLimit(1)
                if !cafe.conceptType.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    Text(cafe.conceptType)
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(Color(hex: "EF6797"))
                        .lineLimit(1)
                }
                Text(cafe.region.city)
                .font(.caption)
                .foregroundStyle(.secondary)
                .lineLimit(1)
                Text(cafe.region.address)
                .font(.caption)
                .foregroundStyle(Color(hex: "EF6797"))
                .lineLimit(1)
            }
            Spacer(minLength: 0)
        }
    }

    private func resolvedRemoteImageUrl(_ raw: String?) -> URL? {
        let trimmed = raw?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if trimmed.isEmpty {
            return nil
        }
        return URL(string: trimmed)
    }
}

struct HomeView_Previews: PreviewProvider {
    static var previews: some View {
        HomeContentView(uiState: .empty, currentBannerPage: Binding(get: { 1 }, set: { _ in }), onAction: { _ in })
    }
}
