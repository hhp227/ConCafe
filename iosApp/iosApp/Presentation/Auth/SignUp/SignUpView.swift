//
//  SignUpView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import SwiftUI
import UIKit
import Shared
import AuthenticationServices

struct SignUpView: View {
    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel = SignUpViewModel()

    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        SignUpContentView(
            uiState: viewModel.uiState,
            onAction: viewModel.onAction
        )
        .onReceive(viewModel.event) { event in
            switch event {
            case .signedUp:
                onNavigationAction(.navigateToMain())
            case .navigateBack:
                onNavigationAction(.navigateBack)
            }
        }
        .onDisappear {
            viewModel.onAction(.cleanupIncompleteAccount)
        }
        .onChange(of: scenePhase) { phase in
            if phase == .background {
                viewModel.onAction(.cleanupIncompleteAccount)
            }
        }
    }
}

private struct SignUpContentView: View {
    let uiState: SignUpUiState

    let onAction: (SignUpAction) -> Void

    @Environment(\.colorScheme) private var colorScheme

    private var filteredCafes: [Cafe] {
        if uiState.cafeSearchQuery.isEmpty {
            return uiState.cafes
        }
        return uiState.cafes.filter {
            $0.name.localizedCaseInsensitiveContains(uiState.cafeSearchQuery) ||
            $0.region.city.localizedCaseInsensitiveContains(uiState.cafeSearchQuery)
        }
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                if uiState.step == .selectType {
                    SignInLogoSection()
                    introSection
                    ForEach(SignUpUiState.UserType.allCases, id: \.title) { type in
                        userTypeCard(type: type)
                    }
                    footer
                } else {
                    if let selectedUserType = uiState.selectedUserType {
                        formHeader(selectedUserType)
                        formSection(selectedUserType)
                        footer
                    }
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, 12)
            .padding(.bottom, 32)
        }
        .background(
            LinearGradient(
                colors: colorScheme == .dark
                    ? [Color(uiColor: .systemBackground), Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }), Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .tertiarySystemBackground : .white })]
                    : [Color(hex: "FFF2F7"), Color(hex: "FFFBFD"), Color(hex: "FDEDF4")],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .navigationTitle(String(localized: String.LocalizationValue("signup_title"), table: "Localizable"))
    }

    private var introSection: some View {
        VStack(spacing: 8) {
            Text(String(localized: String.LocalizationValue("signup_select_type_title"), table: "Localizable"))
                .font(.title2.weight(.bold))
            Text(String(localized: String.LocalizationValue("signup_select_type_subtitle"), table: "Localizable"))
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
    }

    private var footer: some View {
        HStack(spacing: 6) {
            Text(String(localized: String.LocalizationValue("signup_footer_has_account"), table: "Localizable"))
                .foregroundStyle(.secondary)
            Button(String(localized: String.LocalizationValue("signup_footer_sign_in"), table: "Localizable")) {
                onAction(.signInInsteadTapped)
            }
            .font(.system(size: 16, weight: .semibold))
        }
        .font(.footnote)
        .frame(maxWidth: .infinity)
    }

    private func userTypeCard(type: SignUpUiState.UserType) -> some View {
        let accentColor = accentColor(for: type)

        return Button {
            onAction(.userTypeTapped(type))
        } label: {
            HStack(alignment: .top, spacing: 16) {
                ZStack {
                    Circle()
                        .fill(accentColor)
                        .frame(width: 52, height: 52)
                    Image(systemName: iconName(for: type))
                        .font(.system(size: 22, weight: .semibold))
                        .foregroundStyle(.white)
                }
                VStack(alignment: .leading, spacing: 6) {
                    Text(type.title)
                        .font(.headline.weight(.bold))
                        .foregroundStyle(.primary)
                    Text(type.subtitle)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                    Text(type.badge)
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(Color(hex: "DA4E84"))
                }
                Spacer()
            }
            .padding(20)
            .background(colorScheme == .dark ? Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }) : Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 24, style: .continuous)
                    .stroke(
                        colorScheme == .dark ? Color.white.opacity(0.16) : Color(hex: "E7DFE8"),
                        lineWidth: 1
                    )
            )
        }
    }

    private func formHeader(_ type: SignUpUiState.UserType) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 8) {
                Image(systemName: iconName(for: type))
                    .foregroundStyle(.white)
                Text(type.title)
                    .font(.headline.weight(.bold))
                    .foregroundStyle(.white)
            }
            Text(description(for: type))
                .font(.subheadline)
                .foregroundStyle(.white.opacity(0.92))
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(20)
        .background(
            LinearGradient(
                colors: gradientColors(for: type),
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
    }

    private func formSection(_ type: SignUpUiState.UserType) -> some View {
        VStack(spacing: 14) {
            textField(
                title: String(localized: String.LocalizationValue("signup_email_label"), table: "Localizable"),
                placeholder: String(localized: String.LocalizationValue("signup_email_placeholder"), table: "Localizable"),
                text: Binding(
                    get: { uiState.email },
                    set: { onAction(.emailChanged($0)) }
                ),
                keyboardType: .emailAddress
            )
            if type == .cafeOwner {
                textField(
                    title: String(localized: String.LocalizationValue("signup_name_label"), table: "Localizable"),
                    placeholder: String(localized: String.LocalizationValue("signup_name_placeholder"), table: "Localizable"),
                    text: Binding(
                        get: { uiState.name },
                        set: { onAction(.nameChanged($0)) }
                    )
                )
                phoneVerificationSection
                cafeSelectionSection(
                    title: String(localized: String.LocalizationValue("signup_owner_cafe_link_title"), table: "Localizable"),
                    placeholder: String(localized: String.LocalizationValue("signup_owner_cafe_link_placeholder"), table: "Localizable")
                )
                ownerCafeGuideCard
            } else {
                textField(
                    title: type == .cast ? String(localized: String.LocalizationValue("signup_cast_nickname_label"), table: "Localizable") : String(localized: String.LocalizationValue("signup_nickname_label"), table: "Localizable"),
                    placeholder: type == .cast ? String(localized: String.LocalizationValue("signup_cast_nickname_placeholder"), table: "Localizable") : String(localized: String.LocalizationValue("signup_nickname_placeholder"), table: "Localizable"),
                    text: Binding(
                        get: { uiState.nickname },
                        set: { onAction(.nicknameChanged($0)) }
                    )
                )
                if type == .cast {
                    cafeSelectionSection(
                        title: String(localized: String.LocalizationValue("signup_cast_cafe_title"), table: "Localizable"),
                        placeholder: String(localized: String.LocalizationValue("signup_cast_cafe_placeholder"), table: "Localizable")
                    )
                    Text(String(localized: String.LocalizationValue("signup_cast_cafe_approval_required"), table: "Localizable"))
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
            }
            if !uiState.isSocialFlow {
                secureField(
                    title: String(localized: String.LocalizationValue("signup_password_label"), table: "Localizable"),
                    placeholder: String(localized: String.LocalizationValue("signup_password_placeholder"), table: "Localizable"),
                    text: Binding(
                        get: { uiState.password },
                        set: { onAction(.passwordChanged($0)) }
                    )
                )
                secureField(
                    title: String(localized: String.LocalizationValue("signup_confirm_password_label"), table: "Localizable"),
                    placeholder: String(localized: String.LocalizationValue("signup_confirm_password_placeholder"), table: "Localizable"),
                    text: Binding(
                        get: { uiState.confirmPassword },
                        set: { onAction(.confirmPasswordChanged($0)) }
                    )
                )
            }
            if let errorMessage = uiState.errorMessage {
                Text(errorMessage)
                    .font(.caption)
                    .foregroundStyle(Color(hex: "D1436F"))
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            if let infoMessage = uiState.infoMessage {
                Text(infoMessage)
                    .font(.caption)
                    .foregroundStyle(Color(hex: "2E8B57"))
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            Button {
                onAction(.submitTapped)
            } label: {
                Text(uiState.isLoading ? String(localized: String.LocalizationValue("signup_processing"), table: "Localizable") : type.submitLabel)
                    .font(.headline)
                    .foregroundStyle(Color(hex: "2B2330"))
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .background(Color(hex: "FFD1DC"))
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            }
            .disabled(uiState.isLoading)
            if type == .cast {
                Text(String(localized: String.LocalizationValue("signup_cast_after_signup_notice"), table: "Localizable"))
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
            }
            if !uiState.hasAuthenticatedSocialAccount {
                SignInDivider()
                socialButtons
            }
        }
    }

    private var phoneVerificationSection: some View {
        VStack(spacing: 10) {
            HStack(alignment: .bottom, spacing: 10) {
                VStack(alignment: .leading, spacing: 8) {
                    Text(String(localized: String.LocalizationValue("signup_phone_label"), table: "Localizable"))
                        .font(.subheadline.weight(.semibold))
                    PhoneTextField(
                        text: Binding(
                            get: { uiState.phone },
                            set: { onAction(.phoneChanged($0)) }
                        ),
                        placeholder: String(localized: String.LocalizationValue("signup_phone_placeholder"), table: "Localizable")
                    )
                    .frame(height: 52)
                    .padding(.horizontal, 16)
                    .background(colorScheme == .dark ? Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }) : Color.white)
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .stroke(colorScheme == .dark ? Color.white.opacity(0.16) : Color(hex: "E4DDE5"), lineWidth: 1)
                    )
                }
                Button(uiState.isPhoneVerified ? String(localized: String.LocalizationValue("signup_phone_verified"), table: "Localizable") : String(localized: String.LocalizationValue("signup_phone_request"), table: "Localizable")) {
                    onAction(.sendVerificationTapped)
                }
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(.white)
                .frame(height: 52)
                .padding(.horizontal, 16)
                .background(Color(hex: "EF6797"))
                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                .disabled(uiState.isPhoneVerified || uiState.phone.isEmpty)
            }
            if uiState.hasRequestedVerification && !uiState.isPhoneVerified {
                HStack(alignment: .bottom, spacing: 10) {
                    textField(
                        title: String(localized: String.LocalizationValue("signup_verification_code_label"), table: "Localizable"),
                        placeholder: String(localized: String.LocalizationValue("signup_verification_code_placeholder"), table: "Localizable"),
                        text: Binding(
                            get: { uiState.verificationCode },
                            set: { onAction(.verificationCodeChanged($0)) }
                        ),
                        keyboardType: .numberPad
                    )
                    Button(String(localized: String.LocalizationValue("signup_verification_confirm"), table: "Localizable")) {
                        onAction(.verifyCodeTapped)
                    }
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Color(hex: "6B3050"))
                    .frame(height: 52)
                    .padding(.horizontal, 20)
                    .background(Color(hex: "F7D2E1"))
                    .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                }
            }
            if uiState.isPhoneVerified {
                HStack(spacing: 8) {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundStyle(Color(hex: "2E8B57"))
                    Text(String(localized: String.LocalizationValue("signup_phone_verified_message"), table: "Localizable"))
                        .foregroundStyle(Color(hex: "2E8B57"))
                        .font(.subheadline.weight(.semibold))
                    Spacer()
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 12)
                .background(Color(hex: "EAF8EF"))
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            }
        }
    }

    private var ownerCafeGuideCard: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(String(localized: String.LocalizationValue("signup_owner_cafe_guide_title"), table: "Localizable"))
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Color(hex: "5F3AA2"))
            Text(String(localized: String.LocalizationValue("signup_owner_cafe_guide_message"), table: "Localizable"))
                .font(.caption)
                .foregroundStyle(Color(hex: "6B5A82"))
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(14)
        .background(Color(hex: "F6F0FF"))
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(Color(hex: "E6D9FA"), lineWidth: 1)
        )
    }

    private func cafeSelectionSection(title: String, placeholder: String) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(title)
                .font(.subheadline.weight(.semibold))
            Button {
                onAction(.toggleCafeSearchTapped)
            } label: {
                HStack {
                    Text(uiState.selectedCafe?.name ?? placeholder)
                        .foregroundStyle(uiState.selectedCafe == nil ? .secondary : .primary)
                    Spacer()
                    Image(systemName: "magnifyingglass")
                        .foregroundStyle(.secondary)
                }
                .padding(.horizontal, 16)
                .frame(height: 52)
                .background(colorScheme == .dark ? Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }) : Color.white)
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .stroke(colorScheme == .dark ? Color.white.opacity(0.16) : Color(hex: "E4DDE5"), lineWidth: 1)
                )
            }
            if uiState.selectedCafe != nil {
                Button(String(localized: String.LocalizationValue("signup_clear_selected_cafe"), table: "Localizable")) {
                    onAction(.clearCafeTapped)
                }
                .font(.footnote.weight(.semibold))
                .buttonStyle(.plain)
            }
            if uiState.isCafeSearchVisible {
                VStack(spacing: 0) {
                    textField(
                        title: String(localized: String.LocalizationValue("signup_search_cafe_label"), table: "Localizable"),
                        placeholder: String(localized: String.LocalizationValue("signup_search_cafe_placeholder"), table: "Localizable"),
                        text: Binding(
                            get: { uiState.cafeSearchQuery },
                            set: { onAction(.cafeSearchQueryChanged($0)) }
                        )
                    )
                    .padding(12)
                    if filteredCafes.isEmpty {
                        Text(String(localized: String.LocalizationValue("signup_search_no_results"), table: "Localizable"))
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 20)
                    } else {
                        VStack(spacing: 0) {
                            ForEach(filteredCafes, id: \.id) { cafe in
                                Button {
                                    onAction(.cafeTapped(cafe))
                                } label: {
                                    HStack {
                                        VStack(alignment: .leading, spacing: 4) {
                                            Text(cafe.name)
                                                .font(.subheadline.weight(.semibold))
                                                .foregroundStyle(.primary)
                                            Text(cafe.region.city)
                                                .font(.caption)
                                                .foregroundStyle(.secondary)
                                        }
                                        Spacer()
                                        if cafe.approved {
                                            Text(String(localized: String.LocalizationValue("signup_cafe_verified_badge"), table: "Localizable"))
                                                .font(.caption2.weight(.bold))
                                                .foregroundStyle(.white)
                                                .padding(.horizontal, 10)
                                                .padding(.vertical, 6)
                                                .background(Color(hex: "EF6797"))
                                                .clipShape(Capsule())
                                        }
                                    }
                                    .padding(.horizontal, 16)
                                    .padding(.vertical, 14)
                                }
                                if cafe.id != filteredCafes.last?.id {
                                    Divider()
                                        .padding(.leading, 16)
                                }
                            }
                        }
                    }
                }
                .background(colorScheme == .dark ? Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }) : Color.white)
                .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var socialButtons: some View {
        VStack(spacing: 12) {
            SignInSocialButton(
                title: String(localized: String.LocalizationValue("signup_social_kakao"), table: "Localizable"),
                icon: "kakao_icon",
                background: Color(hex: "FEE500"),
                foreground: .primary,
                outlined: false,
                action: {
                    onAction(.socialSignUpTapped(provider: .kakao))
                }
            )
            SignInSocialButton(
                title: String(localized: String.LocalizationValue("signup_social_google"), table: "Localizable"),
                icon: "google_logo",
                background: .white,
                foreground: Color(hex: "222222"),
                outlined: true,
                action: {
                    onAction(.socialSignUpTapped(provider: .google))
                }
            )
            SignInWithAppleButton(
                .signUp,
                onRequest: { request in
                    request.requestedScopes = [.fullName, .email]
                },
                onCompletion: { result in
                    if case let .success(authorization) = result,
                       let credential = authorization.credential as? ASAuthorizationAppleIDCredential,
                       let identityTokenData = credential.identityToken,
                       let identityToken = String(data: identityTokenData, encoding: .utf8) {
                        onAction(.appleIdTokenReceived(identityToken))
                    } else {
                        onAction(.socialSignUpTapped(provider: .apple))
                    }
                }
            )
            .signInWithAppleButtonStyle(.black)
            .frame(height: 52)
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            .padding(.horizontal, 0)
        }
    }

    private func textField(
        title: String,
        placeholder: String,
        text: Binding<String>,
        keyboardType: UIKeyboardType = .default
    ) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.subheadline.weight(.semibold))
            TextField(placeholder, text: text)
                .keyboardType(keyboardType)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .foregroundStyle(.primary)
                .padding(.horizontal, 16)
                .frame(height: 52)
                .background(colorScheme == .dark ? Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }) : Color.white)
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .stroke(colorScheme == .dark ? Color.white.opacity(0.16) : Color(hex: "E4DDE5"), lineWidth: 1)
                )
        }
    }

    private func secureField(
        title: String,
        placeholder: String,
        text: Binding<String>
    ) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.subheadline.weight(.semibold))
            SecureField(placeholder, text: text)
                .foregroundStyle(.primary)
                .padding(.horizontal, 16)
                .frame(height: 52)
                .background(colorScheme == .dark ? Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }) : Color.white)
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .stroke(colorScheme == .dark ? Color.white.opacity(0.16) : Color(hex: "E4DDE5"), lineWidth: 1)
                )
        }
    }

    private func accentColor(for type: SignUpUiState.UserType) -> Color {
        switch type {
        case .visitor:
            return Color(hex: "4F8EF7")
        case .cast:
            return Color(hex: "F06292")
        case .cafeOwner:
            return Color(hex: "8B5CF6")
        }
    }

    private func gradientColors(for type: SignUpUiState.UserType) -> [Color] {
        switch type {
        case .visitor:
            return [Color(hex: "60A5FA"), Color(hex: "3B82F6")]
        case .cast:
            return [Color(hex: "F472B6"), Color(hex: "EC4899")]
        case .cafeOwner:
            return [Color(hex: "A78BFA"), Color(hex: "8B5CF6")]
        }
    }

    private func iconName(for type: SignUpUiState.UserType) -> String {
        switch type {
        case .visitor:
            return "person.fill"
        case .cast:
            return "sparkles"
        case .cafeOwner:
            return "storefront.fill"
        }
    }

    private func description(for type: SignUpUiState.UserType) -> String {
        switch type {
        case .visitor:
            return String(localized: String.LocalizationValue("signup_desc_visitor"), table: "Localizable")
        case .cast:
            return String(localized: String.LocalizationValue("signup_desc_cast"), table: "Localizable")
        case .cafeOwner:
            return String(localized: String.LocalizationValue("signup_desc_owner"), table: "Localizable")
        }
    }
}

struct SignUpView_Previews: PreviewProvider {
    static var previews: some View {
        SignUpView(onNavigationAction: { _ in })
    }
}
