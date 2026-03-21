package com.hhp227.concafe.presentation.auth.signin

interface GoogleIdTokenProvider {
    suspend fun getGoogleIdToken(): String
}
