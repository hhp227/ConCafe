package com.hhp227.concafe.data.source.firestore

import com.hhp227.concafe.domain.model.*
import kotlinx.datetime.*
import kotlinx.serialization.json.*

abstract class FirestoreBaseDataSource(
    protected val config: FirestoreConfig,
    protected val restApi: FirestoreRestApi,
    protected val tokenProvider: FirestoreAuthTokenProvider
) {

    // ── Document body builders ────────────────────────────────────────────────

    protected fun firestoreDocumentBody(fields: Map<String, JsonElement>): String {
        val fieldsJson = fields.entries.joinToString(",") { (key, value) ->
            "\"$key\":$value"
        }
        return "{\"fields\":{$fieldsJson}}"
    }

    protected fun firestoreString(value: String): JsonObject =
        JsonObject(mapOf("stringValue" to JsonPrimitive(value)))

    protected fun firestoreNullableString(value: String?): JsonObject =
        if (value == null) JsonObject(mapOf("nullValue" to JsonNull))
        else firestoreString(value)

    protected fun firestoreBoolean(value: Boolean): JsonObject =
        JsonObject(mapOf("booleanValue" to JsonPrimitive(value)))

    protected fun firestoreLong(value: Long): JsonObject =
        JsonObject(mapOf("integerValue" to JsonPrimitive(value.toString())))

    protected fun firestoreDouble(value: Double): JsonObject =
        JsonObject(mapOf("doubleValue" to JsonPrimitive(value)))

    protected fun firestoreStringArray(values: List<String>): JsonObject {
        val firestoreValues = values.map { value -> firestoreString(value) }
        return JsonObject(
            mapOf(
                "arrayValue" to JsonObject(
                    mapOf("values" to JsonArray(firestoreValues))
                )
            )
        )
    }

    protected fun firestoreMap(fields: Map<String, JsonElement>): JsonObject =
        JsonObject(
            mapOf(
                "mapValue" to JsonObject(
                    mapOf("fields" to JsonObject(fields))
                )
            )
        )

    protected fun firestoreGeoPoint(latitude: Double, longitude: Double): JsonObject =
        JsonObject(
            mapOf(
                "geoPointValue" to JsonObject(
                    mapOf(
                        "latitude" to JsonPrimitive(latitude),
                        "longitude" to JsonPrimitive(longitude)
                    )
                )
            )
        )

    // ── JsonObject field extractors ───────────────────────────────────────────

    protected fun JsonObject.getFirestoreString(key: String): String? {
        val valueObject = this[key]?.jsonObject ?: return null
        return valueObject["stringValue"]?.jsonPrimitive?.contentOrNull
    }

    protected fun JsonObject.getFirestoreBoolean(key: String): Boolean? {
        val valueObject = this[key]?.jsonObject ?: return null
        return valueObject["booleanValue"]?.jsonPrimitive?.booleanOrNull
    }

    protected fun JsonObject.getFirestoreLong(key: String): Long? {
        val valueObject = this[key]?.jsonObject ?: return null
        val fromInteger = valueObject["integerValue"]?.jsonPrimitive?.longOrNull
        val fromDouble = valueObject["doubleValue"]?.jsonPrimitive?.doubleOrNull?.toLong()
        return fromInteger ?: fromDouble
    }

    protected fun JsonObject.getFirestoreDouble(key: String): Double? {
        val valueObject = this[key]?.jsonObject ?: return null
        val fromDouble = valueObject["doubleValue"]?.jsonPrimitive?.doubleOrNull
        val fromInteger = valueObject["integerValue"]?.jsonPrimitive?.longOrNull?.toDouble()
        return fromDouble ?: fromInteger
    }

    protected fun JsonObject.getFirestoreInt(key: String): Int? =
        getFirestoreLong(key)?.toInt()

    protected fun JsonObject.getFirestoreMap(key: String): JsonObject? =
        this[key]?.jsonObject?.get("mapValue")?.jsonObject?.get("fields")?.jsonObject

    protected fun JsonObject.getFirestoreStringList(key: String): List<String> {
        val valueObject = this[key]?.jsonObject ?: return emptyList()
        val arrayValue = valueObject["arrayValue"]?.jsonObject ?: return emptyList()
        val values = arrayValue["values"]?.jsonArray.orEmpty()
        return values.mapNotNull { element ->
            element.jsonObject["stringValue"]?.jsonPrimitive?.contentOrNull
        }
    }

    // ── Escape / ID helpers ───────────────────────────────────────────────────

    protected fun escapeFirestoreQueryString(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"")

    protected fun nextFirestoreEntityId(prefix: String): String {
        val now = Clock.System.now().toEpochMilliseconds()
        return "$prefix-$now"
    }

    protected fun sanitizeDocumentIdPart(value: String): String =
        value.replace("/", "_")

    protected fun Throwable.isFirestoreNotFound(): Boolean =
        message?.contains("request failed(404)") == true

    // ── Cursor helpers ────────────────────────────────────────────────────────

    protected fun JsonObject.toCastQueryCursor(sort: CastSort): String? {
        val fields = this["fields"]?.jsonObject ?: return null
        return when (sort) {
            CastSort.LATEST -> this["name"]?.jsonPrimitive?.contentOrNull
            CastSort.HOME_LINKED_FIRST -> {
                val linkedUserId = fields.getFirestoreString("linkedUserId") ?: CAST_CURSOR_NULL_MARKER
                val followerScore = fields.getFirestoreLong("followerCount")?.toString()
                    ?: fields.getFirestoreDouble("followerCount")?.toString() ?: "0"
                val docName = this["name"]?.jsonPrimitive?.contentOrNull ?: return null
                "$linkedUserId|$followerScore|$docName"
            }
            else -> {
                val followerScore = fields.getFirestoreLong("followerCount")?.toString()
                    ?: fields.getFirestoreDouble("followerCount")?.toString() ?: "0"
                val docName = this["name"]?.jsonPrimitive?.contentOrNull ?: return null
                "$followerScore|$docName"
            }
        }
    }

    protected fun JsonObject.toCafeQueryCursor(sort: CafeSort): String? {
        val fields = this["fields"]?.jsonObject ?: return null
        return when (sort) {
            CafeSort.LATEST -> this["name"]?.jsonPrimitive?.contentOrNull
            CafeSort.POPULAR -> {
                val score = fields.getFirestoreLong("reviewCount")?.toString()
                    ?: fields.getFirestoreDouble("reviewCount")?.toString() ?: "0"
                val docName = this["name"]?.jsonPrimitive?.contentOrNull ?: return null
                "$score|$docName"
            }
            else -> {
                val score = fields.getFirestoreDouble("ratingAvg")?.toString()
                    ?: fields.getFirestoreLong("ratingAvg")?.toString() ?: "0"
                val docName = this["name"]?.jsonPrimitive?.contentOrNull ?: return null
                "$score|$docName"
            }
        }
    }

    protected fun JsonObject.toCafeCastQueryCursor(): String? {
        val fields = this["fields"]?.jsonObject ?: return null
        val castName = fields.getFirestoreString("name") ?: return null
        val documentName = this["name"]?.jsonPrimitive?.contentOrNull ?: return null
        return "$castName|$documentName"
    }

    protected fun JsonObject.toReviewQueryCursor(): String? {
        val fields = this["fields"]?.jsonObject ?: return null
        return fields.getFirestoreString("createdAt")
    }

    protected fun JsonObject.toVisitQueryCursor(): String? {
        val documentName = this["name"]?.jsonPrimitive?.contentOrNull ?: return null
        val fields = this["fields"]?.jsonObject ?: return null
        val visitedAt = fields.getFirestoreString("visitedAt") ?: return null
        return "$visitedAt|$documentName"
    }

    protected fun JsonObject.toNoticeQueryCursor(): String? {
        val documentName = this["name"]?.jsonPrimitive?.contentOrNull ?: return null
        val fields = this["fields"]?.jsonObject ?: return null
        val createdAt = fields.getFirestoreString("createdAt") ?: return null
        return "$createdAt|$documentName"
    }

    protected fun JsonObject.toEventQueryCursor(): String? {
        val fields = this["fields"]?.jsonObject ?: return null
        return fields.getFirestoreString("startDate")
    }

    protected fun JsonObject.toHomeEventQueryCursor(): String? {
        val fields = this["fields"]?.jsonObject ?: return null
        return fields.getFirestoreString("endDate")
    }

    protected fun String?.toStartAfterSection(latestSort: Boolean): String {
        val cursorValue = this
        if (cursorValue.isNullOrBlank()) return ""
        return if (latestSort) {
            val escapedDocumentName = escapeFirestoreQueryString(cursorValue)
            """,
                "startAt": {
                  "values": [
                    { "referenceValue": "$escapedDocumentName" }
                  ],
                  "before": false
                }"""
        } else {
            val separatorIndex = cursorValue.indexOf('|')
            val scoreRaw = if (separatorIndex >= 0) cursorValue.substring(0, separatorIndex) else cursorValue
            val docName = if (separatorIndex >= 0) cursorValue.substring(separatorIndex + 1) else null
            val scoreValue = scoreRaw.toDoubleOrNull() ?: return ""
            val numericValue = if (scoreRaw.contains(".")) {
                """{ "doubleValue": $scoreValue }"""
            } else {
                """{ "integerValue": "${scoreValue.toLong()}" }"""
            }
            if (!docName.isNullOrBlank()) {
                val escapedDocName = escapeFirestoreQueryString(docName)
                """,
                "startAt": {
                  "values": [
                    $numericValue,
                    { "referenceValue": "$escapedDocName" }
                  ],
                  "before": false
                }"""
            } else {
                """,
                "startAt": {
                  "values": [
                    $numericValue
                  ],
                  "before": false
                }"""
            }
        }
    }

    protected fun String?.toCastStartAfterSection(sort: CastSort): String {
        val cursorValue = this
        if (cursorValue.isNullOrBlank()) return ""
        return when (sort) {
            CastSort.LATEST -> {
                val escapedDocumentName = escapeFirestoreQueryString(cursorValue)
                """,
                "startAt": {
                  "values": [
                    { "referenceValue": "$escapedDocumentName" }
                  ],
                  "before": false
                }"""
            }
            CastSort.HOME_LINKED_FIRST -> {
                val firstSep = cursorValue.indexOf('|')
                val secondSep = cursorValue.indexOf('|', startIndex = firstSep + 1)
                val linkedUserRaw = if (firstSep >= 0) cursorValue.substring(0, firstSep) else cursorValue
                val scoreRaw = if (firstSep >= 0 && secondSep > firstSep) cursorValue.substring(firstSep + 1, secondSep) else ""
                val docName = if (secondSep >= 0) cursorValue.substring(secondSep + 1) else null
                val scoreValue = scoreRaw.toDoubleOrNull()
                if (scoreValue == null || docName.isNullOrBlank()) return ""
                val linkedUserValue = if (linkedUserRaw == CAST_CURSOR_NULL_MARKER) {
                    """{ "nullValue": null }"""
                } else {
                    """{ "stringValue": "${escapeFirestoreQueryString(linkedUserRaw)}" }"""
                }
                val numericValue = if (scoreRaw.contains(".")) {
                    """{ "doubleValue": $scoreValue }"""
                } else {
                    """{ "integerValue": "${scoreValue.toLong()}" }"""
                }
                val escapedDocName = escapeFirestoreQueryString(docName)
                """,
                "startAt": {
                  "values": [
                    $linkedUserValue,
                    $numericValue,
                    { "referenceValue": "$escapedDocName" }
                  ],
                  "before": false
                }"""
            }
            else -> {
                val separatorIndex = cursorValue.indexOf('|')
                val scoreRaw = if (separatorIndex >= 0) cursorValue.substring(0, separatorIndex) else cursorValue
                val docName = if (separatorIndex >= 0) cursorValue.substring(separatorIndex + 1) else null
                val scoreValue = scoreRaw.toDoubleOrNull() ?: return ""
                val numericValue = if (scoreRaw.contains(".")) {
                    """{ "doubleValue": $scoreValue }"""
                } else {
                    """{ "integerValue": "${scoreValue.toLong()}" }"""
                }
                if (!docName.isNullOrBlank()) {
                    val escapedDocName = escapeFirestoreQueryString(docName)
                    """,
                "startAt": {
                  "values": [
                    $numericValue,
                    { "referenceValue": "$escapedDocName" }
                  ],
                  "before": false
                }"""
                } else {
                    """,
                "startAt": {
                  "values": [
                    $numericValue
                  ],
                  "before": false
                }"""
                }
            }
        }
    }

    protected fun String?.toCafeCastStartAfterSection(): String {
        val cursorValue = this
        if (cursorValue.isNullOrBlank()) return ""
        val castName = cursorValue.substringBefore("|")
        val documentName = cursorValue.substringAfter("|", missingDelimiterValue = "")
        if (castName.isBlank() || documentName.isBlank()) return ""
        val escapedCastName = escapeFirestoreQueryString(castName)
        val escapedDocumentName = escapeFirestoreQueryString(documentName)
        return """,
                "startAt": {
                  "values": [
                    { "stringValue": "$escapedCastName" },
                    { "referenceValue": "$escapedDocumentName" }
                  ],
                  "before": false
                }"""
    }

    protected fun String?.toReviewStartAfterSection(): String {
        val cursorValue = this
        if (cursorValue.isNullOrBlank()) return ""
        val createdAt = cursorValue.substringBefore("|")
        val escapedCreatedAt = escapeFirestoreQueryString(createdAt)
        return """,
                "startAt": {
                  "values": [
                    { "stringValue": "$escapedCreatedAt" }
                  ],
                  "before": false
                }"""
    }

    protected fun String?.toVisitStartAfterSection(): String {
        val cursorValue = this
        if (cursorValue.isNullOrBlank()) return ""
        val visitedAt = cursorValue.substringBefore("|")
        val documentName = cursorValue.substringAfter("|", missingDelimiterValue = "")
        if (visitedAt.isBlank() || documentName.isBlank()) return ""
        val escapedVisitedAt = escapeFirestoreQueryString(visitedAt)
        val escapedDocumentName = escapeFirestoreQueryString(documentName)
        return """,
                "startAt": {
                  "values": [
                    { "stringValue": "$escapedVisitedAt" },
                    { "referenceValue": "$escapedDocumentName" }
                  ],
                  "before": false
                }"""
    }

    protected fun String?.toStringFieldStartAfterSection(): String {
        val cursorValue = this
        if (cursorValue.isNullOrBlank()) return ""
        val fieldValue = cursorValue.substringBefore("|")
        val escapedFieldValue = escapeFirestoreQueryString(fieldValue)
        return """,
                "startAt": {
                  "values": [
                    { "stringValue": "$escapedFieldValue" }
                  ],
                  "before": false
                }"""
    }

    protected fun String?.toNoticeStartAfterSection(): String {
        val cursorValue = this
        if (cursorValue.isNullOrBlank()) return ""
        val createdAt = cursorValue.substringBefore("|")
        val documentName = cursorValue.substringAfter("|", missingDelimiterValue = "")
        if (createdAt.isBlank() || documentName.isBlank()) return ""
        val escapedCreatedAt = escapeFirestoreQueryString(createdAt)
        val escapedDocumentName = escapeFirestoreQueryString(documentName)
        return """,
                "startAt": {
                  "values": [
                    { "stringValue": "$escapedCreatedAt" },
                    { "referenceValue": "$escapedDocumentName" }
                  ],
                  "before": false
                }"""
    }

    // ── String / birthday utilities ───────────────────────────────────────────

    protected fun String.toUserRoleOrNull(): UserRole? {
        val normalized = trim().uppercase().replace("-", "_").replace(" ", "_")
        return when (normalized) {
            "ADMIN", "ROLE_ADMIN" -> UserRole.ADMIN
            "CAFE_OWNER", "CAFEOWNER", "OWNER", "ROLE_CAFE_OWNER" -> UserRole.CAFE_OWNER
            "CAST", "ROLE_CAST" -> UserRole.CAST
            "VISITOR", "GUEST", "ROLE_VISITOR" -> UserRole.VISITOR
            else -> null
        }
    }

    protected fun String.toBannerLinkTargetTypeOrNull(): BannerLinkTargetType? {
        return when (this) {
            "CAFE", "CAFE_DETAIL" -> BannerLinkTargetType.CAFE_DETAIL
            "EVENT", "EVENT_DETAIL" -> BannerLinkTargetType.EVENT_DETAIL
            "NOTICE" -> BannerLinkTargetType.NOTICE
            "EXTERNAL", "EXTERNAL_LINK" -> BannerLinkTargetType.EXTERNAL_LINK
            else -> null
        }
    }

    protected fun String.isApprovedClaimStatus(): Boolean {
        val normalized = trim().uppercase().replace("-", "_").replace(" ", "_")
        return normalized == "APPROVED" || normalized == "승인" || normalized == "승인완료" || normalized == "승인_완료"
    }

    protected fun monthDayToBirthdayKey(month: Int, dayOfMonth: Int): String {
        val normalizedMonth = month.coerceIn(1, 12).toString().padStart(2, '0')
        val normalizedDay = dayOfMonth.coerceIn(1, 31).toString().padStart(2, '0')
        return "$normalizedMonth-$normalizedDay"
    }

    protected fun String?.toBirthdayKeyOrNull(): String? {
        val monthDay = this?.toBirthMonthDayOrNull() ?: return null
        return monthDayToBirthdayKey(month = monthDay.first, dayOfMonth = monthDay.second)
    }

    protected fun String?.matchesMonthAndDay(month: Int, dayOfMonth: Int): Boolean {
        val monthDay = this?.toBirthMonthDayOrNull() ?: return false
        return monthDay.first == month && monthDay.second == dayOfMonth
    }

    private fun String.toBirthMonthDayOrNull(): Pair<Int, Int>? {
        val normalized = trim()
        val isoParts = normalized.split("-")
        if (isoParts.size == 3) {
            val month = isoParts[1].toIntOrNull()
            val day = isoParts[2].toIntOrNull()
            if (month != null && day != null) return month to day
        }
        val slashParts = normalized.split("/")
        if (slashParts.size == 3) {
            val month = slashParts[0].toIntOrNull()
            val day = slashParts[1].toIntOrNull()
            if (month != null && day != null) return month to day
        }
        return null
    }

    protected fun String.normalizeBirthdayToIsoOrNull(): String? {
        val normalized = trim()
        val isoParts = normalized.split("-")
        if (isoParts.size == 3) {
            val year = isoParts[0].toIntOrNull()
            val month = isoParts[1].toIntOrNull()
            val day = isoParts[2].toIntOrNull()
            if (year != null && month != null && day != null) {
                return "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
            }
        }
        val slashParts = normalized.split("/")
        if (slashParts.size == 3) {
            val month = slashParts[0].toIntOrNull()
            val day = slashParts[1].toIntOrNull()
            val year = slashParts[2].toIntOrNull()
            if (year != null && month != null && day != null) {
                return "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
            }
        }
        return null
    }

    protected fun String.toCafeIdFromCastDocumentName(): String? {
        val segments = split("/")
        val castsIndex = segments.indexOf(FirestorePaths.CAFE_CASTS)
        val cafeSegmentIndex = castsIndex - 1
        val cafeCollectionIndex = castsIndex - 2
        if (castsIndex <= 1 || cafeCollectionIndex < 0 || cafeSegmentIndex < 0) return null
        return if (segments[cafeCollectionIndex] != FirestorePaths.CAFES) null
        else segments[cafeSegmentIndex].takeIf { v -> v.isNotBlank() }
    }

    protected fun String.toCafeIdFromNoticeDocumentName(): String? {
        val segments = split("/")
        val noticesIndex = segments.indexOf(FirestorePaths.CAFE_NOTICES)
        val cafeSegmentIndex = noticesIndex - 1
        val cafeCollectionIndex = noticesIndex - 2
        if (noticesIndex <= 1 || cafeCollectionIndex < 0 || cafeSegmentIndex < 0) return null
        return if (segments[cafeCollectionIndex] != FirestorePaths.CAFES) null
        else segments[cafeSegmentIndex].takeIf { v -> v.isNotBlank() }
    }

    protected fun String.toCafeIdFromEventDocumentName(): String? {
        val segments = split("/")
        val eventsIndex = segments.indexOf(FirestorePaths.CAFE_EVENTS)
        val cafeSegmentIndex = eventsIndex - 1
        val cafeCollectionIndex = eventsIndex - 2
        if (eventsIndex <= 1 || cafeCollectionIndex < 0 || cafeSegmentIndex < 0) return null
        return if (segments[cafeCollectionIndex] != FirestorePaths.CAFES) null
        else segments[cafeSegmentIndex].takeIf { v -> v.isNotBlank() }
    }

    protected fun extractCafeIdFromCastDocumentName(name: String): String? {
        val regex = Regex(""".*/cafes/([^/]+)/casts/[^/]+$""")
        val match = regex.matchEntire(name) ?: return null
        return match.groupValues.getOrNull(1)
    }

    protected fun normalizeScheduleDateOrNull(raw: String?): String? {
        val candidate = raw?.trim().orEmpty()
        val regex = Regex("""(\d{4})[-./](\d{2})[-./](\d{2})""")
        val match = regex.find(candidate) ?: return null
        val year = match.groupValues.getOrNull(1) ?: return null
        val month = match.groupValues.getOrNull(2) ?: return null
        val day = match.groupValues.getOrNull(3) ?: return null
        return "$year-$month-$day"
    }

    protected fun parseCastScheduleStatus(raw: String?): CastScheduleStatus? {
        return when (raw?.trim()?.uppercase()) {
            "WORK", "근무" -> CastScheduleStatus.WORK
            "OFF", "휴무" -> CastScheduleStatus.OFF
            "VACATION", "휴가" -> CastScheduleStatus.VACATION
            else -> null
        }
    }

    protected fun haversineMeters(
        latitude1: Double, longitude1: Double,
        latitude2: Double, longitude2: Double
    ): Double {
        val earthRadiusMeters = 6_371_000.0
        val latitudeDelta = (latitude2 - latitude1) * (kotlin.math.PI / 180.0)
        val longitudeDelta = (longitude2 - longitude1) * (kotlin.math.PI / 180.0)
        val startLatitudeRadians = latitude1 * (kotlin.math.PI / 180.0)
        val endLatitudeRadians = latitude2 * (kotlin.math.PI / 180.0)
        val sinLatitude = kotlin.math.sin(latitudeDelta / 2.0)
        val sinLongitude = kotlin.math.sin(longitudeDelta / 2.0)
        val a = sinLatitude * sinLatitude +
            kotlin.math.cos(startLatitudeRadians) * kotlin.math.cos(endLatitudeRadians) * sinLongitude * sinLongitude
        val c = 2.0 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1.0 - a))
        return earthRadiusMeters * c
    }

    // ── Period label ──────────────────────────────────────────────────────────

    protected fun resolvePeriodLabel(displayDays: Int): String {
        val startDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val endDate = startDate.plus(DatePeriod(days = displayDays - 1))
        return "${startDate.toPeriodText()} - ${endDate.toPeriodText()}"
    }

    private fun LocalDate.toPeriodText(): String {
        val monthText = monthNumber.toString().padStart(2, '0')
        val dayText = dayOfMonth.toString().padStart(2, '0')
        return "$year.$monthText.$dayText"
    }

    // ── Business-hours formatter ──────────────────────────────────────────────

    protected fun formatBusinessHours(update: CafeInfoUpdate): String {
        val weekdayOpen = update.weekdayOpen.trim()
        val weekdayClose = update.weekdayClose.trim()
        val weekendOpen = update.weekendOpen.trim()
        val weekendClose = update.weekendClose.trim()
        val weekday = weekdayOpen.isNotEmpty() && weekdayClose.isNotEmpty()
        val weekend = weekendOpen.isNotEmpty() && weekendClose.isNotEmpty()
        return when {
            weekday && weekend && weekdayOpen == weekendOpen && weekdayClose == weekendClose ->
                "매일 $weekdayOpen - $weekdayClose"
            weekday && weekend ->
                "평일 $weekdayOpen - $weekdayClose / 주말 $weekendOpen - $weekendClose"
            weekday -> "평일 $weekdayOpen - $weekdayClose"
            weekend -> "주말 $weekendOpen - $weekendClose"
            else -> ""
        }
    }

    // ── Auth helper ───────────────────────────────────────────────────────────

    protected fun ensureAuthenticatedUserMatch(requestedUserId: String, action: String) {
        val tokenUserId = tokenProvider.getCurrentUserId()
        if (!tokenUserId.isNullOrBlank() && tokenUserId != requestedUserId) {
            throw IllegalStateException(
                "Auth user mismatch in $action: tokenUserId=$tokenUserId requestedUserId=$requestedUserId"
            )
        }
    }

    // ── Document loaders ──────────────────────────────────────────────────────

    protected suspend fun loadUserDocument(userId: String, idToken: String?): JsonObject {
        val path = "${config.documentBasePath()}/${FirestorePaths.USERS}/$userId"
        val response = restApi.get(path, idToken)
        return Json.parseToJsonElement(response).jsonObject
    }

    protected suspend fun loadCafeDocument(cafeId: String, idToken: String?): JsonObject {
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFES}/$cafeId"
        val response = restApi.get(path, idToken)
        return Json.parseToJsonElement(response).jsonObject
    }

    protected suspend fun loadCafeCastDocument(cafeId: String, castId: String, idToken: String?): JsonObject {
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFES}/$cafeId/${FirestorePaths.CAFE_CASTS}/$castId"
        val response = restApi.get(path, idToken)
        return Json.parseToJsonElement(response).jsonObject
    }

    protected suspend fun loadCafeSubCollectionDocuments(
        cafeId: String,
        collectionId: String,
        idToken: String?
    ): List<JsonObject> {
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFES}/$cafeId/$collectionId"
        val response = restApi.get(path, idToken)
        val parsed = Json.parseToJsonElement(response).jsonObject
        return parsed["documents"]?.jsonArray.orEmpty().map { element -> element.jsonObject }
    }

    protected suspend fun loadHomeBanners(idToken: String?): List<HomeBanner> {
        val response = restApi.get("${config.documentBasePath()}/${FirestorePaths.HOME_BANNERS}", idToken)
        val parsed = Json.parseToJsonElement(response).jsonObject
        val documents = parsed["documents"]?.jsonArray.orEmpty()
        val banners = documents.mapNotNull { element -> parseHomeBannerDocument(element.jsonObject) }
        return banners.sortedByDescending { banner -> banner.createdAtEpochMillis }
    }

    protected suspend fun loadCastDirectoryCafeId(castId: String, idToken: String?): String? {
        val path = "${config.documentBasePath()}/${FirestorePaths.CAST_DIRECTORY}/$castId"
        val response = runCatching { restApi.get(path, idToken) }
            .recoverCatching { restApi.get(path, null) }
            .getOrNull() ?: return null
        val document = runCatching { Json.parseToJsonElement(response).jsonObject }.getOrNull() ?: return null
        return document["fields"]?.jsonObject?.getFirestoreString("cafeId")?.takeIf { it.isNotBlank() }
    }

    // ── Resolve helpers ───────────────────────────────────────────────────────

    protected suspend fun resolveVisitById(visitId: String, idToken: String?): Visit? {
        val path = "${config.documentBasePath()}/${FirestorePaths.VISITS}/$visitId"
        val document = runCatching { Json.parseToJsonElement(restApi.get(path, idToken)).jsonObject }
            .recoverCatching { Json.parseToJsonElement(restApi.get(path, null)).jsonObject }
            .getOrNull()
        return document?.let { parseVisitDocument(it) }
    }

    protected suspend fun resolveReviewById(reviewId: String, idToken: String?): Review? {
        val path = "${config.documentBasePath()}/${FirestorePaths.REVIEWS}/$reviewId"
        val document = runCatching { Json.parseToJsonElement(restApi.get(path, idToken)).jsonObject }
            .recoverCatching { Json.parseToJsonElement(restApi.get(path, null)).jsonObject }
            .getOrNull()
        return document?.let { parseReviewDocument(it) }
    }

    protected suspend fun resolveCastClaimById(claimId: String, idToken: String?): CastClaim? {
        val path = "${config.documentBasePath()}/${FirestorePaths.CAST_CLAIMS}/$claimId"
        val document = try {
            Json.parseToJsonElement(restApi.get(path, idToken)).jsonObject
        } catch (throwable: Throwable) {
            if (throwable.isFirestoreNotFound()) return null else throw throwable
        }
        return parseCastClaimDocument(document)
    }

    protected suspend fun resolveCafeCastById(cafeId: String, castId: String, idToken: String?): Cast? {
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFES}/$cafeId/${FirestorePaths.CAFE_CASTS}/$castId"
        val document = runCatching { Json.parseToJsonElement(restApi.get(path, idToken)).jsonObject }
            .recoverCatching { Json.parseToJsonElement(restApi.get(path, null)).jsonObject }
            .getOrNull() ?: return null
        return parseCastDocument(cafeId = cafeId, document = document)
    }

    protected suspend fun resolveCafeIdByCastId(castId: String, idToken: String?): String? {
        val indexedCafeId = loadCastDirectoryCafeId(castId = castId, idToken = idToken)
        if (!indexedCafeId.isNullOrBlank()) return indexedCafeId

        val tokenUserId = tokenProvider.getCurrentUserId()
        val selfCastDocuments = if (tokenUserId.isNullOrBlank()) emptyList()
        else resolveCastDocumentsByUser(userId = tokenUserId, idToken = idToken)
        val selfMatchedCafeId = selfCastDocuments.firstNotNullOfOrNull { document ->
            val name = document["name"]?.jsonPrimitive?.contentOrNull
            if (name.isNullOrBlank()) null
            else if (name.endsWith("/$castId")) extractCafeIdFromCastDocumentName(name) else null
        }
        if (selfMatchedCafeId != null) return selfMatchedCafeId

        val affiliatedCafeId = if (tokenUserId.isNullOrBlank()) null
        else runCatching { fetchAffiliatedCafeIdRemote(tokenUserId) }.getOrNull()
        val affiliatedMatchedCast = if (affiliatedCafeId.isNullOrBlank()) null
        else resolveCafeCastById(cafeId = affiliatedCafeId, castId = castId, idToken = idToken)
        if (affiliatedMatchedCast != null && !affiliatedCafeId.isNullOrBlank()) return affiliatedCafeId
        return null
    }

    protected suspend fun resolveCastDocumentsByUser(userId: String, idToken: String?): List<JsonObject> {
        val byLinkedUserId = runCatching { runCastByLinkedUserIdQuery(userId = userId, idToken = idToken) }
            .recoverCatching { runCastByLinkedUserIdQuery(userId = userId, idToken = null) }
            .getOrElse { emptyList() }
        if (byLinkedUserId.isNotEmpty()) return byLinkedUserId

        val byUserIdField = runCatching { runCastByUserFieldQuery(userId = userId, idToken = idToken, fieldPath = "userId") }
            .recoverCatching { runCastByUserFieldQuery(userId = userId, idToken = null, fieldPath = "userId") }
            .getOrElse { emptyList() }
        if (byUserIdField.isNotEmpty()) return byUserIdField

        val byUidField = runCatching { runCastByUserFieldQuery(userId = userId, idToken = idToken, fieldPath = "uid") }
            .recoverCatching { runCastByUserFieldQuery(userId = userId, idToken = null, fieldPath = "uid") }
            .getOrElse { emptyList() }
        if (byUidField.isNotEmpty()) return byUidField

        val affiliatedCafeId = runCatching { fetchAffiliatedCafeIdRemote(userId) }.getOrNull()
        if (affiliatedCafeId.isNullOrBlank()) return emptyList()

        val cafeCastDocuments = runCatching { runCafeCastQuery(cafeId = affiliatedCafeId, cursor = null, limit = 200, idToken = idToken) }
            .recoverCatching { runCafeCastQuery(cafeId = affiliatedCafeId, cursor = null, limit = 200, idToken = null) }
            .getOrElse { emptyList() }
        return cafeCastDocuments.filter { document ->
            val fields = document["fields"]?.jsonObject ?: return@filter false
            val linked = fields.getFirestoreString("linkedUserId")
            val legacyUserId = fields.getFirestoreString("userId")
            val legacyUid = fields.getFirestoreString("uid")
            linked == userId || legacyUserId == userId || legacyUid == userId
        }
    }

    protected suspend fun fetchAffiliatedCafeIdRemote(userId: String): String? {
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        val userDocument = runCatching { loadUserDocument(userId = userId, idToken = idToken) }
            .recoverCatching { loadUserDocument(userId = userId, idToken = null) }
            .getOrNull()
        return if (userDocument == null) null else parseUserAffiliatedCafeId(userDocument)
    }

    protected suspend fun loadUserIdListFromProfileDocument(
        userId: String,
        idToken: String?,
        candidates: List<String>
    ): List<String> {
        val userDocument = runCatching { loadUserDocument(userId = userId, idToken = idToken) }
            .recoverCatching { loadUserDocument(userId = userId, idToken = null) }
            .getOrNull() ?: return emptyList()
        val fields = userDocument["fields"]?.jsonObject ?: return emptyList()
        val stats = fields.getFirestoreMap("stats")
        candidates.forEach { fieldName ->
            val fromRoot = fields.getFirestoreStringList(fieldName).map { v -> v.trim() }.filter { v -> v.isNotEmpty() }
            if (fromRoot.isNotEmpty()) return fromRoot
            val fromStats = stats?.getFirestoreStringList(fieldName).orEmpty().map { v -> v.trim() }.filter { v -> v.isNotEmpty() }
            if (fromStats.isNotEmpty()) return fromStats
        }
        return emptyList()
    }

    // ── Count aggregation ─────────────────────────────────────────────────────

    protected suspend fun loadCollectionDocumentCount(
        collectionId: String,
        idToken: String?,
        equalsFilterFieldPath: String? = null,
        equalsFilterValue: JsonObject? = null
    ): Int {
        val requestBody = buildCountAggregationQueryBody(
            collectionId = collectionId,
            equalsFilterFieldPath = equalsFilterFieldPath,
            equalsFilterValue = equalsFilterValue
        )
        val response = restApi.post(
            path = "${config.documentBasePath()}:runAggregationQuery",
            body = requestBody,
            idToken = idToken
        )
        return parseCountAggregationResponse(response)
    }

    protected suspend fun loadCollectionDocumentCountByDatePrefix(
        collectionId: String,
        idToken: String?,
        equalsFilterFieldPath: String,
        equalsFilterValue: String,
        dateFieldPath: String,
        datePrefix: String
    ): Int {
        val requestBody = buildCountAggregationQueryBodyWithDatePrefix(
            collectionId = collectionId,
            equalsFilterFieldPath = equalsFilterFieldPath,
            equalsFilterValue = equalsFilterValue,
            dateFieldPath = dateFieldPath,
            datePrefix = datePrefix
        )
        val response = restApi.post(
            path = "${config.documentBasePath()}:runAggregationQuery",
            body = requestBody,
            idToken = idToken
        )
        return parseCountAggregationResponse(response)
    }

    protected suspend fun loadSubCollectionDocumentCount(
        parentPath: String,
        collectionId: String,
        idToken: String?
    ): Int {
        val requestBody = buildCountAggregationQueryBody(
            collectionId = collectionId,
            equalsFilterFieldPath = null,
            equalsFilterValue = null
        )
        val response = restApi.post(
            path = "$parentPath:runAggregationQuery",
            body = requestBody,
            idToken = idToken
        )
        return parseCountAggregationResponse(response)
    }

    private fun buildCountAggregationQueryBody(
        collectionId: String,
        equalsFilterFieldPath: String?,
        equalsFilterValue: JsonObject?
    ): String {
        val escapedCollectionId = escapeFirestoreQueryString(collectionId)
        val whereClause = if (equalsFilterFieldPath.isNullOrBlank() || equalsFilterValue == null) {
            ""
        } else {
            val escapedFieldPath = escapeFirestoreQueryString(equalsFilterFieldPath)
            """,
                    "where":{
                        "fieldFilter":{
                            "field":{"fieldPath":"$escapedFieldPath"},
                            "op":"EQUAL",
                            "value":$equalsFilterValue
                        }
                    }"""
        }
        return """
        {
            "structuredAggregationQuery":{
                "aggregations":[{"alias":"count","count":{}}],
                "structuredQuery":{
                    "from":[{"collectionId":"$escapedCollectionId"}]$whereClause
                }
            }
        }
        """.trimIndent()
    }

    private fun buildCountAggregationQueryBodyWithDatePrefix(
        collectionId: String,
        equalsFilterFieldPath: String,
        equalsFilterValue: String,
        dateFieldPath: String,
        datePrefix: String
    ): String {
        val escapedCollectionId = escapeFirestoreQueryString(collectionId)
        val escapedEqualsFieldPath = escapeFirestoreQueryString(equalsFilterFieldPath)
        val escapedEqualsValue = escapeFirestoreQueryString(equalsFilterValue)
        val escapedDateFieldPath = escapeFirestoreQueryString(dateFieldPath)
        val escapedDatePrefix = escapeFirestoreQueryString(datePrefix)
        val escapedDateUpperBound = escapeFirestoreQueryString("$datePrefix\uf8ff")
        return """
        {
            "structuredAggregationQuery":{
                "aggregations":[{"alias":"count","count":{}}],
                "structuredQuery":{
                    "from":[{"collectionId":"$escapedCollectionId"}],
                    "where":{
                        "compositeFilter":{
                            "op":"AND",
                            "filters":[
                                {"fieldFilter":{"field":{"fieldPath":"$escapedEqualsFieldPath"},"op":"EQUAL","value":{"stringValue":"$escapedEqualsValue"}}},
                                {"fieldFilter":{"field":{"fieldPath":"$escapedDateFieldPath"},"op":"GREATER_THAN_OR_EQUAL","value":{"stringValue":"$escapedDatePrefix"}}},
                                {"fieldFilter":{"field":{"fieldPath":"$escapedDateFieldPath"},"op":"LESS_THAN_OR_EQUAL","value":{"stringValue":"$escapedDateUpperBound"}}}
                            ]
                        }
                    }
                }
            }
        }
        """.trimIndent()
    }

    private fun parseCountAggregationResponse(response: String): Int {
        val trimmedResponse = response.trim()
        if (trimmedResponse.isEmpty()) return 0
        val parsedElement = runCatching { Json.parseToJsonElement(trimmedResponse) }.getOrNull()
        if (parsedElement != null) {
            val parsedCount = when (parsedElement) {
                is JsonObject -> extractCountFromAggregationItem(parsedElement)
                is kotlinx.serialization.json.JsonArray -> parsedElement.firstNotNullOfOrNull { item ->
                    extractCountFromAggregationItem(item.jsonObject)
                }
                else -> null
            }
            if (parsedCount != null) return parsedCount
        }
        val fallbackCount = trimmedResponse.lineSequence()
            .map { line -> line.trim() }
            .filter { line -> line.startsWith("{") && line.endsWith("}") }
            .mapNotNull { line -> runCatching { Json.parseToJsonElement(line).jsonObject }.getOrNull() }
            .mapNotNull { item -> extractCountFromAggregationItem(item) }
            .firstOrNull()
        return fallbackCount ?: 0
    }

    private fun extractCountFromAggregationItem(item: JsonObject): Int? {
        val result = item["result"]?.jsonObject ?: return null
        val aggregateFields = result["aggregateFields"]?.jsonObject ?: return null
        val aliasField = aggregateFields["count"]?.jsonObject
            ?: aggregateFields.values.firstOrNull()?.jsonObject ?: return null
        val integerValue = aliasField["integerValue"]?.jsonPrimitive?.longOrNull
        val doubleValue = aliasField["doubleValue"]?.jsonPrimitive?.doubleOrNull
        return integerValue?.toInt() ?: doubleValue?.toInt()
    }

    // ── Structured queries ────────────────────────────────────────────────────

    protected suspend fun runCollectionQuery(
        collectionId: String,
        idToken: String?,
        orderByFieldPath: String? = null,
        orderByDescending: Boolean = false
    ): List<JsonObject> {
        val path = "${config.documentBasePath()}:runQuery"
        val orderBySection = if (orderByFieldPath != null) {
            val direction = if (orderByDescending) "DESCENDING" else "ASCENDING"
            """,
                "orderBy": [
                  {
                    "field": { "fieldPath": "${escapeFirestoreQueryString(orderByFieldPath)}" },
                    "direction": "$direction"
                  }
                ]"""
        } else ""
        val body = """
            {
              "structuredQuery": {
                "from": [{ "collectionId": "$collectionId" }]$orderBySection
              }
            }
        """.trimIndent()
        val response = restApi.post(path = path, body = body, idToken = idToken)
        val parsed = Json.parseToJsonElement(response).jsonArray
        return parsed.mapNotNull { element -> element.jsonObject["document"]?.jsonObject }
    }

    protected suspend fun runUserScopedQuery(
        collectionId: String,
        userId: String,
        idToken: String?,
        orderByFieldPath: String? = null,
        orderByDescending: Boolean = false,
        limit: Int? = null
    ): List<JsonObject> {
        val path = "${config.documentBasePath()}:runQuery"
        val orderBySection = if (orderByFieldPath != null) {
            val direction = if (orderByDescending) "DESCENDING" else "ASCENDING"
            """,
                "orderBy": [
                  {
                    "field": { "fieldPath": "${escapeFirestoreQueryString(orderByFieldPath)}" },
                    "direction": "$direction"
                  }
                ]"""
        } else ""
        val limitSection = if (limit != null && limit > 0) ""","limit": ${limit}""" else ""
        val body = """
            {
              "structuredQuery": {
                "from": [{ "collectionId": "$collectionId" }],
                "where": {
                    "fieldFilter": {
                      "field": { "fieldPath": "userId" },
                      "op": "EQUAL",
                      "value": { "stringValue": "${escapeFirestoreQueryString(userId)}" }
                    }
                  }$orderBySection$limitSection
                }
            }
        """.trimIndent()
        val response = restApi.post(path = path, body = body, idToken = idToken)
        val parsed = Json.parseToJsonElement(response).jsonArray
        return parsed.mapNotNull { element -> element.jsonObject["document"]?.jsonObject }
    }

    protected suspend fun runLegacyUserFieldQuery(
        collectionId: String,
        userId: String,
        idToken: String?,
        legacyFieldPath: String
    ): List<JsonObject> {
        return runCatching {
            runFieldScopedQuery(collectionId = collectionId, fieldPath = legacyFieldPath, fieldValue = userId, idToken = idToken, orderByCreatedAtDesc = false)
        }.recoverCatching {
            runFieldScopedQuery(collectionId = collectionId, fieldPath = legacyFieldPath, fieldValue = userId, idToken = null, orderByCreatedAtDesc = false)
        }.getOrElse { emptyList() }
    }

    protected suspend fun runDocumentNamePrefixQuery(
        collectionId: String,
        documentIdPrefix: String,
        idToken: String?
    ): List<JsonObject> {
        val path = "${config.documentBasePath()}:runQuery"
        val escapedCollectionId = escapeFirestoreQueryString(collectionId)
        val escapedPrefix = escapeFirestoreQueryString(documentIdPrefix)
        val body = """
            {
              "structuredQuery": {
                "from": [{ "collectionId": "$escapedCollectionId" }],
                "where": {
                  "compositeFilter": {
                    "op": "AND",
                    "filters": [
                      {
                        "fieldFilter": {
                          "field": { "fieldPath": "__name__" },
                          "op": "GREATER_THAN_OR_EQUAL",
                          "value": { "referenceValue": "${config.documentBasePath()}/$collectionId/$escapedPrefix" }
                        }
                      },
                      {
                        "fieldFilter": {
                          "field": { "fieldPath": "__name__" },
                          "op": "LESS_THAN",
                          "value": { "referenceValue": "${config.documentBasePath()}/$collectionId/${escapedPrefix}~" }
                        }
                      }
                    ]
                  }
                }
              }
            }
        """.trimIndent()
        val response = runCatching { restApi.post(path = path, body = body, idToken = idToken) }
            .recoverCatching { restApi.post(path = path, body = body, idToken = null) }
            .getOrElse { return emptyList() }
        val parsed = Json.parseToJsonElement(response).jsonArray
        return parsed.mapNotNull { element -> element.jsonObject["document"]?.jsonObject }
    }

    protected suspend fun runFieldScopedQuery(
        collectionId: String,
        fieldPath: String,
        fieldValue: String,
        idToken: String?,
        orderByCreatedAtDesc: Boolean,
        limit: Int? = null
    ): List<JsonObject> {
        val path = "${config.documentBasePath()}:runQuery"
        val orderBySection = if (orderByCreatedAtDesc) {
            """,
                "orderBy": [
                  {
                    "field": { "fieldPath": "createdAt" },
                    "direction": "DESCENDING"
                  }
                ]"""
        } else ""
        val limitSection = if (limit != null && limit > 0) ""","limit": $limit""" else ""
        val body = """
            {
              "structuredQuery": {
                "from": [{ "collectionId": "$collectionId" }],
                "where": {
                    "fieldFilter": {
                      "field": { "fieldPath": "$fieldPath" },
                      "op": "EQUAL",
                      "value": { "stringValue": "${escapeFirestoreQueryString(fieldValue)}" }
                    }
                  }$orderBySection$limitSection
                }
            }
        """.trimIndent()
        val response = restApi.post(path = path, body = body, idToken = idToken)
        val parsed = Json.parseToJsonElement(response).jsonArray
        return parsed.mapNotNull { element -> element.jsonObject["document"]?.jsonObject }
    }

    protected suspend fun runArrayContainsStringQuery(
        collectionId: String,
        fieldPath: String,
        fieldValue: String,
        idToken: String?,
        limit: Int? = null
    ): List<JsonObject> {
        val path = "${config.documentBasePath()}:runQuery"
        val limitSection = if (limit != null && limit > 0) ""","limit": $limit""" else ""
        val body = """
            {
              "structuredQuery": {
                "from": [{ "collectionId": "${escapeFirestoreQueryString(collectionId)}" }],
                "where": {
                    "fieldFilter": {
                      "field": { "fieldPath": "${escapeFirestoreQueryString(fieldPath)}" },
                      "op": "ARRAY_CONTAINS",
                      "value": { "stringValue": "${escapeFirestoreQueryString(fieldValue)}" }
                    }
                  }$limitSection
                }
            }
        """.trimIndent()
        val response = restApi.post(path = path, body = body, idToken = idToken)
        val parsed = Json.parseToJsonElement(response).jsonArray
        return parsed.mapNotNull { element -> element.jsonObject["document"]?.jsonObject }
    }

    protected suspend fun runCafeCollectionQuery(
        sort: CafeSort,
        cursor: String?,
        limit: Int,
        country: String?,
        city: String?,
        onlyApproved: Boolean = false,
        idToken: String?
    ): List<JsonObject> {
        val safeLimit = if (limit > 0) limit else 1
        val isLatestSort = sort == CafeSort.LATEST
        val orderFieldPath = if (isLatestSort) "__name__"
        else if (sort == CafeSort.POPULAR) "reviewCount"
        else "ratingAvg"
        val filters = mutableListOf<String>()
        if (onlyApproved) {
            filters.add("""{"fieldFilter":{"field":{"fieldPath":"approved"},"op":"EQUAL","value":{"booleanValue":true}}}""")
        }
        if (!country.isNullOrBlank()) {
            filters.add("""{"fieldFilter":{"field":{"fieldPath":"region.country"},"op":"EQUAL","value":{"stringValue":"${escapeFirestoreQueryString(country)}"}}}""")
        }
        if (!city.isNullOrBlank()) {
            filters.add("""{"fieldFilter":{"field":{"fieldPath":"region.city"},"op":"EQUAL","value":{"stringValue":"${escapeFirestoreQueryString(city)}"}}}""")
        }
        val whereSection = when (filters.size) {
            0 -> ""
            1 -> ""","where": ${filters.first()}"""
            else -> ""","where": {"compositeFilter": {"op": "AND","filters": [${filters.joinToString(",")}]}}"""
        }
        val startAfterSection = cursor.toStartAfterSection(latestSort = isLatestSort)
        val path = "${config.documentBasePath()}:runQuery"
        val body = """
            {
              "structuredQuery": {
                "from": [{"collectionId": "${FirestorePaths.CAFES}"}],
                "orderBy": [
                  {"field": {"fieldPath": "$orderFieldPath"},"direction": "DESCENDING"}${if (!isLatestSort) """,{"field": {"fieldPath": "__name__"},"direction": "ASCENDING"}""" else ""}
                ]$whereSection$startAfterSection,
                "limit": $safeLimit
              }
            }
        """.trimIndent()
        val response = restApi.post(path = path, body = body, idToken = idToken)
        val parsed = Json.parseToJsonElement(response).jsonArray
        return parsed.mapNotNull { element -> element.jsonObject["document"]?.jsonObject }
    }

    protected suspend fun runCastCollectionGroupQuery(
        sort: CastSort,
        cursor: String?,
        limit: Int,
        cafeIds: List<String>? = null,
        idToken: String?
    ): List<JsonObject> {
        val safeLimit = if (limit > 0) limit else 1
        val startAfterSection = cursor.toCastStartAfterSection(sort)
        val cafeFilterSection = when {
            cafeIds.isNullOrEmpty() -> ""
            cafeIds.size == 1 -> {
                val cafeId = escapeFirestoreQueryString(cafeIds.first())
                """"where": {"fieldFilter": {"field": {"fieldPath": "cafeId"},"op": "EQUAL","value": {"stringValue": "$cafeId"}}},"""
            }
            else -> {
                val firestoreValues = cafeIds.joinToString(",") { cafeId ->
                    """{"stringValue": "${escapeFirestoreQueryString(cafeId)}"}"""
                }
                """"where": {"fieldFilter": {"field": {"fieldPath": "cafeId"},"op": "IN","value": {"arrayValue": {"values": [$firestoreValues]}}}},"""
            }
        }
        val orderBySection = when (sort) {
            CastSort.LATEST -> """"orderBy": [{"field": {"fieldPath": "__name__"},"direction": "DESCENDING"}]"""
            CastSort.HOME_LINKED_FIRST -> """"orderBy": [{"field": {"fieldPath": "linkedUserId"},"direction": "DESCENDING"},{"field": {"fieldPath": "followerCount"},"direction": "DESCENDING"},{"field": {"fieldPath": "__name__"},"direction": "ASCENDING"}]"""
            else -> """"orderBy": [{"field": {"fieldPath": "followerCount"},"direction": "DESCENDING"},{"field": {"fieldPath": "__name__"},"direction": "ASCENDING"}]"""
        }
        val path = "${config.documentBasePath()}:runQuery"
        val body = """
            {
              "structuredQuery": {
                "from": [{"collectionId": "${FirestorePaths.CAFE_CASTS}","allDescendants": true}],
                $cafeFilterSection
                $orderBySection$startAfterSection,
                "limit": $safeLimit
              }
            }
        """.trimIndent()
        val response = restApi.post(path = path, body = body, idToken = idToken)
        val parsed = Json.parseToJsonElement(response).jsonArray
        return parsed.mapNotNull { element -> element.jsonObject["document"]?.jsonObject }
    }

    protected suspend fun runCafeCastQuery(
        cafeId: String,
        cursor: String?,
        limit: Int,
        idToken: String?
    ): List<JsonObject> {
        val safeLimit = if (limit > 0) limit else 1
        val startAfterSection = cursor.toCafeCastStartAfterSection()
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFES}/$cafeId:runQuery"
        val body = """
            {
              "structuredQuery": {
                "from": [{"collectionId": "${FirestorePaths.CAFE_CASTS}"}],
                "orderBy": [
                  {"field": {"fieldPath": "name"},"direction": "ASCENDING"},
                  {"field": {"fieldPath": "__name__"},"direction": "ASCENDING"}
                ]$startAfterSection,
                "limit": $safeLimit
              }
            }
        """.trimIndent()
        val response = restApi.post(path = path, body = body, idToken = idToken)
        val parsed = Json.parseToJsonElement(response).jsonArray
        return parsed.mapNotNull { element -> element.jsonObject["document"]?.jsonObject }
    }

    protected suspend fun runCafeCastIdsInQuery(
        cafeId: String,
        castIds: List<String>,
        idToken: String?
    ): List<JsonObject> {
        if (castIds.isEmpty()) return emptyList()
        val references = castIds.joinToString(",") { castId ->
            val sanitizedCastId = castId.trim()
            """{"referenceValue": "${config.documentBasePath()}/${FirestorePaths.CAFES}/${escapeFirestoreQueryString(cafeId)}/${FirestorePaths.CAFE_CASTS}/${escapeFirestoreQueryString(sanitizedCastId)}"}"""
        }
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFES}/$cafeId:runQuery"
        val body = """
            {
              "structuredQuery": {
                "from": [{"collectionId": "${FirestorePaths.CAFE_CASTS}"}],
                "where": {
                  "fieldFilter": {
                    "field": {"fieldPath": "__name__"},
                    "op": "IN",
                    "value": {"arrayValue": {"values": [ $references ]}}
                  }
                },
                "limit": ${castIds.size}
              }
            }
        """.trimIndent()
        val response = restApi.post(path = path, body = body, idToken = idToken)
        val parsed = Json.parseToJsonElement(response).jsonArray
        return parsed.mapNotNull { element -> element.jsonObject["document"]?.jsonObject }
    }

    protected suspend fun runCastScheduleRangeQuery(
        castId: String,
        fromDate: String,
        toDate: String,
        idToken: String?
    ): List<JsonObject> {
        val path = "${config.documentBasePath()}:runQuery"
        val body = """
            {
              "structuredQuery": {
                "from": [{"collectionId": "${FirestorePaths.CAST_SCHEDULES}"}],
                "where": {
                  "compositeFilter": {
                    "op": "AND",
                    "filters": [
                      {"fieldFilter": {"field": {"fieldPath": "castId"},"op": "EQUAL","value": {"stringValue": "${escapeFirestoreQueryString(castId)}"}}},
                      {"fieldFilter": {"field": {"fieldPath": "date"},"op": "GREATER_THAN_OR_EQUAL","value": {"stringValue": "${escapeFirestoreQueryString(fromDate)}"}}},
                      {"fieldFilter": {"field": {"fieldPath": "date"},"op": "LESS_THAN_OR_EQUAL","value": {"stringValue": "${escapeFirestoreQueryString(toDate)}"}}}
                    ]
                  }
                },
                "orderBy": [{"field": {"fieldPath": "date"},"direction": "ASCENDING"}]
              }
            }
        """.trimIndent()
        val response = restApi.post(path = path, body = body, idToken = idToken)
        val parsed = Json.parseToJsonElement(response).jsonArray
        return parsed.mapNotNull { element -> element.jsonObject["document"]?.jsonObject }
    }

    protected suspend fun runWorkingCastScheduleQuery(
        cafeId: String,
        date: String,
        idToken: String?
    ): List<JsonObject> {
        val path = "${config.documentBasePath()}:runQuery"
        val body = """
            {
              "structuredQuery": {
                "from": [{"collectionId": "${FirestorePaths.CAST_SCHEDULES}"}],
                "where": {
                  "compositeFilter": {
                    "op": "AND",
                    "filters": [
                      {"fieldFilter": {"field": {"fieldPath": "cafeId"},"op": "EQUAL","value": {"stringValue": "${escapeFirestoreQueryString(cafeId)}"}}},
                      {"fieldFilter": {"field": {"fieldPath": "date"},"op": "EQUAL","value": {"stringValue": "${escapeFirestoreQueryString(date)}"}}},
                      {"fieldFilter": {"field": {"fieldPath": "status"},"op": "EQUAL","value": {"stringValue": "${CastScheduleStatus.WORK.name}"}}}
                    ]
                  }
                }
              }
            }
        """.trimIndent()
        val response = restApi.post(path = path, body = body, idToken = idToken)
        val parsed = Json.parseToJsonElement(response).jsonArray
        return parsed.mapNotNull { element -> element.jsonObject["document"]?.jsonObject }
    }

    protected suspend fun runGuestCastScheduleRangeQuery(
        cafeId: String,
        fromDate: String,
        toDate: String,
        idToken: String?
    ): List<JsonObject> {
        val path = "${config.documentBasePath()}:runQuery"
        val body = """
            {
              "structuredQuery": {
                "from": [{"collectionId": "${FirestorePaths.GUEST_CAST_SCHEDULES}"}],
                "where": {"fieldFilter": {"field": {"fieldPath": "cafeId"},"op": "EQUAL","value": {"stringValue": "${escapeFirestoreQueryString(cafeId)}"}}}
              }
            }
        """.trimIndent()
        val response = restApi.post(path = path, body = body, idToken = idToken)
        val parsed = Json.parseToJsonElement(response).jsonArray
        return parsed.mapNotNull { element -> element.jsonObject["document"]?.jsonObject }
    }

    protected suspend fun runCastFollowerQuery(
        castId: String,
        idToken: String?,
        orderByCreatedAtDesc: Boolean,
        limit: Int
    ): List<JsonObject> {
        val safeLimit = limit.coerceAtLeast(1)
        val path = "${config.documentBasePath()}:runQuery"
        val orderBySection = if (orderByCreatedAtDesc) {
            ""","orderBy": [{"field": {"fieldPath": "createdAt"},"direction": "DESCENDING"}]"""
        } else ""
        val body = """
            {
              "structuredQuery": {
                "from": [{"collectionId": "${FirestorePaths.CAST_FOLLOWS}"}],
                "where": {
                  "fieldFilter": {
                    "field": {"fieldPath": "castId"},
                    "op": "EQUAL",
                    "value": {"stringValue": "${escapeFirestoreQueryString(castId)}"}
                  }
                }$orderBySection,
                "limit": $safeLimit
              }
            }
        """.trimIndent()
        val response = restApi.post(path = path, body = body, idToken = idToken)
        val parsed = Json.parseToJsonElement(response).jsonArray
        return parsed.mapNotNull { element -> element.jsonObject["document"]?.jsonObject }
    }

    protected suspend fun runVerifiedVisitUserQuery(cafeId: String, idToken: String?): List<JsonObject> {
        val path = "${config.documentBasePath()}:runQuery"
        val body = """
            {
              "structuredQuery": {
                "from": [{"collectionId": "${FirestorePaths.VISITS}"}],
                "where": {
                  "compositeFilter": {
                    "op": "AND",
                    "filters": [
                      {"fieldFilter": {"field": {"fieldPath": "cafeId"},"op": "EQUAL","value": {"stringValue": "${escapeFirestoreQueryString(cafeId)}"}}},
                      {"fieldFilter": {"field": {"fieldPath": "verified"},"op": "EQUAL","value": {"booleanValue": true}}}
                    ]
                  }
                }
              }
            }
        """.trimIndent()
        val response = restApi.post(path = path, body = body, idToken = idToken)
        val parsed = Json.parseToJsonElement(response).jsonArray
        return parsed.mapNotNull { element -> element.jsonObject["document"]?.jsonObject }
    }

    protected suspend fun runVerifiedVisitByUserCafeQuery(userId: String, cafeId: String, idToken: String?): List<JsonObject> {
        val path = "${config.documentBasePath()}:runQuery"
        val body = """
            {
              "structuredQuery": {
                "from": [{"collectionId": "${FirestorePaths.VISITS}"}],
                "where": {
                  "compositeFilter": {
                    "op": "AND",
                    "filters": [
                      {"fieldFilter": {"field": {"fieldPath": "userId"},"op": "EQUAL","value": {"stringValue": "${escapeFirestoreQueryString(userId)}"}}},
                      {"fieldFilter": {"field": {"fieldPath": "cafeId"},"op": "EQUAL","value": {"stringValue": "${escapeFirestoreQueryString(cafeId)}"}}},
                      {"fieldFilter": {"field": {"fieldPath": "verified"},"op": "EQUAL","value": {"booleanValue": true}}}
                    ]
                  }
                }
              }
            }
        """.trimIndent()
        val response = restApi.post(path = path, body = body, idToken = idToken)
        val parsed = Json.parseToJsonElement(response).jsonArray
        return parsed.mapNotNull { element -> element.jsonObject["document"]?.jsonObject }
    }

    protected suspend fun runUserVisitPageQuery(
        userId: String,
        cursor: String?,
        limit: Int,
        idToken: String?
    ): List<JsonObject> {
        val safeLimit = limit.coerceAtLeast(1)
        val startAfterSection = cursor.toVisitStartAfterSection()
        val path = "${config.documentBasePath()}:runQuery"
        val body = """
            {
              "structuredQuery": {
                "from": [{"collectionId": "${FirestorePaths.VISITS}"}],
                "where": {
                  "fieldFilter": {
                    "field": {"fieldPath": "userId"},
                    "op": "EQUAL",
                    "value": {"stringValue": "${escapeFirestoreQueryString(userId)}"}
                  }
                },
                "orderBy": [
                  {"field": {"fieldPath": "visitedAt"},"direction": "DESCENDING"},
                  {"field": {"fieldPath": "__name__"},"direction": "DESCENDING"}
                ]$startAfterSection,
                "limit": $safeLimit
              }
            }
        """.trimIndent()
        val response = restApi.post(path = path, body = body, idToken = idToken)
        val parsed = Json.parseToJsonElement(response).jsonArray
        return parsed.mapNotNull { element -> element.jsonObject["document"]?.jsonObject }
    }

    protected suspend fun runCafeReviewPageQuery(
        cafeId: String,
        cursor: String?,
        limit: Int,
        idToken: String?
    ): List<JsonObject> {
        val safeLimit = if (limit > 0) limit else 1
        val startAfterSection = cursor.toReviewStartAfterSection()
        val path = "${config.documentBasePath()}:runQuery"
        val body = """
            {
              "structuredQuery": {
                "from": [{"collectionId": "${FirestorePaths.REVIEWS}"}],
                "where": {
                  "fieldFilter": {
                    "field": {"fieldPath": "cafeId"},
                    "op": "EQUAL",
                    "value": {"stringValue": "${escapeFirestoreQueryString(cafeId)}"}
                  }
                },
                "orderBy": [{"field": {"fieldPath": "createdAt"},"direction": "DESCENDING"}]$startAfterSection,
                "limit": $safeLimit
              }
            }
        """.trimIndent()
        val response = restApi.post(path = path, body = body, idToken = idToken)
        val parsed = Json.parseToJsonElement(response).jsonArray
        return parsed.mapNotNull { element -> element.jsonObject["document"]?.jsonObject }
    }

    protected suspend fun runRecentTaggedReviewQuery(
        cafeId: String,
        castId: String,
        limit: Int,
        idToken: String?
    ): List<JsonObject> {
        val safeLimit = if (limit > 0) limit else 1
        val path = "${config.documentBasePath()}:runQuery"
        val body = """
            {
              "structuredQuery": {
                "from": [{"collectionId": "${FirestorePaths.REVIEWS}"}],
                "where": {
                  "compositeFilter": {
                    "op": "AND",
                    "filters": [
                      {"fieldFilter": {"field": {"fieldPath": "cafeId"},"op": "EQUAL","value": {"stringValue": "${escapeFirestoreQueryString(cafeId)}"}}},
                      {"fieldFilter": {"field": {"fieldPath": "taggedCastIds"},"op": "ARRAY_CONTAINS","value": {"stringValue": "${escapeFirestoreQueryString(castId)}"}}}
                    ]
                  }
                },
                "orderBy": [{"field": {"fieldPath": "createdAt"},"direction": "DESCENDING"}],
                "limit": $safeLimit
              }
            }
        """.trimIndent()
        val response = restApi.post(path = path, body = body, idToken = idToken)
        val parsed = Json.parseToJsonElement(response).jsonArray
        return parsed.mapNotNull { element -> element.jsonObject["document"]?.jsonObject }
    }

    protected suspend fun runCafeNoticePageQuery(
        cafeId: String,
        cursor: String?,
        limit: Int,
        idToken: String?
    ): List<JsonObject> {
        val safeLimit = if (limit > 0) limit else 1
        val startAfterSection = cursor.toNoticeStartAfterSection()
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFES}/$cafeId:runQuery"
        val body = """
            {
              "structuredQuery": {
                "from": [{"collectionId": "${FirestorePaths.CAFE_NOTICES}"}],
                "orderBy": [
                  {"field": {"fieldPath": "createdAt"},"direction": "DESCENDING"},
                  {"field": {"fieldPath": "__name__"},"direction": "DESCENDING"}
                ]$startAfterSection,
                "limit": $safeLimit
              }
            }
        """.trimIndent()
        val response = restApi.post(path = path, body = body, idToken = idToken)
        val parsed = Json.parseToJsonElement(response).jsonArray
        return parsed.mapNotNull { element -> element.jsonObject["document"]?.jsonObject }
    }

    protected suspend fun runCafeEventPageQuery(
        cafeId: String,
        cursor: String?,
        limit: Int,
        idToken: String?
    ): List<JsonObject> {
        val safeLimit = if (limit > 0) limit else 1
        val startAfterSection = cursor.toStringFieldStartAfterSection()
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFES}/$cafeId:runQuery"
        val body = """
            {
              "structuredQuery": {
                "from": [{"collectionId": "${FirestorePaths.CAFE_EVENTS}"}],
                "orderBy": [{"field": {"fieldPath": "startDate"},"direction": "DESCENDING"}]$startAfterSection,
                "limit": $safeLimit
              }
            }
        """.trimIndent()
        val response = restApi.post(path = path, body = body, idToken = idToken)
        val parsed = Json.parseToJsonElement(response).jsonArray
        return parsed.mapNotNull { element -> element.jsonObject["document"]?.jsonObject }
    }

    protected suspend fun runRecentNoticeFeedQuery(limit: Int, idToken: String?): List<JsonObject> {
        val safeLimit = if (limit > 0) limit else 1
        val path = "${config.documentBasePath()}:runQuery"
        val body = """
            {
              "structuredQuery": {
                "from": [{"collectionId": "${FirestorePaths.CAFE_NOTICES}","allDescendants": true}],
                "orderBy": [{"field": {"fieldPath": "createdAt"},"direction": "DESCENDING"}],
                "limit": $safeLimit
              }
            }
        """.trimIndent()
        val response = restApi.post(path = path, body = body, idToken = idToken)
        val parsed = Json.parseToJsonElement(response).jsonArray
        return parsed.mapNotNull { element -> element.jsonObject["document"]?.jsonObject }
    }

    protected suspend fun runHomeCafeEventFeedQuery(
        cursor: String?,
        todayText: String,
        limit: Int,
        idToken: String?
    ): List<JsonObject> {
        val safeLimit = if (limit > 0) limit else 1
        val startAfterSection = cursor.toStringFieldStartAfterSection()
        val path = "${config.documentBasePath()}:runQuery"
        val body = """
            {
              "structuredQuery": {
                "from": [{"collectionId": "${FirestorePaths.CAFE_EVENTS}","allDescendants": true}],
                "where": {
                  "compositeFilter": {
                    "op": "AND",
                    "filters": [
                      {
                        "fieldFilter": {
                          "field": {"fieldPath": "isDimmed"},
                          "op": "EQUAL",
                          "value": {"booleanValue": false}
                        }
                      },
                      {
                        "fieldFilter": {
                          "field": {"fieldPath": "endDate"},
                          "op": "GREATER_THAN_OR_EQUAL",
                          "value": {"stringValue": "${escapeFirestoreQueryString(todayText)}"}
                        }
                      }
                    ]
                  }
                },
                "orderBy": [{"field": {"fieldPath": "endDate"},"direction": "ASCENDING"}]$startAfterSection,
                "limit": $safeLimit
              }
            }
        """.trimIndent()
        val response = restApi.post(path = path, body = body, idToken = idToken)
        val parsed = Json.parseToJsonElement(response).jsonArray
        return parsed.mapNotNull { element -> element.jsonObject["document"]?.jsonObject }
    }

    protected suspend fun runInquiryPageQuery(cursor: String?, limit: Int, idToken: String?): List<JsonObject> {
        val safeLimit = if (limit > 0) limit else 1
        val startAfterSection = cursor.toStringFieldStartAfterSection()
        val path = "${config.documentBasePath()}:runQuery"
        val body = """
            {
              "structuredQuery": {
                "from": [{"collectionId": "${FirestorePaths.INQUIRIES}"}],
                "orderBy": [{"field": {"fieldPath": "createdAt"},"direction": "DESCENDING"}]$startAfterSection,
                "limit": $safeLimit
              }
            }
        """.trimIndent()
        val response = restApi.post(path = path, body = body, idToken = idToken)
        val parsed = Json.parseToJsonElement(response).jsonArray
        return parsed.mapNotNull { element -> element.jsonObject["document"]?.jsonObject }
    }

    protected suspend fun runReportPageQuery(cursor: String?, limit: Int, idToken: String?): List<JsonObject> {
        val safeLimit = if (limit > 0) limit else 1
        val startAfterSection = cursor.toStringFieldStartAfterSection()
        val path = "${config.documentBasePath()}:runQuery"
        val body = """
            {
              "structuredQuery": {
                "from": [{"collectionId": "${FirestorePaths.REPORTS}"}],
                "orderBy": [{"field": {"fieldPath": "createdAt"},"direction": "DESCENDING"}]$startAfterSection,
                "limit": $safeLimit
              }
            }
        """.trimIndent()
        val response = restApi.post(path = path, body = body, idToken = idToken)
        val parsed = Json.parseToJsonElement(response).jsonArray
        return parsed.mapNotNull { element -> element.jsonObject["document"]?.jsonObject }
    }

    protected suspend fun runCastByLinkedUserIdQuery(userId: String, idToken: String?): List<JsonObject> {
        val path = "${config.documentBasePath()}:runQuery"
        val body = """
            {
              "structuredQuery": {
                "from": [{"collectionId": "${FirestorePaths.CAFE_CASTS}","allDescendants": true}],
                "where": {
                  "fieldFilter": {
                    "field": {"fieldPath": "linkedUserId"},
                    "op": "EQUAL",
                    "value": {"stringValue": "${escapeFirestoreQueryString(userId)}"}
                  }
                },
                "limit": 1
              }
            }
        """.trimIndent()
        val response = restApi.post(path = path, body = body, idToken = idToken)
        val parsed = Json.parseToJsonElement(response).jsonArray
        return parsed.mapNotNull { element -> element.jsonObject["document"]?.jsonObject }
    }

    protected suspend fun runCastByUserFieldQuery(userId: String, idToken: String?, fieldPath: String): List<JsonObject> {
        val path = "${config.documentBasePath()}:runQuery"
        val body = """
            {
              "structuredQuery": {
                "from": [{"collectionId": "${FirestorePaths.CAFE_CASTS}","allDescendants": true}],
                "where": {
                  "fieldFilter": {
                    "field": {"fieldPath": "${escapeFirestoreQueryString(fieldPath)}"},
                    "op": "EQUAL",
                    "value": {"stringValue": "${escapeFirestoreQueryString(userId)}"}
                  }
                },
                "limit": 1
              }
            }
        """.trimIndent()
        val response = restApi.post(path = path, body = body, idToken = idToken)
        val parsed = Json.parseToJsonElement(response).jsonArray
        return parsed.mapNotNull { element -> element.jsonObject["document"]?.jsonObject }
    }

    protected suspend fun runCastByBirthdayKeyQuery(birthdayKey: String, idToken: String?, limit: Int): List<JsonObject> {
        val safeLimit = if (limit > 0) limit else 1
        val path = "${config.documentBasePath()}:runQuery"
        val body = """
            {
              "structuredQuery": {
                "from": [{"collectionId": "${FirestorePaths.CAFE_CASTS}","allDescendants": true}],
                "where": {
                  "fieldFilter": {
                    "field": {"fieldPath": "$BIRTHDAY_KEY_FIELD"},
                    "op": "EQUAL",
                    "value": {"stringValue": "${escapeFirestoreQueryString(birthdayKey)}"}
                  }
                },
                "orderBy": [{"field": {"fieldPath": "name"},"direction": "ASCENDING"}],
                "limit": $safeLimit
              }
            }
        """.trimIndent()
        val response = restApi.post(path = path, body = body, idToken = idToken)
        val parsed = Json.parseToJsonElement(response).jsonArray
        return parsed.mapNotNull { element -> element.jsonObject["document"]?.jsonObject }
    }

    protected suspend fun loadCafeIdsByRegionRemote(
        country: String?,
        city: String?,
        idToken: String?
    ): Set<String>? {
        if (country.isNullOrBlank() && city.isNullOrBlank()) return null
        val documents = runCatching {
            runCafeCollectionQuery(sort = CafeSort.LATEST, cursor = null, limit = REGION_FILTER_CAFE_CACHE_LIMIT, country = country, city = city, idToken = idToken)
        }.recoverCatching {
            runCafeCollectionQuery(sort = CafeSort.LATEST, cursor = null, limit = REGION_FILTER_CAFE_CACHE_LIMIT, country = country, city = city, idToken = null)
        }.getOrElse { emptyList() }
        return documents.mapNotNull { document -> parseCafeDocument(document)?.id }.toSet()
    }

    protected suspend fun fetchCastSchedulesRemoteInternal(
        castId: String,
        fromDate: String,
        toDate: String,
        idToken: String?
    ): List<CastSchedule> {
        val documents = runCatching {
            runCastScheduleRangeQuery(castId = castId, fromDate = fromDate, toDate = toDate, idToken = idToken)
        }.recoverCatching {
            runCastScheduleRangeQuery(castId = castId, fromDate = fromDate, toDate = toDate, idToken = null)
        }.getOrElse { emptyList() }
        return documents.mapNotNull { document ->
            val fields = document["fields"]?.jsonObject ?: return@mapNotNull null
            val entry = parseCastScheduleDocument(document) ?: return@mapNotNull null
            val cafeId = fields.getFirestoreString("cafeId").orEmpty()
            if (entry.status == CastScheduleStatus.WORK) {
                val startTime = entry.startTime?.trim().orEmpty()
                val endTime = entry.endTime?.trim().orEmpty()
                if (startTime.isNotEmpty() && endTime.isNotEmpty()) {
                    CastSchedule(id = entry.id, castId = entry.castId, cafeId = cafeId, date = entry.date, startTime = startTime, endTime = endTime)
                } else null
            } else null
        }.sortedBy { schedule -> schedule.date }
    }

    protected suspend fun resolveCafeNameForNoticeWrite(cafeId: String, idToken: String?): String {
        val remoteName = runCatching {
            parseCafeDocument(loadCafeDocument(cafeId = cafeId, idToken = idToken))?.name?.trim()?.takeIf { v -> v.isNotEmpty() }
        }.recoverCatching {
            parseCafeDocument(loadCafeDocument(cafeId = cafeId, idToken = null))?.name?.trim()?.takeIf { v -> v.isNotEmpty() }
        }.getOrNull()
        return remoteName ?: cafeId
    }

    protected fun buildCastFollowDocumentId(userId: String, castId: String): String =
        "${userId.replace("/", "_")}_${castId.replace("/", "_")}"

    protected fun buildCafeFavoriteDocumentId(userId: String, cafeId: String): String =
        "${userId.replace("/", "_")}_${cafeId.replace("/", "_")}"

    // ── Document parsers ──────────────────────────────────────────────────────

    protected fun parseCafeDocument(document: JsonObject): Cafe? {
        val fields = document["fields"]?.jsonObject ?: return null
        val name = document["name"]?.jsonPrimitive?.contentOrNull ?: return null
        val cafeId = name.substringAfterLast("/")
        val regionField = fields["region"]?.jsonObject?.get("mapValue")?.jsonObject?.get("fields")?.jsonObject
        val locationField = regionField?.get("location")?.jsonObject?.get("geoPointValue")?.jsonObject
        return Cafe(
            id = cafeId,
            name = fields.getFirestoreString("name").orEmpty(),
            desc = fields.getFirestoreString("desc").orEmpty(),
            region = Region(
                country = regionField?.getFirestoreString("country") ?: fields.getFirestoreString("country") ?: "KR",
                city = regionField?.getFirestoreString("city") ?: fields.getFirestoreString("city") ?: "",
                address = regionField?.getFirestoreString("address") ?: fields.getFirestoreString("address") ?: "",
                location = GeoPoint(
                    latitude = locationField?.get("latitude")?.jsonPrimitive?.doubleOrNull ?: 0.0,
                    longitude = locationField?.get("longitude")?.jsonPrimitive?.doubleOrNull ?: 0.0
                )
            ),
            thumbnailImage = fields.getFirestoreString("thumbnailImage"),
            ratingAvg = fields.getFirestoreDouble("ratingAvg") ?: fields.getFirestoreLong("ratingAvg")?.toDouble() ?: 0.0,
            reviewCount = fields.getFirestoreLong("reviewCount")?.toInt() ?: 0,
            approved = fields.getFirestoreBoolean("approved") ?: true,
            conceptType = fields.getFirestoreString("conceptType") ?: "MAID",
            ownerIds = fields.getFirestoreStringList("ownerIds"),
            socialMedia = run {
                val mapFields = fields.getFirestoreMap("socialMedia")
                if (mapFields == null) emptyMap()
                else buildMap {
                    mapFields.getFirestoreString("instagram")?.let { put("instagram", it) }
                    mapFields.getFirestoreString("twitter")?.let { put("twitter", it) }
                    mapFields.getFirestoreString("tiktok")?.let { put("tiktok", it) }
                    mapFields.getFirestoreString("youtube")?.let { put("youtube", it) }
                }
            },
            reservationUrl = fields.getFirestoreString("reservationUrl"),
            tableCounts = run {
                val tableFields = fields.getFirestoreMap("tableCounts")
                TableCounts(
                    current = tableFields?.getFirestoreInt("current") ?: 0,
                    total = tableFields?.getFirestoreInt("total") ?: 0
                )
            },
            favoriteCount = fields.getFirestoreLong("favoriteCount")?.toInt()
                ?: fields.getFirestoreLong("followerCount")?.toInt()
                ?: 0
        )
    }

    protected fun parseCastDocument(cafeId: String, document: JsonObject): Cast? {
        val fields = document["fields"]?.jsonObject ?: return null
        val name = document["name"]?.jsonPrimitive?.contentOrNull ?: return null
        val castId = name.substringAfterLast("/")
        val linkedUserId = fields.getFirestoreString("linkedUserId")
            ?: fields.getFirestoreString("userId")
            ?: fields.getFirestoreString("uid")
        return Cast(
            id = castId,
            cafeId = cafeId,
            name = fields.getFirestoreString("name").orEmpty(),
            linkedUserId = linkedUserId,
            profileImage = fields.getFirestoreString("profileImage"),
            desc = fields.getFirestoreString("desc").orEmpty(),
            birthday = fields.getFirestoreString("birthday"),
            conceptRole = fields.getFirestoreString("conceptRole") ?: "maid",
            followerCount = fields.getFirestoreLong("followerCount")?.toInt() ?: 0,
            rating = fields.getFirestoreDouble("rating") ?: fields.getFirestoreLong("rating")?.toDouble() ?: 0.0,
            visitCertificationCount = fields.getFirestoreLong("visitCertificationCount")?.toInt()
                ?: fields.getFirestoreMap("stats")?.getFirestoreInt("visitCertificationCount")
                ?: 0
        )
    }

    protected fun parseCollectionGroupCastDocuments(documents: List<JsonObject>): List<Cast> {
        return documents.mapNotNull { document ->
            val documentName = document["name"]?.jsonPrimitive?.contentOrNull
            val fields = document["fields"]?.jsonObject
            val cafeId = documentName?.toCafeIdFromCastDocumentName()
                ?: fields?.getFirestoreString("cafeId") ?: ""
            parseCastDocument(cafeId = cafeId, document = document)
        }
    }

    protected fun parseCastScheduleDocument(document: JsonObject): CastScheduleDocumentEntry? {
        val fields = document["fields"]?.jsonObject ?: return null
        val name = document["name"]?.jsonPrimitive?.contentOrNull ?: return null
        val scheduleId = name.substringAfterLast("/")
        val castId = fields.getFirestoreString("castId") ?: return null
        val date = normalizeScheduleDateOrNull(fields.getFirestoreString("date")) ?: return null
        val startTime = fields.getFirestoreString("startTime")
        val endTime = fields.getFirestoreString("endTime")
        val rawStatus = fields.getFirestoreString("status")
        val status = parseCastScheduleStatus(rawStatus)
            ?: if (!startTime.isNullOrBlank() && !endTime.isNullOrBlank()) CastScheduleStatus.WORK else CastScheduleStatus.OFF
        return CastScheduleDocumentEntry(
            id = scheduleId,
            castId = castId,
            date = date,
            status = status,
            startTime = startTime,
            endTime = endTime
        )
    }

    protected fun parseWorkingCastScheduleDocument(document: JsonObject): CastSchedule? {
        val fields = document["fields"]?.jsonObject ?: return null
        val entry = parseCastScheduleDocument(document) ?: return null
        if (entry.status != CastScheduleStatus.WORK) return null
        val startTime = entry.startTime?.trim().orEmpty()
        val endTime = entry.endTime?.trim().orEmpty()
        if (startTime.isEmpty() || endTime.isEmpty()) return null
        val cafeId = fields.getFirestoreString("cafeId").orEmpty()
        if (cafeId.isBlank()) return null
        return CastSchedule(id = entry.id, castId = entry.castId, cafeId = cafeId, date = entry.date, startTime = startTime, endTime = endTime)
    }

    protected fun parseGuestCastScheduleDocument(document: JsonObject): GuestCastSchedule? {
        val fields = document["fields"]?.jsonObject ?: return null
        val name = document["name"]?.jsonPrimitive?.contentOrNull ?: return null
        val scheduleId = name.substringAfterLast("/")
        val cafeId = fields.getFirestoreString("cafeId")?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val date = normalizeScheduleDateOrNull(fields.getFirestoreString("date")) ?: return null
        val guestName = fields.getFirestoreString("name")?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val startTime = fields.getFirestoreString("startTime")?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val endTime = fields.getFirestoreString("endTime")?.trim()?.takeIf { it.isNotEmpty() } ?: return null

        return GuestCastSchedule(
            id = scheduleId,
            cafeId = cafeId,
            date = date,
            name = guestName,
            profileImage = fields.getFirestoreString("profileImage")?.trim()?.takeIf { it.isNotEmpty() },
            startTime = startTime,
            endTime = endTime,
            memo = fields.getFirestoreString("memo")?.trim()?.takeIf { it.isNotEmpty() }
        )
    }

    protected fun parseCastFollowDocument(document: JsonObject): CastFollowerSnapshot? {
        val fields = document["fields"]?.jsonObject ?: return null
        val userId = fields.getFirestoreString("userId") ?: return null
        val createdAt = fields.getFirestoreString("createdAt").orEmpty()
        return CastFollowerSnapshot(
            userId = userId,
            followedAt = createdAt,
            userNickname = fields.getFirestoreString("userNickname"),
            userProfileImage = fields.getFirestoreString("userProfileImage")
        )
    }

    protected fun parseCastClaimDocument(document: JsonObject): CastClaim? {
        val fields = document["fields"]?.jsonObject ?: return null
        val name = document["name"]?.jsonPrimitive?.contentOrNull ?: return null
        val claimId = name.substringAfterLast("/")
        val userId = fields.getFirestoreString("userId") ?: return null
        val cafeId = fields.getFirestoreString("cafeId") ?: return null
        val castId = fields.getFirestoreString("castId") ?: return null
        val castName = fields.getFirestoreString("castName").orEmpty()
        val status = when (fields.getFirestoreString("status")?.uppercase()) {
            CastClaimStatus.APPROVED.name -> CastClaimStatus.APPROVED
            CastClaimStatus.REJECTED.name -> CastClaimStatus.REJECTED
            else -> CastClaimStatus.PENDING
        }
        return CastClaim(
            id = claimId,
            userId = userId,
            cafeId = cafeId,
            castId = castId,
            castName = castName,
            requesterNickname = fields.getFirestoreString("requesterNickname"),
            requesterProfileImage = fields.getFirestoreString("requesterProfileImage"),
            status = status,
            message = fields.getFirestoreString("message"),
            evidenceImageUrls = fields.getFirestoreStringList("evidenceImageUrls"),
            reviewedBy = fields.getFirestoreString("reviewedBy"),
            reviewedAt = fields.getFirestoreString("reviewedAt"),
            createdAt = fields.getFirestoreString("createdAt").orEmpty(),
            createdAtLabel = fields.getFirestoreString("createdAtLabel") ?: "방금 전"
        )
    }

    protected fun parseReviewDocument(document: JsonObject): Review? {
        val fields = document["fields"]?.jsonObject ?: return null
        val name = document["name"]?.jsonPrimitive?.contentOrNull ?: return null
        val reviewId = name.substringAfterLast("/")
        val userId = fields.getFirestoreString("userId") ?: return null
        val cafeId = fields.getFirestoreString("cafeId") ?: return null
        val ratingValue = fields.getFirestoreDouble("rating") ?: fields.getFirestoreLong("rating")?.toDouble() ?: 0.0
        return Review(
            id = reviewId,
            userId = userId,
            userNickname = fields.getFirestoreString("userNickname") ?: "",
            cafeId = cafeId,
            visitId = fields.getFirestoreString("visitId").orEmpty(),
            rating = ratingValue.toFloat(),
            content = fields.getFirestoreString("content").orEmpty(),
            imageUrls = fields.getFirestoreStringList("imageUrls"),
            taggedCastIds = fields.getFirestoreStringList("taggedCastIds"),
            likeCount = fields.getFirestoreLong("likeCount")?.toInt() ?: 0,
            createdAt = fields.getFirestoreString("createdAt").orEmpty(),
            visitVerified = fields.getFirestoreBoolean("visitVerified") ?: false
        )
    }

    protected fun parseVisitDocument(document: JsonObject): Visit? {
        val fields = document["fields"]?.jsonObject ?: return null
        val name = document["name"]?.jsonPrimitive?.contentOrNull ?: return null
        val visitId = name.substringAfterLast("/")
        val userId = fields.getFirestoreString("userId") ?: return null
        val cafeId = fields.getFirestoreString("cafeId") ?: return null
        return Visit(
            id = visitId,
            userId = userId,
            cafeId = cafeId,
            visitedAt = fields.getFirestoreString("visitedAt").orEmpty(),
            memo = fields.getFirestoreString("memo"),
            verified = fields.getFirestoreBoolean("verified") ?: false,
            checkInMethod = fields.getFirestoreString("checkInMethod")
        )
    }

    protected fun parseStampDocument(document: JsonObject): Stamp? {
        val name = document["name"]?.jsonPrimitive?.content ?: return null
        val fields = document["fields"]?.jsonObject ?: return null
        val stampId = name.substringAfterLast("/")
        val userId = fields.getFirestoreString("userId").orEmpty()
        val cafeId = fields.getFirestoreString("cafeId").orEmpty()
        val visitId = fields.getFirestoreString("visitId").orEmpty()
        val earnedAt = fields.getFirestoreString("earnedAt").orEmpty()
        return if (stampId.isBlank() || userId.isBlank() || cafeId.isBlank() || visitId.isBlank() || earnedAt.isBlank()) null
        else Stamp(id = stampId, userId = userId, cafeId = cafeId, visitId = visitId, earnedAt = earnedAt)
    }

    protected fun parseNoticeDocument(cafeId: String, cafeName: String, document: JsonObject): Notice? {
        val fields = document["fields"]?.jsonObject ?: return null
        val name = document["name"]?.jsonPrimitive?.contentOrNull ?: return null
        val noticeId = name.substringAfterLast("/")
        val createdAt = fields.getFirestoreString("createdAt").orEmpty()
        return Notice(
            id = noticeId,
            cafeId = cafeId,
            cafeName = cafeName,
            title = fields.getFirestoreString("title").orEmpty(),
            content = fields.getFirestoreString("content").orEmpty(),
            createdAt = createdAt,
            relativeTime = "최근"
        )
    }

    protected fun parseNoticeManagementDocument(cafeId: String, document: JsonObject): CafeNoticeManagementItem? {
        val fields = document["fields"]?.jsonObject ?: return null
        val name = document["name"]?.jsonPrimitive?.contentOrNull ?: return null
        val noticeId = name.substringAfterLast("/")
        val createdAt = fields.getFirestoreString("createdAt").orEmpty()
        val statusAccent = when (fields.getFirestoreString("statusAccent")?.uppercase()) {
            NoticeStatusAccent.DRAFT.name -> NoticeStatusAccent.DRAFT
            NoticeStatusAccent.ENDED.name -> NoticeStatusAccent.ENDED
            else -> NoticeStatusAccent.PUBLISHED
        }
        return CafeNoticeManagementItem(
            id = noticeId,
            cafeId = cafeId,
            title = fields.getFirestoreString("title").orEmpty(),
            content = fields.getFirestoreString("content").orEmpty(),
            createdAt = createdAt,
            displayDate = createdAt.take(10).replace("-", "."),
            isPinned = fields.getFirestoreBoolean("isPinned") ?: false,
            statusLabel = fields.getFirestoreString("statusLabel") ?: if (statusAccent == NoticeStatusAccent.DRAFT) "임시 저장" else "게시 중",
            statusAccent = statusAccent
        )
    }

    protected fun parseEventManagementDocument(cafeId: String, document: JsonObject): CafeEventManagementItem? {
        val fields = document["fields"]?.jsonObject ?: return null
        val name = document["name"]?.jsonPrimitive?.contentOrNull ?: return null
        val eventId = name.substringAfterLast("/")
        val startDate = fields.getFirestoreString("startDate").orEmpty()
        val endDate = fields.getFirestoreString("endDate") ?: startDate
        return CafeEventManagementItem(
            id = eventId,
            cafeId = cafeId,
            title = fields.getFirestoreString("title").orEmpty(),
            content = fields.getFirestoreString("content").orEmpty(),
            imageUrl = fields.getFirestoreString("imageUrl").orEmpty(),
            startDate = startDate,
            endDate = endDate,
            statusLabel = fields.getFirestoreString("statusLabel") ?: "진행 예정",
            isDimmed = fields.getFirestoreBoolean("isDimmed") ?: false,
            participantCastIds = fields.getFirestoreStringList("participantCastIds"),
            hasLivePerformance = fields.getFirestoreBoolean("hasLivePerformance") ?: false,
            likeCount = fields.getFirestoreInt("likeCount") ?: 0
        )
    }

    protected fun parseInquiryDocument(document: JsonObject): Inquiry? {
        val fields = document["fields"]?.jsonObject ?: return null
        val name = document["name"]?.jsonPrimitive?.contentOrNull ?: return null
        val inquiryId = name.substringAfterLast("/")
        val status = when (fields.getFirestoreString("status")?.uppercase()) {
            InquiryStatus.ANSWERED.name -> InquiryStatus.ANSWERED
            else -> InquiryStatus.PENDING
        }
        val createdAt = fields.getFirestoreString("createdAt").orEmpty()
        return Inquiry(
            id = inquiryId,
            userId = fields.getFirestoreString("userId").orEmpty(),
            userNickname = fields.getFirestoreString("userNickname").orEmpty(),
            inquiryType = fields.getFirestoreString("inquiryType").orEmpty(),
            title = fields.getFirestoreString("title").orEmpty(),
            content = fields.getFirestoreString("content").orEmpty(),
            status = status,
            createdAt = createdAt,
            createdAtLabel = fields.getFirestoreString("createdAtLabel") ?: "최근"
        )
    }

    protected fun parseReportDocument(document: JsonObject): Report? {
        val fields = document["fields"]?.jsonObject ?: return null
        val name = document["name"]?.jsonPrimitive?.contentOrNull ?: return null
        val reportId = name.substringAfterLast("/")
        val status = when (fields.getFirestoreString("status")?.uppercase()) {
            ReportStatus.RESOLVED.name -> ReportStatus.RESOLVED
            else -> ReportStatus.PENDING
        }
        val targetType = when (fields.getFirestoreString("targetType")?.uppercase()) {
            ReportTargetType.COMMUNITY_COMMENT.name -> ReportTargetType.COMMUNITY_COMMENT
            else -> ReportTargetType.COMMUNITY_POST
        }
        val createdAt = fields.getFirestoreString("createdAt").orEmpty()
        return Report(
            id = reportId,
            targetType = targetType,
            targetId = fields.getFirestoreString("targetId").orEmpty(),
            reporterUserId = fields.getFirestoreString("reporterUserId").orEmpty(),
            reporterNickname = fields.getFirestoreString("reporterNickname").orEmpty(),
            reportType = fields.getFirestoreString("reportType").orEmpty(),
            status = status,
            createdAt = createdAt,
            createdAtLabel = fields.getFirestoreString("createdAtLabel") ?: "최근"
        )
    }

    protected fun parseUserDocument(document: JsonObject): User? {
        val fields = document["fields"]?.jsonObject ?: return null
        val name = document["name"]?.jsonPrimitive?.contentOrNull ?: return null
        val userId = name.substringAfterLast("/")
        val email = fields.getFirestoreString("email") ?: return null
        val nickname = fields.getFirestoreString("nickname") ?: return null
        val role = fields.getFirestoreString("role")?.toUserRoleOrNull() ?: UserRole.VISITOR
        val createdAt = fields.getFirestoreString("createdAt") ?: "1970-01-01T00:00:00Z"
        val profileImage = fields.getFirestoreString("profileImage")
        val authProvider = fields.getFirestoreString("authProvider")
            ?.let { value -> runCatching { AuthProvider.valueOf(value) }.getOrDefault(AuthProvider.UNKNOWN) }
            ?: AuthProvider.UNKNOWN
        val phoneNumber = fields.getFirestoreString("phoneNumber")
            ?: fields.getFirestoreString("contactNumber")
            ?: fields.getFirestoreString("phone")
        val signupCompleted = fields.getFirestoreBoolean("signupCompleted") ?: true
        val banned = fields.getFirestoreBoolean("banned") ?: false
        return User(
            id = userId,
            email = email,
            nickname = nickname,
            profileImage = profileImage,
            authProvider = authProvider,
            role = role,
            banned = banned,
            createdAt = createdAt,
            phoneNumber = phoneNumber,
            signupCompleted = signupCompleted
        )
    }

    protected fun parseUserAffiliatedCafeId(document: JsonObject): String? {
        val fields = document["fields"]?.jsonObject ?: return null
        return fields.getFirestoreString("affiliatedCafeId")?.trim()?.takeIf { v -> v.isNotEmpty() }
    }

    protected fun parseHomeBannerDocument(document: JsonObject): HomeBanner? {
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

    protected fun parseMenuDocument(document: JsonObject): CafeMenu? {
        val fields = document["fields"]?.jsonObject ?: return null
        val name = document["name"]?.jsonPrimitive?.contentOrNull ?: return null
        val menuId = name.substringAfterLast("/")
        val category = fields.getFirestoreString("category")?.trim()?.takeIf { v -> v.isNotEmpty() } ?: "drink"
        val description = fields.getFirestoreString("desc") ?: fields.getFirestoreString("description") ?: ""
        return CafeMenu(
            id = menuId,
            name = fields.getFirestoreString("name").orEmpty(),
            price = (fields.getFirestoreLong("price") ?: 0L).toInt(),
            desc = description,
            image = fields.getFirestoreString("image") ?: fields.getFirestoreString("imageUrl") ?: fields.getFirestoreString("thumbnailImage"),
            category = category,
            isAvailable = fields.getFirestoreBoolean("isAvailable") ?: true
        )
    }

    protected fun parseGoodsDocument(document: JsonObject): Goods? {
        val fields = document["fields"]?.jsonObject ?: return null
        val name = document["name"]?.jsonPrimitive?.contentOrNull ?: return null
        val goodsId = name.substringAfterLast("/")
        return Goods(
            id = goodsId,
            name = fields.getFirestoreString("name").orEmpty(),
            price = (fields.getFirestoreLong("price") ?: 0L).toInt(),
            image = fields.getFirestoreString("image") ?: fields.getFirestoreString("imageUrl") ?: fields.getFirestoreString("thumbnailImage"),
            stock = (fields.getFirestoreLong("stock") ?: 0L).toInt()
        )
    }

    protected fun parseCafeDetailMetadata(document: JsonObject): CafeDetailMetadata {
        val fields = document["fields"]?.jsonObject
        val images = fields?.getFirestoreStringList("galleryImages")?.filter { img -> img.isNotBlank() }.orEmpty()
            .ifEmpty { fields?.getFirestoreStringList("images")?.filter { img -> img.isNotBlank() }.orEmpty() }
        val businessHours = (fields?.getFirestoreString("businessHours")
            ?: fields?.getFirestoreString("openingHours")
            ?: fields?.getFirestoreString("operatingHours"))?.trim()?.takeIf { v -> v.isNotEmpty() }
        val phoneNumber = (fields?.getFirestoreString("phoneNumber")
            ?: fields?.getFirestoreString("contactNumber")
            ?: fields?.getFirestoreString("phone"))?.trim()?.takeIf { v -> v.isNotEmpty() }
        return CafeDetailMetadata(images = images.ifEmpty { null }, businessHours = businessHours, phoneNumber = phoneNumber)
    }

    protected fun parsePendingCafeOwnerClaimDocument(document: JsonObject): CafeManagementData.PendingClaimSummary? {
        val fields = document["fields"]?.jsonObject ?: return null
        val name = document["name"]?.jsonPrimitive?.contentOrNull ?: return null
        val claimId = name.substringAfterLast("/")
        val cafeId = fields.getFirestoreString("cafeId").orEmpty()
        val resolvedCafeName = fields.getFirestoreString("cafeName")
        return CafeManagementData.PendingClaimSummary(
            claimId = claimId,
            cafeId = cafeId,
            cafeName = resolvedCafeName ?: "신청 카페",
            requestedAt = fields.getFirestoreString("requestedAt") ?: fields.getFirestoreString("createdAt") ?: "",
            status = fields.getFirestoreString("status") ?: DEFAULT_OWNER_CLAIM_STATUS,
            message = fields.getFirestoreString("message") ?: "관리자 승인 후 내 카페 목록에 자동 연결됩니다"
        )
    }

    protected fun parseCafeRegistrationClaimDocument(document: JsonObject): CafeRegistrationClaim? {
        val fields = document["fields"]?.jsonObject ?: return null
        val name = document["name"]?.jsonPrimitive?.contentOrNull ?: return null
        val claimId = name.substringAfterLast("/")
        val regionFields = fields.getFirestoreMap("region")
        val locationField = regionFields?.get("location")?.jsonObject?.get("geoPointValue")?.jsonObject
        return CafeRegistrationClaim(
            claimId = claimId,
            cafeName = fields.getFirestoreString("cafeName").orEmpty(),
            description = fields.getFirestoreString("description").orEmpty(),
            region = Region(
                country = regionFields?.getFirestoreString("country") ?: fields.getFirestoreString("country") ?: "KR",
                city = regionFields?.getFirestoreString("city") ?: fields.getFirestoreString("city") ?: "",
                address = regionFields?.getFirestoreString("address") ?: fields.getFirestoreString("address") ?: "",
                location = GeoPoint(
                    latitude = locationField?.get("latitude")?.jsonPrimitive?.doubleOrNull ?: 0.0,
                    longitude = locationField?.get("longitude")?.jsonPrimitive?.doubleOrNull ?: 0.0
                )
            ),
            thumbnailImage = fields.getFirestoreString("thumbnailImage"),
            conceptType = fields.getFirestoreString("conceptType") ?: "MAID",
            businessHours = fields.getFirestoreString("businessHours").orEmpty(),
            phoneNumber = fields.getFirestoreString("phoneNumber").orEmpty(),
            requestedAt = fields.getFirestoreString("requestedAt") ?: fields.getFirestoreString("createdAt") ?: "",
            status = fields.getFirestoreString("status") ?: DEFAULT_REGISTRATION_CLAIM_STATUS,
            message = fields.getFirestoreString("message") ?: "관리자 승인 후 새 카페가 생성되고 운영 카페에 자동 연결됩니다."
        )
    }

    protected suspend fun fetchCafeDetailSummaryRemoteInternal(cafeId: String): CafeDetail? {
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        val cafeDocument = runCatching { loadCafeDocument(cafeId = cafeId, idToken = idToken) }
            .recoverCatching { fallbackError ->
                loadCafeDocument(cafeId = cafeId, idToken = null)
            }
            .getOrElse { throwable ->
                return null
            }
        val cafe = parseCafeDocument(cafeDocument) ?: return null
        val detailMetadata = parseCafeDetailMetadata(cafeDocument)
        val castsDocuments = runCatching { loadCafeSubCollectionDocuments(cafeId, FirestorePaths.CAFE_CASTS, idToken) }
            .recoverCatching { loadCafeSubCollectionDocuments(cafeId, FirestorePaths.CAFE_CASTS, null) }
            .getOrElse { emptyList() }
        val parsedCasts = castsDocuments
            .mapNotNull { document -> parseCastDocument(cafeId = cafeId, document = document) }
            .sortedBy { cast -> cast.name }
        return CafeDetail(
            cafe = cafe,
            images = detailMetadata.images ?: listOfNotNull(cafe.thumbnailImage),
            casts = parsedCasts,
            menus = emptyList(),
            goods = emptyList(),
            notices = emptyList(),
            businessHours = detailMetadata.businessHours ?: "운영시간 정보 준비중",
            phoneNumber = detailMetadata.phoneNumber ?: "연락처 정보 준비중"
        )
    }

    protected suspend fun fetchCafeDetailFullRemoteInternal(cafeId: String): CafeDetail? {
        val summary = fetchCafeDetailSummaryRemoteInternal(cafeId) ?: return null
        val menuGoods = fetchCafeMenuGoodsRemoteInternal(cafeId)
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        val noticesDocuments = runCatching { loadCafeSubCollectionDocuments(cafeId, FirestorePaths.CAFE_NOTICES, idToken) }
            .recoverCatching { loadCafeSubCollectionDocuments(cafeId, FirestorePaths.CAFE_NOTICES, null) }
            .getOrElse { emptyList() }
        val parsedNotices = noticesDocuments
            .mapNotNull { document -> parseNoticeDocument(cafeId = cafeId, cafeName = summary.cafe.name, document = document) }
            .sortedByDescending { notice -> notice.createdAt }
        return summary.copy(menus = menuGoods?.menus.orEmpty(), goods = menuGoods?.goods.orEmpty(), notices = parsedNotices)
    }

    protected suspend fun fetchCafeMenuGoodsRemoteInternal(cafeId: String): CafeMenuGoodsSection? {
        fetchCafeByIdRemoteInternal(cafeId) ?: return null
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        val menusDocuments = runCatching { loadCafeSubCollectionDocuments(cafeId, FirestorePaths.CAFE_MENUS, idToken) }
            .recoverCatching { fallbackError ->
                loadCafeSubCollectionDocuments(cafeId, FirestorePaths.CAFE_MENUS, null)
            }
            .getOrElse { throwable ->
                emptyList()
            }
        val goodsDocuments = runCatching { loadCafeSubCollectionDocuments(cafeId, FirestorePaths.CAFE_GOODS, idToken) }
            .recoverCatching { fallbackError ->
                loadCafeSubCollectionDocuments(cafeId, FirestorePaths.CAFE_GOODS, null)
            }
            .getOrElse { throwable ->
                emptyList()
            }
        val parsedMenus = menusDocuments.mapNotNull { document -> parseMenuDocument(document) }.sortedBy { menu -> menu.name.lowercase() }
        val parsedGoods = goodsDocuments.mapNotNull { document -> parseGoodsDocument(document) }.sortedBy { goods -> goods.name.lowercase() }
        return CafeMenuGoodsSection(menus = parsedMenus, goods = parsedGoods)
    }

    protected suspend fun fetchCafeByIdRemoteInternal(cafeId: String): Cafe? {
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        val cafeDocument = runCatching { loadCafeDocument(cafeId = cafeId, idToken = idToken) }
            .recoverCatching { fallbackError ->
                loadCafeDocument(cafeId = cafeId, idToken = null)
            }
            .getOrElse { throwable ->
                return null
            }
        return parseCafeDocument(cafeDocument)
    }

    protected suspend fun fetchCastDetailRemoteByCafeAndCastId(
        cafeId: String,
        castId: String,
        idToken: String?
    ): CastDetail? {
        val cafeDocument = runCatching { loadCafeDocument(cafeId = cafeId, idToken = idToken) }
            .recoverCatching { loadCafeDocument(cafeId = cafeId, idToken = null) }
            .getOrNull() ?: return null
        val castDocument = runCatching { loadCafeCastDocument(cafeId = cafeId, castId = castId, idToken = idToken) }
            .recoverCatching { loadCafeCastDocument(cafeId = cafeId, castId = castId, idToken = null) }
            .getOrNull() ?: return null
        val cafe = parseCafeDocument(cafeDocument) ?: return null
        val cast = parseCastDocument(cafeId = cafeId, document = castDocument) ?: return null
        val castFields = castDocument["fields"]?.jsonObject
        val galleryImages = castFields?.getFirestoreStringList("galleryImages")?.filter { img -> img.isNotBlank() }.orEmpty()
        val images = buildList {
            cast.profileImage?.takeIf { img -> img.isNotBlank() }?.let { img -> add(img) }
            addAll(galleryImages.filterNot { img -> img == cast.profileImage })
        }
        val currentDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val fromDate = (currentDate + DatePeriod(days = -30)).toString()
        val toDate = (currentDate + DatePeriod(days = 60)).toString()
        val schedules = fetchCastSchedulesRemoteInternal(castId = castId, fromDate = fromDate, toDate = toDate, idToken = idToken)
        return CastDetail(cast = cast, cafe = cafe, images = images, schedule = schedules, visitCertificationCount = cast.visitCertificationCount)
    }

    // ── Community post helpers ────────────────────────────────────────────────

    protected fun JsonObject.toCommunityPostQueryCursor(): String? {
        val fields = this["fields"]?.jsonObject ?: return null
        val createdAt = fields.getFirestoreString("createdAt") ?: return null
        val documentName = this["name"]?.jsonPrimitive?.contentOrNull ?: return null
        return "$createdAt$COMMUNITY_POST_CURSOR_SEPARATOR$documentName"
    }

    protected fun String?.toCommunityPostStartAfterSection(): String {
        val cursorValue = this
        if (cursorValue.isNullOrBlank()) return ""
        val separatorIndex = cursorValue.indexOf(COMMUNITY_POST_CURSOR_SEPARATOR)
        val createdAt = if (separatorIndex >= 0) cursorValue.substring(0, separatorIndex) else return ""
        val documentName = if (separatorIndex >= 0) cursorValue.substring(separatorIndex + 1) else return ""
        if (createdAt.isBlank() || documentName.isBlank()) return ""
        val escapedCreatedAt = escapeFirestoreQueryString(createdAt)
        val escapedDocumentName = escapeFirestoreQueryString(documentName)
        return """,
                "startAt": {
                  "values": [
                    { "stringValue": "$escapedCreatedAt" },
                    { "referenceValue": "$escapedDocumentName" }
                  ],
                  "before": false
                }"""
    }

    protected suspend fun runCommunityPostPageQuery(
        cursor: String?,
        limit: Int,
        idToken: String?
    ): List<JsonObject> {
        val safeLimit = if (limit > 0) limit else 1
        val startAfterSection = cursor.toCommunityPostStartAfterSection()
        val path = "${config.documentBasePath()}:runQuery"
        val body = """
            {
              "structuredQuery": {
                "from": [{"collectionId": "${FirestorePaths.COMMUNITY_POSTS}"}],
                "orderBy": [
                  {"field": {"fieldPath": "createdAt"},"direction": "DESCENDING"},
                  {"field": {"fieldPath": "__name__"},"direction": "DESCENDING"}
                ]$startAfterSection,
                "limit": $safeLimit
              }
            }
        """.trimIndent()
        val response = restApi.post(path = path, body = body, idToken = idToken)
        val parsed = Json.parseToJsonElement(response).jsonArray
        return parsed.mapNotNull { element -> element.jsonObject["document"]?.jsonObject }
    }

    protected fun parseCommunityPostDocument(document: JsonObject): com.hhp227.concafe.domain.model.CommunityPost? {
        val documentName = document["name"]?.jsonPrimitive?.contentOrNull ?: return null
        val fields = document["fields"]?.jsonObject ?: return null
        val postId = documentName.substringAfterLast("/").takeIf { it.isNotBlank() } ?: return null
        val userId = fields.getFirestoreString("userId") ?: return null
        val title = fields.getFirestoreString("title") ?: return null
        val content = fields.getFirestoreString("content") ?: return null
        val userNickname = fields.getFirestoreString("userNickname").orEmpty()
        val imageUrls = fields.getFirestoreStringList("imageUrls")
        val likeCount = fields.getFirestoreInt("likeCount") ?: 0
        val commentCount = fields.getFirestoreInt("commentCount") ?: 0
        val viewCount = fields.getFirestoreInt("viewCount") ?: 0
        val createdAt = fields.getFirestoreString("createdAt").orEmpty()
        val displayDate = createdAt.take(10).replace("-", ".")
        return com.hhp227.concafe.domain.model.CommunityPost(
            id = postId,
            userId = userId,
            userNickname = userNickname,
            title = title,
            content = content,
            imageUrls = imageUrls,
            likeCount = likeCount,
            commentCount = commentCount,
            viewCount = viewCount,
            createdAt = createdAt,
            displayDate = displayDate
        )
    }

    companion object {
        val FOLLOWED_CAST_ID_FIELD_CANDIDATES = listOf("followedCastIds", "followingCastIds", "followCastIds")
        val RECENT_VISIT_CAFE_ID_FIELD_CANDIDATES = listOf("recentVisitCafeIds", "recentVisitedCafeIds", "visitedCafeIds")
        const val DEFAULT_OWNER_CLAIM_STATUS = "승인 대기 중"
        const val DEFAULT_REGISTRATION_CLAIM_STATUS = "승인 대기 중"
        const val RECENT_FOLLOWER_FETCH_LIMIT = 30
        const val BIRTHDAY_KEY_FIELD = "birthdayKey"
        const val MAX_FIRESTORE_IN_FILTER_VALUES = 10
        const val REGION_FILTER_CAFE_CACHE_LIMIT = 200
        const val CAST_CURSOR_NULL_MARKER = "__NULL__"
        const val COMMUNITY_POST_CURSOR_SEPARATOR = "|"
        val FAVORITE_CAFE_ID_FIELD_CANDIDATES = listOf("favoriteCafeIds", "favorites", "favoriteCafes")
    }
}

// ── Internal data holders used across subclasses ──────────────────────────────

data class CafeDetailMetadata(
    val images: List<String>?,
    val businessHours: String?,
    val phoneNumber: String?
)

data class CastScheduleDocumentEntry(
    val id: String,
    val castId: String,
    val date: String,
    val status: CastScheduleStatus,
    val startTime: String?,
    val endTime: String?
)
