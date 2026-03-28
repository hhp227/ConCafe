package com.hhp227.concafe.presentation.auth.signin

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.awt.Desktop
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import java.util.concurrent.Executors
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout

class JvmGoogleIdTokenProvider : GoogleIdTokenProvider {
    private val httpClient = HttpClient.newBuilder().build()

    private fun createCallbackServer(
        authCodeDeferred: CompletableDeferred<String>,
        state: String
    ): Pair<HttpServer, String> {
        val bindAddress = InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0)
        val server = HttpServer.create(bindAddress, 0)
        val callbackPath = "/oauth2/callback"
        val callbackUri = "http://127.0.0.1:${server.address.port}$callbackPath"

        server.createContext(callbackPath) { exchange ->
            handleCallback(
                exchange = exchange,
                authCodeDeferred = authCodeDeferred,
                state = state
            )
        }
        server.executor = Executors.newSingleThreadExecutor()

        return server to callbackUri
    }

    private fun handleCallback(
        exchange: HttpExchange,
        authCodeDeferred: CompletableDeferred<String>,
        state: String
    ) {
        val requestParameters = parseForm(exchange.requestURI.rawQuery.orEmpty())
        val error = requestParameters["error"]
        val callbackState = requestParameters["state"]
        val authCode = requestParameters["code"]

        if (!error.isNullOrBlank()) {
            authCodeDeferred.completeExceptionally(
                IllegalStateException("google sign-in failed: $error")
            )
        } else if (callbackState != state) {
            authCodeDeferred.completeExceptionally(
                IllegalStateException("google sign-in failed: invalid callback state")
            )
        } else if (!authCode.isNullOrBlank()) {
            authCodeDeferred.complete(authCode)
        } else {
            authCodeDeferred.completeExceptionally(
                IllegalStateException("google auth code is missing from callback")
            )
        }

        val response = """
            <html><body><h3>ConCafe</h3><p>Google 로그인 처리가 완료되었습니다. 창을 닫아주세요.</p></body></html>
        """.trimIndent().toByteArray()
        exchange.sendResponseHeaders(200, response.size.toLong())
        exchange.responseBody.use { it.write(response) }
        exchange.close()
    }

    private fun parseForm(value: String): Map<String, String> {
        if (value.isBlank()) {
            return emptyMap()
        } else {
            val pairs = value.split("&")
            return buildMap {
                pairs.forEach { pair ->
                    val keyValue = pair.split("=", limit = 2)
                    val key = keyValue.getOrNull(0).orEmpty()
                    val rawValue = keyValue.getOrNull(1).orEmpty()
                    if (key.isNotBlank()) {
                        put(urlDecode(key), urlDecode(rawValue))
                    }
                }
            }
        }
    }

    private fun buildAuthUri(
        redirectUri: String,
        state: String,
        codeChallenge: String
    ): URI {
        val query = buildString {
            append("response_type=code")
            append("&client_id=${urlEncode(googleClientId())}")
            append("&redirect_uri=${urlEncode(redirectUri)}")
            append("&scope=${urlEncode("openid email profile")}")
            append("&state=${urlEncode(state)}")
            append("&code_challenge=${urlEncode(codeChallenge)}")
            append("&code_challenge_method=S256")
            append("&prompt=select_account")
        }
        return URI("$GOOGLE_OAUTH_AUTHORIZE_URL?$query")
    }

    private fun exchangeAuthCodeForIdToken(
        authCode: String,
        codeVerifier: String,
        redirectUri: String
    ): String {
        val requestBody = buildString {
            append("code=${urlEncode(authCode)}")
            append("&client_id=${urlEncode(googleClientId())}")
            append("&code_verifier=${urlEncode(codeVerifier)}")
            append("&redirect_uri=${urlEncode(redirectUri)}")
            append("&grant_type=authorization_code")
        }
        val request = HttpRequest.newBuilder()
            .uri(URI(GOOGLE_OAUTH_TOKEN_URL))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() !in 200..299) {
            throw IllegalStateException("google token exchange failed: status=${response.statusCode()}")
        } else {
            val idToken = parseJsonString(response.body(), "id_token")
            if (idToken.isNullOrBlank()) {
                throw IllegalStateException("google id_token is missing in token response")
            } else {
                return idToken
            }
        }
    }

    private fun parseJsonString(json: String, key: String): String? {
        val pattern = Regex("""\"$key\"\s*:\s*\"([^\"]+)\"""")
        return pattern.find(json)?.groupValues?.getOrNull(1)
    }

    private fun googleClientId(): String {
        val configuredClientId = System.getProperty("concafe.google.clientId")

        return if (configuredClientId.isNullOrBlank()) {
            DEFAULT_GOOGLE_CLIENT_ID
        } else {
            configuredClientId
        }
    }

    private fun urlEncode(value: String): String {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.name())
    }

    private fun urlDecode(value: String): String {
        return URLDecoder.decode(value, StandardCharsets.UTF_8.name())
    }

    private fun generateCodeVerifier(): String {
        val randomBytes = ByteArray(32)
        SecureRandom().nextBytes(randomBytes)
        return base64UrlEncode(randomBytes)
    }

    private fun generateCodeChallenge(codeVerifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(codeVerifier.toByteArray(StandardCharsets.UTF_8))
        return base64UrlEncode(digest)
    }

    private fun base64UrlEncode(value: ByteArray): String {
        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(value)
    }

    override suspend fun getGoogleIdToken(): String {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            throw IllegalStateException("desktop browser is not supported")
        } else {
            val codeVerifier = generateCodeVerifier()
            val codeChallenge = generateCodeChallenge(codeVerifier)
            val state = UUID.randomUUID().toString()
            val authCodeDeferred = CompletableDeferred<String>()
            val (server, callbackUri) = createCallbackServer(authCodeDeferred, state)
            val authUri = buildAuthUri(
                redirectUri = callbackUri,
                state = state,
                codeChallenge = codeChallenge
            )

            server.start()
            return try {
                Desktop.getDesktop().browse(authUri)
                val authCode = withTimeout(GOOGLE_SIGN_IN_TIMEOUT_MS) {
                    authCodeDeferred.await()
                }
                exchangeAuthCodeForIdToken(
                    authCode = authCode,
                    codeVerifier = codeVerifier,
                    redirectUri = callbackUri
                )
            } finally {
                server.stop(0)
                (server.executor as? java.util.concurrent.ExecutorService)?.shutdownNow()
            }
        }
    }
}

private const val GOOGLE_OAUTH_AUTHORIZE_URL = "https://accounts.google.com/o/oauth2/v2/auth"
private const val GOOGLE_OAUTH_TOKEN_URL = "https://oauth2.googleapis.com/token"

private const val DEFAULT_GOOGLE_CLIENT_ID =
    "387905493709-o041q6su1mgu9vf2719ldnihhpfvncnj.apps.googleusercontent.com"

private const val GOOGLE_SIGN_IN_TIMEOUT_MS = 180_000L
