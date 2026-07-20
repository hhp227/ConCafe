//
//  FanManagementView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import SwiftUI
import UIKit
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
        .sheet(
            isPresented: Binding(
                get: { viewModel.uiState.isAnnouncementSheetVisible },
                set: { isPresented in
                    if !isPresented {
                        viewModel.onAction(.dismissAnnouncementSheet)
                    }
                }
            )
        ) {
            FanAnnouncementSheetView(
                uiState: viewModel.uiState,
                onAction: viewModel.onAction
            )
            .compatLargeSheetDetent()
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
        .background(ConCafeColors.background)
    }

    private func infoBanner(message: String) -> some View {
        HStack(spacing: 10) {
            Text(message)
                .font(.caption)
                .foregroundStyle(ConCafeColors.goldDeep)
                .frame(maxWidth: .infinity, alignment: .leading)
            Button("닫기") {
                onAction(.dismissInfoMessage)
            }
            .font(.caption.weight(.bold))
            .foregroundStyle(ConCafeColors.goldDeep)
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
            .foregroundStyle(ConCafeColors.onPrimaryContainer)
            .padding(.horizontal, 18)
            .padding(.vertical, 16)
            .background(ConCafeColors.primaryContainer)
            .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
        }
        .buttonStyle(.plain)
    }

    private func castClaimStatusCard(_ status: FanManagementUiState.CastClaimStatusCard) -> some View {
        let accentBackground: Color = {
            switch status.accent {
            case .pending: return ConCafeColors.primaryContainer
            case .linked: return ConCafeColors.successContainer
            case .rejected: return ConCafeColors.surfaceTint
            }
        }()
        let accentForeground: Color = {
            switch status.accent {
            case .pending: return ConCafeColors.onPrimaryContainer
            case .linked: return ConCafeColors.success
            case .rejected: return ConCafeColors.onTertiaryContainer
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
                    .foregroundStyle(.primary)
                Text(status.body)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
                HStack {
                    Text("프로필 연결 상태 보기")
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(ConCafeColors.primary)
                    Spacer()
                    Image(systemName: "chevron.right")
                        .foregroundStyle(ConCafeColors.primary)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(18)
            .background(ConCafeColors.surface)
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
                                .fill(ConCafeColors.primaryContainer)
                                .frame(width: 40, height: 40)
                                .overlay {
                                    Image(systemName: iconName)
                                        .foregroundStyle(ConCafeColors.primary)
                                }
                            Text(quickAction.title)
                                .font(.subheadline.weight(.semibold))
                                .foregroundStyle(.primary)
                            Text(quickAction.subtitle)
                                .font(.caption)
                                .foregroundStyle(.secondary)
                                .multilineTextAlignment(.center)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 18)
                        .background(ConCafeColors.surface)
                        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
                        .overlay(
                            RoundedRectangle(cornerRadius: 20, style: .continuous)
                                .stroke(ConCafeColors.outline, lineWidth: 1)
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
                            let joinedLabel = relativeFollowerTimeLabel(follower.followedAt)

                            Button {
                                onAction(.clickRecentFollower(id: follower.id))
                            } label: {
                                VStack(spacing: 8) {
                                    Circle()
                                        .fill(
                                            LinearGradient(
                                                colors: accent ? [ConCafeColors.primaryContainer, ConCafeColors.secondaryContainer] : [ConCafeColors.surfaceVariant, ConCafeColors.outline],
                                                startPoint: .topLeading,
                                                endPoint: .bottomTrailing
                                            )
                                        )
                                        .frame(width: 58, height: 58)
                                        .overlay {
                                            Text(String(follower.nickname.prefix(1)).uppercased())
                                                .font(.headline.weight(.bold))
                                                .foregroundStyle(ConCafeColors.textSecondary)
                                        }
                                        .overlay(
                                            Circle().stroke(accent ? ConCafeColors.primaryContainer : .clear, lineWidth: 2)
                                        )
                                    Text(follower.nickname)
                                        .font(.caption.weight(.medium))
                                        .foregroundStyle(.primary)
                                    Text(joinedLabel)
                                        .font(.caption2)
                                        .foregroundStyle(ConCafeColors.textMuted)
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
                        .fill(ConCafeColors.primaryContainer)
                        .frame(width: 34, height: 34)
                        .overlay {
                            Image(systemName: "calendar")
                                .foregroundStyle(ConCafeColors.primary)
                        }
                    VStack(alignment: .leading, spacing: 2) {
                        Text("이번 주 스케줄")
                            .font(.subheadline.weight(.bold))
                            .foregroundStyle(.primary)
                        Text("출근 관리에서 일정을 바로 조정할 수 있습니다.")
                            .font(.caption)
                            .foregroundStyle(.secondary)
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
            .background(ConCafeColors.surface)
            .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 22, style: .continuous)
                    .stroke(ConCafeColors.outline, lineWidth: 1)
            )
        }
    }

    private func weeklyScheduleCard(_ item: WeeklyScheduleItem) -> some View {
        VStack(spacing: 4) {
            Text(item.dayLabel)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(item.isWorking ? ConCafeColors.onPrimary : .secondary)
            Text(item.isWorking ? "출근" : "휴무")
                .font(.caption)
                .foregroundStyle(item.isWorking ? ConCafeColors.onPrimary.opacity(0.92) : .secondary)
        }
        .padding(.vertical, 14)
        .frame(maxWidth: .infinity)
        .background(item.isWorking ? ConCafeColors.primary : ConCafeColors.surfaceVariant)
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(item.isWorking ? .clear : ConCafeColors.outline, lineWidth: 1)
        )
    }

    private var loadingState: some View {
        emptySectionCard(message: "팬관리 정보를 불러오는 중입니다.")
    }

    private func emptySectionCard(message: String) -> some View {
        Text(message)
            .font(.subheadline)
            .foregroundStyle(.secondary)
            .frame(maxWidth: .infinity)
            .padding(.horizontal, 16)
            .padding(.vertical, 18)
            .background(ConCafeColors.surface)
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .stroke(ConCafeColors.outline, lineWidth: 1)
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
                    .font(.headline.weight(.bold))
                    .foregroundStyle(.primary)
                Spacer()
                if let actionLabel, let action {
                    Button(actionLabel) {
                        action()
                    }
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(.secondary)
                } else if let actionLabel {
                    Text(actionLabel)
                        .font(.subheadline.weight(.medium))
                        .foregroundStyle(.secondary)
                }
            }
            content()
        }
    }

}

private struct CastClaimSheetView: View {
    let sheet: FanManagementUiState.CastClaimSheet
    
    let onAction: (FanManagementAction) -> Void
    
    @State private var countBeforeLoad: Int = 0

    var body: some View {
        ScrollViewReader { proxy in
            CompatNavigationContainer(title: "프로필 연결") {
                VStack(spacing: 0) {
                    ScrollView {
                        VStack(alignment: .leading, spacing: 16) {
                            Text(sheet.affiliatedCafeName)
                                .font(.subheadline.weight(.semibold))
                                .foregroundStyle(ConCafeColors.primary)
                            Text(sheet.headline)
                                .font(.title3.weight(.bold))
                            Text(sheet.body)
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                            if !sheet.requestableCasts.isEmpty {
                                VStack(spacing: 10) {
                                    ForEach(sheet.requestableCasts, id: \.castId) { candidate in
                                        Button {
                                            onAction(.selectClaimCandidate(candidate.castId))
                                        } label: {
                                            Text(candidate.castName)
                                                .font(.body.weight(.semibold))
                                                .foregroundStyle(.primary)
                                                .frame(maxWidth: .infinity, alignment: .leading)
                                                .padding(.horizontal, 16)
                                                .padding(.vertical, 14)
                                                .background(sheet.selectedCastId == candidate.castId ? ConCafeColors.primaryContainer : ConCafeColors.surface)
                                                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                                                .overlay(
                                                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                                                        .stroke(ConCafeColors.outline, lineWidth: 1)
                                                )
                                        }
                                        .id(candidate.castId)
                                        .buttonStyle(.plain)
                                    }
                                }
                            }
                            if sheet.canLoadMore || sheet.isLoadingMore {
                                VStack(alignment: .leading, spacing: 6) {
                                    Color.clear
                                        .frame(height: 1)
                                    .onAppear {
                                        guard sheet.canLoadMore, !sheet.isLoadingMore else { return }
                                        let count = sheet.requestableCasts.count
                                        countBeforeLoad = count
                                        onAction(.loadMoreClaimCandidates)
                                    }
                                    Text(sheet.isLoadingMore ? "다음 캐스트 목록을 불러오는 중입니다." : "목록 하단에 도달하면 다음 캐스트를 이어서 불러옵니다.")
                                        .font(.caption)
                                        .foregroundStyle(.secondary)
                                    Color.clear
                                        .frame(height: 1)
                                        .onDisappear {
                                            countBeforeLoad = -1
                                        }
                                }
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
                                .foregroundStyle(ConCafeColors.onPrimaryContainer)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 16)
                                .background(ConCafeColors.primaryContainer)
                                .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                        }
                        .buttonStyle(.plain)
                        .disabled(sheet.isSubmitting)
                        .padding(.horizontal, 16)
                        .padding(.top, 14)
                        .padding(.bottom, 14)
                        .background(ConCafeColors.surface)
                    }
                }
                .onChange(of: sheet.requestableCasts.count) { newCount in
                    guard countBeforeLoad != 0, countBeforeLoad != -1 else { return }
                    let preCount = abs(countBeforeLoad)
                    let wasSubsequent = countBeforeLoad > 0
                    countBeforeLoad = -1
                    guard newCount > preCount else { return }
                    guard wasSubsequent else { return }
                    guard preCount > 0 else { return }
                    proxy.scrollTo(sheet.requestableCasts[preCount - 1].castId, anchor: .bottom)
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

private struct FanAnnouncementSheetView: View {
    let uiState: FanManagementUiState

    let onAction: (FanManagementAction) -> Void

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Text("팬 공지 작성하기")
                    .font(.title3.weight(.bold))
                    .foregroundStyle(.primary)
                Spacer()
                Button {
                    onAction(.dismissAnnouncementSheet)
                } label: {
                    Image(systemName: "xmark")
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(.secondary)
                        .frame(width: 28, height: 28)
                }
                .buttonStyle(.plain)
            }
            .padding(.horizontal, 24)
            .padding(.vertical, 14)
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    ConCafeFormField(
                        label: "제목",
                        text: Binding(
                            get: { uiState.announcementTitle },
                            set: { onAction(.changeAnnouncementTitle($0)) }
                        ),
                        placeholder: "팬에게 전달할 제목을 입력해 주세요"
                    )
                    ConCafeFormEditor(
                        label: "내용",
                        text: Binding(
                            get: { uiState.announcementBody },
                            set: { onAction(.changeAnnouncementBody($0)) }
                        ),
                        placeholder: "팬에게 전달할 공지 내용을 입력해 주세요"
                    )
                    Text("공지 내용은 팔로워에게 즉시 푸시 알림으로 전송됩니다.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                .padding(.horizontal, 24)
                .padding(.top, 8)
                .padding(.bottom, 28)
                .padding(.bottom, 60)
            }
            VStack {
                Button {
                    onAction(.submitAnnouncement)
                } label: {
                    HStack {
                        if uiState.isSendingAnnouncement {
                            ProgressView()
                                .progressViewStyle(.circular)
                                .tint(ConCafeColors.onPrimaryContainer)
                        } else {
                            Text("팬 공지 전송")
                                .font(.headline.weight(.bold))
                                .foregroundStyle(uiState.isAnnouncementSubmitEnabled ? ConCafeColors.onPrimaryContainer : ConCafeColors.outlineStrong)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 60)
                    .background(uiState.isAnnouncementSubmitEnabled ? ConCafeColors.primaryContainer : ConCafeColors.surfaceTint)
                    .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
                }
                .buttonStyle(.plain)
                .disabled(!uiState.isAnnouncementSubmitEnabled)
            }
            .padding(.horizontal, 24)
            .padding(.top, 14)
            .padding(.bottom, 18)
            .background(ConCafeColors.background)
        }
        .background(ConCafeColors.background)
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

private func relativeFollowerTimeLabel(_ followedAt: String) -> String {
    let formatter = ISO8601DateFormatter()
    guard let date = formatter.date(from: followedAt) else {
        return "최근"
    }
    let diff = max(Int(Date().timeIntervalSince(date)), 0)
    if diff < 60 {
        return "방금 전"
    } else if diff < 3600 {
        return "\(diff / 60)분 전"
    } else if diff < 86_400 {
        return "\(diff / 3600)시간 전"
    } else if diff < 2_592_000 {
        return "\(diff / 86_400)일 전"
    } else {
        return "오래 전"
    }
}
