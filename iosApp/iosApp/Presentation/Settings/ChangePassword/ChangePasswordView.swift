//
//  ChangePasswordView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/15.
//

import SwiftUI

struct ChangePasswordView: View {
    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel = ChangePasswordViewModel()

    @State private var alertMessage: String?

    var body: some View {
        ScrollView {
            VStack(spacing: 18) {
                VStack(alignment: .leading, spacing: 8) {
                    Image(systemName: "lock.fill")
                        .foregroundStyle(.white)
                    Text(String(localized: String.LocalizationValue("changepw_hero_title"), table: "Localizable"))
                        .font(.headline)
                        .bold()
                        .foregroundStyle(.white)
                    Text(String(localized: String.LocalizationValue("changepw_hero_desc"), table: "Localizable"))
                        .font(.subheadline)
                        .foregroundStyle(.white.opacity(0.92))
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
                settingsCard(title: String(localized: String.LocalizationValue("changepw_info_title"), table: "Localizable"), symbol: "key.horizontal") {
                    Text(String(localized: String.LocalizationValue("changepw_info_desc"), table: "Localizable"))
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    ConCafeFormField(
                        label: String(localized: String.LocalizationValue("changepw_current_password_label"), table: "Localizable"),
                        text: Binding(
                            get: { viewModel.uiState.currentPassword },
                            set: { viewModel.onAction(.currentPasswordChanged($0)) }
                        ),
                        placeholder: String(localized: String.LocalizationValue("changepw_current_password_placeholder"), table: "Localizable")
                    )
                    ConCafeFormField(
                        label: String(localized: String.LocalizationValue("changepw_new_password_label"), table: "Localizable"),
                        text: Binding(
                            get: { viewModel.uiState.newPassword },
                            set: { viewModel.onAction(.newPasswordChanged($0)) }
                        ),
                        placeholder: String(localized: String.LocalizationValue("changepw_new_password_placeholder"), table: "Localizable")
                    )
                    ConCafeFormField(
                        label: String(localized: String.LocalizationValue("changepw_new_password_confirm_label"), table: "Localizable"),
                        text: Binding(
                            get: { viewModel.uiState.confirmPassword },
                            set: { viewModel.onAction(.confirmPasswordChanged($0)) }
                        ),
                        placeholder: String(localized: String.LocalizationValue("changepw_new_password_confirm_placeholder"), table: "Localizable")
                    )
                }
                settingsCard(title: String(localized: String.LocalizationValue("changepw_guide_title"), table: "Localizable"), symbol: "checkmark.shield") {
                    guideRow(String(localized: String.LocalizationValue("changepw_guide_1"), table: "Localizable"))
                    guideRow(String(localized: String.LocalizationValue("changepw_guide_2"), table: "Localizable"))
                    guideRow(String(localized: String.LocalizationValue("changepw_guide_3"), table: "Localizable"))
                }
                Button {
                    viewModel.onAction(.submitTapped)
                } label: {
                    Text(viewModel.uiState.isSubmitting ? String(localized: String.LocalizationValue("changepw_submitting"), table: "Localizable") : String(localized: String.LocalizationValue("changepw_submit"), table: "Localizable"))
                        .font(.headline)
                        .fontWeight(.bold)
                        .foregroundStyle(Color(hex: "2B2330"))
                        .frame(maxWidth: .infinity)
                        .frame(height: 56)
                        .background(Color(hex: "FFD1DC"))
                        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                }
                .buttonStyle(.plain)
                .disabled(viewModel.uiState.isSubmitting)
            }
            .padding(16)
            .padding(.bottom, 24)
        }
        .background(Color(hex: "FFFBFD"))
        .onReceive(viewModel.event) { event in
            switch event {
            case .navigateBack:
                onNavigationAction(.navigateBack)
            case .showMessage(let message):
                alertMessage = message
            }
        }
        .alert(String(localized: String.LocalizationValue("changepw_title"), table: "Localizable"), isPresented: Binding(
            get: { alertMessage != nil },
            set: { if !$0 { alertMessage = nil } }
        )) {
            Button(String(localized: String.LocalizationValue("common_confirm"), table: "Localizable"), role: .cancel) { alertMessage = nil }
        } message: {
            Text(alertMessage ?? "")
        }
        .navigationTitle(String(localized: String.LocalizationValue("changepw_title"), table: "Localizable"))
        .navigationBarTitleDisplayMode(.inline)
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
                    .font(.headline)
                    .bold()
            }
            content()
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.white)
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
    }

    private func guideRow(_ text: String) -> some View {
        HStack(alignment: .top, spacing: 10) {
            Image(systemName: "checkmark.circle.fill")
                .foregroundStyle(Color(hex: "EF6797"))
            Text(text)
                .font(.caption)
                .foregroundStyle(Color(hex: "6F6673"))
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(hex: "F8F5F6"))
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}

struct ChangePasswordView_Previews: PreviewProvider {
    static var previews: some View {
        ChangePasswordView(onNavigationAction: { _ in })
    }
}
