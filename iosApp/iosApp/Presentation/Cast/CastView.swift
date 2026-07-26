//
//  CastView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/05.
//

import SwiftUI
import Shared

private let castSummaryTitleTriggerOffset: CGFloat = 22
struct CastView: View {
    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel: CastViewModel

    var body: some View {
        CastContentView(
            uiState: viewModel.uiState,
            onAction: viewModel.onAction
        )
        .compatNavigationBarStyle(.transparentScrollEdge)
        .onReceive(viewModel.event) { event in
            switch event {
            case .navigateBack:
                onNavigationAction(.navigateBack)
            case .navigateToCafe(let id):
                onNavigationAction(.navigateToCafe(id: id))
            case .navigateToSignIn:
                onNavigationAction(.navigateToSignIn)
            case .navigateToPicture(let imageUrl):
                onNavigationAction(.navigateToPicture(imageUrl: imageUrl))
            }
        }
    }

    init(
        castId: String,
        onNavigationAction: @escaping (NavigationAction) -> Void
    ) {
        self.onNavigationAction = onNavigationAction
        _viewModel = StateObject(wrappedValue: CastViewModel(castId: castId))
    }
}

private struct CastContentView: View {
    let uiState: CastUiState

    let onAction: (CastAction) -> Void

    @State private var scrollOffset: CGFloat = 0

    @State private var summarySectionMinY: CGFloat = .greatestFiniteMagnitude

    var body: some View {
        GeometryReader { proxy in
            ZStack(alignment: .top) {
                content(topSafeArea: proxy.safeAreaInsets.top)
                    .background(ConCafeColors.background)
            }
            .ignoresSafeArea(edges: .top)
        }
    }

    private var navBarVisible: Bool {
        uiState.detail != nil && summarySectionMinY <= castSummaryTitleTriggerOffset
    }

    @ViewBuilder
    private func content(topSafeArea: CGFloat) -> some View {
        if let detail = uiState.detail {
            ScrollView {
                offsetReader
                LazyVStack(spacing: 18) {
                    CastHeroSection(detail: detail, scrollOffset: scrollOffset, topSafeArea: topSafeArea)
                        .onHeroImageTap { imageUrl in
                            onAction(.imageTapped(imageUrl: imageUrl))
                        }
                    CastSummarySection(
                        detail: detail,
                        isFollowing: uiState.isFollowing,
                        isSelfCast: uiState.isSelfCast,
                        shouldShowFollowTooltip: uiState.shouldShowFollowTooltip,
                        onAction: onAction
                    )
                        .background(summaryOffsetReader)
                    CastTodaySection(detail: detail)
                    CastScheduleSection(detail: detail)
                    CastIntroductionSection(detail: detail)
                    CastRecentActivitySection(detail: detail)
                    CastRecentReviewSection(reviews: uiState.recentReviews)
                }
                .padding(.bottom, 28)
            }
            .coordinateSpace(name: "castScroll")
            .ignoresSafeArea(edges: .top)
            .compatScrollContentInsetAdjustmentNever()
            .onPreferenceChange(CastSummaryOffsetPreferenceKey.self) { value in
                summarySectionMinY = value
            }
        } else if uiState.isLoading {
            ShimmerCardListSkeleton(itemCount: 2, imageHeight: 260)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        } else {
            VStack(spacing: 12) {
                Text(String(localized: String.LocalizationValue("cast_error_detail_load_failed"), table: "Localizable"))
                    .foregroundStyle(.red)
                Button(String(localized: String.LocalizationValue("cast_action_refresh"), table: "Localizable")) {
                    onAction(.refresh)
                }
                .buttonStyle(.borderedProminent)
                .tint(ConCafeColors.primary)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
    }

    private var offsetReader: some View {
        GeometryReader { proxy in
            Color.clear
                .preference(
                    key: CastScrollOffsetPreferenceKey.self,
                    value: proxy.frame(in: .named("castScroll")).minY
                )
        }
        .frame(height: 0)
        .onPreferenceChange(CastScrollOffsetPreferenceKey.self) { value in
            scrollOffset = value
        }
    }

    private var summaryOffsetReader: some View {
        GeometryReader { proxy in
            Color.clear
                .preference(
                    key: CastSummaryOffsetPreferenceKey.self,
                    value: proxy.frame(in: .named("castScroll")).minY
                )
        }
    }
}

private struct CastHeroSection: View {
    let detail: CastDetail

    let scrollOffset: CGFloat

    let topSafeArea: CGFloat

    var onImageTap: ((String) -> Void)? = nil

    var body: some View {
        let heroHeight = 230 + topSafeArea
        let pullDownOffset = scrollOffset > 0 ? scrollOffset : 0
        let dynamicHeroHeight = heroHeight + pullDownOffset
        let heroImages = resolveHeroImages(
            images: detail.images,
            fallbackProfileImage: detail.cast.profileImage
        )

        TabView {
            ForEach(Array(heroImages.enumerated()), id: \.offset) { index, image in
                let trimmed = image.trimmingCharacters(in: .whitespacesAndNewlines)

                ZStack {
                    if let url = ImageUrlUtils.normalizedRemoteUrl(from: trimmed), !trimmed.isEmpty {
                        GeometryReader { geometry in
                            CachedAsyncImage(
                                url: url,
                                placeholder: heroPlaceholder(index: index),
                                displaySize: .medium
                            )
                            .frame(width: geometry.size.width, height: geometry.size.height)
                            .clipped()
                        }
                    } else {
                        heroPlaceholder(index: index)
                    }
                    LinearGradient(
                        colors: [.clear, Color.black.opacity(0.4)],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                    if trimmed.isEmpty {
                        VStack(spacing: 8) {
                            Image(systemName: "person.fill")
                                .font(.system(size: 54))
                                .foregroundStyle(.white)
                            Text(detail.cast.name)
                                .font(.headline.weight(.bold))
                                .foregroundStyle(.white)
                        }
                        .padding(24)
                        .background(Circle().fill(Color.white.opacity(0.16)))
                    }
                    Text(detail.cafe.name)
                        .font(.subheadline.weight(.medium))
                        .foregroundStyle(Color.white.opacity(0.9))
                        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomLeading)
                        .padding(20)
                }
                .frame(height: dynamicHeroHeight)
                .clipped()
                .contentShape(Rectangle())
                .onTapGesture {
                    if !trimmed.isEmpty {
                        onImageTap?(trimmed)
                    }
                }
            }
        }
        .frame(height: dynamicHeroHeight)
        .offset(y: pullDownOffset > 0 ? -pullDownOffset : 0)
        .frame(height: dynamicHeroHeight, alignment: .top)
        .clipShape(Rectangle())
        .tabViewStyle(.page(indexDisplayMode: .automatic))
    }

    func onHeroImageTap(_ action: @escaping (String) -> Void) -> CastHeroSection {
        var copy = self
        copy.onImageTap = action
        return copy
    }

    @ViewBuilder
    private func heroPlaceholder(index: Int) -> some View {
        let gradients: [[Color]] = [
            [ConCafeColors.secondaryContainer, ConCafeColors.primary],
            [ConCafeColors.errorContainer, ConCafeColors.tertiary],
            [ConCafeColors.primaryContainer, ConCafeColors.primary]
        ]
        LinearGradient(
            colors: gradients[index % gradients.count],
            startPoint: .top,
            endPoint: .bottom
        )
    }
}

private func resolveHeroImages(
    images: [String],
    fallbackProfileImage: String?
) -> [String] {
    let normalized = images
        .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
        .filter { !$0.isEmpty }
    if !normalized.isEmpty {
        return normalized
    }
    let fallback = (fallbackProfileImage ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
    return fallback.isEmpty ? [""] : [fallback]
}

private struct CastSummarySection: View {
    let detail: CastDetail

    let isFollowing: Bool

    let isSelfCast: Bool

    let shouldShowFollowTooltip: Bool

    let onAction: (CastAction) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            ZStack(alignment: .topTrailing) {
                HStack(alignment: .top, spacing: 12) {
                    VStack(alignment: .leading, spacing: 8) {
                        HStack(spacing: 6) {
                            Text(detail.cast.name)
                                .font(.title2.bold())
                            if let linkedUserId = detail.cast.linkedUserId, !linkedUserId.isEmpty {
                                Image(systemName: "checkmark.seal.fill")
                                    .foregroundStyle(ConCafeColors.primary)
                                    .font(.title2)
                            }
                        }
                        Text(detail.cast.conceptRole.capitalized)
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(ConCafeColors.primary)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 5)
                            .background(Capsule().fill(ConCafeColors.surfaceTint))
                        Button {
                            onAction(.cafeTapped)
                        } label: {
                            HStack(spacing: 4) {
                                Image(systemName: "mappin.and.ellipse")
                                Text("\(detail.cafe.name) · \(detail.cafe.region.city)")
                            }
                            .font(.subheadline)
                            .foregroundStyle(Color.secondary)
                        }
                    }
                    Spacer()
                    DetailTooltipBox(
                        visible: shouldShowFollowTooltip,
                        text: String(
                            localized: String.LocalizationValue("cast_follow_tooltip"),
                            table: "Localizable"
                        ),
                        offset: CGSize(width: 0, height: 48),
                        onShown: {
                            onAction(.followTooltipShown)
                        },
                        onDismiss: {
                            onAction(.dismissFollowTooltip)
                        }
                    ) {
                        Button {
                            onAction(.followTapped)
                        } label: {
                            Text(
                                isFollowing
                                ? String(localized: String.LocalizationValue("cast_following"), table: "Localizable")
                                : String(localized: String.LocalizationValue("cast_follow"), table: "Localizable")
                            )
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(isFollowing ? ConCafeColors.primary : .white)
                            .padding(.horizontal, 18)
                            .frame(height: 40)
                            .background(
                                Capsule()
                                    .fill(isFollowing ? ConCafeColors.primaryContainer : ConCafeColors.primary)
                            )
                        }
                        .disabled(isSelfCast)
                        .opacity(isSelfCast ? 0.5 : 1.0)
                    }
                    .zIndex(1)
                }
            }
            HStack(spacing: 18) {
                statItem(
                    systemName: "person.2.fill",
                    label: String(localized: String.LocalizationValue("cast_follower_label"), table: "Localizable"),
                    value: String(
                        format: String(localized: String.LocalizationValue("cast_follower_count"), table: "Localizable"),
                        locale: Locale.current,
                        detail.cast.followerCount
                    )
                )
            }
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 22)
        .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
    }

    private func statItem(systemName: String, label: String, value: String) -> some View {
        HStack(spacing: 6) {
            Image(systemName: systemName)
                .foregroundStyle(ConCafeColors.primary)
            HStack(spacing: 4) {
                Text(label)
                    .font(.caption)
                    .foregroundStyle(Color.secondary)
                Text(value)
                    .font(.subheadline.weight(.semibold))
            }
        }
    }
}

private struct CastTodaySection: View {
    let detail: CastDetail

    var body: some View {
        let todaySchedule = CastScheduleAttendanceUtils.todaySchedule(from: detail.schedule)
        let attendanceStatus = CastScheduleAttendanceUtils.attendanceStatus(schedule: todaySchedule)
        let statusText = castAttendanceStatusText(attendanceStatus)
        let timeText = todaySchedule.map { "\($0.startTime) - \($0.endTime)" }
            ?? String(localized: String.LocalizationValue("cast_today_check_schedule"), table: "Localizable")

        VStack(alignment: .leading, spacing: 6) {
            Text(String(localized: String.LocalizationValue("cast_today_status_title"), table: "Localizable"))
                .foregroundStyle(Color.white.opacity(0.82))
            Text(statusText)
                .font(.title3.bold())
                .foregroundStyle(.white)
            Text(timeText)
                .foregroundStyle(Color.white.opacity(0.9))
        }
        .frame(maxWidth: .infinity, alignment: .center)
        .padding(.horizontal, 20)
        .padding(.vertical, 18)
        .background(
            LinearGradient(
                colors: [ConCafeColors.primary, ConCafeColors.secondaryContainer],
                startPoint: .leading,
                endPoint: .trailing
            )
        )
        .clipShape(RoundedRectangle(cornerRadius: 24))
        .padding(.horizontal, 16)
        .multilineTextAlignment(.center)
    }
}

private func castAttendanceStatusText(_ status: CastAttendanceStatus) -> String {
    switch status {
    case .upcoming:
        return String(localized: String.LocalizationValue("cast_today_upcoming"), table: "Localizable")
    case .onShift:
        return String(localized: String.LocalizationValue("cast_today_working"), table: "Localizable")
    case .completed:
        return String(localized: String.LocalizationValue("cast_today_finished"), table: "Localizable")
    default:
        return String(localized: String.LocalizationValue("cast_today_off"), table: "Localizable")
    }
}

private struct CastScheduleSection: View {
    let detail: CastDetail

    var body: some View {
        let weeklyStatus = weeklySchedule(from: detail.schedule)

        VStack(alignment: .leading, spacing: 12) {
            Label(String(localized: String.LocalizationValue("cast_schedule_title"), table: "Localizable"), systemImage: "calendar")
                .font(.headline)
                .foregroundStyle(Color(uiColor: .label))
            HStack(spacing: 8) {
                ForEach(weeklyStatus, id: \.dayLabel) { item in
                    CastScheduleCard(
                        dayLabel: item.dayLabel,
                        isWorking: item.isWorking
                    )
                }
            }
        }
        .padding(.horizontal, 16)
    }
}

private struct CastScheduleCard: View {
    let dayLabel: String

    let isWorking: Bool

    var body: some View {
        VStack(spacing: 4) {
            Text(dayLabel)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(isWorking ? Color.white : .primary)
            Text(
                isWorking
                ? String(localized: String.LocalizationValue("cast_schedule_work"), table: "Localizable")
                : String(localized: String.LocalizationValue("cast_schedule_off"), table: "Localizable")
            )
                .font(.caption)
                .foregroundStyle(isWorking ? Color.white.opacity(0.92) : .secondary)
        }
        .padding(.vertical, 14)
        .frame(maxWidth: .infinity)
        .background(isWorking ? ConCafeColors.primary : Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}

private struct CastIntroductionSection: View {
    let detail: CastDetail

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(String(localized: String.LocalizationValue("cast_section_intro"), table: "Localizable"))
                .font(.headline)
                .foregroundStyle(Color(uiColor: .label))
            Text(detail.cast.desc)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(18)
                .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
                .clipShape(RoundedRectangle(cornerRadius: 22))
                .foregroundStyle(.primary)
                .frame(maxWidth: .infinity)
        }
        .padding(.horizontal, 16)
    }
}

private struct CastRecentActivitySection: View {
    let detail: CastDetail

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(String(localized: String.LocalizationValue("cast_section_recent_activity"), table: "Localizable"))
                .font(.headline)
                .foregroundStyle(Color(uiColor: .label))
            HStack(spacing: 10) {
                CastActivityCard(
                    value: "\(detail.visitCertificationCount)",
                    label: String(localized: String.LocalizationValue("cast_activity_visit_cert"), table: "Localizable")
                )
                CastActivityCard(
                    value: "\(detail.cast.followerCount)",
                    label: String(localized: String.LocalizationValue("cast_activity_follower"), table: "Localizable")
                )
                CastActivityCard(
                    value: RatingUtils.formatOneDecimal(detail.cast.rating),
                    label: String(localized: String.LocalizationValue("cast_activity_rating"), table: "Localizable")
                )
            }
        }
        .padding(.horizontal, 16)
    }
}

private struct CastActivityCard: View {
    let value: String

    let label: String

    var body: some View {
        VStack(spacing: 4) {
            Text(value)
                .font(.title3.bold())
                .foregroundStyle(ConCafeColors.primary)
            Text(label)
                .font(.caption)
                .foregroundStyle(Color.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 18)
        .padding(.horizontal, 12)
        .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }
}

private struct CastRecentReviewSection: View {
    let reviews: [CastRecentReview]

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(String(localized: String.LocalizationValue("cast_section_tagged_reviews"), table: "Localizable"))
                .font(.headline)
                .foregroundStyle(Color(uiColor: .label))
            if reviews.isEmpty {
                CastRecentReviewEmptyView()
            } else {
                VStack(spacing: 10) {
                    ForEach(reviews, id: \.id) { review in
                        VStack(alignment: .leading, spacing: 8) {
                            HStack {
                                Text(review.userNickname)
                                    .font(.subheadline.weight(.semibold))
                                Text(RatingUtils.formatOneDecimal(review.rating))
                                    .font(.caption.weight(.semibold))
                                    .foregroundStyle(ConCafeColors.primary)
                                    .padding(.horizontal, 8)
                                    .padding(.vertical, 4)
                                    .background(ConCafeColors.primaryContainer.opacity(0.12))
                                    .clipShape(Capsule())
                                Spacer()
                                Text(review.createdDateLabel)
                                    .font(.caption)
                                    .foregroundStyle(Color.secondary)
                            }
                            if !review.taggedCastNames.isEmpty {
                                ScrollView(.horizontal, showsIndicators: false) {
                                    HStack(spacing: 6) {
                                        ForEach(review.taggedCastNames, id: \.self) { castName in
                                            Text(castName)
                                                .font(.caption2.weight(.semibold))
                                                .foregroundStyle(ConCafeColors.primary)
                                                .padding(.horizontal, 10)
                                                .padding(.vertical, 5)
                                                .background(ConCafeColors.primaryContainer.opacity(0.12))
                                                .clipShape(Capsule())
                                        }
                                    }
                                }
                            }
                            Text(review.content)
                                .font(.subheadline)
                                .foregroundStyle(.primary)
                                .frame(maxWidth: .infinity, alignment: .leading)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(16)
                        .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
                        .clipShape(RoundedRectangle(cornerRadius: 20))
                    }
                }
            }
        }
        .padding(.horizontal, 16)
    }
}

private struct CastRecentReviewEmptyView: View {
    var body: some View {
        VStack(spacing: 6) {
            Text(String(localized: String.LocalizationValue("cast_tagged_reviews_empty_title"), table: "Localizable"))
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(.primary)
            Text(String(localized: String.LocalizationValue("cast_tagged_reviews_empty_desc"), table: "Localizable"))
                .font(.caption)
                .foregroundStyle(Color.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 28)
        .padding(.horizontal, 16)
        .background(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }))
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }
}

private struct CastScrollOffsetPreferenceKey: PreferenceKey {
    static var defaultValue: CGFloat = 0

    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = nextValue()
    }
}

private struct WeeklyScheduleItem {
    let dayLabel: String
    let isWorking: Bool
}

private func weeklySchedule(from schedules: [CastSchedule]) -> [WeeklyScheduleItem] {
    let workingDays = Set(schedules.compactMap { TimeUtils.weekdayLabel(fromIsoDate: $0.date) })
    let orderedDays = [
        ("월", String(localized: String.LocalizationValue("cast_weekday_mon"), table: "Localizable")),
        ("화", String(localized: String.LocalizationValue("cast_weekday_tue"), table: "Localizable")),
        ("수", String(localized: String.LocalizationValue("cast_weekday_wed"), table: "Localizable")),
        ("목", String(localized: String.LocalizationValue("cast_weekday_thu"), table: "Localizable")),
        ("금", String(localized: String.LocalizationValue("cast_weekday_fri"), table: "Localizable")),
        ("토", String(localized: String.LocalizationValue("cast_weekday_sat"), table: "Localizable")),
        ("일", String(localized: String.LocalizationValue("cast_weekday_sun"), table: "Localizable"))
    ]
    return orderedDays.map { day in
        WeeklyScheduleItem(dayLabel: day.1, isWorking: workingDays.contains(day.0))
    }
}

private extension Array {
    subscript(safe index: Int) -> Element? {
        indices.contains(index) ? self[index] : nil
    }
}

private struct CastSummaryOffsetPreferenceKey: PreferenceKey {
    static var defaultValue: CGFloat = .greatestFiniteMagnitude

    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = nextValue()
    }
}

struct CastView_Previews: PreviewProvider {
    static var previews: some View {
        CastView(
            castId: "maid-1",
            onNavigationAction: { _ in }
        )
    }
}
