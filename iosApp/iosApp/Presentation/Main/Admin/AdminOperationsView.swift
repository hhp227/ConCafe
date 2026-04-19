//
//  AdminOperationsView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import SwiftUI
import Shared

struct AdminOperationsView: View {
    @Environment(\.colorScheme) private var colorScheme

    @StateObject private var viewModel = AdminOperationsViewModel()

    let onNavigationAction: (NavigationAction) -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                metricsGrid
                pendingSection
                inquirySection
                quickMenuSection
                bannerRegisterSection
                if let message = viewModel.uiState.infoMessage {
                    infoBanner(message)
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 12)
            .padding(.bottom, 24)
        }
        .background(
            Group {
                if colorScheme == .dark {
                    Color(hex: "FFF9FC")
                } else {
                    LinearGradient(
                        colors: [Color(hex: "F8F5F6"), Color(hex: "FFFCFD")],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                }
            }
        )
        .onReceive(viewModel.event) { event in
            switch event {
            case .navigateToBanner:
                onNavigationAction(.navigateToBanner())
            case .navigateToBannerEdit:
                onNavigationAction(.navigateToBannerEdit())
            }
        }
    }

    init(
        onNavigationAction: @escaping (NavigationAction) -> Void = { _ in }
    ) {
        self.onNavigationAction = onNavigationAction
    }

    private var metricsGrid: some View {
        LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 12), count: 2), spacing: 12) {
            ForEach(viewModel.uiState.metrics) { metric in
                VStack(alignment: .leading, spacing: 4) {
                    HStack(spacing: 8) {
                        Image(systemName: metric.icon.systemName)
                            .font(.caption.weight(.bold))
                            .foregroundStyle(Color(hex: "EF6797"))
                        Text(metric.title)
                            .font(.caption.weight(.medium))
                            .foregroundStyle(Color(hex: "7A707A"))
                    }
                    Text(metric.value)
                        .font(.title3.weight(.bold))
                    HStack(spacing: 2) {
                        Image(systemName: metric.trend.systemName)
                            .font(.caption2.weight(.bold))
                        Text(metric.delta)
                            .font(.caption2.weight(.bold))
                    }
                    .foregroundStyle(metric.trend.color)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(16)
                .background(Color(hex: "FFD1DC").opacity(0.16))
                .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            }
        }
    }

    private var pendingSection: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                Text(String(localized: String.LocalizationValue("admin_pending_section_title"), table: "Localizable"))
                    .font(.headline.weight(.bold))
                Spacer()
                Button(String(localized: String.LocalizationValue("dashboard_action_view_all"), table: "Localizable")) {
                    viewModel.onAction(.clickSeeAllPending)
                }
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Color(hex: "EF6797"))
                .buttonStyle(.plain)
            }
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(viewModel.uiState.pendingFilters) { chip in
                        Text("\(chip.label) (\(chip.count))")
                            .font(.subheadline.weight(chip.isSelected ? .bold : .medium))
                            .foregroundStyle(chip.isSelected ? Color(hex: "2B2330") : Color(hex: "6F6670"))
                            .padding(.horizontal, 16)
                            .padding(.vertical, 10)
                            .background(chip.isSelected ? Color(hex: "FFD1DC") : Color(hex: "FFD1DC").opacity(0.14))
                            .clipShape(Capsule())
                            .onTapGesture {
                                viewModel.onAction(.selectPendingFilter(chip.filter))
                            }
                    }
                }
            }
            VStack(spacing: 12) {
                if viewModel.uiState.selectedPendingFilter == .cafeRegistration {
                    ForEach(viewModel.uiState.pendingCafeRegistrationClaims, id: \.claimId) { claim in
                        pendingRegistrationCard(claim)
                    }
                } else {
                    ForEach(viewModel.uiState.pendingCafeOwnerClaims, id: \.claimId) { claim in
                        pendingOwnerClaimCard(claim)
                    }
                }
            }
        }
    }

    private func pendingRegistrationCard(_ claim: PendingCafeRegistrationClaimPreview) -> some View {
        pendingCard(
            claimId: claim.claimId,
            title: claim.cafeName,
            subtitle: claim.location,
            requestedAt: claim.requestedAt,
            imageUrl: claim.imageUrl
        )
    }

    private func pendingOwnerClaimCard(_ claim: PendingCafeOwnerClaimPreview) -> some View {
        pendingCard(
            claimId: claim.claimId,
            title: String(
                format: String(localized: String.LocalizationValue("admin_pending_owner_claim_title"), table: "Localizable"),
                locale: Locale.current,
                claim.requesterNickname
            ),
            subtitle: claim.location,
            requestedAt: claim.requestedAt,
            imageUrl: claim.imageUrl
        )
    }

    private func pendingCard(
        claimId: String,
        title: String,
        subtitle: String,
        requestedAt: String,
        imageUrl: String?
    ) -> some View {
        HStack(alignment: .top, spacing: 12) {
            AsyncImage(url: resolvedRemoteImageUrl(imageUrl)) { image in
                image
                    .resizable()
                    .scaledToFill()
            } placeholder: {
                pendingCardImagePlaceholder
            }
            .frame(width: 64, height: 64)
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            VStack(alignment: .leading, spacing: 6) {
                HStack(alignment: .top) {
                    Text(title)
                        .font(.subheadline.weight(.bold))
                        .lineLimit(1)
                    Spacer()
                    Text(requestedAt)
                        .font(.caption2)
                        .foregroundStyle(Color(hex: "7A707A"))
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(Color(hex: "F5F2F4"))
                        .clipShape(Capsule())
                }
                Text(subtitle)
                    .font(.caption)
                    .foregroundStyle(Color(hex: "7A707A"))
                HStack(spacing: 8) {
                    Button {
                        viewModel.onAction(.approvePending(claimId))
                    } label: {
                        Text(String(localized: String.LocalizationValue("dashboard_action_approve"), table: "Localizable"))
                            .font(.caption.weight(.bold))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 10)
                            .background(Color(hex: "FFD1DC"))
                            .foregroundStyle(Color(hex: "2B2330"))
                            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                    }
                    Button {
                        viewModel.onAction(.rejectPending(claimId))
                    } label: {
                        Text(String(localized: String.LocalizationValue("dashboard_action_reject"), table: "Localizable"))
                            .font(.caption.weight(.bold))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 10)
                            .background(Color(hex: "F5F2F4"))
                            .foregroundStyle(Color(hex: "6F6670"))
                            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                    }
                }
            }
        }
        .padding(16)
        .background(
            colorScheme == .dark
                ? Color(uiColor: .secondarySystemBackground)
                : Color.white
        )
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
    }

    private var pendingCardImagePlaceholder: some View {
        LinearGradient(
            colors: [Color(hex: "FFE7EF"), Color(hex: "F4D8E2")],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }

    private func resolvedRemoteImageUrl(_ raw: String?) -> URL? {
        let trimmed = raw?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if trimmed.isEmpty {
            return nil
        }
        return URL(string: trimmed)
    }

    private var quickMenuSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(String(localized: String.LocalizationValue("admin_quick_menu_title"), table: "Localizable"))
                .font(.headline.weight(.bold))
            ForEach(viewModel.uiState.quickMenus) { menu in
                Button {
                    viewModel.onAction(.clickQuickMenu(menu.id))
                } label: {
                    HStack(spacing: 12) {
                        RoundedRectangle(cornerRadius: 12, style: .continuous)
                            .fill(menu.accent.backgroundColor)
                            .frame(width: 40, height: 40)
                            .overlay {
                                Image(systemName: menu.icon.systemName)
                                    .foregroundStyle(menu.accent.contentColor)
                            }
                        VStack(alignment: .leading, spacing: 2) {
                            Text(menu.title)
                                .font(.subheadline.weight(.bold))
                                .foregroundStyle(Color.primary)
                            Text(menu.description)
                                .font(.caption2)
                                .foregroundStyle(Color(hex: "7A707A"))
                        }
                        Spacer()
                        Image(systemName: "chevron.right")
                            .foregroundStyle(Color(hex: "B5AEB5"))
                    }
                    .padding(16)
                    .background(
                        colorScheme == .dark
                            ? Color(uiColor: .secondarySystemBackground)
                            : Color.white
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
                }
                .buttonStyle(.plain)
            }
        }
    }

    private var inquirySection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(String(localized: String.LocalizationValue("admin_inquiry_title"), table: "Localizable"))
                .font(.headline.weight(.bold))
            if viewModel.uiState.inquiries.isEmpty {
                Text(String(localized: String.LocalizationValue("admin_inquiry_empty"), table: "Localizable"))
                    .font(.subheadline)
                    .foregroundStyle(Color(hex: "7A707A"))
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(16)
                    .background(
                        colorScheme == .dark
                            ? Color(uiColor: .secondarySystemBackground)
                            : Color.white
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            } else {
                ForEach(viewModel.uiState.inquiries, id: \.id) { inquiry in
                    inquiryCard(inquiry)
                }
                if viewModel.uiState.canLoadMoreInquiries || viewModel.uiState.isLoadingMoreInquiries {
                    Button {
                        viewModel.onAction(.loadMoreInquiries)
                    } label: {
                        HStack(spacing: 8) {
                            if viewModel.uiState.isLoadingMoreInquiries {
                                ProgressView()
                                    .progressViewStyle(.circular)
                            } else {
                                Text(String(localized: String.LocalizationValue("admin_inquiry_load_more"), table: "Localizable"))
                                    .font(.subheadline.weight(.bold))
                            }
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                    }
                    .buttonStyle(.plain)
                    .background(Color(hex: "F5F2F4"))
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                    .disabled(viewModel.uiState.isLoadingMoreInquiries)
                }
            }
        }
    }

    private func inquiryCard(_ inquiry: Inquiry) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(inquiry.inquiryType)
                    .font(.caption.weight(.bold))
                    .foregroundStyle(Color(hex: "EF6797"))
                Spacer()
                Text(inquiry.createdAtLabel)
                    .font(.caption2)
                    .foregroundStyle(Color(hex: "7A707A"))
            }
            Text(inquiry.title)
                .font(.subheadline.weight(.bold))
            Text(inquiry.content)
                .font(.caption)
                .foregroundStyle(Color(hex: "6F6670"))
            Text(
                String(
                    format: String(localized: String.LocalizationValue("admin_writer"), table: "Localizable"),
                    locale: Locale.current,
                    inquiry.userNickname
                )
            )
                .font(.caption2)
                .foregroundStyle(Color(hex: "8B7F8A"))
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(
            colorScheme == .dark
                ? Color(uiColor: .secondarySystemBackground)
                : Color.white
        )
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
    }

    private func infoBanner(_ message: String) -> some View {
        HStack(spacing: 12) {
            Text(message)
                .font(.subheadline)
                .foregroundStyle(Color(hex: "6B5320"))
                .frame(maxWidth: .infinity, alignment: .leading)
            Button(String(localized: String.LocalizationValue("common_close"), table: "Localizable")) {
                viewModel.onAction(.dismissInfoMessage)
            }
            .font(.caption.weight(.bold))
            .foregroundStyle(Color(hex: "6B5320"))
            .buttonStyle(.plain)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .background(Color(hex: "FFF2D8"))
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
    }

    private var bannerRegisterSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(String(localized: String.LocalizationValue("admin_banner_card_title"), table: "Localizable"))
                .font(.headline.weight(.bold))
            Text(String(localized: String.LocalizationValue("admin_banner_card_description"), table: "Localizable"))
                .font(.subheadline)
                .foregroundStyle(Color(hex: "7A707A"))
            Button {
                viewModel.onAction(.clickBannerRegister)
            } label: {
                HStack(spacing: 8) {
                    Image(systemName: "plus")
                    Text(String(localized: String.LocalizationValue("dashboard_action_create_banner"), table: "Localizable"))
                        .fontWeight(.bold)
                }
                .foregroundStyle(Color(hex: "2B2330"))
                .padding(.horizontal, 18)
                .padding(.vertical, 12)
                .background(Color(hex: "FFD1DC"))
                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            }
            .buttonStyle(.plain)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(18)
        .background(
            colorScheme == .dark
                ? Color(uiColor: .secondarySystemBackground)
                : Color.white
        )
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
    }
}

private extension AdminMetricIcon {
    var systemName: String {
        switch self {
        case .users: return "person.2.fill"
        case .cafe: return "cup.and.saucer.fill"
        case .pending: return "clock.badge.exclamationmark"
        case .report: return "exclamationmark.bubble.fill"
        }
    }
}

private extension MetricTrend {
    var systemName: String {
        switch self {
        case .up: return "arrow.up.right"
        case .down: return "arrow.down.right"
        case .new: return "plus"
        }
    }

    var color: Color {
        switch self {
        case .up, .down: return Color(hex: "2E9E5B")
        case .new: return .red
        }
    }
}

private extension QuickMenuIcon {
    var systemName: String {
        switch self {
        case .banner: return "rectangle.3.group.fill"
        case .moderation: return "hammer.fill"
        case .analytics: return "chart.bar.fill"
        }
    }
}

private extension QuickMenuAccent {
    var backgroundColor: Color {
        switch self {
        case .primary: return Color(hex: "FFD1DC").opacity(0.32)
        case .rose: return Color(hex: "FFE5EA")
        case .blue: return Color(hex: "E6F0FF")
        }
    }

    var contentColor: Color {
        switch self {
        case .primary: return Color(hex: "5E535C")
        case .rose: return Color(hex: "E05A78")
        case .blue: return Color(hex: "4F7DFF")
        }
    }
}

struct AdminOperationsView_Previews: PreviewProvider {
    static var previews: some View {
        AdminOperationsView()
    }
}
