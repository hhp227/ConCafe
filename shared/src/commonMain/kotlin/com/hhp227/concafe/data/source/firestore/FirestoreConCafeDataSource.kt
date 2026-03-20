package com.hhp227.concafe.data.source.firestore

import com.hhp227.concafe.data.source.ConCafeDataSource
import com.hhp227.concafe.data.source.MockConCafeDataSource
import com.hhp227.concafe.domain.model.BannerLinkTargetType
import com.hhp227.concafe.domain.model.HomeBanner
import com.hhp227.concafe.domain.model.User
import com.hhp227.concafe.domain.model.UserRole
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class FirestoreConCafeDataSource(
    private val config: FirestoreConfig,
    private val restApi: FirestoreRestApi,
    private val tokenProvider: FirestoreAuthTokenProvider,
    private val delegate: MockConCafeDataSource = MockConCafeDataSource()
) : ConCafeDataSource by delegate {
    suspend fun bootstrap() {
        val idToken = tokenProvider.getIdToken()

        loadUsers(idToken)
        loadHomeBanners(idToken)
    }

    suspend fun pushUser(user: User) {
        val idToken = tokenProvider.getIdToken()
        val path = "${config.documentBasePath()}/${FirestorePaths.USERS}/${user.id}"
        val body = firestoreDocumentBody(
            mapOf(
                "email" to firestoreString(user.email),
                "nickname" to firestoreString(user.nickname),
                "profileImage" to firestoreNullableString(user.profileImage),
                "role" to firestoreString(user.role.name),
                "banned" to firestoreBoolean(user.banned),
                "createdAt" to firestoreString(user.createdAt)
            )
        )
        restApi.patch(path, body, idToken)
    }

    suspend fun pushHomeBanner(banner: HomeBanner) {
        val idToken = tokenProvider.getIdToken()
        val path = "${config.documentBasePath()}/${FirestorePaths.HOME_BANNERS}/${banner.id}"
        val body = firestoreDocumentBody(
            mapOf(
                "title" to firestoreString(banner.title),
                "subtitle" to firestoreString(banner.subtitle),
                "imageUrl" to firestoreNullableString(banner.imageUrl),
                "relatedCafeId" to firestoreNullableString(banner.cafeId),
                "linkType" to firestoreString(banner.targetType.toFirestoreLinkType()),
                "linkTarget" to firestoreString(banner.targetValue),
                "displayDays" to firestoreLong(banner.displayDays.toLong()),
                "status" to firestoreString(banner.statusLabel),
                "createdAtEpochMillis" to firestoreLong(banner.createdAtEpochMillis),
                "activatedAtEpochMillis" to firestoreLong(banner.activatedAtEpochMillis)
            )
        )
        restApi.patch(path, body, idToken)
    }

    suspend fun deleteHomeBanner(bannerId: String) {
        val idToken = tokenProvider.getIdToken()
        val path = "${config.documentBasePath()}/${FirestorePaths.HOME_BANNERS}/$bannerId"
        restApi.delete(path, idToken)
    }

    private suspend fun loadUsers(idToken: String?) {
        val response = restApi.get("${config.documentBasePath()}/${FirestorePaths.USERS}", idToken)
        val parsed = Json.parseToJsonElement(response).jsonObject
        val documents = parsed["documents"]?.jsonArray.orEmpty()
        val users = documents.mapNotNull { element ->
            parseUserDocument(element.jsonObject)
        }

        if (users.isNotEmpty()) {
            replaceAllUsers(users)
        }
    }

    private suspend fun loadHomeBanners(idToken: String?) {
        val response = restApi.get("${config.documentBasePath()}/${FirestorePaths.HOME_BANNERS}", idToken)
        val parsed = Json.parseToJsonElement(response).jsonObject
        val documents = parsed["documents"]?.jsonArray.orEmpty()
        val banners = documents.mapNotNull { element ->
            parseHomeBannerDocument(element.jsonObject)
        }

        if (banners.isNotEmpty()) {
            this.banners.clear()
            this.banners.addAll(banners)
        }
    }

    private fun parseUserDocument(document: JsonObject): User? {
        val fields = document["fields"]?.jsonObject ?: return null
        val name = document["name"]?.jsonPrimitive?.contentOrNull ?: return null
        val userId = name.substringAfterLast("/")
        val email = fields.getFirestoreString("email") ?: return null
        val nickname = fields.getFirestoreString("nickname") ?: return null
        val role = fields.getFirestoreString("role")?.toUserRoleOrNull() ?: UserRole.VISITOR
        val createdAt = fields.getFirestoreString("createdAt") ?: "1970-01-01T00:00:00Z"
        val profileImage = fields.getFirestoreString("profileImage")
        val banned = fields.getFirestoreBoolean("banned") ?: false
        return User(
            id = userId,
            email = email,
            nickname = nickname,
            profileImage = profileImage,
            role = role,
            banned = banned,
            createdAt = createdAt
        )
    }

    private fun parseHomeBannerDocument(document: JsonObject): HomeBanner? {
        val fields = document["fields"]?.jsonObject ?: return null
        val name = document["name"]?.jsonPrimitive?.contentOrNull ?: return null
        val bannerId = name.substringAfterLast("/")
        val targetType = fields.getFirestoreString("linkType")?.toBannerLinkTargetTypeOrNull()
            ?: BannerLinkTargetType.CAFE_DETAIL
        return HomeBanner(
            id = bannerId,
            title = fields.getFirestoreString("title").orEmpty(),
            subtitle = fields.getFirestoreString("subtitle").orEmpty(),
            startColorHex = fields.getFirestoreString("startColorHex") ?: "FFD1DC",
            endColorHex = fields.getFirestoreString("endColorHex") ?: "F58FB2",
            cafeId = fields.getFirestoreString("relatedCafeId"),
            imageUrl = fields.getFirestoreString("imageUrl"),
            targetType = targetType,
            targetValue = fields.getFirestoreString("linkTarget").orEmpty(),
            displayDays = fields.getFirestoreLong("displayDays")?.toInt() ?: 1,
            statusLabel = fields.getFirestoreString("status") ?: "DRAFT",
            createdAtEpochMillis = fields.getFirestoreLong("createdAtEpochMillis") ?: 0L,
            activatedAtEpochMillis = fields.getFirestoreLong("activatedAtEpochMillis") ?: 0L
        )
    }
}

private fun JsonObject.getFirestoreString(key: String): String? {
    val valueObject = this[key]?.jsonObject ?: return null
    return valueObject["stringValue"]?.jsonPrimitive?.contentOrNull
}

private fun JsonObject.getFirestoreBoolean(key: String): Boolean? {
    val valueObject = this[key]?.jsonObject ?: return null
    return valueObject["booleanValue"]?.jsonPrimitive?.booleanOrNull
}

private fun JsonObject.getFirestoreLong(key: String): Long? {
    val valueObject = this[key]?.jsonObject ?: return null
    val fromInteger = valueObject["integerValue"]?.jsonPrimitive?.longOrNull
    val fromDouble = valueObject["doubleValue"]?.jsonPrimitive?.doubleOrNull?.toLong()
    return fromInteger ?: fromDouble
}

private fun firestoreDocumentBody(fields: Map<String, JsonElement>): String {
    val fieldsJson = fields.entries.joinToString(",") { (key, value) ->
        "\"$key\":$value"
    }
    return "{\"fields\":{$fieldsJson}}"
}

private fun firestoreString(value: String): JsonObject {
    return JsonObject(mapOf("stringValue" to JsonPrimitive(value)))
}

private fun firestoreNullableString(value: String?): JsonObject {
    return if (value == null) {
        JsonObject(mapOf("nullValue" to JsonNull))
    } else {
        firestoreString(value)
    }
}

private fun firestoreBoolean(value: Boolean): JsonObject {
    return JsonObject(mapOf("booleanValue" to JsonPrimitive(value)))
}

private fun firestoreLong(value: Long): JsonObject {
    return JsonObject(mapOf("integerValue" to JsonPrimitive(value.toString())))
}

private fun String.toUserRoleOrNull(): UserRole? {
    return when (this) {
        "ADMIN" -> UserRole.ADMIN
        "CAFE_OWNER" -> UserRole.CAFE_OWNER
        "CAST" -> UserRole.CAST
        "VISITOR" -> UserRole.VISITOR
        else -> null
    }
}

private fun String.toBannerLinkTargetTypeOrNull(): BannerLinkTargetType? {
    return when (this) {
        "CAFE", "CAFE_DETAIL" -> BannerLinkTargetType.CAFE_DETAIL
        "EVENT", "EVENT_DETAIL" -> BannerLinkTargetType.EVENT_DETAIL
        "NOTICE" -> BannerLinkTargetType.NOTICE
        "EXTERNAL", "EXTERNAL_LINK" -> BannerLinkTargetType.EXTERNAL_LINK
        else -> null
    }
}

private fun BannerLinkTargetType.toFirestoreLinkType(): String {
    return when (this) {
        BannerLinkTargetType.CAFE_DETAIL -> "CAFE"
        BannerLinkTargetType.EVENT_DETAIL -> "EVENT"
        BannerLinkTargetType.NOTICE -> "NOTICE"
        BannerLinkTargetType.EXTERNAL_LINK -> "EXTERNAL"
    }
}
