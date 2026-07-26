package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.firestore.FirestoreAuthTokenProvider
import com.hhp227.concafe.data.source.firestore.FirestoreConfig
import com.hhp227.concafe.domain.repository.StorageRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.decodeURLPart
import io.ktor.http.encodeURLPathPart
import io.ktor.http.encodeURLQueryComponent
import kotlin.random.Random
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class StorageRepositoryImpl(
    private val httpClient: HttpClient,

    private val tokenProvider: FirestoreAuthTokenProvider,

    private val firestoreConfig: FirestoreConfig
) : StorageRepository {
    private fun resolveBucketName(): String {
        return "${firestoreConfig.projectId}.firebasestorage.app"
    }

    private fun resolveFileName(fileName: String?): String {
        val trimmed = fileName?.trim().orEmpty()
        val baseName = trimmed.substringAfterLast('/').substringAfterLast('\\')
        val extension = if (baseName.contains('.')) {
            ".${baseName.substringAfterLast('.')}"
        } else {
            DEFAULT_EXTENSION
        }
        val seed = Clock.System.now().toEpochMilliseconds()
        val random = Random.nextInt(100_000, 999_999)
        return "$seed-$random$extension"
    }

    private fun resolveObjectPath(folder: String, fileName: String?): String {
        val normalizedFolder = folder.trim().trim('/')
        val resolvedFileName = resolveFileName(fileName)
        return if (normalizedFolder.isBlank()) {
            resolvedFileName
        } else {
            "$normalizedFolder/$resolvedFileName"
        }
    }

    private fun buildUploadUrl(bucket: String, objectPath: String): String {
        val encodedName = objectPath.encodeURLQueryComponent()
        return "$FIREBASE_STORAGE_BASE_URL/b/$bucket/o?uploadType=media&name=$encodedName"
    }

    private fun buildDownloadUrl(bucket: String, objectPath: String, token: String?): String {
        val encodedPath = objectPath.encodeURLPathPart()
        return if (token.isNullOrBlank()) {
            "$FIREBASE_STORAGE_BASE_URL/b/$bucket/o/$encodedPath?alt=media"
        } else {
            "$FIREBASE_STORAGE_BASE_URL/b/$bucket/o/$encodedPath?alt=media&token=${token.encodeURLQueryComponent()}"
        }
    }

    private fun parseStorageUrl(imageUrl: String): Pair<String, String>? {
        if (!imageUrl.startsWith(FIREBASE_STORAGE_BASE_URL)) {
            return null
        }
        val bucketMarker = "/b/"
        val objectMarker = "/o/"
        val bucketStart = imageUrl.indexOf(bucketMarker)
        val objectStart = imageUrl.indexOf(objectMarker)

        if (bucketStart < 0 || objectStart < 0 || objectStart <= bucketStart + bucketMarker.length) {
            return null
        }
        val bucket = imageUrl.substring(bucketStart + bucketMarker.length, objectStart)
            .trim()
            .takeIf { value -> value.isNotEmpty() }
            ?: return null
        val encodedPathStart = objectStart + objectMarker.length
        val encodedPathEnd = imageUrl.indexOf('?', startIndex = encodedPathStart)
        val encodedPath = if (encodedPathEnd < 0) {
            imageUrl.substring(encodedPathStart)
        } else {
            imageUrl.substring(encodedPathStart, encodedPathEnd)
        }.trim()
        if (encodedPath.isEmpty()) {
            return null
        }
        return bucket to encodedPath.decodeURLPart()
    }

    private fun parseDownloadToken(responseBody: String): String? {
        val root = Json.parseToJsonElement(responseBody).jsonObject
        val tokens = root["downloadTokens"]?.jsonPrimitive?.content
        return tokens
            ?.split(',')
            ?.firstOrNull()
            ?.trim()
            ?.takeIf { token -> token.isNotBlank() }
    }

    private fun resolveContentType(fileName: String?): ContentType {
        val extension = fileName
            ?.substringAfterLast('.', missingDelimiterValue = "")
            ?.lowercase()
            .orEmpty()
        return if (extension == "png") {
            ContentType.Image.PNG
        } else if (extension == "webp") {
            ContentType.parse("image/webp")
        } else if (extension == "gif") {
            ContentType.Image.GIF
        } else {
            ContentType.Image.JPEG
        }
    }

    override suspend fun uploadImage(localPath: String, folder: String): String {
        throw UnsupportedOperationException("uploadImage(localPath) is not supported. Use uploadImageData(bytes, folder, fileName).")
    }

    override suspend fun uploadImageData(bytes: ByteArray, folder: String, fileName: String?): String {
        if (bytes.isEmpty()) {
            throw IllegalArgumentException("image bytes are empty")
        }
        if (folder.isBlank()) {
            throw IllegalArgumentException("folder is required")
        }

        val idToken = tokenProvider.getIdToken()
        if (idToken.isNullOrBlank()) {
            throw IllegalStateException("로그인이 필요합니다. Firebase Storage 업로드 토큰이 없습니다.")
        }

        val bucket = resolveBucketName()
        val objectPath = resolveObjectPath(folder, fileName)
        val uploadUrl = buildUploadUrl(bucket, objectPath)
        val contentType = resolveContentType(fileName)
        val response = httpClient.post(uploadUrl) {
            header("Authorization", "Bearer $idToken")
            header("Accept", "application/json")
            contentType(contentType)
            setBody(bytes)
        }
        val responseBody = response.bodyAsText()

        if (response.status !in HttpStatusCode.OK..HttpStatusCode.MultipleChoices) {
            throw IllegalStateException("Firebase storage upload failed(${response.status.value}): $responseBody")
        }

        val downloadToken = parseDownloadToken(responseBody)
        return buildDownloadUrl(bucket, objectPath, downloadToken)
    }

    override suspend fun deleteImageByUrl(imageUrl: String) {
        val normalizedUrl = imageUrl.trim()
        val parsed = parseStorageUrl(normalizedUrl)

        if (parsed == null) {
            return
        }
        val idToken = tokenProvider.getIdToken()
        if (idToken.isNullOrBlank()) {
            throw IllegalStateException("로그인이 필요합니다. Firebase Storage 삭제 토큰이 없습니다.")
        }
        val bucket = parsed.first
        val objectPath = parsed.second
        val deleteUrl = "$FIREBASE_STORAGE_BASE_URL/b/$bucket/o/${objectPath.encodeURLPathPart()}"
        val response = httpClient.delete(deleteUrl) {
            header("Authorization", "Bearer $idToken")
            header("Accept", "application/json")
        }
        val responseBody = response.bodyAsText()

        if (response.status !in HttpStatusCode.OK..HttpStatusCode.MultipleChoices) {
            throw IllegalStateException("Firebase storage delete failed(${response.status.value}): $responseBody")
        }
    }
}

private const val FIREBASE_STORAGE_BASE_URL = "https://firebasestorage.googleapis.com/v0"
private const val DEFAULT_EXTENSION = ".jpg"
