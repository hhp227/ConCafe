//
//  IosGoogleIdTokenProvider.swift
//  ConCafe
//

import AuthenticationServices
import CryptoKit
import Foundation
import Security
import Shared
import UIKit

/// Google OAuth(PKCE) 브라우저 세션으로 ID 토큰을 발급받는다. shared 모듈의 GoogleIdTokenProvider 구현체.
final class IosGoogleIdTokenProvider: NSObject, GoogleIdTokenProvider {
    private var webAuthSession: ASWebAuthenticationSession?

    private let presentationContextProvider = WebAuthPresentationContextProvider()

    func getGoogleIdToken() async throws -> String {
        let clientId = try requireGoogleServiceValue(key: "CLIENT_ID")
        let callbackScheme = try requireGoogleServiceValue(key: "REVERSED_CLIENT_ID")
        let state = UUID().uuidString
        let redirectUri = "\(callbackScheme):/oauthredirect"
        let codeVerifier = makeCodeVerifier()
        let codeChallenge = makeCodeChallenge(codeVerifier: codeVerifier)
        let authUrlString =
            "https://accounts.google.com/o/oauth2/v2/auth" +
            "?response_type=code" +
            "&client_id=\(urlEncoded(clientId))" +
            "&redirect_uri=\(urlEncoded(redirectUri))" +
            "&scope=\(urlEncoded("openid email profile"))" +
            "&state=\(urlEncoded(state))" +
            "&code_challenge=\(urlEncoded(codeChallenge))" +
            "&code_challenge_method=S256" +
            "&prompt=select_account"

        guard let authUrl = URL(string: authUrlString) else {
            throw GoogleIdTokenError.invalidAuthUrl
        }
        let authCode = try await requestAuthCode(
            authUrl: authUrl,
            callbackScheme: callbackScheme,
            expectedState: state
        )
        return try await exchangeAuthCodeForIdToken(
            clientId: clientId,
            authCode: authCode,
            codeVerifier: codeVerifier,
            redirectUri: redirectUri
        )
    }

    @MainActor
    private func requestAuthCode(
        authUrl: URL,
        callbackScheme: String,
        expectedState: String
    ) async throws -> String {
        return try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<String, Error>) in
            let session = ASWebAuthenticationSession(
                url: authUrl,
                callbackURLScheme: callbackScheme
            ) { [weak self] callbackUrl, error in
                self?.webAuthSession = nil

                if let error {
                    continuation.resume(throwing: error)
                    return
                }

                guard let callbackUrl else {
                    continuation.resume(throwing: GoogleIdTokenError.emptyCallbackUrl)
                    return
                }

                let callbackState = Self.extractQueryValue(url: callbackUrl, key: "state")
                if callbackState != expectedState {
                    continuation.resume(throwing: GoogleIdTokenError.invalidCallbackState)
                    return
                }

                guard
                    let authCode = Self.extractQueryValue(url: callbackUrl, key: "code"),
                    !authCode.isEmpty
                else {
                    continuation.resume(throwing: GoogleIdTokenError.authCodeNotFound)
                    return
                }

                continuation.resume(returning: authCode)
            }

            session.presentationContextProvider = presentationContextProvider
            session.prefersEphemeralWebBrowserSession = false
            webAuthSession = session
            if !session.start() {
                webAuthSession = nil
                continuation.resume(throwing: GoogleIdTokenError.failedToStartWebAuth)
            }
        }
    }

    private func exchangeAuthCodeForIdToken(
        clientId: String,
        authCode: String,
        codeVerifier: String,
        redirectUri: String
    ) async throws -> String {
        guard let url = URL(string: "https://oauth2.googleapis.com/token") else {
            throw GoogleIdTokenError.invalidAuthUrl
        }

        let body =
            "code=\(urlEncoded(authCode))" +
            "&client_id=\(urlEncoded(clientId))" +
            "&code_verifier=\(urlEncoded(codeVerifier))" +
            "&redirect_uri=\(urlEncoded(redirectUri))" +
            "&grant_type=authorization_code"

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
        request.httpBody = body.data(using: .utf8)

        let (data, response) = try await URLSession.shared.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse, (200..<300).contains(httpResponse.statusCode) else {
            throw GoogleIdTokenError.tokenExchangeFailed
        }
        let tokenResponse = try JSONDecoder().decode(GoogleTokenResponse.self, from: data)
        guard let idToken = tokenResponse.idToken, !idToken.isEmpty else {
            throw GoogleIdTokenError.idTokenNotFound
        }
        return idToken
    }

    private func requireGoogleServiceValue(key: String) throws -> String {
        guard
            let path = Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist"),
            let dictionary = NSDictionary(contentsOfFile: path) as? [String: Any],
            let value = dictionary[key] as? String,
            !value.isEmpty
        else {
            throw GoogleIdTokenError.googleServiceConfigMissing
        }
        return value
    }

    private static func extractQueryValue(url: URL, key: String) -> String? {
        let components = URLComponents(url: url, resolvingAgainstBaseURL: false)
        return components?.queryItems?.first(where: { $0.name == key })?.value
    }

    private func makeCodeVerifier() -> String {
        var randomBytes = [UInt8](repeating: 0, count: 32)
        _ = SecRandomCopyBytes(kSecRandomDefault, randomBytes.count, &randomBytes)
        return base64UrlEncode(Data(randomBytes))
    }

    private func makeCodeChallenge(codeVerifier: String) -> String {
        let verifierData = Data(codeVerifier.utf8)
        let digest = SHA256.hash(data: verifierData)
        return base64UrlEncode(Data(digest))
    }

    private func base64UrlEncode(_ data: Data) -> String {
        return data.base64EncodedString()
            .replacingOccurrences(of: "+", with: "-")
            .replacingOccurrences(of: "/", with: "_")
            .replacingOccurrences(of: "=", with: "")
    }

    private func urlEncoded(_ value: String) -> String {
        let allowed = CharacterSet.alphanumerics.union(.init(charactersIn: "-._~"))
        return value.addingPercentEncoding(withAllowedCharacters: allowed) ?? value
    }
}

private enum GoogleIdTokenError: Error {
    case googleServiceConfigMissing
    case invalidAuthUrl
    case failedToStartWebAuth
    case emptyCallbackUrl
    case invalidCallbackState
    case authCodeNotFound
    case idTokenNotFound
    case tokenExchangeFailed
}

private struct GoogleTokenResponse: Decodable {
    let idToken: String?

    private enum CodingKeys: String, CodingKey {
        case idToken = "id_token"
    }
}

private final class WebAuthPresentationContextProvider: NSObject, ASWebAuthenticationPresentationContextProviding {
    func presentationAnchor(for session: ASWebAuthenticationSession) -> ASPresentationAnchor {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first { $0.isKeyWindow } ?? ASPresentationAnchor()
    }
}
