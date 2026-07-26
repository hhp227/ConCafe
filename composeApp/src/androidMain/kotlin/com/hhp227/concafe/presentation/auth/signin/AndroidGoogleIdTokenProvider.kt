package com.hhp227.concafe.presentation.auth.signin

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

class AndroidGoogleIdTokenProvider(
    private val activityProvider: () -> Activity?
) : GoogleIdTokenProvider {
    private fun resolveGoogleWebClientId(activity: Activity): String {
        val overriddenClientId = System.getProperty("concafe.google.webClientId")

        if (!overriddenClientId.isNullOrBlank()) {
            return overriddenClientId
        }

        val resourceId = activity.resources.getIdentifier(
            "default_web_client_id",
            "string",
            activity.packageName
        )

        if (resourceId != 0) {
            val resourceClientId = activity.getString(resourceId)

            if (resourceClientId.isNotBlank()) {
                return resourceClientId
            }
        }
        return DEFAULT_GOOGLE_WEB_CLIENT_ID
    }

    override suspend fun getGoogleIdToken(): String {
        val activity = activityProvider()
            ?: throw IllegalStateException("google sign-in requires current activity")
        val googleWebClientId = resolveGoogleWebClientId(activity)
        val credentialManager = CredentialManager.create(activity)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(googleWebClientId)
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        val result = try {
            credentialManager.getCredential(
                context = activity,
                request = request
            )
        } catch (error: GetCredentialException) {
            throw IllegalStateException(
                "google credential failed: ${error.message ?: "unknown"}",
                error
            )
        }
        val credential = result.credential

        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            return GoogleIdTokenCredential.createFrom(credential.data).idToken
        }
        throw IllegalStateException("failed to get google id token from credential")
    }
}

private const val DEFAULT_GOOGLE_WEB_CLIENT_ID =
    "387905493709-o041q6su1mgu9vf2719ldnihhpfvncnj.apps.googleusercontent.com"
