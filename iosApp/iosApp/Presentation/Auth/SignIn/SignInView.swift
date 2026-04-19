//
//  SignInView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import SwiftUI
import UIKit
import AuthenticationServices

struct SignInView: View {
    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel = SignInViewModel()
    
    var body: some View {
        SignInContentView(
            uiState: viewModel.uiState,
            onBack: {
                onNavigationAction(.navigateBack)
            },
            onResetPassword: {
                onNavigationAction(.navigateToResetPassword)
            },
            onSignUp: {
                onNavigationAction(.navigateToSignUp)
            },
            onAction: viewModel.onAction
        )
        .onReceive(viewModel.event) { event in
            switch event {
            case .signedIn:
                onNavigationAction(.navigateBack)
            }
        }
    }
}

private struct SignInContentView: View {
    let uiState: SignInUiState
    
    let onBack: () -> Void

    let onResetPassword: () -> Void

    let onSignUp: () -> Void
    
    let onAction: (SignInAction) -> Void

    
    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                logoSection
                formCard
                divider
                socialButtons
                footerLinks
            }
            .padding(.horizontal, 20)
            .padding(.top, 12)
            .padding(.bottom, 32)
        }
        .background(
            LinearGradient(
                colors: UITraitCollection.current.userInterfaceStyle == .dark
                    ? [Color(hex: "FFF9FC"), Color(hex: "FFF9FC"), Color(hex: "FFF9FC")]
                    : [Color(hex: "FFF2F7"), Color(hex: "FFFBFD"), Color(hex: "FDEDF4")],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .navigationTitle(String(localized: String.LocalizationValue("signin_title"), table: "Localizable"))
    }
    
    private var logoSection: some View {
        SignInLogoSection()
    }
    
    private var formCard: some View {
        VStack(spacing: 14) {
            VStack(spacing: 12) {
                TextField(String(localized: String.LocalizationValue("signin_email_label"), table: "Localizable"), text: Binding(
                    get: { uiState.email },
                    set: { onAction(.emailChanged($0)) }
                ))
                .textInputAutocapitalization(.never)
                .keyboardType(.emailAddress)
                .autocorrectionDisabled()
                .foregroundStyle(.primary)
                .padding(.horizontal, 16)
                .frame(height: 52)
                .background(UITraitCollection.current.userInterfaceStyle == .dark ? Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }) : Color.white)
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .stroke(
                            UITraitCollection.current.userInterfaceStyle == .dark ? Color.white.opacity(0.16) : Color(hex: "E4DDE5"),
                            lineWidth: 1
                        )
                )
                SecureField(String(localized: String.LocalizationValue("signin_password_label"), table: "Localizable"), text: Binding(
                    get: { uiState.password },
                    set: { onAction(.passwordChanged($0)) }
                ))
                .foregroundStyle(.primary)
                .padding(.horizontal, 16)
                .frame(height: 52)
                .background(UITraitCollection.current.userInterfaceStyle == .dark ? Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .secondarySystemBackground : .white }) : Color.white)
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .stroke(
                            UITraitCollection.current.userInterfaceStyle == .dark ? Color.white.opacity(0.16) : Color(hex: "E4DDE5"),
                            lineWidth: 1
                        )
                )
            }
            if let errorMessage = uiState.errorMessage {
                Text(errorMessage)
                    .font(.caption)
                    .foregroundStyle(Color(hex: "D1436F"))
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            Button {
                onAction(.signInTapped)
            } label: {
                Text(uiState.isLoading ? String(localized: String.LocalizationValue("signin_loading"), table: "Localizable") : String(localized: String.LocalizationValue("signin_submit"), table: "Localizable"))
                    .font(.headline)
                    .foregroundStyle(Color(hex: "2B2330"))
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .background(Color(hex: "FFD1DC"))
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            }
            .disabled(uiState.isLoading)
        }
    }
    
    private var divider: some View {
        SignInDivider()
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
                    onAction(.socialSignInTapped(provider: .kakao))
                }
            )
            SignInSocialButton(
                title: String(localized: String.LocalizationValue("signup_social_google"), table: "Localizable"),
                icon: "google_logo",
                background: .white,
                foreground: Color(hex: "222222"),
                outlined: true,
                action: {
                    onAction(.socialSignInTapped(provider: .google))
                }
            )
            SignInWithAppleButton(
                .signIn,
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
                        onAction(.socialSignInTapped(provider: .apple))
                    }
                }
            )
            .signInWithAppleButtonStyle(.black)
            .frame(height: 52)
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            .padding(.horizontal, 0)
        }
    }
    
    private var footerLinks: some View {
        HStack(spacing: 8) {
            Button(action: onResetPassword) {
                Text(String(localized: String.LocalizationValue("signin_forgot_password"), table: "Localizable"))
            }
            .buttonStyle(.plain)
            Text("|")
            Button(action: onSignUp) {
                Text(String(localized: String.LocalizationValue("signin_sign_up"), table: "Localizable"))
            }
            .buttonStyle(.plain)
        }
        .font(.footnote)
        .foregroundStyle(.secondary)
        .frame(maxWidth: .infinity)
    }
}

struct SignInView_Previews: PreviewProvider {
    static var previews: some View {
        SignInView(onNavigationAction: { _ in })
    }
}
