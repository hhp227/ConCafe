package com.hhp227.concafe.data.source.firestore

class PersistedFirebaseAuthTokenProvider(
    private val delegate: FirestoreAuthTokenProvider,
    private val sessionStore: FirebaseAuthSessionStore
) : FirestoreAuthTokenProvider {
    private var cachedSession: FirebaseAuthSession? = sessionStore.load()

    override suspend fun getIdToken(): String? {
        val currentSession = cachedSession
        return if (currentSession != null) {
            currentSession.idToken
        } else {
            delegate.getIdToken()
        }
    }

    override suspend fun signInWithEmailPassword(email: String, password: String): FirebaseAuthSession? {
        val session = delegate.signInWithEmailPassword(email, password)

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
}

interface FirebaseAuthSessionStore {
    fun load(): FirebaseAuthSession?

    fun save(session: FirebaseAuthSession)

    fun clear()
}
