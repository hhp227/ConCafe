//
//  NotificationSettingsView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/14.
//

import SwiftUI

struct NotificationSettingsView: View {
    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel = NotificationSettingsViewModel()

    var body: some View {
        NotificationSettingsContentView(
            uiState: viewModel.uiState,
            onAction: viewModel.onAction
        )
        .onReceive(viewModel.event) { event in
            switch event {
            case .navigateBack:
                onNavigationAction(.navigateBack)
            }
        }
        .navigationTitle("알림 설정")
        .navigationBarTitleDisplayMode(.inline)
    }
}

private struct NotificationSettingsContentView: View {
    let uiState: NotificationSettingsUiState

    let onAction: (NotificationSettingsAction) -> Void

    var body: some View {
        ScrollView {
            VStack(spacing: 18) {
                heroCard
                settingsCard(title: "기본 수신") {
                    NotificationToggleRow(
                        symbol: "bell.badge.fill",
                        iconBackground: Color(hex: "FFE6F1"),
                        iconForeground: Color(hex: "EB5F97"),
                        title: "푸시 알림 받기",
                        description: "새 공지와 팬 활동 업데이트를 앱 푸시로 받아요.",
                        isOn: uiState.isPushNotificationsEnabled,
                        onToggle: { onAction(.pushNotificationsToggled($0)) }
                    )
                }
                settingsCard(title: "알림 종류") {
                    VStack(spacing: 12) {
                        NotificationToggleRow(
                            symbol: "figure.walk.motion",
                            iconBackground: Color(hex: "E4F7EC"),
                            iconForeground: Color(hex: "2E9E5B"),
                            title: "출근 알림",
                            description: "팔로우한 캐스트의 오늘 출근 소식을 빠르게 받아요.",
                            isOn: uiState.isShiftNotificationsEnabled,
                            onToggle: { onAction(.shiftNotificationsToggled($0)) }
                        )
                        NotificationToggleRow(
                            symbol: "birthday.cake.fill",
                            iconBackground: Color(hex: "FFE6F1"),
                            iconForeground: Color(hex: "EB5F97"),
                            title: "생일 알림",
                            description: "생일이 다가오는 캐스트와 당일 이벤트를 놓치지 않아요.",
                            isOn: uiState.isBirthdayNotificationsEnabled,
                            onToggle: { onAction(.birthdayNotificationsToggled($0)) }
                        )
                        NotificationToggleRow(
                            symbol: "megaphone.fill",
                            iconBackground: Color(hex: "E8F0FF"),
                            iconForeground: Color(hex: "4A79E8"),
                            title: "공지 알림",
                            description: "카페 공지와 이벤트 업데이트를 우선적으로 받아요.",
                            isOn: uiState.isNoticeNotificationsEnabled,
                            onToggle: { onAction(.noticeNotificationsToggled($0)) }
                        )
                    }
                }
                settingsCard(title: "조용한 시간") {
                    VStack(alignment: .leading, spacing: 12) {
                        Text("밤 시간이나 하루 요약 모드를 선택해 알림 강도를 조절할 수 있어요.")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                        quietHoursChips
                        quietHoursDescriptionCard
                    }
                }
            }
            .padding(16)
            .padding(.bottom, 24)
        }
        .background(Color(hex: "FFFBFD"))
    }

    private var heroCard: some View {
        HStack(spacing: 14) {
            VStack(alignment: .leading, spacing: 6) {
                Text("알림 스타일을 취향에 맞게 조절하세요")
                    .font(.headline)
                    .bold()
                    .foregroundStyle(.white)
                Text(heroSummary)
                    .font(.subheadline)
                    .foregroundStyle(.white.opacity(0.92))
            }
            Spacer()
            Circle()
                .fill(.white.opacity(0.18))
                .frame(width: 54, height: 54)
                .overlay(
                    Image(systemName: "moon.stars.fill")
                        .font(.system(size: 22, weight: .semibold))
                        .foregroundStyle(.white)
                )
        }
        .padding(20)
        .background(
            LinearGradient(
                colors: [Color(hex: "EF6797"), Color(hex: "F7A0C1")],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
    }

    private var quietHoursChips: some View {
        VStack(alignment: .leading, spacing: 8) {
            ForEach(NotificationQuietHoursOption.allCases) { option in
                Button {
                    onAction(.quietHoursSelected(option))
                } label: {
                    HStack {
                        Text(option.title)
                            .font(.subheadline)
                            .bold()
                            .foregroundStyle(uiState.quietHoursOption == option ? Color(hex: "B84473") : Color(hex: "5F5664"))
                        Spacer()
                        if uiState.quietHoursOption == option {
                            Image(systemName: "checkmark.circle.fill")
                                .foregroundStyle(Color(hex: "EF6797"))
                        }
                    }
                    .padding(.horizontal, 14)
                    .padding(.vertical, 12)
                    .frame(maxWidth: .infinity)
                    .background(uiState.quietHoursOption == option ? Color(hex: "FFF1F7") : .white)
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .stroke(
                                uiState.quietHoursOption == option ? Color(hex: "FFD6E5") : Color(hex: "F0E8ED"),
                                lineWidth: 1
                            )
                    )
                }
                .buttonStyle(.plain)
            }
        }
    }

    private var quietHoursDescriptionCard: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(uiState.quietHoursOption.title)
                .font(.subheadline)
                .bold()
                .foregroundStyle(Color(hex: "B84473"))
            Text(uiState.quietHoursOption.description)
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(hex: "FFF6FA"))
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(Color(hex: "FFD6E5"), lineWidth: 1)
        )
    }

    private var heroSummary: String {
        let enabledCount = [
            uiState.isPushNotificationsEnabled,
            uiState.isShiftNotificationsEnabled,
            uiState.isBirthdayNotificationsEnabled,
            uiState.isNoticeNotificationsEnabled
        ].filter { $0 }.count
        return "현재 \(enabledCount)개 알림을 켜 두었고, \(uiState.quietHoursOption.title) 모드로 받을 예정입니다."
    }

    private func settingsCard<Content: View>(
        title: String,
        @ViewBuilder content: () -> Content
    ) -> some View {
        VStack(alignment: .leading, spacing: 14) {
            Text(title)
                .font(.title3)
                .bold()
            content()
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.white)
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
    }
}

private struct NotificationToggleRow: View {
    let symbol: String

    let iconBackground: Color

    let iconForeground: Color

    let title: String

    let description: String

    let isOn: Bool

    let onToggle: (Bool) -> Void

    var body: some View {
        HStack(spacing: 14) {
            Circle()
                .fill(iconBackground)
                .frame(width: 46, height: 46)
                .overlay(
                    Image(systemName: symbol)
                        .font(.system(size: 19, weight: .semibold))
                        .foregroundStyle(iconForeground)
                )
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.subheadline)
                    .bold()
                Text(description)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Spacer()
            Toggle("", isOn: Binding(
                get: { isOn },
                set: onToggle
            ))
            .labelsHidden()
            .tint(Color(hex: "EF6797"))
        }
    }
}

struct NotificationSettingsView_Previews: PreviewProvider {
    static var previews: some View {
        NotificationSettingsView(onNavigationAction: { _ in })
    }
}
