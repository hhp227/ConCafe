//
//  MyInfoView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/05.
//

import SwiftUI
import Shared

struct MyInfoView: View {
    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel = MyInfoViewModel()

    var body: some View {
        Group {
            if viewModel.uiState.isLoading {
                ProgressView()
                    .controlSize(.large)
                    .tint(Color(hex: "EF6797"))
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
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
        .background(Color(hex: "FFF9FC"))
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
                    .foregroundStyle(Color(hex: "EF6797"))
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                }
                .padding(20)
                .frame(maxWidth: .infinity)
                .background(
                    LinearGradient(colors: [Color(hex: "EF6797"), Color(hex: "F8A0C2")], startPoint: .topLeading, endPoint: .bottomTrailing)
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
                            .background(.white)
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
                            .foregroundStyle(Color(hex: "EF6797"))
                        }
                        .buttonStyle(.plain)
                    }
                    if !uiState.popularCafes.isEmpty {
                        ForEach(uiState.popularCafes, id: \.id) { cafe in
                            let ratingText = String(
                                format: "%.1f",
                                locale: Locale(identifier: "en_US_POSIX"),
                                cafe.ratingAvg
                            )
                            let conceptType = localizedCafeConceptType(cafe.conceptType)
                            let imageCornerRadius: CGFloat = 12

                            HStack(spacing: 10) {
                                GeometryReader { geometry in
                                    let imageSize = geometry.size

                                    ZStack {
                                        RoundedRectangle(cornerRadius: imageCornerRadius, style: .continuous)
                                            .fill(LinearGradient(colors: [Color(hex: "FFE2D2"), Color(hex: "FFC9A9")], startPoint: .top, endPoint: .bottom))
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
                                            .foregroundStyle(Color(hex: "EF6797"))
                                            .lineLimit(1)
                                    }
                                }
                                Spacer()
                            }
                            .padding(10)
                            .background(.white)
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
                                    colors: [Color(hex: "EF6797"), Color(hex: "F8A0C2")],
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
                    LinearGradient(colors: [Color(hex: "FFEAF2"), Color(hex: "FDE3F0")], startPoint: .topLeading, endPoint: .bottomTrailing)
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
        default:
            return normalized
        }
    }
}

@MainActor
private struct ProfileMyInfoView: View {
    let uiState: MyInfoUiState

    let onAction: @MainActor (MyInfoAction) -> Void

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
    }

    private var profileCard: some View {
        let user = uiState.user
        let castDetail = uiState.castDetail
        let ownerCafe = uiState.ownedCafes.first
        let title = {
            switch user?.role {
            case UserRole.cast:
                return castDetail?.cast.name ?? user?.nickname ?? "메이드러버"
            default:
                return user?.nickname ?? "메이드러버"
            }
        }()
        let subtitle = {
            switch user?.role {
            case UserRole.cast:
                guard let user, let castDetail else { return "" }
                return user.nickname == castDetail.cast.name ? "" : user.nickname
            case UserRole.cafeOwner:
                return "카페 운영자"
            case UserRole.admin:
                return "관리자 계정"
            default:
                return "레벨 \(uiState.summary?.level ?? 1) · 열정적인 팬"
            }
        }()
        let accentText = {
            switch user?.role {
            case UserRole.cast:
                return castDetail?.cafe.name ?? "소속 카페 없음"
            case UserRole.cafeOwner:
                return ownerCafe?.name ?? "운영 카페 없음"
            case UserRole.admin:
                return "ConCafe 운영"
            default:
                return "내 활동 요약"
            }
        }()
        return HStack(spacing: 16) {
            Circle()
                .fill(
                    LinearGradient(
                        colors: [Color(hex: "FFD7E5"), Color(hex: "F2ADC2")],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
                .frame(width: 78, height: 78)
                .overlay(
                    Text(String(title.prefix(2)).uppercased())
                        .font(.title3.weight(.bold))
                        .foregroundStyle(Color(hex: "7C3F67"))
                )
            VStack(alignment: .leading, spacing: 6) {
                HStack(alignment: .bottom, spacing: 8) {
                    Text(title)
                        .font(.title2.weight(.bold))
                        .foregroundStyle(Color(hex: "24161E"))
                    if !subtitle.isEmpty {
                        Text(subtitle)
                            .font(.subheadline)
                            .foregroundStyle(Color(hex: "7A707A"))
                    }
                }
                HStack(spacing: 6) {
                    Image(systemName: "mappin.and.ellipse")
                        .font(.caption.weight(.bold))
                        .foregroundStyle(Color(hex: "EF6797"))
                    Text(accentText)
                        .font(.subheadline)
                        .foregroundStyle(Color(hex: "5B4A57"))
                }
            }
            Spacer(minLength: 0)
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white.opacity(0.94))
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .stroke(Color.white.opacity(0.65), lineWidth: 1)
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
                .foregroundStyle(Color(hex: "7A707A"))
                .multilineTextAlignment(.center)
            Text(metric.value)
                .font(.title2.weight(.bold))
                .foregroundStyle(metric.highlight ? Color(hex: "D94A82") : Color(hex: "24161E"))
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 16)
        .padding(.horizontal, 10)
        .background(metric.highlight ? Color(hex: "FFD1DC").opacity(0.10) : Color.white.opacity(0.92))
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .shadow(color: metric.highlight ? .clear : Color.black.opacity(0.04), radius: 4, x: 0, y: 2)
        .overlay(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .stroke(metric.highlight ? Color(hex: "FFB3C6").opacity(0.20) : Color(hex: "FFD1DC").opacity(0.10), lineWidth: 1)
        )
    }

    private var metricCards: [MyInfoMetricCard] {
        switch uiState.user?.role {
        case UserRole.cast:
            return [
                .init(title: "전체 팔로워", value: "\(uiState.castDetail?.cast.followerCount ?? 0)", highlight: false),
                .init(title: "근무 일정", value: "\(currentWeekScheduleCount)", highlight: true),
                .init(title: "평점", value: String(format: "%.1f", uiState.castDetail?.cast.rating ?? 0), highlight: false)
            ]
        case UserRole.cafeOwner:
            let cafeCount = uiState.ownedCafes.count
            let castCount = uiState.ownedCafes.reduce(0) { $0 + Int($1.castCount) }
            let rating = uiState.ownedCafes.isEmpty ? 0 : uiState.ownedCafes.map(\.rating).reduce(0, +) / Double(uiState.ownedCafes.count)
            return [
                .init(title: "운영 카페", value: "\(cafeCount)", highlight: false),
                .init(title: "소속 캐스트", value: "\(castCount)", highlight: true),
                .init(title: "평균 평점", value: String(format: "%.1f", rating), highlight: false)
            ]
        default:
            return [
                .init(title: "방문 횟수", value: "\(uiState.summary?.totalVisits ?? 0)", highlight: false),
                .init(title: "즐겨찾기", value: "\(uiState.favorites.count)", highlight: true),
                .init(title: "팔로우", value: "\(uiState.summary?.followedCastsCount ?? 0)", highlight: false)
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
                MyInfoSectionTitle(title: "활동 뱃지")
                Spacer()
                Text("\(uiState.badges.filter { $0.unlocked }.count) / \(uiState.badges.count)")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    if !uiState.badges.isEmpty {
                        ForEach(uiState.badges, id: \.id) { badge in
                            BadgeItemView(badge: badge)
                        }
                    } else {
                        MyInfoSectionPlaceholderCard(
                            title: "획득한 활동 뱃지가 아직 없어요",
                            description: "체크인과 팬 활동으로 첫 뱃지를 모아보세요."
                        )
                    }
                }
            }
        }
    }

    private var recentVisitsSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            MyInfoSectionTitle(title: "최근 방문")
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
                                                    colors: [Color(hex: "FFE2D2"), Color(hex: "FFC9A9")],
                                                    startPoint: .top,
                                                    endPoint: .bottom
                                                )
                                            )
                                        if let imageUrl = resolvedRemoteImageUrl(cafe.thumbnailImage) {
                                            CachedAsyncImage(
                                                url: imageUrl,
                                                placeholder: EmptyView()
                                            )
                                            .frame(width: imageSize.width, height: imageSize.height)
                                            .clipped()
                                        } else {
                                            Image(systemName: "photo")
                                                .font(.system(size: 22, weight: .semibold))
                                                .foregroundStyle(Color.white.opacity(0.82))
                                        }
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
                    title: "최근 방문 기록이 없어요",
                    description: "첫 체크인을 완료하면 이곳에 방문한 카페가 표시됩니다."
                )
            }
        }
    }

    private func favoritesSection(contentWidth: CGFloat) -> some View {
        let favoriteItems = Array(uiState.favorites.prefix(12))
        let columnCount = myInfoGridColumnCount(for: contentWidth)
        return VStack(alignment: .leading, spacing: 8) {
            MyInfoSectionTitle(title: "즐겨찾기")
            if !favoriteItems.isEmpty {
                LazyVGrid(columns: myInfoGridColumns(count: columnCount), spacing: myInfoGridItemSpacing) {
                    ForEach(favoriteItems, id: \.id) { cafe in
                        CafeSummaryCard(
                            name: cafe.name,
                            rating: favoriteCafeRating(cafe.ratingAvg),
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
                }
            } else {
                MyInfoSectionPlaceholderCard(
                    title: "즐겨찾기한 카페가 없어요",
                    description: "좋아하는 카페를 즐겨찾기에 추가해보세요."
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
        String(format: "%.1f", locale: Locale(identifier: "en_US_POSIX"), rating)
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

    private var followedMaidsSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            MyInfoSectionTitle(title: "팔로우한 캐스트")
            if !uiState.followedMaids.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 12) {
                        ForEach(uiState.followedMaids.prefix(6), id: \.id) { maid in
                            VStack {
                                GeometryReader { geometry in
                                    let imageSize = geometry.size

                                    ZStack {
                                        Circle()
                                            .fill(LinearGradient(colors: [Color(hex: "FFDFEA"), Color(hex: "FFBED5")], startPoint: .top, endPoint: .bottom))
                                        if let imageUrl = resolvedRemoteImageUrl(maid.profileImage) {
                                            CachedAsyncImage(
                                                url: imageUrl,
                                                placeholder: EmptyView()
                                            )
                                            .frame(width: imageSize.width, height: imageSize.height)
                                            .clipped()
                                        }
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
                    title: "팔로우한 캐스트가 없어요",
                    description: "관심 있는 캐스트를 팔로우하면 여기서 바로 볼 수 있어요."
                )
            }
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

private struct BadgeItemView: View {
    let badge: ProfileBadge

    @State private var showTooltip = false

    var body: some View {
        VStack(spacing: 4) {
            ZStack(alignment: .top) {
                RoundedRectangle(cornerRadius: 16)
                    .fill(badge.unlocked ? Color(hex: "EF6797") : Color(hex: "DADADA"))
                    .frame(width: 70, height: 70)
                    .overlay(Text(badge.icon))
                if showTooltip {
                    BadgeTooltipCard(badge: badge)
                        .offset(y: -56)
                        .zIndex(10)
                        .transition(.opacity.animation(.easeInOut(duration: 0.15)))
                }
            }
            .onTapGesture {
                showTooltip = true
                Task {
                    try? await Task.sleep(nanoseconds: 2_000_000_000)
                    showTooltip = false
                }
            }
            Text(badge.name)
                .font(.caption2)
                .lineLimit(1)
        }
    }
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
                .foregroundStyle(badge.unlocked ? Color(hex: "EF6797") : Color.white.opacity(0.7))
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 6)
        .background(Color(hex: "2B2330").opacity(0.92))
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
            .foregroundStyle(Color(hex: "2B2330"))
    }
}

private struct MyInfoSectionPlaceholderCard: View {
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
