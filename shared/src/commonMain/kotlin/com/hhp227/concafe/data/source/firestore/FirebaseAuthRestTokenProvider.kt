package com.hhp227.concafe.data.source.firestore

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.datetime.Clock

class FirebaseAuthRestTokenProvider(
    private val apiKey: String,
    private val restClient: FirebaseAuthRestClient
) : FirestoreAuthTokenProvider {
    private var currentSession: FirebaseAuthSession? = null

    override suspend fun getIdToken(): String? {
        val session = currentSession ?: return null
        val refreshed = refreshSessionIfNeeded(session)
        currentSession = refreshed
        return refreshed.idToken
    }

    override suspend fun signInWithEmailPassword(email: String, password: String): FirebaseAuthSession? {
        if (!supportsEmailPasswordAuth()) {
            return null
        }

        val body = """
            {
              "email": "${escapeJson(email)}",
              "password": "${escapeJson(password)}",
              "returnSecureToken": true
            }
        """.trimIndent()

        val response = restClient.postJson(signInUrl(), body)
        val session = parseSessionFromResponse(response)

        currentSession = session

        return session
    }

    override suspend fun signInWithGoogleIdToken(idToken: String): FirebaseAuthSession? {
        if (!supportsEmailPasswordAuth()) {
            return null
        }
        if (idToken.isBlank()) {
            throw IllegalArgumentException("google idToken is required")
        }

        val body = """
            {
              "postBody": "id_token=${escapeJson(idToken)}&providerId=google.com",
              "requestUri": "http://localhost",
              "returnSecureToken": true,
              "returnIdpCredential": true
            }
        """.trimIndent()

        val response = restClient.postJson(signInWithIdpUrl(), body)
        val session = parseSessionFromResponse(response)

        currentSession = session

        return session
    }

    override suspend fun signUpWithEmailPassword(email: String, password: String): FirebaseAuthSession? {
        if (!supportsEmailPasswordAuth()) {
            return null
        }

        val body = """
            {
              "email": "${escapeJson(email)}",
              "password": "${escapeJson(password)}",
              "returnSecureToken": true
            }
        """.trimIndent()

        val response = restClient.postJson(signUpUrl(), body)
        val session = parseSessionFromResponse(response)

        currentSession = session

        return session
    }

    override suspend fun signOut() {
        currentSession = null
    }

    override suspend fun deleteCurrentUser(idToken: String?) {
        if (!supportsEmailPasswordAuth()) {
            return
        }

        val resolvedIdToken = idToken ?: currentSession?.idToken

        if (resolvedIdToken.isNullOrBlank()) {
            throw IllegalStateException("Firebase auth delete requires idToken")
        }

        val body = """
            {
              "idToken": "${escapeJson(resolvedIdToken)}"
            }
        """.trimIndent()

        restClient.postJson(deleteAccountUrl(), body)

        currentSession = null
    }

    override suspend fun refreshSession(session: FirebaseAuthSession): FirebaseAuthSession? {
        if (!supportsEmailPasswordAuth()) {
            return null
        }
        val refreshToken = session.refreshToken
        if (refreshToken.isNullOrBlank()) {
            return null
        }
        val body = "grant_type=refresh_token&refresh_token=${escapeFormValue(refreshToken)}"
        val response = restClient.postFormUrlEncoded(refreshUrl(), body)
        val root = Json.parseToJsonElement(response).jsonObject
        val userId = root["user_id"]?.jsonPrimitive?.content.orEmpty()
        val idToken = root["id_token"]?.jsonPrimitive?.content
        val nextRefreshToken = root["refresh_token"]?.jsonPrimitive?.content ?: refreshToken
        val expiresInSeconds = root["expires_in"]?.jsonPrimitive?.content?.toLongOrNull()
        val email = session.email

        if (userId.isBlank() || email.isBlank()) {
            return null
        }
        return FirebaseAuthSession(
            userId = userId,
            email = email,
            idToken = idToken,
            refreshToken = nextRefreshToken,
            expiresAtEpochSeconds = expiresInSeconds?.let { nowEpochSeconds() + it }
        )
    }

    override fun getCurrentUserId(): String? {
        return currentSession?.userId
    }

    override fun getCurrentUserEmail(): String? {
        return currentSession?.email
    }

    override fun supportsEmailPasswordAuth(): Boolean {
        return apiKey.isNotBlank()
    }

    private fun signInUrl(): String {
        return "$FIREBASE_AUTH_BASE_URL/accounts:signInWithPassword?key=$apiKey"
    }

    private fun signUpUrl(): String {
        return "$FIREBASE_AUTH_BASE_URL/accounts:signUp?key=$apiKey"
    }

    private fun signInWithIdpUrl(): String {
        return "$FIREBASE_AUTH_BASE_URL/accounts:signInWithIdp?key=$apiKey"
    }

    private fun deleteAccountUrl(): String {
        return "$FIREBASE_AUTH_BASE_URL/accounts:delete?key=$apiKey"
    }

    private fun refreshUrl(): String {
        return "$FIREBASE_TOKEN_BASE_URL/token?key=$apiKey"
    }

    private fun parseSessionFromResponse(response: String): FirebaseAuthSession {
        val root = Json.parseToJsonElement(response).jsonObject
        val userId = root["localId"]?.jsonPrimitive?.content.orEmpty()
        val email = root["email"]?.jsonPrimitive?.content.orEmpty()
        val idToken = root["idToken"]?.jsonPrimitive?.content
        val refreshToken = root["refreshToken"]?.jsonPrimitive?.content
        val expiresInSeconds = root["expiresIn"]?.jsonPrimitive?.content?.toLongOrNull()

        if (userId.isBlank() || email.isBlank()) {
            throw IllegalStateException("Firebase auth response is missing localId/email")
        }
        return FirebaseAuthSession(
            userId = userId,
            email = email,
            idToken = idToken,
            refreshToken = refreshToken,
            expiresAtEpochSeconds = expiresInSeconds?.let { nowEpochSeconds() + it }
        )
    }

    private suspend fun refreshSessionIfNeeded(session: FirebaseAuthSession): FirebaseAuthSession {
        val expiresAt = session.expiresAtEpochSeconds
        val shouldRefresh = expiresAt != null && (expiresAt - nowEpochSeconds()) <= TOKEN_REFRESH_BUFFER_SECONDS

        if (!shouldRefresh) {
            return session
        }

        val refreshed = runCatching { refreshSession(session) }.getOrNull()
        return refreshed ?: session
    }
}

private const val FIREBASE_AUTH_BASE_URL = "https://identitytoolkit.googleapis.com/v1"
private const val FIREBASE_TOKEN_BASE_URL = "https://securetoken.googleapis.com/v1"
private const val TOKEN_REFRESH_BUFFER_SECONDS = 60L

private fun escapeJson(value: String): String {
    return value.replace("\\", "\\\\").replace("\"", "\\\"")
}

private fun escapeFormValue(value: String): String {
    return value.replace("+", "%2B").replace("=", "%3D").replace("&", "%26")
}

private fun nowEpochSeconds(): Long {
    return Clock.System.now().epochSeconds
}
