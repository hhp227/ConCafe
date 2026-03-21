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
    private val httpClient: HttpClient
) : FirestoreRestApi {
    override suspend fun get(path: String, idToken: String?): String {
        val response = httpClient.get(path) {
            contentType(ContentType.Application.Json)
            applyAuthorization(idToken)
        }

        return readResponseBodyOrThrow("GET", path, response.status, response.bodyAsText())
    }

    override suspend fun post(path: String, body: String, idToken: String?): String {
        val response = httpClient.post(path) {
            contentType(ContentType.Application.Json)
            setBody(body)
            applyAuthorization(idToken)
        }

        return readResponseBodyOrThrow(HttpMethod.Post.value, path, response.status, response.bodyAsText())
    }

    override suspend fun patch(path: String, body: String, idToken: String?): String {
        val response = httpClient.patch(path) {
            contentType(ContentType.Application.Json)
            setBody(body)
            applyAuthorization(idToken)
        }

        return readResponseBodyOrThrow(HttpMethod.Patch.value, path, response.status, response.bodyAsText())
    }

    override suspend fun delete(path: String, idToken: String?) {
        val response = httpClient.delete(path) {
            contentType(ContentType.Application.Json)
            applyAuthorization(idToken)
        }
        readResponseBodyOrThrow(HttpMethod.Delete.value, path, response.status, response.bodyAsText())
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
