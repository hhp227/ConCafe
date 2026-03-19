//
//  FanManagementView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import SwiftUI
import Shared

struct FanManagementView: View {
    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel = FanManagementViewModel()

    @State private var alertMessage: String?

    var body: some View {
        FanManagementContentView(
            uiState: viewModel.uiState,
            onAction: viewModel.onAction
        )
        .navigationTitle("팬 관리")
        .navigationBarTitleDisplayMode(.inline)
        .onReceive(viewModel.event) { event in
            switch event {
            case .showMessage(let message):
                alertMessage = message
            case .navigateToCastEdit(let cafeId, let castId):
                onNavigationAction(.navigateToCastEdit(cafeId: cafeId, castId: castId))
            case .navigateToSchedule(let castId):
                onNavigationAction(.navigateToSchedule(castId: castId))
            }
        }
        .sheet(
            isPresented: Binding(
                get: { viewModel.uiState.isClaimSheetVisible && viewModel.uiState.castClaimSheet != nil },
                set: { isPresented in
                    if !isPresented {
                        viewModel.onAction(.dismissClaimSheet)
                    }
                }
            )
        ) {
            if let sheet = viewModel.uiState.castClaimSheet {
                CastClaimSheetView(sheet: sheet, onAction: viewModel.onAction)
                    .compatLargeSheetDetent()
            }
        }
        .alert(
            "안내",
            isPresented: Binding(
                get: { alertMessage != nil },
                set: { isPresented in
                    if !isPresented {
                        alertMessage = nil
                    }
                }
            ),
            presenting: alertMessage
        ) { _ in
            Button("확인", role: .cancel) {
                alertMessage = nil
            }
        } message: { message in
            Text(message)
        }
    }
}

private struct FanManagementContentView: View {
    let uiState: FanManagementUiState

    let onAction: (FanManagementAction) -> Void

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                if uiState.isLoading {
                    loadingState
                } else if uiState.fanManagementData == nil && uiState.castClaimStatus == nil {
                    emptySectionCard(message: uiState.errorMessage ?? "로그인한 캐스트 정보를 찾을 수 없습니다.")
                }
                if let infoMessage = uiState.infoMessage {
                    infoBanner(message: infoMessage)
                }
                if let claimStatus = uiState.castClaimStatus {
                    castClaimStatusCard(claimStatus)
                }
                primaryAnnouncementButton
                quickActionGrid
                weeklyScheduleSection
                recentFollowersSection
            }
            .padding(.horizontal, 16)
            .padding(.top, 16)
        }
        .background(
            LinearGradient(
                colors: [Color(hex: "F8F5F6"), Color(hex: "FFF8FB"), Color(hex: "FFEFF5")],
                startPoint: .top,
                endPoint: .bottom
            )
        )
    }

    private func infoBanner(message: String) -> some View {
        HStack(spacing: 10) {
            Text(message)
                .font(.caption)
                .foregroundStyle(Color(hex: "6B5320"))
                .frame(maxWidth: .infinity, alignment: .leading)
            Button("닫기") {
                onAction(.dismissInfoMessage)
            }
            .font(.caption.weight(.bold))
            .foregroundStyle(Color(hex: "6B5320"))
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(Color(hex: "FFF6D7"))
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(Color(hex: "F1D88D"), lineWidth: 1)
        )
    }

    private var primaryAnnouncementButton: some View {
        Button {
            onAction(.clickPrimaryAnnouncement)
        } label: {
            HStack {
                HStack(spacing: 12) {
                    Image(systemName: "megaphone.fill")
                    Text("팬 공지 작성하기")
                        .font(.headline.weight(.bold))
                }
                Spacer()
                Image(systemName: "chevron.right")
            }
            .foregroundStyle(Color(hex: "24161E"))
            .padding(.horizontal, 18)
            .padding(.vertical, 16)
            .background(Color(hex: "FFD1DC"))
            .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
        }
        .buttonStyle(.plain)
    }

    private func castClaimStatusCard(_ status: FanManagementUiState.CastClaimStatusCard) -> some View {
        let accentBackground: Color = {
            switch status.accent {
            case .pending: return Color(hex: "FFD1DC")
            case .linked: return Color(hex: "EAF8EF")
            case .rejected: return Color(hex: "F8E9EE")
            }
        }()
        let accentForeground: Color = {
            switch status.accent {
            case .pending: return Color(hex: "6B3050")
            case .linked: return Color(hex: "2E8B57")
            case .rejected: return Color(hex: "8B4A5A")
            }
        }()
        return Button {
            onAction(.clickClaimProfile)
        } label: {
            VStack(alignment: .leading, spacing: 10) {
                Text(status.affiliatedCafeName)
                    .font(.caption.weight(.bold))
                    .foregroundStyle(accentForeground)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background(accentBackground)
                    .clipShape(Capsule())
                Text(status.headline)
                    .font(.headline.weight(.bold))
                    .foregroundStyle(Color(hex: "24161E"))
                Text(status.body)
                    .font(.subheadline)
                    .foregroundStyle(Color(hex: "6C6270"))
                    .fixedSize(horizontal: false, vertical: true)
                HStack {
                    Text("프로필 연결 상태 보기")
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(Color(hex: "EF6797"))
                    Spacer()
                    Image(systemName: "chevron.right")
                        .foregroundStyle(Color(hex: "EF6797"))
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(18)
            .background(Color.white.opacity(0.95))
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        }
        .buttonStyle(.plain)
    }

    private var quickActionGrid: some View {
        HStack(spacing: 12) {
            ForEach(FanManagementUiState.QuickAction.allCases, id: \.self) { quickAction in
                let iconName = quickAction == .workSchedule ? "calendar" : "storefront"
                Button {
                    onAction(.clickQuickAction(quickAction))
                } label: {
                    ZStack(alignment: .topTrailing) {
                        VStack(spacing: 10) {
                            RoundedRectangle(cornerRadius: 14, style: .continuous)
                                .fill(Color(hex: "FFD1DC").opacity(0.08))
                                .frame(width: 40, height: 40)
                                .overlay {
                                    Image(systemName: iconName)
                                        .foregroundStyle(Color(hex: "5D525B"))
                                }
                            Text(quickAction.title)
                                .font(.subheadline.weight(.semibold))
                                .foregroundStyle(Color(hex: "24161E"))
                            Text(quickAction.subtitle)
                                .font(.caption)
                                .foregroundStyle(Color(hex: "7A707A"))
                                .multilineTextAlignment(.center)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 18)
                        .background(Color.white.opacity(0.92))
                        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
                        .overlay(
                            RoundedRectangle(cornerRadius: 20, style: .continuous)
                                .stroke(Color(hex: "FFD1DC").opacity(0.16), lineWidth: 1)
                        )
                    }
                    .frame(maxWidth: .infinity)
                }
                .buttonStyle(.plain)
            }
        }
    }

    private var recentFollowersSection: some View {
        let followers = Array((uiState.fanManagementData?.followers ?? []).prefix(10))
        return sectionContainer(title: "최근 팔로워") {
            if followers.isEmpty {
                emptySectionCard(message: "최근 팔로워 데이터가 아직 없습니다.")
            } else {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 14) {
                        ForEach(Array(followers.enumerated()), id: \.element.id) { index, follower in
                            let accent = index == 0
                            let joinedLabel: String = {
                                switch index {
                                case 0: return "방금 전"
                                case 1: return "2시간 전"
                                case 2: return "5시간 전"
                                default: return "최근"
                                }
                            }()
                            Button {
                                onAction(.clickRecentFollower(id: follower.id))
                            } label: {
                                VStack(spacing: 8) {
                                    Circle()
                                        .fill(
                                            LinearGradient(
                                                colors: accent ? [Color(hex: "FFD7E5"), Color(hex: "F2ADC2")] : [Color(hex: "F2EEF1"), Color(hex: "E3D9E2")],
                                                startPoint: .topLeading,
                                                endPoint: .bottomTrailing
                                            )
                                        )
                                        .frame(width: 58, height: 58)
                                        .overlay {
                                            Text(String(follower.nickname.prefix(1)).uppercased())
                                                .font(.headline.weight(.bold))
                                                .foregroundStyle(Color(hex: "6E5566"))
                                        }
                                        .overlay(
                                            Circle().stroke(accent ? Color(hex: "FFD1DC") : .clear, lineWidth: 2)
                                        )
                                    Text(follower.nickname)
                                        .font(.caption.weight(.medium))
                                        .foregroundStyle(Color(hex: "24161E"))
                                    Text(joinedLabel)
                                        .font(.caption2)
                                        .foregroundStyle(Color(hex: "9C8C98"))
                                }
                                .frame(width: 74)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
            }
        }
    }

    private var weeklyScheduleSection: some View {
        let weeklyStatus = weeklySchedule(from: uiState.fanManagementData?.detail.schedule ?? [])

        return sectionContainer(title: "주간 출근") {
            VStack(alignment: .leading, spacing: 14) {
                HStack(spacing: 8) {
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .fill(Color(hex: "FFD1DC").opacity(0.08))
                        .frame(width: 34, height: 34)
                        .overlay {
                            Image(systemName: "calendar")
                                .foregroundStyle(Color(hex: "EF6797"))
                        }
                    VStack(alignment: .leading, spacing: 2) {
                        Text("이번 주 스케줄")
                            .font(.subheadline.weight(.bold))
                            .foregroundStyle(Color(hex: "24161E"))
                        Text("출근 관리에서 일정을 바로 조정할 수 있습니다.")
                            .font(.caption)
                            .foregroundStyle(Color(hex: "7A707A"))
                    }
                }
                HStack(spacing: 8) {
                    ForEach(weeklyStatus, id: \.dayLabel) { item in
                        weeklyScheduleCard(item)
                    }
                }
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 16)
            .background(Color.white.opacity(0.88))
            .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 22, style: .continuous)
                    .stroke(Color(hex: "FFD1DC").opacity(0.10), lineWidth: 1)
            )
        }
    }

    private func weeklyScheduleCard(_ item: WeeklyScheduleItem) -> some View {
        VStack(spacing: 4) {
            Text(item.dayLabel)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(item.isWorking ? Color.white : Color(hex: "4E4750"))
            Text(item.isWorking ? "출근" : "휴무")
                .font(.caption)
                .foregroundStyle(item.isWorking ? Color.white.opacity(0.92) : Color(hex: "8A8087"))
        }
        .padding(.vertical, 14)
        .frame(maxWidth: .infinity)
        .background(item.isWorking ? Color(hex: "EF6797") : Color(hex: "FDF8FA"))
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(item.isWorking ? .clear : Color(hex: "FFD1DC").opacity(0.10), lineWidth: 1)
        )
    }

    private var loadingState: some View {
        emptySectionCard(message: "팬관리 정보를 불러오는 중입니다.")
    }

    private func emptySectionCard(message: String) -> some View {
        Text(message)
            .font(.subheadline)
            .foregroundStyle(Color(hex: "7A707A"))
            .frame(maxWidth: .infinity)
            .padding(.horizontal, 16)
            .padding(.vertical, 18)
            .background(Color.white.opacity(0.88))
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .stroke(Color(hex: "FFD1DC").opacity(0.16), lineWidth: 1)
            )
    }

    private func sectionContainer<Content: View>(
        title: String,
        actionLabel: String? = nil,
        action: (() -> Void)? = nil,
        @ViewBuilder content: () -> Content
    ) -> some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                Text(title)
                    .font(.title3.weight(.bold))
                    .foregroundStyle(Color(hex: "24161E"))
                Spacer()
                if let actionLabel, let action {
                    Button(actionLabel) {
                        action()
                    }
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(Color(hex: "7A707A"))
                } else if let actionLabel {
                    Text(actionLabel)
                        .font(.subheadline.weight(.medium))
                        .foregroundStyle(Color(hex: "7A707A"))
                }
            }
            content()
        }
    }

}

private struct CastClaimSheetView: View {
    let sheet: FanManagementUiState.CastClaimSheet
    
    let onAction: (FanManagementAction) -> Void

    var body: some View {
        CompatNavigationContainer(title: "프로필 연결") {
            VStack(spacing: 0) {
                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                        Text(sheet.affiliatedCafeName)
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(Color(hex: "EF6797"))
                        Text(sheet.headline)
                            .font(.title3.weight(.bold))
                        Text(sheet.body)
                            .font(.subheadline)
                            .foregroundStyle(Color(hex: "6C6270"))
                        if !sheet.requestableCasts.isEmpty {
                            VStack(spacing: 10) {
                                ForEach(sheet.requestableCasts, id: \.castId) { candidate in
                                    Button {
                                        onAction(.selectClaimCandidate(candidate.castId))
                                    } label: {
                                        Text(candidate.castName)
                                            .font(.body.weight(.semibold))
                                            .foregroundStyle(Color(hex: "24161E"))
                                            .frame(maxWidth: .infinity, alignment: .leading)
                                            .padding(.horizontal, 16)
                                            .padding(.vertical, 14)
                                            .background(sheet.selectedCastId == candidate.castId ? Color(hex: "FFD1DC") : Color.white)
                                            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                                            .overlay(
                                                RoundedRectangle(cornerRadius: 16, style: .continuous)
                                                    .stroke(Color(hex: "FFD1DC").opacity(0.3), lineWidth: 1)
                                            )
                                    }
                                    .buttonStyle(.plain)
                                    .onAppear {
                                        if candidate.castId == sheet.requestableCasts.last?.castId, sheet.canLoadMore, !sheet.isLoadingMore {
                                            onAction(.loadMoreClaimCandidates)
                                        }
                                    }
                                }
                            }
                        }
                        if sheet.canLoadMore || sheet.isLoadingMore {
                            Text(sheet.isLoadingMore ? "다음 캐스트 목록을 불러오는 중입니다." : "목록 하단에 도달하면 다음 캐스트를 이어서 불러옵니다.")
                                .font(.caption)
                                .foregroundStyle(Color(hex: "7A707A"))
                        }
                        Spacer(minLength: 8)
                    }
                    .padding(20)
                }
                if sheet.canSubmit {
                    Button {
                        onAction(.submitCastClaim)
                    } label: {
                        Text(sheet.isSubmitting ? "요청 보내는 중..." : "연결 요청 보내기")
                            .font(.headline.weight(.bold))
                            .foregroundStyle(Color(hex: "24161E"))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 16)
                            .background(Color(hex: "FFD1DC"))
                            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                    }
                    .buttonStyle(.plain)
                    .disabled(sheet.isSubmitting)
                    .padding(.horizontal, 16)
                    .padding(.top, 14)
                    .padding(.bottom, 14)
                    .background(Color.white)
                }
            }
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("닫기") {
                        onAction(.dismissClaimSheet)
                    }
                }
            }
        }
    }
}

struct FanManagementView_Previews: PreviewProvider {
    static var previews: some View {
        FanManagementView(onNavigationAction: { _ in })
    }
}

private struct WeeklyScheduleItem {
    let dayLabel: String
    let isWorking: Bool
}

private func weeklySchedule(from schedules: [CastSchedule]) -> [WeeklyScheduleItem] {
    let workingDays = Set(schedules.compactMap { TimeUtils.weekdayLabel(fromIsoDate: $0.date) })
    return ["월", "화", "수", "목", "금", "토", "일"].map { dayLabel in
        WeeklyScheduleItem(dayLabel: dayLabel, isWorking: workingDays.contains(dayLabel))
    }
}
