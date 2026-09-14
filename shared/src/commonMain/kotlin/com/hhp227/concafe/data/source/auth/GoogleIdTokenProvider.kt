package com.hhp227.concafe.data.source.auth

/**
 * Obtains a Google ID token from the platform identity flow (Credential Manager, OAuth
 * browser session, ...). Throws when the user cancels or the flow fails.
 */
interface GoogleIdTokenProvider {
    suspend fun getGoogleIdToken(): String
}
