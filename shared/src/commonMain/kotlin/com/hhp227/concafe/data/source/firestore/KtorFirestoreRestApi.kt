package com.hhp227.concafe.data.source.firestore

import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

class KtorFirestoreRestApi(
    private val httpClient: HttpClient,
    private val apiKey: String
) : FirestoreRestApi {
    override suspend fun get(path: String, idToken: String?): String {
        val resolvedPath = path.withApiKey(apiKey)
        val response = httpClient.get(resolvedPath) {
            contentType(ContentType.Application.Json)
            applyAuthorization(idToken)
        }
        return readResponseBodyOrThrow("GET", resolvedPath, response.status, response.bodyAsText())
    }

    override suspend fun post(path: String, body: String, idToken: String?): String {
        val resolvedPath = path.withApiKey(apiKey)
        val response = httpClient.post(resolvedPath) {
            contentType(ContentType.Application.Json)
            setBody(body)
            applyAuthorization(idToken)
        }
        return readResponseBodyOrThrow(HttpMethod.Post.value, resolvedPath, response.status, response.bodyAsText())
    }

    override suspend fun patch(path: String, body: String, idToken: String?, updateMask: List<String>): String {
        val resolvedPath = path.withApiKey(apiKey).let { base ->
            if (updateMask.isEmpty()) base
            else {
                val maskQuery = updateMask.joinToString("&") { "updateMask.fieldPaths=$it" }

                if (base.contains("?")) "$base&$maskQuery"
                else "$base?$maskQuery"
            }
        }
        val response = httpClient.patch(resolvedPath) {
            contentType(ContentType.Application.Json)
            setBody(body)
            applyAuthorization(idToken)
        }
        return readResponseBodyOrThrow(HttpMethod.Patch.value, resolvedPath, response.status, response.bodyAsText())
    }

    override suspend fun delete(path: String, idToken: String?) {
        val resolvedPath = path.withApiKey(apiKey)
        val response = httpClient.delete(resolvedPath) {
            contentType(ContentType.Application.Json)
            applyAuthorization(idToken)
        }
        readResponseBodyOrThrow(HttpMethod.Delete.value, resolvedPath, response.status, response.bodyAsText())
    }
}

private fun String.withApiKey(apiKey: String): String {
    val normalizedKey = apiKey.trim()

    if (normalizedKey.isEmpty()) {
        return this
    }
    return if (contains("?")) {
        "$this&key=$normalizedKey"
    } else {
        "$this?key=$normalizedKey"
    }
}

private fun readResponseBodyOrThrow(method: String, path: String, status: HttpStatusCode, body: String): String {
    if (status !in HttpStatusCode.OK..HttpStatusCode.MultipleChoices) {
        throw IllegalStateException("Firestore $method request failed(${status.value}) at $path: $body")
    }

    return body
}

private fun io.ktor.client.request.HttpRequestBuilder.applyAuthorization(idToken: String?) {
    if (!idToken.isNullOrBlank()) {
        header("Authorization", "Bearer $idToken")
    }
}
