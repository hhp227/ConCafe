//
//  MyInfoView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/05.
//

import SwiftUI
import UIKit
import Shared

struct MyInfoView: View {
    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel = MyInfoViewModel()

    var body: some View {
        Group {
            if viewModel.uiState.isLoading {
                ShimmerListSkeleton(itemCount: 6, avatarSize: 56)
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            } else if viewModel.uiState.isLoggedIn {
                ProfileMyInfoView(
                    uiState: viewModel.uiState,
                    onAction: viewModel.onAction
                )
            } else {
                GuestMyInfoView(
                    uiState: viewModel.uiState,
                    onAction: viewModel.onAction
                )
            }
        }
        .background(ConCafeColors.background)
        .onReceive(viewModel.event) { event in
            switch event {
            case .navigateToCafe(let id):
                onNavigationAction(.navigateToCafe(id: id))
            case .navigateToCast(let id):
                onNavigationAction(.navigateToCast(id: id))
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

@MainActor
private struct GuestMyInfoView: View {
    let uiState: MyInfoUiState

    let onAction: @MainActor (MyInfoAction) -> Void

    let features = [
        ("mappin.and.ellipse", String(localized: String.LocalizationValue("myinfo_guest_feature_checkin_title"), table: "Localizable"), String(localized: String.LocalizationValue("myinfo_guest_feature_checkin_desc"), table: "Localizable"), "EF6797", "F57AA8"),
        ("heart.fill", String(localized: String.LocalizationValue("myinfo_guest_feature_bookmark_title"), table: "Localizable"), String(localized: String.LocalizationValue("myinfo_guest_feature_bookmark_desc"), table: "Localizable"), "9C6ADE", "B388EB"),
        ("star.fill", String(localized: String.LocalizationValue("myinfo_guest_feature_badge_title"), table: "Localizable"), String(localized: String.LocalizationValue("myinfo_guest_feature_badge_desc"), table: "Localizable"), "F0B429", "F5C857"),
        ("gift.fill", String(localized: String.LocalizationValue("myinfo_guest_feature_membership_title"), table: "Localizable"), String(localized: String.LocalizationValue("myinfo_guest_feature_membership_desc"), table: "Localizable"), "4C8BF5", "71A7FF")
    ]

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                VStack(spacing: 8) {
                    Text("💗")
                        .font(.system(size: 42))
                    Text(String(localized: String.LocalizationValue("myinfo_guest_welcome_title"), table: "Localizable"))
                        .font(.headline)
                        .bold()
                        .multilineTextAlignment(.center)
                        .foregroundStyle(.white)
                    Text(String(localized: String.LocalizationValue("myinfo_guest_welcome_subtitle"), table: "Localizable"))
                        .font(.caption)
                        .foregroundStyle(.white.opacity(0.9))
                    Button {
                        onAction(.signInTapped)
                    } label: {
                        HStack(spacing: 6) {
                            Image(systemName: "rectangle.portrait.and.arrow.right")
                                .font(.system(size: 14, weight: .bold))
                            Text(String(localized: String.LocalizationValue("myinfo_guest_signin_cta"), table: "Localizable"))
                                .fontWeight(.bold)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 8)
                    }
                    .background(.white)
                    .foregroundStyle(ConCafeColors.primary)
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                }
                .padding(20)
                .frame(maxWidth: .infinity)
                .background(
                    LinearGradient(colors: [ConCafeColors.primary, ConCafeColors.secondaryContainer], startPoint: .topLeading, endPoint: .bottomTrailing)
                )
                .clipShape(RoundedRectangle(cornerRadius: 26, style: .continuous))
                VStack(alignment: .leading, spacing: 8) {
                    MyInfoSectionTitle(title: String(localized: String.LocalizationValue("myinfo_guest_features_title"), table: "Localizable"))
                    LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 8) {
                        ForEach(Array(features.enumerated()), id: \.offset) { _, item in
                            VStack(alignment: .leading, spacing: 6) {
                                RoundedRectangle(cornerRadius: 12, style: .continuous)
                                    .fill(
                                        LinearGradient(
                                            colors: [Color(hex: item.3), Color(hex: item.4)],
                                            startPoint: .topLeading,
                                            endPoint: .bottomTrailing
                                        )
                                    )
                                    .frame(width: 44, height: 44)
                                    .overlay(
                                        Image(systemName: item.0)
                                            .font(.system(size: 20, weight: .semibold))
                                            .foregroundStyle(.white)
                                    )
                                Text(item.1).font(.subheadline).bold()
                                Text(item.2)
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                                    .lineLimit(3)
                                    .multilineTextAlignment(.leading)
                            }
                            .padding(12)
                            .frame(maxWidth: .infinity, minHeight: 140, maxHeight: 140, alignment: .topLeading)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
                            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                        }
                    }
                }
                VStack(alignment: .leading, spacing: 8) {
                    HStack {
                        MyInfoSectionTitle(title: String(localized: String.LocalizationValue("myinfo_guest_popular_cafes_title"), table: "Localizable"))
                        Spacer()
                        Button {
                        } label: {
                            HStack(spacing: 2) {
                                Text(String(localized: String.LocalizationValue("home_show_more"), table: "Localizable"))
                                    .font(.caption)
                                Image(systemName: "chevron.right")
                                    .font(.system(size: 11, weight: .semibold))
                            }
                            .foregroundStyle(ConCafeColors.primary)
                        }
                        .buttonStyle(.plain)
                    }
                    if !uiState.popularCafes.isEmpty {
                        ForEach(uiState.popularCafes, id: \.id) { cafe in
                            let ratingText = RatingUtils.formatOneDecimal(cafe.ratingAvg)
                            let conceptType = localizedCafeConceptType(cafe.conceptType)
                            let imageCornerRadius: CGFloat = 12

                            HStack(spacing: 10) {
                                GeometryReader { geometry in
                                    let imageSize = geometry.size

                                    ZStack {
                                        RoundedRectangle(cornerRadius: imageCornerRadius, style: .continuous)
                                            .fill(LinearGradient(colors: [ConCafeColors.warningContainer, ConCafeColors.warningContainer], startPoint: .top, endPoint: .bottom))
                                        if let imageUrl = resolvedRemoteImageUrl(cafe.thumbnailImage) {
                                            CachedAsyncImage(
                                                url: imageUrl,
                                                placeholder: EmptyView()
                                            )
                                            .frame(width: imageSize.width, height: imageSize.height)
                                            .clipped()
                                            .clipShape(RoundedRectangle(cornerRadius: imageCornerRadius, style: .continuous))
                                        }
                                    }
                                }
                                .frame(width: 64, height: 64)
                                .clipShape(RoundedRectangle(cornerRadius: imageCornerRadius, style: .continuous))
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(cafe.name).bold()
                                    RatingBox(rating: ratingText)
                                    if !conceptType.isEmpty {
                                        Text(conceptType)
                                            .font(.caption.weight(.semibold))
                                            .foregroundStyle(ConCafeColors.primary)
                                            .lineLimit(1)
                                    }
                                }
                                Spacer()
                            }
                            .padding(10)
                            .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
                            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                            .onTapGesture {
                                onAction(.cafeTapped(id: cafe.id))
                            }
                        }
                    } else {
                        MyInfoSectionPlaceholderCard(
                            title: String(localized: String.LocalizationValue("myinfo_guest_popular_empty_title"), table: "Localizable"),
                            description: String(localized: String.LocalizationValue("myinfo_guest_popular_empty_desc"), table: "Localizable")
                        )
                    }
                }
                VStack(spacing: 8) {
                    Text("✨")
                        .font(.title2)
                    Text(String(localized: String.LocalizationValue("myinfo_guest_start_title"), table: "Localizable"))
                        .font(.headline)
                        .bold()
                    Text(String(localized: String.LocalizationValue("myinfo_guest_start_subtitle"), table: "Localizable"))
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Button {
                    } label: {
                        Text(String(localized: String.LocalizationValue("signin_sign_up"), table: "Localizable"))
                            .font(.subheadline.weight(.bold))
                            .foregroundStyle(.white)
                            .padding(.horizontal, 24)
                            .padding(.vertical, 10)
                            .background(
                                LinearGradient(
                                    colors: [ConCafeColors.primary, ConCafeColors.secondaryContainer],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                )
                            )
                            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                    }
                    .buttonStyle(.plain)
                }
                .padding(20)
                .frame(maxWidth: .infinity)
                .background(
                    LinearGradient(colors: [ConCafeColors.surfaceTint, ConCafeColors.surfaceTint], startPoint: .topLeading, endPoint: .bottomTrailing)
                )
                .clipShape(RoundedRectangle(cornerRadius: 26, style: .continuous))
            }
            .padding(16)
        }
    }

    private func resolvedRemoteImageUrl(_ raw: String?) -> URL? {
        let trimmed = raw?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if trimmed.isEmpty {
            return nil
        }
        return URL(string: trimmed)
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
        case "DOLL":
            return String(localized: String.LocalizationValue("home_nearby_cafe_type_doll"), table: "Localizable")
        case "COSPLAY":
            return String(localized: String.LocalizationValue("home_nearby_cafe_type_cosplay"), table: "Localizable")
        case "NAMJANG":
            return String(localized: String.LocalizationValue("home_nearby_cafe_type_namjang"), table: "Localizable")
        case "YOKAI":
            return String(localized: String.LocalizationValue("home_nearby_cafe_type_yokai"), table: "Localizable")
        case "CAT":
            return String(localized: String.LocalizationValue("home_nearby_cafe_type_cat"), table: "Localizable")
        case "OTHER":
            return String(localized: String.LocalizationValue("home_nearby_cafe_type_other"), table: "Localizable")
        default:
            return normalized
        }
    }
}

@MainActor
private struct ProfileMyInfoView: View {
    let uiState: MyInfoUiState

    let onAction: @MainActor (MyInfoAction) -> Void

    @State private var activeBadgeTooltip: BadgeTooltipOverlay?

    @State private var badgeTooltipSize: CGSize = .zero

    @State private var hideBadgeTooltipTask: Task<Void, Never>?

    private let badgeTooltipCoordinateSpace = "MyInfoBadgeSection"

    private let badgeTooltipVerticalSpacing: CGFloat = 8

    var body: some View {
        GeometryReader { geometry in
            ScrollView {
                VStack(spacing: 16) {
                    profileCard
                    statsCard
                    badgesSection
                    recentVisitsSection
                    favoritesSection(contentWidth: geometry.size.width)
                    if uiState.user?.role != .cast {
                        followedMaidsSection
                    }
                }
                .padding(16)
            }
        }
        .onDisappear {
            hideBadgeTooltipTask?.cancel()
            hideBadgeTooltipTask = nil
            activeBadgeTooltip = nil
        }
    }

    private var profileCard: some View {
        let user = uiState.user
        let castDetail = uiState.castDetail
        let ownerCafe = uiState.ownedCafes.first
        let title = {
            switch user?.role {
            case UserRole.cast:
                return castDetail?.cast.name ?? user?.nickname ?? String(localized: String.LocalizationValue("myinfo_profile_default_nickname"), table: "Localizable")
            default:
                return user?.nickname ?? String(localized: String.LocalizationValue("myinfo_profile_default_nickname"), table: "Localizable")
            }
        }()
        let subtitle = {
            switch user?.role {
            case UserRole.cast:
                guard let user, let castDetail else { return "" }
                return user.nickname == castDetail.cast.name ? "" : user.nickname
            case UserRole.cafeOwner:
                return String(localized: String.LocalizationValue("myinfo_profile_role_cafe_owner"), table: "Localizable")
            case UserRole.admin:
                return String(localized: String.LocalizationValue("myinfo_profile_role_admin_account"), table: "Localizable")
            default:
                return String(
                    format: String(localized: String.LocalizationValue("myinfo_profile_role_visitor_level"), table: "Localizable"),
                    locale: Locale.current,
                    uiState.summary?.level ?? 1
                )
            }
        }()
        let accentText = {
            switch user?.role {
            case UserRole.cast:
                return castDetail?.cafe.name ?? String(localized: String.LocalizationValue("myinfo_profile_affiliation_none"), table: "Localizable")
            case UserRole.cafeOwner:
                return ownerCafe?.name ?? String(localized: String.LocalizationValue("myinfo_profile_operating_cafe_none"), table: "Localizable")
            case UserRole.admin:
                return String(localized: String.LocalizationValue("myinfo_profile_accent_admin"), table: "Localizable")
            default:
                return String(localized: String.LocalizationValue("myinfo_profile_accent_visitor"), table: "Localizable")
            }
        }()
        return HStack(spacing: 16) {
            Circle()
                .fill(
                    LinearGradient(
                        colors: [ConCafeColors.primaryContainer, ConCafeColors.secondaryContainer],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
                .frame(width: 78, height: 78)
                .overlay(
                    Text(String(title.prefix(2)).uppercased())
                        .font(.title3.weight(.bold))
                        .foregroundStyle(ConCafeColors.primary)
                )
            VStack(alignment: .leading, spacing: 6) {
                HStack(alignment: .bottom, spacing: 8) {
                    Text(title)
                        .font(.title2.weight(.bold))
                        .foregroundStyle(.primary)
                    if !subtitle.isEmpty {
                        Text(subtitle)
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                }
                HStack(spacing: 6) {
                    Image(systemName: "mappin.and.ellipse")
                        .font(.caption.weight(.bold))
                        .foregroundStyle(ConCafeColors.primary)
                    Text(accentText)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
            }
            Spacer(minLength: 0)
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }).opacity(0.94))
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .stroke(Color(uiColor: .separator).opacity(0.35), lineWidth: 1)
        )
        .shadow(color: Color.black.opacity(0.04), radius: 12, x: 0, y: 6)
    }

    private var statsCard: some View {
        return HStack(spacing: 8) {
            ForEach(metricCards, id: \.title) { metric in
                profileStat(metric)
            }
        }
    }

    private func profileStat(_ metric: MyInfoMetricCard) -> some View {
        VStack(spacing: 6) {
            Text(metric.title)
                .font(.caption2.weight(.bold))
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
            Text(metric.value)
                .font(.title2.weight(.bold))
                .foregroundStyle(metric.highlight ? ConCafeColors.primary : .primary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 16)
        .padding(.horizontal, 10)
        .background(metric.highlight ? ConCafeColors.primaryContainer.opacity(0.10) : Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }).opacity(0.92))
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .shadow(color: metric.highlight ? .clear : Color.black.opacity(0.04), radius: 4, x: 0, y: 2)
        .overlay(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .stroke(metric.highlight ? ConCafeColors.primary.opacity(0.20) : ConCafeColors.primaryContainer.opacity(0.10), lineWidth: 1)
        )
    }

    private var metricCards: [MyInfoMetricCard] {
        switch uiState.user?.role {
        case UserRole.cast:
            return [
                .init(title: String(localized: String.LocalizationValue("myinfo_metric_total_followers"), table: "Localizable"), value: "\(uiState.castDetail?.cast.followerCount ?? 0)", highlight: false),
                .init(title: String(localized: String.LocalizationValue("myinfo_metric_work_schedule"), table: "Localizable"), value: "\(currentWeekScheduleCount)", highlight: true),
                .init(title: String(localized: String.LocalizationValue("myinfo_metric_rating"), table: "Localizable"), value: RatingUtils.formatOneDecimal(uiState.castDetail?.cast.rating ?? 0), highlight: false)
            ]
        case UserRole.cafeOwner:
            let cafeCount = uiState.ownedCafes.count
            let castCount = uiState.ownedCafes.reduce(0) { $0 + Int($1.castCount) }
            let rating = uiState.ownedCafes.isEmpty ? 0 : uiState.ownedCafes.map(\.rating).reduce(0, +) / Double(uiState.ownedCafes.count)
            return [
                .init(title: String(localized: String.LocalizationValue("myinfo_metric_operating_cafes"), table: "Localizable"), value: "\(cafeCount)", highlight: false),
                .init(title: String(localized: String.LocalizationValue("myinfo_metric_affiliated_casts"), table: "Localizable"), value: "\(castCount)", highlight: true),
                .init(title: String(localized: String.LocalizationValue("myinfo_metric_average_rating"), table: "Localizable"), value: RatingUtils.formatOneDecimal(rating), highlight: false)
            ]
        default:
            return [
                .init(title: String(localized: String.LocalizationValue("myinfo_metric_visit_count"), table: "Localizable"), value: "\(uiState.summary?.totalVisits ?? 0)", highlight: false),
                .init(title: String(localized: String.LocalizationValue("myinfo_metric_favorites"), table: "Localizable"), value: "\(uiState.favorites.count)", highlight: true),
                .init(title: String(localized: String.LocalizationValue("myinfo_metric_following"), table: "Localizable"), value: "\(uiState.summary?.followedCastsCount ?? 0)", highlight: false)
            ]
        }
    }

    private var currentWeekScheduleCount: Int {
        guard let schedules = uiState.castDetail?.schedule, !schedules.isEmpty else {
            return 0
        }
        let calendar = Calendar.current
        let now = Date()
        let weekStart = calendar.dateInterval(of: .weekOfYear, for: now)?.start ?? now
        let weekEnd = calendar.date(byAdding: .day, value: 6, to: weekStart) ?? weekStart
        let formatter = DateFormatter()

        formatter.calendar = calendar
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "yyyy-MM-dd"

        let includedDates = schedules.compactMap { schedule -> Date? in
            let normalizedDate = String(schedule.date.prefix(10))
            return formatter.date(from: normalizedDate)
        }.filter { scheduleDate in
            let day = calendar.startOfDay(for: scheduleDate)
            return day >= calendar.startOfDay(for: weekStart) && day <= calendar.startOfDay(for: weekEnd)
        }.map { scheduleDate in
            formatter.string(from: scheduleDate)
        }

        return Set(includedDates).count
    }

    private var badgesSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                MyInfoSectionTitle(title: String(localized: String.LocalizationValue("myinfo_profile_section_badges"), table: "Localizable"))
                Spacer()
                Text("\(uiState.badges.filter { $0.unlocked }.count) / \(uiState.badges.count)")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    if !uiState.badges.isEmpty {
                        ForEach(uiState.badges, id: \.id) { badge in
                            BadgeItemView(
                                badge: badge,
                                coordinateSpaceName: badgeTooltipCoordinateSpace,
                                onTap: { tappedFrame in
                                    presentBadgeTooltip(badge: badge, anchorFrame: tappedFrame)
                                }
                            )
                        }
                    } else {
                        MyInfoSectionPlaceholderCard(
                            title: String(localized: String.LocalizationValue("myinfo_profile_badges_empty_title"), table: "Localizable"),
                            description: String(localized: String.LocalizationValue("myinfo_profile_badges_empty_desc"), table: "Localizable")
                        )
                    }
                }
            }
        }
        .coordinateSpace(name: badgeTooltipCoordinateSpace)
        .overlay(alignment: .topLeading) {
            GeometryReader { proxy in
                if let tooltip = activeBadgeTooltip {
                    let halfTooltipWidth = badgeTooltipSize.width / 2
                    let minimumCenterX = halfTooltipWidth
                    let maximumCenterX = max(halfTooltipWidth, proxy.size.width - halfTooltipWidth)
                    let centerX = tooltip.anchorFrame.midX.clamped(to: minimumCenterX...maximumCenterX)
                    let halfTooltipHeight = badgeTooltipSize.height / 2
                    let centerY = max(
                        halfTooltipHeight,
                        tooltip.anchorFrame.minY - badgeTooltipVerticalSpacing - halfTooltipHeight
                    )
                    BadgeTooltipCard(badge: tooltip.badge)
                        .fixedSize()
                        .readSize { badgeTooltipSize = $0 }
                        .position(x: centerX, y: centerY)
                        .zIndex(20)
                        .allowsHitTesting(false)
                        .transition(.opacity.animation(.easeInOut(duration: 0.15)))
                }
            }
        }
    }

    private func presentBadgeTooltip(badge: ProfileBadge, anchorFrame: CGRect) {
        hideBadgeTooltipTask?.cancel()
        withAnimation(.easeInOut(duration: 0.15)) {
            activeBadgeTooltip = BadgeTooltipOverlay(
                badge: badge,
                anchorFrame: anchorFrame
            )
        }
        hideBadgeTooltipTask = Task { @MainActor in
            do {
                try await Task.sleep(nanoseconds: 2_000_000_000)
            } catch {
                return
            }
            guard !Task.isCancelled else {
                return
            }
            withAnimation(.easeInOut(duration: 0.15)) {
                activeBadgeTooltip = nil
            }
        }
    }

    private var recentVisitsSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            MyInfoSectionTitle(title: String(localized: String.LocalizationValue("myinfo_profile_section_recent_visits"), table: "Localizable"))
            if !uiState.recentVisits.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 10) {
                        ForEach(uiState.recentVisits, id: \.id) { cafe in
                            VStack(alignment: .leading, spacing: 6) {
                                GeometryReader { geometry in
                                    let imageSize = geometry.size

                                    ZStack {
                                        RoundedRectangle(cornerRadius: 14, style: .continuous)
                                            .fill(
                                                LinearGradient(
                                                    colors: [ConCafeColors.warningContainer, ConCafeColors.warningContainer],
                                                    startPoint: .top,
                                                    endPoint: .bottom
                                                )
                                            )
                                        CachedAsyncImage(
                                            url: ImageUrlUtils.normalizedRemoteUrl(from: cafe.thumbnailImage),
                                            placeholder: EmptyView()
                                        )
                                        .frame(width: imageSize.width, height: imageSize.height)
                                        .clipped()
                                    }
                                }
                                .frame(width: 120, height: 120)
                                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                                Text(cafe.name).font(.caption)
                            }
                            .onTapGesture { onAction(.cafeTapped(id: cafe.id)) }
                        }
                    }
                }
            } else {
                MyInfoSectionPlaceholderCard(
                    title: String(localized: String.LocalizationValue("myinfo_profile_recent_visits_empty_title"), table: "Localizable"),
                    description: String(localized: String.LocalizationValue("myinfo_profile_recent_visits_empty_desc"), table: "Localizable")
                )
            }
        }
    }

    private func favoritesSection(contentWidth: CGFloat) -> some View {
        let favoriteItems = Array(uiState.favorites.prefix(12))
        let columnCount = myInfoGridColumnCount(for: contentWidth)
        return VStack(alignment: .leading, spacing: 8) {
            MyInfoSectionTitle(title: String(localized: String.LocalizationValue("myinfo_profile_section_favorites"), table: "Localizable"))
            if !favoriteItems.isEmpty {
                LazyVGrid(columns: myInfoGridColumns(count: columnCount), spacing: myInfoGridItemSpacing) {
                    ForEach(favoriteItems, id: \.id) { cafe in
                        CafeSummaryCard(
                            name: cafe.name,
                            rating: favoriteCafeRating(cafe.ratingAvg),
                            conceptType: localizedCafeConceptType(cafe.conceptType),
                            location: localizedRegionCity(cafe.region.city),
                            thumbnailImage: cafe.thumbnailImage,
                            showLocationIcon: false,
                            trailingLabel: nil,
                            onTap: {
                                onAction(.cafeTapped(id: cafe.id))
                            }
                        )
                    }
                }
            } else {
                MyInfoSectionPlaceholderCard(
                    title: String(localized: String.LocalizationValue("myinfo_profile_favorites_empty_title"), table: "Localizable"),
                    description: String(localized: String.LocalizationValue("myinfo_profile_favorites_empty_desc"), table: "Localizable")
                )
            }
        }
    }

    private func myInfoGridColumns(count: Int) -> [GridItem] {
        Array(
            repeating: GridItem(.flexible(), spacing: myInfoGridItemSpacing),
            count: max(count, 1)
        )
    }

    private func myInfoGridColumnCount(for contentWidth: CGFloat) -> Int {
        let availableWidth = contentWidth - myInfoGridHorizontalPadding
        let minimumGridWidth = (myInfoGridMinimumCellWidth * 2) + myInfoGridItemSpacing
        let normalizedWidth = max(availableWidth, minimumGridWidth)
        let rawCount = Int((normalizedWidth + myInfoGridItemSpacing) /
            (myInfoGridMinimumCellWidth + myInfoGridItemSpacing))
        return min(max(rawCount, myInfoGridMinimumColumnCount), myInfoGridMaximumColumnCount)
    }

    private func favoriteCafeRating(_ rating: Double) -> String {
        RatingUtils.formatOneDecimal(rating)
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
        case "DOLL":
            return String(localized: String.LocalizationValue("home_nearby_cafe_type_doll"), table: "Localizable")
        case "COSPLAY":
            return String(localized: String.LocalizationValue("home_nearby_cafe_type_cosplay"), table: "Localizable")
        case "NAMJANG":
            return String(localized: String.LocalizationValue("home_nearby_cafe_type_namjang"), table: "Localizable")
        case "YOKAI":
            return String(localized: String.LocalizationValue("home_nearby_cafe_type_yokai"), table: "Localizable")
        case "CAT":
            return String(localized: String.LocalizationValue("home_nearby_cafe_type_cat"), table: "Localizable")
        case "OTHER":
            return String(localized: String.LocalizationValue("home_nearby_cafe_type_other"), table: "Localizable")
        default:
            return normalized
        }
    }

    private var followedMaidsSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            MyInfoSectionTitle(title: String(localized: String.LocalizationValue("myinfo_profile_section_followed_casts"), table: "Localizable"))
            if !uiState.followedMaids.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 12) {
                        ForEach(uiState.followedMaids.prefix(6), id: \.id) { maid in
                            VStack {
                                GeometryReader { geometry in
                                    let imageSize = geometry.size

                                    ZStack {
                                        Circle()
                                            .fill(LinearGradient(colors: [ConCafeColors.surfaceTint, ConCafeColors.primaryContainer], startPoint: .top, endPoint: .bottom))
                                        CachedAsyncImage(
                                            url: ImageUrlUtils.normalizedRemoteUrl(from: maid.profileImage),
                                            placeholder: EmptyView()
                                        )
                                        .frame(width: imageSize.width, height: imageSize.height)
                                        .clipped()
                                    }
                                }
                                .frame(width: 72, height: 72)
                                .clipShape(Circle())
                                Text(maid.name)
                                    .font(.caption)
                            }
                            .onTapGesture { onAction(.maidTapped(id: maid.id)) }
                        }
                    }
                }
            } else {
                MyInfoSectionPlaceholderCard(
                    title: String(localized: String.LocalizationValue("myinfo_profile_followed_casts_empty_title"), table: "Localizable"),
                    description: String(localized: String.LocalizationValue("myinfo_profile_followed_casts_empty_desc"), table: "Localizable")
                )
            }
        }
    }
}

private struct BadgeItemView: View {
    let badge: ProfileBadge

    let coordinateSpaceName: String

    let onTap: (CGRect) -> Void

    var body: some View {
        VStack(spacing: 4) {
            GeometryReader { proxy in
                RoundedRectangle(cornerRadius: 16)
                    .fill(badge.unlocked ? ConCafeColors.primary : ConCafeColors.outline)
                    .overlay(Text(badge.icon))
                    .onTapGesture {
                        let frame = proxy.frame(in: .named(coordinateSpaceName))
                        onTap(frame)
                    }
            }
            .frame(width: 70, height: 70)
            Text(badge.name)
                .font(.caption2)
                .lineLimit(1)
        }
    }
}

private struct BadgeTooltipOverlay {
    let badge: ProfileBadge
    let anchorFrame: CGRect
}

private struct BadgeTooltipCard: View {
    let badge: ProfileBadge

    var body: some View {
        let current = min(Int(badge.currentCount), Int(badge.goalCount))
        let goal = Int(badge.goalCount)
        return VStack(spacing: 2) {
            Text(badge.name)
                .font(.caption2.weight(.semibold))
                .foregroundStyle(.white)
            Text("\(current) / \(goal)")
                .font(.caption.weight(.bold))
                .foregroundStyle(badge.unlocked ? ConCafeColors.primary : Color.white.opacity(0.7))
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 6)
        .background(.primary.opacity(0.92))
        .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
        .shadow(color: .black.opacity(0.15), radius: 4, x: 0, y: 2)
        .allowsHitTesting(false)
        .fixedSize()
    }
}

private struct MyInfoMetricCard {
    let title: String
    let value: String
    let highlight: Bool
}

private struct MyInfoSectionTitle: View {
    let title: String

    var body: some View {
        Text(title)
            .font(.headline.weight(.bold))
            .foregroundStyle(ConCafeColors.textPrimary)
    }
}

private struct MyInfoSectionPlaceholderCard: View {
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
        .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
    }
}

private struct ViewSizePreferenceKey: PreferenceKey {
    static var defaultValue: CGSize = .zero

    static func reduce(value: inout CGSize, nextValue: () -> CGSize) {
        value = nextValue()
    }
}

private extension View {
    func readSize(onChange: @escaping (CGSize) -> Void) -> some View {
        background(
            GeometryReader { proxy in
                Color.clear
                    .preference(key: ViewSizePreferenceKey.self, value: proxy.size)
            }
        )
        .onPreferenceChange(ViewSizePreferenceKey.self, perform: onChange)
    }
}

private extension CGFloat {
    func clamped(to range: ClosedRange<CGFloat>) -> CGFloat {
        Swift.min(Swift.max(self, range.lowerBound), range.upperBound)
    }
}

struct MyInfoView_Previews: PreviewProvider {
    static var previews: some View {
        MyInfoView(onNavigationAction: { _ in })
    }
}

private let myInfoGridMinimumColumnCount = 2
private let myInfoGridMaximumColumnCount = 6
private let myInfoGridHorizontalPadding: CGFloat = 24
private let myInfoGridItemSpacing: CGFloat = 12
private let myInfoGridMinimumCellWidth: CGFloat = 180
