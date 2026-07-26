//
//  CafeManagementView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import SwiftUI
import UIKit
import Shared

struct CafeManagementView: View {
    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel = CafeManagementViewModel()

    var body: some View {
        CafeManagementContentView(
            uiState: viewModel.uiState,
            onAction: viewModel.onAction
        )
        .onReceive(viewModel.event) { event in
            switch event {
            case .navigateToCafeDashboard(let cafeId):
                onNavigationAction(.navigateToCafeDashboard(id: cafeId))
            case .navigateToCafe(let cafeId):
                onNavigationAction(.navigateToCafe(id: cafeId))
            case .navigateToCafeInfoRegistration:
                onNavigationAction(.navigateToCafeInfoEdit(id: nil, isRegistrationMode: true))
            }
        }
    }
}

private struct CafeManagementContentView: View {
    let uiState: CafeManagementUiState

    let onAction: (CafeManagementAction) -> Void
    

    var body: some View {
        Group {
            if !uiState.isLoading {
                GeometryReader { geometry in
                    let contentWidth = max(0, geometry.size.width - 40)

                    ScrollView {
                        VStack(spacing: 18) {
                            heroCard
                            if let infoMessage = uiState.infoMessage {
                                infoBanner(
                                    message: {
                                        switch infoMessage {
                                        case "cafemgmt_info_owner_claim_registered":
                                            return String(localized: String.LocalizationValue("cafemgmt_info_owner_claim_registered"), table: "Localizable")
                                        default:
                                            return infoMessage
                                        }
                                    }()
                                )
                            }
                            if uiState.hasOwnedCafes {
                                sectionHeader(
                                    title: String(localized: String.LocalizationValue("cafemgmt_section_my_cafe_title"), table: "Localizable"),
                                    subtitle: String(localized: String.LocalizationValue("cafemgmt_section_my_cafe_subtitle"), table: "Localizable")
                                )
                                ownedCafeGrid(contentWidth: contentWidth)
                                if uiState.hasHiddenOwnedCafes {
                                    expandOwnedCafeButton
                                }
                                if !uiState.pendingClaims.isEmpty {
                                    sectionHeader(
                                        title: String(localized: String.LocalizationValue("cafemgmt_section_claim_status_title"), table: "Localizable"),
                                        subtitle: String(localized: String.LocalizationValue("cafemgmt_section_claim_status_subtitle"), table: "Localizable")
                                    )
                                    VStack(spacing: 12) {
                                        ForEach(Array(uiState.pendingClaims.enumerated()), id: \.offset) { _, claim in
                                            pendingClaimCard(claim: claim)
                                        }
                                    }
                                }
                                searchCafeSection
                                addCafeCard
                            } else {
                                searchCafeSection
                                emptyStateCard
                            }
                        }
                        .padding(.horizontal, 20)
                        .padding(.top, 16)
                        .padding(.bottom, 32)
                    }
                }
            } else {
                VStack {
                    ProgressView()
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 48)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .center)
            }
        }
        .background(ConCafeColors.background)
    }

    private var heroCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(String(localized: String.LocalizationValue("cafemgmt_hero_title"), table: "Localizable"))
                .font(.title3.weight(.bold))
                .foregroundStyle(.white)
            Text(
                uiState.featuredCafe != nil
                    ? String(localized: String.LocalizationValue("cafemgmt_hero_desc_with_cafe"), table: "Localizable")
                    : String(localized: String.LocalizationValue("cafemgmt_hero_desc_empty"), table: "Localizable")
            )
            .font(.subheadline)
            .foregroundStyle(.white.opacity(0.9))
            Text(
                String(
                    format: String(localized: String.LocalizationValue("cafemgmt_hero_count"), table: "Localizable"),
                    locale: Locale.current,
                    uiState.ownedCafes.count
                )
            )
                .font(.caption.weight(.semibold))
                .foregroundStyle(.white)
                .padding(.horizontal, 12)
                .padding(.vertical, 7)
                .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }).opacity(0.18))
                .clipShape(Capsule())
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(22)
        .background(
            LinearGradient(
                colors: [ConCafeColors.textPrimary, ConCafeColors.primary, ConCafeColors.primary],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
    }

    private func infoBanner(message: String) -> some View {
        HStack(spacing: 10) {
            Text(message)
                .font(.caption)
                .foregroundStyle(ConCafeColors.goldDeep)
                .frame(maxWidth: .infinity, alignment: .leading)
            Button {
                onAction(.dismissInfoMessage)
            } label: {
                Image(systemName: "xmark")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(ConCafeColors.goldDeep)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(ConCafeColors.goldContainer)
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(ConCafeColors.gold, lineWidth: 1)
        )
    }

    private func sectionHeader(title: String, subtitle: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
                .font(.headline.weight(.bold))
                .foregroundStyle(.primary)
            Text(subtitle)
                .font(.caption)
                .foregroundStyle(ConCafeColors.textSecondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func ownedCafeGrid(contentWidth: CGFloat) -> some View {
        let columnCount = ownedCafeGridColumnCount(for: contentWidth)
        let cardWidth = ownedCafeGridCardWidth(contentWidth: contentWidth, columnCount: columnCount)

        return LazyVGrid(columns: ownedCafeGridColumns(count: columnCount), spacing: 12) {
            ForEach(uiState.visibleOwnedCafes, id: \.id) { cafe in
                ownedCafeCard(cafe: cafe, cardWidth: cardWidth)
            }
        }
    }

    private func ownedCafeGridColumns(count: Int) -> [GridItem] {
        Array(repeating: GridItem(.flexible(), spacing: 12), count: count)
    }

    private func ownedCafeGridColumnCount(for contentWidth: CGFloat) -> Int {
        contentWidth >= 700 ? 2 : 1
    }

    private func ownedCafeGridCardWidth(contentWidth: CGFloat, columnCount: Int) -> CGFloat {
        let spacing = CGFloat(columnCount - 1) * 12
        return max(0, (contentWidth - spacing) / CGFloat(columnCount))
    }

    private func ownedCafeCard(cafe: CafeManagementData.OwnedCafeSummary, cardWidth: CGFloat) -> some View {
        let dynamicHeight = min(max(cardWidth / 1.8, 220), 500)
        return ZStack(alignment: .trailing) {
            Button {
                onAction(.clickCafe(cafe.id))
            } label: {
                ZStack(alignment: .bottomLeading) {
                    GeometryReader { geometry in
                        let imageSize = geometry.size
                        if let imageUrl = resolvedRemoteImageUrl(cafe.thumbnailImage) {
                            CachedAsyncImage(
                                url: imageUrl,
                                placeholder: EmptyView()
                            )
                            .frame(width: imageSize.width, height: imageSize.height)
                            .clipped()
                        }
                    }
                    LinearGradient(
                        colors: cafe.isApproved
                        ? [ConCafeColors.textPrimary, ConCafeColors.primary, ConCafeColors.primary]
                        : [ConCafeColors.textPrimary, ConCafeColors.textSecondary, ConCafeColors.outlineStrong],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                    .opacity(resolvedRemoteImageUrl(cafe.thumbnailImage) == nil ? 1 : 0.34)
                    LinearGradient(
                        colors: [.clear, Color.black.opacity(0.14), Color.black.opacity(0.52)],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                    VStack(alignment: .leading, spacing: 6) {
                        Text(cafe.name)
                        .font(.title3.weight(.bold))
                        .foregroundStyle(.white)
                        .lineLimit(1)
                        Text(cafe.city)
                        .font(.subheadline)
                        .foregroundStyle(.white.opacity(0.88))
                        .lineLimit(1)
                    }
                    .padding(18)
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
                .frame(maxWidth: .infinity)
                .frame(height: dynamicHeight)
                .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
            }
            .buttonStyle(.plain)
            .contentShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
            Button {
                onAction(.clickCafeDetail(cafe.id))
            } label: {
                ZStack {
                    Circle()
                    .fill(Color.black.opacity(0.18))
                    Image(systemName: "chevron.right")
                    .font(.headline.weight(.semibold))
                    .foregroundStyle(.white)
                }
                .frame(width: 40, height: 40)
                .contentShape(Circle())
            }
            .buttonStyle(.plain)
            .padding(.trailing, 10)
            .zIndex(1)
        }
    }

    private func resolvedRemoteImageUrl(_ raw: String?) -> URL? {
        let trimmed = raw?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if trimmed.isEmpty {
            return nil
        }
        return URL(string: trimmed)
    }

    private var expandOwnedCafeButton: some View {
        Button {
            onAction(.toggleCafeListExpanded)
        } label: {
            HStack {
                Text(
                    uiState.isShowingAllCafes
                        ? String(localized: String.LocalizationValue("cafemgmt_fold_cafe_list"), table: "Localizable")
                        : String(
                            format: String(localized: String.LocalizationValue("cafemgmt_more_cafe_list"), table: "Localizable"),
                            locale: Locale.current,
                            (uiState.ownedCafes.count - uiState.visibleOwnedCafes.count)
                        )
                )
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(ConCafeColors.textSecondary)
                Spacer()
                Image(systemName: uiState.isShowingAllCafes ? "chevron.up" : "chevron.down")
                    .foregroundStyle(ConCafeColors.textSecondary)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .background(ConCafeColors.surfaceTint)
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .stroke(ConCafeColors.outline, lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
    }

    private var addCafeCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(String(localized: String.LocalizationValue("cafemgmt_add_cafe_title"), table: "Localizable"))
                .font(.title3.weight(.bold))
                .foregroundStyle(.primary)
            Text(String(localized: String.LocalizationValue("cafemgmt_add_cafe_desc"), table: "Localizable"))
                .font(.subheadline)
                .foregroundStyle(ConCafeColors.textSecondary)
            Button(String(localized: String.LocalizationValue("cafemgmt_register_new_cafe"), table: "Localizable")) {
                onAction(.clickCreateCafe)
            }
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(ConCafeColors.textSecondary)
            .padding(.horizontal, 16)
            .frame(height: 44)
            .background(ConCafeColors.surfaceTint)
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(20)
        .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
        .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 28, style: .continuous)
                .stroke(ConCafeColors.outline, lineWidth: 1)
        )
    }

    private var searchCafeSection: some View {
        let ownedCafeIds = Set(uiState.ownedCafes.map(\.id))
        let visibleSearchableCafes = uiState.filteredSearchableCafes.filter { !ownedCafeIds.contains($0.id) }
        return VStack(alignment: .leading, spacing: 12) {
            sectionHeader(
                title: String(localized: String.LocalizationValue("cafemgmt_search_existing_title"), table: "Localizable"),
                subtitle: String(localized: String.LocalizationValue("cafemgmt_search_existing_subtitle"), table: "Localizable")
            )
            HStack(spacing: 12) {
                Image(systemName: "magnifyingglass")
                    .foregroundStyle(ConCafeColors.textMuted)
                TextField(
                    String(localized: String.LocalizationValue("cafemgmt_search_placeholder"), table: "Localizable"),
                    text: Binding(
                        get: { uiState.cafeSearchQuery },
                        set: { onAction(.changeCafeSearchQuery($0)) }
                    )
                )
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 16)
            .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .stroke(ConCafeColors.outline, lineWidth: 1)
            )
            if !uiState.cafeSearchQuery.isEmpty {
                VStack(spacing: 0) {
                    if visibleSearchableCafes.isEmpty {
                        Text(String(localized: String.LocalizationValue("cafemgmt_search_no_result"), table: "Localizable"))
                            .font(.subheadline)
                            .foregroundStyle(ConCafeColors.textMuted)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 20)
                    } else {
                        ForEach(Array(visibleSearchableCafes.enumerated()), id: \.element.id) { index, cafe in
                            searchCafeItem(cafe: cafe)

                            if index < visibleSearchableCafes.count - 1 {
                                Divider()
                                    .overlay(ConCafeColors.surfaceTint)
                            }
                        }
                    }
                }
                .frame(maxWidth: .infinity)
                .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
                .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 20, style: .continuous)
                        .stroke(ConCafeColors.outline, lineWidth: 1)
                )
            }
        }
    }

    private func searchCafeItem(cafe: CafeManagementData.SearchableCafeSummary) -> some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                Text(cafe.name)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.primary)
                Text(cafe.location)
                    .font(.caption)
                    .foregroundStyle(ConCafeColors.textMuted)
            }
            Spacer()
            Button(String(localized: String.LocalizationValue("cafemgmt_register"), table: "Localizable")) {
                onAction(.clickClaimCafe(cafe.id))
            }
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(.white)
            .padding(.horizontal, 14)
            .frame(height: 38)
            .background(ConCafeColors.primary)
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
    }

    private var emptyStateCard: some View {
        VStack(alignment: .leading, spacing: 16) {
            ZStack {
                Circle()
                    .fill(ConCafeColors.surfaceTint)
                    .frame(width: 54, height: 54)
                Image(systemName: "building.2.crop.circle")
                    .font(.system(size: 24, weight: .semibold))
                    .foregroundStyle(ConCafeColors.primary)
            }
            Text(String(localized: String.LocalizationValue("cafemgmt_empty_title"), table: "Localizable"))
                .font(.title3.weight(.bold))
                .foregroundStyle(.primary)
            Text(String(localized: String.LocalizationValue("cafemgmt_empty_desc"), table: "Localizable"))
                .font(.subheadline)
                .foregroundStyle(ConCafeColors.textSecondary)
            Button(String(localized: String.LocalizationValue("cafemgmt_register_new_cafe"), table: "Localizable")) {
                onAction(.clickCreateCafe)
            }
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(ConCafeColors.textSecondary)
            .padding(.horizontal, 16)
            .frame(height: 44)
            .background(ConCafeColors.surfaceTint)
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))

            if !uiState.pendingClaims.isEmpty {
                Text(String(localized: String.LocalizationValue("cafemgmt_section_claim_status_title"), table: "Localizable"))
                    .font(.headline.weight(.bold))
                    .foregroundStyle(.primary)

                ForEach(Array(uiState.pendingClaims.enumerated()), id: \.offset) { _, claim in
                    pendingClaimCard(claim: claim)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(20)
        .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
        .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 28, style: .continuous)
                .stroke(ConCafeColors.outline, lineWidth: 1)
        )
    }

    private func pendingClaimCard(claim: CafeManagementData.PendingClaimSummary) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(claim.cafeName)
                    .font(.headline.weight(.bold))
                    .foregroundStyle(.primary)
                Spacer()
                ZStack {
                    Circle()
                        .fill(ConCafeColors.warningContainer)
                        .frame(width: 30, height: 30)
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundStyle(ConCafeColors.goldDeep)
                }
            }
            Text("\(claim.status) · \(claim.requestedAt)")
                .font(.caption)
                .foregroundStyle(ConCafeColors.goldDeep)
            Text(claim.message)
                .font(.caption)
                .foregroundStyle(ConCafeColors.goldDeep)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(ConCafeColors.warningContainer)
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(ConCafeColors.warningContainer, lineWidth: 1)
        )
    }
}

struct CafeManagementView_Previews: PreviewProvider {
    static var previews: some View {
        CafeManagementView(onNavigationAction: { _ in })
    }
}
