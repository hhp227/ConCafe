package com.hhp227.concafe.data.source.firestore

import com.hhp227.concafe.domain.model.*
import kotlinx.datetime.Clock
import kotlinx.serialization.json.*

class FirestoreSyncRemoteDataSource(
    private val config: FirestoreConfig,
    private val restApi: FirestoreRestApi,
    private val tokenProvider: FirestoreAuthTokenProvider
) : FirestoreSyncDataSource {
    override suspend fun fetchUser(userId: String): User? {
        val document = loadUserDocument(userId = userId, idToken = tokenProvider.getIdToken()) ?: return null
        return parseUserDocument(document)
    }

    override suspend fun fetchMyPageSummary(userId: String): MyPageSummary? {
        val idToken = tokenProvider.getIdToken()
        val document = loadUserDocument(userId = userId, idToken = idToken) ?: return null
        val parsedSummary = parseMyPageSummaryDocument(userId = userId, document = document)
        val visitsCount = loadCollectionDocumentCount(
            collectionId = FirestorePaths.VISITS,
            idToken = idToken,
            equalsFilterFieldPath = "userId",
            equalsFilterValue = firestoreString(userId)
        )
        val stampsCount = loadCollectionDocumentCount(
            collectionId = FirestorePaths.STAMPS,
            idToken = idToken,
            equalsFilterFieldPath = "userId",
            equalsFilterValue = firestoreString(userId)
        )
        return parsedSummary.copy(totalVisits = visitsCount, badgesCount = stampsCount)
    }

    override suspend fun updateUserProfile(userId: String, nickname: String, profileImage: String?) {
        val path = "${config.documentBasePath()}/${FirestorePaths.USERS}/$userId"
        val body = firestoreDocumentBody(
            mapOf(
                "nickname" to firestoreString(nickname),
                "profileImage" to firestoreNullableString(profileImage)
            )
        )
        restApi.patch(path = path, body = body, idToken = tokenProvider.getIdToken(), updateMask = listOf("nickname", "profileImage"))
    }

    override suspend fun pushUser(user: User) {
        val idToken = tokenProvider.getIdToken()
        val existingAffiliatedCafeId = loadUserDocument(userId = user.id, idToken = idToken)?.let { parseUserAffiliatedCafeId(it) }
        val path = "${config.documentBasePath()}/${FirestorePaths.USERS}/${user.id}"
        val body = firestoreDocumentBody(
            mapOf(
                "email" to firestoreString(user.email),
                "nickname" to firestoreString(user.nickname),
                "profileImage" to firestoreNullableString(user.profileImage),
                "authProvider" to firestoreString(user.authProvider.name),
                "role" to firestoreString(user.role.name),
                "banned" to firestoreBoolean(user.banned),
                "createdAt" to firestoreString(user.createdAt),
                "phoneNumber" to firestoreNullableString(user.phoneNumber),
                "signupCompleted" to firestoreBoolean(user.signupCompleted),
                "affiliatedCafeId" to firestoreNullableString(existingAffiliatedCafeId)
            )
        )
        restApi.patch(path = path, body = body, idToken = idToken)
    }

    override suspend fun deleteUser(userId: String) {
        restApi.delete(path = "${config.documentBasePath()}/${FirestorePaths.USERS}/$userId", idToken = tokenProvider.getIdToken())
    }

    override suspend fun deleteCurrentUserCascade(idToken: String) {
        restApi.post(path = "${config.functionsBaseUrl()}/deleteCurrentUserCascade", body = "{}", idToken = idToken)
    }

    override suspend fun pushCafeRegistrationClaim(requesterUserId: String, claim: CafeRegistrationClaim) {
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
                        "location" to firestoreGeoPoint(claim.region.location.latitude, claim.region.location.longitude)
                    )
                )
            )
        )
        restApi.patch(path = path, body = body, idToken = tokenProvider.getIdToken())
    }

    override suspend fun pushCafeOwnerClaim(
        requesterUserId: String,
        claim: CafeManagementData.PendingClaimSummary,
        location: String,
        imageUrl: String?
    ) {
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFE_OWNER_CLAIMS}/${claim.claimId}"
        val body = firestoreDocumentBody(
            mapOf(
                "userId" to firestoreString(requesterUserId),
                "cafeId" to firestoreString(claim.cafeId),
                "cafeName" to firestoreString(claim.cafeName),
                "location" to firestoreString(location),
                "imageUrl" to firestoreNullableString(imageUrl),
                "requestedAt" to firestoreString(claim.requestedAt),
                "status" to firestoreString(claim.status),
                "message" to firestoreString(claim.message)
            )
        )
        restApi.patch(path = path, body = body, idToken = tokenProvider.getIdToken())
    }

    override suspend fun pushHomeBanner(banner: HomeBanner) {
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
        restApi.patch(path = path, body = body, idToken = tokenProvider.getIdToken())
    }

    override suspend fun deleteHomeBanner(bannerId: String) {
        restApi.delete(path = "${config.documentBasePath()}/${FirestorePaths.HOME_BANNERS}/$bannerId", idToken = tokenProvider.getIdToken())
    }

    override suspend fun refreshHomeBanners() {
        loadHomeBanners(idToken = tokenProvider.getIdToken())
    }

    override suspend fun refreshCafeManagementData(userId: String) {
        loadCollectionDocuments(
            collectionId = FirestorePaths.CAFE_OWNER_CLAIMS,
            idToken = tokenProvider.getIdToken(),
            equalsFilterFieldPath = "userId",
            equalsFilterValue = firestoreString(userId)
        )
        loadCollectionDocuments(
            collectionId = FirestorePaths.CAFE_REGISTRATION_CLAIMS,
            idToken = tokenProvider.getIdToken(),
            equalsFilterFieldPath = "userId",
            equalsFilterValue = firestoreString(userId)
        )
    }

    override suspend fun fetchPendingCafeOwnerClaimsForAdmin(): List<PendingCafeOwnerClaimPreview> {
        val documents = loadCollectionDocuments(
            collectionId = FirestorePaths.CAFE_OWNER_CLAIMS,
            idToken = tokenProvider.getIdToken()
        )
        return documents.mapNotNull { parsePendingCafeOwnerClaimPreviewForAdmin(it) }
            .sortedByDescending { it.requestedAt }
    }

    override suspend fun fetchPendingCafeRegistrationClaimsForAdmin(): List<PendingCafeRegistrationClaimPreview> {
        val documents = loadCollectionDocuments(
            collectionId = FirestorePaths.CAFE_REGISTRATION_CLAIMS,
            idToken = tokenProvider.getIdToken()
        )
        return documents.mapNotNull { parsePendingCafeRegistrationClaimPreviewForAdmin(it) }
            .sortedByDescending { it.requestedAt }
    }

    override suspend fun approveCafeOwnerClaimForAdmin(
        claimId: String,
        reviewedBy: String
    ): PendingCafeOwnerClaimPreview {
        val idToken = tokenProvider.getIdToken()
        val claimDocument = loadCafeOwnerClaimDocument(claimId, idToken)
        val fields = claimDocument["fields"]?.jsonObject ?: throw NoSuchElementException("claim not found")
        val currentStatus = fields.getFirestoreString("status").orEmpty()
        if (!currentStatus.isPendingClaimStatus()) throw IllegalArgumentException("already reviewed claim")

        val preview = parsePendingCafeOwnerClaimPreviewForAdmin(claimDocument) ?: throw NoSuchElementException("claim not found")
        markCafeOwnerClaimReviewed(
            claimId = claimId,
            reviewedBy = reviewedBy,
            status = "APPROVED",
            message = "관리자 승인으로 운영 카페에 연결되었습니다.",
            idToken = idToken
        )
        appendOwnerMapping(userId = preview.requesterUserId, cafeId = preview.cafeId, idToken = idToken)
        promoteRequesterRoleToCafeOwnerIfNeeded(requesterUserId = preview.requesterUserId, idToken = idToken)
        return preview
    }

    override suspend fun rejectCafeOwnerClaimForAdmin(
        claimId: String,
        reviewedBy: String
    ): PendingCafeOwnerClaimPreview {
        val idToken = tokenProvider.getIdToken()
        val claimDocument = loadCafeOwnerClaimDocument(claimId, idToken)
        val fields = claimDocument["fields"]?.jsonObject ?: throw NoSuchElementException("claim not found")
        val currentStatus = fields.getFirestoreString("status").orEmpty()
        if (!currentStatus.isPendingClaimStatus()) throw IllegalArgumentException("already reviewed claim")

        val preview = parsePendingCafeOwnerClaimPreviewForAdmin(claimDocument) ?: throw NoSuchElementException("claim not found")
        markCafeOwnerClaimReviewed(
            claimId = claimId,
            reviewedBy = reviewedBy,
            status = "REJECTED",
            message = "관리자 검토 결과 반려되었습니다.",
            idToken = idToken
        )
        return preview
    }

    override suspend fun approveCafeRegistrationClaimForAdmin(
        claimId: String,
        reviewedBy: String
    ): PendingCafeRegistrationClaimPreview {
        val idToken = tokenProvider.getIdToken()
        val claimDocument = loadCafeRegistrationClaimDocument(claimId, idToken)
        val fields = claimDocument["fields"]?.jsonObject ?: throw NoSuchElementException("claim not found")
        val currentStatus = fields.getFirestoreString("status").orEmpty()
        if (!currentStatus.isPendingClaimStatus()) throw IllegalArgumentException("already reviewed claim")

        val preview = parsePendingCafeRegistrationClaimPreviewForAdmin(claimDocument) ?: throw NoSuchElementException("claim not found")
        val claim = parseCafeRegistrationClaimDocument(claimDocument) ?: throw NoSuchElementException("claim not found")
        val newCafeId = nextFirestoreEntityId("cafe")

        createApprovedCafeDocument(cafeId = newCafeId, claim = claim, idToken = idToken)
        markCafeRegistrationClaimReviewed(
            claimId = claimId,
            reviewedBy = reviewedBy,
            status = "APPROVED",
            message = "관리자 승인으로 카페가 생성되었습니다.",
            approvedCafeId = newCafeId,
            idToken = idToken
        )
        appendOwnerMapping(userId = preview.requesterUserId, cafeId = newCafeId, idToken = idToken)
        promoteRequesterRoleToCafeOwnerIfNeeded(requesterUserId = preview.requesterUserId, idToken = idToken)
        return preview.copy(approvedCafeId = newCafeId)
    }

    override suspend fun rejectCafeRegistrationClaimForAdmin(
        claimId: String,
        reviewedBy: String
    ): PendingCafeRegistrationClaimPreview {
        val idToken = tokenProvider.getIdToken()
        val claimDocument = loadCafeRegistrationClaimDocument(claimId, idToken)
        val fields = claimDocument["fields"]?.jsonObject ?: throw NoSuchElementException("claim not found")
        val currentStatus = fields.getFirestoreString("status").orEmpty()
        if (!currentStatus.isPendingClaimStatus()) throw IllegalArgumentException("already reviewed claim")

        val preview = parsePendingCafeRegistrationClaimPreviewForAdmin(claimDocument) ?: throw NoSuchElementException("claim not found")
        markCafeRegistrationClaimReviewed(
            claimId = claimId,
            reviewedBy = reviewedBy,
            status = "REJECTED",
            message = "관리자 검토 결과 반려되었습니다.",
            approvedCafeId = null,
            idToken = idToken
        )
        return preview
    }

    override suspend fun fetchAdminOperationsMetrics(): AdminOperationsMetrics {
        val idToken = tokenProvider.getIdToken()
        val totalUsersCount = loadCollectionDocumentCount(collectionId = FirestorePaths.USERS, idToken = idToken)
        val totalCafesCount = loadCollectionDocumentCount(collectionId = FirestorePaths.CAFES, idToken = idToken)
        val approvedCafesCount = loadCollectionDocumentCount(
            collectionId = FirestorePaths.CAFES,
            idToken = idToken,
            equalsFilterFieldPath = "approved",
            equalsFilterValue = firestoreBoolean(true)
        )
        val reportItemsCount = runCatching {
            loadCollectionDocumentCount(collectionId = FirestorePaths.REPORTS, idToken = idToken)
        }.getOrElse { 0 }
        val activeCafesCount = if (approvedCafesCount == 0 && totalCafesCount > 0) totalCafesCount else approvedCafesCount
        return AdminOperationsMetrics(
            totalUsersCount = totalUsersCount,
            activeCafesCount = activeCafesCount,
            reportItemsCount = reportItemsCount
        )
    }

    private suspend fun loadHomeBanners(idToken: String?): List<HomeBanner> {
        val response = restApi.get(path = "${config.documentBasePath()}/${FirestorePaths.HOME_BANNERS}", idToken = idToken)
        val parsed = Json.parseToJsonElement(response).jsonObject
        val documents = parsed["documents"]?.jsonArray.orEmptyArray()
        return documents.mapNotNull { parseHomeBannerDocument(it.jsonObject) }
            .sortedByDescending { it.createdAtEpochMillis }
    }

    private suspend fun loadCollectionDocumentCount(
        collectionId: String,
        idToken: String?,
        equalsFilterFieldPath: String? = null,
        equalsFilterValue: JsonObject? = null
    ): Int {
        return if (equalsFilterFieldPath.isNullOrBlank() || equalsFilterValue == null) {
            loadCollectionDocuments(collectionId = collectionId, idToken = idToken).size
        } else {
            loadCollectionDocuments(
                collectionId = collectionId,
                idToken = idToken,
                equalsFilterFieldPath = equalsFilterFieldPath,
                equalsFilterValue = equalsFilterValue
            ).size
        }
    }

    private suspend fun loadCollectionDocuments(
        collectionId: String,
        idToken: String?,
        equalsFilterFieldPath: String? = null,
        equalsFilterValue: JsonObject? = null
    ): List<JsonObject> {
        return if (equalsFilterFieldPath.isNullOrBlank() || equalsFilterValue == null) {
            val response = restApi.get(path = "${config.documentBasePath()}/$collectionId", idToken = idToken)
            val parsed = Json.parseToJsonElement(response).jsonObject
            parsed["documents"]?.jsonArray.orEmptyArray().map { it.jsonObject }
        } else {
            val path = "${config.documentBasePath()}:runQuery"
            val body = """
                {
                  "structuredQuery": {
                    "from": [{ "collectionId": "$collectionId" }],
                    "where": {
                      "fieldFilter": {
                        "field": { "fieldPath": "${escapeFirestoreQueryString(equalsFilterFieldPath)}" },
                        "op": "EQUAL",
                        "value": ${equalsFilterValue}
                      }
                    }
                  }
                }
            """.trimIndent()
            val response = restApi.post(path = path, body = body, idToken = idToken)
            Json.parseToJsonElement(response).jsonArray.mapNotNull { it.jsonObject["document"]?.jsonObject }
        }
    }

    private suspend fun loadCafeRegistrationClaimDocument(claimId: String, idToken: String?): JsonObject {
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFE_REGISTRATION_CLAIMS}/$claimId"
        return Json.parseToJsonElement(restApi.get(path = path, idToken = idToken)).jsonObject
    }

    private suspend fun loadCafeOwnerClaimDocument(claimId: String, idToken: String?): JsonObject {
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFE_OWNER_CLAIMS}/$claimId"
        return Json.parseToJsonElement(restApi.get(path = path, idToken = idToken)).jsonObject
    }

    private suspend fun loadUserDocument(userId: String, idToken: String?): JsonObject? {
        val path = "${config.documentBasePath()}/${FirestorePaths.USERS}/$userId"
        val response = runCatching { restApi.get(path = path, idToken = idToken) }.getOrNull() ?: return null
        return Json.parseToJsonElement(response).jsonObject
    }

    private suspend fun loadCafeDocument(cafeId: String, idToken: String?): JsonObject? {
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFES}/$cafeId"
        val response = runCatching { restApi.get(path = path, idToken = idToken) }.getOrNull() ?: return null
        return Json.parseToJsonElement(response).jsonObject
    }

    private suspend fun createApprovedCafeDocument(
        cafeId: String,
        claim: CafeRegistrationClaim,
        idToken: String?
    ) {
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFES}/$cafeId"
        val now = Clock.System.now().toString()
        val body = firestoreDocumentBody(
            mapOf(
                "name" to firestoreString(claim.cafeName),
                "desc" to firestoreString(claim.description),
                "thumbnailImage" to firestoreNullableString(claim.thumbnailImage),
                "conceptType" to firestoreString(claim.conceptType),
                "businessHours" to firestoreString(claim.businessHours),
                "phoneNumber" to firestoreString(claim.phoneNumber),
                "approved" to firestoreBoolean(true),
                "ownerIds" to firestoreStringArray(emptyList()),
                "region" to firestoreMap(
                    mapOf(
                        "country" to firestoreString(claim.region.country),
                        "city" to firestoreString(claim.region.city),
                        "address" to firestoreString(claim.region.address),
                        "location" to firestoreGeoPoint(claim.region.location.latitude, claim.region.location.longitude)
                    )
                ),
                "createdAt" to firestoreString(now),
                "updatedAt" to firestoreString(now),
                "ratingAvg" to firestoreLong(0),
                "reviewCount" to firestoreLong(0)
            )
        )
        restApi.patch(path = path, body = body, idToken = idToken)
    }

    private suspend fun markCafeRegistrationClaimReviewed(
        claimId: String,
        reviewedBy: String,
        status: String,
        message: String,
        approvedCafeId: String?,
        idToken: String?
    ) {
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFE_REGISTRATION_CLAIMS}/$claimId"
        val body = firestoreDocumentBody(
            mapOf(
                "status" to firestoreString(status),
                "message" to firestoreString(message),
                "approvedCafeId" to firestoreNullableString(approvedCafeId),
                "reviewedBy" to firestoreString(reviewedBy),
                "reviewedAt" to firestoreString(Clock.System.now().toString())
            )
        )
        restApi.patch(
            path = path,
            body = body,
            idToken = idToken,
            updateMask = listOf("status", "message", "approvedCafeId", "reviewedBy", "reviewedAt")
        )
    }

    private suspend fun markCafeOwnerClaimReviewed(
        claimId: String,
        reviewedBy: String,
        status: String,
        message: String,
        idToken: String?
    ) {
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFE_OWNER_CLAIMS}/$claimId"
        val body = firestoreDocumentBody(
            mapOf(
                "status" to firestoreString(status),
                "message" to firestoreString(message),
                "reviewedBy" to firestoreString(reviewedBy),
                "reviewedAt" to firestoreString(Clock.System.now().toString())
            )
        )
        restApi.patch(path = path, body = body, idToken = idToken, updateMask = listOf("status", "message", "reviewedBy", "reviewedAt"))
    }

    private suspend fun appendOwnerMapping(userId: String, cafeId: String, idToken: String?) {
        appendCafeIdToUser(userId = userId, cafeId = cafeId, idToken = idToken)
        appendOwnerIdToCafe(cafeId = cafeId, userId = userId, idToken = idToken)
    }

    private suspend fun appendCafeIdToUser(userId: String, cafeId: String, idToken: String?) {
        val userDocument = loadUserDocument(userId = userId, idToken = idToken) ?: return
        val fields = userDocument["fields"]?.jsonObject ?: return
        val existingIds = fields.getFirestoreStringList("ownedCafeIds").toMutableList()
        if (!existingIds.contains(cafeId)) existingIds.add(cafeId)

        val path = "${config.documentBasePath()}/${FirestorePaths.USERS}/$userId"
        val body = firestoreDocumentBody(mapOf("ownedCafeIds" to firestoreStringArray(existingIds)))
        restApi.patch(path = path, body = body, idToken = idToken, updateMask = listOf("ownedCafeIds"))
    }

    private suspend fun appendOwnerIdToCafe(cafeId: String, userId: String, idToken: String?) {
        val cafeDocument = loadCafeDocument(cafeId = cafeId, idToken = idToken) ?: return
        val fields = cafeDocument["fields"]?.jsonObject ?: return
        val existingIds = fields.getFirestoreStringList("ownerIds").toMutableList()
        if (!existingIds.contains(userId)) existingIds.add(userId)

        val path = "${config.documentBasePath()}/${FirestorePaths.CAFES}/$cafeId"
        val body = firestoreDocumentBody(mapOf("ownerIds" to firestoreStringArray(existingIds)))
        restApi.patch(path = path, body = body, idToken = idToken, updateMask = listOf("ownerIds"))
    }

    private suspend fun promoteRequesterRoleToCafeOwnerIfNeeded(requesterUserId: String, idToken: String?) {
        val userDocument = loadUserDocument(userId = requesterUserId, idToken = idToken) ?: return
        val currentUser = parseUserDocument(userDocument) ?: return
        if (currentUser.role == UserRole.ADMIN || currentUser.role == UserRole.CAFE_OWNER) return

        val path = "${config.documentBasePath()}/${FirestorePaths.USERS}/$requesterUserId"
        val body = firestoreDocumentBody(mapOf("role" to firestoreString(UserRole.CAFE_OWNER.name)))
        restApi.patch(path = path, body = body, idToken = idToken, updateMask = listOf("role"))
    }

    private fun parseUserDocument(document: JsonObject): User? {
        val fields = document["fields"]?.jsonObject ?: return null
        val name = document["name"]?.jsonPrimitive?.contentOrNull ?: return null
        val userId = name.substringAfterLast("/")
        val email = fields.getFirestoreString("email") ?: return null
        val nickname = fields.getFirestoreString("nickname") ?: return null
        val role = fields.getFirestoreString("role")?.toUserRoleOrNull() ?: UserRole.VISITOR
        val createdAt = fields.getFirestoreString("createdAt") ?: "1970-01-01T00:00:00Z"
        val authProvider = fields.getFirestoreString("authProvider")
            ?.let { runCatching { AuthProvider.valueOf(it) }.getOrDefault(AuthProvider.UNKNOWN) }
            ?: AuthProvider.UNKNOWN
        return User(
            id = userId,
            email = email,
            nickname = nickname,
            profileImage = fields.getFirestoreString("profileImage"),
            authProvider = authProvider,
            role = role,
            banned = fields.getFirestoreBoolean("banned") ?: false,
            createdAt = createdAt,
            phoneNumber = fields.getFirestoreString("phoneNumber"),
            signupCompleted = fields.getFirestoreBoolean("signupCompleted") ?: true
        )
    }

    private fun parseUserAffiliatedCafeId(document: JsonObject): String? {
        val fields = document["fields"]?.jsonObject ?: return null
        return fields.getFirestoreString("affiliatedCafeId")?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun parseMyPageSummaryDocument(userId: String, document: JsonObject): MyPageSummary {
        val fields = document["fields"]?.jsonObject
        val totalVisits = fields?.getFirestoreLong("totalVisits")?.toInt()
            ?: fields?.getFirestoreLong("visitCount")?.toInt()
            ?: 0
        val favoritesCount = fields?.getFirestoreLong("favoritesCount")?.toInt()
            ?: fields?.getFirestoreLong("favoriteCafeCount")?.toInt()
            ?: 0
        val followedCastsCount = fields?.getFirestoreLong("followedCastsCount")?.toInt()
            ?: fields?.getFirestoreLong("followCount")?.toInt()
            ?: 0
        val badgesCount = fields?.getFirestoreLong("badgesCount")?.toInt()
            ?: fields?.getFirestoreLong("stampCount")?.toInt()
            ?: 0
        val level = fields?.getFirestoreLong("level")?.toInt() ?: 1
        return MyPageSummary(
            userId = userId,
            totalVisits = totalVisits,
            favoritesCount = favoritesCount,
            followedCastsCount = followedCastsCount,
            badgesCount = badgesCount,
            level = level
        )
    }

    private fun parseCafeRegistrationClaimDocument(document: JsonObject): CafeRegistrationClaim? {
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

    private suspend fun parsePendingCafeOwnerClaimPreviewForAdmin(document: JsonObject): PendingCafeOwnerClaimPreview? {
        val fields = document["fields"]?.jsonObject ?: return null
        val status = fields.getFirestoreString("status").orEmpty()
        if (!status.isPendingClaimStatus()) return null
        val name = document["name"]?.jsonPrimitive?.contentOrNull ?: return null
        val claimId = name.substringAfterLast("/")
        val requesterUserId = fields.getFirestoreString("userId") ?: return null
        val requesterNickname = resolveUserNickname(requesterUserId)
        val cafeId = fields.getFirestoreString("cafeId").orEmpty()
        val cafeName = fields.getFirestoreString("cafeName") ?: "신청 카페"
        val location = fields.getFirestoreString("location") ?: "위치 정보 없음"
        val requestedAt = fields.getFirestoreString("requestedAt") ?: fields.getFirestoreString("createdAt") ?: ""
        val message = fields.getFirestoreString("message") ?: "관리자 승인 후 내 카페 목록에 자동 연결됩니다"
        return PendingCafeOwnerClaimPreview(
            claimId = claimId,
            requesterUserId = requesterUserId,
            requesterNickname = requesterNickname,
            cafeId = cafeId,
            cafeName = cafeName,
            location = location,
            requestedAt = requestedAt,
            message = message,
            imageUrl = fields.getFirestoreString("imageUrl")
        )
    }

    private suspend fun parsePendingCafeRegistrationClaimPreviewForAdmin(
        document: JsonObject
    ): PendingCafeRegistrationClaimPreview? {
        val fields = document["fields"]?.jsonObject ?: return null
        val status = fields.getFirestoreString("status").orEmpty()
        if (!status.isPendingClaimStatus()) return null
        val name = document["name"]?.jsonPrimitive?.contentOrNull ?: return null
        val claimId = name.substringAfterLast("/")
        val requesterUserId = fields.getFirestoreString("userId") ?: return null
        val requesterNickname = resolveUserNickname(requesterUserId)
        val regionFields = fields.getFirestoreMap("region")
        val city = regionFields?.getFirestoreString("city") ?: fields.getFirestoreString("city") ?: ""
        val address = regionFields?.getFirestoreString("address") ?: fields.getFirestoreString("address") ?: ""
        return PendingCafeRegistrationClaimPreview(
            claimId = claimId,
            requesterUserId = requesterUserId,
            requesterNickname = requesterNickname,
            approvedCafeId = fields.getFirestoreString("approvedCafeId"),
            cafeName = fields.getFirestoreString("cafeName").orEmpty(),
            location = listOf(city, address).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "위치 정보 없음" },
            requestedAt = fields.getFirestoreString("requestedAt") ?: fields.getFirestoreString("createdAt") ?: "",
            message = fields.getFirestoreString("message") ?: "관리자 승인 후 새 카페가 생성되고 운영 카페에 자동 연결됩니다.",
            imageUrl = fields.getFirestoreString("thumbnailImage")
        )
    }

    private suspend fun resolveUserNickname(userId: String): String {
        return fetchUser(userId)?.nickname ?: "알 수 없음"
    }

    private fun parseHomeBannerDocument(document: JsonObject): HomeBanner? {
        val fields = document["fields"]?.jsonObject ?: return null
        val name = document["name"]?.jsonPrimitive?.contentOrNull ?: return null
        val bannerId = name.substringAfterLast("/")
        val targetType = fields.getFirestoreString("linkType")
            ?.uppercase()
            ?.toBannerLinkTargetTypeOrNull()
            ?: BannerLinkTargetType.EXTERNAL_LINK
        val targetValue = fields.getFirestoreString("linkTarget")
            ?: fields.getFirestoreString("externalUrl")
            ?: ""
        return HomeBanner(
            id = bannerId,
            title = fields.getFirestoreString("title").orEmpty(),
            startColorHex = fields.getFirestoreString("startColorHex") ?: "#FB7185",
            endColorHex = fields.getFirestoreString("endColorHex") ?: "#F97316",
            subtitle = fields.getFirestoreString("subtitle").orEmpty(),
            cafeId = fields.getFirestoreString("relatedCafeId"),
            imageUrl = fields.getFirestoreString("imageUrl"),
            targetType = targetType,
            targetValue = targetValue,
            displayDays = fields.getFirestoreLong("displayDays")?.toInt() ?: 1,
            statusLabel = fields.getFirestoreString("status") ?: "ACTIVE",
            createdAtEpochMillis = fields.getFirestoreLong("createdAtEpochMillis") ?: 0L,
            activatedAtEpochMillis = fields.getFirestoreLong("activatedAtEpochMillis") ?: 0L
        )
    }

    private fun firestoreDocumentBody(fields: Map<String, JsonElement>): String {
        return JsonObject(mapOf("fields" to JsonObject(fields))).toString()
    }

    private fun firestoreString(value: String): JsonObject = JsonObject(mapOf("stringValue" to JsonPrimitive(value)))
    private fun firestoreNullableString(value: String?): JsonObject {
        return if (value == null) JsonObject(mapOf("nullValue" to JsonPrimitive("NULL_VALUE"))) else firestoreString(value)
    }
    private fun firestoreBoolean(value: Boolean): JsonObject = JsonObject(mapOf("booleanValue" to JsonPrimitive(value)))
    private fun firestoreLong(value: Long): JsonObject = JsonObject(mapOf("integerValue" to JsonPrimitive(value.toString())))
    private fun firestoreMap(fields: Map<String, JsonElement>): JsonObject {
        return JsonObject(mapOf("mapValue" to JsonObject(mapOf("fields" to JsonObject(fields)))))
    }
    private fun firestoreStringArray(values: List<String>): JsonObject {
        return JsonObject(
            mapOf(
                "arrayValue" to JsonObject(
                    mapOf("values" to JsonArray(values.map { value -> JsonObject(mapOf("stringValue" to JsonPrimitive(value))) }))
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

    private fun JsonObject.getFirestoreString(key: String): String? {
        return this[key]?.jsonObject?.get("stringValue")?.jsonPrimitive?.contentOrNull
    }

    private fun JsonObject.getFirestoreLong(key: String): Long? {
        val value = this[key]?.jsonObject ?: return null
        return value["integerValue"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
            ?: value["doubleValue"]?.jsonPrimitive?.doubleOrNull?.toLong()
    }

    private fun JsonObject.getFirestoreBoolean(key: String): Boolean? {
        val value = this[key]?.jsonObject ?: return null
        return value["booleanValue"]?.jsonPrimitive?.booleanOrNull
    }

    private fun JsonObject.getFirestoreMap(key: String): JsonObject? {
        return this[key]?.jsonObject?.get("mapValue")?.jsonObject?.get("fields")?.jsonObject
    }

    private fun JsonObject.getFirestoreStringList(key: String): List<String> {
        val values = this[key]
            ?.jsonObject
            ?.get("arrayValue")
            ?.jsonObject
            ?.get("values")
            ?.jsonArray
            .orEmptyArray()
        return values.mapNotNull { element ->
            element.jsonObject["stringValue"]?.jsonPrimitive?.contentOrNull
        }
    }

    private fun JsonArray?.orEmptyArray(): JsonArray = this ?: JsonArray(emptyList())

    private fun escapeFirestoreQueryString(value: String): String {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
    }

    private fun String.isPendingClaimStatus(): Boolean {
        val normalized = trim().uppercase().replace("-", "_").replace(" ", "_")
        return normalized == "PENDING" ||
            normalized == "승인대기" ||
            normalized == "승인_대기" ||
            normalized == "승인대기중" ||
            normalized == "승인_대기_중"
    }

    private fun String.toUserRoleOrNull(): UserRole? {
        return when (trim().uppercase()) {
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

    private fun nextFirestoreEntityId(prefix: String): String {
        return "$prefix-${Clock.System.now().toEpochMilliseconds()}"
    }

    private companion object {
        const val DEFAULT_REGISTRATION_CLAIM_STATUS = "승인 대기 중"
    }
}
