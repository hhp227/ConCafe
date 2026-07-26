//
//  RankingView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/05.
//

import SwiftUI
import Shared
import UIKit
#if canImport(GoogleMobileAds)
import GoogleMobileAds
#endif

struct RankingView: View {
    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel = RankingViewModel()

    var body: some View {
        RankingContentView(
            uiState: viewModel.uiState,
            onAction: viewModel.onAction
        )
        .onReceive(viewModel.event) { event in
            switch event {
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
            viewModel.onAction(.loadNativeAdIfNeeded)
        }
        .task(id: "\(viewModel.uiState.ads.count)-\(viewModel.uiState.selectedAdIndex)") {
            guard viewModel.uiState.ads.count > 1 else { return }
            let delayNanos: UInt64 = (viewModel.uiState.selectedAdIndex == 1 || viewModel.uiState.selectedAdIndex == 2) ? 15_000_000_000 : 5_000_000_000
            try? await Task.sleep(nanoseconds: delayNanos)
            guard !Task.isCancelled else { return }
            let nextIndex = (viewModel.uiState.selectedAdIndex + 1) % viewModel.uiState.ads.count
            viewModel.onAction(.selectAd(nextIndex))
        }
    }
}

private struct RankingContentView: View {
    let uiState: RankingUiState

    let onAction: (RankingAction) -> Void

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 16, pinnedViews: [.sectionHeaders]) {
                headerSection
                Section {
                    VStack(spacing: 16) {
                        promoBanner
                        rankingList
                    }
                } header: {
                    tabSection
                }
            }
            .padding(.vertical, 16)
        }
        .background(ConCafeColors.background)
    }

    private var headerSection: some View {
        RankingHeaderSection(
            uiState: uiState,
            onPeriodSelected: { onAction(.changePeriod($0)) },
            onRegionSelected: { onAction(.changeRegion($0)) }
        )
    }

    private var tabSection: some View {
        ConCafeTabBar(
            labels: RankingUiState.TabType.allCases.map { $0.rawValue },
            selectedIndex: RankingUiState.TabType.allCases.firstIndex(of: uiState.selectedTab) ?? 0,
            backgroundColor: ConCafeColors.background,
            onSelect: { index in
                onAction(.changeTab(RankingUiState.TabType.allCases[index]))
            }
        )
    }

    private var promoBanner: some View {
        RankingPromoBanner(
            ad: uiState.currentAd,
            selectedIndex: uiState.selectedAdIndex,
            size: uiState.ads.count,
            nativeAdHandleSlot1: uiState.nativeAdSlot1,
            nativeAdHandleSlot2: uiState.nativeAdSlot2,
            bannerHeight: uiState.bannerHeight,
            onHeightMeasured: { onAction(.updateBannerHeight($0)) },
            onSelect: { onAction(.selectAd($0)) }
        )
    }

    private var rankingList: some View {
        VStack(spacing: 12) {
            if uiState.isLoading {
                ShimmerListSkeleton(itemCount: 8, avatarSize: 64)
                    .padding(.horizontal, -16)
            } else if !uiState.rankingEntries.isEmpty {
                ForEach(uiState.rankingEntries, id: \.id) { item in
                    rankingCard(item)
                }
            } else {
                RankingEmptyPlaceholderCard()
            }
        }
        .padding(.horizontal, 16)
    }

    private func rankingCard(_ item: Shared.RankingFeedEntry) -> some View {
        RankingEntryCard(
            item: item,
            isMaid: uiState.selectedTab == .maids,
            onTap: {
                if uiState.selectedTab == .maids {
                    onAction(.tapMaid(item.id))
                } else {
                    onAction(.tapCafe(item.id))
                }
            }
        )
    }
}

private struct RankingEmptyPlaceholderCard: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(String(localized: String.LocalizationValue("ranking_empty_title"), table: "Localizable"))
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(.primary)
            Text(String(localized: String.LocalizationValue("ranking_empty_desc"), table: "Localizable"))
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 14)
        .padding(.vertical, 16)
        .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}

struct RankingHeaderSection: View {
    let uiState: RankingUiState
    
    let onPeriodSelected: (RankingPeriod) -> Void
    
    let onRegionSelected: (RankingUiState.RegionFilter) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 8) {
                Image(systemName: "trophy.fill")
                .foregroundStyle(ConCafeColors.primary)
                Text(String(localized: String.LocalizationValue("ranking_title"), table: "Localizable"))
                .font(.title2.weight(.bold))
            }
            HStack(spacing: 8) {
                Menu {
                    ForEach([RankingPeriod.weekly, RankingPeriod.monthly], id: \.self) { period in
                        Button(period.label) {
                            onPeriodSelected(period)
                        }
                    }
                } label: {
                    CapsuleDropdownLabel(text: uiState.selectedPeriod.label)
                }
                Menu {
                    ForEach(RankingUiState.RegionFilter.allCases, id: \.self) { region in
                        Button(region.rawValue) {
                            onRegionSelected(region)
                        }
                    }
                } label: {
                    CapsuleDropdownLabel(text: uiState.selectedRegion.rawValue)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 16)
    }
}

private extension RankingPeriod {
    var label: String {
        switch self {
        case .weekly:
            return String(localized: String.LocalizationValue("ranking_period_weekly"), table: "Localizable")
        case .monthly:
            return String(localized: String.LocalizationValue("ranking_period_monthly"), table: "Localizable")
        default:
            return ""
        }
    }
}


struct RankingPromoBanner: View {
    let ad: Shared.RankingPromoAd

    let selectedIndex: Int

    let size: Int

    let nativeAdHandleSlot1: (any NativeAdHandle)?

    let nativeAdHandleSlot2: (any NativeAdHandle)?

    let bannerHeight: CGFloat

    let onHeightMeasured: (CGFloat) -> Void

    let onSelect: (Int) -> Void

    var body: some View {
        ZStack {
            if selectedIndex == 1 || selectedIndex == 2 {
                LinearGradient(
                    colors: [Color(hex: ad.startColorHex), Color(hex: ad.endColorHex)],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                RankingNativeAdCard(nativeAdHandle: selectedIndex == 1 ? nativeAdHandleSlot1 : nativeAdHandleSlot2)
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                    .frame(height: bannerHeight > 0 ? bannerHeight : 120)
                    .clipped()
            } else {
                LinearGradient(
                    colors: [Color(hex: ad.startColorHex), Color(hex: ad.endColorHex)],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                VStack(spacing: 16) {
                    HStack(alignment: .top, spacing: 12) {
                        VStack(alignment: .leading, spacing: 6) {
                            HStack(spacing: 6) {
                                Image(systemName: ad.systemImageName)
                                .font(.caption.weight(.semibold))
                                Text(ad.badge)
                                .font(.caption2.weight(.semibold))
                            }
                            .foregroundStyle(.white)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 6)
                            .background(Color.white.opacity(0.22))
                            .clipShape(Capsule())
                            Text(ad.title)
                            .font(.title3.weight(.bold))
                            .foregroundStyle(.white)
                            Text(ad.subtitle)
                            .font(.headline.weight(.semibold))
                            .foregroundStyle(.white)
                            Text(ad.desc)
                            .font(.caption)
                            .foregroundStyle(.white.opacity(0.92))
                        }
                        Spacer(minLength: 8)
                        Button(String(localized: String.LocalizationValue("ranking_detail"), table: "Localizable")) { }
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(Color.primary)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 10)
                        .background(Color(uiColor: .systemBackground).opacity(0.92))
                        .clipShape(Capsule())
                    }
                }
                .padding(20)
                .background(
                    GeometryReader { proxy in
                        Color.clear
                            .onAppear {
                                onHeightMeasured(proxy.size.height)
                            }
                            .onChange(of: proxy.size.height) { newValue in
                                onHeightMeasured(newValue)
                            }
                    }
                )
                .frame(minHeight: 120, alignment: .topLeading)
            }
        }
        .animation(.easeInOut, value: selectedIndex)
        .overlay(alignment: .bottom) {
            HStack(spacing: 6) {
                ForEach(0..<size, id: \.self) { index in
                    Capsule()
                        .fill(indicatorColor(for: index))
                        .frame(width: index == selectedIndex ? 22 : 8, height: 8)
                        .onTapGesture {
                            onSelect(index)
                        }
                }
            }
            .padding(.bottom, 16)
        }
        .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
        .shadow(color: Color.black.opacity(0.08), radius: 10, y: 4)
        .padding(.horizontal, 16)
    }

    private func indicatorColor(for index: Int) -> Color {
        if selectedIndex == 1 || selectedIndex == 2 {
            return index == selectedIndex ? ConCafeColors.primary : ConCafeColors.outline
        } else {
            return index == selectedIndex ? Color.white : Color.white.opacity(0.5)
        }
    }
}

struct RankingEntryCard: View {
    let item: Shared.RankingFeedEntry
    
    let isMaid: Bool
    
    let onTap: () -> Void

    var body: some View {
        HStack(spacing: 10) {
            RankingChangeIndicator(change: item.change)
            rankIndicator
            ZStack {
                LinearGradient(
                    colors: [Color(hex: item.startColorHex), Color(hex: item.endColorHex)],
                    startPoint: .top,
                    endPoint: .bottom
                )
                if let imageUrl = ImageUrlUtils.normalizedRemoteUrl(from: item.imageUrl) {
                    CachedAsyncImage(url: imageUrl, placeholder: Color.clear, displaySize: .thumbnail)
                } else {
                    Text(item.symbol)
                    .font(.title2)
                }
            }
            .frame(width: 64, height: 64)
            .clipShape(isMaid ? AnyShape(Circle()) : AnyShape(RoundedRectangle(cornerRadius: 18, style: .continuous)))
            VStack(alignment: .leading, spacing: 4) {
                Text(item.name)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(UITraitCollection.current.userInterfaceStyle == .dark ? .white : .primary)
                .lineLimit(1)
                Text(item.subtitle)
                .font(.caption)
                .foregroundStyle(UITraitCollection.current.userInterfaceStyle == .dark ? Color.white.opacity(0.78) : .secondary)
                .lineLimit(1)
                Text(String(format: String(localized: String.LocalizationValue("ranking_score_format"), table: "Localizable"), locale: Locale.current, item.score))
                .font(.subheadline.weight(.bold))
                .foregroundStyle(ConCafeColors.primary)
            }
            Spacer()
        }
        .padding(16)
        .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
        .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
        .shadow(color: Color.black.opacity(0.04), radius: 6, y: 2)
        .onTapGesture {
            onTap()
        }
    }

    @ViewBuilder
    private var rankIndicator: some View {
        Group {
            if item.rank <= 3 {
                VStack(spacing: 0) {
                    Image(systemName: "trophy.fill")
                        .font(.footnote.weight(.bold))
                        .foregroundStyle(rankColor(Int(item.rank)))
                    Text("\(item.rank)")
                        .font(.subheadline.weight(.bold))
                        .monospacedDigit()
                        .foregroundStyle(rankColor(Int(item.rank)))
                }
            } else {
                Text("\(item.rank)")
                    .font(.title2.weight(.bold))
                    .monospacedDigit()
                    .foregroundStyle(rankColor(Int(item.rank)))
            }
        }
        .frame(width: 36, alignment: .center)
    }

    private func rankColor(_ rank: Int) -> Color {
        switch rank {
        case 1: return ConCafeColors.gold
        case 2: return ConCafeColors.outlineStrong
        case 3: return ConCafeColors.warning
        default: return ConCafeColors.textMuted
        }
    }
}

struct RankingChangeIndicator: View {
    let change: String

    var body: some View {
        HStack(spacing: 2) {
            if change.hasPrefix("+") {
                Image(systemName: "arrow.up")
                .foregroundStyle(ConCafeColors.success)
            } else if change.hasPrefix("-") {
                Image(systemName: "arrow.down")
                .foregroundStyle(ConCafeColors.error)
            } else {
                Text("-")
                .foregroundStyle(.secondary)
            }
            Text(change)
            .font(.caption2)
            .foregroundStyle(.secondary)
        }
        .font(.caption2.weight(.semibold))
    }
}

private extension Shared.RankingPromoAd {
    var systemImageName: String {
        switch symbol {
        case "✨":
            return "sparkles"
        case "🎁":
            return "gift.fill"
        default:
            return "tag.fill"
        }
    }
}

private struct AnyShape: Shape {
    private let pathBuilder: (CGRect) -> Path

    func path(in rect: CGRect) -> Path {
        pathBuilder(rect)
    }

    init<S: Shape>(_ shape: S) {
        pathBuilder = { rect in
            shape.path(in: rect)
        }
    }
}

struct RankingView_Previews: PreviewProvider {
    static var previews: some View {
        RankingView(onNavigationAction: { _ in })
    }
}
