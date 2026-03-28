package com.hhp227.concafe.data.source.firestore

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.datetime.Clock

class FirebaseAuthRestTokenProvider(
    private val apiKey: String,
    private val fallbackApiKeys: List<String> = emptyList(),
    private val restClient: FirebaseAuthRestClient
) : FirestoreAuthTokenProvider {
    private var currentSession: FirebaseAuthSession? = null

    override suspend fun getIdToken(): String? {
        val session = currentSession ?: runCatching {
            signInAnonymously()
        }.getOrNull()
            ?: return null
        val refreshed = refreshSessionIfNeeded(session)
        currentSession = refreshed
        return refreshed.idToken
    }

    override suspend fun signInAnonymously(): FirebaseAuthSession? {
        if (!supportsEmailPasswordAuth()) {
            return null
        }
        val body = """
            {
              "returnSecureToken": true
            }
        """.trimIndent()
        val response = postJsonWithApiKeyFallback(
            buildUrl = { key -> signUpUrl(key) },
            body = body
        )
        val session = parseSessionFromResponse(response, allowMissingEmail = true)

        currentSession = session
        return session
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

        val response = postJsonWithApiKeyFallback(
            buildUrl = { key -> signInUrl(key) },
            body = body
        )
        val session = parseSessionFromResponse(response, allowMissingEmail = false)

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

        val response = postJsonWithApiKeyFallback(
            buildUrl = { key -> signInWithIdpUrl(key) },
            body = body
        )
        val session = parseSessionFromResponse(response, allowMissingEmail = false)

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

        val response = postJsonWithApiKeyFallback(
            buildUrl = { key -> signUpUrl(key) },
            body = body
        )
        val session = parseSessionFromResponse(response, allowMissingEmail = false)

        currentSession = session

        return session
    }

    override suspend fun signOut() {
        currentSession = null
    }

    override suspend fun updateCurrentUserPassword(
        idToken: String,
        newPassword: String
    ): FirebaseAuthSession? {
        if (!supportsEmailPasswordAuth()) {
            return null
        }
        if (idToken.isBlank() || newPassword.isBlank()) {
            throw IllegalArgumentException("idToken/newPassword is required")
        }

        val body = """
            {
              "idToken": "${escapeJson(idToken)}",
              "password": "${escapeJson(newPassword)}",
              "returnSecureToken": true
            }
        """.trimIndent()

        val response = postJsonWithApiKeyFallback(
            buildUrl = { key -> updatePasswordUrl(key) },
            body = body
        )
        val session = parseSessionFromResponse(response, allowMissingEmail = false)

        currentSession = session
        return session
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

        postJsonWithApiKeyFallback(
            buildUrl = { key -> deleteAccountUrl(key) },
            body = body
        )

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
        val response = postFormUrlEncodedWithApiKeyFallback(
            buildUrl = { key -> refreshUrl(key) },
            body = body
        )
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

    private fun signInUrl(apiKey: String): String {
        return "$FIREBASE_AUTH_BASE_URL/accounts:signInWithPassword?key=$apiKey"
    }

    private fun signUpUrl(apiKey: String): String {
        return "$FIREBASE_AUTH_BASE_URL/accounts:signUp?key=$apiKey"
    }

    private fun signInWithIdpUrl(apiKey: String): String {
        return "$FIREBASE_AUTH_BASE_URL/accounts:signInWithIdp?key=$apiKey"
    }

    private fun deleteAccountUrl(apiKey: String): String {
        return "$FIREBASE_AUTH_BASE_URL/accounts:delete?key=$apiKey"
    }

    private fun updatePasswordUrl(apiKey: String): String {
        return "$FIREBASE_AUTH_BASE_URL/accounts:update?key=$apiKey"
    }

    private fun refreshUrl(apiKey: String): String {
        return "$FIREBASE_TOKEN_BASE_URL/token?key=$apiKey"
    }

    private suspend fun postJsonWithApiKeyFallback(
        buildUrl: (String) -> String,
        body: String
    ): String {
        var lastError: Throwable? = null
        val errorSummaries = mutableListOf<String>()

        authApiKeys().forEach { key ->
            val result = runCatching {
                restClient.postJson(buildUrl(key), body)
            }

            if (result.isSuccess) {
                return result.getOrThrow()
            } else {
                lastError = result.exceptionOrNull()
                val message = result.exceptionOrNull()?.message ?: "unknown"
                errorSummaries.add(message)
            }
        }
        val detail = if (errorSummaries.isEmpty()) {
            ""
        } else {
            ": ${errorSummaries.joinToString(separator = " | ")}"
        }
        throw IllegalStateException("Firebase auth request failed for all configured API keys$detail", lastError)
    }

    private suspend fun postFormUrlEncodedWithApiKeyFallback(
        buildUrl: (String) -> String,
        body: String
    ): String {
        var lastError: Throwable? = null
        val errorSummaries = mutableListOf<String>()

        authApiKeys().forEach { key ->
            val result = runCatching {
                restClient.postFormUrlEncoded(buildUrl(key), body)
            }

            if (result.isSuccess) {
                return result.getOrThrow()
            } else {
                lastError = result.exceptionOrNull()
                val message = result.exceptionOrNull()?.message ?: "unknown"
                errorSummaries.add(message)
            }
        }
        val detail = if (errorSummaries.isEmpty()) {
            ""
        } else {
            ": ${errorSummaries.joinToString(separator = " | ")}"
        }
        throw IllegalStateException("Firebase auth refresh failed for all configured API keys$detail", lastError)
    }

    private fun authApiKeys(): List<String> {
        val keys = mutableListOf<String>()
        val normalizedPrimary = apiKey.trim()

        if (normalizedPrimary.isNotBlank()) {
            keys.add(normalizedPrimary)
        }
        fallbackApiKeys.forEach { key ->
            val normalized = key.trim()

            if (normalized.isNotBlank() && !keys.contains(normalized)) {
                keys.add(normalized)
            }
        }
        return keys
    }

    private fun parseSessionFromResponse(response: String, allowMissingEmail: Boolean): FirebaseAuthSession {
        val root = Json.parseToJsonElement(response).jsonObject
        val userId = root["localId"]?.jsonPrimitive?.content.orEmpty()
        val rawEmail = root["email"]?.jsonPrimitive?.content.orEmpty()
        val email = if (rawEmail.isNotBlank()) {
            rawEmail
        } else {
            "anonymous-$userId@concafe.local"
        }
        val idToken = root["idToken"]?.jsonPrimitive?.content
        val refreshToken = root["refreshToken"]?.jsonPrimitive?.content
        val expiresInSeconds = root["expiresIn"]?.jsonPrimitive?.content?.toLongOrNull()

        if (userId.isBlank() || (!allowMissingEmail && email.isBlank())) {
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
