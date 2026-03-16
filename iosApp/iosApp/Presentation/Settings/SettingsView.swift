//
//  SettingsView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import SwiftUI

struct SettingsView: View {
    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel = SettingsViewModel()

    var body: some View {
        SettingsContentView(
            uiState: viewModel.uiState,
            onAction: viewModel.onAction
        )
        .onReceive(viewModel.event) { event in
            switch event {
            case .navigateBack:
                onNavigationAction(.navigateBack)
            case .navigateToAccountSettings:
                onNavigationAction(.navigateToAccountSettings)
            case .navigateToNotificationSettings:
                onNavigationAction(.navigateToNotificationSettings)
            case .navigateToInquiry:
                onNavigationAction(.navigateToInquiry)
            case .navigateToExternalLink(let title, let url):
                onNavigationAction(.navigateToExternalLink(title: title, url: url))
            }
        }
        .navigationTitle("설정")
        .navigationBarTitleDisplayMode(.inline)
    }
}

private struct SettingsContentView: View {
    let uiState: SettingsUiState

    let onAction: (SettingsAction) -> Void

    var body: some View {
        List {
            Section {
                SettingsRow(item: .account)
                    .contentShape(Rectangle())
                    .onTapGesture { onAction(.accountSettingsTapped) }

                SettingsRow(item: .notification)
                    .contentShape(Rectangle())
                    .onTapGesture { onAction(.notificationSettingsTapped) }

                SettingsRow(item: .appInfo(version: uiState.appVersion))

                SettingsRow(item: .customerSupport)
                    .contentShape(Rectangle())
                    .onTapGesture { onAction(.customerSupportTapped) }

                SettingsRow(item: .inquiry)
                    .contentShape(Rectangle())
                    .onTapGesture { onAction(.inquiryTapped) }

                SettingsRow(item: .privacyPolicy)
                    .contentShape(Rectangle())
                    .onTapGesture { onAction(.privacyPolicyTapped) }

                SettingsRow(item: .appInfo(version: uiState.appVersion))

                SettingsRow(item: .signOut)
                    .contentShape(Rectangle())
                    .onTapGesture { onAction(.signOutTapped) }
            }
            if let errorMessage = uiState.errorMessage {
                Section {
                    Text(errorMessage)
                        .font(.caption)
                        .foregroundStyle(Color.red)
                }
            }
        }
    }
}

private struct SettingsRow: View {
    let item: SettingsItem

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: item.icon)
                .foregroundStyle(item.foregroundColor)
                .frame(width: 24)
            VStack(alignment: .leading, spacing: 4) {
                Text(item.title)
                    .font(.subheadline)
                    .bold()
                Text(item.description)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Spacer()
            if let trailingText = item.trailingText {
                Text(trailingText)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(.vertical, 4)
    }
}

private struct SettingsItem {
    let icon: String
    let title: String
    let description: String
    let foregroundColor: Color
    let trailingText: String?

    static let account = SettingsItem(
        icon: "person.crop.circle",
        title: "계정 관리",
        description: "프로필과 로그인 정보를 관리합니다.",
        foregroundColor: Color(hex: "EF6797"),
        trailingText: nil
    )

    static let notification = SettingsItem(
        icon: "bell.badge",
        title: "알림 설정",
        description: "출근, 생일, 공지 알림 설정 영역입니다.",
        foregroundColor: Color(hex: "EF6797"),
        trailingText: nil
    )

    static func appInfo(version: String) -> SettingsItem {
        SettingsItem(
            icon: "info.circle",
            title: "앱 정보",
            description: "현재 설치된 앱 버전을 확인합니다.",
            foregroundColor: Color(hex: "EF6797"),
            trailingText: "v\(version)"
        )
    }

    static let customerSupport = SettingsItem(
        icon: "headphones",
        title: "고객지원",
        description: "서비스 이용 관련 문의를 남길 수 있습니다.",
        foregroundColor: Color(hex: "EF6797"),
        trailingText: nil
    )

    static let inquiry = SettingsItem(
        icon: "bubble.left.and.text.bubble.right",
        title: "문의하기",
        description: "불편사항이나 제안을 입력 폼으로 전달합니다.",
        foregroundColor: Color(hex: "EF6797"),
        trailingText: nil
    )

    static let privacyPolicy = SettingsItem(
        icon: "lock.doc",
        title: "개인정보 처리방침",
        description: "개인정보 처리방침 외부 링크를 확인합니다.",
        foregroundColor: Color(hex: "EF6797"),
        trailingText: nil
    )

    static let signOut = SettingsItem(
        icon: "rectangle.portrait.and.arrow.right",
        title: "로그아웃",
        description: "현재 계정에서 로그아웃합니다.",
        foregroundColor: .red,
        trailingText: nil
    )
}

struct SettingsView_Previews: PreviewProvider {
    static var previews: some View {
        SettingsView(onNavigationAction: { _ in })
    }
}
