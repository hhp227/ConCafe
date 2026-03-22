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
import com.hhp227.concafe.domain.model.CafeDetail
import com.hhp227.concafe.domain.model.CafeInfoUpdate
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
import kotlinx.datetime.Clock
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
            .onFailure { throwable -> println("Firestore bootstrap loadUsers failed: ${throwable.message}") }
        runCatching { loadHomeBanners(idToken = null) }
            .onFailure { throwable -> println("Firestore bootstrap loadHomeBanners failed: ${throwable.message}") }
        runCatching { loadCafes(idToken = null) }
            .onFailure { throwable -> println("Firestore bootstrap loadCafes failed: ${throwable.message}") }
        runCatching { loadCasts(idToken = null) }
            .onFailure { throwable -> println("Firestore bootstrap loadCasts failed: ${throwable.message}") }
        runCatching { loadNotices(idToken = null) }
            .onFailure { throwable -> println("Firestore bootstrap loadNotices failed: ${throwable.message}") }
        runCatching { loadVisits(idToken) }
            .onFailure { throwable -> println("Firestore bootstrap loadVisits failed: ${throwable.message}") }
    }

    override fun cafeDetail(cafeId: String): CafeDetail? {
        val cafe = cafes.firstOrNull { it.id == cafeId } ?: return null
        val cached = delegate.cafeDetailsById[cafeId]
        val synced = cached?.copy(
            cafe = cafe,
            casts = delegate.casts.filter { cast -> cast.cafeId == cafeId },
            notices = notices
                .filter { notice -> notice.cafeId == cafeId }
                .sortedByDescending { notice -> notice.createdAt }
        )
            ?: CafeDetail(
                cafe = cafe,
                images = listOfNotNull(cafe.thumbnailImage),
                casts = delegate.casts.filter { cast -> cast.cafeId == cafeId },
                menus = emptyList(),
                goods = emptyList(),
                notices = notices
                    .filter { notice -> notice.cafeId == cafeId }
                    .sortedByDescending { notice -> notice.createdAt },
                businessHours = "운영시간 정보 준비중",
                phoneNumber = "연락처 정보 준비중"
            )
        delegate.cafeDetailsById[cafeId] = synced
        return synced
    }

    suspend fun refreshCafeDetail(cafeId: String) {
        val idToken = tokenProvider.getIdToken()
        val cafeDocument = runCatching {
            loadCafeDocument(cafeId, idToken)
        }.recoverCatching {
            loadCafeDocument(cafeId, null)
        }.getOrElse { throwable ->
            throw IllegalStateException("Failed to load cafe document for $cafeId", throwable)
        }
        val cafe = parseCafeDocument(cafeDocument) ?: throw NoSuchElementException("cafe not found")
        val detailMetadata = parseCafeDetailMetadata(cafeDocument)
        val cafePath = "${config.documentBasePath()}/${FirestorePaths.CAFES}/$cafeId"
        val castsResponse = restApi.get("$cafePath/${FirestorePaths.CAFE_CASTS}", null)
        val castsDocuments = Json.parseToJsonElement(castsResponse).jsonObject["documents"]?.jsonArray.orEmpty()
        val parsedCasts = castsDocuments.mapNotNull { document ->
            parseCastDocument(cafeId, document.jsonObject)
        }
        val noticesResponse = restApi.get("$cafePath/${FirestorePaths.CAFE_NOTICES}", null)
        val noticeDocuments = Json.parseToJsonElement(noticesResponse).jsonObject["documents"]?.jsonArray.orEmpty()
        val parsedNotices = noticeDocuments.mapNotNull { document ->
            parseNoticeDocument(
                cafeId = cafeId,
                cafeName = cafe.name,
                document = document.jsonObject
            )
        }.sortedByDescending { notice -> notice.createdAt }
        upsertCafeAndDetail(
            cafe = cafe,
            casts = parsedCasts,
            notices = parsedNotices,
            images = detailMetadata.images,
            businessHours = detailMetadata.businessHours,
            phoneNumber = detailMetadata.phoneNumber
        )
    }

    suspend fun updateCafeInfoRemote(update: CafeInfoUpdate): CafeDetail {
        val cafe = cafes.firstOrNull { current -> current.id == update.cafeId }
            ?: throw NoSuchElementException("cafe not found")
        val currentDetail = delegate.cafeDetailsById[update.cafeId] ?: cafeDetail(update.cafeId)
        val representativeImage = update.representativeImageUrl
            ?.trim()
            ?.takeIf { value -> value.isNotEmpty() }
            ?: currentDetail?.images?.firstOrNull()
            ?: cafe.thumbnailImage
        val galleryImages = update.galleryImages
            .map { image -> image.trim() }
            .filter { image -> image.isNotEmpty() }
        val nextImages = buildList {
            representativeImage?.let { image -> add(image) }
            addAll(galleryImages.filterNot { image -> image == representativeImage })
        }
        val resolvedLocation = update.location ?: cafe.region.location
        val businessHours = formatBusinessHours(update)
        val phoneNumber = update.contactNumber.trim()
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFES}/${update.cafeId}" +
            "?updateMask.fieldPaths=name" +
            "&updateMask.fieldPaths=desc" +
            "&updateMask.fieldPaths=thumbnailImage" +
            "&updateMask.fieldPaths=galleryImages" +
            "&updateMask.fieldPaths=businessHours" +
            "&updateMask.fieldPaths=phoneNumber" +
            "&updateMask.fieldPaths=region"
        val body = firestoreDocumentBody(
            mapOf(
                "name" to firestoreString(update.name.trim()),
                "desc" to firestoreString(update.description.trim()),
                "thumbnailImage" to firestoreNullableString(representativeImage),
                "galleryImages" to firestoreStringArray(nextImages),
                "businessHours" to firestoreString(businessHours),
                "phoneNumber" to firestoreString(phoneNumber),
                "region" to firestoreMap(
                    mapOf(
                        "country" to firestoreString(cafe.region.country),
                        "city" to firestoreString(cafe.region.city),
                        "address" to firestoreString(update.address.trim()),
                        "location" to firestoreGeoPoint(
                            latitude = resolvedLocation.latitude,
                            longitude = resolvedLocation.longitude
                        )
                    )
                )
            )
        )
        val idToken = tokenProvider.getIdToken()

        restApi.patch(path, body, idToken)
        return delegate.updateCafeInfo(
            update.copy(
                name = update.name.trim(),
                description = update.description.trim(),
                representativeImageUrl = representativeImage,
                galleryImages = nextImages,
                location = resolvedLocation,
                address = update.address.trim(),
                contactNumber = phoneNumber
            )
        )
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

    override suspend fun pushCafeOwnerClaim(
        requesterUserId: String,
        claim: CafeManagementData.PendingClaimSummary,
        location: String,
        imageUrl: String?
    ) {
        val idToken = tokenProvider.getIdToken()
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
        val userDocument = runCatching {
            val path = "${config.documentBasePath()}/${FirestorePaths.USERS}/$userId"
            val response = restApi.get(path, idToken)
            Json.parseToJsonElement(response).jsonObject
        }.getOrNull()
        val ownedCafeIdsFromUser = userDocument
            ?.get("fields")
            ?.jsonObject
            ?.getFirestoreStringList("ownedCafeIds")
            .orEmpty()
        ownedCafeIds.addAll(ownedCafeIdsFromUser)
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
        syncOwnedCafeDocuments(
            ownedCafeIds = ownedCafeIds,
            idToken = idToken
        )
        pendingCafeClaimsByUser[userId] = ownerClaims
            .sortedByDescending { it.requestedAt }
            .toMutableList()
        pendingCafeRegistrationClaimsByUser[userId] = registrationClaims
            .sortedByDescending { it.requestedAt }
            .toMutableList()
        ownedCafeIdsByUser[userId] = ownedCafeIds.toMutableList()
    }

    private suspend fun syncOwnedCafeDocuments(
        ownedCafeIds: Set<String>,
        idToken: String?
    ) {
        ownedCafeIds.forEach { cafeId ->
            val hasCafe = cafes.any { it.id == cafeId }

            if (hasCafe) {
                return@forEach
            }
            val cafeDocument = runCatching {
                val path = "${config.documentBasePath()}/${FirestorePaths.CAFES}/$cafeId"
                val response = restApi.get(path, idToken)
                Json.parseToJsonElement(response).jsonObject
            }.getOrNull()
            val parsedCafe = cafeDocument?.let { document ->
                parseCafeDocument(document)
            }

            if (parsedCafe != null) {
                cafes.add(parsedCafe)
            }
        }
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

    override suspend fun approveCafeOwnerClaimForAdmin(
        claimId: String,
        reviewedBy: String
    ): PendingCafeOwnerClaimPreview {
        val idToken = tokenProvider.getIdToken()
        val claimDocument = loadCafeOwnerClaimDocument(claimId, idToken)
        val claimFields = claimDocument["fields"]?.jsonObject ?: throw NoSuchElementException("claim not found")
        val currentStatus = claimFields.getFirestoreString("status").orEmpty()

        if (!currentStatus.isPendingClaimStatus()) {
            throw IllegalArgumentException("already reviewed claim")
        }
        val preview = parsePendingCafeOwnerClaimPreviewForAdmin(claimDocument)
            ?: throw NoSuchElementException("claim not found")

        markCafeOwnerClaimReviewed(
            claimId = claimId,
            reviewedBy = reviewedBy,
            status = "APPROVED",
            message = "관리자 승인으로 운영 카페에 연결되었습니다.",
            idToken = idToken
        )
        appendOwnerMapping(
            userId = preview.requesterUserId,
            cafeId = preview.cafeId,
            idToken = idToken
        )
        promoteRequesterRoleToCafeOwnerIfNeeded(
            requesterUserId = preview.requesterUserId,
            idToken = idToken
        )
        applyApprovedOwnerClaimToCache(preview)
        return preview
    }

    override suspend fun rejectCafeOwnerClaimForAdmin(
        claimId: String,
        reviewedBy: String
    ): PendingCafeOwnerClaimPreview {
        val idToken = tokenProvider.getIdToken()
        val claimDocument = loadCafeOwnerClaimDocument(claimId, idToken)
        val claimFields = claimDocument["fields"]?.jsonObject ?: throw NoSuchElementException("claim not found")
        val currentStatus = claimFields.getFirestoreString("status").orEmpty()

        if (!currentStatus.isPendingClaimStatus()) {
            throw IllegalArgumentException("already reviewed claim")
        }
        val preview = parsePendingCafeOwnerClaimPreviewForAdmin(claimDocument)
            ?: throw NoSuchElementException("claim not found")

        markCafeOwnerClaimReviewed(
            claimId = claimId,
            reviewedBy = reviewedBy,
            status = "REJECTED",
            message = "관리자 검토 결과 반려되었습니다.",
            idToken = idToken
        )
        applyRejectedOwnerClaimToCache(preview)
        return preview
    }

    override suspend fun approveCafeRegistrationClaimForAdmin(
        claimId: String,
        reviewedBy: String
    ): PendingCafeRegistrationClaimPreview {
        val idToken = tokenProvider.getIdToken()
        val claimDocument = loadCafeRegistrationClaimDocument(claimId, idToken)
        val claimFields = claimDocument["fields"]?.jsonObject ?: throw NoSuchElementException("claim not found")
        val currentStatus = claimFields.getFirestoreString("status").orEmpty()

        if (!currentStatus.isPendingClaimStatus()) {
            throw IllegalArgumentException("already reviewed claim")
        }
        val preview = parsePendingCafeRegistrationClaimPreviewForAdmin(claimDocument)
            ?: throw NoSuchElementException("claim not found")
        val claim = parseCafeRegistrationClaimDocument(claimDocument)
            ?: throw NoSuchElementException("claim not found")
        val newCafeId = nextFirestoreEntityId("cafe")

        createApprovedCafeDocument(
            cafeId = newCafeId,
            claim = claim,
            idToken = idToken
        )
        markCafeRegistrationClaimReviewed(
            claimId = claimId,
            reviewedBy = reviewedBy,
            status = "APPROVED",
            message = "관리자 승인으로 카페가 생성되었습니다.",
            approvedCafeId = newCafeId,
            idToken = idToken
        )
        appendOwnerMapping(
            userId = preview.requesterUserId,
            cafeId = newCafeId,
            idToken = idToken
        )
        promoteRequesterRoleToCafeOwnerIfNeeded(
            requesterUserId = preview.requesterUserId,
            idToken = idToken
        )
        applyApprovedRegistrationClaimToCache(
            preview = preview,
            claim = claim,
            newCafeId = newCafeId
        )
        return preview
    }

    override suspend fun rejectCafeRegistrationClaimForAdmin(
        claimId: String,
        reviewedBy: String
    ): PendingCafeRegistrationClaimPreview {
        val idToken = tokenProvider.getIdToken()
        val claimDocument = loadCafeRegistrationClaimDocument(claimId, idToken)
        val claimFields = claimDocument["fields"]?.jsonObject ?: throw NoSuchElementException("claim not found")
        val currentStatus = claimFields.getFirestoreString("status").orEmpty()

        if (!currentStatus.isPendingClaimStatus()) {
            throw IllegalArgumentException("already reviewed claim")
        }
        val preview = parsePendingCafeRegistrationClaimPreviewForAdmin(claimDocument)
            ?: throw NoSuchElementException("claim not found")

        markCafeRegistrationClaimReviewed(
            claimId = claimId,
            reviewedBy = reviewedBy,
            status = "REJECTED",
            message = "관리자 검토 결과 반려되었습니다.",
            approvedCafeId = null,
            idToken = idToken
        )
        applyRejectedRegistrationClaimToCache(
            preview = preview,
            claimId = claimId
        )
        return preview
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

    private suspend fun loadCafeRegistrationClaimDocument(claimId: String, idToken: String?): JsonObject {
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFE_REGISTRATION_CLAIMS}/$claimId"
        val response = restApi.get(path, idToken)
        return Json.parseToJsonElement(response).jsonObject
    }

    private suspend fun loadCafeOwnerClaimDocument(claimId: String, idToken: String?): JsonObject {
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFE_OWNER_CLAIMS}/$claimId"
        val response = restApi.get(path, idToken)
        return Json.parseToJsonElement(response).jsonObject
    }

    private suspend fun loadUserDocument(userId: String, idToken: String?): JsonObject {
        val path = "${config.documentBasePath()}/${FirestorePaths.USERS}/$userId"
        val response = restApi.get(path, idToken)
        return Json.parseToJsonElement(response).jsonObject
    }

    private suspend fun loadCafeDocument(cafeId: String, idToken: String?): JsonObject {
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFES}/$cafeId"
        val response = restApi.get(path, idToken)
        return Json.parseToJsonElement(response).jsonObject
    }

    private suspend fun createApprovedCafeDocument(
        cafeId: String,
        claim: CafeRegistrationClaim,
        idToken: String?
    ) {
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFES}/$cafeId"
        val body = firestoreDocumentBody(
            mapOf(
                "name" to firestoreString(claim.cafeName),
                "desc" to firestoreString(claim.description),
                "thumbnailImage" to firestoreNullableString(claim.thumbnailImage),
                "images" to firestoreStringArray(
                    listOfNotNull(claim.thumbnailImage?.trim()?.takeIf { value -> value.isNotEmpty() })
                ),
                "businessHours" to firestoreString(claim.businessHours),
                "phoneNumber" to firestoreString(claim.phoneNumber),
                "approved" to firestoreBoolean(true),
                "conceptType" to firestoreString(claim.conceptType),
                "ratingAvg" to firestoreLong(0),
                "reviewCount" to firestoreLong(0),
                "ownerIds" to firestoreStringArray(emptyList()),
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
                "reviewedBy" to firestoreString(reviewedBy),
                "reviewedAt" to firestoreString(Clock.System.now().toString()),
                "approvedCafeId" to firestoreNullableString(approvedCafeId)
            )
        )
        restApi.patch(path, body, idToken)
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
        restApi.patch(path, body, idToken)
    }

    private suspend fun appendOwnerMapping(
        userId: String,
        cafeId: String,
        idToken: String?
    ) {
        appendOwnedCafeIdToUser(
            userId = userId,
            cafeId = cafeId,
            idToken = idToken
        )
        appendOwnerIdToCafe(
            cafeId = cafeId,
            userId = userId,
            idToken = idToken
        )
    }

    private suspend fun appendOwnedCafeIdToUser(
        userId: String,
        cafeId: String,
        idToken: String?
    ) {
        val userDocument = runCatching {
            loadUserDocument(userId, idToken)
        }.getOrNull() ?: return
        val fields = userDocument["fields"]?.jsonObject ?: return
        val existingIds = fields.getFirestoreStringList("ownedCafeIds").toMutableList()

        if (!existingIds.contains(cafeId)) {
            existingIds.add(cafeId)
        }
        val path = "${config.documentBasePath()}/${FirestorePaths.USERS}/$userId?updateMask.fieldPaths=ownedCafeIds"
        val body = firestoreDocumentBody(
            mapOf(
                "ownedCafeIds" to firestoreStringArray(existingIds)
            )
        )
        restApi.patch(path, body, idToken)
    }

    private suspend fun appendOwnerIdToCafe(
        cafeId: String,
        userId: String,
        idToken: String?
    ) {
        val cafeDocument = runCatching {
            loadCafeDocument(cafeId, idToken)
        }.getOrNull() ?: return
        val fields = cafeDocument["fields"]?.jsonObject ?: return
        val existingIds = fields.getFirestoreStringList("ownerIds").toMutableList()

        if (!existingIds.contains(userId)) {
            existingIds.add(userId)
        }
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFES}/$cafeId?updateMask.fieldPaths=ownerIds"
        val body = firestoreDocumentBody(
            mapOf(
                "ownerIds" to firestoreStringArray(existingIds)
            )
        )
        restApi.patch(path, body, idToken)
    }

    private suspend fun promoteRequesterRoleToCafeOwnerIfNeeded(
        requesterUserId: String,
        idToken: String?
    ) {
        val userDocument = runCatching {
            val path = "${config.documentBasePath()}/${FirestorePaths.USERS}/$requesterUserId"
            val response = restApi.get(path, idToken)
            Json.parseToJsonElement(response).jsonObject
        }.getOrNull() ?: return
        val currentUser = parseUserDocument(userDocument) ?: return

        if (currentUser.role == UserRole.ADMIN || currentUser.role == UserRole.CAFE_OWNER) {
            return
        }
        val path = "${config.documentBasePath()}/${FirestorePaths.USERS}/$requesterUserId"
        val body = firestoreDocumentBody(
            mapOf(
                "email" to firestoreString(currentUser.email),
                "nickname" to firestoreString(currentUser.nickname),
                "profileImage" to firestoreNullableString(currentUser.profileImage),
                "role" to firestoreString(UserRole.CAFE_OWNER.name),
                "banned" to firestoreBoolean(currentUser.banned),
                "createdAt" to firestoreString(currentUser.createdAt)
            )
        )
        restApi.patch(path, body, idToken)
    }

    private fun applyApprovedRegistrationClaimToCache(
        preview: PendingCafeRegistrationClaimPreview,
        claim: CafeRegistrationClaim,
        newCafeId: String
    ) {
        val requesterUserId = preview.requesterUserId
        val originalClaims = pendingCafeRegistrationClaimsByUser[requesterUserId]?.toList().orEmpty()
        val originalOwnedCafeIds = ownedCafeIdsByUser[requesterUserId]?.toList().orEmpty()
        val originalUser = findUserById(requesterUserId)
        val originalCafe = cafes.firstOrNull { it.id == newCafeId }

        try {
            pendingCafeRegistrationClaimsByUser[requesterUserId] =
                originalClaims.filterNot { it.claimId == preview.claimId }.toMutableList()

            val nextOwnedCafeIds = originalOwnedCafeIds.toMutableList()

            if (!nextOwnedCafeIds.contains(newCafeId)) {
                nextOwnedCafeIds.add(newCafeId)
            }
            ownedCafeIdsByUser[requesterUserId] = nextOwnedCafeIds

            if (originalCafe == null) {
                cafes.add(
                    Cafe(
                        id = newCafeId,
                        name = claim.cafeName,
                        desc = claim.description,
                        region = claim.region,
                        thumbnailImage = claim.thumbnailImage,
                        ratingAvg = 0.0,
                        reviewCount = 0,
                        approved = true,
                        conceptType = claim.conceptType
                    )
                )
            }
            if (originalUser != null && originalUser.role != UserRole.ADMIN && originalUser.role != UserRole.CAFE_OWNER) {
                replaceUser(originalUser.copy(role = UserRole.CAFE_OWNER))
            }
        } catch (e: Exception) {
            pendingCafeRegistrationClaimsByUser[requesterUserId] = originalClaims.toMutableList()
            ownedCafeIdsByUser[requesterUserId] = originalOwnedCafeIds.toMutableList()

            if (originalUser != null) {
                replaceUser(originalUser)
            }
            if (originalCafe == null) {
                cafes.removeAll { it.id == newCafeId }
            }
            throw e
        }
    }

    private fun applyApprovedOwnerClaimToCache(preview: PendingCafeOwnerClaimPreview) {
        val requesterUserId = preview.requesterUserId
        val originalClaims = pendingCafeClaimsByUser[requesterUserId]?.toList().orEmpty()
        val originalOwnedCafeIds = ownedCafeIdsByUser[requesterUserId]?.toList().orEmpty()
        val originalUser = findUserById(requesterUserId)

        try {
            pendingCafeClaimsByUser[requesterUserId] =
                originalClaims.filterNot { it.claimId == preview.claimId }.toMutableList()

            val nextOwnedCafeIds = originalOwnedCafeIds.toMutableList()

            if (!nextOwnedCafeIds.contains(preview.cafeId)) {
                nextOwnedCafeIds.add(preview.cafeId)
            }
            ownedCafeIdsByUser[requesterUserId] = nextOwnedCafeIds

            if (originalUser != null && originalUser.role != UserRole.ADMIN && originalUser.role != UserRole.CAFE_OWNER) {
                replaceUser(originalUser.copy(role = UserRole.CAFE_OWNER))
            }
        } catch (e: Exception) {
            pendingCafeClaimsByUser[requesterUserId] = originalClaims.toMutableList()
            ownedCafeIdsByUser[requesterUserId] = originalOwnedCafeIds.toMutableList()
            if (originalUser != null) {
                replaceUser(originalUser)
            }
            throw e
        }
    }

    private fun applyRejectedOwnerClaimToCache(preview: PendingCafeOwnerClaimPreview) {
        val requesterUserId = preview.requesterUserId
        val originalClaims = pendingCafeClaimsByUser[requesterUserId]?.toList().orEmpty()

        try {
            pendingCafeClaimsByUser[requesterUserId] =
                originalClaims.filterNot { it.claimId == preview.claimId }.toMutableList()
        } catch (e: Exception) {
            pendingCafeClaimsByUser[requesterUserId] = originalClaims.toMutableList()
            throw e
        }
    }

    private fun applyRejectedRegistrationClaimToCache(
        preview: PendingCafeRegistrationClaimPreview,
        claimId: String
    ) {
        val requesterUserId = preview.requesterUserId
        val originalClaims = pendingCafeRegistrationClaimsByUser[requesterUserId]?.toList().orEmpty()

        try {
            pendingCafeRegistrationClaimsByUser[requesterUserId] =
                originalClaims.filterNot { it.claimId == claimId }.toMutableList()
        } catch (e: Exception) {
            pendingCafeRegistrationClaimsByUser[requesterUserId] = originalClaims.toMutableList()
            throw e
        }
    }

    private fun clearHomeFeedCollections() {
        this.cafes.clear()
        delegate.casts.clear()
        this.notices.clear()
        this.visits.clear()
    }

    private fun upsertCafeAndDetail(
        cafe: Cafe,
        casts: List<Cast>,
        notices: List<Notice>,
        images: List<String>? = null,
        businessHours: String? = null,
        phoneNumber: String? = null
    ) {
        val cafeIndex = cafes.indexOfFirst { current -> current.id == cafe.id }

        if (cafeIndex >= 0) {
            cafes[cafeIndex] = cafe
        } else {
            cafes.add(cafe)
        }
        delegate.casts.removeAll { cast -> cast.cafeId == cafe.id }
        delegate.casts.addAll(casts)
        this.notices.removeAll { notice -> notice.cafeId == cafe.id }
        this.notices.addAll(notices)
        val cachedDetail = delegate.cafeDetailsById[cafe.id]
        val detail = if (cachedDetail != null) {
            cachedDetail.copy(
                cafe = cafe,
                casts = casts,
                images = images ?: cachedDetail.images,
                notices = notices,
                businessHours = businessHours ?: cachedDetail.businessHours,
                phoneNumber = phoneNumber ?: cachedDetail.phoneNumber
            )
        } else {
            CafeDetail(
                cafe = cafe,
                images = images ?: listOfNotNull(cafe.thumbnailImage),
                casts = casts,
                menus = emptyList(),
                goods = emptyList(),
                notices = notices,
                businessHours = businessHours ?: "운영시간 정보 준비중",
                phoneNumber = phoneNumber ?: "연락처 정보 준비중"
            )
        }
        delegate.cafeDetailsById[cafe.id] = detail
    }

    private fun parseCafeDetailMetadata(document: JsonObject): CafeDetailMetadata {
        val fields = document["fields"]?.jsonObject
        val images = fields
            ?.getFirestoreStringList("galleryImages")
            ?.filter { image -> image.isNotBlank() }
            .orEmpty()
            .ifEmpty {
                fields?.getFirestoreStringList("images")
                    ?.filter { image -> image.isNotBlank() }
                    .orEmpty()
            }
        val businessHours = fields
            ?.getFirestoreString("businessHours")
            ?.trim()
            ?.takeIf { value -> value.isNotEmpty() }
        val phoneNumber = fields
            ?.getFirestoreString("phoneNumber")
            ?.trim()
            ?.takeIf { value -> value.isNotEmpty() }
        return CafeDetailMetadata(
            images = if (images.isEmpty()) null else images,
            businessHours = businessHours,
            phoneNumber = phoneNumber
        )
    }

    private fun formatBusinessHours(update: CafeInfoUpdate): String {
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
            weekday ->
                "평일 $weekdayOpen - $weekdayClose"
            weekend ->
                "주말 $weekendOpen - $weekendClose"
            else -> ""
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

private data class CafeDetailMetadata(
    val images: List<String>?,
    val businessHours: String?,
    val phoneNumber: String?
)

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

private fun JsonObject.getFirestoreStringList(key: String): List<String> {
    val valueObject = this[key]?.jsonObject ?: return emptyList()
    val arrayValue = valueObject["arrayValue"]?.jsonObject ?: return emptyList()
    val values = arrayValue["values"]?.jsonArray.orEmpty()
    return values.mapNotNull { element ->
        element.jsonObject["stringValue"]?.jsonPrimitive?.contentOrNull
    }
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

private fun firestoreStringArray(values: List<String>): JsonObject {
    val firestoreValues = values.map { value ->
        firestoreString(value)
    }
    return JsonObject(
        mapOf(
            "arrayValue" to JsonObject(
                mapOf(
                    "values" to kotlinx.serialization.json.JsonArray(firestoreValues)
                )
            )
        )
    )
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

private fun nextFirestoreEntityId(prefix: String): String {
    val now = Clock.System.now().toEpochMilliseconds()
    return "$prefix-$now"
}
