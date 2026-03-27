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
import com.hhp227.concafe.domain.model.AdminOperationsMetrics
import com.hhp227.concafe.domain.model.Cafe
import com.hhp227.concafe.domain.model.CafeDashboardData
import com.hhp227.concafe.domain.model.CafeDetail
import com.hhp227.concafe.domain.model.CafeEventCreate
import com.hhp227.concafe.domain.model.CafeEventManagementItem
import com.hhp227.concafe.domain.model.CafeEventUpdate
import com.hhp227.concafe.domain.model.CafeInfoUpdate
import com.hhp227.concafe.domain.model.CafeMenu
import com.hhp227.concafe.domain.model.CafeMenuGoodsUpsert
import com.hhp227.concafe.domain.model.CafeNoticeCreate
import com.hhp227.concafe.domain.model.CafeNoticeManagementItem
import com.hhp227.concafe.domain.model.CafeNoticeUpdate
import com.hhp227.concafe.domain.model.CafeRegistrationClaim
import com.hhp227.concafe.domain.model.Cast
import com.hhp227.concafe.domain.model.CastDetail
import com.hhp227.concafe.domain.model.CastSchedule
import com.hhp227.concafe.domain.model.CastScheduleStatus
import com.hhp227.concafe.domain.model.CastScheduleUpdate
import com.hhp227.concafe.domain.model.CastUpsert
import com.hhp227.concafe.domain.model.GeoPoint
import com.hhp227.concafe.domain.model.Goods
import com.hhp227.concafe.domain.model.HomeBanner
import com.hhp227.concafe.domain.model.MyPageSummary
import com.hhp227.concafe.domain.model.Notice
import com.hhp227.concafe.domain.model.NoticeStatusAccent
import com.hhp227.concafe.domain.model.CafeManagementData
import com.hhp227.concafe.domain.model.PendingCafeOwnerClaimPreview
import com.hhp227.concafe.domain.model.PendingCafeRegistrationClaimPreview
import com.hhp227.concafe.domain.model.Region
import com.hhp227.concafe.domain.model.Review
import com.hhp227.concafe.domain.model.User
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.model.Visit
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
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
    private val hydratedCafeDetailIds = mutableSetOf<String>()

    fun isCafeDetailHydrated(cafeId: String): Boolean {
        return hydratedCafeDetailIds.contains(cafeId)
    }

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
        runCatching { loadReviews(idToken = null) }
            .onFailure { throwable -> println("Firestore bootstrap loadReviews failed: ${throwable.message}") }
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
        val menusDocuments = runCatching {
            loadCafeSubCollectionDocuments(cafeId, FirestorePaths.CAFE_MENUS, idToken)
        }.recoverCatching {
            loadCafeSubCollectionDocuments(cafeId, FirestorePaths.CAFE_MENUS, null)
        }.getOrElse { emptyList() }
        val goodsDocuments = runCatching {
            loadCafeSubCollectionDocuments(cafeId, FirestorePaths.CAFE_GOODS, idToken)
        }.recoverCatching {
            loadCafeSubCollectionDocuments(cafeId, FirestorePaths.CAFE_GOODS, null)
        }.getOrElse { emptyList() }
        val parsedMenus = menusDocuments
            .mapNotNull { document -> parseMenuDocument(document) }
            .sortedBy { menu -> menu.name.lowercase() }
        val parsedGoods = goodsDocuments
            .mapNotNull { document -> parseGoodsDocument(document) }
            .sortedBy { goods -> goods.name.lowercase() }

        upsertCafeAndDetail(
            cafe = cafe,
            casts = parsedCasts,
            notices = parsedNotices,
            menus = parsedMenus,
            goods = parsedGoods,
            images = detailMetadata.images,
            businessHours = detailMetadata.businessHours,
            phoneNumber = detailMetadata.phoneNumber
        )
        hydratedCafeDetailIds.add(cafeId)
    }

    suspend fun refreshCafeNoticeEventManagement(cafeId: String) {
        val idToken = tokenProvider.getIdToken()
        val cafeName = cafes.firstOrNull { cafe -> cafe.id == cafeId }?.name.orEmpty()
        val noticesDocuments = runCatching {
            loadCafeSubCollectionDocuments(cafeId, FirestorePaths.CAFE_NOTICES, idToken)
        }.recoverCatching {
            loadCafeSubCollectionDocuments(cafeId, FirestorePaths.CAFE_NOTICES, null)
        }.getOrElse { emptyList() }
        val eventsDocuments = runCatching {
            loadCafeSubCollectionDocuments(cafeId, FirestorePaths.CAFE_EVENTS, idToken)
        }.recoverCatching {
            loadCafeSubCollectionDocuments(cafeId, FirestorePaths.CAFE_EVENTS, null)
        }.getOrElse { emptyList() }
        val parsedNotices = noticesDocuments.mapNotNull { document ->
            parseNoticeManagementDocument(cafeId = cafeId, document = document)
        }.sortedByDescending { item -> item.createdAt }
        val parsedNoticeFeeds = noticesDocuments.mapNotNull { document ->
            parseNoticeDocument(cafeId = cafeId, cafeName = cafeName, document = document)
        }.sortedByDescending { item -> item.createdAt }
        val parsedEvents = eventsDocuments.mapNotNull { document ->
            parseEventManagementDocument(cafeId = cafeId, document = document)
        }.sortedByDescending { item -> item.startDate }

        cafeNoticeManagementItems.removeAll { item -> item.cafeId == cafeId }
        cafeNoticeManagementItems.addAll(parsedNotices)
        notices.removeAll { notice -> notice.cafeId == cafeId }
        notices.addAll(parsedNoticeFeeds)
        notices.sortByDescending { notice -> notice.createdAt }
        cafeEventManagementItems.removeAll { item -> item.cafeId == cafeId }
        cafeEventManagementItems.addAll(parsedEvents)
    }

    suspend fun createCafeNoticeRemote(input: CafeNoticeCreate): CafeNoticeManagementItem {
        val idToken = tokenProvider.getIdToken()
        val noticeId = nextFirestoreEntityId("notice-management")
        val createdAt = Clock.System.now().toString()
        val statusLabel = if (input.reservedAt.isNullOrBlank()) "게시 중" else "임시 저장"
        val statusAccent = if (input.reservedAt.isNullOrBlank()) NoticeStatusAccent.PUBLISHED else NoticeStatusAccent.DRAFT
        val body = firestoreDocumentBody(
            mapOf(
                "title" to firestoreString(input.title.trim()),
                "content" to firestoreString(input.content.trim()),
                "createdAt" to firestoreString(createdAt),
                "isPinned" to firestoreBoolean(input.isPinned),
                "statusLabel" to firestoreString(statusLabel),
                "statusAccent" to firestoreString(statusAccent.name),
                "reservedAt" to firestoreNullableString(input.reservedAt?.trim()?.takeIf { value -> value.isNotEmpty() })
            )
        )
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFES}/${input.cafeId}/${FirestorePaths.CAFE_NOTICES}/$noticeId"

        restApi.patch(path, body, idToken)
        runCatching {
            refreshCafeNoticeEventManagement(input.cafeId)
        }
        return cafeNoticeManagementItems.firstOrNull { item -> item.id == noticeId }
            ?: CafeNoticeManagementItem(
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

    suspend fun updateCafeNoticeRemote(input: CafeNoticeUpdate): CafeNoticeManagementItem {
        val idToken = tokenProvider.getIdToken()
        val statusLabel = if (input.reservedAt.isNullOrBlank()) "게시 중" else "임시 저장"
        val statusAccent = if (input.reservedAt.isNullOrBlank()) NoticeStatusAccent.PUBLISHED else NoticeStatusAccent.DRAFT
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFES}/${input.cafeId}/${FirestorePaths.CAFE_NOTICES}/${input.noticeId}" +
            "?updateMask.fieldPaths=title" +
            "&updateMask.fieldPaths=content" +
            "&updateMask.fieldPaths=isPinned" +
            "&updateMask.fieldPaths=statusLabel" +
            "&updateMask.fieldPaths=statusAccent" +
            "&updateMask.fieldPaths=reservedAt"
        val body = firestoreDocumentBody(
            mapOf(
                "title" to firestoreString(input.title.trim()),
                "content" to firestoreString(input.content.trim()),
                "isPinned" to firestoreBoolean(input.isPinned),
                "statusLabel" to firestoreString(statusLabel),
                "statusAccent" to firestoreString(statusAccent.name),
                "reservedAt" to firestoreNullableString(input.reservedAt?.trim()?.takeIf { value -> value.isNotEmpty() })
            )
        )

        restApi.patch(path, body, idToken)
        runCatching {
            refreshCafeNoticeEventManagement(input.cafeId)
        }
        return cafeNoticeManagementItems.firstOrNull { item -> item.id == input.noticeId }
            ?: throw NoSuchElementException("notice not found")
    }

    suspend fun deleteCafeNoticeRemote(cafeId: String, noticeId: String) {
        val idToken = tokenProvider.getIdToken()
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFES}/$cafeId/${FirestorePaths.CAFE_NOTICES}/$noticeId"

        restApi.delete(path, idToken)
        runCatching {
            refreshCafeNoticeEventManagement(cafeId)
        }
    }

    suspend fun createCafeEventRemote(input: CafeEventCreate): CafeEventManagementItem {
        val idToken = tokenProvider.getIdToken()
        val eventId = nextFirestoreEntityId("event-management")
        val periodText = input.periodText?.trim()?.takeIf { value -> value.isNotEmpty() } ?: "게시 일정 선택 필요"
        val startDate = periodText.substringBefore(" - ", missingDelimiterValue = periodText)
        val endDate = periodText.substringAfter(" - ", missingDelimiterValue = startDate)
        val statusLabel = if (input.periodText.isNullOrBlank()) "진행 예정" else "진행 중"
        val body = firestoreDocumentBody(
            mapOf(
                "title" to firestoreString(input.title.trim()),
                "content" to firestoreString(input.content.trim()),
                "imageUrl" to firestoreString(input.imageUrl.trim()),
                "startDate" to firestoreString(startDate),
                "endDate" to firestoreString(endDate),
                "statusLabel" to firestoreString(statusLabel),
                "isDimmed" to firestoreBoolean(false)
            )
        )
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFES}/${input.cafeId}/${FirestorePaths.CAFE_EVENTS}/$eventId"

        restApi.patch(path, body, idToken)
        runCatching {
            refreshCafeNoticeEventManagement(input.cafeId)
        }
        return cafeEventManagementItems.firstOrNull { item -> item.id == eventId }
            ?: CafeEventManagementItem(
                id = eventId,
                cafeId = input.cafeId,
                title = input.title.trim(),
                content = input.content.trim(),
                imageUrl = input.imageUrl.trim(),
                startDate = startDate,
                endDate = endDate,
                statusLabel = statusLabel,
                isDimmed = false
            )
    }

    suspend fun updateCafeEventRemote(input: CafeEventUpdate): CafeEventManagementItem {
        val idToken = tokenProvider.getIdToken()
        val original = cafeEventManagementItems.firstOrNull { item -> item.id == input.eventId && item.cafeId == input.cafeId }
        val periodText = input.periodText?.trim()?.takeIf { value -> value.isNotEmpty() } ?: original?.periodText ?: "게시 일정 선택 필요"
        val startDate = periodText.substringBefore(" - ", missingDelimiterValue = periodText)
        val endDate = periodText.substringAfter(" - ", missingDelimiterValue = startDate)
        val statusLabel = if (input.periodText.isNullOrBlank()) original?.statusLabel ?: "진행 예정" else "진행 중"
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFES}/${input.cafeId}/${FirestorePaths.CAFE_EVENTS}/${input.eventId}" +
            "?updateMask.fieldPaths=title" +
            "&updateMask.fieldPaths=content" +
            "&updateMask.fieldPaths=imageUrl" +
            "&updateMask.fieldPaths=startDate" +
            "&updateMask.fieldPaths=endDate" +
            "&updateMask.fieldPaths=statusLabel" +
            "&updateMask.fieldPaths=isDimmed"
        val body = firestoreDocumentBody(
            mapOf(
                "title" to firestoreString(input.title.trim()),
                "content" to firestoreString(input.content.trim()),
                "imageUrl" to firestoreString(input.imageUrl.trim()),
                "startDate" to firestoreString(startDate),
                "endDate" to firestoreString(endDate),
                "statusLabel" to firestoreString(statusLabel),
                "isDimmed" to firestoreBoolean(false)
            )
        )

        restApi.patch(path, body, idToken)
        runCatching {
            refreshCafeNoticeEventManagement(input.cafeId)
        }
        return cafeEventManagementItems.firstOrNull { item -> item.id == input.eventId }
            ?: throw NoSuchElementException("event not found")
    }

    suspend fun deleteCafeEventRemote(cafeId: String, eventId: String) {
        val idToken = tokenProvider.getIdToken()
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFES}/$cafeId/${FirestorePaths.CAFE_EVENTS}/$eventId"

        restApi.delete(path, idToken)
        runCatching {
            refreshCafeNoticeEventManagement(cafeId)
        }
    }

    suspend fun refreshCafeReviews(cafeId: String) {
        val idToken = tokenProvider.getIdToken()
        val reviewDocuments = runCatching {
            runFieldScopedQuery(
                collectionId = FirestorePaths.REVIEWS,
                fieldPath = "cafeId",
                fieldValue = cafeId,
                idToken = idToken,
                orderByCreatedAtDesc = false
            )
        }.recoverCatching {
            runFieldScopedQuery(
                collectionId = FirestorePaths.REVIEWS,
                fieldPath = "cafeId",
                fieldValue = cafeId,
                idToken = null,
                orderByCreatedAtDesc = false
            )
        }.getOrElse { throwable ->
            println("Firestore refreshCafeReviews failed: ${throwable.message}")
            return
        }
        val parsedReviews = reviewDocuments
            .mapNotNull { document -> parseReviewDocument(document) }
            .sortedByDescending { review -> review.createdAt }

        reviews.removeAll { review -> review.cafeId == cafeId }
        reviews.addAll(parsedReviews)
        refreshReviewProjections(cafeId = cafeId, taggedCastIds = emptyList())
    }

    suspend fun getRecentTaggedReviews(
        cafeId: String,
        castId: String,
        limit: Int
    ): List<Review> {
        val safeLimit = if (limit > 0) limit else 1
        val idToken = tokenProvider.getIdToken()
        val reviewDocuments: List<JsonObject> = runCatching {
            runRecentTaggedReviewQuery(
                cafeId = cafeId,
                castId = castId,
                limit = safeLimit,
                idToken = idToken
            )
        }.recoverCatching {
            runRecentTaggedReviewQuery(
                cafeId = cafeId,
                castId = castId,
                limit = safeLimit,
                idToken = null
            )
        }.getOrElse {
            emptyList()
        }
        val parsedReviews = if (reviewDocuments.isNotEmpty()) {
            reviewDocuments.mapNotNull { document ->
                parseReviewDocument(document)
            }.sortedByDescending { review ->
                review.createdAt
            }.take(safeLimit)
        } else {
            reviews.filter { review ->
                review.cafeId == cafeId && review.taggedCastIds.contains(castId)
            }.sortedByDescending { review ->
                review.createdAt
            }.take(safeLimit)
        }

        parsedReviews.forEach { review ->
            reviews.removeAll { current -> current.id == review.id }
            reviews.add(review)
        }
        return parsedReviews
    }

    suspend fun refreshVisitsByUserRemote(userId: String) {
        if (userId.isBlank()) {
            return
        }
        val idToken = tokenProvider.getIdToken()
        val visitDocuments = runCatching {
            runUserScopedQuery(
                collectionId = FirestorePaths.VISITS,
                userId = userId,
                idToken = idToken,
                orderByFieldPath = "visitedAt",
                orderByDescending = true
            )
        }.recoverCatching {
            runUserScopedQuery(
                collectionId = FirestorePaths.VISITS,
                userId = userId,
                idToken = null,
                orderByFieldPath = "visitedAt",
                orderByDescending = true
            )
        }.getOrElse { throwable ->
            throw IllegalStateException("Failed to refresh visits for user: $userId", throwable)
        }
        val refreshedVisits = visitDocuments
            .mapNotNull { document -> parseVisitDocument(document) }

        visits.removeAll { visit -> visit.userId == userId }
        visits.addAll(refreshedVisits)
    }

    suspend fun getVerifiedVisitUserIdsByCafe(cafeId: String): Set<String> {
        val idToken = tokenProvider.getIdToken()
        val visitDocuments = runCatching {
            runVerifiedVisitUserQuery(cafeId = cafeId, idToken = idToken)
        }.recoverCatching {
            runVerifiedVisitUserQuery(cafeId = cafeId, idToken = null)
        }.getOrElse {
            emptyList()
        }
        val parsedVisits = visitDocuments.mapNotNull { document ->
            parseVisitDocument(document)
        }

        parsedVisits.forEach { visit ->
            visits.removeAll { current -> current.id == visit.id }
            visits.add(visit)
        }
        return parsedVisits
            .asSequence()
            .filter { visit -> visit.verified && visit.cafeId == cafeId }
            .map { visit -> visit.userId }
            .toSet()
    }

    suspend fun hasVerifiedVisitAtCafe(userId: String, cafeId: String): Boolean {
        val idToken = tokenProvider.getIdToken()
        val visitDocuments = runCatching {
            runVerifiedVisitByUserCafeQuery(userId = userId, cafeId = cafeId, idToken = idToken)
        }.recoverCatching {
            runVerifiedVisitByUserCafeQuery(userId = userId, cafeId = cafeId, idToken = null)
        }.getOrElse {
            emptyList()
        }
        val parsedVisits = visitDocuments.mapNotNull { document ->
            parseVisitDocument(document)
        }

        parsedVisits.forEach { visit ->
            visits.removeAll { current -> current.id == visit.id }
            visits.add(visit)
        }
        return parsedVisits.any { visit ->
            visit.userId == userId && visit.cafeId == cafeId && visit.verified
        }
    }

    suspend fun getWorkingCastIdsByCafeAndDate(cafeId: String, date: String): Set<String> {
        val idToken = tokenProvider.getIdToken()
        val scheduleDocuments = runCatching {
            runWorkingCastScheduleQuery(cafeId = cafeId, date = date, idToken = idToken)
        }.recoverCatching {
            runWorkingCastScheduleQuery(cafeId = cafeId, date = date, idToken = null)
        }.getOrElse {
            emptyList()
        }
        val parsedSchedules = scheduleDocuments.mapNotNull { document ->
            parseCastScheduleDocument(document)
        }.filter { entry ->
            entry.cafeId == cafeId && entry.date == date && entry.status == CastScheduleStatus.WORK
        }

        parsedSchedules.forEach { entry ->
            delegate.updateCastSchedule(
                CastScheduleUpdate(
                    castId = entry.castId,
                    date = entry.date,
                    status = CastScheduleStatus.WORK,
                    startTime = entry.startTime,
                    endTime = entry.endTime
                )
            )
        }
        return parsedSchedules
            .asSequence()
            .map { entry -> entry.castId }
            .toSet()
    }

    suspend fun createVisitRemote(
        userId: String,
        cafeId: String,
        visitedAt: String,
        memo: String?
    ): Visit {
        val idToken = tokenProvider.getIdToken()
        val visitId = nextFirestoreEntityId("visit")
        val now = Clock.System.now().toString()
        val path = "${config.documentBasePath()}/${FirestorePaths.VISITS}/$visitId"
        val body = firestoreDocumentBody(
            mapOf(
                "userId" to firestoreString(userId),
                "cafeId" to firestoreString(cafeId),
                "visitedAt" to firestoreString(visitedAt),
                "memo" to firestoreNullableString(memo?.trim()?.takeIf { value -> value.isNotEmpty() }),
                "verified" to firestoreBoolean(false),
                "createdAt" to firestoreString(now),
                "updatedAt" to firestoreString(now)
            )
        )

        restApi.patch(path, body, idToken)
        val createdVisit = Visit(
            id = visitId,
            userId = userId,
            cafeId = cafeId,
            visitedAt = visitedAt,
            memo = memo?.trim()?.takeIf { value -> value.isNotEmpty() },
            verified = false
        )

        val refreshedVisit = runCatching {
            refreshVisitsByUserRemote(userId)
            visits.firstOrNull { visit -> visit.id == visitId }
        }.getOrNull()

        return refreshedVisit ?: createdVisit
    }

    suspend fun updateVisitRemote(
        visitId: String,
        requesterId: String,
        visitedAt: String,
        memo: String?
    ): Visit {
        val idToken = tokenProvider.getIdToken()
        val existing = resolveVisitById(visitId = visitId, idToken = idToken)
            ?: throw NoSuchElementException("visit not found")

        if (existing.userId != requesterId) {
            throw IllegalStateException("no permission to update visit")
        }

        val path = "${config.documentBasePath()}/${FirestorePaths.VISITS}/$visitId" +
            "?updateMask.fieldPaths=visitedAt" +
            "&updateMask.fieldPaths=memo" +
            "&updateMask.fieldPaths=updatedAt"
        val body = firestoreDocumentBody(
            mapOf(
                "visitedAt" to firestoreString(visitedAt),
                "memo" to firestoreNullableString(memo?.trim()?.takeIf { value -> value.isNotEmpty() }),
                "updatedAt" to firestoreString(Clock.System.now().toString())
            )
        )

        restApi.patch(path, body, idToken)
        val fallbackUpdated = existing.copy(
            visitedAt = visitedAt,
            memo = memo?.trim()?.takeIf { value -> value.isNotEmpty() }
        )

        val refreshedVisit = runCatching {
            refreshVisitsByUserRemote(existing.userId)
            visits.firstOrNull { visit -> visit.id == visitId }
        }.getOrNull()

        return refreshedVisit ?: fallbackUpdated
    }

    suspend fun deleteVisitRemote(visitId: String, requesterId: String) {
        val idToken = tokenProvider.getIdToken()
        val existing = resolveVisitById(visitId = visitId, idToken = idToken)
            ?: throw NoSuchElementException("visit not found")

        if (existing.userId != requesterId) {
            throw IllegalStateException("no permission to delete visit")
        }

        val path = "${config.documentBasePath()}/${FirestorePaths.VISITS}/$visitId"
        restApi.delete(path, idToken)
        runCatching {
            refreshVisitsByUserRemote(existing.userId)
        }.onFailure {
            visits.removeAll { visit -> visit.id == visitId }
        }
    }

    suspend fun createReviewRemote(
        userId: String,
        cafeId: String,
        visitId: String,
        rating: Float,
        content: String,
        imageUrls: List<String>,
        taggedCastIds: List<String>
    ): Review {
        val idToken = tokenProvider.getIdToken()
        val userNickname = delegate.findUserById(userId)?.nickname.orEmpty()
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

        val created = Review(
            id = reviewId,
            userId = userId,
            cafeId = cafeId,
            visitId = visitId,
            rating = rating,
            content = content.trim(),
            imageUrls = imageUrls,
            taggedCastIds = taggedCastIds,
            likeCount = 0,
            createdAt = createdAt,
            visitVerified = false,
            userNickname = userNickname
        )
        reviews.removeAll { review -> review.id == reviewId }
        reviews.add(created)
        refreshReviewProjections(cafeId = cafeId, taggedCastIds = taggedCastIds)

        return created
    }

    suspend fun updateReviewRemote(
        reviewId: String,
        requesterId: String,
        rating: Float,
        content: String,
        imageUrls: List<String>,
        taggedCastIds: List<String>
    ): Review {
        val idToken = tokenProvider.getIdToken()
        val existing = resolveReviewById(reviewId = reviewId, idToken = idToken)
            ?: throw NoSuchElementException("review not found")

        if (existing.userId != requesterId) {
            throw IllegalStateException("no permission to update review")
        }

        val path = "${config.documentBasePath()}/${FirestorePaths.REVIEWS}/$reviewId" +
            "?updateMask.fieldPaths=rating" +
            "&updateMask.fieldPaths=content" +
            "&updateMask.fieldPaths=imageUrls" +
            "&updateMask.fieldPaths=taggedCastIds" +
            "&updateMask.fieldPaths=updatedAt"
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

        val updated = existing.copy(
            rating = rating,
            content = content.trim(),
            imageUrls = imageUrls,
            taggedCastIds = taggedCastIds
        )
        val index = reviews.indexOfFirst { review -> review.id == reviewId }

        if (index >= 0) {
            reviews[index] = updated
        } else {
            reviews.add(updated)
        }
        refreshReviewProjections(cafeId = updated.cafeId, taggedCastIds = updated.taggedCastIds)

        return updated
    }

    suspend fun deleteReviewRemote(reviewId: String, requesterId: String) {
        val idToken = tokenProvider.getIdToken()
        val existing = resolveReviewById(reviewId = reviewId, idToken = idToken)
            ?: throw NoSuchElementException("review not found")

        if (existing.userId != requesterId) {
            throw IllegalStateException("no permission to delete review")
        }

        val path = "${config.documentBasePath()}/${FirestorePaths.REVIEWS}/$reviewId"
        restApi.delete(path, idToken)

        reviews.removeAll { review -> review.id == reviewId }
        refreshReviewProjections(cafeId = existing.cafeId, taggedCastIds = existing.taggedCastIds)
    }

    suspend fun likeReviewRemote(reviewId: String) {
        val idToken = tokenProvider.getIdToken()
        val existing = resolveReviewById(reviewId = reviewId, idToken = idToken)
            ?: throw NoSuchElementException("review not found")
        val nextLikeCount = existing.likeCount + 1
        val path = "${config.documentBasePath()}/${FirestorePaths.REVIEWS}/$reviewId?updateMask.fieldPaths=likeCount"
        val body = firestoreDocumentBody(
            mapOf(
                "likeCount" to firestoreLong(nextLikeCount.toLong())
            )
        )

        restApi.patch(path, body, idToken)

        val index = reviews.indexOfFirst { review -> review.id == reviewId }

        if (index >= 0) {
            reviews[index] = existing.copy(likeCount = nextLikeCount)
        } else {
            reviews.add(existing.copy(likeCount = nextLikeCount))
        }
    }

    suspend fun refreshFollowedCastIds(userId: String) {
        val idToken = tokenProvider.getIdToken()
        val followDocuments = runCatching {
            runUserScopedQuery(
                collectionId = FirestorePaths.CAST_FOLLOWS,
                userId = userId,
                idToken = idToken
            )
        }.recoverCatching {
            runUserScopedQuery(
                collectionId = FirestorePaths.CAST_FOLLOWS,
                userId = userId,
                idToken = null
            )
        }.getOrElse { emptyList() }
        val followedCastIds = followDocuments
            .mapNotNull { document ->
                document["fields"]?.jsonObject?.getFirestoreString("castId")
            }
            .toMutableSet()

        followedCastIdsByUser[userId] = followedCastIds
    }

    suspend fun refreshFavoriteCafeIds(userId: String) {
        val idToken = tokenProvider.getIdToken()
        val favoriteDocuments = runCatching {
            runUserScopedQuery(
                collectionId = FirestorePaths.CAFE_FAVORITES,
                userId = userId,
                idToken = idToken
            )
        }.recoverCatching {
            runUserScopedQuery(
                collectionId = FirestorePaths.CAFE_FAVORITES,
                userId = userId,
                idToken = null
            )
        }.getOrElse { emptyList() }
        val favoriteCafeIds = favoriteDocuments
            .mapNotNull { document ->
                document["fields"]?.jsonObject?.getFirestoreString("cafeId")
            }
            .toMutableSet()

        favoriteCafeIdsByUser[userId] = favoriteCafeIds
    }

    suspend fun refreshFavoriteUserIds(cafeId: String) {
        val idToken = tokenProvider.getIdToken()
        val favoriteDocuments = runCatching {
            runFieldScopedQuery(
                collectionId = FirestorePaths.CAFE_FAVORITES,
                fieldPath = "cafeId",
                fieldValue = cafeId,
                idToken = idToken,
                orderByCreatedAtDesc = false
            )
        }.recoverCatching {
            runFieldScopedQuery(
                collectionId = FirestorePaths.CAFE_FAVORITES,
                fieldPath = "cafeId",
                fieldValue = cafeId,
                idToken = null,
                orderByCreatedAtDesc = false
            )
        }.getOrElse { emptyList() }
        val favoriteUserIds = favoriteDocuments
            .mapNotNull { document ->
                document["fields"]?.jsonObject?.getFirestoreString("userId")
            }
            .toMutableSet()

        favoriteUserIdsByCafeId[cafeId] = favoriteUserIds
    }

    suspend fun refreshFollowerUserIds(castId: String) {
        val idToken = tokenProvider.getIdToken()
        val followDocuments = runCatching {
            runFieldScopedQuery(
                collectionId = FirestorePaths.CAST_FOLLOWS,
                fieldPath = "castId",
                fieldValue = castId,
                idToken = idToken,
                orderByCreatedAtDesc = false
            )
        }.recoverCatching {
            runFieldScopedQuery(
                collectionId = FirestorePaths.CAST_FOLLOWS,
                fieldPath = "castId",
                fieldValue = castId,
                idToken = null,
                orderByCreatedAtDesc = false
            )
        }.getOrElse { emptyList() }
        val followerIds = followDocuments
            .mapNotNull { document ->
                document["fields"]?.jsonObject?.getFirestoreString("userId")
            }
            .toMutableSet()

        followerUserIdsByCastId[castId] = followerIds
        syncCastFollowerCountInCache(castId = castId, followerCount = followerIds.size)
    }

    suspend fun followCastRemote(userId: String, castId: String) {
        val idToken = tokenProvider.getIdToken()
        val cast = delegate.casts.firstOrNull { item -> item.id == castId }
            ?: runCatching {
                refreshCastDetailRemote(castId)
                delegate.casts.firstOrNull { item -> item.id == castId }
            }.getOrNull()
            ?: throw NoSuchElementException("cast not found")
        runCatching { refreshFollowedCastIds(userId) }
        runCatching { refreshFollowerUserIds(castId) }
        val followId = buildCastFollowDocumentId(userId = userId, castId = castId)
        val path = "${config.documentBasePath()}/${FirestorePaths.CAST_FOLLOWS}/$followId"
        val createdAt = Clock.System.now().toString()
        val body = firestoreDocumentBody(
            mapOf(
                "userId" to firestoreString(userId),
                "castId" to firestoreString(castId),
                "cafeId" to firestoreString(cast.cafeId),
                "createdAt" to firestoreString(createdAt)
            )
        )

        restApi.patch(path, body, idToken)
        val followedSet = followedCastIdsByUser.getOrPut(userId) { mutableSetOf() }
        val followerSet = followerUserIdsByCastId.getOrPut(castId) { mutableSetOf() }

        followedSet.add(castId)
        followerSet.add(userId)
        syncCastFollowerCountInCache(castId = castId, followerCount = followerSet.size)
    }

    suspend fun unfollowCastRemote(userId: String, castId: String) {
        val idToken = tokenProvider.getIdToken()
        runCatching { refreshFollowedCastIds(userId) }
        runCatching { refreshFollowerUserIds(castId) }
        val followId = buildCastFollowDocumentId(userId = userId, castId = castId)
        val path = "${config.documentBasePath()}/${FirestorePaths.CAST_FOLLOWS}/$followId"

        runCatching {
            restApi.delete(path, idToken)
        }
        val followedSet = followedCastIdsByUser.getOrPut(userId) { mutableSetOf() }
        val followerSet = followerUserIdsByCastId.getOrPut(castId) { mutableSetOf() }

        followedSet.remove(castId)
        followerSet.remove(userId)
        syncCastFollowerCountInCache(castId = castId, followerCount = followerSet.size)
    }

    suspend fun favoriteCafeRemote(userId: String, cafeId: String) {
        val idToken = tokenProvider.getIdToken()

        runCatching { refreshFavoriteCafeIds(userId) }
        runCatching { refreshFavoriteUserIds(cafeId) }

        val favoriteId = buildCafeFavoriteDocumentId(userId = userId, cafeId = cafeId)
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFE_FAVORITES}/$favoriteId"
        val createdAt = Clock.System.now().toString()
        val body = firestoreDocumentBody(
            mapOf(
                "userId" to firestoreString(userId),
                "cafeId" to firestoreString(cafeId),
                "createdAt" to firestoreString(createdAt)
            )
        )

        restApi.patch(path, body, idToken)
        val favoriteCafeSet = favoriteCafeIdsByUser.getOrPut(userId) { mutableSetOf() }
        val favoriteUserSet = favoriteUserIdsByCafeId.getOrPut(cafeId) { mutableSetOf() }

        favoriteCafeSet.add(cafeId)
        favoriteUserSet.add(userId)
    }

    suspend fun unfavoriteCafeRemote(userId: String, cafeId: String) {
        val idToken = tokenProvider.getIdToken()

        runCatching { refreshFavoriteCafeIds(userId) }
        runCatching { refreshFavoriteUserIds(cafeId) }

        val favoriteId = buildCafeFavoriteDocumentId(userId = userId, cafeId = cafeId)
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFE_FAVORITES}/$favoriteId"

        runCatching {
            restApi.delete(path, idToken)
        }
        val favoriteCafeSet = favoriteCafeIdsByUser.getOrPut(userId) { mutableSetOf() }
        val favoriteUserSet = favoriteUserIdsByCafeId.getOrPut(cafeId) { mutableSetOf() }

        favoriteCafeSet.remove(cafeId)
        favoriteUserSet.remove(userId)
    }

    suspend fun hasReviewForVisitRemote(visitId: String): Boolean {
        if (visitId.isBlank()) {
            return false
        }
        val idToken = tokenProvider.getIdToken()
        val reviewDocuments = runCatching {
            runFieldScopedQuery(
                collectionId = FirestorePaths.REVIEWS,
                fieldPath = "visitId",
                fieldValue = visitId,
                idToken = idToken,
                orderByCreatedAtDesc = false
            )
        }.recoverCatching {
            runFieldScopedQuery(
                collectionId = FirestorePaths.REVIEWS,
                fieldPath = "visitId",
                fieldValue = visitId,
                idToken = null,
                orderByCreatedAtDesc = false
            )
        }.getOrElse { emptyList() }
        val parsedReviews = reviewDocuments.mapNotNull { document -> parseReviewDocument(document) }

        if (parsedReviews.isNotEmpty()) {
            val first = parsedReviews.first()
            reviews.removeAll { review -> review.id == first.id }
            reviews.add(first)
        }

        return parsedReviews.isNotEmpty()
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
        val updated = delegate.updateCafeInfo(
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
        hydratedCafeDetailIds.add(update.cafeId)
        return updated
    }

    suspend fun upsertCastRemote(update: CastUpsert): CastDetail {
        require(update.name.isNotBlank()) { "cast name is required" }
        require(update.conceptRole.isNotBlank()) { "concept role is required" }

        val existingCast = update.castId
            ?.takeIf { value -> value.isNotBlank() }
            ?.let { castId -> casts.firstOrNull { cast -> cast.id == castId } }
        val targetCafeId = update.cafeId
            ?.takeIf { value -> value.isNotBlank() }
            ?: existingCast?.cafeId
            ?: throw IllegalArgumentException("cafeId is required")
        val targetCafe = cafes.firstOrNull { cafe -> cafe.id == targetCafeId }
            ?: throw NoSuchElementException("cafe not found")
        val castId = existingCast?.id
            ?: update.castId?.takeIf { value -> value.isNotBlank() }
            ?: nextFirestoreEntityId("cast")
        val normalizedName = update.name.trim()
        val normalizedConceptRole = update.conceptRole.trim()
        val normalizedIntroduction = update.introduction.trim()
        val normalizedBirthday = update.birthday?.trim()?.takeIf { value -> value.isNotEmpty() }
        val normalizedProfileImage = update.profileImage
            ?.trim()
            ?.takeIf { value -> value.isNotEmpty() }
            ?: existingCast?.profileImage
        val normalizedGalleryImages = update.galleryImages
            .map { image -> image.trim() }
            .filter { image -> image.isNotEmpty() }
        val linkedUserId = existingCast?.linkedUserId ?: currentUserId
        val idToken = tokenProvider.getIdToken()
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFES}/$targetCafeId/${FirestorePaths.CAFE_CASTS}/$castId"
        val body = firestoreDocumentBody(
            mapOf(
                "name" to firestoreString(normalizedName),
                "linkedUserId" to firestoreNullableString(linkedUserId),
                "profileImage" to firestoreNullableString(normalizedProfileImage),
                "desc" to firestoreString(normalizedIntroduction),
                "birthday" to firestoreNullableString(normalizedBirthday),
                "conceptRole" to firestoreString(normalizedConceptRole),
                "followerCount" to firestoreLong((existingCast?.followerCount ?: 0).toLong()),
                "rating" to firestoreDouble(existingCast?.rating ?: 0.0),
                "galleryImages" to firestoreStringArray(normalizedGalleryImages)
            )
        )

        restApi.patch(path, body, idToken)
        runCatching {
            refreshCafeDetail(targetCafeId)
        }

        val cached = delegate.upsertCast(
            update.copy(
                castId = castId,
                cafeId = targetCafe.id,
                name = normalizedName,
                conceptRole = normalizedConceptRole,
                birthday = normalizedBirthday,
                introduction = normalizedIntroduction,
                profileImage = normalizedProfileImage,
                galleryImages = normalizedGalleryImages
            )
        )
        hydratedCafeDetailIds.add(targetCafeId)
        return cached
    }

    suspend fun deleteCastRemote(castId: String): Cast {
        require(castId.isNotBlank()) { "castId is required" }

        val existingCast = casts.firstOrNull { cast -> cast.id == castId }
            ?: throw NoSuchElementException("cast detail not found")
        val idToken = tokenProvider.getIdToken()
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFES}/${existingCast.cafeId}/${FirestorePaths.CAFE_CASTS}/$castId"

        restApi.delete(path, idToken)
        runCatching {
            refreshCafeDetail(existingCast.cafeId)
        }
        val deleted = delegate.deleteCast(castId)
        hydratedCafeDetailIds.add(existingCast.cafeId)
        return deleted
    }

    suspend fun refreshCastSchedulesRemote(
        castId: String,
        fromDate: String,
        toDate: String
    ) {
        require(castId.isNotBlank()) { "castId is required" }
        val idToken = tokenProvider.getIdToken()
        val schedulesDocuments = runCatching {
            runCastScheduleRangeQuery(
                castId = castId,
                fromDate = fromDate,
                toDate = toDate,
                idToken = idToken
            )
        }.recoverCatching {
            runCastScheduleByCastIdQuery(
                castId = castId,
                idToken = idToken
            )
        }.recoverCatching {
            runCastScheduleRangeQuery(
                castId = castId,
                fromDate = fromDate,
                toDate = toDate,
                idToken = null
            )
        }.recoverCatching {
            runCastScheduleByCastIdQuery(
                castId = castId,
                idToken = null
            )
        }.getOrElse {
            emptyList()
        }

        val remoteSchedulesByDate = schedulesDocuments.mapNotNull { document ->
            parseCastScheduleDocument(document)
        }.filter { scheduleEntry ->
            scheduleEntry.castId == castId && scheduleEntry.date >= fromDate && scheduleEntry.date <= toDate
        }.associateBy { scheduleEntry -> scheduleEntry.date }

        enumerateDates(fromDate, toDate).forEach { date ->
            val scheduleEntry = remoteSchedulesByDate[date]
            val status = scheduleEntry?.status ?: CastScheduleStatus.OFF
            val update = if (
                status == CastScheduleStatus.WORK &&
                scheduleEntry?.startTime != null &&
                scheduleEntry.endTime != null
            ) {
                CastScheduleUpdate(
                    castId = castId,
                    date = date,
                    status = CastScheduleStatus.WORK,
                    startTime = scheduleEntry.startTime,
                    endTime = scheduleEntry.endTime
                )
            } else {
                CastScheduleUpdate(
                    castId = castId,
                    date = date,
                    status = if (status == CastScheduleStatus.WORK) CastScheduleStatus.OFF else status,
                    startTime = null,
                    endTime = null
                )
            }
            delegate.updateCastSchedule(update)
        }
    }

    suspend fun refreshCastDetailRemote(castId: String) {
        require(castId.isNotBlank()) { "castId is required" }
        val idToken = tokenProvider.getIdToken()
        refreshCastSummaryRemote(castId = castId, idToken = idToken)
        val currentDate = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
        val fromDate = (currentDate + DatePeriod(days = -30)).toString()
        val toDate = (currentDate + DatePeriod(days = 60)).toString()
        runCatching {
            refreshCastSchedulesRemote(castId = castId, fromDate = fromDate, toDate = toDate)
        }
            .getOrElse { throwable ->
                throw IllegalStateException("Failed to refresh cast schedules for cast detail", throwable)
            }
    }

    suspend fun updateCastScheduleRemote(update: CastScheduleUpdate): CastSchedule? {
        require(update.castId.isNotBlank()) { "castId is required" }
        val cast = casts.firstOrNull { candidate -> candidate.id == update.castId }
            ?: throw NoSuchElementException("cast detail not found")
        val idToken = tokenProvider.getIdToken()
        val documentId = "${update.castId}_${update.date}"
        val schedulePath = "${config.documentBasePath()}/${FirestorePaths.CAST_SCHEDULES}/$documentId"

        when (update.status) {
            CastScheduleStatus.WORK -> {
                val startTime = update.startTime?.trim()?.takeIf { value -> value.isNotEmpty() }
                    ?: throw IllegalArgumentException("start time is required")
                val endTime = update.endTime?.trim()?.takeIf { value -> value.isNotEmpty() }
                    ?: throw IllegalArgumentException("end time is required")
                require(startTime < endTime) { "end time must be after start time" }

                val scheduleBody = firestoreDocumentBody(
                    mapOf(
                        "castId" to firestoreString(update.castId),
                        "cafeId" to firestoreString(cast.cafeId),
                        "date" to firestoreString(update.date),
                        "startTime" to firestoreString(startTime),
                        "endTime" to firestoreString(endTime),
                        "status" to firestoreString(CastScheduleStatus.WORK.name)
                    )
                )

                restApi.patch(schedulePath, scheduleBody, idToken)
            }
            CastScheduleStatus.OFF,
            CastScheduleStatus.VACATION -> {
                val scheduleBody = firestoreDocumentBody(
                    mapOf(
                        "castId" to firestoreString(update.castId),
                        "cafeId" to firestoreString(cast.cafeId),
                        "date" to firestoreString(update.date),
                        "startTime" to firestoreNullableString(null),
                        "endTime" to firestoreNullableString(null),
                        "status" to firestoreString(update.status.name)
                    )
                )
                restApi.patch(schedulePath, scheduleBody, idToken)
            }
        }

        val updated = delegate.updateCastSchedule(update)
        runCatching {
            refreshCastSchedulesRemote(update.castId, update.date, update.date)
        }
        return updated
    }

    suspend fun upsertCafeMenuGoodsRemote(update: CafeMenuGoodsUpsert): CafeDetail {
        require(update.cafeId.isNotBlank()) { "cafeId is required" }
        require(update.name.isNotBlank()) { "name is required" }
        require(update.price >= 0) { "price must be zero or positive" }
        require(update.category.isNotBlank()) { "category is required" }

        val idToken = tokenProvider.getIdToken()
        val normalizedCategory = update.category.trim().lowercase()
        val isGoodsCategory = normalizedCategory == "goods"
        val itemId = update.itemId?.trim()?.takeIf { value -> value.isNotEmpty() }
            ?: nextFirestoreEntityId(if (isGoodsCategory) "goods" else "menu")
        val cachedDetail = delegate.cafeDetailsById[update.cafeId]
        val existingMenu = cachedDetail?.menus?.firstOrNull { menu -> menu.id == itemId }
        val existingGoods = cachedDetail?.goods?.firstOrNull { goods -> goods.id == itemId }
        val resolvedImage = update.imageUrl
            ?.trim()
            ?.takeIf { value -> value.isNotEmpty() }
            ?: existingMenu?.image
            ?: existingGoods?.image

        if (isGoodsCategory) {
            val stock = if (update.isInStock) {
                when {
                    existingGoods?.stock != null && existingGoods.stock > 0 -> existingGoods.stock
                    else -> 50
                }
            } else {
                0
            }
            val body = firestoreDocumentBody(
                mapOf(
                    "name" to firestoreString(update.name.trim()),
                    "price" to firestoreLong(update.price.toLong()),
                    "image" to firestoreNullableString(resolvedImage),
                    "stock" to firestoreLong(stock.toLong())
                )
            )
            val goodsPath = "${config.documentBasePath()}/${FirestorePaths.CAFES}/${update.cafeId}/${FirestorePaths.CAFE_GOODS}/$itemId"
            restApi.patch(goodsPath, body, idToken)
            if (existingMenu != null) {
                val menuPath = "${config.documentBasePath()}/${FirestorePaths.CAFES}/${update.cafeId}/${FirestorePaths.CAFE_MENUS}/$itemId"
                runCatching { restApi.delete(menuPath, idToken) }
            }
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
            val menuPath = "${config.documentBasePath()}/${FirestorePaths.CAFES}/${update.cafeId}/${FirestorePaths.CAFE_MENUS}/$itemId"
            restApi.patch(menuPath, body, idToken)
            if (existingGoods != null) {
                val goodsPath = "${config.documentBasePath()}/${FirestorePaths.CAFES}/${update.cafeId}/${FirestorePaths.CAFE_GOODS}/$itemId"
                runCatching { restApi.delete(goodsPath, idToken) }
            }
        }
        refreshCafeDetail(update.cafeId)
        return delegate.cafeDetailsById[update.cafeId]
            ?: throw NoSuchElementException("cafe detail not found")
    }

    suspend fun deleteCafeMenuGoodsRemote(cafeId: String, itemId: String): CafeDetail {
        require(cafeId.isNotBlank()) { "cafeId is required" }
        require(itemId.isNotBlank()) { "itemId is required" }

        val idToken = tokenProvider.getIdToken()
        val cachedDetail = delegate.cafeDetailsById[cafeId]
        val targetIsMenu = cachedDetail?.menus?.any { menu -> menu.id == itemId } == true
        val targetIsGoods = cachedDetail?.goods?.any { goods -> goods.id == itemId } == true
        var deleted = false

        if (targetIsMenu || !targetIsGoods) {
            val menuPath = "${config.documentBasePath()}/${FirestorePaths.CAFES}/$cafeId/${FirestorePaths.CAFE_MENUS}/$itemId"
            if (runCatching { restApi.delete(menuPath, idToken) }.isSuccess) {
                deleted = true
            }
        }
        if (targetIsGoods || !targetIsMenu) {
            val goodsPath = "${config.documentBasePath()}/${FirestorePaths.CAFES}/$cafeId/${FirestorePaths.CAFE_GOODS}/$itemId"
            if (runCatching { restApi.delete(goodsPath, idToken) }.isSuccess) {
                deleted = true
            }
        }
        if (!deleted) {
            throw NoSuchElementException("menu goods item not found")
        }

        refreshCafeDetail(cafeId)
        return delegate.cafeDetailsById[cafeId]
            ?: throw NoSuchElementException("cafe detail not found")
    }

    override suspend fun fetchUser(userId: String): User? {
        val idToken = tokenProvider.getIdToken()
        val path = "${config.documentBasePath()}/${FirestorePaths.USERS}/$userId"
        return runCatching {
            val response = restApi.get(path, idToken)
            val parsed = Json.parseToJsonElement(response).jsonObject
            val user = parseUserDocument(parsed)
            val affiliatedCafeId = parseUserAffiliatedCafeId(parsed)

            if (user != null && !affiliatedCafeId.isNullOrBlank()) {
                delegate.affiliatedCafeIdByUser[user.id] = affiliatedCafeId
            } else if (user != null) {
                delegate.affiliatedCafeIdByUser.remove(user.id)
            }
            user
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
        val affiliatedCafeId = delegate.affiliatedCafeIdByUser[user.id]
        val body = firestoreDocumentBody(
            mapOf(
                "email" to firestoreString(user.email),
                "nickname" to firestoreString(user.nickname),
                "profileImage" to firestoreNullableString(user.profileImage),
                "role" to firestoreString(user.role.name),
                "banned" to firestoreBoolean(user.banned),
                "createdAt" to firestoreString(user.createdAt),
                "affiliatedCafeId" to firestoreNullableString(affiliatedCafeId)
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

    override suspend fun refreshHomeBanners() {
        val idToken = tokenProvider.getIdToken()
        runCatching {
            loadHomeBanners(idToken = idToken)
        }.recoverCatching {
            loadHomeBanners(idToken = null)
        }.getOrThrow()
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

    override suspend fun fetchAdminOperationsMetrics(): AdminOperationsMetrics {
        val idToken = tokenProvider.getIdToken()
        val totalUsersCount = loadCollectionDocumentCount(
            collectionId = FirestorePaths.USERS,
            idToken = idToken
        )
        val totalCafesCount = loadCollectionDocumentCount(
            collectionId = FirestorePaths.CAFES,
            idToken = idToken
        )
        val approvedCafesCount = loadCollectionDocumentCount(
            collectionId = FirestorePaths.CAFES,
            idToken = idToken,
            equalsFilterFieldPath = "approved",
            equalsFilterValue = firestoreBoolean(true)
        )
        val reportItemsCount = runCatching {
            loadCollectionDocumentCount(
                collectionId = FirestorePaths.REPORTS,
                idToken = idToken
            )
        }.getOrElse { 0 }
        val activeCafesCount = if (approvedCafesCount == 0 && totalCafesCount > 0) {
            totalCafesCount
        } else {
            approvedCafesCount
        }

        return AdminOperationsMetrics(
            totalUsersCount = totalUsersCount,
            activeCafesCount = activeCafesCount,
            reportItemsCount = reportItemsCount
        )
    }

    private suspend fun loadUsers(idToken: String?) {
        val response = restApi.get("${config.documentBasePath()}/${FirestorePaths.USERS}", idToken)
        val parsed = Json.parseToJsonElement(response).jsonObject
        val documents = parsed["documents"]?.jsonArray.orEmpty()
        val affiliatedCafeByUser = mutableMapOf<String, String>()
        val users = documents.mapNotNull { element ->
            val document = element.jsonObject
            val user = parseUserDocument(document) ?: return@mapNotNull null
            val affiliatedCafeId = parseUserAffiliatedCafeId(document)

            if (!affiliatedCafeId.isNullOrBlank()) {
                affiliatedCafeByUser[user.id] = affiliatedCafeId
            }
            user
        }

        replaceAllUsers(users)
        delegate.affiliatedCafeIdByUser.clear()
        delegate.affiliatedCafeIdByUser.putAll(affiliatedCafeByUser)
    }

    private suspend fun loadHomeBanners(idToken: String?) {
        val response = restApi.get("${config.documentBasePath()}/${FirestorePaths.HOME_BANNERS}", idToken)
        val parsed = Json.parseToJsonElement(response).jsonObject
        val documents = parsed["documents"]?.jsonArray.orEmpty()
        val banners = documents.mapNotNull { element ->
            parseHomeBannerDocument(element.jsonObject)
        }

        this.banners.clear()
        this.banners.addAll(banners.sortedByDescending { banner -> banner.createdAtEpochMillis })
        rebuildCafeHomeBannerPreviewByCafeId()
    }

    private fun rebuildCafeHomeBannerPreviewByCafeId() {
        val previewByCafeId = banners
            .asSequence()
            .mapNotNull { banner ->
                val cafeId = banner.cafeId?.takeIf { value -> value.isNotBlank() } ?: return@mapNotNull null
                cafeId to banner
            }
            .groupBy(keySelector = { entry -> entry.first }, valueTransform = { entry -> entry.second })
            .mapValues { entry ->
                entry.value
                    .sortedWith(
                        compareByDescending<HomeBanner> { banner -> banner.statusLabel.uppercase() == "ACTIVE" }
                            .thenByDescending { banner -> banner.createdAtEpochMillis }
                    )
                    .first()
            }

        cafeHomeBannerPreviewByCafeId.clear()
        previewByCafeId.forEach { entry ->
            val statusLabel = when (entry.value.statusLabel.uppercase()) {
                "ACTIVE" -> "노출 중"
                "SCHEDULED" -> "예약 중"
                else -> "미노출"
            }
            cafeHomeBannerPreviewByCafeId[entry.key] = CafeDashboardData.HomeBannerPreview(
                title = entry.value.title,
                period = resolvePeriodLabel(entry.value.displayDays),
                statusLabel = statusLabel,
                imageUrl = entry.value.imageUrl
            )
        }
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

    private suspend fun loadReviews(idToken: String?) {
        val response = restApi.get("${config.documentBasePath()}/${FirestorePaths.REVIEWS}", idToken)
        val parsed = Json.parseToJsonElement(response).jsonObject
        val documents = parsed["documents"]?.jsonArray.orEmpty()
        val parsedReviews = documents
            .mapNotNull { element -> parseReviewDocument(element.jsonObject) }
            .sortedByDescending { review -> review.createdAt }

        this.reviews.clear()
        this.reviews.addAll(parsedReviews)
        cafes.forEach { cafe ->
            refreshReviewProjections(cafeId = cafe.id, taggedCastIds = emptyList())
        }
    }

    private suspend fun loadVisits(idToken: String?) {
        val visitDocuments = runCollectionQuery(
            collectionId = FirestorePaths.VISITS,
            idToken = idToken,
            orderByFieldPath = "visitedAt",
            orderByDescending = true
        )
        val loadedVisits = visitDocuments.mapNotNull { document ->
            parseVisitDocument(document)
        }

        this.visits.clear()
        this.visits.addAll(loadedVisits)
    }

    private suspend fun runCollectionQuery(
        collectionId: String,
        idToken: String?,
        orderByFieldPath: String? = null,
        orderByDescending: Boolean = false
    ): List<JsonObject> {
        val path = "${config.documentBasePath()}:runQuery"
        val orderBySection = if (orderByFieldPath != null) {
            val direction = if (orderByDescending) {
                "DESCENDING"
            } else {
                "ASCENDING"
            }

            """
            ,
                "orderBy": [
                  {
                    "field": { "fieldPath": "${escapeFirestoreQueryString(orderByFieldPath)}" },
                    "direction": "$direction"
                  }
                ]
            """.trimIndent()
        } else {
            ""
        }
        val body = """
            {
              "structuredQuery": {
                "from": [
                  { "collectionId": "$collectionId" }
                ]$orderBySection
              }
            }
        """.trimIndent()
        val response = restApi.post(path = path, body = body, idToken = idToken)
        val parsed = Json.parseToJsonElement(response).jsonArray
        return parsed.mapNotNull { element ->
            element.jsonObject["document"]?.jsonObject
        }
    }

    private suspend fun runUserScopedQuery(
        collectionId: String,
        userId: String,
        idToken: String?,
        orderByFieldPath: String? = null,
        orderByDescending: Boolean = false
    ): List<JsonObject> {
        val path = "${config.documentBasePath()}:runQuery"
        val orderBySection = if (orderByFieldPath != null) {
            val direction = if (orderByDescending) {
                "DESCENDING"
            } else {
                "ASCENDING"
            }

            """
            ,
                "orderBy": [
                  {
                    "field": { "fieldPath": "${escapeFirestoreQueryString(orderByFieldPath)}" },
                    "direction": "$direction"
                  }
                ]
            """.trimIndent()
        } else {
            ""
        }
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
                  }$orderBySection
                }
            }
        """.trimIndent()
        val response = restApi.post(path = path, body = body, idToken = idToken)
        val parsed = Json.parseToJsonElement(response).jsonArray

        return parsed.mapNotNull { element ->
            element.jsonObject["document"]?.jsonObject
        }
    }

    private suspend fun runFieldScopedQuery(
        collectionId: String,
        fieldPath: String,
        fieldValue: String,
        idToken: String?,
        orderByCreatedAtDesc: Boolean
    ): List<JsonObject> {
        val path = "${config.documentBasePath()}:runQuery"
        val orderBySection = if (orderByCreatedAtDesc) {
            """
            ,
                "orderBy": [
                  {
                    "field": { "fieldPath": "createdAt" },
                    "direction": "DESCENDING"
                  }
                ]
            """.trimIndent()
        } else {
            ""
        }
        val body = """
            {
              "structuredQuery": {
                "from": [
                  { "collectionId": "$collectionId" }
                ],
                "where": {
                    "fieldFilter": {
                      "field": { "fieldPath": "$fieldPath" },
                      "op": "EQUAL",
                      "value": { "stringValue": "${escapeFirestoreQueryString(fieldValue)}" }
                    }
                  }$orderBySection
                }
            }
        """.trimIndent()
        val response = restApi.post(path = path, body = body, idToken = idToken)
        val parsed = Json.parseToJsonElement(response).jsonArray

        return parsed.mapNotNull { element ->
            element.jsonObject["document"]?.jsonObject
        }
    }

    private suspend fun runCastScheduleRangeQuery(
        castId: String,
        fromDate: String,
        toDate: String,
        idToken: String?
    ): List<JsonObject> {
        val path = "${config.documentBasePath()}:runQuery"
        val body = """
            {
              "structuredQuery": {
                "from": [
                  { "collectionId": "${FirestorePaths.CAST_SCHEDULES}" }
                ],
                "where": {
                  "compositeFilter": {
                    "op": "AND",
                    "filters": [
                      {
                        "fieldFilter": {
                          "field": { "fieldPath": "castId" },
                          "op": "EQUAL",
                          "value": { "stringValue": "${escapeFirestoreQueryString(castId)}" }
                        }
                      },
                      {
                        "fieldFilter": {
                          "field": { "fieldPath": "date" },
                          "op": "GREATER_THAN_OR_EQUAL",
                          "value": { "stringValue": "${escapeFirestoreQueryString(fromDate)}" }
                        }
                      },
                      {
                        "fieldFilter": {
                          "field": { "fieldPath": "date" },
                          "op": "LESS_THAN_OR_EQUAL",
                          "value": { "stringValue": "${escapeFirestoreQueryString(toDate)}" }
                        }
                      }
                    ]
                  }
                },
                "orderBy": [
                  {
                    "field": { "fieldPath": "date" },
                    "direction": "ASCENDING"
                  }
                ]
              }
            }
        """.trimIndent()
        val response = restApi.post(path = path, body = body, idToken = idToken)
        val parsed = Json.parseToJsonElement(response).jsonArray

        return parsed.mapNotNull { element ->
            element.jsonObject["document"]?.jsonObject
        }
    }

    private suspend fun runRecentTaggedReviewQuery(
        cafeId: String,
        castId: String,
        limit: Int,
        idToken: String?
    ): List<JsonObject> {
        val safeLimit = if (limit > 0) {
            limit
        } else {
            1
        }
        val path = "${config.documentBasePath()}:runQuery"
        val body = """
            {
              "structuredQuery": {
                "from": [
                  { "collectionId": "${FirestorePaths.REVIEWS}" }
                ],
                "where": {
                  "compositeFilter": {
                    "op": "AND",
                    "filters": [
                      {
                        "fieldFilter": {
                          "field": { "fieldPath": "cafeId" },
                          "op": "EQUAL",
                          "value": { "stringValue": "${escapeFirestoreQueryString(cafeId)}" }
                        }
                      },
                      {
                        "fieldFilter": {
                          "field": { "fieldPath": "taggedCastIds" },
                          "op": "ARRAY_CONTAINS",
                          "value": { "stringValue": "${escapeFirestoreQueryString(castId)}" }
                        }
                      }
                    ]
                  }
                },
                "orderBy": [
                  {
                    "field": { "fieldPath": "createdAt" },
                    "direction": "DESCENDING"
                  }
                ],
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

    private suspend fun runVerifiedVisitUserQuery(
        cafeId: String,
        idToken: String?
    ): List<JsonObject> {
        val path = "${config.documentBasePath()}:runQuery"
        val body = """
            {
              "structuredQuery": {
                "from": [
                  { "collectionId": "${FirestorePaths.VISITS}" }
                ],
                "where": {
                  "compositeFilter": {
                    "op": "AND",
                    "filters": [
                      {
                        "fieldFilter": {
                          "field": { "fieldPath": "cafeId" },
                          "op": "EQUAL",
                          "value": { "stringValue": "${escapeFirestoreQueryString(cafeId)}" }
                        }
                      },
                      {
                        "fieldFilter": {
                          "field": { "fieldPath": "verified" },
                          "op": "EQUAL",
                          "value": { "booleanValue": true }
                        }
                      }
                    ]
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

    private suspend fun runWorkingCastScheduleQuery(
        cafeId: String,
        date: String,
        idToken: String?
    ): List<JsonObject> {
        val path = "${config.documentBasePath()}:runQuery"
        val body = """
            {
              "structuredQuery": {
                "from": [
                  { "collectionId": "${FirestorePaths.CAST_SCHEDULES}" }
                ],
                "where": {
                  "compositeFilter": {
                    "op": "AND",
                    "filters": [
                      {
                        "fieldFilter": {
                          "field": { "fieldPath": "cafeId" },
                          "op": "EQUAL",
                          "value": { "stringValue": "${escapeFirestoreQueryString(cafeId)}" }
                        }
                      },
                      {
                        "fieldFilter": {
                          "field": { "fieldPath": "date" },
                          "op": "EQUAL",
                          "value": { "stringValue": "${escapeFirestoreQueryString(date)}" }
                        }
                      },
                      {
                        "fieldFilter": {
                          "field": { "fieldPath": "status" },
                          "op": "EQUAL",
                          "value": { "stringValue": "${CastScheduleStatus.WORK.name}" }
                        }
                      }
                    ]
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

    private suspend fun runCastScheduleByCastIdQuery(
        castId: String,
        idToken: String?
    ): List<JsonObject> {
        val path = "${config.documentBasePath()}:runQuery"
        val body = """
            {
              "structuredQuery": {
                "from": [
                  { "collectionId": "${FirestorePaths.CAST_SCHEDULES}" }
                ],
                "where": {
                  "fieldFilter": {
                    "field": { "fieldPath": "castId" },
                    "op": "EQUAL",
                    "value": { "stringValue": "${escapeFirestoreQueryString(castId)}" }
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

    private suspend fun runVerifiedVisitByUserCafeQuery(
        userId: String,
        cafeId: String,
        idToken: String?
    ): List<JsonObject> {
        val path = "${config.documentBasePath()}:runQuery"
        val body = """
            {
              "structuredQuery": {
                "from": [
                  { "collectionId": "${FirestorePaths.VISITS}" }
                ],
                "where": {
                  "compositeFilter": {
                    "op": "AND",
                    "filters": [
                      {
                        "fieldFilter": {
                          "field": { "fieldPath": "userId" },
                          "op": "EQUAL",
                          "value": { "stringValue": "${escapeFirestoreQueryString(userId)}" }
                        }
                      },
                      {
                        "fieldFilter": {
                          "field": { "fieldPath": "cafeId" },
                          "op": "EQUAL",
                          "value": { "stringValue": "${escapeFirestoreQueryString(cafeId)}" }
                        }
                      },
                      {
                        "fieldFilter": {
                          "field": { "fieldPath": "verified" },
                          "op": "EQUAL",
                          "value": { "booleanValue": true }
                        }
                      }
                    ]
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

    private suspend fun resolveReviewById(reviewId: String, idToken: String?): Review? {
        val cached = reviews.firstOrNull { review -> review.id == reviewId }

        if (cached != null) {
            return cached
        }
        val path = "${config.documentBasePath()}/${FirestorePaths.REVIEWS}/$reviewId"
        val document = runCatching {
            Json.parseToJsonElement(restApi.get(path, idToken)).jsonObject
        }.recoverCatching {
            Json.parseToJsonElement(restApi.get(path, null)).jsonObject
        }.getOrNull()
        val parsed = document?.let { parseReviewDocument(it) }

        if (parsed != null) {
            reviews.removeAll { review -> review.id == parsed.id }
            reviews.add(parsed)
        }
        return parsed
    }

    private suspend fun resolveVisitById(visitId: String, idToken: String?): Visit? {
        val cached = visits.firstOrNull { visit -> visit.id == visitId }

        if (cached != null) {
            return cached
        }
        val path = "${config.documentBasePath()}/${FirestorePaths.VISITS}/$visitId"
        val document = runCatching {
            Json.parseToJsonElement(restApi.get(path, idToken)).jsonObject
        }.recoverCatching {
            Json.parseToJsonElement(restApi.get(path, null)).jsonObject
        }.getOrNull()
        val parsed = document?.let { value -> parseVisitDocument(value) }

        if (parsed != null) {
            visits.removeAll { visit -> visit.id == parsed.id }
            visits.add(parsed)
        }
        return parsed
    }

    private fun syncCastFollowerCountInCache(castId: String, followerCount: Int) {
        val index = delegate.casts.indexOfFirst { cast -> cast.id == castId }

        if (index >= 0) {
            val cast = delegate.casts[index]
            val updatedCast = cast.copy(followerCount = followerCount)

            delegate.casts[index] = updatedCast
            delegate.cafeDetailsById[cast.cafeId]?.let { detail ->
                delegate.cafeDetailsById[cast.cafeId] = detail.copy(
                    casts = detail.casts.map { item ->
                        if (item.id == castId) {
                            updatedCast
                        } else {
                            item
                        }
                    }
                )
            }
        }
    }

    private fun buildCastFollowDocumentId(userId: String, castId: String): String {
        val normalizedUserId = userId.replace("/", "_")
        val normalizedCastId = castId.replace("/", "_")
        return "${normalizedUserId}_$normalizedCastId"
    }

    private fun buildCafeFavoriteDocumentId(userId: String, cafeId: String): String {
        val normalizedUserId = userId.replace("/", "_")
        val normalizedCafeId = cafeId.replace("/", "_")
        return "${normalizedUserId}_$normalizedCafeId"
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

    private suspend fun loadCollectionDocumentCount(
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

    private suspend fun resolveCafeIdByCastId(
        castId: String,
        idToken: String?
    ): String? {
        cafes.forEach { cafe ->
            val path = "${config.documentBasePath()}/${FirestorePaths.CAFES}/${cafe.id}/${FirestorePaths.CAFE_CASTS}/$castId"
            val response = runCatching { restApi.get(path, idToken) }
                .recoverCatching { restApi.get(path, null) }
                .getOrNull()
                ?: return@forEach
            val parsed = runCatching {
                parseCastDocument(cafe.id, Json.parseToJsonElement(response).jsonObject)
            }.getOrNull()
            if (parsed != null) {
                delegate.casts.removeAll { cast -> cast.id == castId }
                delegate.casts.add(parsed)
            }
            return cafe.id
        }
        return null
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

    private suspend fun loadCafeCastDocument(
        cafeId: String,
        castId: String,
        idToken: String?
    ): JsonObject {
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFES}/$cafeId/${FirestorePaths.CAFE_CASTS}/$castId"
        val response = restApi.get(path, idToken)
        return Json.parseToJsonElement(response).jsonObject
    }

    private suspend fun refreshCastSummaryRemote(castId: String, idToken: String?) {
        val cachedCast = casts.firstOrNull { cast ->
            cast.id == castId
        }
        val cachedCafeId = cachedCast?.cafeId
        val resolvedCafeId = if (cachedCafeId != null) {
            cachedCafeId
        } else {
            resolveCafeIdByCastId(castId = castId, idToken = idToken)
                ?: throw NoSuchElementException("cast detail not found")
        }
        val castDocument = runCatching {
            loadCafeCastDocument(cafeId = resolvedCafeId, castId = castId, idToken = idToken)
        }.recoverCatching {
            loadCafeCastDocument(cafeId = resolvedCafeId, castId = castId, idToken = null)
        }.getOrElse { throwable ->
            throw IllegalStateException("Failed to load cast document for $castId", throwable)
        }
        val parsedCast = parseCastDocument(cafeId = resolvedCafeId, document = castDocument)
            ?: throw NoSuchElementException("cast detail not found")
        val cachedCafe = cafes.firstOrNull { cafe ->
            cafe.id == resolvedCafeId
        }
        val parsedCafe = if (cachedCafe != null) {
            cachedCafe
        } else {
            val cafeDocument = runCatching {
                loadCafeDocument(cafeId = resolvedCafeId, idToken = idToken)
            }.recoverCatching {
                loadCafeDocument(cafeId = resolvedCafeId, idToken = null)
            }.getOrElse { throwable ->
                throw IllegalStateException("Failed to load cafe document for $resolvedCafeId", throwable)
            }
            parseCafeDocument(cafeDocument) ?: throw NoSuchElementException("cafe not found")
        }
        val cafeIndex = cafes.indexOfFirst { cafe ->
            cafe.id == parsedCafe.id
        }
        val castIndex = delegate.casts.indexOfFirst { cast ->
            cast.id == parsedCast.id
        }
        val nextCafeCasts = delegate.casts
            .filter { cast ->
                cast.cafeId == parsedCafe.id && cast.id != parsedCast.id
            }.plus(parsedCast)
            .sortedBy { cast ->
                cast.name
            }
        val existingDetail = delegate.cafeDetailsById[parsedCafe.id]
        val nextDetail = if (existingDetail != null) {
            existingDetail.copy(
                cafe = parsedCafe,
                casts = nextCafeCasts
            )
        } else {
            CafeDetail(
                cafe = parsedCafe,
                images = listOfNotNull(parsedCafe.thumbnailImage),
                casts = nextCafeCasts,
                menus = emptyList(),
                goods = emptyList(),
                notices = notices
                    .filter { notice -> notice.cafeId == parsedCafe.id }
                    .sortedByDescending { notice -> notice.createdAt },
                businessHours = "운영시간 정보 준비중",
                phoneNumber = "연락처 정보 준비중"
            )
        }

        if (cafeIndex >= 0) {
            cafes[cafeIndex] = parsedCafe
        } else {
            cafes.add(parsedCafe)
        }
        if (castIndex >= 0) {
            delegate.casts[castIndex] = parsedCast
        } else {
            delegate.casts.add(parsedCast)
        }
        delegate.cafeDetailsById[parsedCafe.id] = nextDetail
    }

    private suspend fun loadCafeSubCollectionDocuments(
        cafeId: String,
        collectionId: String,
        idToken: String?
    ): List<JsonObject> {
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFES}/$cafeId/$collectionId"
        val response = restApi.get(path, idToken)
        val parsed = Json.parseToJsonElement(response).jsonObject
        return parsed["documents"]?.jsonArray.orEmpty().map { element -> element.jsonObject }
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
        hydratedCafeDetailIds.clear()
    }

    private fun upsertCafeAndDetail(
        cafe: Cafe,
        casts: List<Cast>,
        notices: List<Notice>,
        menus: List<CafeMenu>? = null,
        goods: List<Goods>? = null,
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
                menus = menus ?: cachedDetail.menus,
                goods = goods ?: cachedDetail.goods,
                notices = notices,
                businessHours = businessHours ?: cachedDetail.businessHours,
                phoneNumber = phoneNumber ?: cachedDetail.phoneNumber
            )
        } else {
            CafeDetail(
                cafe = cafe,
                images = images ?: listOfNotNull(cafe.thumbnailImage),
                casts = casts,
                menus = menus ?: emptyList(),
                goods = goods ?: emptyList(),
                notices = notices,
                businessHours = businessHours ?: "운영시간 정보 준비중",
                phoneNumber = phoneNumber ?: "연락처 정보 준비중"
            )
        }
        delegate.cafeDetailsById[cafe.id] = detail
    }

    private fun parseMenuDocument(document: JsonObject): CafeMenu? {
        val fields = document["fields"]?.jsonObject ?: return null
        val name = document["name"]?.jsonPrimitive?.contentOrNull ?: return null
        val menuId = name.substringAfterLast("/")
        val category = fields.getFirestoreString("category")
            ?.trim()
            ?.takeIf { value -> value.isNotEmpty() }
            ?: "drink"
        val description = fields.getFirestoreString("desc")
            ?: fields.getFirestoreString("description")
            ?: ""
        return CafeMenu(
            id = menuId,
            name = fields.getFirestoreString("name").orEmpty(),
            price = (fields.getFirestoreLong("price") ?: 0L).toInt(),
            desc = description,
            image = fields.getFirestoreString("image")
                ?: fields.getFirestoreString("imageUrl")
                ?: fields.getFirestoreString("thumbnailImage"),
            category = category,
            isAvailable = fields.getFirestoreBoolean("isAvailable") ?: true
        )
    }

    private fun parseGoodsDocument(document: JsonObject): Goods? {
        val fields = document["fields"]?.jsonObject ?: return null
        val name = document["name"]?.jsonPrimitive?.contentOrNull ?: return null
        val goodsId = name.substringAfterLast("/")
        return Goods(
            id = goodsId,
            name = fields.getFirestoreString("name").orEmpty(),
            price = (fields.getFirestoreLong("price") ?: 0L).toInt(),
            image = fields.getFirestoreString("image")
                ?: fields.getFirestoreString("imageUrl")
                ?: fields.getFirestoreString("thumbnailImage"),
            stock = (fields.getFirestoreLong("stock") ?: 0L).toInt()
        )
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
            ?: fields?.getFirestoreString("openingHours")
            ?: fields?.getFirestoreString("operatingHours")
        val normalizedBusinessHours = businessHours
            ?.trim()
            ?.takeIf { value -> value.isNotEmpty() }
        val phoneNumber = fields
            ?.getFirestoreString("phoneNumber")
            ?: fields?.getFirestoreString("contactNumber")
            ?: fields?.getFirestoreString("phone")
        val normalizedPhoneNumber = phoneNumber
            ?.trim()
            ?.takeIf { value -> value.isNotEmpty() }
        return CafeDetailMetadata(
            images = if (images.isEmpty()) null else images,
            businessHours = normalizedBusinessHours,
            phoneNumber = normalizedPhoneNumber
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

    private fun resolvePeriodLabel(displayDays: Int): String {
        val startDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val endDate = startDate.plus(DatePeriod(days = displayDays - 1))
        return "${startDate.toPeriodText()} - ${endDate.toPeriodText()}"
    }

    private fun LocalDate.toPeriodText(): String {
        val monthText = monthNumber.toString().padStart(2, '0')
        val dayText = dayOfMonth.toString().padStart(2, '0')
        return "$year.$monthText.$dayText"
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

    private fun parseUserAffiliatedCafeId(document: JsonObject): String? {
        val fields = document["fields"]?.jsonObject ?: return null
        return fields.getFirestoreString("affiliatedCafeId")
            ?.trim()
            ?.takeIf { value -> value.isNotEmpty() }
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
                ?: 0.0,
            visitCertificationCount = fields.getFirestoreLong("visitCertificationCount")?.toInt()
                ?: fields.getFirestoreMap("stats")?.getFirestoreInt("visitCertificationCount")
                ?: 0
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

    private fun parseNoticeManagementDocument(
        cafeId: String,
        document: JsonObject
    ): CafeNoticeManagementItem? {
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
            statusLabel = fields.getFirestoreString("statusLabel")
                ?: if (statusAccent == NoticeStatusAccent.DRAFT) "임시 저장" else "게시 중",
            statusAccent = statusAccent
        )
    }

    private fun parseEventManagementDocument(
        cafeId: String,
        document: JsonObject
    ): CafeEventManagementItem? {
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
            isDimmed = fields.getFirestoreBoolean("isDimmed") ?: false
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

    private fun parseReviewDocument(document: JsonObject): Review? {
        val fields = document["fields"]?.jsonObject ?: return null
        val name = document["name"]?.jsonPrimitive?.contentOrNull ?: return null
        val reviewId = name.substringAfterLast("/")
        val userId = fields.getFirestoreString("userId") ?: return null
        val cafeId = fields.getFirestoreString("cafeId") ?: return null
        val ratingValue = fields.getFirestoreDouble("rating")
            ?: fields.getFirestoreLong("rating")?.toDouble()
            ?: 0.0

        return Review(
            id = reviewId,
            userId = userId,
            userNickname = fields.getFirestoreString("userNickname")
                ?: findUserById(userId)?.nickname
                ?: "",
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

    private fun parseCastScheduleDocument(document: JsonObject): CastScheduleDocumentEntry? {
        val fields = document["fields"]?.jsonObject ?: return null
        val name = document["name"]?.jsonPrimitive?.contentOrNull ?: return null
        val scheduleId = name.substringAfterLast("/")
        val castId = fields.getFirestoreString("castId") ?: return null
        val cafeId = fields.getFirestoreString("cafeId") ?: return null
        val date = fields.getFirestoreString("date") ?: return null
        val startTime = fields.getFirestoreString("startTime")
        val endTime = fields.getFirestoreString("endTime")
        val rawStatus = fields.getFirestoreString("status")
        val status = parseCastScheduleStatus(rawStatus)
            ?: if (!startTime.isNullOrBlank() && !endTime.isNullOrBlank()) {
                CastScheduleStatus.WORK
            } else {
                CastScheduleStatus.OFF
            }
        return CastScheduleDocumentEntry(
            id = scheduleId,
            castId = castId,
            cafeId = cafeId,
            date = date,
            status = status,
            startTime = startTime,
            endTime = endTime
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

private data class CastScheduleDocumentEntry(
    val id: String,
    val castId: String,
    val cafeId: String,
    val date: String,
    val status: CastScheduleStatus,
    val startTime: String?,
    val endTime: String?
)

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
                "aggregations":[
                    {
                        "alias":"count",
                        "count":{}
                    }
                ],
                "structuredQuery":{
                    "from":[
                        {
                            "collectionId":"$escapedCollectionId"
                        }
                    ]$whereClause
                }
            }
        }
    """.trimIndent()
}

private fun parseCountAggregationResponse(response: String): Int {
    val trimmedResponse = response.trim()

    if (trimmedResponse.isEmpty()) {
        return 0
    }
    val parsedElement = runCatching {
        Json.parseToJsonElement(trimmedResponse)
    }.getOrNull()

    if (parsedElement != null) {
        val parsedCount = when (parsedElement) {
            is JsonObject -> extractCountFromAggregationItem(parsedElement)
            is kotlinx.serialization.json.JsonArray -> parsedElement
                .mapNotNull { item -> extractCountFromAggregationItem(item.jsonObject) }
                .firstOrNull()
            else -> null
        }

        if (parsedCount != null) {
            return parsedCount
        }
    }

    val fallbackCount = trimmedResponse
        .lineSequence()
        .map { line -> line.trim() }
        .filter { line -> line.startsWith("{") && line.endsWith("}") }
        .mapNotNull { line ->
            runCatching { Json.parseToJsonElement(line).jsonObject }.getOrNull()
        }
        .mapNotNull { item -> extractCountFromAggregationItem(item) }
        .firstOrNull()

    return fallbackCount ?: 0
}

private fun extractCountFromAggregationItem(item: JsonObject): Int? {
    val result = item["result"]?.jsonObject ?: return null
    val aggregateFields = result["aggregateFields"]?.jsonObject ?: return null
    val aliasField = aggregateFields["count"]?.jsonObject
        ?: aggregateFields.values.firstOrNull()?.jsonObject
        ?: return null
    val integerValue = aliasField["integerValue"]?.jsonPrimitive?.longOrNull
    val doubleValue = aliasField["doubleValue"]?.jsonPrimitive?.doubleOrNull

    if (integerValue != null) {
        return integerValue.toInt()
    } else {
        if (doubleValue != null) {
            return doubleValue.toInt()
        }
    }

    return null
}

private fun parseCastScheduleStatus(raw: String?): CastScheduleStatus? {
    return when (raw?.trim()?.uppercase()) {
        "WORK", "근무" -> CastScheduleStatus.WORK
        "OFF", "휴무" -> CastScheduleStatus.OFF
        "VACATION", "휴가" -> CastScheduleStatus.VACATION
        else -> null
    }
}

private fun enumerateDates(fromDate: String, toDate: String): List<String> {
    val from = runCatching { LocalDate.parse(fromDate) }.getOrNull() ?: return emptyList()
    val to = runCatching { LocalDate.parse(toDate) }.getOrNull() ?: return emptyList()
    if (from > to) return emptyList()
    val dates = mutableListOf<String>()
    var current = from
    while (current <= to) {
        dates += current.toString()
        current += DatePeriod(days = 1)
    }
    return dates
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

private fun firestoreDouble(value: Double): JsonObject {
    return JsonObject(mapOf("doubleValue" to JsonPrimitive(value)))
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
