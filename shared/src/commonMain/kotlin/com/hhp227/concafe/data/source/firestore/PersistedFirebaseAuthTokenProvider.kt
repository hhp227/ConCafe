package com.hhp227.concafe.data.source.firestore

import kotlinx.datetime.Clock

class PersistedFirebaseAuthTokenProvider(
    private val delegate: FirestoreAuthTokenProvider,
    private val sessionStore: FirebaseAuthSessionStore
) : FirestoreAuthTokenProvider {
    private var cachedSession: FirebaseAuthSession? = sessionStore.load()

    override suspend fun getIdToken(): String? {
        val currentSession = cachedSession

        if (currentSession != null) {
            val refreshed = refreshSessionIfNeeded(currentSession)

            if (refreshed != currentSession) {
                persistSession(refreshed)
            }
            return refreshed.idToken
        }
        return delegate.getIdToken()
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

    override suspend fun signUpWithEmailPassword(email: String, password: String): FirebaseAuthSession? {
        val session = delegate.signUpWithEmailPassword(email, password)

        persistSession(session)
        return session
    }

    override suspend fun signOut() {
        delegate.signOut()
        clearPersistedSession()
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
        } else {
            sessionStore.save(session)
        }
    }

    private fun clearPersistedSession() {
        persistSession(null)
    }

    private suspend fun refreshSessionIfNeeded(session: FirebaseAuthSession): FirebaseAuthSession {
        val expiresAt = session.expiresAtEpochSeconds
        val shouldRefresh = expiresAt != null && (expiresAt - nowEpochSeconds()) <= TOKEN_REFRESH_BUFFER_SECONDS

        if (!shouldRefresh) {
            return session
        }
        return runCatching { delegate.refreshSession(session) }
            .getOrNull()
            ?: session
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
