//
//  FanManagementView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import SwiftUI

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
                profileSummaryCard
                statsRow
                if let infoMessage = uiState.infoMessage {
                    infoBanner(message: infoMessage)
                }
                primaryAnnouncementButton
                quickActionGrid
                recentFollowersSection
                topFansSection
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

    private var profileSummaryCard: some View {
        HStack(spacing: 16) {
            ZStack(alignment: .bottomTrailing) {
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
                        Circle()
                            .stroke(Color(hex: "FFD1DC"), lineWidth: 2)
                    )
                    .overlay {
                        Text(uiState.castProfile.profileAccent)
                            .font(.title3.weight(.bold))
                            .foregroundStyle(Color(hex: "7C3F67"))
                    }
                if uiState.castProfile.isOnline {
                    Circle()
                        .fill(Color(hex: "37B26C"))
                        .frame(width: 18, height: 18)
                        .overlay(Circle().stroke(.white, lineWidth: 2))
                }
            }
            VStack(alignment: .leading, spacing: 6) {
                HStack(alignment: .bottom, spacing: 8) {
                    HStack(alignment: .bottom, spacing: 8) {
                        Text(uiState.castProfile.stageName)
                            .font(.title2.weight(.bold))
                            .foregroundStyle(Color(hex: "24161E"))
                        Text(uiState.castProfile.localizedName)
                            .font(.subheadline)
                            .foregroundStyle(Color(hex: "7A707A"))
                    }
                    Spacer(minLength: 8)
                    Button {
                        onAction(.clickEditProfile)
                    } label: {
                        HStack(spacing: 4) {
                            Image(systemName: "pencil")
                                .font(.caption.weight(.bold))
                            Text("수정")
                                .font(.caption.weight(.bold))
                        }
                        .foregroundStyle(Color(hex: "7C3F67"))
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(Color(hex: "FFD1DC").opacity(0.08))
                        .clipShape(Capsule())
                    }
                    .buttonStyle(.plain)
                }
                HStack(spacing: 6) {
                    Image(systemName: "storefront")
                        .font(.caption.weight(.bold))
                        .foregroundStyle(Color(hex: "EF6797"))
                    Text(uiState.castProfile.cafeName)
                        .font(.subheadline)
                        .foregroundStyle(Color(hex: "5B4A57"))
                }
            }
            Spacer(minLength: 0)
        }
        .padding(18)
        .background(Color.white.opacity(0.94))
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .stroke(Color.white.opacity(0.65), lineWidth: 1)
        )
        .shadow(color: Color.black.opacity(0.04), radius: 12, x: 0, y: 6)
    }

    private var statsRow: some View {
        HStack(spacing: 10) {
            ForEach(uiState.stats, id: \.label) { stat in
                let isPrimary = stat.highlight == .primary

                VStack(spacing: 6) {
                    Text(stat.label)
                        .font(.caption2.weight(.bold))
                        .foregroundStyle(Color(hex: "7A707A"))
                        .multilineTextAlignment(.center)
                    Text(stat.value)
                        .font(.title3.weight(.bold))
                        .foregroundStyle(isPrimary ? Color(hex: "D94A82") : Color(hex: "24161E"))
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 16)
                .padding(.horizontal, 10)
                .background(isPrimary ? Color(hex: "FFD1DC").opacity(0.12) : Color.white.opacity(0.92))
                .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 20, style: .continuous)
                        .stroke(isPrimary ? Color(hex: "FFB3C6").opacity(0.4) : Color(hex: "FFD1DC").opacity(0.16), lineWidth: 1)
                )
            }
        }
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

    private var quickActionGrid: some View {
        HStack(spacing: 12) {
            ForEach(FanManagementUiState.QuickAction.allCases, id: \.self) { quickAction in
                Button {
                    onAction(.clickQuickAction(quickAction))
                } label: {
                    ZStack(alignment: .topTrailing) {
                        VStack(spacing: 10) {
                            RoundedRectangle(cornerRadius: 14, style: .continuous)
                                .fill(Color(hex: "FFD1DC").opacity(0.08))
                                .frame(width: 40, height: 40)
                                .overlay {
                                    Image(systemName: "calendar")
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
                }
                .buttonStyle(.plain)
            }
            Spacer(minLength: 0)
        }
    }

    private var recentFollowersSection: some View {
        sectionContainer(title: "최근 팔로워", actionLabel: "전체보기") {
            onAction(.clickViewAllFollowers)
        } content: {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 14) {
                    ForEach(uiState.recentFollowers) { follower in
                        Button {
                            onAction(.clickRecentFollower(id: follower.id))
                        } label: {
                            VStack(spacing: 8) {
                                Circle()
                                    .fill(
                                        LinearGradient(
                                            colors: follower.accent ? [Color(hex: "FFD7E5"), Color(hex: "F2ADC2")] : [Color(hex: "F2EEF1"), Color(hex: "E3D9E2")],
                                            startPoint: .topLeading,
                                            endPoint: .bottomTrailing
                                        )
                                    )
                                    .frame(width: 58, height: 58)
                                    .overlay {
                                        Text(follower.initial)
                                            .font(.headline.weight(.bold))
                                            .foregroundStyle(Color(hex: "6E5566"))
                                    }
                                    .overlay(
                                        Circle().stroke(follower.accent ? Color(hex: "FFD1DC") : .clear, lineWidth: 2)
                                    )
                                Text(follower.name)
                                    .font(.caption.weight(.medium))
                                    .foregroundStyle(Color(hex: "24161E"))
                                Text(follower.joinedLabel)
                                    .font(.caption2)
                                    .foregroundStyle(Color(hex: "9C8C98"))
                            }
                            .frame(width: 74)
                        }
                        .buttonStyle(.plain)
                    }
                    VStack(spacing: 8) {
                        Circle()
                            .fill(Color(hex: "F8F1F4"))
                            .frame(width: 58, height: 58)
                            .overlay(
                                Circle().stroke(Color(hex: "D9CBD4"), lineWidth: 1)
                            )
                            .overlay {
                                Image(systemName: "person.2.fill")
                                    .foregroundStyle(Color(hex: "A28E9B"))
                            }
                        Text("팬 확장")
                            .font(.caption.weight(.medium))
                            .foregroundStyle(Color(hex: "7A707A"))
                    }
                    .frame(width: 74)
                }
            }
        }
    }

    private var topFansSection: some View {
        sectionContainer(title: "이달의 TOP 팬", actionLabel: "상호작용 기준", action: nil) {
            VStack(spacing: 12) {
                ForEach(uiState.topFans) { fan in
                    Button {
                        onAction(.clickTopFan(id: fan.id))
                    } label: {
                        HStack(spacing: 12) {
                            Text("\(fan.rank)")
                                .font(.headline.weight(.bold))
                                .foregroundStyle(rankColor(fan.rank))
                            Circle()
                                .fill(Color(hex: "F6E3EC"))
                                .frame(width: 42, height: 42)
                                .overlay {
                                    Text(String(fan.name.prefix(1)))
                                        .font(.headline.weight(.bold))
                                        .foregroundStyle(Color(hex: "7C3F67"))
                                }
                            VStack(alignment: .leading, spacing: 4) {
                                Text(fan.name)
                                    .font(.subheadline.weight(.bold))
                                    .foregroundStyle(Color(hex: "24161E"))
                                Text("포인트: \(fan.pointsLabel)")
                                    .font(.caption)
                                    .foregroundStyle(Color(hex: "7A707A"))
                            }
                            Spacer()
                            if fan.isBest {
                                HStack(spacing: 4) {
                                    Image(systemName: "heart.fill")
                                        .font(.caption)
                                    Text("BEST")
                                        .font(.caption2.weight(.bold))
                                }
                                .foregroundStyle(Color(hex: "D94A82"))
                                .padding(.horizontal, 10)
                                .padding(.vertical, 6)
                                .background(Color(hex: "FFD1DC").opacity(0.12))
                                .clipShape(Capsule())
                            }
                        }
                        .padding(.horizontal, 14)
                        .padding(.vertical, 14)
                        .background(Color.white.opacity(0.92))
                        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                        .overlay(
                            RoundedRectangle(cornerRadius: 18, style: .continuous)
                                .stroke(Color(hex: "FFD1DC").opacity(0.16), lineWidth: 1)
                        )
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private func sectionContainer<Content: View>(
        title: String,
        actionLabel: String,
        action: (() -> Void)? = nil,
        @ViewBuilder content: () -> Content
    ) -> some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                Text(title)
                    .font(.title3.weight(.bold))
                    .foregroundStyle(Color(hex: "24161E"))
                Spacer()
                if let action {
                    Button(actionLabel) {
                        action()
                    }
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(Color(hex: "7A707A"))
                } else {
                    Text(actionLabel)
                        .font(.subheadline.weight(.medium))
                        .foregroundStyle(Color(hex: "7A707A"))
                }
            }
            content()
        }
    }

    private func rankColor(_ rank: Int) -> Color {
        switch rank {
        case 1:
            return Color(hex: "D99A00")
        case 2:
            return Color(hex: "8E8896")
        default:
            return Color(hex: "DC8346")
        }
    }
}

struct FanManagementView_Previews: PreviewProvider {
    static var previews: some View {
        FanManagementView(onNavigationAction: { _ in })
    }
}
