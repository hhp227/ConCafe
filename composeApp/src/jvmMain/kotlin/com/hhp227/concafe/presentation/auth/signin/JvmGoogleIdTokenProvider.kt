package com.hhp227.concafe.presentation.auth.signin

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.awt.Desktop
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.Executors
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout

class JvmGoogleIdTokenProvider : GoogleIdTokenProvider {
    private fun buildAuthUri(redirectUri: String): URI {
        val nonce = UUID.randomUUID().toString()
        val query = buildString {
            append("response_type=id_token")
            append("&client_id=${urlEncode(googleClientId())}")
            append("&redirect_uri=${urlEncode(redirectUri)}")
            append("&scope=${urlEncode("openid email profile")}")
            append("&nonce=${urlEncode(nonce)}")
            append("&prompt=select_account")
            append("&response_mode=form_post")
        }
        return URI("$GOOGLE_OAUTH_AUTHORIZE_URL?$query")
    }

    private fun createCallbackServer(
        idTokenDeferred: CompletableDeferred<String>
    ): Pair<HttpServer, String> {
        val bindAddress = InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0)
        val server = HttpServer.create(bindAddress, 0)
        val callbackPath = "/oauth2/callback"
        val callbackUri = "http://127.0.0.1:${server.address.port}$callbackPath"

        server.createContext(callbackPath) { exchange ->
            handleCallback(exchange, idTokenDeferred)
        }
        server.executor = Executors.newSingleThreadExecutor()

        return server to callbackUri
    }

    private fun handleCallback(
        exchange: HttpExchange,
        idTokenDeferred: CompletableDeferred<String>
    ) {
        val requestParameters = when (exchange.requestMethod.uppercase()) {
            "POST" -> parseForm(exchange.requestBody.readBytes().decodeToString())
            else -> parseForm(exchange.requestURI.rawQuery.orEmpty())
        }
        val error = requestParameters["error"]
        val idToken = requestParameters["id_token"]

        if (!error.isNullOrBlank()) {
            idTokenDeferred.completeExceptionally(
                IllegalStateException("google sign-in failed: $error")
            )
        } else if (!idToken.isNullOrBlank()) {
            idTokenDeferred.complete(idToken)
        } else {
            idTokenDeferred.completeExceptionally(
                IllegalStateException("google id_token is missing from callback")
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
        }

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

    private fun googleClientId(): String {
        val configuredClientId = System.getProperty("concafe.google.clientId")

        return if (configuredClientId.isNullOrBlank()) {
            DEFAULT_GOOGLE_CLIENT_ID
        } else {
            configuredClientId
        }
    }

    private fun urlEncode(value: String): String {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8.name())
    }

    private fun urlDecode(value: String): String {
        return URLDecoder.decode(value, StandardCharsets.UTF_8.name())
    }

    override suspend fun getGoogleIdToken(): String {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            throw IllegalStateException("desktop browser is not supported")
        }

        val idTokenDeferred = CompletableDeferred<String>()
        val (server, callbackUri) = createCallbackServer(idTokenDeferred)
        val authUri = buildAuthUri(callbackUri)

        server.start()
        return try {
            Desktop.getDesktop().browse(authUri)
            withTimeout(GOOGLE_SIGN_IN_TIMEOUT_MS) {
                idTokenDeferred.await()
            }
        } finally {
            server.stop(0)
            (server.executor as? java.util.concurrent.ExecutorService)?.shutdownNow()
        }
    }
}

private const val GOOGLE_OAUTH_AUTHORIZE_URL = "https://accounts.google.com/o/oauth2/v2/auth"

private const val DEFAULT_GOOGLE_CLIENT_ID =
    "387905493709-o041q6su1mgu9vf2719ldnihhpfvncnj.apps.googleusercontent.com"

private const val GOOGLE_SIGN_IN_TIMEOUT_MS = 180_000L
