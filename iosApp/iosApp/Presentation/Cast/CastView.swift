//
//  CastView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/05.
//

import SwiftUI
import Shared

private let castCurrentDate = "2026-03-08"
private let castHeroHeight: CGFloat = 340
private let castSummaryTitleTriggerOffset: CGFloat = 22

struct CastView: View {
    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel: CastViewModel

    var body: some View {
        CastContentView(
            uiState: viewModel.uiState,
            onAction: viewModel.onAction
        )
        .castNavigationBarHidden()
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
                CastNavigationBar(
                    title: uiState.detail?.cast.name ?? "",
                    isVisible: navBarVisible,
                    topSafeArea: proxy.safeAreaInsets.top,
                    onBack: { onAction(.backTapped) }
                )
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
            ZStack(alignment: .top) {
                CastHeroSection(
                    detail: detail,
                    scrollOffset: scrollOffset,
                    topSafeArea: topSafeArea
                )
                ScrollView {
                    offsetReader
                    LazyVStack(spacing: 18) {
                        Color.clear
                            .frame(height: castHeroHeight + topSafeArea)
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
                .castScrollIndicatorsHidden()
                .coordinateSpace(name: "castScroll")
                .onPreferenceChange(CastSummaryOffsetPreferenceKey.self) { value in
                    summarySectionMinY = value
                }
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

private struct CastNavigationBar: View {
    let title: String

    let isVisible: Bool

    let topSafeArea: CGFloat

    let onBack: () -> Void

    var body: some View {
        let backgroundOpacity = isVisible ? 1.0 : 0.0
        let foregroundColor = isVisible ? Color.black : Color.white

        VStack(spacing: 0) {
            Color.clear.frame(height: topSafeArea)
            HStack(spacing: 12) {
                Button(action: onBack) {
                    Image(systemName: "chevron.left")
                        .font(.headline)
                        .foregroundStyle(foregroundColor)
                        .frame(width: 36, height: 36)
                        .background(
                            Circle()
                                .fill(isVisible ? Color.clear : Color.white.opacity(0.16))
                        )
                }
                Text(isVisible ? title : "")
                    .font(.headline.weight(.semibold))
                    .foregroundStyle(Color.black)
                    .lineLimit(1)
                Spacer()
            }
            .padding(.horizontal, 16)
            .frame(height: 52)
            .background(Color.white.opacity(backgroundOpacity))
            Divider()
                .opacity(isVisible ? 1 : 0)
        }
    }
}

private struct CastHeroSection: View {
    let detail: CastDetail

    let scrollOffset: CGFloat

    let topSafeArea: CGFloat

    var body: some View {
        let upwardScroll = min(scrollOffset, 0)
        let downwardScroll = max(scrollOffset, 0)
        let parallaxOffset = -upwardScroll * 0.35

        TabView {
            ForEach(Array(detail.images.enumerated()), id: \.offset) { index, image in
                ZStack {
                    if let url = URL(string: image), !image.isEmpty {
                        AsyncImage(url: url) { phase in
                            switch phase {
                            case .empty:
                                heroPlaceholder(index: index)
                            case .success(let loadedImage):
                                loadedImage
                                    .resizable()
                                    .scaledToFill()
                            case .failure:
                                heroPlaceholder(index: index)
                            @unknown default:
                                heroPlaceholder(index: index)
                            }
                        }
                    } else {
                        heroPlaceholder(index: index)
                    }
                    LinearGradient(
                        colors: [.clear, Color.black.opacity(0.52)],
                        startPoint: .top,
                        endPoint: .bottom
                    )
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
                    Text(detail.cafe.name)
                        .font(.subheadline.weight(.medium))
                        .foregroundStyle(Color.white.opacity(0.9))
                        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomLeading)
                        .padding(20)
                }
                .offset(y: parallaxOffset - downwardScroll)
                .clipped()
            }
        }
        .frame(height: castHeroHeight + topSafeArea + downwardScroll)
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
                CastActivityCard(value: "\(detail.recentVisitCount)", label: "방문 인증")
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
            Text("최근 방문 후기")
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
                                Spacer()
                                Text(review.createdDateLabel)
                                    .font(.caption)
                                    .foregroundStyle(Color.secondary)
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
            Text("아직 방문 후기가 없어요.")
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Color(hex: "4E4750"))
            Text("첫 후기를 기다리고 있어요.")
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

private extension View {
    @ViewBuilder
    func castNavigationBarHidden() -> some View {
        if #available(iOS 16.0, *) {
            self.toolbar(.hidden, for: .navigationBar)
        } else {
            self
                .navigationBarBackButtonHidden(true)
                .navigationBarHidden(true)
        }
    }

    @ViewBuilder
    func castScrollIndicatorsHidden() -> some View {
        if #available(iOS 16.0, *) {
            self.scrollIndicators(.hidden)
        } else {
            self
        }
    }
}

private extension CastDetail {
    var recentVisitCount: Int {
        max(Int(cast.followerCount) / 8, schedule.count)
    }
}

private struct WeeklyScheduleItem {
    let dayLabel: String
    let isWorking: Bool
}

private func weeklySchedule(from schedules: [CastSchedule]) -> [WeeklyScheduleItem] {
    let workingDays = Set(schedules.compactMap { weekdayLabel(from: $0.date) })
    let orderedDays = ["월", "화", "수", "목", "금", "토", "일"]
    return orderedDays.map { dayLabel in
        WeeklyScheduleItem(dayLabel: dayLabel, isWorking: workingDays.contains(dayLabel))
    }
}

private func weekdayLabel(from date: String) -> String? {
    let parts = date.split(separator: "-")
    guard parts.count == 3,
          let year = Int(parts[0]),
          let month = Int(parts[1]),
          let day = Int(parts[2]) else {
        return nil
    }
    let orderedDays = ["월", "화", "수", "목", "금", "토", "일"]
    return orderedDays[safe: dayOfWeekIndex(year: year, month: month, day: day)]
}

private func dayOfWeekIndex(year: Int, month: Int, day: Int) -> Int {
    var adjustedYear = year
    var adjustedMonth = month
    if adjustedMonth < 3 {
        adjustedMonth += 12
        adjustedYear -= 1
    }
    let k = adjustedYear % 100
    let j = adjustedYear / 100
    let h = (day + (13 * (adjustedMonth + 1)) / 5 + k + (k / 4) + (j / 4) + (5 * j)) % 7
    switch h {
    case 2: return 0
    case 3: return 1
    case 4: return 2
    case 5: return 3
    case 6: return 4
    case 0: return 5
    default: return 6
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
