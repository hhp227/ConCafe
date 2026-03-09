//
//  SignUpView.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import SwiftUI
import UIKit

struct SignUpView: View {
    let onNavigationAction: (NavigationAction) -> Void

    @StateObject private var viewModel = SignUpViewModel()

    var body: some View {
        SignUpContentView(
            uiState: viewModel.uiState,
            onAction: viewModel.onAction
        )
        .onReceive(viewModel.event) { event in
            switch event {
            case .signedUp:
                onNavigationAction(.navigateBack)
            case .navigateBack:
                onNavigationAction(.navigateBack)
            }
        }
    }
}

private struct SignUpContentView: View {
    let uiState: SignUpUiState

    let onAction: (SignUpAction) -> Void

    private var filteredCafes: [SignUpUiState.CafeOption] {
        if uiState.cafeSearchQuery.isEmpty {
            return uiState.cafes
        }

        return uiState.cafes.filter {
            $0.name.localizedCaseInsensitiveContains(uiState.cafeSearchQuery)
        }
    }

    var body: some View {
        VStack(spacing: 0) {
            header
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
            Button(action: {
                onAction(.backTapped)
            }) {
                Image(systemName: "chevron.left")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(Color.primary)
                    .frame(width: 40, height: 40)
                    .background(Color.white.opacity(0.9))
                    .clipShape(Circle())
            }
            Text("회원가입")
                .font(.title3.weight(.bold))
            Spacer()
        }
        .padding(.horizontal, 20)
        .padding(.top, 12)
        .padding(.bottom, 8)
    }

    private var introSection: some View {
        VStack(spacing: 8) {
            Text("회원 유형 선택")
                .font(.title2.weight(.bold))
            Text("어떤 방법으로 가입하시겠어요?")
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
    }

    private var footer: some View {
        HStack(spacing: 6) {
            Text("이미 계정이 있으신가요?")
                .foregroundStyle(.secondary)
            Button("로그인") {
                onAction(.signInInsteadTapped)
            }
            .fontWeight(.semibold)
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
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 24, style: .continuous)
                    .stroke(Color(hex: "E7DFE8"), lineWidth: 1)
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
                title: "이메일",
                placeholder: "email@example.com",
                text: Binding(
                    get: { uiState.email },
                    set: { onAction(.emailChanged($0)) }
                ),
                keyboardType: .emailAddress
            )

            if type == .cafeOwner {
                textField(
                    title: "이름",
                    placeholder: "실명을 입력하세요",
                    text: Binding(
                        get: { uiState.name },
                        set: { onAction(.nameChanged($0)) }
                    )
                )
                phoneVerificationSection
                cafeSelectionSection(
                    title: "운영 카페 선택",
                    placeholder: "카페를 검색하세요"
                )
            } else {
                textField(
                    title: type == .cast ? "활동명 (닉네임)" : "닉네임",
                    placeholder: type == .cast ? "활동할 이름을 입력하세요" : "사용할 닉네임을 입력하세요",
                    text: Binding(
                        get: { uiState.nickname },
                        set: { onAction(.nicknameChanged($0)) }
                    )
                )
                if type == .cast {
                    cafeSelectionSection(
                        title: "소속 카페",
                        placeholder: "소속 카페를 선택하세요"
                    )
                    Text("* 소속 카페의 승인이 필요합니다")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
            }

            secureField(
                title: "비밀번호",
                placeholder: "8자 이상 입력하세요",
                text: Binding(
                    get: { uiState.password },
                    set: { onAction(.passwordChanged($0)) }
                )
            )
            secureField(
                title: "비밀번호 확인",
                placeholder: "비밀번호를 다시 입력하세요",
                text: Binding(
                    get: { uiState.confirmPassword },
                    set: { onAction(.confirmPasswordChanged($0)) }
                )
            )

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
                Text(uiState.isLoading ? "처리 중..." : type.submitLabel)
                    .font(.headline)
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .background(Color(hex: "EF6797"))
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            }
            .disabled(uiState.isLoading)

            if type == .cast {
                Text("가입 후 소속 카페의 승인이 완료되면 활동을 시작할 수 있습니다")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
            }

            if type == .visitor {
                SignInDivider()
                socialButtons
            }
        }
    }

    private var phoneVerificationSection: some View {
        VStack(spacing: 10) {
            HStack(alignment: .bottom, spacing: 10) {
                textField(
                    title: "휴대폰 번호",
                    placeholder: "010-1234-5678",
                    text: Binding(
                        get: { uiState.phone },
                        set: { onAction(.phoneChanged($0)) }
                    ),
                    keyboardType: .phonePad
                )
                Button(uiState.isPhoneVerified ? "인증완료" : "인증요청") {
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

            if !uiState.isPhoneVerified && !uiState.phone.isEmpty {
                HStack(alignment: .bottom, spacing: 10) {
                    textField(
                        title: "인증번호",
                        placeholder: "인증번호 4자리",
                        text: Binding(
                            get: { uiState.verificationCode },
                            set: { onAction(.verificationCodeChanged($0)) }
                        ),
                        keyboardType: .numberPad
                    )
                    Button("확인") {
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
                    Text("휴대폰 인증이 완료되었습니다")
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
                .background(Color.white)
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .stroke(Color(hex: "E4DDE5"), lineWidth: 1)
                )
            }

            if uiState.isCafeSearchVisible {
                VStack(spacing: 0) {
                    textField(
                        title: "카페 검색",
                        placeholder: "카페 이름 검색...",
                        text: Binding(
                            get: { uiState.cafeSearchQuery },
                            set: { onAction(.cafeSearchQueryChanged($0)) }
                        )
                    )
                    .padding(12)

                    if filteredCafes.isEmpty {
                        Text("검색 결과가 없습니다")
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
                                            Text(cafe.location)
                                                .font(.caption)
                                                .foregroundStyle(.secondary)
                                        }
                                        Spacer()
                                        if cafe.isVerified {
                                            Text("인증")
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
                .background(Color.white)
                .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var socialButtons: some View {
        VStack(spacing: 12) {
            SignInSocialButton(
                title: "카카오로 시작하기",
                icon: "💬",
                background: Color(hex: "FEE500"),
                foreground: .black,
                action: {
                    onAction(.socialSignUpTapped(provider: .kakao))
                }
            )
            SignInSocialButton(
                title: "구글로 시작하기",
                icon: "🔍",
                background: .white,
                foreground: Color(hex: "222222"),
                action: {
                    onAction(.socialSignUpTapped(provider: .google))
                }
            )
            SignInSocialButton(
                title: "애플로 시작하기",
                icon: "🍎",
                background: Color(hex: "111111"),
                foreground: .white,
                action: {
                    onAction(.socialSignUpTapped(provider: .apple))
                }
            )
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
                .padding(.horizontal, 16)
                .frame(height: 52)
                .background(Color.white)
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
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
                .padding(.horizontal, 16)
                .frame(height: 52)
                .background(Color.white)
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
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
            return "간편하게 시작하세요!"
        case .cast:
            return "소속 카페를 등록하세요"
        case .cafeOwner:
            return "사업자 인증이 필요합니다"
        }
    }
}

struct SignUpView_Previews: PreviewProvider {
    static var previews: some View {
        SignUpView(onNavigationAction: { _ in })
    }
}
