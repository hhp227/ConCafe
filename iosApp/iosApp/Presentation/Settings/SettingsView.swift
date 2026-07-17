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
        .navigationTitle(String(localized: String.LocalizationValue("settings_title"), table: "Localizable"))
        .navigationBarTitleDisplayMode(.inline)
    }
}

private struct SettingsContentView: View {
    let uiState: SettingsUiState

    let onAction: (SettingsAction) -> Void

    var body: some View {
        List {
            Section(String(localized: String.LocalizationValue("settings_preferences_title"), table: "Localizable")) {
                ThemePickerRow(
                    selectedThemeMode: uiState.themeMode,
                    onSelect: { onAction(.themeModeSelected($0)) }
                )
                BrandThemePickerRow(
                    selectedBrandTheme: uiState.brandTheme,
                    onSelect: { onAction(.brandThemeSelected($0)) }
                )
            }
            Section {
                SettingsRow(item: .account)
                    .contentShape(Rectangle())
                    .onTapGesture { onAction(.accountSettingsTapped) }
                SettingsRow(item: .notification)
                    .contentShape(Rectangle())
                    .onTapGesture { onAction(.notificationSettingsTapped) }
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

private struct ThemePickerRow: View {
    let selectedThemeMode: AppThemeMode

    let onSelect: (AppThemeMode) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 12) {
                Image(systemName: "paintpalette")
                    .foregroundStyle(ConCafeColors.primary)
                    .frame(width: 24)
                VStack(alignment: .leading, spacing: 4) {
                    Text(String(localized: String.LocalizationValue("settings_theme_title"), table: "Localizable"))
                        .font(.subheadline)
                        .bold()
                    Text(String(localized: String.LocalizationValue("settings_theme_desc"), table: "Localizable"))
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer()
            }
            Picker(String(localized: String.LocalizationValue("settings_theme_title"), table: "Localizable"), selection: Binding(
                get: { selectedThemeMode },
                set: { onSelect($0) }
            )) {
                ForEach(AppThemeMode.allCases) { themeMode in
                    Text(themeMode.title)
                        .tag(themeMode)
                }
            }
            .pickerStyle(.segmented)
        }
        .padding(.vertical, 4)
    }
}

private struct BrandThemePickerRow: View {
    let selectedBrandTheme: AppBrandTheme

    let onSelect: (AppBrandTheme) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 12) {
                Image(systemName: "sparkles")
                    .foregroundStyle(ConCafeColors.primary)
                    .frame(width: 24)
                VStack(alignment: .leading, spacing: 4) {
                    Text(String(localized: String.LocalizationValue("settings_brand_theme_title"), table: "Localizable"))
                        .font(.subheadline)
                        .bold()
                    Text(String(localized: String.LocalizationValue("settings_brand_theme_desc"), table: "Localizable"))
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer()
            }
            Picker(String(localized: String.LocalizationValue("settings_brand_theme_title"), table: "Localizable"), selection: Binding(
                get: { selectedBrandTheme },
                set: { onSelect($0) }
            )) {
                ForEach(AppBrandTheme.allCases) { brandTheme in
                    Text(brandTheme.title)
                        .tag(brandTheme)
                }
            }
            .pickerStyle(.segmented)
        }
        .padding(.vertical, 4)
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
        title: String(localized: String.LocalizationValue("settings_account_title"), table: "Localizable"),
        description: String(localized: String.LocalizationValue("settings_account_desc"), table: "Localizable"),
        foregroundColor: ConCafeColors.primary,
        trailingText: nil
    )

    static let notification = SettingsItem(
        icon: "bell.badge",
        title: String(localized: String.LocalizationValue("settings_notification_title"), table: "Localizable"),
        description: String(localized: String.LocalizationValue("settings_notification_desc"), table: "Localizable"),
        foregroundColor: ConCafeColors.primary,
        trailingText: nil
    )

    static func appInfo(version: String) -> SettingsItem {
        SettingsItem(
            icon: "info.circle",
            title: String(localized: String.LocalizationValue("settings_app_info_title"), table: "Localizable"),
            description: String(localized: String.LocalizationValue("settings_app_info_desc"), table: "Localizable"),
            foregroundColor: ConCafeColors.primary,
            trailingText: "v\(version)"
        )
    }

    static let inquiry = SettingsItem(
        icon: "bubble.left.and.text.bubble.right",
        title: String(localized: String.LocalizationValue("settings_inquiry_title"), table: "Localizable"),
        description: String(localized: String.LocalizationValue("settings_inquiry_desc"), table: "Localizable"),
        foregroundColor: ConCafeColors.primary,
        trailingText: nil
    )

    static let privacyPolicy = SettingsItem(
        icon: "lock.doc",
        title: String(localized: String.LocalizationValue("settings_privacy_title"), table: "Localizable"),
        description: String(localized: String.LocalizationValue("settings_privacy_desc"), table: "Localizable"),
        foregroundColor: ConCafeColors.primary,
        trailingText: nil
    )

    static let signOut = SettingsItem(
        icon: "rectangle.portrait.and.arrow.right",
        title: String(localized: String.LocalizationValue("settings_sign_out_title"), table: "Localizable"),
        description: String(localized: String.LocalizationValue("settings_sign_out_desc"), table: "Localizable"),
        foregroundColor: .red,
        trailingText: nil
    )
}

struct SettingsView_Previews: PreviewProvider {
    static var previews: some View {
        SettingsView(onNavigationAction: { _ in })
    }
}
