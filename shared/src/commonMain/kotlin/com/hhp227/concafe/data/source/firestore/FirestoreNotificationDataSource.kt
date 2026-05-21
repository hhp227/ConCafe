package com.hhp227.concafe.data.source.firestore

import com.hhp227.concafe.data.source.NotificationDataSource
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.AppNotification
import com.hhp227.concafe.domain.model.NotificationQuietHoursMode
import com.hhp227.concafe.domain.model.UserNotificationSettings
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class FirestoreNotificationDataSource(
    private val config: FirestoreConfig,
    private val restApi: FirestoreRestApi,
    private val tokenProvider: FirestoreAuthTokenProvider
) : NotificationDataSource {
    override suspend fun getNotifications(userId: String, cursor: String?, pageSize: Int): PagedResult<AppNotification> {
        val safePageSize = if (pageSize > 0) pageSize else 20
        val documents = runUserNotificationPageQuery(
            userId = userId,
            cursor = cursor,
            limit = safePageSize,
            idToken = tokenProvider.getIdToken()
        )
        val parsedItems = documents.mapNotNull { document ->
            parseNotificationDocument(userId = userId, document = document)
        }
        val sortedItems = parsedItems.sortedByDescending { item -> item.createdAt }
        val nextCursor = if (parsedItems.size < safePageSize) {
            null
        } else {
            documents.lastOrNull()?.toStringFieldCursor(fieldPath = "createdAt")
        }
        return PagedResult(
            items = sortedItems,
            nextCursor = nextCursor,
            hasNext = nextCursor != null
        )
    }

    override suspend fun getUnreadNotificationCount(userId: String): Int {
        val path = "${config.documentBasePath()}/${FirestorePaths.USERS}/$userId:runAggregationQuery"
        val body = """
            {
              "structuredAggregationQuery": {
                "aggregations": [
                  { "alias": "count", "count": {} }
                ],
                "structuredQuery": {
                  "from": [
                    { "collectionId": "${FirestorePaths.USER_NOTIFICATIONS}" }
                  ],
                  "where": {
                    "fieldFilter": {
                      "field": { "fieldPath": "isRead" },
                      "op": "EQUAL",
                      "value": { "booleanValue": false }
                    }
                  }
                }
              }
            }
        """.trimIndent()
        val response = restApi.post(path = path, body = body, idToken = tokenProvider.getIdToken())
        return parseCountAggregationResponse(response)
    }

    override suspend fun markNotificationAsRead(userId: String, notificationId: String) {
        val idToken = tokenProvider.getIdToken()
        val path = "${config.documentBasePath()}/${FirestorePaths.USERS}/$userId/${FirestorePaths.USER_NOTIFICATIONS}/$notificationId"
        val body = firestoreDocumentBody(
            fields = mapOf(
                "isRead" to firestoreBoolean(true),
                "readAt" to firestoreString(Clock.System.now().toString())
            )
        )

        runCatching {
            restApi.patch(
                path = path,
                body = body,
                idToken = idToken,
                updateMask = listOf("isRead", "readAt")
            )
        }.getOrElse { throwable ->
            throw IllegalStateException("Firestore request failed at $path", throwable)
        }
    }

    override suspend fun getNotificationSettings(userId: String): UserNotificationSettings {
        val path = "${config.documentBasePath()}/${FirestorePaths.USERS}/$userId/${FirestorePaths.USER_NOTIFICATION_SETTINGS}/default"
        val document = runCatching {
            restApi.get(path = path, idToken = tokenProvider.getIdToken())
        }.getOrNull()
        if (document.isNullOrBlank()) return UserNotificationSettings.default()
        val parsed = runCatching { Json.parseToJsonElement(document).jsonObject }.getOrNull()
        if (parsed == null || parsed["fields"] == null) return UserNotificationSettings.default()
        return parseNotificationSettingsDocument(parsed)
    }

    override suspend fun updateNotificationSettings(
        userId: String,
        settings: UserNotificationSettings
    ): UserNotificationSettings {
        val idToken = tokenProvider.getIdToken()
        val path = "${config.documentBasePath()}/${FirestorePaths.USERS}/$userId/${FirestorePaths.USER_NOTIFICATION_SETTINGS}/default"
        val body = firestoreDocumentBody(
            fields = mapOf(
                "userId" to firestoreString(userId),
                "isPushNotificationsEnabled" to firestoreBoolean(settings.isPushNotificationsEnabled),
                "isShiftNotificationsEnabled" to firestoreBoolean(settings.isShiftNotificationsEnabled),
                "isBirthdayNotificationsEnabled" to firestoreBoolean(settings.isBirthdayNotificationsEnabled),
                "isNoticeNotificationsEnabled" to firestoreBoolean(settings.isNoticeNotificationsEnabled),
                "isFollowNotificationsEnabled" to firestoreBoolean(settings.isFollowNotificationsEnabled),
                "isEventNotificationsEnabled" to firestoreBoolean(settings.isEventNotificationsEnabled),
                "isCommunityNotificationsEnabled" to firestoreBoolean(settings.isCommunityNotificationsEnabled),
                "quietHoursMode" to firestoreString(settings.quietHoursMode.name),
                "updatedAt" to firestoreString(Clock.System.now().toString())
            )
        )

        runCatching {
            restApi.patch(path = path, body = body, idToken = idToken)
        }.getOrElse { throwable ->
            throw IllegalStateException("Failed to update notification settings", throwable)
        }
        return settings
    }

    override suspend fun registerPushToken(userId: String, platform: String, token: String) {
        val normalizedPlatform = platform.trim()
        val normalizedToken = token.trim()
        val normalizedUserId = userId.trim()

        if (normalizedUserId.isEmpty() || normalizedPlatform.isEmpty() || normalizedToken.isEmpty()) {
            throw IllegalArgumentException("invalid push token payload")
        }
        val idToken = tokenProvider.getIdToken()
        val tokenDocumentId = "token_${normalizedToken.hashCode().toString().replace("-", "_")}"
        val now = Clock.System.now().toString()
        val path = "${config.documentBasePath()}/${FirestorePaths.USERS}/$normalizedUserId/${FirestorePaths.USER_DEVICE_TOKENS}/$tokenDocumentId"
        val body = firestoreDocumentBody(
            fields = mapOf(
                "userId" to firestoreString(normalizedUserId),
                "platform" to firestoreString(normalizedPlatform),
                "token" to firestoreString(normalizedToken),
                "locale" to firestoreString("ko-KR"),
                "isEnabled" to firestoreBoolean(true),
                "updatedAt" to firestoreString(now),
                "createdAt" to firestoreString(now)
            )
        )

        runCatching {
            restApi.patch(path = path, body = body, idToken = idToken)
        }.getOrElse { throwable ->
            throw IllegalStateException("Failed to register push token", throwable)
        }
    }

    override suspend fun disableAllPushTokens(userId: String) {
        val normalizedUserId = userId.trim()
        if (normalizedUserId.isEmpty()) throw IllegalArgumentException("invalid push token payload")

        val idToken = tokenProvider.getIdToken()
        val collectionPath = "${config.documentBasePath()}/${FirestorePaths.USERS}/$normalizedUserId/${FirestorePaths.USER_DEVICE_TOKENS}"
        val response = restApi.get(path = collectionPath, idToken = idToken)
        val parsed = Json.parseToJsonElement(response).jsonObject
        val documents = parsed["documents"]?.jsonArray.orEmpty()
            .map { element -> element.jsonObject }

        if (documents.isEmpty()) return
        val now = Clock.System.now().toString()
        for (document in documents) {
            val name = document["name"]?.jsonPrimitive?.contentOrNull ?: continue
            val tokenDocumentId = name.substringAfterLast("/")
            val path = "${config.documentBasePath()}/${FirestorePaths.USERS}/$normalizedUserId/${FirestorePaths.USER_DEVICE_TOKENS}/$tokenDocumentId"
            val body = firestoreDocumentBody(
                fields = mapOf(
                    "isEnabled" to firestoreBoolean(false),
                    "updatedAt" to firestoreString(now)
                )
            )
            restApi.patch(
                path = path,
                body = body,
                idToken = idToken,
                updateMask = listOf("isEnabled", "updatedAt")
            )
        }
    }

    override suspend fun sendFanAnnouncement(
        userId: String,
        cafeId: String,
        castId: String,
        title: String,
        body: String
    ) {
        val normalizedUserId = userId.trim()
        val normalizedCafeId = cafeId.trim()
        val normalizedCastId = castId.trim()
        val normalizedTitle = title.trim()
        val normalizedBody = body.trim()

        if (normalizedUserId.isEmpty() || normalizedCafeId.isEmpty() || normalizedCastId.isEmpty()) {
            throw IllegalArgumentException("fan announcement target is required")
        }
        if (normalizedTitle.isEmpty()) throw IllegalArgumentException("fan announcement title is required")
        if (normalizedBody.isEmpty()) throw IllegalArgumentException("fan announcement body is required")
        if (normalizedTitle.length > 50) throw IllegalArgumentException("fan announcement title is too long")
        if (normalizedBody.length > 300) throw IllegalArgumentException("fan announcement body is too long")

        val idToken = tokenProvider.getIdToken()
        val requestId = nextFirestoreEntityId("fan-announcement")
        val now = Clock.System.now().toString()
        val path = "${config.documentBasePath()}/${FirestorePaths.FAN_ANNOUNCEMENT_REQUESTS}/$requestId"
        val bodyPayload = firestoreDocumentBody(
            fields = mapOf(
                "userId" to firestoreString(normalizedUserId),
                "cafeId" to firestoreString(normalizedCafeId),
                "castId" to firestoreString(normalizedCastId),
                "title" to firestoreString(normalizedTitle),
                "body" to firestoreString(normalizedBody),
                "createdAt" to firestoreString(now)
            )
        )
        restApi.patch(path = path, body = bodyPayload, idToken = idToken)
    }

    private suspend fun runUserNotificationPageQuery(
        userId: String,
        cursor: String?,
        limit: Int,
        idToken: String?
    ): List<JsonObject> {
        val safeLimit = if (limit > 0) limit else 1
        val startAfterSection = cursor.toStringFieldStartAfterSection()
        val path = "${config.documentBasePath()}/${FirestorePaths.USERS}/$userId:runQuery"
        val body = """
            {
              "structuredQuery": {
                "from": [
                  {
                    "collectionId": "${FirestorePaths.USER_NOTIFICATIONS}"
                  }
                ],
                "orderBy": [
                  {
                    "field": { "fieldPath": "createdAt" },
                    "direction": "DESCENDING"
                  }
                ]$startAfterSection,
                "limit": $safeLimit
              }
            }
        """.trimIndent()
        val response = restApi.post(path = path, body = body, idToken = idToken)
        val parsed = Json.parseToJsonElement(response).jsonArray
        return parsed.mapNotNull { element ->
            element.jsonObject["document"]?.jsonObject
        }
    }

    private fun parseNotificationDocument(userId: String, document: JsonObject): AppNotification? {
        val fields = document["fields"]?.jsonObject ?: return null
        val name = document["name"]?.jsonPrimitive?.contentOrNull ?: return null
        val notificationId = name.substringAfterLast("/")
        val createdAt = fields.getFirestoreString("createdAt").orEmpty()
        val targetUserId = fields.getFirestoreString("userId")
            ?: fields.getFirestoreString("recipientUserId")
            ?: userId
        val relativeTime = fields.getFirestoreString("relativeTime")
            ?: fields.getFirestoreString("createdAtLabel")
            ?: "최근"
        return AppNotification(
            id = notificationId,
            userId = targetUserId,
            title = fields.getFirestoreString("title").orEmpty(),
            body = fields.getFirestoreString("body").orEmpty(),
            type = fields.getFirestoreString("type").orEmpty(),
            targetId = fields.getFirestoreString("targetId"),
            isRead = fields.getFirestoreBoolean("isRead") ?: false,
            createdAt = createdAt,
            relativeTime = relativeTime
        )
    }

    private fun parseNotificationSettingsDocument(document: JsonObject): UserNotificationSettings {
        val fields = document["fields"]?.jsonObject ?: return UserNotificationSettings.default()
        val quietHoursMode = when (fields.getFirestoreString("quietHoursMode")?.uppercase()) {
            NotificationQuietHoursMode.OFF.name -> NotificationQuietHoursMode.OFF
            NotificationQuietHoursMode.NIGHT.name -> NotificationQuietHoursMode.NIGHT
            NotificationQuietHoursMode.ALL_DAY.name -> NotificationQuietHoursMode.ALL_DAY
            else -> NotificationQuietHoursMode.OFF
        }
        return UserNotificationSettings(
            isPushNotificationsEnabled = fields.getFirestoreBoolean("isPushNotificationsEnabled") ?: true,
            isShiftNotificationsEnabled = fields.getFirestoreBoolean("isShiftNotificationsEnabled") ?: true,
            isBirthdayNotificationsEnabled = fields.getFirestoreBoolean("isBirthdayNotificationsEnabled") ?: true,
            isNoticeNotificationsEnabled = fields.getFirestoreBoolean("isNoticeNotificationsEnabled") ?: true,
            isFollowNotificationsEnabled = fields.getFirestoreBoolean("isFollowNotificationsEnabled") ?: true,
            isEventNotificationsEnabled = fields.getFirestoreBoolean("isEventNotificationsEnabled") ?: true,
            isCommunityNotificationsEnabled = fields.getFirestoreBoolean("isCommunityNotificationsEnabled") ?: true,
            quietHoursMode = quietHoursMode
        )
    }

    private fun String?.toStringFieldStartAfterSection(): String {
        if (this.isNullOrBlank()) return ""
        val escaped = escapeFirestoreQueryString(this)
        return """,
                "startAt": {
                  "values": [
                    { "stringValue": "$escaped" }
                  ],
                  "before": false
                }""".trimIndent()
    }

    private fun JsonObject.toStringFieldCursor(fieldPath: String): String? {
        val fields = this["fields"]?.jsonObject ?: return null
        val stringValue = fields[fieldPath]
            ?.jsonObject
            ?.get("stringValue")
            ?.jsonPrimitive
            ?.contentOrNull
        if (!stringValue.isNullOrBlank()) return stringValue
        return this["name"]?.jsonPrimitive?.contentOrNull
    }

    private fun JsonObject.getFirestoreString(key: String): String? {
        return this[key]?.jsonObject?.get("stringValue")?.jsonPrimitive?.contentOrNull
    }

    private fun JsonObject.getFirestoreBoolean(key: String): Boolean? {
        return this[key]?.jsonObject?.get("booleanValue")?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull()
    }

    private fun parseCountAggregationResponse(response: String): Int {
        val parsed = Json.parseToJsonElement(response).jsonArray
        parsed.forEach { element ->
            val aggregateFields = element.jsonObject["result"]
                ?.jsonObject
                ?.get("aggregateFields")
                ?.jsonObject
                ?: return@forEach
            val countValue = aggregateFields["count"]
                ?.jsonObject
                ?.get("integerValue")
                ?.jsonPrimitive
                ?.contentOrNull
            if (countValue != null) {
                return countValue.toIntOrNull() ?: 0
            }
        }
        return 0
    }

    private fun firestoreDocumentBody(fields: Map<String, JsonElement>): String {
        return JsonObject(mapOf("fields" to JsonObject(fields))).toString()
    }

    private fun firestoreString(value: String): JsonObject {
        return JsonObject(mapOf("stringValue" to JsonPrimitive(value)))
    }

    private fun firestoreBoolean(value: Boolean): JsonObject {
        return JsonObject(mapOf("booleanValue" to JsonPrimitive(value)))
    }

    private fun escapeFirestoreQueryString(value: String): String {
        val builder = StringBuilder(value.length)
        value.forEach { ch ->
            when (ch) {
                '\\' -> builder.append("\\\\")
                '"' -> builder.append("\\\"")
                '\n' -> builder.append("\\n")
                '\r' -> builder.append("\\r")
                '\t' -> builder.append("\\t")
                else -> builder.append(ch)
            }
        }
        return builder.toString()
    }

    private fun nextFirestoreEntityId(prefix: String): String {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        return "${prefix}_${timestamp}_${randomSuffix(8)}"
    }

    private fun randomSuffix(length: Int): String {
        val alphabet = "abcdefghijklmnopqrstuvwxyz0123456789"
        val builder = StringBuilder(length)
        repeat(length) {
            val randomIndex = kotlin.random.Random.nextInt(alphabet.length)
            builder.append(alphabet[randomIndex])
        }
        return builder.toString()
    }

    private fun JsonArray?.orEmpty(): JsonArray = this ?: JsonArray(emptyList())
}
