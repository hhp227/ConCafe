//
//  NotificationSettingsView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/14.
//

import SwiftUI
import Shared

struct NotificationSettingsView: View {
    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel = NotificationSettingsViewModel()

    @State private var toastMessage: String?

    var body: some View {
        ZStack {
            NotificationSettingsContentView(
                uiState: viewModel.uiState,
                onAction: viewModel.onAction
            )
            if viewModel.uiState.isLoading {
                ProgressView()
            }
        }
        .onReceive(viewModel.event) { event in
            switch event {
            case .navigateBack:
                onNavigationAction(.navigateBack)
            case .showMessage(let message):
                toastMessage = message
            }
        }
        .alert(String(localized: String.LocalizationValue("common_notification"), table: "Localizable"), isPresented: Binding(
            get: { toastMessage != nil },
            set: { isPresented in
                if !isPresented {
                    toastMessage = nil
                }
            }
        )) {
            Button(String(localized: String.LocalizationValue("common_confirm"), table: "Localizable"), role: .cancel) {
                toastMessage = nil
            }
        } message: {
            Text(toastMessage ?? "")
        }
        .navigationTitle(String(localized: String.LocalizationValue("notification_settings_title"), table: "Localizable"))
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
                settingsCard(title: String(localized: String.LocalizationValue("notification_settings_basic_title"), table: "Localizable")) {
                    NotificationToggleRow(
                        symbol: "bell.badge.fill",
                        iconBackground: Color(hex: "FFE6F1"),
                        iconForeground: Color(hex: "EB5F97"),
                        title: String(localized: String.LocalizationValue("notification_settings_push_title"), table: "Localizable"),
                        description: String(localized: String.LocalizationValue("notification_settings_push_desc"), table: "Localizable"),
                        isOn: uiState.isPushNotificationsEnabled,
                        onToggle: { onAction(.pushNotificationsToggled($0)) },
                        isEnabled: !uiState.isSaving
                    )
                }
                settingsCard(title: String(localized: String.LocalizationValue("notification_settings_type_title"), table: "Localizable")) {
                    VStack(spacing: 12) {
                        NotificationToggleRow(
                            symbol: "figure.walk.motion",
                            iconBackground: Color(hex: "E4F7EC"),
                            iconForeground: Color(hex: "2E9E5B"),
                            title: String(localized: String.LocalizationValue("notification_settings_shift_title"), table: "Localizable"),
                            description: String(localized: String.LocalizationValue("notification_settings_shift_desc"), table: "Localizable"),
                            isOn: uiState.isShiftNotificationsEnabled,
                            onToggle: { onAction(.shiftNotificationsToggled($0)) },
                            isEnabled: !uiState.isSaving
                        )
                        NotificationToggleRow(
                            symbol: "birthday.cake.fill",
                            iconBackground: Color(hex: "FFE6F1"),
                            iconForeground: Color(hex: "EB5F97"),
                            title: String(localized: String.LocalizationValue("notification_settings_birthday_title"), table: "Localizable"),
                            description: String(localized: String.LocalizationValue("notification_settings_birthday_desc"), table: "Localizable"),
                            isOn: uiState.isBirthdayNotificationsEnabled,
                            onToggle: { onAction(.birthdayNotificationsToggled($0)) },
                            isEnabled: !uiState.isSaving
                        )
                        NotificationToggleRow(
                            symbol: "megaphone.fill",
                            iconBackground: Color(hex: "E8F0FF"),
                            iconForeground: Color(hex: "4A79E8"),
                            title: String(localized: String.LocalizationValue("notification_settings_notice_title"), table: "Localizable"),
                            description: String(localized: String.LocalizationValue("notification_settings_notice_desc"), table: "Localizable"),
                            isOn: uiState.isNoticeNotificationsEnabled,
                            onToggle: { onAction(.noticeNotificationsToggled($0)) },
                            isEnabled: !uiState.isSaving
                        )
                        if uiState.isCastRole {
                            NotificationToggleRow(
                                symbol: "person.badge.plus.fill",
                                iconBackground: Color(hex: "F1E8FF"),
                                iconForeground: Color(hex: "8A52E2"),
                                title: String(localized: String.LocalizationValue("notification_settings_follow_title"), table: "Localizable"),
                                description: String(localized: String.LocalizationValue("notification_settings_follow_desc"), table: "Localizable"),
                                isOn: uiState.isFollowNotificationsEnabled,
                                onToggle: { onAction(.followNotificationsToggled($0)) },
                                isEnabled: !uiState.isSaving
                            )
                        }
                        NotificationToggleRow(
                            symbol: "party.popper.fill",
                            iconBackground: Color(hex: "FFF4E2"),
                            iconForeground: Color(hex: "E29B35"),
                            title: String(localized: String.LocalizationValue("notification_settings_event_title"), table: "Localizable"),
                            description: String(localized: String.LocalizationValue("notification_settings_event_desc"), table: "Localizable"),
                            isOn: uiState.isEventNotificationsEnabled,
                            onToggle: { onAction(.eventNotificationsToggled($0)) },
                            isEnabled: !uiState.isSaving
                        )
                    }
                }
                settingsCard(title: String(localized: String.LocalizationValue("notification_settings_quiet_title"), table: "Localizable")) {
                    VStack(alignment: .leading, spacing: 12) {
                        Text(String(localized: String.LocalizationValue("notification_settings_quiet_desc"), table: "Localizable"))
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                        quietHoursChips
                        quietHoursDescriptionCard
                        if let errorMessage = uiState.errorMessage {
                            Text(errorMessage)
                                .font(.caption)
                                .foregroundStyle(Color(hex: "C33E6A"))
                        }
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
                Text(String(localized: String.LocalizationValue("notification_settings_hero_title"), table: "Localizable"))
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
        let quietHourOptions: [NotificationQuietHoursMode] = [.off, .night, .allDay]
        return VStack(alignment: .leading, spacing: 8) {
            ForEach(quietHourOptions, id: \.self) { option in
                Button {
                    onAction(.quietHoursSelected(option))
                } label: {
                    HStack {
                        Text(option.titleText)
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
                .disabled(uiState.isSaving)
            }
        }
    }

    private var quietHoursDescriptionCard: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(uiState.quietHoursOption.titleText)
                .font(.subheadline)
                .bold()
                .foregroundStyle(Color(hex: "B84473"))
            Text(uiState.quietHoursOption.descriptionText)
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
        var toggles = [
            uiState.isPushNotificationsEnabled,
            uiState.isShiftNotificationsEnabled,
            uiState.isBirthdayNotificationsEnabled,
            uiState.isNoticeNotificationsEnabled,
            uiState.isEventNotificationsEnabled
        ]

        if uiState.isCastRole {
            toggles.append(uiState.isFollowNotificationsEnabled)
        }
        let enabledCount = toggles.filter { $0 }.count
        return String(
            format: String(localized: String.LocalizationValue("notification_settings_hero_summary"), table: "Localizable"),
            locale: Locale.current,
            enabledCount,
            uiState.quietHoursOption.titleText
        )
    }

    private func settingsCard<Content: View>(
        title: String,
        @ViewBuilder content: () -> Content
    ) -> some View {
        VStack(alignment: .leading, spacing: 14) {
            Text(title)
                .font(.headline)
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

    let isEnabled: Bool

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
            .disabled(!isEnabled)
        }
    }
}

struct NotificationSettingsView_Previews: PreviewProvider {
    static var previews: some View {
        NotificationSettingsView(onNavigationAction: { _ in })
    }
}

private extension NotificationQuietHoursMode {
    var titleText: String {
        switch self {
        case .off:
            return String(localized: String.LocalizationValue("notification_quiet_off_title"), table: "Localizable")
        case .night:
            return String(localized: String.LocalizationValue("notification_quiet_night_title"), table: "Localizable")
        case .allDay:
            return String(localized: String.LocalizationValue("notification_quiet_all_day_title"), table: "Localizable")
        default:
            return String(localized: String.LocalizationValue("notification_quiet_night_title"), table: "Localizable")
        }
    }

    var descriptionText: String {
        switch self {
        case .off:
            return String(localized: String.LocalizationValue("notification_quiet_off_desc"), table: "Localizable")
        case .night:
            return String(localized: String.LocalizationValue("notification_quiet_night_desc"), table: "Localizable")
        case .allDay:
            return String(localized: String.LocalizationValue("notification_quiet_all_day_desc"), table: "Localizable")
        default:
            return String(localized: String.LocalizationValue("notification_quiet_night_desc"), table: "Localizable")
        }
    }
}
