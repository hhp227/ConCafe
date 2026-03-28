package com.hhp227.concafe.data.source.firestore

import kotlinx.datetime.Clock

class PersistedFirebaseAuthTokenProvider(
    private val delegate: FirestoreAuthTokenProvider,
    private val sessionStore: FirebaseAuthSessionStore
) : FirestoreAuthTokenProvider {
    private var cachedSession: FirebaseAuthSession? = sessionStore.load()

    private var hasTriedAnonymousSignIn = false

    override suspend fun getIdToken(): String? {
        val currentSession = cachedSession

        if (currentSession == null) {
            val tokenFromDelegate = delegate.getIdToken()

            if (!tokenFromDelegate.isNullOrBlank()) {
                return tokenFromDelegate
            }
            if (hasTriedAnonymousSignIn) {
                return null
            }
            val anonymousSession = runCatching {
                delegate.signInAnonymously()
            }.onFailure { error ->
                println("TEST, ${error.message}")
            }.getOrNull()
            hasTriedAnonymousSignIn = true

            if (anonymousSession != null) {
                persistSession(anonymousSession)
                return anonymousSession.idToken
            }
            return null
        } else {
            val refreshed = refreshSessionIfNeeded(currentSession)

            if (refreshed == null) {
                clearPersistedSession()
                return null
            } else if (refreshed != currentSession) {
                persistSession(refreshed)
            }
            return refreshed.idToken
        }
    }

    override suspend fun signInAnonymously(): FirebaseAuthSession? {
        val session = delegate.signInAnonymously()

        persistSession(session)
        return session
    }

    override suspend fun signInWithEmailPassword(email: String, password: String): FirebaseAuthSession? {
        val session = delegate.signInWithEmailPassword(email, password)

        persistSession(session)
        return session
    }

    override suspend fun signInWithGoogleIdToken(idToken: String): FirebaseAuthSession? {
        val session = delegate.signInWithGoogleIdToken(idToken)

        persistSession(session)
        return session
    }

    override suspend fun signInWithAppleIdToken(idToken: String): FirebaseAuthSession? {
        val session = delegate.signInWithAppleIdToken(idToken)

        persistSession(session)
        return session
    }

    override suspend fun signUpWithEmailPassword(email: String, password: String): FirebaseAuthSession? {
        val session = delegate.signUpWithEmailPassword(email, password)

        persistSession(session)
        return session
    }

    override suspend fun signOut() {
        delegate.signOut()
        clearPersistedSession()
    }

    override suspend fun sendPasswordResetEmail(email: String) {
        delegate.sendPasswordResetEmail(email)
    }

    override suspend fun updateCurrentUserPassword(
        idToken: String,
        newPassword: String
    ): FirebaseAuthSession? {
        val session = delegate.updateCurrentUserPassword(idToken, newPassword)

        persistSession(session)
        return session
    }

    override suspend fun deleteCurrentUser(idToken: String?) {
        val resolvedIdToken = idToken ?: cachedSession?.idToken

        delegate.deleteCurrentUser(resolvedIdToken)
        clearPersistedSession()
    }

    override suspend fun refreshSession(session: FirebaseAuthSession): FirebaseAuthSession? {
        val refreshed = delegate.refreshSession(session) ?: return null

        persistSession(refreshed)
        return refreshed
    }

    override fun getCurrentUserId(): String? {
        val currentSession = cachedSession
        return currentSession?.userId ?: delegate.getCurrentUserId()
    }

    override fun getCurrentUserEmail(): String? {
        val currentSession = cachedSession
        return currentSession?.email ?: delegate.getCurrentUserEmail()
    }

    override fun supportsEmailPasswordAuth(): Boolean {
        return delegate.supportsEmailPasswordAuth()
    }

    private fun persistSession(session: FirebaseAuthSession?) {
        cachedSession = session

        if (session == null) {
            sessionStore.clear()
            hasTriedAnonymousSignIn = false
        } else {
            sessionStore.save(session)
            hasTriedAnonymousSignIn = false
        }
    }

    private fun clearPersistedSession() {
        persistSession(null)
    }

    private suspend fun refreshSessionIfNeeded(session: FirebaseAuthSession): FirebaseAuthSession? {
        val expiresAt = session.expiresAtEpochSeconds
        val nowEpochSeconds = nowEpochSeconds()
        val shouldRefresh = if (expiresAt == null) {
            true
        } else {
            (expiresAt - nowEpochSeconds) <= TOKEN_REFRESH_BUFFER_SECONDS
        }

        if (!shouldRefresh) {
            return session
        } else {
            val refreshed = runCatching { delegate.refreshSession(session) }.getOrNull()

            if (refreshed != null) {
                return refreshed
            }
        }

        val hasValidIdToken = !session.idToken.isNullOrBlank()
        val isExpired = if (expiresAt == null) {
            true
        } else {
            expiresAt <= nowEpochSeconds
        }
        return if (hasValidIdToken && !isExpired) {
            session
        } else {
            null
        }
    }
}

interface FirebaseAuthSessionStore {
    fun load(): FirebaseAuthSession?

    fun save(session: FirebaseAuthSession)

    fun clear()
}

private const val TOKEN_REFRESH_BUFFER_SECONDS = 60L

private fun nowEpochSeconds(): Long {
    return Clock.System.now().epochSeconds
}
