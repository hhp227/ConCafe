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
                SettingsRow(
                    item: .account
                )
                .contentShape(Rectangle())
                .onTapGesture {
                    onAction(.accountSettingsTapped)
                }
                SettingsRow(
                    item: .notification
                )
                .contentShape(Rectangle())
                .onTapGesture {
                    onAction(.notificationSettingsTapped)
                }
                SettingsRow(
                    item: .app
                )
                SettingsRow(
                    item: .privacyPolicy
                )
                .contentShape(Rectangle())
                .onTapGesture {
                    onAction(.privacyPolicyTapped)
                }
                SettingsRow(
                    item: .signOut
                )
                .contentShape(Rectangle())
                .onTapGesture {
                    onAction(.signOutTapped)
                }
            } header: {
                Text("내정보 탭에서 진입한 설정")
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
        }
        .padding(.vertical, 4)
    }
}

private struct SettingsItem {
    let icon: String
    let title: String
    let description: String
    let foregroundColor: Color

    static let account = SettingsItem(
        icon: "person.crop.circle",
        title: "계정 관리",
        description: "프로필과 로그인 정보를 관리합니다.",
        foregroundColor: Color(hex: "EF6797")
    )

    static let notification = SettingsItem(
        icon: "bell.badge",
        title: "알림 설정",
        description: "출근, 생일, 공지 알림 설정 영역입니다.",
        foregroundColor: Color(hex: "EF6797")
    )

    static let app = SettingsItem(
        icon: "info.circle",
        title: "앱 정보",
        description: "버전 및 고객지원 안내를 제공합니다.",
        foregroundColor: Color(hex: "EF6797")
    )

    static let privacyPolicy = SettingsItem(
        icon: "lock.doc",
        title: "개인정보 처리방침",
        description: "개인정보 처리방침 외부 링크를 확인합니다.",
        foregroundColor: Color(hex: "EF6797")
    )

    static let signOut = SettingsItem(
        icon: "rectangle.portrait.and.arrow.right",
        title: "로그아웃",
        description: "현재 계정에서 로그아웃합니다.",
        foregroundColor: .red
    }
}

struct SettingsView_Previews: PreviewProvider {
    static var previews: some View {
        SettingsView(onNavigationAction: { _ in })
    }
}
