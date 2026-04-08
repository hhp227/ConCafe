//
//  AccountSettingsView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/14.
//

import SwiftUI
import AuthenticationServices
import Shared

struct AccountSettingsView: View {
    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel = AccountSettingsViewModel()
    
    @State private var alertMessage: String?

    var body: some View {
        AccountSettingsContentView(
            uiState: viewModel.uiState,
            onAction: viewModel.onAction
        )
        .onReceive(viewModel.event) { event in
            switch event {
            case .navigateBack:
                onNavigationAction(.navigateBack)
            case .navigateToMain:
                onNavigationAction(.navigateToMain())
            case .navigateToCastEdit(let cafeId, let castId):
                onNavigationAction(.navigateToCastEdit(cafeId: cafeId, castId: castId))
            case .navigateToChangePassword:
                onNavigationAction(.navigateToChangePassword)
            case .showMessage(let message):
                alertMessage = message
            }
        }
        .alert(String(localized: String.LocalizationValue("account_settings_title"), table: "Localizable"), isPresented: Binding(
            get: { alertMessage != nil },
            set: { if !$0 { alertMessage = nil } }
        )) {
            Button(String(localized: String.LocalizationValue("common_confirm"), table: "Localizable"), role: .cancel) { alertMessage = nil }
        } message: {
            Text(alertMessage ?? "")
        }
        .navigationTitle(String(localized: String.LocalizationValue("account_settings_title"), table: "Localizable"))
        .navigationBarTitleDisplayMode(.inline)
    }
}

private struct AccountSettingsContentView: View {
    let uiState: AccountSettingsUiState
    
    let onAction: (AccountSettingsAction) -> Void

    private var myInfoFeed: Shared.MyInfoFeed? { uiState.myInfoFeed }

    private var currentUser: User? { myInfoFeed?.user }

    private var currentCast: Cast? { myInfoFeed?.castDetail?.cast }

    private var linkedCafeName: String? { myInfoFeed?.castDetail?.cafe.name }

    var body: some View {
        Group {
            if uiState.isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                ScrollView {
                    VStack(spacing: 18) {
                        heroCard
                        errorMessageView
                        basicInfoSection
                        saveSection
                        castStatusSection
                        ownerStatusSection
                        securitySection
                        deleteButton
                    }
                    .padding(16)
                    .padding(.bottom, 24)
                }
            }
        }
        .background(Color(hex: "FFFBFD"))
        .sheet(isPresented: Binding(
            get: { uiState.isDeleteDialogVisible },
            set: { if !$0 { onAction(.dismissDeleteDialogTapped) } }
        )) {
            AccountDeleteConfirmationSheet(
                authProvider: uiState.authProvider,
                passwordText: Binding(
                    get: { uiState.deletePassword },
                    set: { onAction(.deletePasswordChanged($0)) }
                ),
                errorMessage: uiState.deletePasswordErrorMessage,
                onDismiss: { onAction(.dismissDeleteDialogTapped) },
                onDelete: { onAction(.deleteAccountTapped) },
                onAppleTokenReceived: { onAction(.appleDeleteIdTokenReceived($0)) }
            )
        }
    }

    @ViewBuilder
    private var errorMessageView: some View {
        if let errorMessage = uiState.errorMessage {
            Text(errorMessage)
                .font(.subheadline)
                .foregroundStyle(Color.red)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    private var basicInfoSection: some View {
        settingsCard(title: String(localized: String.LocalizationValue("account_settings_section_basic"), table: "Localizable"), symbol: "person.crop.circle") {
            sectionEyebrow(String(localized: String.LocalizationValue("account_settings_section_basic_eyebrow"), table: "Localizable"))
            ConCafeFormField(
                label: String(localized: String.LocalizationValue("account_settings_label_nickname"), table: "Localizable"),
                text: Binding(
                    get: { uiState.nicknameInput },
                    set: { onAction(.nicknameChanged($0)) }
                ),
                placeholder: String(localized: String.LocalizationValue("account_settings_placeholder_nickname"), table: "Localizable")
            )
            infoSummaryCard
            if uiState.role == .cafeOwner {
                Text(String(localized: String.LocalizationValue("account_settings_owner_hint"), table: "Localizable"))
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
    }

    private var infoSummaryCard: some View {
        VStack(spacing: 10) {
            infoRow(label: String(localized: String.LocalizationValue("account_settings_meta_role"), table: "Localizable"), value: uiState.role?.displayText ?? String(localized: String.LocalizationValue("account_settings_role_guest"), table: "Localizable"))
            infoRow(
                label: String(localized: String.LocalizationValue("account_settings_meta_joined_at"), table: "Localizable"),
                value: (currentUser?.createdAt.isEmpty == false ? currentUser?.createdAt : String(localized: String.LocalizationValue("account_settings_meta_joined_pending"), table: "Localizable")) ?? String(localized: String.LocalizationValue("account_settings_meta_joined_pending"), table: "Localizable")
            )
            if uiState.role == .cafeOwner {
                infoRow(
                    label: String(localized: String.LocalizationValue("account_settings_meta_owned_cafe_count_label"), table: "Localizable"),
                    value: String(
                        format: String(localized: String.LocalizationValue("account_settings_meta_owned_cafe_count_value"), table: "Localizable"),
                        locale: Locale.current,
                        myInfoFeed?.ownedCafes.count ?? 0
                    )
                )
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(hex: "F8F5F6"))
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
    }

    private var saveSection: some View {
        settingsCard(title: String(localized: String.LocalizationValue("account_settings_section_save"), table: "Localizable"), symbol: "square.and.arrow.down") {
            sectionEyebrow(String(localized: String.LocalizationValue("account_settings_section_save_eyebrow"), table: "Localizable"))
            primaryButton(title: String(localized: String.LocalizationValue("account_settings_save_user"), table: "Localizable")) {
                onAction(.saveUserInfoTapped)
            }
        }
    }

    @ViewBuilder
    private var castStatusSection: some View {
        if uiState.role == .cast {
            settingsCard(title: String(localized: String.LocalizationValue("account_settings_section_cast_status"), table: "Localizable"), symbol: "person.crop.rectangle.stack") {
                sectionEyebrow(String(localized: String.LocalizationValue("account_settings_section_cast_status_eyebrow"), table: "Localizable"))
                Text(castDescriptionText)
                    .font(.subheadline)
                    .foregroundStyle(Color(hex: "6F6673"))
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
    }

    @ViewBuilder
    private var ownerStatusSection: some View {
        if uiState.role == .cafeOwner || uiState.role == .admin {
            settingsCard(title: String(localized: String.LocalizationValue("account_settings_section_owner_status"), table: "Localizable"), symbol: "storefront") {
                sectionEyebrow(String(localized: String.LocalizationValue("account_settings_section_owner_status_eyebrow"), table: "Localizable"))
                Text(ownerStatusText)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
    }

    private var securitySection: some View {
        settingsCard(title: String(localized: String.LocalizationValue("account_settings_section_security"), table: "Localizable"), symbol: "lock.shield") {
            sectionEyebrow(String(localized: String.LocalizationValue("account_settings_section_security_eyebrow"), table: "Localizable"))
            if uiState.canChangePassword {
                linkedDestinationCard(
                    title: String(localized: String.LocalizationValue("account_settings_link_change_password_title"), table: "Localizable"),
                    description: String(localized: String.LocalizationValue("account_settings_link_change_password_desc"), table: "Localizable"),
                    supporting: String(localized: String.LocalizationValue("account_settings_link_default_supporting"), table: "Localizable"),
                    symbol: "lock.shield",
                    onTap: { onAction(.openChangePasswordTapped) }
                )
            }
            if uiState.role == .cast {
                linkedDestinationCard(
                    title: String(localized: String.LocalizationValue("account_settings_link_cast_edit_title"), table: "Localizable"),
                    description: castLinkDescription,
                    supporting: linkedCafeName ?? String(localized: String.LocalizationValue("account_settings_link_cast_edit_supporting"), table: "Localizable"),
                    symbol: "person.text.rectangle",
                    onTap: { onAction(.openCastEditTapped) }
                )
            }
        }
    }

    private var deleteButton: some View {
        Button {
            onAction(.showDeleteDialogTapped)
        } label: {
            Text(uiState.isDeleteRequested ? String(localized: String.LocalizationValue("account_settings_delete_requested"), table: "Localizable") : String(localized: String.LocalizationValue("account_settings_delete"), table: "Localizable"))
                .font(.footnote)
                .foregroundStyle(uiState.isDeleteRequested ? Color(hex: "B84473") : Color(hex: "8E8794"))
                .frame(maxWidth: .infinity)
        }
        .buttonStyle(.plain)
    }

    private var castDescriptionText: String {
        (currentCast?.desc.isEmpty == false ? currentCast?.desc : nil)
        ?? String(localized: String.LocalizationValue("account_settings_cast_desc_empty"), table: "Localizable")
    }

    private var ownerStatusText: String {
        if uiState.role == .admin {
            return String(localized: String.LocalizationValue("account_settings_admin_status_desc"), table: "Localizable")
        }
        return String(
            format: String(localized: String.LocalizationValue("account_settings_owner_status_desc"), table: "Localizable"),
            locale: Locale.current,
            myInfoFeed?.ownedCafes.count ?? 0
        )
    }

    private var castLinkDescription: String {
        [currentCast?.name ?? "", currentCast?.conceptRole ?? ""]
            .filter { !$0.isEmpty }
            .joined(separator: " · ")
    }

    private var heroCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(currentUser?.nickname.isEmpty == false ? (currentUser?.nickname ?? "") : String(localized: String.LocalizationValue("account_settings_default_user_name"), table: "Localizable"))
                .font(.title3)
                .bold()
                .foregroundStyle(.white)
            Text(currentUser?.email.isEmpty == false ? (currentUser?.email ?? "") : String(localized: String.LocalizationValue("account_settings_no_login_info"), table: "Localizable"))
                .font(.subheadline)
                .foregroundStyle(.white.opacity(0.92))
            Text(uiState.role?.roleSummary ?? "")
                .font(.subheadline)
                .foregroundStyle(.white.opacity(0.92))
                .padding(.top, 4)
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            LinearGradient(
                colors: [Color(hex: "EF6797"), Color(hex: "F7A0C1")],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
    }

    private func sectionEyebrow(_ text: String) -> some View {
        Text(text)
            .font(.caption)
            .foregroundStyle(Color(hex: "8E8794"))
            .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func settingsCard<Content: View>(
        title: String,
        symbol: String,
        @ViewBuilder content: () -> Content
    ) -> some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 10) {
                Image(systemName: symbol)
                    .foregroundStyle(Color(hex: "EF6797"))
                Text(title)
                    .font(.title3)
                    .bold()
            }
            content()
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.white)
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
    }

    private func linkedDestinationCard(
        title: String,
        description: String,
        supporting: String,
        symbol: String,
        onTap: @escaping () -> Void
    ) -> some View {
        Button(action: onTap) {
            HStack(spacing: 14) {
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .fill(Color(hex: "FFF1F7"))
                    .frame(width: 46, height: 46)
                    .overlay(
                        Image(systemName: symbol)
                            .foregroundStyle(Color(hex: "EF6797"))
                    )
                VStack(alignment: .leading, spacing: 4) {
                    Text(title)
                        .font(.subheadline)
                        .bold()
                        .foregroundStyle(Color.primary)
                    if !description.isEmpty {
                        Text(description)
                            .font(.subheadline)
                            .foregroundStyle(Color(hex: "302732"))
                    }
                    Text(supporting)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(Color(hex: "B3ACB7"))
            }
            .padding(18)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(.white)
            .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        }
        .buttonStyle(.plain)
    }

    private func primaryButton(title: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(.headline)
                .fontWeight(.bold)
                .foregroundStyle(Color(hex: "2B2330"))
                .frame(maxWidth: .infinity)
                .frame(height: 56)
                .background(Color(hex: "FFD1DC"))
                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        }
        .buttonStyle(.plain)
    }

    private func infoRow(label: String, value: String) -> some View {
        HStack {
            Text(label)
                .font(.caption)
                .foregroundStyle(.secondary)
            Spacer()
            Text(value)
                .font(.subheadline)
                .bold()
                .foregroundStyle(Color.primary)
        }
    }
}

private extension UserRole {
    var displayText: String {
        switch self {
        case .admin:
            return String(localized: String.LocalizationValue("account_settings_role_admin"), table: "Localizable")
        case .cafeOwner:
            return String(localized: String.LocalizationValue("account_settings_role_owner"), table: "Localizable")
        case .cast:
            return String(localized: String.LocalizationValue("account_settings_role_cast"), table: "Localizable")
        case .visitor:
            return String(localized: String.LocalizationValue("account_settings_role_visitor"), table: "Localizable")
        default:
            return String(localized: String.LocalizationValue("account_settings_role_guest"), table: "Localizable")
        }
    }

    var roleSummary: String {
        switch self {
        case .cast:
            return String(localized: String.LocalizationValue("account_settings_role_summary_cast"), table: "Localizable")
        case .cafeOwner:
            return String(localized: String.LocalizationValue("account_settings_role_summary_owner"), table: "Localizable")
        case .admin:
            return String(localized: String.LocalizationValue("account_settings_role_summary_admin"), table: "Localizable")
        case .visitor:
            return String(localized: String.LocalizationValue("account_settings_role_summary_visitor"), table: "Localizable")
        default:
            return String(localized: String.LocalizationValue("account_settings_role_summary_guest"), table: "Localizable")
        }
    }
}

private struct AccountDeleteConfirmationSheet: View {
    let authProvider: AuthProvider

    @Binding var passwordText: String

    let errorMessage: String?

    let onDismiss: () -> Void

    let onDelete: () -> Void

    let onAppleTokenReceived: (String) -> Void

    var body: some View {
        NavigationView {
            VStack(alignment: .leading, spacing: 18) {
                VStack(alignment: .leading, spacing: 8) {
                    Text(String(localized: String.LocalizationValue("account_settings_delete"), table: "Localizable"))
                        .font(.title3.bold())
                    Text(deleteDescription)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
                if authProvider == .email || authProvider == .unknown {
                    ConCafeFormField(
                        label: String(localized: String.LocalizationValue("account_settings_delete_password_label"), table: "Localizable"),
                        text: $passwordText,
                        placeholder: String(localized: String.LocalizationValue("account_settings_delete_password_placeholder"), table: "Localizable"),
                        isSecure: true
                    )
                    .textInputAutocapitalization(.never)
                } else if authProvider == .apple {
                    SignInWithAppleButton(.continue) { request in
                        request.requestedScopes = []
                    } onCompletion: { result in
                        if case let .success(authorization) = result,
                           let credential = authorization.credential as? ASAuthorizationAppleIDCredential,
                           let identityTokenData = credential.identityToken,
                           let identityToken = String(data: identityTokenData, encoding: .utf8) {
                            onAppleTokenReceived(identityToken)
                        }
                    }
                    .signInWithAppleButtonStyle(.black)
                    .frame(height: 52)
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                }
                if let errorMessage, !errorMessage.isEmpty {
                    Text(errorMessage)
                        .font(.caption)
                        .foregroundStyle(Color.red)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                HStack(spacing: 12) {
                    Button(action: onDismiss) {
                        Text(String(localized: String.LocalizationValue("common_cancel"), table: "Localizable"))
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(Color(hex: "6F6673"))
                            .frame(maxWidth: .infinity)
                            .frame(height: 52)
                            .background(Color(hex: "F4EDF1"))
                            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                    }
                    .buttonStyle(.plain)
                    if authProvider != .apple {
                        Button(action: onDelete) {
                            Text(String(localized: String.LocalizationValue("account_settings_delete"), table: "Localizable"))
                                .font(.subheadline.weight(.bold))
                                .foregroundStyle(.white)
                                .frame(maxWidth: .infinity)
                                .frame(height: 52)
                                .background(Color(hex: "C9527E"))
                                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                        }
                        .buttonStyle(.plain)
                    }
                }
                Spacer()
            }
            .padding(20)
            .background(Color(hex: "FFFBFD"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(String(localized: String.LocalizationValue("common_close"), table: "Localizable")) {
                        onDismiss()
                    }
                }
            }
        }
    }

    private var deleteDescription: String {
        switch authProvider {
        case .google:
            return String(localized: String.LocalizationValue("account_settings_delete_dialog_desc_google"), table: "Localizable")
        case .kakao:
            return String(localized: String.LocalizationValue("account_settings_delete_dialog_desc_kakao"), table: "Localizable")
        case .apple:
            return String(localized: String.LocalizationValue("account_settings_delete_dialog_desc_apple"), table: "Localizable")
        default:
            return String(localized: String.LocalizationValue("account_settings_delete_dialog_desc"), table: "Localizable")
        }
    }
}

struct AccountSettingsView_Previews: PreviewProvider {
    static var previews: some View {
        AccountSettingsView(onNavigationAction: { _ in })
    }
}
