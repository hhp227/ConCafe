//
//  IosNativeFirebaseAuthDataSource.swift
//  ConCafe
//

import FirebaseAuth
import Foundation
import Shared

/// shared 모듈의 REST 세션을 Firebase iOS SDK 세션과 동기화하고, 전화 인증·자격 증명 연결 등
/// SDK 전용 기능을 제공한다. Firebase 오류는 오류 코드명(예: ERROR_INVALID_VERIFICATION_CODE)을
/// 메시지로 실어 shared 쪽 UseCase/ViewModel이 문자열로 매핑할 수 있게 한다.
final class IosNativeFirebaseAuthDataSource: NSObject, NativeFirebaseAuthDataSource {
    private var verificationId: String?

    func signInWithGoogleIdToken(idToken: String) async throws -> String? {
        let credential = GoogleAuthProvider.credential(withIDToken: idToken, accessToken: "")
        return try await signIn(with: credential)
    }

    func signInWithAppleIdToken(idToken: String) async throws -> String? {
        let credential = OAuthProvider.credential(
            withProviderID: Self.appleProviderId,
            idToken: idToken,
            rawNonce: nil
        )
        return try await signIn(with: credential)
    }

    func signInWithKakaoIdToken(idToken: String) async throws -> String? {
        let credential = OAuthProvider.credential(
            withProviderID: Self.kakaoOidcProviderId,
            idToken: idToken,
            rawNonce: nil
        )
        return try await signIn(with: credential)
    }

    func signOut() async throws {
        try Auth.auth().signOut()
    }

    func sendPhoneVerificationCode(phoneNumber: String) async throws {
        verificationId = try await withFirebaseErrorCode {
            try await PhoneAuthProvider.provider().verifyPhoneNumber(phoneNumber, uiDelegate: nil)
        }
    }

    func signInWithPhoneVerificationCode(code: String) async throws {
        let credential = try phoneCredential(code: code)
        _ = try await withFirebaseErrorCode {
            try await Auth.auth().signIn(with: credential)
        }
    }

    func linkPhoneCredential(code: String) async throws {
        let credential = try phoneCredential(code: code)
        let currentUser = try requireCurrentUser()
        _ = try await withFirebaseErrorCode {
            try await currentUser.link(with: credential)
        }
    }

    func linkEmailCredential(email: String, password: String) async throws {
        let credential = EmailAuthProvider.credential(withEmail: email, password: password)
        let currentUser = try requireCurrentUser()
        _ = try await withFirebaseErrorCode {
            try await currentUser.link(with: credential)
        }
    }

    func deleteCurrentUser() async throws {
        verificationId = nil
        guard let currentUser = Auth.auth().currentUser else { return }

        do {
            try await currentUser.delete()
        } catch {
            try? Auth.auth().signOut()
            throw Self.withErrorCode(error)
        }
    }

    private func signIn(with credential: AuthCredential) async throws -> String {
        let authResult = try await withFirebaseErrorCode {
            try await Auth.auth().signIn(with: credential)
        }
        return authResult.user.uid
    }

    private func phoneCredential(code: String) throws -> PhoneAuthCredential {
        guard let verificationId else {
            throw Self.error(code: "VERIFICATION_NOT_REQUESTED")
        }
        return PhoneAuthProvider.provider().credential(
            withVerificationID: verificationId,
            verificationCode: code
        )
    }

    private func requireCurrentUser() throws -> FirebaseAuth.User {
        guard let currentUser = Auth.auth().currentUser else {
            throw Self.error(code: "NO_CURRENT_USER")
        }
        return currentUser
    }

    private func withFirebaseErrorCode<T>(_ block: () async throws -> T) async throws -> T {
        do {
            return try await block()
        } catch {
            throw Self.withErrorCode(error)
        }
    }

    // Firebase는 오류 이름을 userInfo에 실어 보내므로 그것을 localizedDescription으로 옮겨
    // Kotlin 쪽 예외 메시지에 코드명이 드러나게 한다.
    private static func withErrorCode(_ error: Error) -> Error {
        let nsError = error as NSError
        let codeName = nsError.userInfo[Self.firebaseErrorNameKey] as? String
        return NSError(
            domain: nsError.domain,
            code: nsError.code,
            userInfo: [NSLocalizedDescriptionKey: codeName ?? nsError.localizedDescription]
        )
    }

    private static func error(code: String) -> Error {
        return NSError(
            domain: AuthErrorDomain,
            code: AuthErrorCode.internalError.rawValue,
            userInfo: [NSLocalizedDescriptionKey: code]
        )
    }

    private static let appleProviderId = "apple.com"
    private static let kakaoOidcProviderId = "oidc.kakao"
    private static let firebaseErrorNameKey = "FIRAuthErrorUserInfoNameKey"
}
