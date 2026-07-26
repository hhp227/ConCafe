package com.hhp227.concafe.data.source.firestore

import com.hhp227.concafe.domain.model.AuthProvider
import kotlinx.datetime.Clock
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PersistedFirebaseAuthTokenProvider(
    private val delegate: FirestoreAuthTokenProvider,
    private val sessionStore: FirebaseAuthSessionStore
) : FirestoreAuthTokenProvider {
    private var cachedSession: FirebaseAuthSession? = sessionStore.load()

    private val tokenMutex = Mutex()

    override suspend fun getIdToken(): String? {
        return tokenMutex.withLock {
            val currentSession = cachedSession

            if (currentSession == null) {
                return@withLock null
            } else {
                val refreshed = refreshSessionIfNeeded(currentSession)

                if (refreshed == null) {
                    clearPersistedSession()
                    return@withLock null
                } else if (refreshed != currentSession) {
                    persistSession(refreshed)
                }
                return@withLock refreshed.idToken
            }
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

    override suspend fun signInWithKakaoIdToken(idToken: String): FirebaseAuthSession? {
        val session = delegate.signInWithKakaoIdToken(idToken)

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

    override fun getCurrentAuthProvider(): AuthProvider {
        val currentSession = cachedSession
        return currentSession?.authProvider ?: delegate.getCurrentAuthProvider()
    }

    override fun supportsEmailPasswordAuth(): Boolean {
        return delegate.supportsEmailPasswordAuth()
    }

    override fun getCachedSignupCompleted(): Boolean? {
        return cachedSession?.signupCompleted
    }

    override fun setCachedSignupCompleted(value: Boolean) {
        val current = cachedSession ?: return
        val updated = current.copy(signupCompleted = value)

        persistSession(updated)
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
        }

        val refreshResult = runCatching { delegate.refreshSession(session) }
        val refreshed = refreshResult.getOrNull()

        if (refreshed != null) {
            return refreshed
        }

        val refreshFailure = refreshResult.exceptionOrNull()
        if (refreshFailure != null && !refreshFailure.isPermanentRefreshFailure()) {
            return session
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

private fun Throwable.isPermanentRefreshFailure(): Boolean {
    val response = when (this) {
        is FirebaseAuthRestException -> responseBody
        else -> message.orEmpty()
    }.uppercase()
    return response.contains("INVALID_REFRESH_TOKEN") ||
        response.contains("TOKEN_EXPIRED") ||
        response.contains("USER_DISABLED") ||
        response.contains("USER_NOT_FOUND")
}
