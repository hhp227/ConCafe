package com.hhp227.concafe.data.source.firestore

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class FirebaseAuthRestTokenProvider(
    private val apiKey: String,
    private val restClient: FirebaseAuthRestClient
) : FirestoreAuthTokenProvider {
    private var currentSession: FirebaseAuthSession? = null

    override suspend fun getIdToken(): String? {
        return currentSession?.idToken
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

    private fun parseSessionFromResponse(response: String): FirebaseAuthSession {
        val root = Json.parseToJsonElement(response).jsonObject
        val userId = root["localId"]?.jsonPrimitive?.content.orEmpty()
        val email = root["email"]?.jsonPrimitive?.content.orEmpty()
        val idToken = root["idToken"]?.jsonPrimitive?.content

        if (userId.isBlank() || email.isBlank()) {
            throw IllegalStateException("Firebase auth response is missing localId/email")
        }

        return FirebaseAuthSession(
            userId = userId,
            email = email,
            idToken = idToken
        )
    }
}

private const val FIREBASE_AUTH_BASE_URL = "https://identitytoolkit.googleapis.com/v1"

private fun escapeJson(value: String): String {
    return value.replace("\\", "\\\\").replace("\"", "\\\"")
}
