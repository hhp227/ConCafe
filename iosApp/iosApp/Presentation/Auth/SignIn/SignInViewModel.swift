//
//  SignInViewModel.swift
//  ConCafe
//
//  Created by 홍희표 on 2026/03/07.
//

import Foundation
import Combine
import Shared

@MainActor
class SignInViewModel: ObservableObject {
    private let signInUseCase: SignInUseCase
    
    @Published private(set) var uiState = SignInUiState.empty
    
    let event = PassthroughSubject<SignInEvent, Never>()
    
    private var signInTask: Task<Void, Never>?
    
    private func signIn(email: String, password: String) {
        uiState.isLoading = true
        uiState.errorMessage = nil
        
        signInTask?.cancel()
        signInTask = Task {
            do {
                let result = try await signInUseCase.invoke(email: email, password: password)
                
                if result is AppResultSuccess<AnyObject> {
                    uiState.isLoading = false
                    event.send(.signedIn)
                } else if result is AppResultFailure {
                    uiState.isLoading = false
                    uiState.errorMessage = "로그인에 실패했습니다. 입력값을 확인해주세요."
                } else {
                    uiState.isLoading = false
                    uiState.errorMessage = "로그인에 실패했습니다. 입력값을 확인해주세요."
                }
            } catch {
                if Task.isCancelled { return }
                uiState.isLoading = false
                uiState.errorMessage = error.localizedDescription
            }
        }
    }
    
    func onAction(_ action: SignInAction) {
        switch action {
        case .emailChanged(let value):
            uiState.email = value
            uiState.errorMessage = nil
        case .passwordChanged(let value):
            uiState.password = value
            uiState.errorMessage = nil
        case .signInTapped:
            signIn(email: uiState.email, password: uiState.password)
        case .socialSignInTapped(let provider):
            signIn(email: "\(provider.rawValue)@mock.concafe", password: "social-sign-in")
        }
    }
    
    init(
        signInUseCase: SignInUseCase = KoinInitializerKt.resolveSignInUseCase()
    ) {
        self.signInUseCase = signInUseCase
    }
    
    deinit {
        signInTask?.cancel()
    }
}
