//
//  CastView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/05.
//

import SwiftUI
import Shared

private let castCurrentDate = "2026-03-08"
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
                    .background(Color(hex: "FFF9FC"))
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
                    CastSummarySection(detail: detail, isFollowing: uiState.isFollowing, onAction: onAction)
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
            .onPreferenceChange(CastSummaryOffsetPreferenceKey.self) { value in
                summarySectionMinY = value
            }
        } else if uiState.isLoading {
            ProgressView()
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else {
            VStack(spacing: 12) {
                Text(uiState.errorMessage ?? "캐스트 상세 데이터를 불러오지 못했습니다.")
                    .foregroundStyle(.red)
                Button("새로고침") {
                    onAction(.refresh)
                }
                .buttonStyle(.borderedProminent)
                .tint(Color(hex: "EF6797"))
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

    var body: some View {
        let stretchScale = scrollOffset > 0 ? 1 + (scrollOffset / 700) : 1
        let heroImages = resolveHeroImages(
            images: detail.images,
            fallbackProfileImage: detail.cast.profileImage
        )

        TabView {
            ForEach(Array(heroImages.enumerated()), id: \.offset) { index, image in
                ZStack {
                    let trimmed = image.trimmingCharacters(in: .whitespacesAndNewlines)

                    if let url = URL(string: trimmed), !trimmed.isEmpty {
                        GeometryReader { geometry in
                            CachedAsyncImage(
                                url: url,
                                placeholder: heroPlaceholder(index: index)
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
                .scaleEffect(stretchScale, anchor: .top)
                .clipped()
            }
        }
        .frame(height: 230 + topSafeArea)
        .background(Color.black)
        .clipShape(Rectangle())
        .tabViewStyle(.page(indexDisplayMode: .automatic))
    }

    @ViewBuilder
    private func heroPlaceholder(index: Int) -> some View {
        let gradients: [[Color]] = [
            [Color(hex: "F8A3C5"), Color(hex: "EF6797")],
            [Color(hex: "FFC6C7"), Color(hex: "FF8E9E")],
            [Color(hex: "F8D6E9"), Color(hex: "D98AB7")]
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

    let onAction: (CastAction) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top, spacing: 12) {
                VStack(alignment: .leading, spacing: 8) {
                    Text(detail.cast.name)
                        .font(.title2.bold())
                    Text(detail.cast.conceptRole.capitalized)
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(Color(hex: "C9527E"))
                        .padding(.horizontal, 10)
                        .padding(.vertical, 5)
                        .background(Capsule().fill(Color(hex: "FFE7F1")))
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
                Button {
                    onAction(.followTapped)
                } label: {
                    Text(isFollowing ? "팔로잉" : "팔로우")
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(isFollowing ? Color(hex: "6A4960") : .white)
                        .padding(.horizontal, 18)
                        .frame(height: 40)
                        .background(
                            Capsule()
                                .fill(isFollowing ? Color(hex: "F1E3EB") : Color(hex: "EF6797"))
                        )
                }
            }
            HStack(spacing: 18) {
                statItem(systemName: "person.2.fill", label: "팔로워", value: "\(detail.cast.followerCount)명")
            }
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 22)
        .background(Color.white)
    }

    private func statItem(systemName: String, label: String, value: String) -> some View {
        HStack(spacing: 6) {
            Image(systemName: systemName)
                .foregroundStyle(Color(hex: "EF6797"))
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
        let todaySchedule = detail.schedule.first(where: { $0.date == castCurrentDate })

        VStack(alignment: .leading, spacing: 6) {
            Text("오늘의 출근 상태")
                .foregroundStyle(Color.white.opacity(0.82))
            Text(todaySchedule != nil ? "출근 예정" : "오늘은 휴무")
                .font(.title3.bold())
                .foregroundStyle(.white)
            Text(todaySchedule.map { "\($0.startTime) - \($0.endTime)" } ?? "다음 스케줄을 확인해 주세요.")
                .foregroundStyle(Color.white.opacity(0.9))
        }
        .frame(maxWidth: .infinity, alignment: .center)
        .padding(.horizontal, 20)
        .padding(.vertical, 18)
        .background(
            LinearGradient(
                colors: [Color(hex: "EF6797"), Color(hex: "F8A3C5")],
                startPoint: .leading,
                endPoint: .trailing
            )
        )
        .clipShape(RoundedRectangle(cornerRadius: 24))
        .padding(.horizontal, 16)
        .multilineTextAlignment(.center)
    }
}

private struct CastScheduleSection: View {
    let detail: CastDetail

    var body: some View {
        let weeklyStatus = weeklySchedule(from: detail.schedule)

        VStack(alignment: .leading, spacing: 12) {
            Label("출근 일정", systemImage: "calendar")
                .font(.headline)
                .foregroundStyle(Color.primary)
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
                .foregroundStyle(isWorking ? Color.white : Color(hex: "4E4750"))
            Text(isWorking ? "출근" : "휴무")
                .font(.caption)
                .foregroundStyle(isWorking ? Color.white.opacity(0.92) : Color(hex: "8A8087"))
        }
        .padding(.vertical, 14)
        .frame(maxWidth: .infinity)
        .background(isWorking ? Color(hex: "EF6797") : Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}

private struct CastIntroductionSection: View {
    let detail: CastDetail

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("소개")
                .font(.headline)
            Text(detail.cast.desc)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(18)
                .background(Color.white)
                .clipShape(RoundedRectangle(cornerRadius: 22))
                .foregroundStyle(Color(hex: "4E4750"))
                .frame(maxWidth: .infinity)
        }
        .padding(.horizontal, 16)
    }
}

private struct CastRecentActivitySection: View {
    let detail: CastDetail

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("최근 활동")
                .font(.headline)
            HStack(spacing: 10) {
                CastActivityCard(value: "\(detail.visitCertificationCount)", label: "방문 인증")
                CastActivityCard(value: "\(detail.cast.followerCount)", label: "팔로워")
                CastActivityCard(value: String(format: "%.1f", detail.cast.rating), label: "평점")
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
                .foregroundStyle(Color(hex: "EF6797"))
            Text(label)
                .font(.caption)
                .foregroundStyle(Color.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 18)
        .padding(.horizontal, 12)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }
}

private struct CastRecentReviewSection: View {
    let reviews: [CastRecentReview]

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("함께 언급된 후기")
                .font(.headline)
            if reviews.isEmpty {
                CastRecentReviewEmptyView()
            } else {
                VStack(spacing: 10) {
                    ForEach(reviews, id: \.id) { review in
                        VStack(alignment: .leading, spacing: 8) {
                            HStack {
                                Text(review.userNickname)
                                    .font(.subheadline.weight(.semibold))
                                Text("\(review.rating)")
                                    .font(.caption.weight(.semibold))
                                    .foregroundStyle(Color(hex: "EF6797"))
                                    .padding(.horizontal, 8)
                                    .padding(.vertical, 4)
                                    .background(Color(hex: "FFD1DC").opacity(0.12))
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
                                                .foregroundStyle(Color(hex: "C9527E"))
                                                .padding(.horizontal, 10)
                                                .padding(.vertical, 5)
                                                .background(Color(hex: "FFD1DC").opacity(0.12))
                                                .clipShape(Capsule())
                                        }
                                    }
                                }
                            }
                            Text(review.content)
                                .font(.subheadline)
                                .foregroundStyle(Color(hex: "4E4750"))
                                .frame(maxWidth: .infinity, alignment: .leading)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(16)
                        .background(Color.white)
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
            Text("아직 함께 언급된 후기가 없어요.")
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Color(hex: "4E4750"))
            Text("이 캐스트가 태그된 카페 리뷰가 표시됩니다.")
                .font(.caption)
                .foregroundStyle(Color.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 28)
        .padding(.horizontal, 16)
        .background(Color.white)
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
    let orderedDays = ["월", "화", "수", "목", "금", "토", "일"]
    return orderedDays.map { dayLabel in
        WeeklyScheduleItem(dayLabel: dayLabel, isWorking: workingDays.contains(dayLabel))
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
