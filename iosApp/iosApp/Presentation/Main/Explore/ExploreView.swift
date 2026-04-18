//
//  ExploreView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/05.
//

import SwiftUI
import Shared
import UIKit

struct ExploreView: View {
    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel = ExploreViewModel()

    var body: some View {
        ExploreContentView(
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
    }
}

private struct ExploreContentView: View {
    @Environment(\.colorScheme) private var colorScheme

    @FocusState private var isSearchFocused: Bool
    
    let uiState: ExploreUiState
    
    let onAction: (ExploreAction) -> Void
    
    private var cafeNameById: [String: String] {
        Dictionary(uniqueKeysWithValues: uiState.cafes.map { ($0.id, $0.name) })
    }
    
    var body: some View {
        GeometryReader { geometry in
            ScrollView {
                LazyVStack(spacing: 12, pinnedViews: [.sectionHeaders]) {
                    searchSection
                    Section {
                        gridContent(contentWidth: geometry.size.width)
                            .padding(.horizontal, 12)
                    } header: {
                        tabHeader
                    }
                }
                .padding(.vertical, 12)
            }
            .background(ScrollViewKeyboardDismissConfigurator())
            .background(Color(hex: "FFF9FC"))
            .modifier(ExploreKeyboardDismissModifier())
        }
    }
    
    private var searchSection: some View {
        VStack(spacing: 10) {
            HStack(spacing: 8) {
                Image(systemName: "magnifyingglass")
                    .foregroundStyle(.secondary)
                TextField(String(localized: String.LocalizationValue("explore_search_placeholder"), table: "Localizable"), text: Binding(
                    get: { uiState.query },
                    set: { onAction(.queryChanged($0)) }
                ))
                .focused($isSearchFocused)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .background(Color(uiColor: .secondarySystemBackground))
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .stroke(
                        isSearchFocused ? Color(hex: "EF6797") : .clear,
                        lineWidth: isSearchFocused ? 1 : 0
                    )
            )
            HStack(spacing: 8) {
                Menu {
                    ForEach(ExploreUiState.RegionFilter.allCases, id: \.self) { region in
                        Button(region.label) {
                            onAction(.regionChanged(region))
                        }
                    }
                } label: {
                    CapsuleDropdownLabel(text: uiState.selectedRegion.label)
                }
                Menu {
                    ForEach(ExploreUiState.SortFilter.allCases, id: \.self) { sort in
                        Button(sort.label) {
                            onAction(.sortChanged(sort))
                        }
                    }
                } label: {
                    CapsuleDropdownLabel(text: uiState.selectedSort.label)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.horizontal, 12)
    }

    private var tabHeader: some View {
        ConCafeTabBar(
            labels: ExploreUiState.TabType.allCases.map { $0.rawValue },
            selectedIndex: ExploreUiState.TabType.allCases.firstIndex(of: uiState.selectedTab) ?? 0,
            backgroundColor: Color(hex: "FFF9FC"),
            onSelect: { index in
                onAction(.tabChanged(ExploreUiState.TabType.allCases[index]))
            }
        )
        .zIndex(1)
    }

    @ViewBuilder
    private func gridContent(contentWidth: CGFloat) -> some View {
        if uiState.isLoading {
            ProgressView()
                .frame(maxWidth: .infinity)
                .padding(.vertical, 32)
        } else if uiState.errorMessage != nil {
            Text(String(localized: String.LocalizationValue("explore_error_load_failed"), table: "Localizable"))
                .foregroundStyle(.red)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 32)
        } else {
            LazyVGrid(columns: exploreGridColumns(for: contentWidth), spacing: 12) {
                if uiState.selectedTab == .cafe {
                    ForEach(Array(uiState.cafes.enumerated()), id: \.element.id) { index, cafe in
                        cafeCard(cafe)
                            .onAppear {
                                guard index == uiState.cafes.indices.last,
                                      uiState.canLoadMoreCafes,
                                      !uiState.isLoadingMoreCafes else { return }
                                onAction(.loadMoreCafes)
                            }
                    }
                } else {
                    ForEach(Array(uiState.maids.enumerated()), id: \.element.id) { index, maid in
                        maidCard(maid)
                            .onAppear {
                                guard index == uiState.maids.indices.last,
                                      uiState.canLoadMoreMaids,
                                      !uiState.isLoadingMoreMaids else { return }
                                onAction(.loadMoreMaids)
                        }
                    }
                }
            }
            if (uiState.selectedTab == .cafe && !uiState.cafes.isEmpty) || (uiState.selectedTab == .maid && !uiState.maids.isEmpty) {
                EmptyView()
            } else {
                    ExploreEmptyPlaceholderCard(
                        title: uiState.selectedTab == .cafe ? String(localized: String.LocalizationValue("explore_empty_cafe_title"), table: "Localizable") : String(localized: String.LocalizationValue("explore_empty_cast_title"), table: "Localizable"),
                        description: String(localized: String.LocalizationValue("explore_empty_hint"), table: "Localizable")
                    )
                }
            pagingFooter
        }
    }

    private func exploreGridColumns(for contentWidth: CGFloat) -> [GridItem] {
        Array(
            repeating: GridItem(.flexible(), spacing: exploreGridItemSpacing),
            count: exploreGridColumnCount(for: contentWidth)
        )
    }

    private func exploreGridColumnCount(for contentWidth: CGFloat) -> Int {
        let availableWidth = contentWidth - exploreGridHorizontalPadding
        let minimumGridWidth = (exploreGridMinimumCellWidth * 2) + exploreGridItemSpacing
        let normalizedWidth = max(availableWidth, minimumGridWidth)
        let rawCount = Int((normalizedWidth + exploreGridItemSpacing) /
            (exploreGridMinimumCellWidth + exploreGridItemSpacing))
        return min(max(rawCount, exploreGridMinimumColumnCount), exploreGridMaximumColumnCount)
    }

    @ViewBuilder
    private var pagingFooter: some View {
        let isLoadingMore = uiState.selectedTab == .cafe ? uiState.isLoadingMoreCafes : uiState.isLoadingMoreMaids
        let canLoadMore = uiState.selectedTab == .cafe ? uiState.canLoadMoreCafes : uiState.canLoadMoreMaids

        if isLoadingMore {
            ProgressView()
                .frame(maxWidth: .infinity)
                .padding(.top, 12)
        } else if canLoadMore {
            Text(String(localized: String.LocalizationValue("explore_paging_hint"), table: "Localizable"))
                .font(.caption)
                .foregroundStyle(.secondary)
                .frame(maxWidth: .infinity)
                .padding(.top, 8)
        }
    }

    private func cafeCard(_ cafe: Cafe) -> some View {
        CafeSummaryCard(
            name: cafe.name,
            rating: String(format: "%.1f", cafe.ratingAvg),
            conceptType: localizedCafeConceptType(cafe.conceptType),
            location: cafe.region.city,
            thumbnailImage: cafe.thumbnailImage,
            showLocationIcon: false,
            trailingLabel: nil,
            onTap: {
                onAction(.cafeTapped(id: cafe.id))
            }
        )
    }

    private func localizedCafeConceptType(_ rawConceptType: String) -> String {
        let normalized = rawConceptType.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !normalized.isEmpty else {
            return ""
        }
        switch normalized.uppercased() {
        case "MAID":
            return String(localized: String.LocalizationValue("home_nearby_cafe_type_maid"), table: "Localizable")
        case "BUTLER":
            return String(localized: String.LocalizationValue("home_nearby_cafe_type_butler"), table: "Localizable")
        case "IDOL":
            return String(localized: String.LocalizationValue("home_nearby_cafe_type_idol"), table: "Localizable")
        case "DEVIL":
            return String(localized: String.LocalizationValue("home_nearby_cafe_type_devil"), table: "Localizable")
        default:
            return normalized
        }
    }

    private func maidCard(_ maid: Cast) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            GeometryReader { proxy in
                ZStack {
                    placeholderMaidImage
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
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                .clipped()
            }
            .frame(height: 120)
            VStack(alignment: .leading, spacing: 4) {
                Text(maid.name)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(colorScheme == .dark ? .white : .primary)
                    .lineLimit(1)
                Text(cafeNameById[maid.cafeId] ?? maid.cafeId)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
                Text(String(format: String(localized: String.LocalizationValue("explore_cast_followers"), table: "Localizable"), locale: Locale.current, maid.followerCount))
                    .font(.caption)
                    .foregroundStyle(Color(hex: "EF6797"))
            }
            .padding(.horizontal, 4)
        }
        .onTapGesture {
            onAction(.maidTapped(id: maid.id))
        }
    }

    private var placeholderMaidImage: some View {
        LinearGradient(
            colors: [Color(hex: "FFDFEA"), Color(hex: "FFBED5")],
            startPoint: .top,
            endPoint: .bottom
        )
        .overlay(Image(systemName: "person.fill").foregroundStyle(Color.white.opacity(0.85)))
    }
}

private struct ExploreEmptyPlaceholderCard: View {
    let title: String

    let description: String

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(.primary)
            Text(description)
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 14)
        .padding(.vertical, 16)
        .background(Color(uiColor: .secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
    }
}

struct ExploreView_Previews: PreviewProvider {
    static var previews: some View {
        ExploreContentView(uiState: .empty, onAction: { _ in })
    }
}

private let exploreGridMinimumColumnCount = 2
private let exploreGridMaximumColumnCount = 6
private let exploreGridHorizontalPadding: CGFloat = 24
private let exploreGridItemSpacing: CGFloat = 12
private let exploreGridMinimumCellWidth: CGFloat = 180
