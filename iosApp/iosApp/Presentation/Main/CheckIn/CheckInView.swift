//
//  CheckInView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/05.
//

import SwiftUI
import Shared

struct CheckInView: View {
    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel = CheckInViewModel()

    var body: some View {
        ZStack {
            Color(hex: "FFFBFD")
                .ignoresSafeArea()
            Group {
                if viewModel.uiState.isLoading {
                    ProgressView()
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if viewModel.uiState.currentUser == nil {
                    CheckInGuestContentView(
                        uiState: viewModel.uiState,
                        onAction: viewModel.onAction
                    )
                } else {
                    CheckInUserContentView(
                        uiState: viewModel.uiState,
                        onAction: viewModel.onAction
                    )
                }
            }
        }
        .sheet(
            isPresented: Binding(
                get: { viewModel.uiState.isLoginPromptVisible },
                set: { presented in
                    if !presented {
                        viewModel.onAction(.dismissLoginPrompt)
                    }
                }
            )
        ) {
            CheckInLoginPromptSheet(onAction: viewModel.onAction)
        }
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
    }
}

private struct CheckInGuestContentView: View {
    let uiState: CheckInUiState

    let onAction: (CheckInAction) -> Void

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                CheckInMapSection(
                    currentLocationLabel: uiState.currentLocationLabel,
                    cafes: uiState.mapCafes,
                    onCafeTap: { onAction(.cafeTapped(id: $0)) },
                    onCheckInTap: { onAction(.checkInTapped) }
                )
                .padding(.top, 16)
                CheckInLoginPromotionSection(onAction: onAction)
                CheckInSectionTitle(title: "🔥 인기 메이드 카페", trailing: nil)
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 14) {
                        ForEach(uiState.popularCafes, id: \.id) { cafe in
                            CafeSummaryCard(
                                name: cafe.name,
                                rating: String(format: "%.1f", cafe.rating),
                                location: cafe.locationLabel,
                                thumbnailImage: nil,
                                trailingLabel: "체크인 \(cafe.checkInCount)",
                                onTap: { onAction(.cafeTapped(id: cafe.id)) }
                            )
                            .frame(width: 220)
                        }
                    }
                    .padding(.horizontal, 16)
                }
                CheckInSectionTitle(title: "☕ 오늘 인기 캐스트", trailing: nil)
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 12) {
                        ForEach(uiState.popularCasts, id: \.id) { cast in
                            CheckInCastCard(cast: cast) {
                                onAction(.castTapped(id: cast.id))
                            }
                        }
                    }
                    .padding(.horizontal, 16)
                }
                if let errorMessage = uiState.errorMessage {
                    Text(errorMessage)
                        .font(.caption)
                        .foregroundStyle(.red)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 16)
                }
            }
        }
    }
}

private struct CheckInUserContentView: View {
    let uiState: CheckInUiState

    let onAction: (CheckInAction) -> Void

    var body: some View {
        ScrollView {
            VStack(spacing: 10) {
                CheckInMapSection(
                    currentLocationLabel: uiState.currentLocationLabel,
                    cafes: uiState.mapCafes,
                    onCafeTap: { onAction(.cafeTapped(id: $0)) },
                    onCheckInTap: { onAction(.checkInTapped) }
                )
                .padding(.top, 16)
                CheckInPrimaryButton(title: "새 방문 체크인") {
                    onAction(.checkInTapped)
                }
                .padding(.horizontal, 64)
                .padding(.vertical, 12)
                CheckInSectionTitle(title: "오늘의 방문", trailing: "3월 9일")
                CheckInTodayVisitsRow(visits: uiState.todayVisits)
                Spacer()
                    .frame(height: 20)
                CheckInSectionTitle(title: "최근 타임라인", trailing: "🕘")
                CheckInTimelineList(visits: uiState.recentVisits)
            }
        }
    }
}

private struct CheckInMapSection: View {
    let currentLocationLabel: String

    let cafes: [CheckInCafeSummary]

    let onCafeTap: (String) -> Void

    let onCheckInTap: () -> Void

    private let markerPositions: [CGPoint] = [
        CGPoint(x: 0.18, y: 0.18),
        CGPoint(x: 0.82, y: 0.28),
        CGPoint(x: 0.22, y: 0.56),
        CGPoint(x: 0.50, y: 0.50),
        CGPoint(x: 0.80, y: 0.46),
        CGPoint(x: 0.36, y: 0.82)
    ]

    var body: some View {
        VStack(spacing: 14) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("주변 메이드카페 지도")
                        .font(.headline)
                        .fontWeight(.bold)
                    HStack(spacing: 4) {
                        Image(systemName: "mappin.and.ellipse")
                            .foregroundStyle(Color(hex: "EF6797"))
                        Text(currentLocationLabel)
                            .font(.caption)
                            .foregroundStyle(Color(hex: "7B7480"))
                    }
                }
                Spacer()
                Button("체크인", action: onCheckInTap)
                    .buttonStyle(.bordered)
                    .tint(Color(hex: "EF6797"))
            }
            GeometryReader { geometry in
                ZStack {
                    RoundedRectangle(cornerRadius: 24, style: .continuous)
                        .fill(
                            LinearGradient(
                                colors: [Color(hex: "FFDDEB"), Color(hex: "FFF5F9"), Color(hex: "FFE8F1")],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                    ForEach(Array(cafes.prefix(6).enumerated()), id: \.element.id) { index, cafe in
                        let position = markerPositions[index]

                        Button {
                            onCafeTap(cafe.id)
                        } label: {
                            HStack(spacing: 6) {
                                Circle()
                                    .fill(Color(hex: "EF6797"))
                                    .frame(width: 10, height: 10)
                                Text(cafe.name)
                                    .font(.caption.weight(.semibold))
                                    .foregroundStyle(Color(hex: "4E4750"))
                                    .lineLimit(1)
                            }
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(Color.white.opacity(0.96))
                            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                            .shadow(color: .black.opacity(0.08), radius: 8, y: 3)
                        }
                        .buttonStyle(.plain)
                        .position(
                            x: geometry.size.width * position.x,
                            y: geometry.size.height * position.y
                        )
                    }
                }
            }
            .frame(height: 240)
        }
        .padding(18)
        .background(
            LinearGradient(
                colors: [Color(hex: "FFF0F6"), Color(hex: "FFFAFC"), Color(hex: "FFF3F8")],
                startPoint: .top,
                endPoint: .bottom
            )
        )
        .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 28, style: .continuous)
                .stroke(Color.white.opacity(0.8), lineWidth: 1)
        )
        .padding(.horizontal, 16)
    }
}

private struct CheckInLoginPromotionSection: View {
    let onAction: (CheckInAction) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            VStack(alignment: .leading, spacing: 10) {
                HStack {
                    Image(systemName: "sparkles")
                        .foregroundStyle(.white)
                    Text("로그인하고 체크인을 시작해보세요")
                        .font(.headline.weight(.bold))
                        .foregroundStyle(.white)
                }
                Text("방문 기록 저장, 팬 레벨, 배지 획득 기능을 사용할 수 있습니다.")
                    .font(.caption)
                    .foregroundStyle(.white.opacity(0.9))
                VStack(alignment: .leading, spacing: 6) {
                    Text("• 방문 기록 저장").foregroundStyle(.white)
                    Text("• 체크인 히스토리 관리").foregroundStyle(.white)
                    Text("• 배지와 활동 기록 누적").foregroundStyle(.white)
                }
                .font(.caption)
            }
            HStack(spacing: 10) {
                Button("로그인") {
                    onAction(.signInTapped)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 10)
                .background(Color.white)
                .foregroundStyle(Color(hex: "EF6797"))
                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                Button("회원가입") {
                    onAction(.signUpTapped)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 10)
                .background(Color.white.opacity(0.18))
                .foregroundStyle(.white)
                .overlay(
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .stroke(Color.white.opacity(0.4), lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            }
            .font(.subheadline.weight(.bold))
        }
        .padding(18)
        .background(
            LinearGradient(
                colors: [Color(hex: "EF6797"), Color(hex: "F7A0C1")],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .padding(.horizontal, 16)
    }
}

private struct CheckInCastCard: View {
    let cast: CheckInCastSummary

    let onTap: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 12) {
                Circle()
                    .fill(
                        LinearGradient(
                            colors: [Color(hex: "FFD1E2"), Color(hex: "FFEAF2")],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .frame(width: 56, height: 56)
                    .overlay(
                        Text(String(cast.name.prefix(1)))
                            .font(.system(size: 20, weight: .bold))
                            .foregroundStyle(Color(hex: "B74C72"))
                    )

                VStack(alignment: .leading, spacing: 4) {
                    Text(cast.name)
                        .font(.subheadline.weight(.bold))
                        .foregroundStyle(Color.primary)
                        .lineLimit(1)
                    Text(cast.cafeName)
                        .font(.caption)
                        .foregroundStyle(Color(hex: "7A7380"))
                        .lineLimit(1)
                }
            }

            HStack {
                Text("오늘 방문 \(cast.todayVisit)")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Color(hex: "EF6797"))
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(Color(hex: "FFEEF5"))
                    .clipShape(Capsule())
                Spacer(minLength: 0)
            }
        }
        .padding(16)
        .frame(width: 200, alignment: .leading)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
        .shadow(color: .black.opacity(0.03), radius: 8, y: 3)
        .contentShape(Rectangle())
        .onTapGesture(perform: onTap)
    }
}

private struct CheckInTodayVisitsRow: View {
    let visits: [CheckInVisitEntry]

    var body: some View {
        if visits.isEmpty {
            CheckInEmptyState(
                title: "오늘 방문 기록이 아직 없어요",
                description: "지금 체크인하고 첫 방문 기록을 남겨보세요."
            )
        } else {
            HStack(spacing: 12) {
                switch visits.count {
                case 1:
                    CheckInVisitCard(name: visits[0].cafeName, time: visits[0].visitedLabel)
                    Spacer(minLength: 0)
                default:
                    CheckInVisitCard(name: visits[0].cafeName, time: visits[0].visitedLabel)
                    CheckInMoreVisitCard(remainingCount: visits.count - 1)
                }
            }
            .padding(.horizontal, 16)
        }
    }
}

private struct CheckInVisitCard: View {
    let name: String

    let time: String

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .fill(
                    LinearGradient(
                        colors: [Color(hex: "FFE2D2"), Color(hex: "FFC9A9")],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                )

            VStack(alignment: .leading, spacing: 4) {
                Text(name)
                    .font(.subheadline.weight(.bold))
                    .foregroundStyle(.white)
                    .lineLimit(2)
                Text(time)
                    .font(.caption)
                    .foregroundStyle(Color.white.opacity(0.8))
                    .lineLimit(1)
            }
            .padding(16)
        }
        .frame(maxWidth: .infinity)
        .frame(height: 200)
    }
}

private struct CheckInMoreVisitCard: View {
    let remainingCount: Int

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .fill(
                    LinearGradient(
                        colors: [Color(hex: "FFF1F6"), Color(hex: "FFE1EC")],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                )

            VStack(spacing: 6) {
                Text("+\(remainingCount)")
                    .font(.system(size: 28, weight: .bold))
                    .foregroundStyle(Color(hex: "EF6797"))
                Text("더 방문했어요")
                    .font(.caption)
                    .foregroundStyle(Color(hex: "7C7480"))
            }
        }
        .frame(maxWidth: .infinity)
        .frame(height: 200)
    }
}

private struct CheckInPrimaryButton: View {
    let title: String

    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                Image(systemName: "plus")
                Text(title)
                    .fontWeight(.bold)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(Color(hex: "EF6797"))
            .foregroundStyle(.white)
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        }
        .buttonStyle(.plain)
    }
}

private struct CheckInTimelineList: View {
    let visits: [CheckInVisitEntry]

    var body: some View {
        if visits.isEmpty {
            CheckInEmptyState(
                title: "최근 타임라인이 비어 있어요",
                description: "체크인한 방문 기록이 이 영역에 시간순으로 표시됩니다."
            )
        } else {
            VStack(spacing: 12) {
                ForEach(Array(visits.enumerated()), id: \.element.id) { index, visit in
                    CheckInTimelineItem(
                        visit: visit,
                        showsConnector: index < visits.count - 1
                    )
                }
            }
            .padding(.horizontal, 16)
        }
    }
}

private struct CheckInTimelineItem: View {
    let visit: CheckInVisitEntry

    let showsConnector: Bool

    var body: some View {
        HStack(alignment: .top, spacing: 16) {
            VStack(spacing: 0) {
                Circle()
                    .fill(Color.white)
                    .frame(width: 32, height: 32)
                    .overlay(
                        Circle()
                            .stroke(Color(hex: "F6BCD1"), lineWidth: 1)
                    )
                    .overlay(
                        Image(systemName: "mappin")
                            .foregroundStyle(Color(hex: "4E4750"))
                    )
                if showsConnector {
                    Rectangle()
                        .fill(Color(hex: "F6BCD1").opacity(0.3))
                        .frame(width: 2, height: 100)
                }
            }
            VStack(alignment: .leading, spacing: 8) {
                HStack(alignment: .top) {
                    Text(visit.cafeName)
                        .font(.subheadline.weight(.bold))
                        .foregroundStyle(Color(hex: "4E4750"))
                    Spacer(minLength: 8)
                    Text(visit.relativeVisitedLabel)
                        .font(.caption)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(Color(hex: "F5F5F5"))
                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                }
                Text(visit.memo ?? "방문 메모 없음")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            .shadow(color: .black.opacity(0.03), radius: 8, y: 3)
            .padding(.bottom, 24)
        }
    }
}

private struct CheckInEmptyState: View {
    let title: String

    let description: String

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title)
                .font(.headline)
                .fontWeight(.bold)
            Text(description)
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white)
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .padding(.horizontal, 16)
    }
}

private struct CheckInSectionTitle: View {
    let title: String

    let trailing: String?

    var body: some View {
        HStack {
            Text(title)
                .font(.headline.weight(.bold))
            Spacer()
            if let trailing {
                Text(trailing)
                    .font(.caption)
                    .foregroundStyle(Color(hex: "7B7480"))
            }
        }
        .padding(.horizontal, 16)
    }
}

private struct CheckInLoginPromptSheet: View {
    let onAction: (CheckInAction) -> Void

    var body: some View {
        VStack(spacing: 18) {
            RoundedRectangle(cornerRadius: 3, style: .continuous)
                .fill(Color(hex: "E1D7DE"))
                .frame(width: 42, height: 5)
                .padding(.top, 8)
            VStack(spacing: 10) {
                Text("로그인이 필요합니다")
                    .font(.headline.weight(.bold))
                Text("로그인 후 방문 기록 저장, 팬 레벨, 배지 획득 기능을 사용할 수 있습니다.")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 12)
            }
            HStack(spacing: 10) {
                Button("로그인") {
                    onAction(.signInTapped)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
                .background(Color(hex: "EF6797"))
                .foregroundStyle(.white)
                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                Button("회원가입") {
                    onAction(.signUpTapped)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
                .background(Color(hex: "FFF1F6"))
                .foregroundStyle(Color(hex: "D44F82"))
                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            }
            .font(.subheadline.weight(.bold))
            Spacer()
        }
        .padding(.horizontal, 20)
        .padding(.bottom, 24)
        .background(Color.white)
    }
}

private extension CheckInVisitEntry {
    var relativeVisitedLabel: String {
        guard
            let referenceEpochDay = Self.toEpochDay("2026-03-09"),
            let visitedEpochDay = Self.toEpochDay(String(visitedAt.prefix(10)))
        else {
            return visitedLabel
        }

        let daysAgo = referenceEpochDay - visitedEpochDay

        switch daysAgo {
        case ..<0:
            return visitedLabel
        case 0:
            return "오늘"
        case 1:
            return "어제"
        default:
            return "\(daysAgo)일 전"
        }
    }

    private static func toEpochDay(_ value: String) -> Int? {
        let parts = value.split(separator: "-")
        guard parts.count == 3,
              let year = Int(parts[0]),
              let month = Int(parts[1]),
              let day = Int(parts[2]),
              (1...12).contains(month),
              (1...31).contains(day) else {
            return nil
        }

        let adjustedYear = year - (month <= 2 ? 1 : 0)
        let era = adjustedYear >= 0 ? adjustedYear / 400 : (adjustedYear - 399) / 400
        let yearOfEra = adjustedYear - era * 400
        let adjustedMonth = month + (month > 2 ? -3 : 9)
        let dayOfYear = (153 * adjustedMonth + 2) / 5 + day - 1
        let dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear

        return era * 146097 + dayOfEra - 719468
    }
}

struct CheckInView_Previews: PreviewProvider {
    static var previews: some View {
        CheckInView(onNavigationAction: { _ in })
    }
}
