//
//  ResetPasswordView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/28.
//

import SwiftUI
import UIKit

struct ResetPasswordView: View {
    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel = ResetPasswordViewModel()

    @State private var alertMessage: String?

    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        ScrollView {
            VStack(spacing: 18) {
                VStack(alignment: .leading, spacing: 8) {
                    Image(systemName: "envelope.fill")
                        .foregroundStyle(.white)
                    Text(String(localized: String.LocalizationValue("reset_password_hero_title"), table: "Localizable"))
                        .font(.headline)
                        .bold()
                        .foregroundStyle(.white)
                    Text(String(localized: String.LocalizationValue("reset_password_hero_desc"), table: "Localizable"))
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
                settingsCard(title: String(localized: String.LocalizationValue("reset_password_input_title"), table: "Localizable"), symbol: "envelope") {
                    Text(String(localized: String.LocalizationValue("reset_password_input_desc"), table: "Localizable"))
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    ConCafeFormField(
                        label: String(localized: String.LocalizationValue("reset_password_email_label"), table: "Localizable"),
                        text: Binding(
                            get: { viewModel.uiState.email },
                            set: { viewModel.onAction(.emailChanged($0)) }
                        ),
                        placeholder: String(localized: String.LocalizationValue("reset_password_email_placeholder"), table: "Localizable"),
                        keyboardType: .emailAddress,
                        trailingContent: {
                            Image(systemName: "envelope")
                                .foregroundStyle(colorScheme == .dark ? Color(uiColor: .secondaryLabel) : Color(hex: "B3ACB7"))
                        }
                    )
                }
                settingsCard(title: String(localized: String.LocalizationValue("reset_password_guide_title"), table: "Localizable"), symbol: "checkmark.shield") {
                    guideRow(String(localized: String.LocalizationValue("reset_password_guide_1"), table: "Localizable"))
                    guideRow(String(localized: String.LocalizationValue("reset_password_guide_2"), table: "Localizable"))
                    guideRow(String(localized: String.LocalizationValue("reset_password_guide_3"), table: "Localizable"))
                }
                Button {
                    viewModel.onAction(.submitTapped)
                } label: {
                    Text(viewModel.uiState.isSubmitting ? String(localized: String.LocalizationValue("reset_password_sending"), table: "Localizable") : String(localized: String.LocalizationValue("reset_password_submit"), table: "Localizable"))
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
        .background(colorScheme == .dark ? Color(uiColor: .systemBackground) : Color(hex: "FFFBFD"))
        .onReceive(viewModel.event) { event in
            switch event {
            case .navigateBack:
                onNavigationAction(.navigateBack)
            case .showMessage(let message):
                alertMessage = message
            }
        }
        .alert(String(localized: String.LocalizationValue("reset_password_title"), table: "Localizable"), isPresented: Binding(
            get: { alertMessage != nil },
            set: { if !$0 { alertMessage = nil } }
        )) {
            Button(String(localized: String.LocalizationValue("common_confirm"), table: "Localizable"), role: .cancel) { alertMessage = nil }
        } message: {
            Text(alertMessage ?? "")
        }
        .navigationTitle(String(localized: String.LocalizationValue("reset_password_title"), table: "Localizable"))
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
        .background(colorScheme == .dark ? Color(uiColor: .secondarySystemBackground) : .white)
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .stroke(
                    colorScheme == .dark ? Color.white.opacity(0.12) : Color.clear,
                    lineWidth: 1
                )
        )
    }

    private func guideRow(_ text: String) -> some View {
        HStack(alignment: .top, spacing: 10) {
            Image(systemName: "checkmark.circle.fill")
                .foregroundStyle(Color(hex: "EF6797"))
            Text(text)
                .font(.caption)
                .foregroundStyle(colorScheme == .dark ? Color(uiColor: .secondaryLabel) : Color(hex: "6F6673"))
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(colorScheme == .dark ? Color(uiColor: .tertiarySystemBackground) : Color(hex: "F8F5F6"))
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}

struct ResetPasswordView_Previews: PreviewProvider {
    static var previews: some View {
        ResetPasswordView(onNavigationAction: { _ in })
    }
}
