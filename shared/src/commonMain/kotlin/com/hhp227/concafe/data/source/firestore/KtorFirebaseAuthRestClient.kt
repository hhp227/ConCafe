package com.hhp227.concafe.data.source.firestore

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

class KtorFirebaseAuthRestClient(
    private val httpClient: HttpClient
) : FirebaseAuthRestClient {
    override suspend fun postJson(url: String, body: String): String {
        val response = httpClient.post(url) {
            contentType(ContentType.Application.Json)
            setBody(body)
            header("Accept", "application/json")
        }
        val responseBody = response.bodyAsText()

        if (response.status !in HttpStatusCode.OK..HttpStatusCode.MultipleChoices) {
            throw FirebaseAuthRestException(
                statusCode = response.status.value,
                responseBody = responseBody
            )
        }
        return responseBody
    }

    override suspend fun postFormUrlEncoded(url: String, body: String): String {
        val response = httpClient.post(url) {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(body)
            header("Accept", "application/json")
        }
        val responseBody = response.bodyAsText()

        if (response.status !in HttpStatusCode.OK..HttpStatusCode.MultipleChoices) {
            throw FirebaseAuthRestException(
                statusCode = response.status.value,
                responseBody = responseBody
            )
        }
        return responseBody
    }
}
