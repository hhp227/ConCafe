//
//  RankingView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/05.
//

import SwiftUI
import Combine
import Shared

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
            }
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
            items: RankingUiState.TabType.allCases.map { ConCafeTabItem(id: $0.rawValue, title: $0.rawValue) },
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
            ForEach(uiState.rankingEntries, id: \.id) { item in
                rankingCard(item)
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

struct RankingHeaderSection: View {
    let uiState: RankingUiState
    
    let onPeriodSelected: (RankingPeriod) -> Void
    
    let onRegionSelected: (RankingUiState.RegionFilter) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 8) {
                Image(systemName: "trophy.fill")
                .foregroundStyle(Color(hex: "EF6797"))
                Text("랭킹")
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
            return "주간"
        case .monthly:
            return "월간"
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
                        Text(ad.detailText)
                        .font(.caption)
                        .foregroundStyle(.white.opacity(0.92))
                    }
                    Spacer(minLength: 8)
                    Button("자세히") { }
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Color.black.opacity(0.85))
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(.white)
                    .clipShape(Capsule())
                }
                HStack(spacing: 6) {
                    ForEach(0..<size, id: \.self) { index in
                        Capsule()
                        .fill(index == selectedIndex ? Color.white : Color.white.opacity(0.5))
                        .frame(width: index == selectedIndex ? 22 : 8, height: 8)
                        .onTapGesture {
                            onSelect(index)
                        }
                    }
                }
            }
            .padding(20)
        }
        .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
        .shadow(color: Color.black.opacity(0.08), radius: 10, y: 4)
        .padding(.horizontal, 16)
    }
}

struct RankingEntryCard: View {
    let item: Shared.RankingFeedEntry
    
    let isMaid: Bool
    
    let onTap: () -> Void

    var body: some View {
        HStack(spacing: 14) {
            Text(item.rank <= 3 ? "🏆" : "\(item.rank)")
            .font(.title2.weight(.bold))
            .foregroundStyle(rankColor(item.rank))
            .frame(width: 32)
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
