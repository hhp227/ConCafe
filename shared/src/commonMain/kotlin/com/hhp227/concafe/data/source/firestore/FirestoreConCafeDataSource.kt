package com.hhp227.concafe.data.source.firestore

import com.hhp227.concafe.data.source.AuthDataSource
import com.hhp227.concafe.data.source.BannerDataSource
import com.hhp227.concafe.data.source.CafeDataSource
import com.hhp227.concafe.data.source.CastClaimDataSource
import com.hhp227.concafe.data.source.CastDataSource
import com.hhp227.concafe.data.source.ExternalLinkDataSource
import com.hhp227.concafe.data.source.FirestoreCacheDataSource
import com.hhp227.concafe.data.source.InquiryDataSource
import com.hhp227.concafe.data.source.MyInfoDataSource
import com.hhp227.concafe.data.source.NoticeDataSource
import com.hhp227.concafe.data.source.NotificationDataSource
import com.hhp227.concafe.data.source.PagingDataSource
import com.hhp227.concafe.data.source.RankingDataSource
import com.hhp227.concafe.data.source.ReviewDataSource
import com.hhp227.concafe.data.source.ScheduleStatusDataSource
import com.hhp227.concafe.data.source.SocialDataSource
import com.hhp227.concafe.data.source.StampDataSource
import com.hhp227.concafe.data.source.VisitDataSource
import com.hhp227.concafe.domain.model.BannerLinkTargetType
import com.hhp227.concafe.domain.model.Cafe
import com.hhp227.concafe.domain.model.CafeRegistrationClaim
import com.hhp227.concafe.domain.model.Cast
import com.hhp227.concafe.domain.model.GeoPoint
import com.hhp227.concafe.domain.model.HomeBanner
import com.hhp227.concafe.domain.model.MyPageSummary
import com.hhp227.concafe.domain.model.Notice
import com.hhp227.concafe.domain.model.CafeManagementData
import com.hhp227.concafe.domain.model.PendingCafeOwnerClaimPreview
import com.hhp227.concafe.domain.model.PendingCafeRegistrationClaimPreview
import com.hhp227.concafe.domain.model.Region
import com.hhp227.concafe.domain.model.User
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.model.Visit
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
    private val delegate: FirestoreCacheDataSource = FirestoreCacheDataSource()
) :
    AuthDataSource by delegate,
    CafeDataSource by delegate,
    CastDataSource by delegate,
    CastClaimDataSource by delegate,
    BannerDataSource by delegate,
    InquiryDataSource by delegate,
    NoticeDataSource by delegate,
    ReviewDataSource by delegate,
    VisitDataSource by delegate,
    ExternalLinkDataSource by delegate,
    StampDataSource by delegate,
    ScheduleStatusDataSource by delegate,
    NotificationDataSource by delegate,
    SocialDataSource by delegate,
    MyInfoDataSource by delegate,
    RankingDataSource by delegate,
    PagingDataSource by delegate,
    FirestoreSyncDataSource {
    suspend fun bootstrap() {
        val idToken = tokenProvider.getIdToken()
        clearHomeFeedCollections()
        runCatching { loadUsers(idToken) }
        runCatching { loadHomeBanners(idToken) }
        runCatching { loadCafes(idToken) }
        runCatching { loadCasts(idToken) }
        runCatching { loadNotices(idToken) }
        runCatching { loadVisits(idToken) }
    }

    override suspend fun fetchUser(userId: String): User? {
        val idToken = tokenProvider.getIdToken()
        val path = "${config.documentBasePath()}/${FirestorePaths.USERS}/$userId"
        return runCatching {
            val response = restApi.get(path, idToken)
            val parsed = Json.parseToJsonElement(response).jsonObject
            parseUserDocument(parsed)
        }.getOrNull()
    }

    override suspend fun fetchMyPageSummary(userId: String): MyPageSummary? {
        val idToken = tokenProvider.getIdToken()
        val path = "${config.documentBasePath()}/${FirestorePaths.USERS}/$userId"
        return runCatching {
            val response = restApi.get(path, idToken)
            val parsed = Json.parseToJsonElement(response).jsonObject
            parseMyPageSummaryDocument(userId = userId, document = parsed)
        }.getOrNull()
    }

    override suspend fun pushUser(user: User) {
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

    override suspend fun deleteUser(userId: String) {
        val idToken = tokenProvider.getIdToken()
        val path = "${config.documentBasePath()}/${FirestorePaths.USERS}/$userId"
        restApi.delete(path, idToken)
    }

    override suspend fun pushCafeRegistrationClaim(
        requesterUserId: String,
        claim: CafeRegistrationClaim
    ) {
        val idToken = tokenProvider.getIdToken()
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFE_REGISTRATION_CLAIMS}/${claim.claimId}"
        val body = firestoreDocumentBody(
            mapOf(
                "userId" to firestoreString(requesterUserId),
                "cafeName" to firestoreString(claim.cafeName),
                "description" to firestoreString(claim.description),
                "thumbnailImage" to firestoreNullableString(claim.thumbnailImage),
                "conceptType" to firestoreString(claim.conceptType),
                "businessHours" to firestoreString(claim.businessHours),
                "phoneNumber" to firestoreString(claim.phoneNumber),
                "requestedAt" to firestoreString(claim.requestedAt),
                "status" to firestoreString(claim.status),
                "message" to firestoreString(claim.message),
                "region" to firestoreMap(
                    mapOf(
                        "country" to firestoreString(claim.region.country),
                        "city" to firestoreString(claim.region.city),
                        "address" to firestoreString(claim.region.address),
                        "location" to firestoreGeoPoint(
                            latitude = claim.region.location.latitude,
                            longitude = claim.region.location.longitude
                        )
                    )
                )
            )
        )
        restApi.patch(path, body, idToken)
    }

    override suspend fun pushHomeBanner(banner: HomeBanner) {
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

    override suspend fun deleteHomeBanner(bannerId: String) {
        val idToken = tokenProvider.getIdToken()
        val path = "${config.documentBasePath()}/${FirestorePaths.HOME_BANNERS}/$bannerId"

        restApi.delete(path, idToken)
    }

    override suspend fun refreshCafeManagementData(userId: String) {
        val idToken = tokenProvider.getIdToken()
        val ownerClaimDocuments = runUserScopedQuery(
            collectionId = FirestorePaths.CAFE_OWNER_CLAIMS,
            userId = userId,
            idToken = idToken
        )
        val registrationClaimDocuments = runUserScopedQuery(
            collectionId = FirestorePaths.CAFE_REGISTRATION_CLAIMS,
            userId = userId,
            idToken = idToken
        )
        val ownerClaims = ownerClaimDocuments.mapNotNull { document ->
            parsePendingCafeOwnerClaimDocument(document)
        }
        val registrationClaims = registrationClaimDocuments.mapNotNull { document ->
            parseCafeRegistrationClaimDocument(document)
        }
        val ownedCafeIds = mutableSetOf<String>()

        ownerClaims.forEach { claim ->
            val isApproved = claim.status.isApprovedClaimStatus()

            if (isApproved) {
                ownedCafeIds.add(claim.cafeId)
            }
        }
        registrationClaimDocuments.forEach { document ->
            val fields = document["fields"]?.jsonObject

            if (fields != null) {
                val status = fields.getFirestoreString("status").orEmpty()
                val approvedCafeId = fields.getFirestoreString("approvedCafeId")
                    ?: fields.getFirestoreString("cafeId")
                val isApproved = status.isApprovedClaimStatus()

                if (isApproved && !approvedCafeId.isNullOrBlank()) {
                    ownedCafeIds.add(approvedCafeId)
                }
            }
        }
        pendingCafeClaimsByUser[userId] = ownerClaims
            .sortedByDescending { it.requestedAt }
            .toMutableList()
        pendingCafeRegistrationClaimsByUser[userId] = registrationClaims
            .sortedByDescending { it.requestedAt }
            .toMutableList()
        ownedCafeIdsByUser[userId] = ownedCafeIds.toMutableList()
    }

    override suspend fun fetchPendingCafeOwnerClaimsForAdmin(): List<PendingCafeOwnerClaimPreview> {
        val idToken = tokenProvider.getIdToken()
        val documents = loadCollectionDocuments(
            collectionId = FirestorePaths.CAFE_OWNER_CLAIMS,
            idToken = idToken
        )
        val previews = mutableListOf<PendingCafeOwnerClaimPreview>()

        for (document in documents) {
            val preview = parsePendingCafeOwnerClaimPreviewForAdmin(document)

            if (preview != null) {
                previews.add(preview)
            }
        }
        return previews.sortedByDescending { it.requestedAt }
    }

    override suspend fun fetchPendingCafeRegistrationClaimsForAdmin(): List<PendingCafeRegistrationClaimPreview> {
        val idToken = tokenProvider.getIdToken()
        val documents = loadCollectionDocuments(
            collectionId = FirestorePaths.CAFE_REGISTRATION_CLAIMS,
            idToken = idToken
        )
        val previews = mutableListOf<PendingCafeRegistrationClaimPreview>()

        for (document in documents) {
            val preview = parsePendingCafeRegistrationClaimPreviewForAdmin(document)

            if (preview != null) {
                previews.add(preview)
            }
        }
        return previews.sortedByDescending { it.requestedAt }
    }

    private suspend fun loadUsers(idToken: String?) {
        val response = restApi.get("${config.documentBasePath()}/${FirestorePaths.USERS}", idToken)
        val parsed = Json.parseToJsonElement(response).jsonObject
        val documents = parsed["documents"]?.jsonArray.orEmpty()
        val users = documents.mapNotNull { element ->
            parseUserDocument(element.jsonObject)
        }

        replaceAllUsers(users)
    }

    private suspend fun loadHomeBanners(idToken: String?) {
        val response = restApi.get("${config.documentBasePath()}/${FirestorePaths.HOME_BANNERS}", idToken)
        val parsed = Json.parseToJsonElement(response).jsonObject
        val documents = parsed["documents"]?.jsonArray.orEmpty()
        val banners = documents.mapNotNull { element ->
            parseHomeBannerDocument(element.jsonObject)
        }

        this.banners.clear()
        this.banners.addAll(banners)
    }

    private suspend fun loadCafes(idToken: String?) {
        val response = restApi.get("${config.documentBasePath()}/${FirestorePaths.CAFES}", idToken)
        val parsed = Json.parseToJsonElement(response).jsonObject
        val documents = parsed["documents"]?.jsonArray.orEmpty()
        val cafes = documents.mapNotNull { element ->
            parseCafeDocument(element.jsonObject)
        }

        this.cafes.clear()
        this.cafes.addAll(cafes)
    }

    private suspend fun loadCasts(idToken: String?) {
        val loadedCasts = mutableListOf<Cast>()

        cafes.forEach { cafe ->
            val path = "${config.documentBasePath()}/${FirestorePaths.CAFES}/${cafe.id}/${FirestorePaths.CAFE_CASTS}"
            val response = restApi.get(path, idToken)
            val parsed = Json.parseToJsonElement(response).jsonObject
            val documents = parsed["documents"]?.jsonArray.orEmpty()
            val casts = documents.mapNotNull { element ->
                parseCastDocument(cafe.id, element.jsonObject)
            }

            loadedCasts.addAll(casts)
        }
        delegate.casts.clear()
        delegate.casts.addAll(loadedCasts)
    }

    private suspend fun loadNotices(idToken: String?) {
        val loadedNotices = mutableListOf<Notice>()
        val cafeNameById = cafes.associate { it.id to it.name }

        cafes.forEach { cafe ->
            val path = "${config.documentBasePath()}/${FirestorePaths.CAFES}/${cafe.id}/${FirestorePaths.CAFE_NOTICES}"
            val response = restApi.get(path, idToken)
            val parsed = Json.parseToJsonElement(response).jsonObject
            val documents = parsed["documents"]?.jsonArray.orEmpty()
            val notices = documents.mapNotNull { element ->
                parseNoticeDocument(
                    cafeId = cafe.id,
                    cafeName = cafeNameById[cafe.id].orEmpty(),
                    document = element.jsonObject
                )
            }

            loadedNotices.addAll(notices)
        }
        loadedNotices.sortByDescending { it.createdAt }
        this.notices.clear()
        this.notices.addAll(loadedNotices)
    }

    private suspend fun loadVisits(idToken: String?) {
        val response = restApi.get("${config.documentBasePath()}/${FirestorePaths.VISITS}", idToken)
        val parsed = Json.parseToJsonElement(response).jsonObject
        val documents = parsed["documents"]?.jsonArray.orEmpty()
        val visits = documents.mapNotNull { element ->
            parseVisitDocument(element.jsonObject)
        }

        this.visits.clear()
        this.visits.addAll(visits.sortedByDescending { it.visitedAt })
    }

    private suspend fun runUserScopedQuery(
        collectionId: String,
        userId: String,
        idToken: String?
    ): List<JsonObject> {
        val path = "${config.documentBasePath()}:runQuery"
        val body = """
            {
              "structuredQuery": {
                "from": [
                  { "collectionId": "$collectionId" }
                ],
                "where": {
                    "fieldFilter": {
                      "field": { "fieldPath": "userId" },
                      "op": "EQUAL",
                      "value": { "stringValue": "${escapeFirestoreQueryString(userId)}" }
                    }
                  }
                }
            }
        """.trimIndent()
        val response = restApi.post(path = path, body = body, idToken = idToken)
        val parsed = Json.parseToJsonElement(response).jsonArray

        return parsed.mapNotNull { element ->
            element.jsonObject["document"]?.jsonObject
        }
    }

    private suspend fun loadCollectionDocuments(
        collectionId: String,
        idToken: String?
    ): List<JsonObject> {
        val response = restApi.get("${config.documentBasePath()}/$collectionId", idToken)
        val parsed = Json.parseToJsonElement(response).jsonObject
        return parsed["documents"]?.jsonArray.orEmpty().map { element ->
            element.jsonObject
        }
    }

    private fun clearHomeFeedCollections() {
        this.cafes.clear()
        delegate.casts.clear()
        this.notices.clear()
        this.visits.clear()
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

    private fun parseCafeDocument(document: JsonObject): Cafe? {
        val fields = document["fields"]?.jsonObject ?: return null
        val name = document["name"]?.jsonPrimitive?.contentOrNull ?: return null
        val cafeId = name.substringAfterLast("/")
        val regionField = fields["region"]?.jsonObject?.get("mapValue")?.jsonObject?.get("fields")?.jsonObject
        val locationField = regionField
            ?.get("location")
            ?.jsonObject
            ?.get("geoPointValue")
            ?.jsonObject
        return Cafe(
            id = cafeId,
            name = fields.getFirestoreString("name").orEmpty(),
            desc = fields.getFirestoreString("desc").orEmpty(),
            region = Region(
                country = regionField?.getFirestoreString("country")
                    ?: fields.getFirestoreString("country")
                    ?: "KR",
                city = regionField?.getFirestoreString("city")
                    ?: fields.getFirestoreString("city")
                    ?: "",
                address = regionField?.getFirestoreString("address")
                    ?: fields.getFirestoreString("address")
                    ?: "",
                location = GeoPoint(
                    latitude = locationField?.get("latitude")?.jsonPrimitive?.doubleOrNull ?: 0.0,
                    longitude = locationField?.get("longitude")?.jsonPrimitive?.doubleOrNull ?: 0.0
                )
            ),
            thumbnailImage = fields.getFirestoreString("thumbnailImage"),
            ratingAvg = fields.getFirestoreDouble("ratingAvg")
                ?: fields.getFirestoreLong("ratingAvg")?.toDouble()
                ?: 0.0,
            reviewCount = fields.getFirestoreLong("reviewCount")?.toInt() ?: 0,
            approved = fields.getFirestoreBoolean("approved") ?: true,
            conceptType = fields.getFirestoreString("conceptType") ?: "MAID"
        )
    }

    private fun parseCastDocument(cafeId: String, document: JsonObject): Cast? {
        val fields = document["fields"]?.jsonObject ?: return null
        val name = document["name"]?.jsonPrimitive?.contentOrNull ?: return null
        val castId = name.substringAfterLast("/")
        return Cast(
            id = castId,
            cafeId = cafeId,
            name = fields.getFirestoreString("name").orEmpty(),
            linkedUserId = fields.getFirestoreString("linkedUserId"),
            profileImage = fields.getFirestoreString("profileImage"),
            desc = fields.getFirestoreString("desc").orEmpty(),
            birthday = fields.getFirestoreString("birthday"),
            conceptRole = fields.getFirestoreString("conceptRole") ?: "maid",
            followerCount = fields.getFirestoreLong("followerCount")?.toInt() ?: 0,
            rating = fields.getFirestoreDouble("rating")
                ?: fields.getFirestoreLong("rating")?.toDouble()
                ?: 0.0
        )
    }

    private fun parseNoticeDocument(cafeId: String, cafeName: String, document: JsonObject): Notice? {
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

    private fun parseVisitDocument(document: JsonObject): Visit? {
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
            verified = fields.getFirestoreBoolean("verified") ?: false
        )
    }

    private fun parsePendingCafeOwnerClaimDocument(document: JsonObject): CafeManagementData.PendingClaimSummary? {
        val fields = document["fields"]?.jsonObject ?: return null
        val name = document["name"]?.jsonPrimitive?.contentOrNull ?: return null
        val claimId = name.substringAfterLast("/")
        val cafeId = fields.getFirestoreString("cafeId").orEmpty()
        val resolvedCafeName = if (fields.getFirestoreString("cafeName").isNullOrBlank()) {
            cafes.firstOrNull { it.id == cafeId }?.name
        } else {
            fields.getFirestoreString("cafeName")
        }
        return CafeManagementData.PendingClaimSummary(
            claimId = claimId,
            cafeId = cafeId,
            cafeName = resolvedCafeName ?: "신청 카페",
            requestedAt = fields.getFirestoreString("requestedAt")
                ?: fields.getFirestoreString("createdAt")
                ?: "",
            status = fields.getFirestoreString("status") ?: DEFAULT_OWNER_CLAIM_STATUS,
            message = fields.getFirestoreString("message")
                ?: "관리자 승인 후 내 카페 목록에 자동 연결됩니다"
        )
    }

    private fun parseCafeRegistrationClaimDocument(document: JsonObject): CafeRegistrationClaim? {
        val fields = document["fields"]?.jsonObject ?: return null
        val name = document["name"]?.jsonPrimitive?.contentOrNull ?: return null
        val claimId = name.substringAfterLast("/")
        val regionFields = fields.getFirestoreMap("region")
        val locationField = regionFields
            ?.get("location")
            ?.jsonObject
            ?.get("geoPointValue")
            ?.jsonObject
        return CafeRegistrationClaim(
            claimId = claimId,
            cafeName = fields.getFirestoreString("cafeName").orEmpty(),
            description = fields.getFirestoreString("description").orEmpty(),
            region = Region(
                country = regionFields?.getFirestoreString("country")
                    ?: fields.getFirestoreString("country")
                    ?: "KR",
                city = regionFields?.getFirestoreString("city")
                    ?: fields.getFirestoreString("city")
                    ?: "",
                address = regionFields?.getFirestoreString("address")
                    ?: fields.getFirestoreString("address")
                    ?: "",
                location = GeoPoint(
                    latitude = locationField?.get("latitude")?.jsonPrimitive?.doubleOrNull ?: 0.0,
                    longitude = locationField?.get("longitude")?.jsonPrimitive?.doubleOrNull ?: 0.0
                )
            ),
            thumbnailImage = fields.getFirestoreString("thumbnailImage"),
            conceptType = fields.getFirestoreString("conceptType") ?: "MAID",
            businessHours = fields.getFirestoreString("businessHours").orEmpty(),
            phoneNumber = fields.getFirestoreString("phoneNumber").orEmpty(),
            requestedAt = fields.getFirestoreString("requestedAt")
                ?: fields.getFirestoreString("createdAt")
                ?: "",
            status = fields.getFirestoreString("status") ?: DEFAULT_REGISTRATION_CLAIM_STATUS,
            message = fields.getFirestoreString("message")
                ?: "관리자 승인 후 새 카페가 생성되고 운영 카페에 자동 연결됩니다."
        )
    }

    private suspend fun parsePendingCafeOwnerClaimPreviewForAdmin(
        document: JsonObject
    ): PendingCafeOwnerClaimPreview? {
        val fields = document["fields"]?.jsonObject ?: return null
        val status = fields.getFirestoreString("status").orEmpty()

        if (!status.isPendingClaimStatus()) {
            return null
        }
        val name = document["name"]?.jsonPrimitive?.contentOrNull ?: return null
        val claimId = name.substringAfterLast("/")
        val requesterUserId = fields.getFirestoreString("userId") ?: return null
        val cafeId = fields.getFirestoreString("cafeId").orEmpty()
        val cafe = cafes.firstOrNull { it.id == cafeId }
        val requesterNickname = resolveUserNickname(requesterUserId)
        val cafeName = fields.getFirestoreString("cafeName")
            ?: cafe?.name
            ?: "신청 카페"
        val location = fields.getFirestoreString("location")
            ?: listOfNotNull(cafe?.region?.city, cafe?.region?.address).joinToString(" ").ifBlank { "위치 정보 없음" }
        val requestedAt = fields.getFirestoreString("requestedAt")
            ?: fields.getFirestoreString("createdAt")
            ?: ""
        val message = fields.getFirestoreString("message")
            ?: "관리자 승인 후 내 카페 목록에 자동 연결됩니다"
        val imageUrl = fields.getFirestoreString("imageUrl")
            ?: cafe?.thumbnailImage
        return PendingCafeOwnerClaimPreview(
            claimId = claimId,
            requesterUserId = requesterUserId,
            requesterNickname = requesterNickname,
            cafeId = cafeId,
            cafeName = cafeName,
            location = location,
            requestedAt = requestedAt,
            message = message,
            imageUrl = imageUrl
        )
    }

    private suspend fun parsePendingCafeRegistrationClaimPreviewForAdmin(
        document: JsonObject
    ): PendingCafeRegistrationClaimPreview? {
        val fields = document["fields"]?.jsonObject ?: return null
        val status = fields.getFirestoreString("status").orEmpty()
        if (!status.isPendingClaimStatus()) {
            return null
        }
        val name = document["name"]?.jsonPrimitive?.contentOrNull ?: return null
        val claimId = name.substringAfterLast("/")
        val requesterUserId = fields.getFirestoreString("userId") ?: return null
        val requesterNickname = resolveUserNickname(requesterUserId)
        val regionFields = fields.getFirestoreMap("region")
        val city = regionFields?.getFirestoreString("city")
            ?: fields.getFirestoreString("city")
            ?: ""
        val address = regionFields?.getFirestoreString("address")
            ?: fields.getFirestoreString("address")
            ?: ""
        val location = listOf(city, address).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "위치 정보 없음" }
        val requestedAt = fields.getFirestoreString("requestedAt")
            ?: fields.getFirestoreString("createdAt")
            ?: ""
        val message = fields.getFirestoreString("message")
            ?: "관리자 승인 후 새 카페가 생성되고 운영 카페에 자동 연결됩니다."
        return PendingCafeRegistrationClaimPreview(
            claimId = claimId,
            requesterUserId = requesterUserId,
            requesterNickname = requesterNickname,
            cafeName = fields.getFirestoreString("cafeName").orEmpty(),
            location = location,
            requestedAt = requestedAt,
            message = message,
            imageUrl = fields.getFirestoreString("thumbnailImage")
        )
    }

    private suspend fun resolveUserNickname(userId: String): String {
        val localUser = findUserById(userId)

        if (localUser != null) {
            return localUser.nickname
        }
        val remoteUser = fetchUser(userId)

        if (remoteUser != null) {
            return remoteUser.nickname
        }
        return "알 수 없음"
    }

    private fun parseMyPageSummaryDocument(userId: String, document: JsonObject): MyPageSummary {
        val fields = document["fields"]?.jsonObject
        val statsField = fields?.getFirestoreMap("stats")
        val visitCount = statsField?.getFirestoreInt("visitCount")
            ?: fields?.getFirestoreInt("visitCount")
            ?: 0
        val favoritesCount = statsField?.getFirestoreInt("favoritesCount")
            ?: statsField?.getFirestoreInt("favoriteCount")
            ?: fields?.getFirestoreInt("favoritesCount")
            ?: 0
        val followedCastsCount = statsField?.getFirestoreInt("followedCastsCount")
            ?: statsField?.getFirestoreInt("followedCount")
            ?: fields?.getFirestoreInt("followedCastsCount")
            ?: 0
        val badgesCount = statsField?.getFirestoreInt("badgesCount")
            ?: statsField?.getFirestoreInt("badgeCount")
            ?: statsField?.getFirestoreInt("stampCount")
            ?: fields?.getFirestoreInt("badgesCount")
            ?: fields?.getFirestoreInt("badgeCount")
            ?: fields?.getFirestoreInt("stampCount")
            ?: 0
        val level = statsField?.getFirestoreInt("level")
            ?: fields?.getFirestoreInt("level")
            ?: 1
        return MyPageSummary(
            userId = userId,
            totalVisits = visitCount,
            favoritesCount = favoritesCount,
            followedCastsCount = followedCastsCount,
            badgesCount = badgesCount,
            level = level
        )
    }

    private companion object {
        const val DEFAULT_OWNER_CLAIM_STATUS = "승인 대기 중"
        const val DEFAULT_REGISTRATION_CLAIM_STATUS = "승인 대기 중"
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

private fun JsonObject.getFirestoreDouble(key: String): Double? {
    val valueObject = this[key]?.jsonObject ?: return null
    val fromDouble = valueObject["doubleValue"]?.jsonPrimitive?.doubleOrNull
    val fromInteger = valueObject["integerValue"]?.jsonPrimitive?.longOrNull?.toDouble()
    return fromDouble ?: fromInteger
}

private fun JsonObject.getFirestoreInt(key: String): Int? {
    return getFirestoreLong(key)?.toInt()
}

private fun JsonObject.getFirestoreMap(key: String): JsonObject? {
    return this[key]
        ?.jsonObject
        ?.get("mapValue")
        ?.jsonObject
        ?.get("fields")
        ?.jsonObject
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

private fun firestoreMap(fields: Map<String, JsonElement>): JsonObject {
    return JsonObject(
        mapOf(
            "mapValue" to JsonObject(
                mapOf(
                    "fields" to JsonObject(fields)
                )
            )
        )
    )
}

private fun firestoreGeoPoint(latitude: Double, longitude: Double): JsonObject {
    return JsonObject(
        mapOf(
            "geoPointValue" to JsonObject(
                mapOf(
                    "latitude" to JsonPrimitive(latitude),
                    "longitude" to JsonPrimitive(longitude)
                )
            )
        )
    )
}

private fun String.toUserRoleOrNull(): UserRole? {
    val normalized = trim()
        .uppercase()
        .replace("-", "_")
        .replace(" ", "_")
    return when (normalized) {
        "ADMIN", "ROLE_ADMIN" -> UserRole.ADMIN
        "CAFE_OWNER", "CAFEOWNER", "OWNER", "ROLE_CAFE_OWNER" -> UserRole.CAFE_OWNER
        "CAST", "ROLE_CAST" -> UserRole.CAST
        "VISITOR", "GUEST", "ROLE_VISITOR" -> UserRole.VISITOR
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

private fun String.isApprovedClaimStatus(): Boolean {
    val normalized = trim()
        .uppercase()
        .replace("-", "_")
        .replace(" ", "_")
    return normalized == "APPROVED"
        || normalized == "승인"
        || normalized == "승인완료"
        || normalized == "승인_완료"
}

private fun String.isPendingClaimStatus(): Boolean {
    val normalized = trim()
        .uppercase()
        .replace("-", "_")
        .replace(" ", "_")
    return normalized == "PENDING"
        || normalized == "승인대기"
        || normalized == "승인_대기"
        || normalized == "승인대기중"
        || normalized == "승인_대기_중"
}

private fun escapeFirestoreQueryString(value: String): String {
    return value.replace("\\", "\\\\").replace("\"", "\\\"")
}
