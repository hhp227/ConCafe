//
//  IosKakaoIdTokenProvider.swift
//  ConCafe
//

import Foundation
import KakaoSDKAuth
import KakaoSDKUser
import Shared

/// Kakao SDK 로그인으로 OIDC ID 토큰과 프로필(이메일·닉네임)을 얻는다. shared 모듈의 KakaoIdTokenProvider 구현체.
final class IosKakaoIdTokenProvider: NSObject, KakaoIdTokenProvider {
    func getKakaoAuthPayload() async throws -> KakaoAuthPayload {
        let idToken = try await requestIdToken()
        let profile = await requestProfile()
        return KakaoAuthPayload(
            idToken: idToken,
            email: profile.email,
            nickname: profile.nickname
        )
    }

    @MainActor
    private func requestIdToken() async throws -> String {
        return try await withCheckedThrowingContinuation { continuation in
            let loginCompletion: (OAuthToken?, Error?) -> Void = { token, error in
                if let error {
                    continuation.resume(throwing: error)
                    return
                }

                guard let idToken = token?.idToken, !idToken.isEmpty else {
                    continuation.resume(throwing: KakaoIdTokenError.idTokenNotFound)
                    return
                }
                continuation.resume(returning: idToken)
            }

            if UserApi.isKakaoTalkLoginAvailable() {
                UserApi.shared.loginWithKakaoTalk(completion: loginCompletion)
            } else {
                UserApi.shared.loginWithKakaoAccount(completion: loginCompletion)
            }
        }
    }

    private func requestProfile() async -> (email: String?, nickname: String?) {
        return await withCheckedContinuation { continuation in
            UserApi.shared.me { user, error in
                if error != nil {
                    continuation.resume(returning: (email: nil, nickname: nil))
                } else {
                    let nickname = user?.kakaoAccount?.profile?.nickname?.trimmingCharacters(in: .whitespacesAndNewlines)
                    let email = user?.kakaoAccount?.email?.trimmingCharacters(in: .whitespacesAndNewlines)
                    continuation.resume(
                        returning: (
                            email: email?.isEmpty == true ? nil : email,
                            nickname: nickname?.isEmpty == true ? nil : nickname
                        )
                    )
                }
            }
        }
    }
}

private enum KakaoIdTokenError: Error {
    case idTokenNotFound
}
