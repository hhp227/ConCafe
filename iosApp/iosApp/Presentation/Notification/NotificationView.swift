//
//  NotificationView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/05.
//

import SwiftUI
import Shared

struct NotificationView: View {
    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel = NotificationViewModel()

    var body: some View {
        NotificationContentView(
            uiState: viewModel.uiState,
            onAction: viewModel.onAction
        )
        .onReceive(viewModel.event) { event in
            switch event {
            case .navigateToCafe(let id):
                onNavigationAction(.navigateToCafe(id: id))
            case .navigateToCast(let id):
                onNavigationAction(.navigateToCast(id: id))
            case .navigateToSignIn:
                onNavigationAction(.navigateToSignIn)
            case .navigateBack:
                onNavigationAction(.navigateBack)
            }
        }
        .navigationTitle(String(localized: String.LocalizationValue("common_notification"), table: "Localizable"))
        .navigationBarTitleDisplayMode(.inline)
    }
}

private struct NotificationContentView: View {
    let uiState: NotificationUiState

    let onAction: (NotificationAction) -> Void

    var body: some View {
        Group {
            if uiState.isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if !uiState.isLoggedIn {
                NotificationSignInRequiredView(onAction: onAction)
            } else {
                NotificationSectionsView(
                    uiState: uiState,
                    onAction: onAction
                )
            }
        }
        .background(Color(hex: "FFFBFD"))
    }
}

private struct NotificationSignInRequiredView: View {
    let onAction: (NotificationAction) -> Void

    var body: some View {
        ZStack {
            Color(hex: "FFFBFD")
                .ignoresSafeArea()
            VStack(spacing: 12) {
                Image(systemName: "bell.badge")
                    .font(.system(size: 32, weight: .semibold))
                    .foregroundStyle(Color(hex: "EF6797"))
                Text(String(localized: String.LocalizationValue("notification_login_required_title"), table: "Localizable"))
                    .font(.headline)
                    .bold()
                    .frame(maxWidth: .infinity)
                    .multilineTextAlignment(.center)
                Text(String(localized: String.LocalizationValue("notification_login_required_desc"), table: "Localizable"))
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity)
                    .multilineTextAlignment(.center)
                Button {
                    onAction(.signInTapped)
                } label: {
                    Text(String(localized: String.LocalizationValue("signin_submit"), table: "Localizable"))
                        .font(.headline)
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 48)
                        .background(Color(hex: "EF6797"))
                        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                }
            }
            .padding(24)
            .frame(maxWidth: 420)
            .background(.white)
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            .padding(24)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private struct NotificationSectionsView: View {
    let uiState: NotificationUiState

    let onAction: (NotificationAction) -> Void

    var body: some View {
        ScrollView {
            VStack(spacing: 18) {
                summaryCard
                ForEach(uiState.sections, id: \.id) { section in
                    sectionView(section)
                }
            }
            .padding(16)
            .padding(.bottom, 20)
        }
        .background(Color(hex: "FFFBFD"))
    }

    private var summaryCard: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(String(format: String(localized: String.LocalizationValue("notification_summary_title"), table: "Localizable"), locale: Locale.current, uiState.unreadCount))
                    .font(.headline)
                    .bold()
                    .foregroundStyle(.white)
                Text(String(localized: String.LocalizationValue("notification_summary_desc"), table: "Localizable"))
                    .font(.subheadline)
                    .foregroundStyle(.white.opacity(0.92))
            }
            Spacer()
            Image(systemName: "bell.badge.fill")
                .font(.system(size: 24, weight: .semibold))
                .foregroundStyle(.white)
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

    private func sectionView(_ section: NotificationSection) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(section.title)
                .font(.title3)
                .bold()
            VStack(spacing: 8) {
                ForEach(section.items, id: \.id) { item in
                    notificationCard(item)
                }
            }
        }
    }

    private func notificationCard(_ item: NotificationListItem) -> some View {
        let visual = notificationVisual(type: item.type)
        let containerColor = item.isRead ? Color.white : Color(hex: "FFF3F8")
        return HStack(alignment: .top, spacing: 12) {
            Circle()
                .fill(visual.background)
                .frame(width: 48, height: 48)
                .overlay(
                    Image(systemName: visual.symbol)
                        .font(.system(size: 20, weight: .semibold))
                        .foregroundStyle(visual.foreground)
                )
                .overlay(alignment: .topTrailing) {
                    if !item.isRead {
                        Circle()
                            .fill(Color(hex: "EF6797"))
                            .frame(width: 10, height: 10)
                            .overlay(Circle().stroke(Color.white, lineWidth: 2))
                            .offset(x: 2, y: -2)
                    }
                }
            VStack(alignment: .leading, spacing: 4) {
                HStack(alignment: .top) {
                    Text(item.title)
                        .font(.subheadline)
                        .bold()
                    Spacer()
                    Text(item.relativeTime)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Text(item.message)
                    .font(.subheadline)
                    .foregroundStyle(Color(hex: "6D6671"))
            }
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(containerColor)
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .onTapGesture {
            onAction(.notificationTapped(id: item.id, type: item.type, targetId: item.targetId))
        }
    }

    private func notificationVisual(type: String) -> (symbol: String, background: Color, foreground: Color) {
        if type == "CAST_SHIFT" {
            return ("figure.walk.motion", Color(hex: "E4F7EC"), Color(hex: "2E9E5B"))
        } else if type == "BIRTHDAY" {
            return ("birthday.cake.fill", Color(hex: "FFE6F1"), Color(hex: "EB5F97"))
        } else if type == "CAFE_NOTICE" {
            return ("megaphone.fill", Color(hex: "E8F0FF"), Color(hex: "4A79E8"))
        } else if type == "CAFE_EVENT" {
            return ("party.popper.fill", Color(hex: "FFF4E2"), Color(hex: "E29B35"))
        } else if type == "FOLLOW_UPDATE" {
            return ("person.badge.plus.fill", Color(hex: "F1E8FF"), Color(hex: "8A52E2"))
        } else {
            return ("bell.fill", Color(hex: "F2F2F2"), Color(hex: "666666"))
        }
    }
}

struct NotificationView_Previews: PreviewProvider {
    static var previews: some View {
        NotificationView(onNavigationAction: { _ in })
    }
}
