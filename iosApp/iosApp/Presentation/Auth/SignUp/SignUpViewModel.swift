//
//  SignUpViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import Foundation
import Combine
import Shared

@MainActor
class SignUpViewModel: ObservableObject {
    private let getSignUpCafeListUseCase: GetSignUpCafeListUseCase

    private let signUpUseCase: SignUpUseCase

    private let signInUseCase: SignInUseCase

    @Published private(set) var uiState = SignUpUiState.empty

    let event = PassthroughSubject<SignUpEvent, Never>()

    private var requestTask: Task<Void, Never>?

    private func selectUserType(_ type: SignUpUiState.UserType) {
        uiState.step = .form
        uiState.selectedUserType = type
        uiState.phone = ""
        uiState.verificationCode = ""
        uiState.hasRequestedVerification = false
        uiState.isPhoneVerified = false
        uiState.selectedCafe = nil
        uiState.cafeSearchQuery = ""
        uiState.isCafeSearchVisible = false
        clearMessages()
    }

    private func handleBack() {
        if uiState.step == .form {
            backToTypeSelection()
        } else {
            event.send(.navigateBack)
        }
    }

    private func backToTypeSelection() {
        uiState.step = .selectType
        uiState.selectedUserType = nil
        uiState.phone = ""
        uiState.verificationCode = ""
        uiState.hasRequestedVerification = false
        uiState.isPhoneVerified = false
        uiState.selectedCafe = nil
        uiState.cafeSearchQuery = ""
        uiState.isCafeSearchVisible = false
        clearMessages()
    }

    private func changePhone(_ value: String) {
        uiState.phone = value
        uiState.verificationCode = ""
        uiState.hasRequestedVerification = false
        uiState.isPhoneVerified = false
        clearMessages()
    }

    private func clearCafeSelection() {
        uiState.selectedCafe = nil
        clearMessages()
    }

    private func loadCafeOptions() {
        requestTask?.cancel()
        requestTask = Task {
            do {
                let result = try await getSignUpCafeListUseCase.invoke()

                if let success = result as? AppResultSuccess<AnyObject>,
                   let cafes = success.data as? [Cafe] {
                    uiState.cafes = cafes
                } else if let failure = result as? AppResultFailure {
                    uiState.cafes = []
                    uiState.errorMessage = "\(failure.error)"
                }
            } catch {
                if Task.isCancelled { return }
                uiState.cafes = []
                uiState.errorMessage = error.localizedDescription
            }
        }
    }

    private func sendVerification() {
        let trimmedPhone = uiState.phone.trimmingCharacters(in: .whitespacesAndNewlines)

        guard !trimmedPhone.isEmpty else {
            uiState.errorMessage = "휴대폰 번호를 입력해주세요."
            uiState.infoMessage = nil
            return
        }

        uiState.errorMessage = nil
        uiState.hasRequestedVerification = true
        uiState.infoMessage = "인증번호가 \(trimmedPhone) 로 전송되었습니다. 테스트 코드는 1234입니다."
    }

    private func verifyCode() {
        if uiState.verificationCode.trimmingCharacters(in: .whitespacesAndNewlines) == Self.verificationCode {
            uiState.hasRequestedVerification = true
            uiState.isPhoneVerified = true
            uiState.errorMessage = nil
            uiState.infoMessage = "휴대폰 인증이 완료되었습니다."
        } else {
            uiState.isPhoneVerified = false
            uiState.errorMessage = "인증번호가 일치하지 않습니다."
            uiState.infoMessage = nil
        }
    }

    private func submit() {
        if let message = validate(uiState) {
            uiState.errorMessage = message
            uiState.infoMessage = nil
            return
        }

        uiState.isLoading = true
        clearMessages()

        requestTask?.cancel()
        requestTask = Task {
            do {
                let role = resolveRole(uiState)
                let result = try await signUpUseCase.invoke(
                    email: uiState.email.trimmingCharacters(in: .whitespacesAndNewlines),
                    password: uiState.password,
                    nickname: resolveNickname(uiState),
                    role: role
                )

                if result is AppResultSuccess<AnyObject> {
                    uiState.isLoading = false
                    event.send(.signedUp)
                } else {
                    uiState.isLoading = false
                    uiState.errorMessage = "회원가입에 실패했습니다. 입력값을 확인해주세요."
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isLoading = false
                uiState.errorMessage = error.localizedDescription
            }
        }
    }

    private func socialSignUp(_ provider: SignUpProvider) {
        uiState.isLoading = true
        clearMessages()

        requestTask?.cancel()
        requestTask = Task {
            do {
                let result = try await signInUseCase.invoke(
                    email: "\(provider.rawValue)@mock.concafe",
                    password: "social-sign-in"
                )

                if result is AppResultSuccess<AnyObject> {
                    uiState.isLoading = false
                    event.send(.signedUp)
                } else {
                    uiState.isLoading = false
                    uiState.errorMessage = "소셜 회원가입에 실패했습니다. 잠시 후 다시 시도해주세요."
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isLoading = false
                uiState.errorMessage = error.localizedDescription
            }
        }
    }

    private func updateState(
        email: String? = nil,
        password: String? = nil,
        confirmPassword: String? = nil,
        nickname: String? = nil,
        name: String? = nil,
        verificationCode: String? = nil
    ) {
        if let email {
            uiState.email = email
        }
        if let password {
            uiState.password = password
        }
        if let confirmPassword {
            uiState.confirmPassword = confirmPassword
        }
        if let nickname {
            uiState.nickname = nickname
        }
        if let name {
            uiState.name = name
        }
        if let verificationCode {
            uiState.verificationCode = verificationCode
        }
        clearMessages()
    }

    private func clearMessages() {
        uiState.errorMessage = nil
        uiState.infoMessage = nil
    }

    private func validate(_ state: SignUpUiState) -> String? {
        guard let userType = state.selectedUserType else {
            return "회원 유형을 선택해주세요."
        }
        if state.email.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return "이메일을 입력해주세요."
        }
        if userType == .cafeOwner && state.name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return "이름을 입력해주세요."
        }
        if userType != .cafeOwner && state.nickname.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return "닉네임을 입력해주세요."
        }
        if state.password.count < Self.minimumPasswordLength {
            return "비밀번호는 8자 이상 입력해주세요."
        }
        if state.password != state.confirmPassword {
            return "비밀번호가 일치하지 않습니다."
        }
        if userType == .cafeOwner && !state.isPhoneVerified {
            return "휴대폰 인증을 완료해주세요."
        }
        if userType == .cast && state.selectedCafe == nil {
            return "카페를 선택해주세요."
        }

        return nil
    }

    private func resolveNickname(_ state: SignUpUiState) -> String {
        switch state.selectedUserType {
        case .cafeOwner:
            return state.name.trimmingCharacters(in: .whitespacesAndNewlines)
        case .cast:
            return state.nickname.trimmingCharacters(in: .whitespacesAndNewlines)
        case .visitor, .none:
            return state.nickname.trimmingCharacters(in: .whitespacesAndNewlines)
        }
    }

    private func resolveRole(_ state: SignUpUiState) -> UserRole {
        switch state.selectedUserType {
        case .cafeOwner:
            return .cafeOwner
        case .cast:
            return .cast
        case .visitor, .none:
            return .visitor
        }
    }

    func onAction(_ action: SignUpAction) {
        switch action {
        case .backTapped:
            handleBack()
        case .userTypeTapped(let type):
            selectUserType(type)
        case .backToTypeSelectionTapped:
            backToTypeSelection()
        case .emailChanged(let value):
            updateState(email: value)
        case .passwordChanged(let value):
            updateState(password: value)
        case .confirmPasswordChanged(let value):
            updateState(confirmPassword: value)
        case .nicknameChanged(let value):
            updateState(nickname: value)
        case .nameChanged(let value):
            updateState(name: value)
        case .phoneChanged(let value):
            changePhone(value)
        case .verificationCodeChanged(let value):
            updateState(verificationCode: value)
        case .cafeSearchQueryChanged(let value):
            uiState.cafeSearchQuery = value
            clearMessages()
        case .sendVerificationTapped:
            sendVerification()
        case .verifyCodeTapped:
            verifyCode()
        case .toggleCafeSearchTapped:
            uiState.isCafeSearchVisible.toggle()
            clearMessages()
        case .cafeTapped(let cafe):
            uiState.selectedCafe = cafe
            uiState.isCafeSearchVisible = false
            uiState.cafeSearchQuery = ""
            clearMessages()
        case .clearCafeTapped:
            clearCafeSelection()
        case .submitTapped:
            submit()
        case .socialSignUpTapped(let provider):
            socialSignUp(provider)
        case .signInInsteadTapped:
            event.send(.navigateBack)
        }
    }

    init(
        getSignUpCafeListUseCase: GetSignUpCafeListUseCase = KoinInitializerKt.resolveGetSignUpCafeListUseCase(),
        signUpUseCase: SignUpUseCase = KoinInitializerKt.resolveSignUpUseCase(),
        signInUseCase: SignInUseCase = KoinInitializerKt.resolveSignInUseCase()
    ) {
        self.getSignUpCafeListUseCase = getSignUpCafeListUseCase
        self.signUpUseCase = signUpUseCase
        self.signInUseCase = signInUseCase
        loadCafeOptions()
    }

    deinit {
        requestTask?.cancel()
    }

    private static let minimumPasswordLength = 8

    private static let verificationCode = "1234"
}
