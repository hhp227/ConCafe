package com.hhp227.concafe.data.source.firestore

import com.hhp227.concafe.data.source.*
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.*
import com.hhp227.concafe.domain.util.isScheduleEndAfterStart
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.*

// ── Banner ───────────────────────────────────────────────────────────────────

class FirestoreBannerRemoteDataSource(
    config: FirestoreConfig,
    restApi: FirestoreRestApi,
    tokenProvider: FirestoreAuthTokenProvider
) : FirestoreBaseDataSource(config, restApi, tokenProvider), BannerRemoteDataSource {
    override suspend fun fetchHomeBanners(): List<HomeBanner> {
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        return runCatching { loadHomeBanners(idToken = idToken) }
            .recoverCatching { loadHomeBanners(idToken = null) }
            .getOrThrow()
    }
}

// ── Cafe ─────────────────────────────────────────────────────────────────────

class FirestoreCafeRemoteDataSource(
    config: FirestoreConfig,
    restApi: FirestoreRestApi,
    tokenProvider: FirestoreAuthTokenProvider
) : FirestoreBaseDataSource(config, restApi, tokenProvider), CafeRemoteDataSource {
    override suspend fun searchCafesRemote(
        query: String?,
        country: String?,
        city: String?,
        sort: CafeSort,
        cursor: String?,
        pageSize: Int
    ): PagedResult<Cafe> {
        val safePageSize = if (pageSize > 0) pageSize else 1
        val normalizedQuery = query?.trim()?.takeIf { it.isNotEmpty() }
        val normalizedCountry = country?.trim()?.takeIf { it.isNotEmpty() }
        val normalizedCity = city?.trim()?.takeIf { it.isNotEmpty() }
        val useServerApprovedFilter = sort != CafeSort.LATEST
        val queryBatchSize = safePageSize.coerceAtMost(50) * 3
        var nextCursorToken = cursor
        var exhausted = false
        val aggregated = mutableListOf<Cafe>()
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()

        while (aggregated.size < safePageSize && !exhausted) {
            val queryCursor = nextCursorToken
            val documents = runCatching {
                runCafeCollectionQuery(sort, queryCursor, queryBatchSize + 1, normalizedCountry, normalizedCity, useServerApprovedFilter, idToken)
            }.recoverCatching { firstError ->
                runCafeCollectionQuery(sort, queryCursor, queryBatchSize + 1, normalizedCountry, normalizedCity, useServerApprovedFilter, null)
            }.getOrElse { throwable ->
                throw IllegalStateException("Failed to search cafes: ${throwable.message}", throwable)
            }
            val batch = documents.take(queryBatchSize)
            val hasMoreBatch = documents.size > queryBatchSize
            val lastBatchDocument = batch.lastOrNull()
            val parsedWithDocs = batch.mapNotNull { document ->
                parseCafeDocument(document)?.let { cafe -> document to cafe }
            }
            val filteredWithDocs = parsedWithDocs.filter { (_, cafe) ->
                val matchesQuery = normalizedQuery == null || cafe.name.contains(normalizedQuery, ignoreCase = true)
                val matchesApproval = if (useServerApprovedFilter) true else cafe.approved
                matchesApproval && matchesQuery
            }
            val remaining = safePageSize - aggregated.size
            val takenWithDocs = filteredWithDocs.take(remaining)
            aggregated.addAll(takenWithDocs.map { (_, cafe) -> cafe })
            val hasMoreInBatch = takenWithDocs.size < filteredWithDocs.size
            nextCursorToken = if (hasMoreInBatch) {
                takenWithDocs.lastOrNull()?.first?.toCafeQueryCursor(sort) ?: lastBatchDocument?.toCafeQueryCursor(sort)
            } else {
                lastBatchDocument?.toCafeQueryCursor(sort)
            }
            exhausted = !hasMoreBatch && !hasMoreInBatch
        }
        println("--ConCafe--, searchCafesRemote aggregated $aggregated")
        return PagedResult(items = aggregated, nextCursor = if (exhausted) null else nextCursorToken, hasNext = !exhausted)
    }

    override suspend fun refreshCafeDetail(cafeId: String) {
        fetchCafeDetailSummaryRemoteInternal(cafeId)
    }

    override suspend fun fetchCafeDetail(cafeId: String): CafeDetail {
        return fetchCafeDetailFullRemoteInternal(cafeId) ?: run {
            throw NoSuchElementException("cafe detail not found")
        }
    }

    override suspend fun fetchCafeMenuGoods(cafeId: String): CafeMenuGoodsSection {
        return fetchCafeMenuGoodsRemoteInternal(cafeId) ?: run {
            throw NoSuchElementException("cafe menu goods not found")
        }
    }

    override suspend fun fetchCafeById(cafeId: String): Cafe? = fetchCafeByIdRemoteInternal(cafeId)

    override suspend fun fetchAllCafes(): List<Cafe> {
        val loaded = mutableListOf<Cafe>()
        var cursor: String? = null
        var hasNext = true

        while (hasNext) {
            val page = searchCafesRemote(null, null, null, CafeSort.LATEST, cursor, 200)
            loaded.addAll(page.items)
            cursor = page.nextCursor
            hasNext = page.hasNext && !cursor.isNullOrBlank()
        }
        return loaded.distinctBy { it.id }
    }

    override suspend fun updateCafeInfoRemote(update: CafeInfoUpdate): CafeDetail {
        val idToken = tokenProvider.getIdToken()
        val cafe = runCatching { parseCafeDocument(loadCafeDocument(cafeId = update.cafeId, idToken = idToken)) }
            .recoverCatching { parseCafeDocument(loadCafeDocument(cafeId = update.cafeId, idToken = null)) }
            .getOrNull() ?: throw NoSuchElementException("cafe not found")
        val currentDetail = fetchCafeDetailFullRemoteInternal(update.cafeId)
        val representativeImage = update.representativeImageUrl?.trim()?.takeIf { it.isNotEmpty() }
            ?: currentDetail?.images?.firstOrNull()
            ?: cafe.thumbnailImage
        val galleryImages = update.galleryImages.map { it.trim() }.filter { it.isNotEmpty() }
        val nextImages = buildList {
            representativeImage?.let { add(it) }
            addAll(galleryImages.filterNot { it == representativeImage })
        }
        val resolvedLocation = update.location ?: cafe.region.location
        val businessHours = formatBusinessHours(update)
        val phoneNumber = update.contactNumber.trim()
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFES}/${update.cafeId}" +
            "?updateMask.fieldPaths=name&updateMask.fieldPaths=desc&updateMask.fieldPaths=conceptType&updateMask.fieldPaths=thumbnailImage" +
            "&updateMask.fieldPaths=galleryImages&updateMask.fieldPaths=businessHours" +
            "&updateMask.fieldPaths=phoneNumber&updateMask.fieldPaths=region"
        val body = firestoreDocumentBody(
            mapOf(
                "name" to firestoreString(update.name.trim()),
                "desc" to firestoreString(update.description.trim()),
                "conceptType" to firestoreString(update.conceptType.trim().uppercase()),
                "thumbnailImage" to firestoreNullableString(representativeImage),
                "galleryImages" to firestoreStringArray(nextImages),
                "businessHours" to firestoreString(businessHours),
                "phoneNumber" to firestoreString(phoneNumber),
                "region" to firestoreMap(
                    mapOf(
                        "country" to firestoreString(cafe.region.country),
                        "city" to firestoreString(cafe.region.city),
                        "address" to firestoreString(update.address.trim()),
                        "location" to firestoreGeoPoint(resolvedLocation.latitude, resolvedLocation.longitude)
                    )
                )
            )
        )
        restApi.patch(path, body, idToken)
        return fetchCafeDetailFullRemoteInternal(update.cafeId) ?: throw NoSuchElementException("cafe detail not found")
    }

    override suspend fun upsertCafeMenuGoodsRemote(update: CafeMenuGoodsUpsert): CafeDetail {
        require(update.cafeId.isNotBlank()) { "cafeId is required" }
        require(update.name.isNotBlank()) { "name is required" }
        require(update.price >= 0) { "price must be zero or positive" }
        require(update.category.isNotBlank()) { "category is required" }

        val idToken = tokenProvider.getIdToken()
        val normalizedCategory = update.category.trim().lowercase()
        val isGoodsCategory = normalizedCategory == "goods"
        val itemId = update.itemId?.trim()?.takeIf { it.isNotEmpty() }
            ?: nextFirestoreEntityId(if (isGoodsCategory) "goods" else "menu")
        val menuPath = "${config.documentBasePath()}/${FirestorePaths.CAFES}/${update.cafeId}/${FirestorePaths.CAFE_MENUS}/$itemId"
        val goodsPath = "${config.documentBasePath()}/${FirestorePaths.CAFES}/${update.cafeId}/${FirestorePaths.CAFE_GOODS}/$itemId"
        val existingMenu = runCatching { parseMenuDocument(Json.parseToJsonElement(restApi.get(menuPath, idToken)).jsonObject) }
            .recoverCatching { parseMenuDocument(Json.parseToJsonElement(restApi.get(menuPath, null)).jsonObject) }
            .getOrNull()
        val existingGoods = runCatching { parseGoodsDocument(Json.parseToJsonElement(restApi.get(goodsPath, idToken)).jsonObject) }
            .recoverCatching { parseGoodsDocument(Json.parseToJsonElement(restApi.get(goodsPath, null)).jsonObject) }
            .getOrNull()
        val resolvedImage = update.imageUrl?.trim()?.takeIf { it.isNotEmpty() }
            ?: existingMenu?.image
            ?: existingGoods?.image

        if (isGoodsCategory) {
            val stock = if (update.isInStock) {
                if (existingGoods?.stock != null && existingGoods.stock > 0) existingGoods.stock else 50
            } else 0
            val body = firestoreDocumentBody(
                mapOf(
                    "name" to firestoreString(update.name.trim()),
                    "price" to firestoreLong(update.price.toLong()),
                    "image" to firestoreNullableString(resolvedImage),
                    "stock" to firestoreLong(stock.toLong())
                )
            )
            restApi.patch(goodsPath, body, idToken)
            if (existingMenu != null) runCatching { restApi.delete(menuPath, idToken) }
        } else {
            val body = firestoreDocumentBody(
                mapOf(
                    "name" to firestoreString(update.name.trim()),
                    "price" to firestoreLong(update.price.toLong()),
                    "desc" to firestoreString(update.description.trim()),
                    "image" to firestoreNullableString(resolvedImage),
                    "category" to firestoreString(normalizedCategory),
                    "isAvailable" to firestoreBoolean(update.isInStock)
                )
            )
            restApi.patch(menuPath, body, idToken)
            if (existingGoods != null) runCatching { restApi.delete(goodsPath, idToken) }
        }
        return fetchCafeDetailFullRemoteInternal(update.cafeId) ?: throw NoSuchElementException("cafe detail not found")
    }

    override suspend fun deleteCafeMenuGoodsRemote(cafeId: String, itemId: String): CafeDetail {
        require(cafeId.isNotBlank()) { "cafeId is required" }
        require(itemId.isNotBlank()) { "itemId is required" }

        val idToken = tokenProvider.getIdToken()
        val menuPath = "${config.documentBasePath()}/${FirestorePaths.CAFES}/$cafeId/${FirestorePaths.CAFE_MENUS}/$itemId"
        val goodsPath = "${config.documentBasePath()}/${FirestorePaths.CAFES}/$cafeId/${FirestorePaths.CAFE_GOODS}/$itemId"
        var deleted = false
        if (runCatching { restApi.delete(menuPath, idToken) }.isSuccess) deleted = true
        if (runCatching { restApi.delete(goodsPath, idToken) }.isSuccess) deleted = true
        if (!deleted) throw NoSuchElementException("menu goods item not found")
        return fetchCafeDetailFullRemoteInternal(cafeId) ?: throw NoSuchElementException("cafe detail not found")
    }

    override suspend fun refreshFavoriteCafeIds(userId: String) {
        fetchFavoriteCafeIdsRemote(userId)
    }

    override suspend fun fetchFavoriteCafeIds(userId: String, limit: Int?): List<String> {
        return fetchFavoriteCafeIdsRemote(userId, limit)
    }

    private suspend fun fetchFavoriteCafeIdsRemote(userId: String, limit: Int? = null): List<String> {
        val queryLimit = if (limit != null && limit > 0) limit else null
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        val tokenUserId = tokenProvider.getCurrentUserId()
        val primaryDocuments = runCatching {
            runUserScopedQuery(FirestorePaths.CAFE_FAVORITES, userId, idToken, limit = queryLimit)
        }.recoverCatching {
            runUserScopedQuery(FirestorePaths.CAFE_FAVORITES, userId, null, limit = queryLimit)
        }.getOrElse { error ->
            println("TEST, fetchFavoriteCafeIdsRemote query failed: userId=$userId tokenUserId=$tokenUserId message=${error.message}")
            emptyList()
        }
        val legacyFieldDocuments = if (primaryDocuments.isEmpty()) {
            runLegacyUserFieldQuery(FirestorePaths.CAFE_FAVORITES, userId, idToken, "uid")
        } else emptyList()
        val legacyNameDocuments = if (primaryDocuments.isEmpty() && legacyFieldDocuments.isEmpty()) {
            runDocumentNamePrefixQuery(FirestorePaths.CAFE_FAVORITES, "${sanitizeDocumentIdPart(userId)}_", idToken)
        } else emptyList()
        val favoriteDocuments = (primaryDocuments + legacyFieldDocuments + legacyNameDocuments)
            .distinctBy { it["name"]?.jsonPrimitive?.contentOrNull ?: it.toString() }
        val cafeIdsFromDocuments = favoriteDocuments
            .mapNotNull { it["fields"]?.jsonObject?.getFirestoreString("cafeId") }
            .distinct().sorted()
        val legacyProfileCafeIds = if (cafeIdsFromDocuments.isEmpty()) {
            loadUserIdListFromProfileDocument(userId, idToken, FAVORITE_CAFE_ID_FIELD_CANDIDATES)
        } else emptyList()
        return (cafeIdsFromDocuments + legacyProfileCafeIds)
            .filter { it.isNotBlank() }.distinct().sorted()
            .let { ids -> if (queryLimit != null) ids.take(queryLimit) else ids }
    }

    override suspend fun favoriteCafeRemote(userId: String, cafeId: String) {
        ensureAuthenticatedUserMatch(requestedUserId = userId, action = "favoriteCafeRemote")
        val idToken = tokenProvider.getIdToken()
        val favoriteId = buildCafeFavoriteDocumentId(userId, cafeId)
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFE_FAVORITES}/$favoriteId"
        val body = firestoreDocumentBody(
            mapOf(
                "userId" to firestoreString(userId),
                "cafeId" to firestoreString(cafeId),
                "createdAt" to firestoreString(Clock.System.now().toString())
            )
        )
        restApi.patch(path, body, idToken)
    }

    override suspend fun unfavoriteCafeRemote(userId: String, cafeId: String) {
        val idToken = tokenProvider.getIdToken()
        val favoriteId = buildCafeFavoriteDocumentId(userId, cafeId)
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFE_FAVORITES}/$favoriteId"
        restApi.delete(path, idToken)
    }

    override suspend fun refreshCafeReviews(cafeId: String) {
        val idToken = tokenProvider.getIdToken()
        runCatching {
            runFieldScopedQuery(FirestorePaths.REVIEWS, "cafeId", cafeId, idToken, orderByCreatedAtDesc = false)
        }.recoverCatching {
            runFieldScopedQuery(FirestorePaths.REVIEWS, "cafeId", cafeId, null, orderByCreatedAtDesc = false)
        }.getOrElse { throwable ->
            println("Firestore refreshCafeReviews failed: ${throwable.message}")
            return
        }.mapNotNull { parseReviewDocument(it) }.sortedByDescending { it.createdAt }
    }

    override suspend fun refreshCafeManagementData(userId: String) {
        fetchOwnedCafeIdsRemote(userId)
        fetchPendingCafeOwnerClaimsRemote(userId)
        fetchPendingCafeRegistrationClaimsRemote(userId)
    }

    override suspend fun fetchOwnedCafeIds(userId: String): Set<String> = fetchOwnedCafeIdsRemote(userId)

    private suspend fun fetchOwnedCafeIdsRemote(userId: String): Set<String> {
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        val ownerClaimDocuments = runCatching { runUserScopedQuery(FirestorePaths.CAFE_OWNER_CLAIMS, userId, idToken) }
            .recoverCatching { runUserScopedQuery(FirestorePaths.CAFE_OWNER_CLAIMS, userId, null) }
            .getOrElse { emptyList() }
        val registrationClaimDocuments = runCatching { runUserScopedQuery(FirestorePaths.CAFE_REGISTRATION_CLAIMS, userId, idToken) }
            .recoverCatching { runUserScopedQuery(FirestorePaths.CAFE_REGISTRATION_CLAIMS, userId, null) }
            .getOrElse { emptyList() }
        val directlyOwnedCafeDocuments = runCatching {
            runArrayContainsStringQuery(
                collectionId = FirestorePaths.CAFES,
                fieldPath = "ownerIds",
                fieldValue = userId,
                idToken = idToken
            )
        }.recoverCatching {
            runArrayContainsStringQuery(
                collectionId = FirestorePaths.CAFES,
                fieldPath = "ownerIds",
                fieldValue = userId,
                idToken = null
            )
        }.getOrElse { emptyList() }
        val ownedCafeIds = mutableSetOf<String>()
        ownerClaimDocuments.mapNotNull { parsePendingCafeOwnerClaimDocument(it) }.forEach { claim ->
            if (claim.status.isApprovedClaimStatus()) ownedCafeIds.add(claim.cafeId)
        }
        registrationClaimDocuments.forEach { document ->
            val fields = document["fields"]?.jsonObject
            val status = fields?.getFirestoreString("status").orEmpty()
            val approvedCafeId = fields?.getFirestoreString("approvedCafeId") ?: fields?.getFirestoreString("cafeId")
            if (status.isApprovedClaimStatus() && !approvedCafeId.isNullOrBlank()) ownedCafeIds.add(approvedCafeId)
        }
        directlyOwnedCafeDocuments.mapNotNull { parseCafeDocument(it)?.id }.forEach { cafeId ->
            ownedCafeIds.add(cafeId)
        }
        val userDocument = runCatching { loadUserDocument(userId, idToken) }
            .recoverCatching { loadUserDocument(userId, null) }
            .getOrNull()
        ownedCafeIds.addAll(userDocument?.get("fields")?.jsonObject?.getFirestoreStringList("ownedCafeIds").orEmpty())
        return ownedCafeIds
    }

    override suspend fun fetchPendingCafeOwnerClaims(userId: String): List<CafeManagementData.PendingClaimSummary> =
        fetchPendingCafeOwnerClaimsRemote(userId)

    private suspend fun fetchPendingCafeOwnerClaimsRemote(userId: String): List<CafeManagementData.PendingClaimSummary> {
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        val documents = runCatching { runUserScopedQuery(FirestorePaths.CAFE_OWNER_CLAIMS, userId, idToken) }
            .recoverCatching { runUserScopedQuery(FirestorePaths.CAFE_OWNER_CLAIMS, userId, null) }
            .getOrElse { emptyList() }
        return documents.mapNotNull { parsePendingCafeOwnerClaimDocument(it) }.sortedByDescending { it.requestedAt }
    }

    override suspend fun fetchPendingCafeRegistrationClaims(userId: String): List<CafeRegistrationClaim> =
        fetchPendingCafeRegistrationClaimsRemote(userId)

    private suspend fun fetchPendingCafeRegistrationClaimsRemote(userId: String): List<CafeRegistrationClaim> {
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        val documents = runCatching { runUserScopedQuery(FirestorePaths.CAFE_REGISTRATION_CLAIMS, userId, idToken) }
            .recoverCatching { runUserScopedQuery(FirestorePaths.CAFE_REGISTRATION_CLAIMS, userId, null) }
            .getOrElse { emptyList() }
        return documents.mapNotNull { parseCafeRegistrationClaimDocument(it) }.sortedByDescending { it.requestedAt }
    }

    override suspend fun fetchCafeTodayCheckInCount(cafeId: String): Int {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        return runCatching {
            loadCollectionDocumentCountByDatePrefix(FirestorePaths.VISITS, idToken, "cafeId", cafeId, "visitedAt", today)
        }.recoverCatching {
            loadCollectionDocumentCountByDatePrefix(FirestorePaths.VISITS, null, "cafeId", cafeId, "visitedAt", today)
        }.getOrThrow()
    }

    override suspend fun fetchCafeTodayReviewCount(cafeId: String): Int {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        return runCatching {
            loadCollectionDocumentCountByDatePrefix(FirestorePaths.REVIEWS, idToken, "cafeId", cafeId, "createdAt", today)
        }.recoverCatching {
            loadCollectionDocumentCountByDatePrefix(FirestorePaths.REVIEWS, null, "cafeId", cafeId, "createdAt", today)
        }.getOrThrow()
    }

    override suspend fun fetchCafeCheckInCount(cafeId: String): Int {
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        return runCatching {
            loadCollectionDocumentCount(FirestorePaths.VISITS, idToken, "cafeId", firestoreString(cafeId))
        }.recoverCatching {
            loadCollectionDocumentCount(FirestorePaths.VISITS, null, "cafeId", firestoreString(cafeId))
        }.getOrThrow()
    }

    override suspend fun fetchCafeHomeBannerPreview(cafeId: String): CafeDashboardData.HomeBannerPreview? {
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        val banners = runCatching { loadHomeBanners(idToken) }
            .recoverCatching { loadHomeBanners(null) }
            .getOrThrow()
        val targetBanner = banners.asSequence()
            .filter { it.cafeId == cafeId }
            .sortedWith(compareByDescending<HomeBanner> { it.statusLabel.uppercase() == "ACTIVE" }.thenByDescending { it.createdAtEpochMillis })
            .firstOrNull() ?: return null
        val statusLabel = when (targetBanner.statusLabel.uppercase()) {
            "ACTIVE" -> "노출 중"
            "SCHEDULED" -> "예약 중"
            else -> "미노출"
        }
        return CafeDashboardData.HomeBannerPreview(
            title = targetBanner.title,
            period = resolvePeriodLabel(targetBanner.displayDays),
            statusLabel = statusLabel,
            imageUrl = targetBanner.imageUrl
        )
    }

    override suspend fun fetchNoticeCount(cafeId: String): Int {
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        return runCatching {
            loadSubCollectionDocumentCount("${config.documentBasePath()}/${FirestorePaths.CAFES}/$cafeId", FirestorePaths.CAFE_NOTICES, idToken)
        }.recoverCatching {
            loadSubCollectionDocumentCount("${config.documentBasePath()}/${FirestorePaths.CAFES}/$cafeId", FirestorePaths.CAFE_NOTICES, null)
        }.getOrThrow()
    }

    override suspend fun updateCafeSocialMediaRemote(
        cafeId: String,
        instagramId: String?,
        twitterId: String?,
        tiktokId: String?,
        youtubeId: String?
    ) {
        val idToken = tokenProvider.getIdToken()
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFES}/$cafeId?updateMask.fieldPaths=socialMedia"
        val mapEntries = buildMap<String, JsonElement> {
            if (instagramId != null) put("instagram", firestoreString(instagramId))
            if (twitterId != null) put("twitter", firestoreString(twitterId))
            if (tiktokId != null) put("tiktok", firestoreString(tiktokId))
            if (youtubeId != null) put("youtube", firestoreString(youtubeId))
        }
        val socialMediaValue = firestoreMap(mapEntries)
        val body = firestoreDocumentBody(mapOf("socialMedia" to socialMediaValue))
        restApi.patch(path, body, idToken)
    }

    override suspend fun updateCafeReservationUrlRemote(cafeId: String, reservationUrl: String?) {
        val idToken = tokenProvider.getIdToken()
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFES}/$cafeId?updateMask.fieldPaths=reservationUrl"
        val body = firestoreDocumentBody(mapOf("reservationUrl" to firestoreNullableString(reservationUrl)))
        restApi.patch(path, body, idToken)
    }

    override suspend fun updateCafeTableCountsRemote(cafeId: String, current: Int, total: Int) {
        val idToken = tokenProvider.getIdToken()
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFES}/$cafeId?updateMask.fieldPaths=tableCounts"
        val tableCountsValue = firestoreMap(
            mapOf(
                "current" to firestoreLong(current.toLong()),
                "total" to firestoreLong(total.toLong())
            )
        )
        val body = firestoreDocumentBody(mapOf("tableCounts" to tableCountsValue))
        restApi.patch(path, body, idToken)
    }
}

// ── Cast ─────────────────────────────────────────────────────────────────────

class FirestoreCastRemoteDataSource(
    config: FirestoreConfig,
    restApi: FirestoreRestApi,
    tokenProvider: FirestoreAuthTokenProvider
) : FirestoreBaseDataSource(config, restApi, tokenProvider), CastRemoteDataSource {

    override suspend fun searchCastsRemote(
        query: String?,
        country: String?,
        city: String?,
        sort: CastSort,
        cursor: String?,
        pageSize: Int
    ): PagedResult<Cast> {
        val normalizedQuery = query?.trim()?.takeIf { it.isNotEmpty() }
        val normalizedCountry = country?.trim()?.takeIf { it.isNotEmpty() }
        val normalizedCity = city?.trim()?.takeIf { it.isNotEmpty() }
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        val targetCafeIds = loadCafeIdsByRegionRemote(normalizedCountry, normalizedCity, idToken)
        if (targetCafeIds != null && targetCafeIds.isEmpty()) {
            return PagedResult(items = emptyList(), nextCursor = null, hasNext = false)
        }
        val serverCafeFilterIds = if (
            targetCafeIds != null && targetCafeIds.isNotEmpty() &&
            targetCafeIds.size <= MAX_FIRESTORE_IN_FILTER_VALUES &&
            (sort == CastSort.POPULAR || sort == CastSort.FOLLOWERS)
        ) targetCafeIds.toList().sorted() else null
        val safePageSize = if (pageSize > 0) pageSize else 1
        val queryBatchSize = safePageSize.coerceAtMost(50) * 3
        var nextCursorToken = cursor
        var exhausted = false
        val aggregated = mutableListOf<Cast>()

        while (aggregated.size < safePageSize && !exhausted) {
            val queryCursor = nextCursorToken
            val documents = runCatching {
                runCastCollectionGroupQuery(sort, queryCursor, queryBatchSize + 1, serverCafeFilterIds, idToken)
            }.recoverCatching {
                runCastCollectionGroupQuery(sort, queryCursor, queryBatchSize + 1, serverCafeFilterIds, null)
            }.getOrElse { throwable ->
                throw IllegalStateException("Failed to search casts: ${throwable.message}", throwable)
            }
            val batch = documents.take(queryBatchSize)
            val hasMoreBatch = documents.size > queryBatchSize
            val lastBatchDocument = batch.lastOrNull()
            val parsedWithDocs = batch.mapNotNull { document ->
                val documentName = document["name"]?.jsonPrimitive?.contentOrNull
                val fields = document["fields"]?.jsonObject
                val cafeId = documentName?.toCafeIdFromCastDocumentName() ?: fields?.getFirestoreString("cafeId") ?: ""
                parseCastDocument(cafeId = cafeId, document = document)?.let { cast -> document to cast }
            }
            val filteredWithDocs = parsedWithDocs.filter { (_, cast) ->
                val matchesQuery = normalizedQuery == null || cast.name.contains(normalizedQuery, ignoreCase = true)
                val matchesCafe = if (serverCafeFilterIds != null) true else targetCafeIds?.contains(cast.cafeId) ?: true
                matchesQuery && matchesCafe
            }
            val remaining = safePageSize - aggregated.size
            val takenWithDocs = filteredWithDocs.take(remaining)
            aggregated.addAll(takenWithDocs.map { (_, cast) -> cast })
            val hasMoreInBatch = takenWithDocs.size < filteredWithDocs.size
            nextCursorToken = if (hasMoreInBatch) {
                takenWithDocs.lastOrNull()?.first?.toCastQueryCursor(sort) ?: lastBatchDocument?.toCastQueryCursor(sort)
            } else {
                lastBatchDocument?.toCastQueryCursor(sort)
            }
            exhausted = !hasMoreBatch && !hasMoreInBatch
        }
        return PagedResult(items = aggregated, nextCursor = if (exhausted) null else nextCursorToken, hasNext = !exhausted)
    }

    override suspend fun getHomePopularCastPageRemote(cursor: String?, pageSize: Int): PagedResult<Cast> {
        return searchCastsRemote(null, null, null, CastSort.HOME_LINKED_FIRST, cursor, pageSize)
    }

    override suspend fun fetchBirthdayCastsRemote(month: Int, dayOfMonth: Int, limit: Int): List<Cast> {
        val safeLimit = if (limit > 0) limit else 1
        val birthdayKey = monthDayToBirthdayKey(month, dayOfMonth)
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        val castDocuments = runCatching { runCastByBirthdayKeyQuery(birthdayKey, idToken, safeLimit) }
            .recoverCatching { runCastByBirthdayKeyQuery(birthdayKey, null, safeLimit) }
            .getOrElse { throwable -> throw IllegalStateException("Failed to load birthday casts", throwable) }
        return parseCollectionGroupCastDocuments(castDocuments)
            .filter { it.birthday.matchesMonthAndDay(month, dayOfMonth) }
            .take(safeLimit)
    }

    override suspend fun refreshCastDetailRemote(castId: String) {
        require(castId.isNotBlank()) { "castId is required" }
        fetchCastDetailRemoteById(castId)
    }

    override suspend fun fetchCastDetail(castId: String): CastDetail {
        require(castId.isNotBlank()) { "castId is required" }
        refreshCastDetailRemote(castId)
        return fetchCastDetailRemoteById(castId) ?: throw NoSuchElementException("cast detail not found")
    }

    private suspend fun fetchCastDetailRemoteById(castId: String): CastDetail? {
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        val cafeId = resolveCafeIdByCastId(castId, idToken) ?: return null
        return fetchCastDetailRemoteByCafeAndCastId(cafeId, castId, idToken)
    }

    override suspend fun getCafeCastPageRemote(cafeId: String, cursor: String?, pageSize: Int): PagedResult<Cast> {
        val safePageSize = if (pageSize > 0) pageSize else 1
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        val documents = runCatching { runCafeCastQuery(cafeId, cursor, safePageSize + 1, idToken) }
            .recoverCatching { runCafeCastQuery(cafeId, cursor, safePageSize + 1, null) }
            .getOrElse { throwable ->
                throw IllegalStateException("Failed to load cafe cast page($cafeId): ${throwable.message}", throwable)
            }
        val parsed = documents.mapNotNull { parseCastDocument(cafeId, it) }
        val hasNext = parsed.size > safePageSize
        val items = parsed.take(safePageSize)
        val nextCursorToken = if (hasNext) documents.getOrNull(safePageSize - 1)?.toCafeCastQueryCursor() else null
        return PagedResult(items = items, nextCursor = nextCursorToken, hasNext = hasNext)
    }

    override suspend fun upsertCastRemote(update: CastUpsert): CastDetail {
        require(update.name.isNotBlank()) { "cast name is required" }
        require(update.conceptRole.isNotBlank()) { "concept role is required" }

        val providedCastId = update.castId?.takeIf { it.isNotBlank() }
        val remoteExistingCast = providedCastId?.let { fetchCastsByIdsRemote(listOf(it)).firstOrNull() }
        val idToken = tokenProvider.getIdToken()
        val resolvedCafeId: String? = update.cafeId?.takeIf { it.isNotBlank() }
            ?: remoteExistingCast?.cafeId
            ?: if (providedCastId != null) resolveCafeIdByCastId(providedCastId, idToken) else null
        val targetCafeId = resolvedCafeId ?: throw IllegalArgumentException("cafeId is required")
        val existingCast = remoteExistingCast
        val targetCafeDocument = runCatching { loadCafeDocument(targetCafeId, idToken) }
            .recoverCatching { loadCafeDocument(targetCafeId, null) }
            .getOrThrow()
        parseCafeDocument(targetCafeDocument) ?: throw NoSuchElementException("cafe not found")
        val castId = existingCast?.id ?: update.castId?.takeIf { it.isNotBlank() } ?: nextFirestoreEntityId("cast")
        val normalizedName = update.name.trim()
        val normalizedConceptRole = update.conceptRole.trim()
        val normalizedIntroduction = update.introduction.trim()
        val normalizedBirthdaySource = update.birthday?.trim()?.takeIf { it.isNotEmpty() }
        val normalizedBirthday = normalizedBirthdaySource?.normalizeBirthdayToIsoOrNull() ?: normalizedBirthdaySource
        val normalizedBirthdayKey = normalizedBirthday.toBirthdayKeyOrNull()
        val normalizedProfileImage = update.profileImage?.trim()?.takeIf { it.isNotEmpty() } ?: existingCast?.profileImage
        val normalizedGalleryImages = update.galleryImages.map { it.trim() }.filter { it.isNotEmpty() }
        val linkedUserId = existingCast?.linkedUserId
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFES}/$targetCafeId/${FirestorePaths.CAFE_CASTS}/$castId"
        val body = firestoreDocumentBody(
            mapOf(
                "name" to firestoreString(normalizedName),
                "linkedUserId" to firestoreNullableString(linkedUserId),
                "profileImage" to firestoreNullableString(normalizedProfileImage),
                "desc" to firestoreString(normalizedIntroduction),
                "birthday" to firestoreNullableString(normalizedBirthday),
                BIRTHDAY_KEY_FIELD to firestoreNullableString(normalizedBirthdayKey),
                "conceptRole" to firestoreString(normalizedConceptRole),
                "followerCount" to firestoreLong((existingCast?.followerCount ?: 0).toLong()),
                "rating" to firestoreDouble(existingCast?.rating ?: 0.0),
                "visitCertificationCount" to firestoreLong((existingCast?.visitCertificationCount ?: 0).toLong()),
                "galleryImages" to firestoreStringArray(normalizedGalleryImages)
            )
        )
        runCatching { restApi.patch(path, body, idToken) }
            .onFailure { println("TEST, upsertCastRemote patch failed: ${it.message}") }
            .getOrThrow()
        runCatching { fetchCafeDetailSummaryRemoteInternal(targetCafeId) }
            .onFailure { println("TEST, upsertCastRemote refreshCafeDetail failed: ${it.message}") }
        return fetchCastDetailRemoteByCafeAndCastId(targetCafeId, castId, idToken)
            ?: throw NoSuchElementException("cast detail not found")
    }

    override suspend fun deleteCastRemote(castId: String): Cast {
        require(castId.isNotBlank()) { "castId is required" }
        val existingCast = fetchCastsByIdsRemote(listOf(castId)).firstOrNull()
            ?: throw NoSuchElementException("cast detail not found")
        val idToken = tokenProvider.getIdToken()
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFES}/${existingCast.cafeId}/${FirestorePaths.CAFE_CASTS}/$castId"
        restApi.delete(path, idToken)
        runCatching { fetchCafeDetailSummaryRemoteInternal(existingCast.cafeId) }
        return existingCast
    }

    override suspend fun refreshCastSchedulesRemote(castId: String, fromDate: String, toDate: String) {
        require(castId.isNotBlank()) { "castId is required" }
        val idToken = tokenProvider.getIdToken()
        runCastScheduleRangeQuery(castId, fromDate, toDate, idToken)
            .mapNotNull { parseCastScheduleDocument(it) }
            .filter { it.castId == castId && it.date >= fromDate && it.date <= toDate }
    }

    override suspend fun fetchCastSchedules(castId: String, fromDate: String, toDate: String): List<CastSchedule> {
        require(castId.isNotBlank()) { "castId is required" }
        return fetchCastSchedulesRemoteInternal(castId, fromDate, toDate, runCatching { tokenProvider.getIdToken() }.getOrNull())
    }

    override suspend fun fetchCastScheduleStatuses(
        castId: String,
        fromDate: String,
        toDate: String
    ): Map<String, CastScheduleStatus> {
        require(castId.isNotBlank()) { "castId is required" }
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        val documents = runCatching { runCastScheduleRangeQuery(castId, fromDate, toDate, idToken) }
            .recoverCatching { runCastScheduleRangeQuery(castId, fromDate, toDate, null) }
            .getOrElse { emptyList() }
        return documents.mapNotNull { document ->
            val entry = parseCastScheduleDocument(document) ?: return@mapNotNull null
            entry.date to entry.status
        }.toMap()
    }

    override suspend fun getWorkingCastIdsByCafeAndDate(cafeId: String, date: String): Set<String> {
        val idToken = tokenProvider.getIdToken()
        return runWorkingCastScheduleQuery(cafeId, date, idToken)
            .mapNotNull { parseCastScheduleDocument(it) }
            .filter { it.date == date && it.status == CastScheduleStatus.WORK }
            .map { it.castId }.toSet()
    }

    override suspend fun getWorkingCastSchedulesByCafeAndDate(cafeId: String, date: String): Map<String, CastSchedule> {
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        val scheduleDocuments = runCatching { runWorkingCastScheduleQuery(cafeId, date, idToken) }
            .recoverCatching { runWorkingCastScheduleQuery(cafeId, date, null) }
            .getOrElse { emptyList() }
        return scheduleDocuments.mapNotNull { parseWorkingCastScheduleDocument(it) }.associateBy { it.castId }
    }

    override suspend fun updateCastScheduleRemote(update: CastScheduleUpdate): CastSchedule? {
        require(update.castId.isNotBlank()) { "castId is required" }
        val idToken = tokenProvider.getIdToken()
        val currentUserId = tokenProvider.getCurrentUserId()?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("signed-in user is required")
        val resolvedCafeId = resolveCafeIdByCastId(update.castId, idToken)
            ?: throw NoSuchElementException("cast detail not found")
        val documentId = "${update.castId}_${update.date}"
        val schedulePath = "${config.documentBasePath()}/${FirestorePaths.CAST_SCHEDULES}/$documentId"
        val requestBatchId = update.requestBatchId?.trim()?.takeIf { it.isNotEmpty() }

        when (update.status) {
            CastScheduleStatus.WORK -> {
                val startTime = update.startTime?.trim()?.takeIf { it.isNotEmpty() }
                    ?: throw IllegalArgumentException("start time is required")
                val endTime = update.endTime?.trim()?.takeIf { it.isNotEmpty() }
                    ?: throw IllegalArgumentException("end time is required")
                require(isScheduleEndAfterStart(startTime, endTime)) { "end time must be after start time" }
                val scheduleBody = firestoreDocumentBody(
                    mapOf(
                        "castId" to firestoreString(update.castId),
                        "cafeId" to firestoreString(resolvedCafeId),
                        "date" to firestoreString(update.date),
                        "startTime" to firestoreString(startTime),
                        "endTime" to firestoreString(endTime),
                        "status" to firestoreString(CastScheduleStatus.WORK.name),
                        "createdBy" to firestoreString(currentUserId),
                        "requestBatchId" to firestoreNullableString(requestBatchId)
                    )
                )
                restApi.patch(schedulePath, scheduleBody, idToken)
            }
            CastScheduleStatus.OFF, CastScheduleStatus.VACATION -> {
                val scheduleBody = firestoreDocumentBody(
                    mapOf(
                        "castId" to firestoreString(update.castId),
                        "cafeId" to firestoreString(resolvedCafeId),
                        "date" to firestoreString(update.date),
                        "startTime" to firestoreNullableString(null),
                        "endTime" to firestoreNullableString(null),
                        "status" to firestoreString(update.status.name),
                        "createdBy" to firestoreString(currentUserId),
                        "requestBatchId" to firestoreNullableString(requestBatchId)
                    )
                )
                restApi.patch(schedulePath, scheduleBody, idToken)
            }
        }
        refreshCastSchedulesRemote(update.castId, update.date, update.date)
        return fetchCastSchedulesRemoteInternal(update.castId, update.date, update.date, idToken).firstOrNull()
    }

    override suspend fun fetchGuestCastSchedules(cafeId: String, fromDate: String, toDate: String): List<GuestCastSchedule> {
        require(cafeId.isNotBlank()) { "cafeId is required" }
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        val documents = runCatching { runGuestCastScheduleRangeQuery(cafeId, fromDate, toDate, idToken) }
            .recoverCatching { runGuestCastScheduleRangeQuery(cafeId, fromDate, toDate, null) }
            .getOrElse { emptyList() }
        return documents
            .mapNotNull { parseGuestCastScheduleDocument(it) }
            .filter { it.date in fromDate..toDate }
            .sortedWith(compareBy<GuestCastSchedule> { it.date }.thenBy { it.startTime }.thenBy { it.name })
    }

    override suspend fun upsertGuestCastScheduleRemote(input: GuestCastScheduleUpsert): GuestCastSchedule {
        val cafeId = input.cafeId.trim()
        val date = normalizeScheduleDateOrNull(input.date) ?: throw IllegalArgumentException("date is required")
        val name = input.name.trim().takeIf { it.isNotEmpty() } ?: throw IllegalArgumentException("guest name is required")
        val startTime = input.startTime.trim().takeIf { it.isNotEmpty() } ?: throw IllegalArgumentException("start time is required")
        val endTime = input.endTime.trim().takeIf { it.isNotEmpty() } ?: throw IllegalArgumentException("end time is required")
        require(cafeId.isNotBlank()) { "cafeId is required" }
        require(isScheduleEndAfterStart(startTime, endTime)) { "end time must be after start time" }

        val idToken = tokenProvider.getIdToken()
        val currentUserId = tokenProvider.getCurrentUserId()?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("signed-in user is required")
        val scheduleId = input.id?.trim()?.takeIf { it.isNotEmpty() } ?: nextFirestoreEntityId("guest-schedule")
        val path = "${config.documentBasePath()}/${FirestorePaths.GUEST_CAST_SCHEDULES}/$scheduleId"
        val now = Clock.System.now().toString()
        val body = firestoreDocumentBody(
            mapOf(
                "cafeId" to firestoreString(cafeId),
                "date" to firestoreString(date),
                "name" to firestoreString(name),
                "profileImage" to firestoreNullableString(input.profileImage?.trim()?.takeIf { it.isNotEmpty() }),
                "startTime" to firestoreString(startTime),
                "endTime" to firestoreString(endTime),
                "memo" to firestoreNullableString(input.memo?.trim()?.takeIf { it.isNotEmpty() }),
                "createdBy" to firestoreString(currentUserId),
                "updatedAt" to firestoreString(now)
            )
        )

        restApi.patch(path, body, idToken)
        return GuestCastSchedule(
            id = scheduleId,
            cafeId = cafeId,
            date = date,
            name = name,
            profileImage = input.profileImage?.trim()?.takeIf { it.isNotEmpty() },
            startTime = startTime,
            endTime = endTime,
            memo = input.memo?.trim()?.takeIf { it.isNotEmpty() }
        )
    }

    override suspend fun deleteGuestCastScheduleRemote(scheduleId: String) {
        require(scheduleId.isNotBlank()) { "scheduleId is required" }
        val idToken = tokenProvider.getIdToken()
        val path = "${config.documentBasePath()}/${FirestorePaths.GUEST_CAST_SCHEDULES}/${scheduleId.trim()}"
        restApi.delete(path, idToken)
    }

    override suspend fun refreshFollowedCastIds(userId: String) {
        fetchFollowedCastIdsRemote(userId)
    }

    override suspend fun fetchFollowedCastIds(userId: String): List<String> = fetchFollowedCastIdsRemote(userId)

    private suspend fun fetchFollowedCastIdsRemote(userId: String): List<String> {
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        val tokenUserId = tokenProvider.getCurrentUserId()
        val primaryFollowDocuments = runCatching {
            runUserScopedQuery(FirestorePaths.CAST_FOLLOWS, userId, idToken)
        }.recoverCatching {
            runUserScopedQuery(FirestorePaths.CAST_FOLLOWS, userId, null)
        }.getOrElse { error ->
            println("TEST, fetchFollowedCastIdsRemote query failed: userId=$userId tokenUserId=$tokenUserId message=${error.message}")
            emptyList()
        }
        val legacyFieldDocuments = if (primaryFollowDocuments.isEmpty()) {
            runLegacyUserFieldQuery(FirestorePaths.CAST_FOLLOWS, userId, idToken, "uid")
        } else emptyList()
        val legacyNameDocuments = if (primaryFollowDocuments.isEmpty() && legacyFieldDocuments.isEmpty()) {
            runDocumentNamePrefixQuery(FirestorePaths.CAST_FOLLOWS, "${sanitizeDocumentIdPart(userId)}_", idToken)
        } else emptyList()
        val followDocuments = (primaryFollowDocuments + legacyFieldDocuments + legacyNameDocuments)
            .distinctBy { it["name"]?.jsonPrimitive?.contentOrNull ?: it.toString() }
        val castIdsFromDocuments = followDocuments
            .mapNotNull { it["fields"]?.jsonObject?.getFirestoreString("castId") }
            .distinct().sorted()
        val legacyProfileCastIds = if (castIdsFromDocuments.isEmpty()) {
            loadUserIdListFromProfileDocument(userId, idToken, FOLLOWED_CAST_ID_FIELD_CANDIDATES)
        } else emptyList()
        return (castIdsFromDocuments + legacyProfileCastIds).filter { it.isNotBlank() }.distinct().sorted()
    }

    override suspend fun getFollowedCastsRemote(userId: String): List<Cast> {
        if (userId.isBlank()) return emptyList()
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        val followDocuments = runCatching { runUserScopedQuery(FirestorePaths.CAST_FOLLOWS, userId, idToken) }
            .recoverCatching { runUserScopedQuery(FirestorePaths.CAST_FOLLOWS, userId, null) }
            .getOrElse { emptyList() }
        val followedCastIds = mutableListOf<String>()
        val followsByCafeId = mutableMapOf<String, MutableList<String>>()
        followDocuments.forEach { document ->
            val fields = document["fields"]?.jsonObject ?: return@forEach
            val castId = fields.getFirestoreString("castId")?.takeIf { it.isNotBlank() } ?: return@forEach
            val cafeId = fields.getFirestoreString("cafeId")?.takeIf { it.isNotBlank() } ?: return@forEach
            followedCastIds.add(castId)
            followsByCafeId.getOrPut(cafeId) { mutableListOf() }.add(castId)
        }
        if (followsByCafeId.isEmpty()) {
            return fetchCastsByIdsRemote(followedCastIds.toSet().toList())
        }
        val resolvedCasts = mutableListOf<Cast>()
        followsByCafeId.forEach { (cafeId, castIds) ->
            castIds.distinct().chunked(MAX_FIRESTORE_IN_FILTER_VALUES).forEach { chunk ->
                val documents = runCatching { runCafeCastIdsInQuery(cafeId, chunk, idToken) }
                    .recoverCatching { runCafeCastIdsInQuery(cafeId, chunk, null) }
                    .getOrElse { emptyList() }
                resolvedCasts.addAll(documents.mapNotNull { parseCastDocument(cafeId, it) })
            }
        }
        val idSet = followedCastIds.toSet()
        return if (resolvedCasts.isNotEmpty()) {
            val resolvedDistinct = resolvedCasts.distinctBy { it.id }
            val unresolvedIds = idSet - resolvedDistinct.map { it.id }.toSet()
            if (unresolvedIds.isEmpty()) resolvedDistinct
            else (resolvedDistinct + fetchCastsByIdsRemote(unresolvedIds.toList())).distinctBy { it.id }
        } else {
            fetchCastsByIdsRemote(idSet.toList())
        }
    }

    override suspend fun refreshCastByLinkedUserId(userId: String): Cast? {
        require(userId.isNotBlank()) { "userId is required" }
        val idToken = tokenProvider.getIdToken()
        val documents = resolveCastDocumentsByUser(userId, idToken)
        val firstDocument = documents.firstOrNull() ?: return null
        val name = firstDocument["name"]?.jsonPrimitive?.contentOrNull ?: return null
        val cafeId = extractCafeIdFromCastDocumentName(name) ?: return null
        println("TEST, refreshCastByLinkedUserId result: userId=$userId documents=${documents.size} cafeId=$cafeId")
        return parseCastDocument(cafeId, firstDocument)
    }

    override suspend fun fetchCastByLinkedUserId(userId: String): Cast? = refreshCastByLinkedUserId(userId)

    override suspend fun refreshCafeCastsRemote(cafeId: String) {
        require(cafeId.isNotBlank()) { "cafeId is required" }
        fetchCafeCastsByCafeIdRemote(cafeId)
    }

    override suspend fun fetchCafeCasts(cafeId: String): List<Cast> = fetchCafeCastsByCafeIdRemote(cafeId)

    private suspend fun fetchCafeCastsByCafeIdRemote(cafeId: String): List<Cast> {
        val loaded = mutableListOf<Cast>()
        var cursor: String? = null
        var hasNext = true

        while (hasNext) {
            val page = getCafeCastPageRemote(cafeId, cursor, 200)
            loaded.addAll(page.items)
            cursor = page.nextCursor
            hasNext = page.hasNext && !cursor.isNullOrBlank()
        }
        return loaded.distinctBy { it.id }
    }

    override suspend fun fetchCafeCastCount(cafeId: String): Int {
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        return runCatching {
            loadSubCollectionDocumentCount("${config.documentBasePath()}/${FirestorePaths.CAFES}/$cafeId", FirestorePaths.CAFE_CASTS, idToken)
        }.recoverCatching {
            loadSubCollectionDocumentCount("${config.documentBasePath()}/${FirestorePaths.CAFES}/$cafeId", FirestorePaths.CAFE_CASTS, null)
        }.getOrThrow()
    }

    override suspend fun fetchCastsByIds(castIds: List<String>): List<Cast> = fetchCastsByIdsRemote(castIds)

    private suspend fun fetchCastsByIdsRemote(castIds: List<String>): List<Cast> {
        val targetIds = castIds.asSequence().map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        if (targetIds.isEmpty()) return emptyList()
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        val resolved = mutableListOf<Cast>()
        targetIds.forEach { castId ->
            val cafeId = resolveCafeIdByCastId(castId, idToken) ?: return@forEach
            val cast = resolveCafeCastById(cafeId, castId, idToken) ?: return@forEach
            resolved.add(cast)
        }
        return resolved.distinctBy { it.id }
    }

    override suspend fun fetchAllCasts(): List<Cast> {
        val loaded = mutableListOf<Cast>()
        var cursor: String? = null
        var hasNext = true

        while (hasNext) {
            val page = searchCastsRemote(null, null, null, CastSort.LATEST, cursor, 200)
            loaded.addAll(page.items)
            cursor = page.nextCursor
            hasNext = page.hasNext && !cursor.isNullOrBlank()
        }
        return loaded.distinctBy { it.id }
    }

    override suspend fun fetchAffiliatedCafeId(userId: String): String? = fetchAffiliatedCafeIdRemote(userId)

    override suspend fun setAffiliatedCafeId(userId: String, cafeId: String) {
        val idToken = tokenProvider.getIdToken()
        val path = "${config.documentBasePath()}/${FirestorePaths.USERS}/$userId?updateMask.fieldPaths=affiliatedCafeId"
        val body = firestoreDocumentBody(mapOf("affiliatedCafeId" to firestoreString(cafeId)))

        restApi.patch(path, body, idToken)
    }

    override suspend fun clearAffiliatedCafeId(userId: String) {
        val idToken = tokenProvider.getIdToken()
        val path = "${config.documentBasePath()}/${FirestorePaths.USERS}/$userId?updateMask.fieldPaths=affiliatedCafeId"
        val body = firestoreDocumentBody(mapOf("affiliatedCafeId" to firestoreNullableString(null)))

        restApi.patch(path, body, idToken)
    }

    override suspend fun followCastRemote(userId: String, castId: String) {
        ensureAuthenticatedUserMatch(requestedUserId = userId, action = "followCastRemote")
        val idToken = tokenProvider.getIdToken()
        val cast = fetchCastsByIdsRemote(listOf(castId)).firstOrNull() ?: throw NoSuchElementException("cast not found")
        val followId = buildCastFollowDocumentId(userId, castId)
        val path = "${config.documentBasePath()}/${FirestorePaths.CAST_FOLLOWS}/$followId"
        val createdAt = Clock.System.now().toString()
        val follower = runCatching { parseUserDocument(loadUserDocument(userId, idToken)) }
            .recoverCatching { parseUserDocument(loadUserDocument(userId, null)) }
            .getOrNull()
        val body = firestoreDocumentBody(
            mapOf(
                "userId" to firestoreString(userId),
                "castId" to firestoreString(castId),
                "cafeId" to firestoreString(cast.cafeId),
                "userNickname" to firestoreNullableString(follower?.nickname),
                "userProfileImage" to firestoreNullableString(follower?.profileImage),
                "createdAt" to firestoreString(createdAt)
            )
        )

        restApi.patch(path, body, idToken)
    }

    override suspend fun unfollowCastRemote(userId: String, castId: String) {
        val idToken = tokenProvider.getIdToken()
        val followId = buildCastFollowDocumentId(userId, castId)
        val path = "${config.documentBasePath()}/${FirestorePaths.CAST_FOLLOWS}/$followId"

        restApi.delete(path, idToken)
    }

    override suspend fun refreshFollowerUserIds(castId: String) {
        getCastFollowerSnapshots(castId)
    }

    override suspend fun fetchFollowerUserIds(castId: String): List<String> {
        return getCastFollowerSnapshots(castId).map { it.userId }.distinct().sorted()
    }

    override suspend fun getCastFollowerSnapshots(castId: String): List<CastFollowerSnapshot> {
        val idToken = tokenProvider.getIdToken()
        return runCastFollowerQuery(castId, idToken, orderByCreatedAtDesc = true, limit = RECENT_FOLLOWER_FETCH_LIMIT)
            .mapNotNull { parseCastFollowDocument(it) }
            .filter { it.userId.isNotBlank() }
            .sortedByDescending { it.followedAt }
    }
}

// ── CastClaim ─────────────────────────────────────────────────────────────────

class FirestoreCastClaimRemoteDataSource(
    config: FirestoreConfig,
    restApi: FirestoreRestApi,
    tokenProvider: FirestoreAuthTokenProvider
) : FirestoreBaseDataSource(config, restApi, tokenProvider), CastClaimRemoteDataSource {
    override suspend fun refreshCastClaimsForUser(userId: String) {
        fetchCastClaimsForUserRemote(userId)
    }

    override suspend fun fetchCastClaimsForUser(userId: String): List<CastClaim> = fetchCastClaimsForUserRemote(userId)

    private suspend fun fetchCastClaimsForUserRemote(userId: String): List<CastClaim> {
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        val documents = runCatching { runUserScopedQuery(FirestorePaths.CAST_CLAIMS, userId, idToken) }
            .recoverCatching { runUserScopedQuery(FirestorePaths.CAST_CLAIMS, userId, null) }
            .getOrElse { emptyList() }
        return documents.mapNotNull { parseCastClaimDocument(it) }.sortedByDescending { it.createdAt }
    }

    override suspend fun refreshCastClaimsForCafe(cafeId: String) {
        fetchCastClaimsForCafeRemote(cafeId)
    }

    override suspend fun fetchCastClaimsForCafe(cafeId: String): List<CastClaim> = fetchCastClaimsForCafeRemote(cafeId)

    private suspend fun fetchCastClaimsForCafeRemote(cafeId: String): List<CastClaim> {
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        val documents = runCatching {
            runFieldScopedQuery(FirestorePaths.CAST_CLAIMS, "cafeId", cafeId, idToken, orderByCreatedAtDesc = false)
        }.recoverCatching {
            runFieldScopedQuery(FirestorePaths.CAST_CLAIMS, "cafeId", cafeId, null, orderByCreatedAtDesc = false)
        }.getOrElse { emptyList() }
        return documents.mapNotNull { parseCastClaimDocument(it) }.sortedByDescending { it.createdAt }
    }

    override suspend fun fetchAllCastClaims(): List<CastClaim> {
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        val documents = runCatching { runCollectionQuery(FirestorePaths.CAST_CLAIMS, idToken) }
            .recoverCatching { runCollectionQuery(FirestorePaths.CAST_CLAIMS, null) }
            .getOrElse { emptyList() }
        return documents.mapNotNull { parseCastClaimDocument(it) }.sortedByDescending { it.createdAt }
    }

    override suspend fun hasCastClaimCafeSyncChanged(cafeId: String): Boolean {
        require(cafeId.isNotBlank()) { "cafeId is required" }
        return true
    }

    override suspend fun createCastClaimRemote(
        userId: String,
        cafeId: String,
        castId: String,
        message: String?
    ): CastClaim {
        // Resolve cast name using the base class resolveCafeCastById
        val idToken = tokenProvider.getIdToken()
        val castName = runCatching {
            resolveCafeCastById(cafeId, castId, idToken)?.name
        }.recoverCatching {
            resolveCafeCastById(cafeId, castId, null)?.name
        }.getOrNull() ?: castId

        val claimId = nextFirestoreEntityId("cast-claim")
        val createdAt = Clock.System.now().toString()
        val normalizedMessage = message?.trim()?.takeIf { it.isNotEmpty() }
        val requester = parseUserDocument(loadUserDocument(userId, idToken))
            ?: throw NoSuchElementException("user not found")
        val requesterNickname = requester.nickname.trim().takeIf { it.isNotEmpty() }
        val requesterProfileImage = requester.profileImage?.trim()?.takeIf { it.isNotEmpty() }
        val path = "${config.documentBasePath()}/${FirestorePaths.CAST_CLAIMS}/$claimId"
        val body = firestoreDocumentBody(
            mapOf(
                "userId" to firestoreString(userId),
                "cafeId" to firestoreString(cafeId),
                "castId" to firestoreString(castId),
                "castName" to firestoreString(castName),
                "requesterNickname" to firestoreNullableString(requesterNickname),
                "requesterProfileImage" to firestoreNullableString(requesterProfileImage),
                "status" to firestoreString(CastClaimStatus.PENDING.name),
                "message" to firestoreNullableString(normalizedMessage),
                "evidenceImageUrls" to firestoreStringArray(emptyList()),
                "createdAt" to firestoreString(createdAt),
                "createdAtLabel" to firestoreString("방금 전")
            )
        )
        restApi.patch(path, body, idToken)
        return CastClaim(
            id = claimId,
            userId = userId,
            cafeId = cafeId,
            castId = castId,
            castName = castName,
            requesterNickname = requesterNickname,
            requesterProfileImage = requesterProfileImage,
            status = CastClaimStatus.PENDING,
            message = normalizedMessage,
            evidenceImageUrls = emptyList(),
            reviewedBy = null,
            reviewedAt = null,
            createdAt = createdAt,
            createdAtLabel = "방금 전"
        )
    }

    override suspend fun updateCastClaimStatusRemote(
        claimId: String,
        reviewedBy: String,
        status: CastClaimStatus
    ): CastClaim {
        val idToken = tokenProvider.getIdToken()
        val existing = resolveCastClaimById(claimId, idToken) ?: throw NoSuchElementException("claim not found")
        val reviewedAt = Clock.System.now().toString()
        val path = "${config.documentBasePath()}/${FirestorePaths.CAST_CLAIMS}/$claimId" +
            "?updateMask.fieldPaths=status&updateMask.fieldPaths=reviewedBy&updateMask.fieldPaths=reviewedAt"
        val body = firestoreDocumentBody(
            mapOf(
                "status" to firestoreString(status.name),
                "reviewedBy" to firestoreString(reviewedBy),
                "reviewedAt" to firestoreString(reviewedAt)
            )
        )

        restApi.patch(path, body, idToken)
        return existing.copy(status = status, reviewedBy = reviewedBy, reviewedAt = reviewedAt)
    }
}

// ── Inquiry ───────────────────────────────────────────────────────────────────

class FirestoreInquiryRemoteDataSource(
    config: FirestoreConfig,
    restApi: FirestoreRestApi,
    tokenProvider: FirestoreAuthTokenProvider
) : FirestoreBaseDataSource(config, restApi, tokenProvider), InquiryRemoteDataSource {
    override suspend fun createInquiry(
        userId: String,
        userNickname: String,
        input: InquiryCreate
    ): Inquiry {
        val idToken = tokenProvider.getIdToken()
        val inquiryId = nextFirestoreEntityId("inquiry")
        val createdAt = Clock.System.now().toString()
        val normalizedInquiryType = input.inquiryType.trim()
        val normalizedTitle = input.title.trim()
        val normalizedContent = input.content.trim()
        val body = firestoreDocumentBody(
            mapOf(
                "userId" to firestoreString(userId),
                "userNickname" to firestoreString(userNickname),
                "inquiryType" to firestoreString(normalizedInquiryType),
                "title" to firestoreString(normalizedTitle),
                "content" to firestoreString(normalizedContent),
                "status" to firestoreString(InquiryStatus.PENDING.name),
                "createdAt" to firestoreString(createdAt),
                "createdAtLabel" to firestoreString("방금 전")
            )
        )
        val path = "${config.documentBasePath()}/${FirestorePaths.INQUIRIES}/$inquiryId"

        restApi.patch(path, body, idToken)
        return Inquiry(
            id = inquiryId,
            userId = userId,
            userNickname = userNickname,
            inquiryType = normalizedInquiryType,
            title = normalizedTitle,
            content = normalizedContent,
            status = InquiryStatus.PENDING,
            createdAt = createdAt,
            createdAtLabel = "방금 전"
        )
    }

    override suspend fun fetchInquiryPage(cursor: String?, pageSize: Int): PagedResult<Inquiry> {
        val safePageSize = pageSize.coerceAtLeast(1)
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        val documents = runCatching { runInquiryPageQuery(cursor, safePageSize + 1, idToken) }
            .recoverCatching { runInquiryPageQuery(cursor, safePageSize + 1, null) }
            .getOrElse { throwable ->
                throw IllegalStateException("Failed to load inquiry page", throwable)
            }
        val pageDocuments = documents.take(safePageSize)
        val hasNext = documents.size > safePageSize
        val nextCursorToken = if (hasNext) {
            pageDocuments.lastOrNull()?.get("fields")?.jsonObject?.getFirestoreString("createdAt")
        } else null
        return PagedResult(
            items = pageDocuments.mapNotNull { parseInquiryDocument(it) },
            nextCursor = nextCursorToken,
            hasNext = hasNext
        )
    }
}

class FirestoreReportRemoteDataSource(
    config: FirestoreConfig,
    restApi: FirestoreRestApi,
    tokenProvider: FirestoreAuthTokenProvider
) : FirestoreBaseDataSource(config, restApi, tokenProvider), ReportRemoteDataSource {
    override suspend fun createReport(
        reporterUserId: String,
        reporterNickname: String,
        input: ReportCreate
    ): Report {
        val idToken = tokenProvider.getIdToken()
        val reportId = nextFirestoreEntityId("report")
        val createdAt = Clock.System.now().toString()
        val normalizedType = input.reportType.trim()
        val path = "${config.documentBasePath()}/${FirestorePaths.REPORTS}/$reportId"
        val body = firestoreDocumentBody(
            mapOf(
                "targetType" to firestoreString(input.targetType.name),
                "targetId" to firestoreString(input.targetId.trim()),
                "reporterUserId" to firestoreString(reporterUserId),
                "reporterNickname" to firestoreString(reporterNickname),
                "reportType" to firestoreString(normalizedType),
                "status" to firestoreString(ReportStatus.PENDING.name),
                "createdAt" to firestoreString(createdAt),
                "createdAtLabel" to firestoreString("방금 전")
            )
        )
        restApi.patch(path, body, idToken)
        return Report(
            id = reportId,
            targetType = input.targetType,
            targetId = input.targetId.trim(),
            reporterUserId = reporterUserId,
            reporterNickname = reporterNickname,
            reportType = normalizedType,
            status = ReportStatus.PENDING,
            createdAt = createdAt,
            createdAtLabel = "방금 전"
        )
    }

    override suspend fun fetchReportPage(cursor: String?, pageSize: Int): PagedResult<Report> {
        val safePageSize = pageSize.coerceAtLeast(1)
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        val documents = runCatching { runReportPageQuery(cursor, safePageSize + 1, idToken) }
            .recoverCatching { runReportPageQuery(cursor, safePageSize + 1, null) }
            .getOrElse { throwable ->
                throw IllegalStateException("Failed to load report page", throwable)
            }
        val pageDocuments = documents.take(safePageSize)
        val hasNext = documents.size > safePageSize
        val nextCursorToken = if (hasNext) {
            pageDocuments.lastOrNull()?.get("fields")?.jsonObject?.getFirestoreString("createdAt")
        } else null
        return PagedResult(
            items = pageDocuments.mapNotNull { parseReportDocument(it) },
            nextCursor = nextCursorToken,
            hasNext = hasNext
        )
    }
}

class FirestoreUserBlockRemoteDataSource(
    config: FirestoreConfig,
    restApi: FirestoreRestApi,
    tokenProvider: FirestoreAuthTokenProvider
) : FirestoreBaseDataSource(config, restApi, tokenProvider), UserBlockRemoteDataSource {
    override suspend fun createUserBlock(
        blockerUserId: String,
        blockerNickname: String,
        input: UserBlockCreate
    ): UserBlock {
        val idToken = tokenProvider.getIdToken()
        val normalizedBlockerUserId = blockerUserId.trim()
        val normalizedBlockedUserId = input.blockedUserId.trim()
        val blockId = "${normalizedBlockerUserId}_$normalizedBlockedUserId"
        val createdAt = Clock.System.now().toString()
        val path = "${config.documentBasePath()}/${FirestorePaths.USER_BLOCKS}/$blockId"
        val body = firestoreDocumentBody(
            mapOf(
                "blockerUserId" to firestoreString(normalizedBlockerUserId),
                "blockerNickname" to firestoreString(blockerNickname.trim()),
                "blockedUserId" to firestoreString(normalizedBlockedUserId),
                "blockedNickname" to firestoreString(input.blockedNickname.trim()),
                "createdAt" to firestoreString(createdAt)
            )
        )

        restApi.patch(path, body, idToken)
        return UserBlock(
            id = blockId,
            blockerUserId = normalizedBlockerUserId,
            blockerNickname = blockerNickname.trim(),
            blockedUserId = normalizedBlockedUserId,
            blockedNickname = input.blockedNickname.trim(),
            createdAt = createdAt
        )
    }
}

// ── Notice ────────────────────────────────────────────────────────────────────

class FirestoreNoticeRemoteDataSource(
    config: FirestoreConfig,
    restApi: FirestoreRestApi,
    tokenProvider: FirestoreAuthTokenProvider
) : FirestoreBaseDataSource(config, restApi, tokenProvider), NoticeRemoteDataSource {
    override suspend fun fetchRecentNotices(limit: Int): List<Notice> {
        val safeLimit = if (limit > 0) limit else 1
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        val noticeDocuments = runCatching { runRecentNoticeFeedQuery(safeLimit, idToken) }
            .recoverCatching { runRecentNoticeFeedQuery(safeLimit, null) }
            .getOrElse { throwable ->
                throw IllegalStateException("Failed to load recent notices: ${throwable.message}", throwable)
            }

        if (noticeDocuments.isEmpty()) return emptyList()
        return noticeDocuments.mapNotNull { document ->
            val documentName = document["name"]?.jsonPrimitive?.contentOrNull
            val fields = document["fields"]?.jsonObject
            val cafeId = documentName?.toCafeIdFromNoticeDocumentName()
                ?: fields?.getFirestoreString("cafeId")
                ?: return@mapNotNull null
            val cafeName = fields?.getFirestoreString("cafeName")?.trim()?.takeIf { it.isNotEmpty() } ?: cafeId
            parseNoticeDocument(cafeId, cafeName, document)
        }.sortedByDescending { it.createdAt }
    }

    override suspend fun fetchCafeNoticePage(
        cafeId: String,
        query: String,
        cursor: String?,
        pageSize: Int
    ): PagedResult<CafeNoticeManagementItem> {
        val safePageSize = if (pageSize > 0) pageSize else 1
        val normalizedQuery = query.trim()
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        val documents = runCatching { runCafeNoticePageQuery(cafeId, cursor, safePageSize + 1, idToken) }
            .recoverCatching { runCafeNoticePageQuery(cafeId, cursor, safePageSize + 1, null) }
            .getOrElse { throwable ->
                throw IllegalStateException("Failed to load cafe notice page($cafeId): ${throwable.message}", throwable)
            }
        val pageDocuments = documents.take(safePageSize)
        val hasNext = documents.size > safePageSize
        val nextCursorToken = if (hasNext) pageDocuments.lastOrNull()?.toNoticeQueryCursor() else null
        val pageItems = pageDocuments.mapNotNull { parseNoticeManagementDocument(cafeId, it) }
            .filter { item ->
                normalizedQuery.isBlank() ||
                    item.title.contains(normalizedQuery, ignoreCase = true) ||
                    item.content.contains(normalizedQuery, ignoreCase = true)
            }
        return PagedResult(items = pageItems, nextCursor = nextCursorToken, hasNext = hasNext)
    }

    override suspend fun fetchCafeEventPage(
        cafeId: String,
        query: String,
        cursor: String?,
        pageSize: Int
    ): PagedResult<CafeEventManagementItem> {
        val safePageSize = if (pageSize > 0) pageSize else 1
        val normalizedQuery = query.trim()
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        val documents = runCatching { runCafeEventPageQuery(cafeId, cursor, safePageSize + 1, idToken) }
            .recoverCatching { runCafeEventPageQuery(cafeId, cursor, safePageSize + 1, null) }
            .getOrElse { throwable ->
                throw IllegalStateException("Failed to load cafe event page: $cafeId", throwable)
            }
        val pageDocuments = documents.take(safePageSize)
        val hasNext = documents.size > safePageSize
        val nextCursorToken = if (hasNext) pageDocuments.lastOrNull()?.toEventQueryCursor() else null
        val pageItems = pageDocuments.mapNotNull { parseEventManagementDocument(cafeId, it) }
            .filter { item ->
                normalizedQuery.isBlank() ||
                    item.title.contains(normalizedQuery, ignoreCase = true) ||
                    item.content.contains(normalizedQuery, ignoreCase = true)
            }
        return PagedResult(items = pageItems, nextCursor = nextCursorToken, hasNext = hasNext)
    }

    override suspend fun fetchHomeCafeEventPage(
        cursor: String?,
        pageSize: Int
    ): PagedResult<CafeEventManagementItem> {
        val safePageSize = if (pageSize > 0) pageSize else 1
        val todayText = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
            .toString()
            .replace("-", ".")
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        val documents = runCatching { runHomeCafeEventFeedQuery(cursor, todayText, safePageSize + 1, idToken) }
            .recoverCatching { runHomeCafeEventFeedQuery(cursor, todayText, safePageSize + 1, null) }
            .getOrElse { throwable ->
                throw IllegalStateException("Failed to load home cafe event page", throwable)
            }
        val pageDocuments = documents.take(safePageSize)
        val hasNext = documents.size > safePageSize
        val nextCursorToken = if (hasNext) pageDocuments.lastOrNull()?.toHomeEventQueryCursor() else null
        val pageItems = pageDocuments.mapNotNull { document ->
            val name = document["name"]?.jsonPrimitive?.contentOrNull
            val cafeId = name?.toCafeIdFromEventDocumentName() ?: return@mapNotNull null
            parseEventManagementDocument(cafeId, document)
        }
        return PagedResult(items = pageItems, nextCursor = nextCursorToken, hasNext = hasNext)
    }

    override suspend fun createCafeNotice(input: CafeNoticeCreate): CafeNoticeManagementItem {
        val idToken = tokenProvider.getIdToken()
        val noticeId = nextFirestoreEntityId("notice-management")
        val createdAt = Clock.System.now().toString()
        val cafeName = resolveCafeNameForNoticeWrite(input.cafeId, idToken)
        val statusLabel = if (input.reservedAt.isNullOrBlank()) "게시 중" else "임시 저장"
        val statusAccent = if (input.reservedAt.isNullOrBlank()) NoticeStatusAccent.PUBLISHED else NoticeStatusAccent.DRAFT
        val body = firestoreDocumentBody(
            mapOf(
                "title" to firestoreString(input.title.trim()),
                "content" to firestoreString(input.content.trim()),
                "cafeName" to firestoreString(cafeName),
                "createdAt" to firestoreString(createdAt),
                "isPinned" to firestoreBoolean(input.isPinned),
                "statusLabel" to firestoreString(statusLabel),
                "statusAccent" to firestoreString(statusAccent.name),
                "reservedAt" to firestoreNullableString(input.reservedAt?.trim()?.takeIf { it.isNotEmpty() })
            )
        )
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFES}/${input.cafeId}/${FirestorePaths.CAFE_NOTICES}/$noticeId"

        restApi.patch(path, body, idToken)
        return CafeNoticeManagementItem(
            id = noticeId,
            cafeId = input.cafeId,
            title = input.title.trim(),
            content = input.content.trim(),
            createdAt = createdAt,
            displayDate = createdAt.take(10).replace("-", "."),
            isPinned = input.isPinned,
            statusLabel = statusLabel,
            statusAccent = statusAccent
        )
    }

    override suspend fun createCafeEvent(input: CafeEventCreate): CafeEventManagementItem {
        val idToken = tokenProvider.getIdToken()
        val eventId = nextFirestoreEntityId("event-management")
        val periodText = input.periodText?.trim()?.takeIf { it.isNotEmpty() } ?: "게시 일정 선택 필요"
        val startDate = periodText.substringBefore(" - ", missingDelimiterValue = periodText)
        val endDate = periodText.substringAfter(" - ", missingDelimiterValue = startDate)
        val statusLabel = if (input.periodText.isNullOrBlank()) "진행 예정" else "진행 중"
        val participantCastIds = input.participantCastIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        val body = firestoreDocumentBody(
            mapOf(
                "title" to firestoreString(input.title.trim()),
                "content" to firestoreString(input.content.trim()),
                "imageUrl" to firestoreString(input.imageUrl.trim()),
                "startDate" to firestoreString(startDate),
                "endDate" to firestoreString(endDate),
                "statusLabel" to firestoreString(statusLabel),
                "isDimmed" to firestoreBoolean(false),
                "participantCastIds" to firestoreStringArray(participantCastIds),
                "hasLivePerformance" to firestoreBoolean(input.hasLivePerformance)
            )
        )
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFES}/${input.cafeId}/${FirestorePaths.CAFE_EVENTS}/$eventId"

        restApi.patch(path, body, idToken)
        return CafeEventManagementItem(
            id = eventId,
            cafeId = input.cafeId,
            title = input.title.trim(),
            content = input.content.trim(),
            imageUrl = input.imageUrl.trim(),
            startDate = startDate,
            endDate = endDate,
            statusLabel = statusLabel,
            isDimmed = false,
            participantCastIds = participantCastIds,
            hasLivePerformance = input.hasLivePerformance
        )
    }

    override suspend fun updateCafeNotice(input: CafeNoticeUpdate): CafeNoticeManagementItem {
        val idToken = tokenProvider.getIdToken()
        val cafeName = resolveCafeNameForNoticeWrite(input.cafeId, idToken)
        val statusLabel = if (input.reservedAt.isNullOrBlank()) "게시 중" else "임시 저장"
        val statusAccent = if (input.reservedAt.isNullOrBlank()) NoticeStatusAccent.PUBLISHED else NoticeStatusAccent.DRAFT
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFES}/${input.cafeId}/${FirestorePaths.CAFE_NOTICES}/${input.noticeId}" +
            "?updateMask.fieldPaths=title&updateMask.fieldPaths=content&updateMask.fieldPaths=cafeName" +
            "&updateMask.fieldPaths=isPinned&updateMask.fieldPaths=statusLabel&updateMask.fieldPaths=statusAccent&updateMask.fieldPaths=reservedAt"
        val body = firestoreDocumentBody(
            mapOf(
                "title" to firestoreString(input.title.trim()),
                "content" to firestoreString(input.content.trim()),
                "cafeName" to firestoreString(cafeName),
                "isPinned" to firestoreBoolean(input.isPinned),
                "statusLabel" to firestoreString(statusLabel),
                "statusAccent" to firestoreString(statusAccent.name),
                "reservedAt" to firestoreNullableString(input.reservedAt?.trim()?.takeIf { it.isNotEmpty() })
            )
        )

        restApi.patch(path, body, idToken)
        return CafeNoticeManagementItem(
            id = input.noticeId,
            cafeId = input.cafeId,
            title = input.title.trim(),
            content = input.content.trim(),
            createdAt = Clock.System.now().toString(),
            displayDate = Clock.System.now().toString().take(10).replace("-", "."),
            isPinned = input.isPinned,
            statusLabel = statusLabel,
            statusAccent = statusAccent
        )
    }

    override suspend fun updateCafeEvent(input: CafeEventUpdate): CafeEventManagementItem {
        val idToken = tokenProvider.getIdToken()
        val periodText = input.periodText?.trim()?.takeIf { it.isNotEmpty() } ?: "게시 일정 선택 필요"
        val startDate = periodText.substringBefore(" - ", missingDelimiterValue = periodText)
        val endDate = periodText.substringAfter(" - ", missingDelimiterValue = startDate)
        val statusLabel = if (input.periodText.isNullOrBlank()) "진행 예정" else "진행 중"
        val participantCastIds = input.participantCastIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFES}/${input.cafeId}/${FirestorePaths.CAFE_EVENTS}/${input.eventId}" +
            "?updateMask.fieldPaths=title&updateMask.fieldPaths=content&updateMask.fieldPaths=imageUrl" +
            "&updateMask.fieldPaths=startDate&updateMask.fieldPaths=endDate&updateMask.fieldPaths=statusLabel&updateMask.fieldPaths=isDimmed" +
            "&updateMask.fieldPaths=participantCastIds&updateMask.fieldPaths=hasLivePerformance"
        val body = firestoreDocumentBody(
            mapOf(
                "title" to firestoreString(input.title.trim()),
                "content" to firestoreString(input.content.trim()),
                "imageUrl" to firestoreString(input.imageUrl.trim()),
                "startDate" to firestoreString(startDate),
                "endDate" to firestoreString(endDate),
                "statusLabel" to firestoreString(statusLabel),
                "isDimmed" to firestoreBoolean(false),
                "participantCastIds" to firestoreStringArray(participantCastIds),
                "hasLivePerformance" to firestoreBoolean(input.hasLivePerformance)
            )
        )

        restApi.patch(path, body, idToken)
        return CafeEventManagementItem(
            id = input.eventId,
            cafeId = input.cafeId,
            title = input.title.trim(),
            content = input.content.trim(),
            imageUrl = input.imageUrl.trim(),
            startDate = startDate,
            endDate = endDate,
            statusLabel = statusLabel,
            isDimmed = false,
            participantCastIds = participantCastIds,
            hasLivePerformance = input.hasLivePerformance
        )
    }

    override suspend fun deleteCafeNotice(cafeId: String, noticeId: String): String {
        val idToken = tokenProvider.getIdToken()
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFES}/$cafeId/${FirestorePaths.CAFE_NOTICES}/$noticeId"

        restApi.delete(path, idToken)
        runCatching { refreshCafeNoticeEventManagement(cafeId, idToken) }
        return noticeId
    }

    override suspend fun deleteCafeEvent(cafeId: String, eventId: String): String {
        val idToken = tokenProvider.getIdToken()
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFES}/$cafeId/${FirestorePaths.CAFE_EVENTS}/$eventId"

        restApi.delete(path, idToken)
        runCatching { refreshCafeNoticeEventManagement(cafeId, idToken) }
        return eventId
    }

    override suspend fun isCafeEventLikedByUser(cafeId: String, eventId: String, userId: String): Boolean {
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFES}/$cafeId/${FirestorePaths.CAFE_EVENTS}/$eventId/${FirestorePaths.CAFE_EVENT_LIKES}/$userId"
        return try {
            restApi.get(path, idToken)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun toggleCafeEventLike(cafeId: String, eventId: String, userId: String): Boolean {
        val idToken = tokenProvider.getIdToken()
        val likePath = "${config.documentBasePath()}/${FirestorePaths.CAFES}/$cafeId/${FirestorePaths.CAFE_EVENTS}/$eventId/${FirestorePaths.CAFE_EVENT_LIKES}/$userId"
        val eventPath = "${config.documentBasePath()}/${FirestorePaths.CAFES}/$cafeId/${FirestorePaths.CAFE_EVENTS}/$eventId"
        val isCurrentlyLiked = try { restApi.get(likePath, idToken); true } catch (e: Exception) { false }
        return if (isCurrentlyLiked) {
            restApi.delete(likePath, idToken)
            val eventDoc = runCatching { Json.parseToJsonElement(restApi.get(eventPath, idToken)).jsonObject }.getOrNull()
            val currentCount = eventDoc?.get("fields")?.jsonObject?.getFirestoreInt("likeCount") ?: 1
            val newCount = maxOf(0, currentCount - 1)
            restApi.patch(eventPath, firestoreDocumentBody(mapOf("likeCount" to firestoreLong(newCount.toLong()))), idToken, listOf("likeCount"))
            false
        } else {
            val body = firestoreDocumentBody(mapOf("userId" to firestoreString(userId), "createdAt" to firestoreString(Clock.System.now().toString())))
            restApi.patch(likePath, body, idToken)
            val eventDoc = runCatching { Json.parseToJsonElement(restApi.get(eventPath, idToken)).jsonObject }.getOrNull()
            val currentCount = eventDoc?.get("fields")?.jsonObject?.getFirestoreInt("likeCount") ?: 0
            val newCount = currentCount + 1
            restApi.patch(eventPath, firestoreDocumentBody(mapOf("likeCount" to firestoreLong(newCount.toLong()))), idToken, listOf("likeCount"))
            true
        }
    }

    private suspend fun refreshCafeNoticeEventManagement(cafeId: String, idToken: String?) {
        val cafeName = runCatching {
            parseCafeDocument(loadCafeDocument(cafeId, idToken))?.name.orEmpty()
        }.recoverCatching {
            parseCafeDocument(loadCafeDocument(cafeId, null))?.name.orEmpty()
        }.getOrDefault("")
        val noticesDocuments = runCatching { loadCafeSubCollectionDocuments(cafeId, FirestorePaths.CAFE_NOTICES, idToken) }
            .recoverCatching { loadCafeSubCollectionDocuments(cafeId, FirestorePaths.CAFE_NOTICES, null) }
            .getOrElse { emptyList() }
        val eventsDocuments = runCatching { loadCafeSubCollectionDocuments(cafeId, FirestorePaths.CAFE_EVENTS, idToken) }
            .recoverCatching { loadCafeSubCollectionDocuments(cafeId, FirestorePaths.CAFE_EVENTS, null) }
            .getOrElse { emptyList() }
        noticesDocuments.mapNotNull { parseNoticeManagementDocument(cafeId, it) }.sortedByDescending { it.createdAt }
        noticesDocuments.mapNotNull { parseNoticeDocument(cafeId, cafeName, it) }.sortedByDescending { it.createdAt }
        eventsDocuments.mapNotNull { parseEventManagementDocument(cafeId, it) }.sortedByDescending { it.startDate }
    }
}

// ── Review ────────────────────────────────────────────────────────────────────

class FirestoreReviewRemoteDataSource(
    config: FirestoreConfig,
    restApi: FirestoreRestApi,
    tokenProvider: FirestoreAuthTokenProvider
) : FirestoreBaseDataSource(config, restApi, tokenProvider), ReviewRemoteDataSource {
    override suspend fun fetchCafeReviews(cafeId: String, cursor: String?, pageSize: Int): PagedResult<Review> {
        val safePageSize = if (pageSize > 0) pageSize else 1
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        val reviewDocuments = runCatching { runCafeReviewPageQuery(cafeId, cursor, safePageSize + 1, idToken) }
            .recoverCatching { runCafeReviewPageQuery(cafeId, cursor, safePageSize + 1, null) }
            .getOrElse { throwable ->
                throw IllegalStateException("Failed to load cafe review page($cafeId): ${throwable.message}", throwable)
            }
        val pageDocuments = reviewDocuments.take(safePageSize)
        val hasNext = reviewDocuments.size > safePageSize
        val nextCursorToken = if (hasNext) pageDocuments.lastOrNull()?.toReviewQueryCursor() else null
        return PagedResult(items = pageDocuments.mapNotNull { parseReviewDocument(it) }, nextCursor = nextCursorToken, hasNext = hasNext)
    }

    override suspend fun fetchReview(reviewId: String): Review {
        val idToken = tokenProvider.getIdToken()
        return resolveReviewById(reviewId, idToken) ?: throw NoSuchElementException("review not found: $reviewId")
    }

    override suspend fun fetchRecentTaggedReviews(cafeId: String, castId: String, limit: Int): List<Review> {
        val safeLimit = if (limit > 0) limit else 1
        val idToken = tokenProvider.getIdToken()
        val reviewDocuments = runCatching { runRecentTaggedReviewQuery(cafeId, castId, safeLimit, idToken) }
            .recoverCatching { runRecentTaggedReviewQuery(cafeId, castId, safeLimit, null) }
            .getOrElse { emptyList() }
        return reviewDocuments.mapNotNull { parseReviewDocument(it) }
            .sortedByDescending { it.createdAt }
            .take(safeLimit)
    }

    override suspend fun createReview(
        userId: String,
        cafeId: String,
        visitId: String,
        rating: Float,
        content: String,
        imageUrls: List<String>,
        taggedCastIds: List<String>
    ): Review {
        val idToken = tokenProvider.getIdToken()
        val userNickname = runCatching { parseUserDocument(loadUserDocument(userId, idToken))?.nickname.orEmpty() }
            .recoverCatching { parseUserDocument(loadUserDocument(userId, null))?.nickname.orEmpty() }
            .getOrDefault("")
        val reviewId = nextFirestoreEntityId("review")
        val createdAt = Clock.System.now().toString()
        val path = "${config.documentBasePath()}/${FirestorePaths.REVIEWS}/$reviewId"
        val body = firestoreDocumentBody(
            mapOf(
                "userId" to firestoreString(userId),
                "userNickname" to firestoreString(userNickname),
                "cafeId" to firestoreString(cafeId),
                "visitId" to firestoreString(visitId),
                "rating" to firestoreDouble(rating.toDouble()),
                "content" to firestoreString(content.trim()),
                "imageUrls" to firestoreStringArray(imageUrls),
                "taggedCastIds" to firestoreStringArray(taggedCastIds),
                "visitVerified" to firestoreBoolean(false),
                "likeCount" to firestoreLong(0L),
                "createdAt" to firestoreString(createdAt),
                "updatedAt" to firestoreString(createdAt)
            )
        )

        restApi.patch(path, body, idToken)
        return Review(
            id = reviewId, userId = userId, cafeId = cafeId, visitId = visitId,
            rating = rating, content = content.trim(), imageUrls = imageUrls,
            taggedCastIds = taggedCastIds, likeCount = 0, createdAt = createdAt,
            visitVerified = false, userNickname = userNickname
        )
    }

    override suspend fun updateReview(
        reviewId: String,
        requesterId: String,
        rating: Float,
        content: String,
        imageUrls: List<String>,
        taggedCastIds: List<String>
    ): Review {
        val idToken = tokenProvider.getIdToken()
        val existing = resolveReviewById(reviewId, idToken) ?: throw NoSuchElementException("review not found")
        if (existing.userId != requesterId) throw IllegalStateException("no permission to update review")
        val path = "${config.documentBasePath()}/${FirestorePaths.REVIEWS}/$reviewId" +
            "?updateMask.fieldPaths=rating&updateMask.fieldPaths=content" +
            "&updateMask.fieldPaths=imageUrls&updateMask.fieldPaths=taggedCastIds&updateMask.fieldPaths=updatedAt"
        val body = firestoreDocumentBody(
            mapOf(
                "rating" to firestoreDouble(rating.toDouble()),
                "content" to firestoreString(content.trim()),
                "imageUrls" to firestoreStringArray(imageUrls),
                "taggedCastIds" to firestoreStringArray(taggedCastIds),
                "updatedAt" to firestoreString(Clock.System.now().toString())
            )
        )

        restApi.patch(path, body, idToken)
        return existing.copy(rating = rating, content = content.trim(), imageUrls = imageUrls, taggedCastIds = taggedCastIds)
    }

    override suspend fun hasReviewForVisit(visitId: String): Boolean {
        if (visitId.isBlank()) return false
        val idToken = tokenProvider.getIdToken()
        val reviewDocuments = runCatching {
            runFieldScopedQuery(FirestorePaths.REVIEWS, "visitId", visitId, idToken, orderByCreatedAtDesc = false, limit = 1)
        }.recoverCatching {
            runFieldScopedQuery(FirestorePaths.REVIEWS, "visitId", visitId, null, orderByCreatedAtDesc = false, limit = 1)
        }.getOrElse { emptyList() }
        return reviewDocuments.isNotEmpty()
    }

    override suspend fun likeReview(reviewId: String) {
        val idToken = tokenProvider.getIdToken()
        val existing = resolveReviewById(reviewId, idToken) ?: throw NoSuchElementException("review not found")
        val nextLikeCount = existing.likeCount + 1
        val path = "${config.documentBasePath()}/${FirestorePaths.REVIEWS}/$reviewId?updateMask.fieldPaths=likeCount"
        val body = firestoreDocumentBody(mapOf("likeCount" to firestoreLong(nextLikeCount.toLong())))

        restApi.patch(path, body, idToken)
    }

    override suspend fun deleteReview(reviewId: String, requesterId: String) {
        val idToken = tokenProvider.getIdToken()
        val existing = resolveReviewById(reviewId, idToken) ?: throw NoSuchElementException("review not found")
        if (existing.userId != requesterId) throw IllegalStateException("no permission to delete review")
        val path = "${config.documentBasePath()}/${FirestorePaths.REVIEWS}/$reviewId"

        restApi.delete(path, idToken)
    }
}

// ── Visit ─────────────────────────────────────────────────────────────────────

class FirestoreVisitRemoteDataSource(
    config: FirestoreConfig,
    restApi: FirestoreRestApi,
    tokenProvider: FirestoreAuthTokenProvider
) : FirestoreBaseDataSource(config, restApi, tokenProvider), VisitRemoteDataSource {
    override suspend fun verifyVisit(cafeId: String, latitude: Double, longitude: Double): VisitVerificationResult {
        val cafe = fetchCafeByIdRemoteInternal(cafeId)
        val cafeLatitude = cafe?.region?.location?.latitude ?: latitude
        val cafeLongitude = cafe?.region?.location?.longitude ?: longitude
        val distanceMeters = haversineMeters(cafeLatitude, cafeLongitude, latitude, longitude)
        val allowedDistanceMeters = 200.0
        return if (distanceMeters <= allowedDistanceMeters) {
            VisitVerificationResult(verified = true, distanceMeters = distanceMeters, allowedRadiusMeters = allowedDistanceMeters, message = "방문 인증 성공")
        } else {
            VisitVerificationResult(verified = false, distanceMeters = distanceMeters, allowedRadiusMeters = allowedDistanceMeters, message = "카페 반경 200m 밖입니다")
        }
    }

    override suspend fun createVisit(
        userId: String,
        cafeId: String,
        visitedAt: String,
        memo: String?,
        latitude: Double,
        longitude: Double
    ): Visit {
        ensureAuthenticatedUserMatch(requestedUserId = userId, action = "createVisitRemote")
        val idToken = tokenProvider.getIdToken()
        val visitId = nextFirestoreEntityId("visit")
        val now = Clock.System.now().toString()
        val path = "${config.documentBasePath()}/${FirestorePaths.VISITS}/$visitId"
        val body = firestoreDocumentBody(
            mapOf(
                "userId" to firestoreString(userId),
                "cafeId" to firestoreString(cafeId),
                "location" to firestoreGeoPoint(latitude, longitude),
                "visitedAt" to firestoreString(visitedAt),
                "memo" to firestoreNullableString(memo?.trim()?.takeIf { it.isNotEmpty() }),
                "verified" to firestoreBoolean(false),
                "checkInMethod" to firestoreString("LOCATION"),
                "createdAt" to firestoreString(now),
                "updatedAt" to firestoreString(now)
            )
        )
        restApi.patch(path, body, idToken)
        val createdVisit = Visit(id = visitId, userId = userId, cafeId = cafeId, visitedAt = visitedAt, memo = memo?.trim()?.takeIf { it.isNotEmpty() }, verified = false, checkInMethod = "LOCATION")
        return resolveVisitById(visitId, idToken) ?: createdVisit
    }

    override suspend fun createQrVisit(userId: String, cafeId: String, visitedAt: String, memo: String?): Visit {
        ensureAuthenticatedUserMatch(requestedUserId = userId, action = "createQrVisitRemote")
        val idToken = tokenProvider.getIdToken()
        val visitId = nextFirestoreEntityId("visit")
        val now = Clock.System.now().toString()
        val path = "${config.documentBasePath()}/${FirestorePaths.VISITS}/$visitId"
        val body = firestoreDocumentBody(
            mapOf(
                "userId" to firestoreString(userId),
                "cafeId" to firestoreString(cafeId),
                "visitedAt" to firestoreString(visitedAt),
                "memo" to firestoreNullableString(memo?.trim()?.takeIf { it.isNotEmpty() }),
                "verified" to firestoreBoolean(false),
                "checkInMethod" to firestoreString("QR"),
                "createdAt" to firestoreString(now),
                "updatedAt" to firestoreString(now)
            )
        )
        restApi.patch(path, body, idToken)
        val createdVisit = Visit(id = visitId, userId = userId, cafeId = cafeId, visitedAt = visitedAt, memo = memo?.trim()?.takeIf { it.isNotEmpty() }, verified = false, checkInMethod = "QR")
        return resolveVisitById(visitId, idToken) ?: createdVisit
    }

    override suspend fun updateVisit(visitId: String, userId: String, visitedAt: String, memo: String?): Visit {
        val idToken = tokenProvider.getIdToken()
        val existing = resolveVisitById(visitId, idToken) ?: throw NoSuchElementException("visit not found")
        if (existing.userId != userId) throw IllegalStateException("no permission to update visit")
        val path = "${config.documentBasePath()}/${FirestorePaths.VISITS}/$visitId" +
            "?updateMask.fieldPaths=visitedAt&updateMask.fieldPaths=memo&updateMask.fieldPaths=updatedAt"
        val body = firestoreDocumentBody(
            mapOf(
                "visitedAt" to firestoreString(visitedAt),
                "memo" to firestoreNullableString(memo?.trim()?.takeIf { it.isNotEmpty() }),
                "updatedAt" to firestoreString(Clock.System.now().toString())
            )
        )
        restApi.patch(path, body, idToken)
        val fallbackUpdated = existing.copy(visitedAt = visitedAt, memo = memo?.trim()?.takeIf { it.isNotEmpty() })
        return resolveVisitById(visitId, idToken) ?: fallbackUpdated
    }

    override suspend fun deleteVisit(visitId: String, userId: String) {
        val idToken = tokenProvider.getIdToken()
        val existing = resolveVisitById(visitId, idToken) ?: throw NoSuchElementException("visit not found")
        if (existing.userId != userId) throw IllegalStateException("no permission to delete visit")
        val path = "${config.documentBasePath()}/${FirestorePaths.VISITS}/$visitId"
        restApi.delete(path, idToken)
        runCatching { refreshVisitsByUserRemote(existing.userId) }
    }

    private suspend fun refreshVisitsByUserRemote(userId: String) {
        if (userId.isBlank()) return
        val idToken = tokenProvider.getIdToken()
        runCatching {
            runUserScopedQuery(FirestorePaths.VISITS, userId, idToken, "visitedAt", true)
        }.recoverCatching {
            runUserScopedQuery(FirestorePaths.VISITS, userId, null, "visitedAt", true)
        }.getOrElse { throwable ->
            throw IllegalStateException("Failed to refresh visits for user: $userId", throwable)
        }.mapNotNull { parseVisitDocument(it) }
    }

    override suspend fun fetchVisits(userId: String, cursor: String?, pageSize: Int): PagedResult<Visit> {
        if (userId.isBlank()) return PagedResult(emptyList(), null, false)
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        val tokenUserId = tokenProvider.getCurrentUserId()
        val primaryVisitDocuments = runCatching {
            runUserVisitPageQuery(userId, cursor, pageSize.coerceAtLeast(1) + 1, idToken)
        }.recoverCatching {
            runUserVisitPageQuery(userId, cursor, pageSize.coerceAtLeast(1) + 1, null)
        }.getOrElse { error ->
            println("TEST, fetchVisits query failed: userId=$userId tokenUserId=$tokenUserId message=${error.message}")
            emptyList()
        }
        val safePageSize = pageSize.coerceAtLeast(1)
        val mappedVisits = primaryVisitDocuments.mapNotNull { parseVisitDocument(it) }.sortedByDescending { it.visitedAt }
        val pageItems = mappedVisits.take(safePageSize)
        val hasNext = primaryVisitDocuments.size > safePageSize
        val nextCursor = if (hasNext) primaryVisitDocuments.take(safePageSize).lastOrNull()?.toVisitQueryCursor() else null
        println("TEST, fetchVisits result: userId=$userId tokenUserId=$tokenUserId documents=${primaryVisitDocuments.size} visits=${mappedVisits.size}")
        return PagedResult(items = pageItems, nextCursor = nextCursor, hasNext = hasNext)
    }

    override suspend fun fetchVerifiedVisitUserIdsByCafe(cafeId: String): Set<String> {
        val idToken = tokenProvider.getIdToken()
        val visitDocuments = runCatching { runVerifiedVisitUserQuery(cafeId, idToken) }
            .recoverCatching { runVerifiedVisitUserQuery(cafeId, null) }
            .getOrElse { emptyList() }
        return visitDocuments.mapNotNull { parseVisitDocument(it) }
            .asSequence()
            .filter { it.verified && it.cafeId == cafeId }
            .map { it.userId }
            .toSet()
    }

    override suspend fun hasVerifiedVisitAtCafe(userId: String, cafeId: String): Boolean {
        val idToken = tokenProvider.getIdToken()
        val visitDocuments = runCatching { runVerifiedVisitByUserCafeQuery(userId, cafeId, idToken) }
            .recoverCatching { runVerifiedVisitByUserCafeQuery(userId, cafeId, null) }
            .getOrElse { emptyList() }
        return visitDocuments.mapNotNull { parseVisitDocument(it) }
            .any { it.userId == userId && it.cafeId == cafeId && it.verified }
    }

    override suspend fun fetchStamps(userId: String): List<Stamp> {
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        val stampDocuments = runCatching {
            runUserScopedQuery(FirestorePaths.STAMPS, userId, idToken, "earnedAt", true)
        }.recoverCatching {
            runUserScopedQuery(FirestorePaths.STAMPS, userId, null, "earnedAt", true)
        }.getOrElse { emptyList() }
        return stampDocuments.mapNotNull { parseStampDocument(it) }
    }

    override suspend fun fetchVisitCountByCafe(cafeId: String): Int {
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        return runCatching {
            loadCollectionDocumentCount(FirestorePaths.VISITS, idToken, "cafeId", firestoreString(cafeId))
        }.recoverCatching {
            loadCollectionDocumentCount(FirestorePaths.VISITS, null, "cafeId", firestoreString(cafeId))
        }.getOrThrow()
    }
}

// ── MyInfo ────────────────────────────────────────────────────────────────────

class FirestoreMyInfoRemoteDataSource(
    config: FirestoreConfig,
    restApi: FirestoreRestApi,
    tokenProvider: FirestoreAuthTokenProvider
) : FirestoreBaseDataSource(config, restApi, tokenProvider), MyInfoRemoteDataSource {
    override suspend fun isReviewPromptDismissed(userId: String, visitId: String): Boolean {
        require(userId.isNotBlank()) { "userId is required" }
        require(visitId.isNotBlank()) { "visitId is required" }
        val userDocument = runCatching { loadUserDocument(userId, tokenProvider.getIdToken()) }
            .recoverCatching { loadUserDocument(userId, null) }
            .getOrNull() ?: return false
        val fields = userDocument["fields"]?.jsonObject ?: return false
        return fields.getFirestoreStringList("dismissedReviewPromptVisitIds").contains(visitId)
    }

    override suspend fun dismissReviewPrompt(userId: String, visitId: String) {
        require(userId.isNotBlank()) { "userId is required" }
        require(visitId.isNotBlank()) { "visitId is required" }
        val userDocument = runCatching { loadUserDocument(userId, tokenProvider.getIdToken()) }
            .recoverCatching { loadUserDocument(userId, null) }
            .getOrNull()
        val fields = userDocument?.get("fields")?.jsonObject
        val currentIds = fields?.getFirestoreStringList("dismissedReviewPromptVisitIds").orEmpty()
        val nextIds = (currentIds + visitId).distinct()
        val path = "${config.documentBasePath()}/${FirestorePaths.USERS}/$userId?updateMask.fieldPaths=dismissedReviewPromptVisitIds"
        val body = firestoreDocumentBody(mapOf("dismissedReviewPromptVisitIds" to firestoreStringArray(nextIds)))
        runCatching { restApi.patch(path, body, tokenProvider.getIdToken()) }
            .recoverCatching { restApi.patch(path, body, null) }
            .getOrElse { throwable ->
                throw IllegalStateException("Failed to dismiss review prompt for user: $userId", throwable)
            }
    }
}

// ── Community Post ────────────────────────────────────────────────────────────

class FirestoreCommunityPostRemoteDataSource(
    config: FirestoreConfig,
    restApi: FirestoreRestApi,
    tokenProvider: FirestoreAuthTokenProvider
) : FirestoreBaseDataSource(config, restApi, tokenProvider), CommunityPostRemoteDataSource {

    override suspend fun fetchCommunityPostPage(
        cursor: String?,
        pageSize: Int
    ): PagedResult<CommunityPost> {
        val safePageSize = if (pageSize > 0) pageSize else 1
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        val documents = runCatching { runCommunityPostPageQuery(cursor, safePageSize + 1, idToken) }
            .recoverCatching { runCommunityPostPageQuery(cursor, safePageSize + 1, null) }
            .getOrElse { throwable ->
                throw IllegalStateException("Failed to load community post page: ${throwable.message}", throwable)
            }
        val pageDocuments = documents.take(safePageSize)
        val hasNext = documents.size > safePageSize
        val nextCursorToken = if (hasNext) pageDocuments.lastOrNull()?.toCommunityPostQueryCursor() else null
        val pageItems = pageDocuments.mapNotNull { parseCommunityPostDocument(it) }
        return PagedResult(items = pageItems, nextCursor = nextCursorToken, hasNext = hasNext)
    }

    override suspend fun createCommunityPost(
        userId: String,
        title: String,
        content: String,
        imageUrls: List<String>
    ): CommunityPost {
        val idToken = tokenProvider.getIdToken()
        val userNickname = runCatching { parseUserDocument(loadUserDocument(userId, idToken))?.nickname.orEmpty() }
            .recoverCatching { parseUserDocument(loadUserDocument(userId, null))?.nickname.orEmpty() }
            .getOrDefault("")
        val postId = nextFirestoreEntityId("post")
        val createdAt = Clock.System.now().toString()
        val body = firestoreDocumentBody(
            mapOf(
                "userId" to firestoreString(userId),
                "userNickname" to firestoreString(userNickname),
                "title" to firestoreString(title),
                "content" to firestoreString(content),
                "imageUrls" to firestoreStringArray(imageUrls),
                "likeCount" to firestoreLong(0L),
                "commentCount" to firestoreLong(0L),
                "viewCount" to firestoreLong(0L),
                "createdAt" to firestoreString(createdAt),
                "updatedAt" to firestoreString(createdAt)
            )
        )
        val path = "${config.documentBasePath()}/${FirestorePaths.COMMUNITY_POSTS}/$postId"
        restApi.patch(path, body, idToken)
        return CommunityPost(
            id = postId,
            userId = userId,
            userNickname = userNickname,
            title = title,
            content = content,
            imageUrls = imageUrls,
            likeCount = 0,
            commentCount = 0,
            viewCount = 0,
            createdAt = createdAt,
            displayDate = createdAt.take(10).replace("-", ".")
        )
    }

    override suspend fun fetchCommunityPost(postId: String): CommunityPost {
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        val path = "${config.documentBasePath()}/${FirestorePaths.COMMUNITY_POSTS}/$postId"
        val response = runCatching { restApi.get(path, idToken) }
            .recoverCatching { restApi.get(path, null) }
            .getOrElse { throw IllegalStateException("Failed to load post: ${it.message}", it) }
        val document = Json.parseToJsonElement(response).jsonObject
        return parseCommunityPostDocument(document)
            ?: throw IllegalStateException("Failed to parse post document")
    }

    override suspend fun deleteCommunityPost(postId: String) {
        val idToken = tokenProvider.getIdToken()
        val path = "${config.documentBasePath()}/${FirestorePaths.COMMUNITY_POSTS}/$postId"
        restApi.delete(path, idToken)
    }

    override suspend fun incrementViewCount(postId: String) {
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        val documentName = "projects/${config.projectId}/databases/${config.databaseId}/documents/${FirestorePaths.COMMUNITY_POSTS}/$postId"
        val path = "${config.documentBasePath()}:commit"
        val body = """
            {
              "writes": [
                {
                  "transform": {
                    "document": "$documentName",
                    "fieldTransforms": [
                      {
                        "fieldPath": "viewCount",
                        "increment": {"integerValue": "1"}
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()
        restApi.post(path, body, idToken)
    }

    override suspend fun updateCommunityPost(
        postId: String,
        title: String,
        content: String,
        imageUrls: List<String>
    ): CommunityPost {
        val idToken = tokenProvider.getIdToken()
        val updatedAt = Clock.System.now().toString()
        val body = firestoreDocumentBody(
            mapOf(
                "title" to firestoreString(title),
                "content" to firestoreString(content),
                "imageUrls" to firestoreStringArray(imageUrls),
                "updatedAt" to firestoreString(updatedAt)
            )
        )
        val path = "${config.documentBasePath()}/${FirestorePaths.COMMUNITY_POSTS}/$postId"
        restApi.patch(path, body, idToken, listOf("title", "content", "imageUrls", "updatedAt"))
        val response = restApi.get(path, idToken)
        val document = Json.parseToJsonElement(response).jsonObject
        return parseCommunityPostDocument(document)
            ?: throw IllegalStateException("Failed to parse updated post")
    }

    override suspend fun isLikedByUser(postId: String, userId: String): Boolean {
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        val path = "${config.documentBasePath()}/${FirestorePaths.COMMUNITY_POSTS}/$postId/${FirestorePaths.POST_LIKES}/$userId"
        return try {
            restApi.get(path, idToken)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun toggleLike(postId: String, userId: String): Boolean {
        val idToken = tokenProvider.getIdToken()
        val likePath = "${config.documentBasePath()}/${FirestorePaths.COMMUNITY_POSTS}/$postId/${FirestorePaths.POST_LIKES}/$userId"
        val postPath = "${config.documentBasePath()}/${FirestorePaths.COMMUNITY_POSTS}/$postId"
        val isCurrentlyLiked = try { restApi.get(likePath, idToken); true } catch (e: Exception) { false }
        return if (isCurrentlyLiked) {
            restApi.delete(likePath, idToken)
            val postDoc = runCatching { Json.parseToJsonElement(restApi.get(postPath, idToken)).jsonObject }.getOrNull()
            val currentCount = postDoc?.get("fields")?.jsonObject?.getFirestoreInt("likeCount") ?: 1
            val newCount = maxOf(0, currentCount - 1)
            restApi.patch(postPath, firestoreDocumentBody(mapOf("likeCount" to firestoreLong(newCount.toLong()))), idToken, listOf("likeCount"))
            false
        } else {
            val body = firestoreDocumentBody(mapOf("userId" to firestoreString(userId), "createdAt" to firestoreString(Clock.System.now().toString())))
            restApi.patch(likePath, body, idToken)
            val postDoc = runCatching { Json.parseToJsonElement(restApi.get(postPath, idToken)).jsonObject }.getOrNull()
            val currentCount = postDoc?.get("fields")?.jsonObject?.getFirestoreInt("likeCount") ?: 0
            val newCount = currentCount + 1
            restApi.patch(postPath, firestoreDocumentBody(mapOf("likeCount" to firestoreLong(newCount.toLong()))), idToken, listOf("likeCount"))
            true
        }
    }

    override suspend fun fetchComments(postId: String): List<Comment> {
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        val path = "${config.documentBasePath()}/${FirestorePaths.COMMUNITY_POSTS}/$postId:runQuery"
        val body = """
            {
              "structuredQuery": {
                "from": [{"collectionId": "${FirestorePaths.POST_COMMENTS}", "allDescendants": false}],
                "orderBy": [{"field": {"fieldPath": "createdAt"}, "direction": "ASCENDING"}]
              }
            }
        """.trimIndent()
        val response = runCatching { restApi.post(path, body, idToken) }
            .recoverCatching { restApi.post(path, body, null) }
            .getOrElse { return emptyList() }
        val parsed = runCatching { Json.parseToJsonElement(response).jsonArray }.getOrElse { return emptyList() }
        return parsed.mapNotNull { element ->
            val document = element.jsonObject["document"]?.jsonObject ?: return@mapNotNull null
            parseCommentDocument(document, postId)
        }
    }

    override suspend fun addComment(postId: String, userId: String, content: String): Comment {
        val idToken = tokenProvider.getIdToken()
        val userNickname = runCatching { parseUserDocument(loadUserDocument(userId, idToken))?.nickname.orEmpty() }
            .getOrDefault("")
        val commentId = nextFirestoreEntityId("comment")
        val createdAt = Clock.System.now().toString()
        val body = firestoreDocumentBody(
            mapOf(
                "postId" to firestoreString(postId),
                "userId" to firestoreString(userId),
                "userNickname" to firestoreString(userNickname),
                "content" to firestoreString(content),
                "createdAt" to firestoreString(createdAt)
            )
        )
        val path = "${config.documentBasePath()}/${FirestorePaths.COMMUNITY_POSTS}/$postId/${FirestorePaths.POST_COMMENTS}/$commentId"
        restApi.patch(path, body, idToken)
        val postPath = "${config.documentBasePath()}/${FirestorePaths.COMMUNITY_POSTS}/$postId"
        val postDoc = runCatching { Json.parseToJsonElement(restApi.get(postPath, idToken)).jsonObject }.getOrNull()
        val currentCount = postDoc?.get("fields")?.jsonObject?.getFirestoreInt("commentCount") ?: 0
        restApi.patch(postPath, firestoreDocumentBody(mapOf("commentCount" to firestoreLong((currentCount + 1).toLong()))), idToken, listOf("commentCount"))
        return Comment(
            id = commentId,
            postId = postId,
            userId = userId,
            userNickname = userNickname,
            content = content,
            createdAt = createdAt,
            displayDate = createdAt.take(10).replace("-", ".")
        )
    }

    override suspend fun fetchCommentPage(postId: String, beforeCursor: String?, pageSize: Int): PagedResult<Comment> {
        val safePageSize = if (pageSize > 0) pageSize else 1
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        val path = "${config.documentBasePath()}/${FirestorePaths.COMMUNITY_POSTS}/$postId:runQuery"
        val startAfterSection = if (!beforeCursor.isNullOrBlank()) {
            val escaped = escapeFirestoreQueryString(beforeCursor)
            ""","startAt": {"values": [{"stringValue": "$escaped"}], "before": false}"""
        } else ""
        val body = """
            {
              "structuredQuery": {
                "from": [{"collectionId": "${FirestorePaths.POST_COMMENTS}", "allDescendants": false}],
                "orderBy": [{"field": {"fieldPath": "createdAt"}, "direction": "DESCENDING"}]
                $startAfterSection,
                "limit": ${safePageSize + 1}
              }
            }
        """.trimIndent()
        val response = runCatching { restApi.post(path, body, idToken) }
            .recoverCatching { restApi.post(path, body, null) }
            .getOrElse { return PagedResult(items = emptyList(), nextCursor = null, hasNext = false) }
        val parsed = runCatching { Json.parseToJsonElement(response).jsonArray }.getOrElse {
            return PagedResult(items = emptyList(), nextCursor = null, hasNext = false)
        }
        val documents = parsed.mapNotNull { it.jsonObject["document"]?.jsonObject }
        val pageDocuments = documents.take(safePageSize)
        val hasMore = documents.size > safePageSize
        val nextCursor = if (hasMore) {
            pageDocuments.lastOrNull()?.get("fields")?.jsonObject?.getFirestoreString("createdAt")
        } else null
        // Return in ascending order (oldest first)
        val items = pageDocuments.mapNotNull { parseCommentDocument(it, postId) }.reversed()
        return PagedResult(items = items, nextCursor = nextCursor, hasNext = hasMore)
    }

    override suspend fun updateComment(postId: String, commentId: String, content: String): Comment {
        val idToken = tokenProvider.getIdToken()
        val updatedAt = Clock.System.now().toString()
        val path = "${config.documentBasePath()}/${FirestorePaths.COMMUNITY_POSTS}/$postId/${FirestorePaths.POST_COMMENTS}/$commentId"
        val body = firestoreDocumentBody(
            mapOf(
                "content" to firestoreString(content),
                "updatedAt" to firestoreString(updatedAt)
            )
        )
        restApi.patch(path, body, idToken, listOf("content", "updatedAt"))
        val response = restApi.get(path, idToken)
        val document = Json.parseToJsonElement(response).jsonObject
        return parseCommentDocument(document, postId)
            ?: throw IllegalStateException("Failed to parse updated comment")
    }

    override suspend fun deleteComment(postId: String, commentId: String) {
        val idToken = tokenProvider.getIdToken()
        val commentPath = "${config.documentBasePath()}/${FirestorePaths.COMMUNITY_POSTS}/$postId/${FirestorePaths.POST_COMMENTS}/$commentId"
        restApi.delete(commentPath, idToken)
        val postPath = "${config.documentBasePath()}/${FirestorePaths.COMMUNITY_POSTS}/$postId"
        val postDoc = runCatching { Json.parseToJsonElement(restApi.get(postPath, idToken)).jsonObject }.getOrNull()
        val currentCount = postDoc?.get("fields")?.jsonObject?.getFirestoreInt("commentCount") ?: 1
        val newCount = maxOf(0, currentCount - 1)
        restApi.patch(postPath, firestoreDocumentBody(mapOf("commentCount" to firestoreLong(newCount.toLong()))), idToken, listOf("commentCount"))
    }

    private fun parseCommentDocument(document: JsonObject, postId: String): Comment? {
        val fields = document["fields"]?.jsonObject ?: return null
        val documentName = document["name"]?.jsonPrimitive?.contentOrNull ?: return null
        val commentId = documentName.substringAfterLast("/").takeIf { it.isNotBlank() } ?: return null
        val userId = fields.getFirestoreString("userId") ?: return null
        val content = fields.getFirestoreString("content") ?: return null
        val userNickname = fields.getFirestoreString("userNickname").orEmpty()
        val createdAt = fields.getFirestoreString("createdAt").orEmpty()
        val displayDate = createdAt.take(10).replace("-", ".")
        return Comment(
            id = commentId,
            postId = postId,
            userId = userId,
            userNickname = userNickname,
            content = content,
            createdAt = createdAt,
            displayDate = displayDate
        )
    }
}
