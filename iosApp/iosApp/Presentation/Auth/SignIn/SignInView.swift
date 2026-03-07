//
//  SignInView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import SwiftUI

struct SignInView: View {
    let onNavigationAction: (NavigationAction) -> Void
    
    @Environment(\.dismiss) private var dismiss
    
    @StateObject private var viewModel = SignInViewModel()
    
    var body: some View {
        SignInContentView(
            uiState: viewModel.uiState,
            onBack: {
                dismiss()
            },
            onAction: viewModel.onAction
        )
        .onReceive(viewModel.event) { event in
            switch event {
            case .signedIn:
                dismiss()
            }
        }
    }
}

private struct SignInContentView: View {
    let uiState: SignInUiState
    
    let onBack: () -> Void
    
    let onAction: (SignInAction) -> Void
    
    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                header
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
                colors: [Color(hex: "FFF2F7"), Color(hex: "FFFBFD"), Color(hex: "FDEDF4")],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .navigationBarBackButtonHidden(true)
    }
    
    private var header: some View {
        HStack(spacing: 12) {
            Button(action: onBack) {
                Image(systemName: "chevron.left")
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundStyle(.primary)
                    .frame(width: 40, height: 40)
                    .background(.white.opacity(0.92))
                    .clipShape(Circle())
            }
            Text("로그인")
                .font(.title2.weight(.bold))
            Spacer()
        }
    }
    
    private var logoSection: some View {
        SignInLogoSection()
    }
    
    private var formCard: some View {
        VStack(spacing: 14) {
            VStack(spacing: 12) {
                TextField("이메일", text: Binding(
                    get: { uiState.email },
                    set: { onAction(.emailChanged($0)) }
                ))
                .textInputAutocapitalization(.never)
                .keyboardType(.emailAddress)
                .autocorrectionDisabled()
                .padding(.horizontal, 16)
                .frame(height: 52)
                .background(Color.white)
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                SecureField("비밀번호", text: Binding(
                    get: { uiState.password },
                    set: { onAction(.passwordChanged($0)) }
                ))
                .padding(.horizontal, 16)
                .frame(height: 52)
                .background(Color.white)
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
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
                Text(uiState.isLoading ? "로그인 중..." : "로그인")
                    .font(.headline)
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .background(Color(hex: "EF6797"))
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
            socialButton(
                title: "카카오로 시작하기",
                icon: "💬",
                background: Color(hex: "FEE500"),
                foreground: .black,
                provider: .kakao
            )
            socialButton(
                title: "구글로 시작하기",
                icon: "🔍",
                background: .white,
                foreground: Color(hex: "222222"),
                provider: .google
            )
            socialButton(
                title: "애플로 시작하기",
                icon: "🍎",
                background: Color(hex: "111111"),
                foreground: .white,
                provider: .apple
            )
        }
    }
    
    private var footerLinks: some View {
        HStack(spacing: 8) {
            Text("비밀번호 찾기")
            Text("|")
            Text("회원가입")
        }
        .font(.footnote)
        .foregroundStyle(.secondary)
        .frame(maxWidth: .infinity)
    }
    
    private func socialButton(
        title: String,
        icon: String,
        background: Color,
        foreground: Color,
        provider: SignInProvider
    ) -> some View {
        SignInSocialButton(
            title: title,
            icon: icon,
            background: background,
            foreground: foreground,
            action: {
                onAction(.socialSignInTapped(provider: provider))
            }
        )
    }
}

struct SignInView_Previews: PreviewProvider {
    static var previews: some View {
        SignInView(onNavigationAction: { _ in })
    }
}
