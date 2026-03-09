//
//  SettingsView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import SwiftUI

struct SettingsView: View {
    let onNavigationAction: (NavigationAction) -> Void

    var body: some View {
        List {
            Section {
                settingsRow(
                    icon: "person.crop.circle",
                    title: "계정 관리",
                    description: "프로필과 로그인 정보를 관리합니다."
                )
                settingsRow(
                    icon: "bell.badge",
                    title: "알림 설정",
                    description: "출근, 생일, 공지 알림 설정 영역입니다."
                )
                settingsRow(
                    icon: "info.circle",
                    title: "앱 정보",
                    description: "버전 및 고객지원 안내를 제공합니다."
                )
                settingsRow(
                    icon: "rectangle.portrait.and.arrow.right",
                    title: "로그아웃",
                    description: "현재 단계에서는 진입점만 제공합니다."
                )
            } header: {
                Text("내정보 탭에서 진입한 설정")
            }
        }
        .navigationTitle("설정")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func settingsRow(
        icon: String,
        title: String,
        description: String
    ) -> some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .foregroundStyle(Color(hex: "EF6797"))
                .frame(width: 24)
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.subheadline)
                    .bold()
                Text(description)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(.vertical, 4)
    }
}

struct SettingsView_Previews: PreviewProvider {
    static var previews: some View {
        SettingsView(onNavigationAction: { _ in })
    }
}
