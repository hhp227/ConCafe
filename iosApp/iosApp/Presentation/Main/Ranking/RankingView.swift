//
//  RankingView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/05.
//

import SwiftUI
import Combine
import Shared
import UIKit
#if canImport(GoogleMobileAds)
import GoogleMobileAds
#endif

struct RankingView: View {
    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel = RankingViewModel()

    @State private var adTimer = Timer.publish(every: 3.5, on: .main, in: .common).autoconnect()

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
        .onReceive(adTimer) { _ in
            guard viewModel.uiState.ads.count > 1 else { return }
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
        .background(Color(hex: "FFFBFD"))
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
            backgroundColor: Color(hex: "FFFBFD"),
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
            onSelect: { onAction(.selectAd($0)) }
        )
    }

    private var rankingList: some View {
        VStack(spacing: 12) {
            if uiState.isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 28)
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
                .foregroundStyle(Color(hex: "5C525D"))
            Text(String(localized: String.LocalizationValue("ranking_empty_desc"), table: "Localizable"))
                .font(.caption)
                .foregroundStyle(Color(hex: "8A7F8B"))
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 14)
        .padding(.vertical, 16)
        .background(.white)
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
                .foregroundStyle(Color(hex: "EF6797"))
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
    
    let onSelect: (Int) -> Void

    var body: some View {
        ZStack {
            if selectedIndex == 0 {
                RankingNativeAdCard()
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
                        .foregroundStyle(Color.black.opacity(0.85))
                        .padding(.horizontal, 16)
                        .padding(.vertical, 10)
                        .background(.white)
                        .clipShape(Capsule())
                    }
                }
                .padding(20)
            }
        }
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
        if selectedIndex == 0 {
            return index == selectedIndex ? Color(hex: "EF6797") : Color(hex: "E3D9E0")
        } else {
            return index == selectedIndex ? Color.white : Color.white.opacity(0.5)
        }
    }
}

#if canImport(GoogleMobileAds)
private struct RankingNativeAdCard: View {
    @StateObject private var loader = RankingNativeAdLoader()

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            if let nativeAd = loader.nativeAd {
                RankingNativeAdRepresentable(nativeAd: nativeAd)
                    .frame(maxWidth: .infinity)
                    .frame(height: 190)
            } else {
                ProgressView()
                    .frame(maxWidth: .infinity)
                    .frame(height: 190)
            }
        }
        .padding(20)
        .background(Color.white)
    }
}

private final class RankingNativeAdLoader: NSObject, ObservableObject, GADNativeAdLoaderDelegate {
    @Published var nativeAd: GADNativeAd?

    private var adLoader: GADAdLoader?

    override init() {
        super.init()

        let adUnitId: String
        #if DEBUG
        adUnitId = "ca-app-pub-3940256099942544/3986624511"
        #else
        adUnitId = "ca-app-pub-6216021268300256/5283160617"
        #endif

        adLoader = GADAdLoader(
            adUnitID: adUnitId,
            rootViewController: UIApplication.shared.connectedScenes
                .compactMap { $0 as? UIWindowScene }
                .flatMap { $0.windows }
                .first { $0.isKeyWindow }?.rootViewController,
            adTypes: [.native],
            options: nil
        )
        adLoader?.delegate = self
        adLoader?.load(GADRequest())
    }

    func adLoader(_ adLoader: GADAdLoader, didReceive nativeAd: GADNativeAd) {
        self.nativeAd = nativeAd
    }

    func adLoader(_ adLoader: GADAdLoader, didFailToReceiveAdWithError error: Error) {
        print("Ranking native ad load failed: \(error.localizedDescription)")
    }
}

private struct RankingNativeAdRepresentable: UIViewRepresentable {
    let nativeAd: GADNativeAd

    func makeUIView(context: Context) -> GADNativeAdView {
        let nativeAdView = GADNativeAdView()
        let container = UIStackView()
        let badgeLabel = UILabel()
        let headlineLabel = UILabel()
        let bodyLabel = UILabel()
        let callToActionButton = UIButton(type: .system)

        container.axis = .vertical
        container.spacing = 10
        container.translatesAutoresizingMaskIntoConstraints = false
        nativeAdView.addSubview(container)

        NSLayoutConstraint.activate([
            container.leadingAnchor.constraint(equalTo: nativeAdView.leadingAnchor, constant: 16),
            container.trailingAnchor.constraint(equalTo: nativeAdView.trailingAnchor, constant: -16),
            container.topAnchor.constraint(equalTo: nativeAdView.topAnchor, constant: 16),
            container.bottomAnchor.constraint(equalTo: nativeAdView.bottomAnchor, constant: -16)
        ])

        badgeLabel.text = String(localized: String.LocalizationValue("ranking_native_ad_badge"), table: "Localizable")
        badgeLabel.font = .systemFont(ofSize: 12, weight: .bold)
        badgeLabel.textColor = UIColor(Color(hex: "B74D73"))
        badgeLabel.backgroundColor = UIColor(Color(hex: "FFE9F1"))
        badgeLabel.textAlignment = .center
        badgeLabel.layer.cornerRadius = 12
        badgeLabel.clipsToBounds = true
        badgeLabel.widthAnchor.constraint(greaterThanOrEqualToConstant: 46).isActive = true

        headlineLabel.font = .systemFont(ofSize: 19, weight: .bold)
        headlineLabel.textColor = UIColor(Color(hex: "2B2330"))
        headlineLabel.numberOfLines = 2

        bodyLabel.font = .systemFont(ofSize: 13, weight: .regular)
        bodyLabel.textColor = UIColor(Color(hex: "6F6670"))
        bodyLabel.numberOfLines = 3

        callToActionButton.titleLabel?.font = .systemFont(ofSize: 14, weight: .semibold)
        callToActionButton.setTitleColor(UIColor(Color(hex: "2B2330")), for: .normal)
        callToActionButton.backgroundColor = UIColor(Color(hex: "FFD1DC"))
        callToActionButton.contentEdgeInsets = UIEdgeInsets(top: 8, left: 14, bottom: 8, right: 14)
        callToActionButton.layer.cornerRadius = 16

        container.addArrangedSubview(badgeLabel)
        container.addArrangedSubview(headlineLabel)
        container.addArrangedSubview(bodyLabel)
        container.addArrangedSubview(callToActionButton)

        nativeAdView.headlineView = headlineLabel
        nativeAdView.bodyView = bodyLabel
        nativeAdView.callToActionView = callToActionButton

        return nativeAdView
    }

    func updateUIView(_ nativeAdView: GADNativeAdView, context: Context) {
        (nativeAdView.headlineView as? UILabel)?.text = nativeAd.headline
        (nativeAdView.bodyView as? UILabel)?.text = nativeAd.body

        if let callToAction = nativeAd.callToAction, !callToAction.isEmpty {
            (nativeAdView.callToActionView as? UIButton)?.setTitle(callToAction, for: .normal)
            nativeAdView.callToActionView?.isHidden = false
        } else {
            (nativeAdView.callToActionView as? UIButton)?.setTitle(nil, for: .normal)
            nativeAdView.callToActionView?.isHidden = true
        }

        nativeAdView.nativeAd = nativeAd
    }
}
#else
private struct RankingNativeAdCard: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(String(localized: String.LocalizationValue("ranking_native_ad_badge"), table: "Localizable"))
                .font(.caption2.weight(.bold))
                .foregroundStyle(Color(hex: "B74D73"))
                .padding(.horizontal, 10)
                .padding(.vertical, 6)
                .background(Color(hex: "FFE9F1"))
                .clipShape(Capsule())
            Text(String(localized: String.LocalizationValue("ranking_native_ad_title"), table: "Localizable"))
                .font(.title3.weight(.bold))
                .foregroundStyle(Color(hex: "2B2330"))
            Text(String(localized: String.LocalizationValue("ranking_native_ad_desc"), table: "Localizable"))
                .font(.caption)
                .foregroundStyle(Color(hex: "6F6670"))
        }
        .padding(20)
        .background(Color.white)
    }
}
#endif

struct RankingEntryCard: View {
    let item: Shared.RankingFeedEntry
    
    let isMaid: Bool
    
    let onTap: () -> Void

    var body: some View {
        HStack(spacing: 14) {
            rankIndicator
            ZStack {
                LinearGradient(
                    colors: [Color(hex: item.startColorHex), Color(hex: item.endColorHex)],
                    startPoint: .top,
                    endPoint: .bottom
                )
                Text(item.symbol)
                .font(.title2)
            }
            .frame(width: 64, height: 64)
            .clipShape(isMaid ? AnyShape(Circle()) : AnyShape(RoundedRectangle(cornerRadius: 18, style: .continuous)))
            VStack(alignment: .leading, spacing: 4) {
                Text(item.name)
                .font(.subheadline.weight(.semibold))
                .lineLimit(1)
                Text(item.subtitle)
                .font(.caption)
                .foregroundStyle(.secondary)
                .lineLimit(1)
                HStack(spacing: 8) {
                    Text("\(item.score) pt")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Color(hex: "EF6797"))
                    RankingChangeIndicator(change: item.change)
                }
            }
            Spacer()
        }
        .padding(16)
        .background(.white)
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
                Image(systemName: "trophy.fill")
                    .font(.title3.weight(.bold))
                    .foregroundStyle(rankColor(Int(item.rank)))
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
        case 1: return Color(hex: "E2B11E")
        case 2: return Color(hex: "A2A7B1")
        case 3: return Color(hex: "B8753B")
        default: return Color(hex: "8A8A8A")
        }
    }
}

struct RankingChangeIndicator: View {
    let change: String

    var body: some View {
        HStack(spacing: 2) {
            if change.hasPrefix("+") {
                Image(systemName: "arrow.up")
                .foregroundStyle(Color(hex: "34A853"))
            } else if change.hasPrefix("-") {
                Image(systemName: "arrow.down")
                .foregroundStyle(Color(hex: "E24B62"))
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

    init<S: Shape>(_ shape: S) {
        pathBuilder = { rect in
            shape.path(in: rect)
        }
    }

    func path(in rect: CGRect) -> Path {
        pathBuilder(rect)
    }
}

struct RankingView_Previews: PreviewProvider {
    static var previews: some View {
        RankingView(onNavigationAction: { _ in })
    }
}
