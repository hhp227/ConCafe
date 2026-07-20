//
//  HomeView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/05.
//

import Foundation
import UIKit
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
            case .navigateToCafeEvent(let cafeId, let eventId):
                onNavigationAction(.navigateToCafeEvent(cafeId: cafeId, eventId: eventId, showCafeButton: true))
            case .navigateToSignIn:
                onNavigationAction(.navigateToSignIn)
            case .navigateToCommunity:
                onNavigationAction(.navigateToCommunity)
            case .navigateToPostDetail(let postId):
                onNavigationAction(.navigateToPostDetail(postId: postId))
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
                    HomeBannerSection(
                        uiState: uiState,
                        currentBannerPage: $currentBannerPage,
                        onAction: onAction
                    )
                    cafeEventSection
                    popularCastSection
                    nearbyCafeSection
                    if !uiState.communityPosts.isEmpty {
                        communitySection
                    }
                    if !uiState.birthdayCasts.isEmpty {
                        birthdaySection
                    }
                }
                .padding(.vertical, 16)
            }
            .background(ConCafeColors.background)
        } else {
            ZStack {
                ConCafeColors.background
                    .ignoresSafeArea()
                ProgressView()
                    .tint(ConCafeColors.primary)
                    .controlSize(.regular)
            }
        }
    }

    private var communitySection: some View {
        VStack(alignment: .leading, spacing: 10) {
            SectionTitle(
                title: String(localized: String.LocalizationValue("home_community_latest_title"), table: "Localizable"),
                actionTitle: String(localized: String.LocalizationValue("home_community_see_all"), table: "Localizable"),
                onAction: { onAction(.communityTapped) }
            )
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    ForEach(uiState.communityPosts, id: \.id) { post in
                        HomeCommunityPostCard(post: post)
                            .frame(width: 276, height: 200)
                            .contentShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
                            .onTapGesture {
                                onAction(.communityPostTapped(postId: post.id))
                            }
                    }
                }
                .padding(.horizontal, 16)
                .compatScrollTargetLayout()
            }
            .compatViewAlignedScrollSnap()
        }
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
                        ForEach(Array(uiState.popularCasts.enumerated()), id: \.element.id) { index, maid in
                            let cafeName = uiState.popularCastCafeNames[maid.cafeId] ?? maid.cafeId
                            let cafeRegion = uiState.popularCastCafeRegions[maid.cafeId]?.trimmingCharacters(in: .whitespacesAndNewlines)
                            let subtitle = (cafeRegion?.isEmpty == false) ? "\(cafeName)(\(localizedRegionCity(cafeRegion!)))" : cafeName

                            ConCafeCastCard(
                                name: maid.name,
                                subtitle: subtitle,
                                imageUrl: maid.profileImage,
                                metaText: String(format: String(localized: String.LocalizationValue("home_cast_followers"), table: "Localizable"), locale: Locale.current, maid.followerCount),
                                onTap: { onAction(.maidTapped(id: maid.id)) }
                            )
                            .frame(width: 132, alignment: .leading)
                            .lazyListImagePrefetch(
                                index: index,
                                imageUrls: uiState.popularCasts.map { $0.profileImage },
                                aheadCount: 10,
                                displaySize: .thumbnail
                            )
                        }
                    } else {
                        HomeSectionPlaceholderCard(
                            title: String(localized: String.LocalizationValue("home_popular_cast_empty_title"), table: "Localizable"),
                            description: String(localized: String.LocalizationValue("home_popular_cast_empty_desc"), table: "Localizable")
                        )
                    }
                }
                .padding(.horizontal, 16)
                .compatScrollTargetLayout()
            }
            .compatViewAlignedScrollSnap()
        }
    }

    private var nearbyCafeSection: some View {
        GeometryReader { geometry in
            let contentWidth = geometry.size.width
            let itemWidth = nearbyCafeItemWidth(for: contentWidth)

            VStack(alignment: .leading, spacing: 10) {
                SectionTitle(
                    title: String(localized: String.LocalizationValue("home_section_nearby_cafe"), table: "Localizable"),
                    actionTitle: uiState.canLoadMoreNearbyCafes && !uiState.nearbyCafes.isEmpty ? String(localized: String.LocalizationValue("home_show_more"), table: "Localizable") : nil,
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
                                ForEach(Array(uiState.nearbyCafes.enumerated()), id: \.element.id) { index, cafe in
                                    NearByCafeItem(cafe: cafe)
                                        .frame(width: itemWidth, height: 92, alignment: .leading)
                                        .contentShape(Rectangle())
                                        .lazyListImagePrefetch(
                                            index: index,
                                            imageUrls: uiState.nearbyCafes.map { $0.thumbnailImage },
                                            aheadCount: 12,
                                            displaySize: .thumbnail
                                        )
                                        .onTapGesture {
                                            onAction(.cafeTapped(id: cafe.id))
                                        }
                                }
                            }
                            .compatScrollTargetLayout()
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
                .compatViewAlignedScrollSnap()
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

        if availableWidth >= 768 {
            return (availableWidth - horizontalPadding - (itemSpacing * 2) - nextItemPeekWidth) / 2
        }
        return availableWidth - horizontalPadding - itemSpacing - nextItemPeekWidth
    }

    private var birthdaySection: some View {
        VStack(alignment: .leading, spacing: 10) {
            SectionTitle(title: String(localized: String.LocalizationValue("home_section_birthday_cast"), table: "Localizable"))
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 16) {
                    ForEach(Array(uiState.birthdayCasts.enumerated()), id: \.element.id) { index, maid in
                        VStack(spacing: 8) {
                            GeometryReader { proxy in
                                ZStack {
                                    Circle()
                                        .fill(LinearGradient(colors: [ConCafeColors.primaryContainer, ConCafeColors.primaryContainer], startPoint: .top, endPoint: .bottom))
                                    CachedAsyncImage(
                                        url: ImageUrlUtils.normalizedRemoteUrl(from: maid.profileImage),
                                        placeholder: birthdayCastFallbackImage,
                                        displaySize: .thumbnail
                                    )
                                }
                                .frame(width: proxy.size.width, height: proxy.size.height)
                                .clipShape(Circle())
                                .clipped()
                            }
                            .frame(width: 74, height: 74)
                            Text(maid.name)
                                .font(.caption)
                                .lineLimit(1)
                            if let cafeName = uiState.birthdayCastCafeNames[maid.cafeId], !cafeName.isEmpty {
                                Text(cafeName)
                                    .font(.caption2)
                                    .foregroundStyle(.secondary)
                                    .lineLimit(1)
                            }
                        }
                        .frame(width: 74)
                        .onTapGesture {
                            onAction(.birthdayMaidTapped(id: maid.id))
                        }
                        .lazyListImagePrefetch(
                            index: index,
                            imageUrls: uiState.birthdayCasts.map { $0.profileImage },
                            aheadCount: 10,
                            displaySize: .thumbnail
                        )
                    }
                }
                .padding(.horizontal, 16)
                .compatScrollTargetLayout()
            }
            .compatViewAlignedScrollSnap()
        }
    }

    private var birthdayCastFallbackImage: some View {
        Image("maid_logo")
            .resizable()
            .scaledToFill()
    }

    private var cafeEventSection: some View {
        return VStack(alignment: .leading, spacing: 10) {
            SectionTitle(
                title: String(localized: String.LocalizationValue("home_section_ongoing_cafe_event"), table: "Localizable"),
                actionTitle: uiState.canLoadMoreCafeEvents && !uiState.cafeEvents.isEmpty ? String(localized: String.LocalizationValue("home_show_more"), table: "Localizable") : nil,
                onAction: { onAction(.loadMoreCafeEvents) }
            )
            if !uiState.cafeEvents.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 12) {
                        ForEach(Array(uiState.cafeEvents.enumerated()), id: \.element.id) { index, event in
                            HomeCafeEventCard(event: event)
                                .frame(width: 276, height: 236, alignment: .top)
                                .contentShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                                .lazyListImagePrefetch(
                                    index: index,
                                    imageUrls: uiState.cafeEvents.map { $0.imageUrl },
                                    aheadCount: 8,
                                    displaySize: .medium
                                )
                                .onTapGesture {
                                    onAction(.cafeEventTapped(cafeId: event.cafeId, eventId: event.id))
                                }
                        }
                    }
                    .padding(.horizontal, 16)
                    .compatScrollTargetLayout()
                }
                .compatViewAlignedScrollSnap()
                .frame(height: 236)
            } else {
                HomeSectionPlaceholderCard(
                    title: String(localized: String.LocalizationValue("home_ongoing_cafe_event_empty_title"), table: "Localizable"),
                    description: String(localized: String.LocalizationValue("home_ongoing_cafe_event_empty_desc"), table: "Localizable")
                )
                .padding(.horizontal, 16)
            }
        }
    }
}

private struct HomeCafeEventCard: View {
    let event: Shared.HomeCafeEvent

    private let cardCornerRadius: CGFloat = 16

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            ZStack {
                if let imageUrl = resolvedRemoteImageUrl(event.imageUrl) {
                    GeometryReader { proxy in
                        CachedAsyncImage(
                            url: imageUrl,
                            displaySize: .medium
                        )
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
            .clipShape(RoundedRectangle(cornerRadius: cardCornerRadius, style: .continuous))
            VStack(alignment: .leading, spacing: 4) {
                Text(event.cafeName)
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(ConCafeColors.primary)
                    .lineLimit(1)
                    .truncationMode(.tail)
                Text(event.title)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(UITraitCollection.current.userInterfaceStyle == .dark ? .white : .primary)
                    .lineLimit(1)
                    .truncationMode(.tail)
            }
            .padding(.horizontal, 4)
        }
        .frame(maxHeight: .infinity, alignment: .top)
    }

    private func resolvedRemoteImageUrl(_ raw: String?) -> URL? {
        let trimmed = raw?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        return trimmed.isEmpty ? nil : URL(string: trimmed)
    }
}

private struct HomeBannerSection: View {
    let uiState: HomeUiState

    @Binding var currentBannerPage: Int

    let onAction: (HomeAction) -> Void

    var body: some View {
        let containerWidth = UIScreen.main.bounds.width
        let bannerHeight = homeBannerHeight(containerWidth: containerWidth)
        let sectionHeight = bannerSectionHeight(containerWidth: containerWidth)

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
                .frame(height: bannerHeight)
                .tabViewStyle(.page(indexDisplayMode: .never))
            } else {
                HomeBannerPlaceholderCard(height: bannerHeight)
            }
            if uiState.banners.count > 1 {
                HStack(spacing: 6) {
                    ForEach(Array(uiState.banners.enumerated()), id: \.element.id) { index, _ in
                        RoundedRectangle(cornerRadius: 999)
                            .fill(currentBannerPage == index ? ConCafeColors.primary : ConCafeColors.outline)
                            .frame(width: currentBannerPage == index ? 18 : 8, height: 8)
                    }
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .top)
        .frame(height: sectionHeight)
    }

    private func homeBannerHeight(containerWidth: CGFloat) -> CGFloat {
        let horizontalPadding: CGFloat = 32
        let contentWidth = max(containerWidth - horizontalPadding, 0)
        return min(contentWidth * (10.0 / 16.0), 360)
    }

    private func bannerSectionHeight(containerWidth: CGFloat) -> CGFloat {
        let bannerHeight = homeBannerHeight(containerWidth: containerWidth)
        let indicatorHeight: CGFloat = uiState.banners.count > 1 ? 18 : 0
        return bannerHeight + indicatorHeight
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
                GeometryReader { proxy in
                    CachedAsyncImage(
                        url: imageUrl,
                        displaySize: .medium
                    )
                    .frame(width: proxy.size.width, height: proxy.size.height)
                    .clipped()
                    LinearGradient(
                        colors: [Color.black.opacity(0.04), Color.black.opacity(0.34)],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                    .frame(width: proxy.size.width, height: proxy.size.height)
                }
            }
        }
        .frame(maxWidth: .infinity)
        .frame(height: height)
        .clipShape(cardShape)
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
        .contentShape(cardShape)
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

private struct HomeCommunityPostCard: View {
    let post: Shared.CommunityPost

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 6) {
                ZStack {
                    Circle()
                        .fill(LinearGradient(
                            colors: [ConCafeColors.surfaceTint, ConCafeColors.primaryContainer],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        ))
                    if let initial = post.userNickname.first {
                        Text(String(initial))
                            .font(.caption.weight(.bold))
                            .foregroundStyle(ConCafeColors.primary)
                    }
                }
                .frame(width: 24, height: 24)
                Text(post.userNickname.isEmpty ? "익명" : post.userNickname)
                    .font(.caption.weight(.medium))
                    .foregroundStyle(ConCafeColors.textSecondary)
                    .lineLimit(1)
                Spacer()
                Text(post.displayDate)
                    .font(.caption)
                    .foregroundStyle(ConCafeColors.outlineStrong)
            }
            Text(post.title)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(ConCafeColors.textPrimary)
                .lineLimit(2)
            let trimmedContent = post.content.trimmingCharacters(in: .whitespacesAndNewlines)
            Text(trimmedContent)
                .font(.caption)
                .foregroundStyle(ConCafeColors.textSecondary)
                .lineLimit(4)
                .frame(height: 60, alignment: .topLeading)
            Divider()
                .overlay(ConCafeColors.primaryContainer.opacity(0.3))
            HStack(spacing: 10) {
                Label("\(post.likeCount)", systemImage: "heart")
                    .font(.caption)
                    .foregroundStyle(ConCafeColors.textMuted)
                Label("\(post.commentCount)", systemImage: "bubble.left")
                    .font(.caption)
                    .foregroundStyle(ConCafeColors.textMuted)
            }
        }
        .padding(14)
        .background(UITraitCollection.current.userInterfaceStyle == .dark ? Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }) : Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
        .shadow(color: UITraitCollection.current.userInterfaceStyle == .dark ? .black.opacity(0.20) : .black.opacity(0.03), radius: 8, y: 3)
    }
}

private struct HomeBannerPlaceholderCard: View {
    let height: CGFloat

    var body: some View {
        ZStack(alignment: .leading) {
            LinearGradient(
                colors: [ConCafeColors.outline, ConCafeColors.surfaceTint],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            VStack(alignment: .leading, spacing: 6) {
                Text(String(localized: String.LocalizationValue("home_banner_placeholder_title"), table: "Localizable"))
                    .font(.headline.weight(.bold))
                    .foregroundStyle(ConCafeColors.textSecondary)
                Text(String(localized: String.LocalizationValue("home_banner_placeholder_desc"), table: "Localizable"))
                    .font(.subheadline)
                    .foregroundStyle(ConCafeColors.textMuted)
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
                .foregroundStyle(ConCafeColors.textSecondary)
            Text(description)
                .font(.caption)
                .foregroundStyle(ConCafeColors.textMuted)
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

    private let actionSlotHeight: CGFloat = 24

    var body: some View {
        HStack(spacing: 6) {
            Text(title)
                .font(.headline.weight(.bold))
                .foregroundStyle(ConCafeColors.textPrimary)
                .lineLimit(1)
                .truncationMode(.tail)
            Spacer(minLength: 6)
            if let actionTitle, let onAction {
                Button(actionTitle, action: onAction)
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(ConCafeColors.primary)
                    .lineLimit(1)
                    .fixedSize(horizontal: true, vertical: false)
                    .buttonStyle(.plain)
            }
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
                                colors: [ConCafeColors.warningContainer, ConCafeColors.warningContainer],
                                startPoint: .top,
                                endPoint: .bottom
                            )
                        )
                    if let resolvedImageUrl = resolvedRemoteImageUrl(cafe.thumbnailImage) {
                        CachedAsyncImage(
                            url: resolvedImageUrl,
                            displaySize: .thumbnail
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
                .foregroundStyle(UITraitCollection.current.userInterfaceStyle == .dark ? .white : .primary)
                .lineLimit(1)
                let conceptLabel = nearbyCafeConceptLabel(cafe.conceptType)
                if !conceptLabel.isEmpty {
                    Text(conceptLabel)
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(ConCafeColors.primary)
                        .lineLimit(1)
                }
                Text(localizedRegionCity(cafe.region.city))
                .font(.caption)
                .foregroundStyle(.secondary)
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

    private func nearbyCafeConceptLabel(_ rawConceptType: String) -> String {
        let normalized = rawConceptType.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !normalized.isEmpty else {
            return ""
        }
        let lower = normalized.lowercased()
        let key: String
        if lower == "maid" || lower == "home_nearby_cafe_type_maid" || lower.contains("maid") {
            key = "home_nearby_cafe_type_maid"
        } else if lower == "butler" || lower == "home_nearby_cafe_type_butler" || lower.contains("butler") {
            key = "home_nearby_cafe_type_butler"
        } else if lower == "idol" || lower == "home_nearby_cafe_type_idol" || lower.contains("idol") {
            key = "home_nearby_cafe_type_idol"
        } else if lower == "devil" || lower == "home_nearby_cafe_type_devil" || lower.contains("devil") {
            key = "home_nearby_cafe_type_devil"
        } else if lower == "doll" || lower == "home_nearby_cafe_type_doll" || lower.contains("doll") {
            key = "home_nearby_cafe_type_doll"
        } else if lower == "cosplay" || lower == "home_nearby_cafe_type_cosplay" || lower.contains("cosplay") {
            key = "home_nearby_cafe_type_cosplay"
        } else if lower == "namjang" || lower == "home_nearby_cafe_type_namjang" || lower.contains("namjang") || lower.contains("남장") {
            key = "home_nearby_cafe_type_namjang"
        } else if lower == "yokai" || lower == "home_nearby_cafe_type_yokai" || lower.contains("yokai") || lower.contains("요괴") {
            key = "home_nearby_cafe_type_yokai"
        } else if lower == "cat" || lower == "home_nearby_cafe_type_cat" || lower.contains("cat") || lower.contains("고양이") {
            key = "home_nearby_cafe_type_cat"
        } else if lower == "other" || lower == "home_nearby_cafe_type_other" || lower.contains("other") {
            key = "home_nearby_cafe_type_other"
        } else {
            return ""
        }
        return String(localized: String.LocalizationValue(key), table: "Localizable")
    }
}

struct HomeView_Previews: PreviewProvider {
    static var previews: some View {
        HomeContentView(uiState: .empty, currentBannerPage: Binding(get: { 1 }, set: { _ in }), onAction: { _ in })
    }
}
