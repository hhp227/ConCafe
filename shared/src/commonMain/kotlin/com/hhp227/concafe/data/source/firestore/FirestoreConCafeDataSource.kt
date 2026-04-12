package com.hhp227.concafe.data.source.firestore

import com.hhp227.concafe.data.source.AuthDataSource
import com.hhp227.concafe.data.source.NotificationDataSource
import com.hhp227.concafe.data.source.RankingDataSource
import com.hhp227.concafe.domain.model.BannerLinkTargetType
import com.hhp227.concafe.domain.model.AdminOperationsMetrics
import com.hhp227.concafe.domain.model.AppNotification
import com.hhp227.concafe.domain.model.Cafe
import com.hhp227.concafe.domain.model.CafeSort
import com.hhp227.concafe.domain.model.CafeDashboardData
import com.hhp227.concafe.domain.model.CafeDetail
import com.hhp227.concafe.domain.model.CafeEventCreate
import com.hhp227.concafe.domain.model.CafeEventManagementItem
import com.hhp227.concafe.domain.model.CafeEventUpdate
import com.hhp227.concafe.domain.model.CafeInfoUpdate
import com.hhp227.concafe.domain.model.CafeMenu
import com.hhp227.concafe.domain.model.CafeMenuGoodsSection
import com.hhp227.concafe.domain.model.CafeMenuGoodsUpsert
import com.hhp227.concafe.domain.model.CafeNoticeCreate
import com.hhp227.concafe.domain.model.CafeNoticeManagementItem
import com.hhp227.concafe.domain.model.CafeNoticeUpdate
import com.hhp227.concafe.domain.model.CafeRegistrationClaim
import com.hhp227.concafe.domain.model.Cast
import com.hhp227.concafe.domain.model.CastSort
import com.hhp227.concafe.domain.model.CastClaim
import com.hhp227.concafe.domain.model.CastClaimStatus
import com.hhp227.concafe.domain.model.CastDetail
import com.hhp227.concafe.domain.model.CastFollowerSnapshot
import com.hhp227.concafe.domain.model.CastSchedule
import com.hhp227.concafe.domain.model.CastScheduleStatus
import com.hhp227.concafe.domain.model.CastScheduleUpdate
import com.hhp227.concafe.domain.model.CastUpsert
import com.hhp227.concafe.domain.model.GeoPoint
import com.hhp227.concafe.domain.model.Goods
import com.hhp227.concafe.domain.model.HomeBanner
import com.hhp227.concafe.domain.model.Inquiry
import com.hhp227.concafe.domain.model.InquiryCreate
import com.hhp227.concafe.domain.model.InquiryStatus
import com.hhp227.concafe.domain.model.MyPageSummary
import com.hhp227.concafe.domain.model.Notice
import com.hhp227.concafe.domain.model.NoticeStatusAccent
import com.hhp227.concafe.domain.model.NotificationQuietHoursMode
import com.hhp227.concafe.domain.model.CafeManagementData
import com.hhp227.concafe.domain.model.PendingCafeOwnerClaimPreview
import com.hhp227.concafe.domain.model.PendingCafeRegistrationClaimPreview
import com.hhp227.concafe.domain.model.RankingItem
import com.hhp227.concafe.domain.model.RankingPeriod
import com.hhp227.concafe.domain.model.Region
import com.hhp227.concafe.domain.model.Review
import com.hhp227.concafe.domain.model.Stamp
import com.hhp227.concafe.domain.model.AuthProvider
import com.hhp227.concafe.domain.model.User
import com.hhp227.concafe.domain.model.UserNotificationSettings
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.model.Visit
import com.hhp227.concafe.domain.model.VisitVerificationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
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
import com.hhp227.concafe.domain.common.PagedResult

class FirestoreConCafeDataSource(
    private val config: FirestoreConfig,
    private val restApi: FirestoreRestApi,
    private val tokenProvider: FirestoreAuthTokenProvider
) :
    AuthDataSource,
    RankingDataSource,
    NotificationDataSource,
    FirestoreSyncDataSource {
    private val _currentUserId = MutableStateFlow<String?>(null)

    override var currentUserId: String?
        get() = _currentUserId.value
        set(value) {
            _currentUserId.value = value
        }

    override val currentUserIdFlow: StateFlow<String?> = _currentUserId.asStateFlow()

    override suspend fun rankingItemsFromCasts(
        period: RankingPeriod,
        country: String?,
        city: String?
    ): List<RankingItem> {
        return loadRankingItemsRemote(
            kind = RANKING_KIND_CAST,
            period = period,
            country = country,
            city = city
        )
    }

    override suspend fun rankingItemsFromCafes(
        period: RankingPeriod,
        country: String?,
        city: String?
    ): List<RankingItem> {
        return loadRankingItemsRemote(
            kind = RANKING_KIND_CAFE,
            period = period,
            country = country,
            city = city
        )
    }

    override fun homePopularCastPage(cursor: String?, pageSize: Int): PagedResult<Cast> {
        return runBlocking {
            getHomePopularCastPageRemote(cursor = cursor, pageSize = pageSize)
        }
    }

    override suspend fun getNotifications(
        userId: String,
        cursor: String?,
        pageSize: Int
    ): PagedResult<AppNotification> {
        val safePageSize = if (pageSize > 0) pageSize else 20
        val idToken = tokenProvider.getIdToken()
        val documents = runCatching {
            runUserNotificationPageQuery(
                userId = userId,
                cursor = cursor,
                limit = safePageSize,
                idToken = idToken
            )
        }.recoverCatching {
            runUserNotificationPageQuery(
                userId = userId,
                cursor = cursor,
                limit = safePageSize,
                idToken = null
            )
        }.getOrElse { throwable ->
            throw IllegalStateException("Failed to load notification page", throwable)
        }
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
        val idToken = tokenProvider.getIdToken()
        val document = runCatching {
            restApi.get(path = path, idToken = idToken)
        }.recoverCatching {
            restApi.get(path = path, idToken = null)
        }.getOrNull()

        return if (document.isNullOrBlank()) {
            UserNotificationSettings.default()
        } else {
            val parsed = runCatching {
                Json.parseToJsonElement(document).jsonObject
            }.getOrNull()
            if (parsed == null || parsed["fields"] == null) {
                UserNotificationSettings.default()
            } else {
                parseNotificationSettingsDocument(parsed)
            }
        }
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
                "quietHoursMode" to firestoreString(settings.quietHoursMode.name),
                "updatedAt" to firestoreString(Clock.System.now().toString())
            )
        )

        runCatching {
            restApi.patch(path = path, body = body, idToken = idToken)
        }.recoverCatching {
            restApi.patch(path = path, body = body, idToken = null)
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
        val locale = "ko-KR"
        val now = Clock.System.now().toString()
        val path = "${config.documentBasePath()}/${FirestorePaths.USERS}/$normalizedUserId/${FirestorePaths.USER_DEVICE_TOKENS}/$tokenDocumentId"
        val body = firestoreDocumentBody(
            fields = mapOf(
                "userId" to firestoreString(normalizedUserId),
                "platform" to firestoreString(normalizedPlatform),
                "token" to firestoreString(normalizedToken),
                "locale" to firestoreString(locale),
                "isEnabled" to firestoreBoolean(true),
                "updatedAt" to firestoreString(now),
                "createdAt" to firestoreString(now)
            )
        )

        runCatching {
            restApi.patch(path = path, body = body, idToken = idToken)
        }.recoverCatching {
            restApi.patch(path = path, body = body, idToken = null)
        }.getOrElse { throwable ->
            throw IllegalStateException("Failed to register push token", throwable)
        }
    }

    override suspend fun disableAllPushTokens(userId: String) {
        val normalizedUserId = userId.trim()

        if (normalizedUserId.isEmpty()) {
            throw IllegalArgumentException("invalid push token payload")
        }
        val idToken = tokenProvider.getIdToken()
        val collectionPath = "${config.documentBasePath()}/${FirestorePaths.USERS}/$normalizedUserId/${FirestorePaths.USER_DEVICE_TOKENS}"
        val documents = runCatching {
            val response = restApi.get(path = collectionPath, idToken = idToken)
            val parsed = Json.parseToJsonElement(response).jsonObject
            parsed["documents"]?.jsonArray.orEmpty().map { element -> element.jsonObject }
        }.recoverCatching {
            val response = restApi.get(path = collectionPath, idToken = null)
            val parsed = Json.parseToJsonElement(response).jsonObject
            parsed["documents"]?.jsonArray.orEmpty().map { element -> element.jsonObject }
        }.getOrElse { throwable ->
            throw IllegalStateException("Failed to load push token documents", throwable)
        }

        if (documents.isEmpty()) {
            Unit
        } else {
            val now = Clock.System.now().toString()

            for (document in documents) {
                val name = document["name"]?.jsonPrimitive?.contentOrNull

                if (name.isNullOrBlank()) {
                    continue
                } else {
                    val tokenDocumentId = name.substringAfterLast("/")
                    val path = "${config.documentBasePath()}/${FirestorePaths.USERS}/$normalizedUserId/${FirestorePaths.USER_DEVICE_TOKENS}/$tokenDocumentId"
                    val body = firestoreDocumentBody(
                        fields = mapOf(
                            "isEnabled" to firestoreBoolean(false),
                            "updatedAt" to firestoreString(now)
                        )
                    )

                    runCatching {
                        restApi.patch(
                            path = path,
                            body = body,
                            idToken = idToken,
                            updateMask = listOf("isEnabled", "updatedAt")
                        )
                    }.recoverCatching {
                        restApi.patch(
                            path = path,
                            body = body,
                            idToken = null,
                            updateMask = listOf("isEnabled", "updatedAt")
                        )
                    }.getOrElse { throwable ->
                        throw IllegalStateException("Failed to disable push token", throwable)
                    }
                }
            }
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
        if (normalizedTitle.isEmpty()) {
            throw IllegalArgumentException("fan announcement title is required")
        }
        if (normalizedBody.isEmpty()) {
            throw IllegalArgumentException("fan announcement body is required")
        }
        if (normalizedTitle.length > 50) {
            throw IllegalArgumentException("fan announcement title is too long")
        }
        if (normalizedBody.length > 300) {
            throw IllegalArgumentException("fan announcement body is too long")
        }
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

    suspend fun refreshCafeDetail(cafeId: String) {
        fetchCafeDetailRemote(cafeId)
    }

    suspend fun fetchCafeDetailRemote(cafeId: String): CafeDetail? {
        return fetchCafeDetailSummaryRemoteInternal(cafeId)
    }

    suspend fun fetchCafeMenuGoodsRemote(cafeId: String): CafeMenuGoodsSection? {
        return fetchCafeMenuGoodsRemoteInternal(cafeId)
    }

    suspend fun fetchCafeByIdRemote(cafeId: String): Cafe? {
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        val cafeDocument = runCatching {
            loadCafeDocument(cafeId = cafeId, idToken = idToken)
        }.recoverCatching {
            loadCafeDocument(cafeId = cafeId, idToken = null)
        }.getOrNull() ?: return null
        return parseCafeDocument(cafeDocument)
    }

    suspend fun refreshCafeNoticeEventManagement(cafeId: String) {
        val idToken = tokenProvider.getIdToken()
        val cafeName = runCatching {
            parseCafeDocument(loadCafeDocument(cafeId = cafeId, idToken = idToken))?.name.orEmpty()
        }.recoverCatching {
            parseCafeDocument(loadCafeDocument(cafeId = cafeId, idToken = null))?.name.orEmpty()
        }.getOrDefault("")
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

        noticesDocuments.mapNotNull { document ->
            parseNoticeManagementDocument(cafeId = cafeId, document = document)
        }.sortedByDescending { item -> item.createdAt }
        noticesDocuments.mapNotNull { document ->
            parseNoticeDocument(cafeId = cafeId, cafeName = cafeName, document = document)
        }.sortedByDescending { item -> item.createdAt }
        eventsDocuments.mapNotNull { document ->
            parseEventManagementDocument(cafeId = cafeId, document = document)
        }.sortedByDescending { item -> item.startDate }
    }

    suspend fun createInquiryRemote(
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
        val inquiry = Inquiry(
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
        return inquiry
    }

    suspend fun getInquiryPageRemote(
        cursor: String?,
        pageSize: Int
    ): PagedResult<Inquiry> {
        val safePageSize = pageSize.coerceAtLeast(1)
        val idToken = runCatching {
            tokenProvider.getIdToken()
        }.getOrNull()
        val documents = runCatching {
            runInquiryPageQuery(
                cursor = cursor,
                limit = safePageSize + 1,
                idToken = idToken
            )
        }.recoverCatching {
            runInquiryPageQuery(
                cursor = cursor,
                limit = safePageSize + 1,
                idToken = null
            )
        }.getOrElse { throwable ->
            throw IllegalStateException("Failed to load inquiry page", throwable)
        }
        val pageDocuments = documents.take(safePageSize)
        val hasNext = documents.size > safePageSize
        val nextCursorToken = if (hasNext) {
            pageDocuments.lastOrNull()
                ?.get("fields")
                ?.jsonObject
                ?.getFirestoreString("createdAt")
        } else {
            null
        }
        val pageItems = pageDocuments.mapNotNull { document ->
            parseInquiryDocument(document)
        }
        return PagedResult(
            items = pageItems,
            nextCursor = nextCursorToken,
            hasNext = hasNext
        )
    }

    suspend fun createCafeNoticeRemote(input: CafeNoticeCreate): CafeNoticeManagementItem {
        val idToken = tokenProvider.getIdToken()
        val noticeId = nextFirestoreEntityId("notice-management")
        val createdAt = Clock.System.now().toString()
        val cafeName = resolveCafeNameForNoticeWrite(cafeId = input.cafeId, idToken = idToken)
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
                "reservedAt" to firestoreNullableString(input.reservedAt?.trim()?.takeIf { value -> value.isNotEmpty() })
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

    suspend fun updateCafeNoticeRemote(input: CafeNoticeUpdate): CafeNoticeManagementItem {
        val idToken = tokenProvider.getIdToken()
        val cafeName = resolveCafeNameForNoticeWrite(cafeId = input.cafeId, idToken = idToken)
        val statusLabel = if (input.reservedAt.isNullOrBlank()) "게시 중" else "임시 저장"
        val statusAccent = if (input.reservedAt.isNullOrBlank()) NoticeStatusAccent.PUBLISHED else NoticeStatusAccent.DRAFT
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFES}/${input.cafeId}/${FirestorePaths.CAFE_NOTICES}/${input.noticeId}" +
            "?updateMask.fieldPaths=title" +
            "&updateMask.fieldPaths=content" +
            "&updateMask.fieldPaths=cafeName" +
            "&updateMask.fieldPaths=isPinned" +
            "&updateMask.fieldPaths=statusLabel" +
            "&updateMask.fieldPaths=statusAccent" +
            "&updateMask.fieldPaths=reservedAt"
        val body = firestoreDocumentBody(
            mapOf(
                "title" to firestoreString(input.title.trim()),
                "content" to firestoreString(input.content.trim()),
                "cafeName" to firestoreString(cafeName),
                "isPinned" to firestoreBoolean(input.isPinned),
                "statusLabel" to firestoreString(statusLabel),
                "statusAccent" to firestoreString(statusAccent.name),
                "reservedAt" to firestoreNullableString(input.reservedAt?.trim()?.takeIf { value -> value.isNotEmpty() })
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
        return CafeEventManagementItem(
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
        val periodText = input.periodText?.trim()?.takeIf { value -> value.isNotEmpty() } ?: "게시 일정 선택 필요"
        val startDate = periodText.substringBefore(" - ", missingDelimiterValue = periodText)
        val endDate = periodText.substringAfter(" - ", missingDelimiterValue = startDate)
        val statusLabel = if (input.periodText.isNullOrBlank()) "진행 예정" else "진행 중"
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
        return CafeEventManagementItem(
            id = input.eventId,
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

    suspend fun deleteCafeEventRemote(cafeId: String, eventId: String) {
        val idToken = tokenProvider.getIdToken()
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFES}/$cafeId/${FirestorePaths.CAFE_EVENTS}/$eventId"

        restApi.delete(path, idToken)
        runCatching {
            refreshCafeNoticeEventManagement(cafeId)
        }
    }

    suspend fun getCafeNoticePageRemote(
        cafeId: String,
        query: String,
        cursor: String?,
        pageSize: Int
    ): PagedResult<CafeNoticeManagementItem> {
        val safePageSize = if (pageSize > 0) pageSize else 1
        val normalizedQuery = query.trim()
        val idToken = runCatching {
            tokenProvider.getIdToken()
        }.getOrNull()
        val documents = runCatching {
            runCafeNoticePageQuery(
                cafeId = cafeId,
                cursor = cursor,
                limit = safePageSize + 1,
                idToken = idToken
            )
        }.recoverCatching {
            runCafeNoticePageQuery(
                cafeId = cafeId,
                cursor = cursor,
                limit = safePageSize + 1,
                idToken = null
            )
        }.getOrElse { throwable ->
            throw IllegalStateException("Failed to load cafe notice page($cafeId): ${throwable.message}", throwable)
        }
        val pageDocuments = documents.take(safePageSize)
        val hasNext = documents.size > safePageSize
        val nextCursorToken = if (hasNext) {
            pageDocuments.lastOrNull()?.toNoticeQueryCursor()
        } else {
            null
        }
        val pageItems = pageDocuments.mapNotNull { document ->
            parseNoticeManagementDocument(
                cafeId = cafeId,
                document = document
            )
        }.filter { item ->
            normalizedQuery.isBlank() ||
                item.title.contains(normalizedQuery, ignoreCase = true) ||
                item.content.contains(normalizedQuery, ignoreCase = true)
        }
        return PagedResult(
            items = pageItems,
            nextCursor = nextCursorToken,
            hasNext = hasNext
        )
    }

    suspend fun getCafeEventPageRemote(
        cafeId: String,
        query: String,
        cursor: String?,
        pageSize: Int
    ): PagedResult<CafeEventManagementItem> {
        val safePageSize = if (pageSize > 0) pageSize else 1
        val normalizedQuery = query.trim()
        val idToken = runCatching {
            tokenProvider.getIdToken()
        }.getOrNull()
        val documents = runCatching {
            runCafeEventPageQuery(
                cafeId = cafeId,
                cursor = cursor,
                limit = safePageSize + 1,
                idToken = idToken
            )
        }.recoverCatching {
            runCafeEventPageQuery(
                cafeId = cafeId,
                cursor = cursor,
                limit = safePageSize + 1,
                idToken = null
            )
        }.getOrElse { throwable ->
            throw IllegalStateException("Failed to load cafe event page: $cafeId", throwable)
        }
        val pageDocuments = documents.take(safePageSize)
        val hasNext = documents.size > safePageSize
        val nextCursorToken = if (hasNext) {
            pageDocuments.lastOrNull()?.toEventQueryCursor()
        } else {
            null
        }
        val pageItems = pageDocuments.mapNotNull { document ->
            parseEventManagementDocument(
                cafeId = cafeId,
                document = document
            )
        }.filter { item ->
            normalizedQuery.isBlank() ||
                item.title.contains(normalizedQuery, ignoreCase = true) ||
                item.content.contains(normalizedQuery, ignoreCase = true)
        }
        return PagedResult(
            items = pageItems,
            nextCursor = nextCursorToken,
            hasNext = hasNext
        )
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
        reviewDocuments
            .mapNotNull { document -> parseReviewDocument(document) }
            .sortedByDescending { review -> review.createdAt }
    }

    suspend fun getRecentNoticesRemote(limit: Int): List<Notice> {
        val safeLimit = if (limit > 0) limit else 1
        val idToken = runCatching {
            tokenProvider.getIdToken()
        }.getOrNull()
        val noticeDocuments = runCatching {
            runRecentNoticeFeedQuery(
                limit = safeLimit,
                idToken = idToken
            )
        }.recoverCatching {
            runRecentNoticeFeedQuery(
                limit = safeLimit,
                idToken = null
            )
        }.getOrElse { throwable ->
            throw IllegalStateException("Failed to load recent notices: ${throwable.message}", throwable)
        }

        if (noticeDocuments.isEmpty()) {
            return emptyList()
        }
        return noticeDocuments.mapNotNull { document ->
            val documentName = document["name"]?.jsonPrimitive?.contentOrNull
            val fields = document["fields"]?.jsonObject
            val cafeId = documentName?.toCafeIdFromNoticeDocumentName()
                ?: fields?.getFirestoreString("cafeId")
                ?: return@mapNotNull null
            val cafeName = fields
                ?.getFirestoreString("cafeName")
                ?.trim()
                ?.takeIf { value -> value.isNotEmpty() }
                ?: cafeId

            parseNoticeDocument(
                cafeId = cafeId,
                cafeName = cafeName,
                document = document
            )
        }.sortedByDescending { notice ->
            notice.createdAt
        }
    }

    suspend fun getCafeReviewsPageRemote(
        cafeId: String,
        cursor: String?,
        pageSize: Int
    ): PagedResult<Review> {
        val safePageSize = if (pageSize > 0) pageSize else 1
        val idToken = runCatching {
            tokenProvider.getIdToken()
        }.getOrNull()
        val reviewDocuments = runCatching {
            runCafeReviewPageQuery(
                cafeId = cafeId,
                cursor = cursor,
                limit = safePageSize + 1,
                idToken = idToken
            )
        }.recoverCatching {
            runCafeReviewPageQuery(
                cafeId = cafeId,
                cursor = cursor,
                limit = safePageSize + 1,
                idToken = null
            )
        }.getOrElse { throwable ->
            throw IllegalStateException("Failed to load cafe review page($cafeId): ${throwable.message}", throwable)
        }
        val pageDocuments = reviewDocuments.take(safePageSize)
        val hasNext = reviewDocuments.size > safePageSize
        val nextCursorToken = if (hasNext) {
            pageDocuments.lastOrNull()?.toReviewQueryCursor()
        } else {
            null
        }
        val pageReviews = pageDocuments.mapNotNull { document ->
            parseReviewDocument(document)
        }
        return PagedResult(
            items = pageReviews,
            nextCursor = nextCursorToken,
            hasNext = hasNext
        )
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
        val parsedReviews = reviewDocuments.mapNotNull { document ->
            parseReviewDocument(document)
        }.sortedByDescending { review ->
            review.createdAt
        }.take(safeLimit)
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

        visitDocuments.mapNotNull { document -> parseVisitDocument(document) }
    }

    suspend fun fetchVisitsByUserPageRemote(
        userId: String,
        cursor: String?,
        pageSize: Int
    ): PagedResult<Visit> {
        if (userId.isBlank()) {
            return PagedResult(
                items = emptyList(),
                nextCursor = null,
                hasNext = false
            )
        }
        val idToken = runCatching {
            tokenProvider.getIdToken()
        }.getOrNull()
        val tokenUserId = tokenProvider.getCurrentUserId()
        val primaryVisitDocuments = runCatching {
            runUserVisitPageQuery(
                userId = userId,
                cursor = cursor,
                limit = pageSize.coerceAtLeast(1) + 1,
                idToken = idToken,
            )
        }.recoverCatching {
            runUserVisitPageQuery(
                userId = userId,
                cursor = cursor,
                limit = pageSize.coerceAtLeast(1) + 1,
                idToken = null,
            )
        }.getOrElse { error ->
            println(
                "TEST, fetchVisitsByUserPageRemote query failed: " +
                    "userId=$userId tokenUserId=$tokenUserId tokenPresent=${!idToken.isNullOrBlank()} " +
                    "sameUser=${tokenUserId == userId} message=${error.message}"
            )
            emptyList()
        }
        val safePageSize = pageSize.coerceAtLeast(1)
        val visitDocuments = primaryVisitDocuments
        val mappedVisits = visitDocuments
            .mapNotNull { document -> parseVisitDocument(document) }
            .sortedByDescending { visit -> visit.visitedAt }
        val pageItems = mappedVisits.take(safePageSize)
        val hasNext = visitDocuments.size > safePageSize
        val nextCursor = if (hasNext) {
            visitDocuments
                .take(safePageSize)
                .lastOrNull()
                ?.toVisitQueryCursor()
        } else {
            null
        }
        println(
            "TEST, fetchVisitsByUserPageRemote result: " +
                "userId=$userId tokenUserId=$tokenUserId tokenPresent=${!idToken.isNullOrBlank()} " +
                "sameUser=${tokenUserId == userId} " +
                "documents=${visitDocuments.size} visits=${mappedVisits.size}"
        )
        return PagedResult(
            items = pageItems,
            nextCursor = nextCursor,
            hasNext = hasNext
        )
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
        return parsedVisits.any { visit ->
            visit.userId == userId && visit.cafeId == cafeId && visit.verified
        }
    }

    suspend fun fetchVisitCountByCafeRemote(cafeId: String): Int {
        val idToken = runCatching {
            tokenProvider.getIdToken()
        }.getOrNull()
        return runCatching {
            loadCollectionDocumentCount(
                collectionId = FirestorePaths.VISITS,
                idToken = idToken,
                equalsFilterFieldPath = "cafeId",
                equalsFilterValue = firestoreString(cafeId)
            )
        }.recoverCatching {
            loadCollectionDocumentCount(
                collectionId = FirestorePaths.VISITS,
                idToken = null,
                equalsFilterFieldPath = "cafeId",
                equalsFilterValue = firestoreString(cafeId)
            )
        }.getOrThrow()
    }

    suspend fun verifyVisitRemote(
        cafeId: String,
        latitude: Double,
        longitude: Double
    ): VisitVerificationResult {
        val cafe = fetchCafeByIdRemote(cafeId)
        val cafeLatitude = cafe?.region?.location?.latitude ?: latitude
        val cafeLongitude = cafe?.region?.location?.longitude ?: longitude
        val distanceMeters = haversineMeters(
            latitude1 = cafeLatitude,
            longitude1 = cafeLongitude,
            latitude2 = latitude,
            longitude2 = longitude
        )
        val allowedDistanceMeters = 100.0
        return if (distanceMeters <= allowedDistanceMeters) {
            VisitVerificationResult(
                verified = true,
                distanceMeters = distanceMeters,
                allowedRadiusMeters = allowedDistanceMeters,
                message = "방문 인증 성공"
            )
        } else {
            VisitVerificationResult(
                verified = false,
                distanceMeters = distanceMeters,
                allowedRadiusMeters = allowedDistanceMeters,
                message = "카페 반경 100m 밖입니다"
            )
        }
    }

    suspend fun fetchStampsByUserRemote(userId: String): List<Stamp> {
        val idToken = runCatching {
            tokenProvider.getIdToken()
        }.getOrNull()
        val stampDocuments = runCatching {
            runUserScopedQuery(
                collectionId = FirestorePaths.STAMPS,
                userId = userId,
                idToken = idToken,
                orderByFieldPath = "earnedAt",
                orderByDescending = true
            )
        }.recoverCatching {
            runUserScopedQuery(
                collectionId = FirestorePaths.STAMPS,
                userId = userId,
                idToken = null,
                orderByFieldPath = "earnedAt",
                orderByDescending = true
            )
        }.getOrElse { emptyList() }
        return stampDocuments.mapNotNull { document ->
            parseStampDocument(document)
        }
    }

    suspend fun getWorkingCastIdsByCafeAndDate(cafeId: String, date: String): Set<String> {
        val idToken = tokenProvider.getIdToken()
        val scheduleDocuments = runWorkingCastScheduleQuery(cafeId = cafeId, date = date, idToken = idToken)
        val parsedSchedules = scheduleDocuments.mapNotNull { document ->
            parseCastScheduleDocument(document)
        }.filter { entry ->
            entry.date == date && entry.status == CastScheduleStatus.WORK
        }
        return parsedSchedules
            .asSequence()
            .map { entry -> entry.castId }
            .toSet()
    }

    suspend fun getWorkingCastSchedulesByCafeAndDateRemote(
        cafeId: String,
        date: String
    ): Map<String, CastSchedule> {
        val idToken = runCatching {
            tokenProvider.getIdToken()
        }.getOrNull()
        val scheduleDocuments = runCatching {
            runWorkingCastScheduleQuery(cafeId = cafeId, date = date, idToken = idToken)
        }.recoverCatching {
            runWorkingCastScheduleQuery(cafeId = cafeId, date = date, idToken = null)
        }.getOrElse { emptyList() }
        return scheduleDocuments.mapNotNull { document ->
            parseWorkingCastScheduleDocument(document)
        }.associateBy { schedule -> schedule.castId }
    }

    suspend fun createVisitRemote(
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
                "location" to firestoreGeoPoint(latitude = latitude, longitude = longitude),
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
        val refreshedVisit = resolveVisitById(visitId = visitId, idToken = idToken)
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
        val refreshedVisit = resolveVisitById(visitId = visitId, idToken = idToken)
        return refreshedVisit ?: fallbackUpdated
    }

    suspend fun createStampRemote(
        userId: String,
        cafeId: String,
        visitId: String
    ) {
        val idToken = tokenProvider.getIdToken()
        val now = Clock.System.now().toString()
        val path = "${config.documentBasePath()}/${FirestorePaths.STAMPS}/$visitId"
        val body = firestoreDocumentBody(
            mapOf(
                "userId" to firestoreString(userId),
                "cafeId" to firestoreString(cafeId),
                "visitId" to firestoreString(visitId),
                "earnedAt" to firestoreString(now),
                "updatedAt" to firestoreString(now)
            )
        )

        restApi.patch(path, body, idToken)
    }

    suspend fun deleteStampRemote(visitId: String) {
        val idToken = tokenProvider.getIdToken()
        val path = "${config.documentBasePath()}/${FirestorePaths.STAMPS}/$visitId"

        runCatching {
            restApi.delete(path, idToken)
        }
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
        }
    }

    suspend fun getReviewRemote(reviewId: String): Review {
        val idToken = tokenProvider.getIdToken()
        return resolveReviewById(reviewId = reviewId, idToken = idToken)
            ?: throw NoSuchElementException("review not found: $reviewId")
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
        val userNickname = runCatching {
            parseUserDocument(loadUserDocument(userId = userId, idToken = idToken))?.nickname.orEmpty()
        }.recoverCatching {
            parseUserDocument(loadUserDocument(userId = userId, idToken = null))?.nickname.orEmpty()
        }.getOrDefault("")
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
        return existing.copy(
            rating = rating,
            content = content.trim(),
            imageUrls = imageUrls,
            taggedCastIds = taggedCastIds
        )
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

    }

    suspend fun refreshFollowedCastIds(userId: String) {
        fetchFollowedCastIdsRemote(userId)
    }

    suspend fun getFollowedCastsRemote(userId: String): List<Cast> {
        if (userId.isBlank()) {
            return emptyList()
        }
        val idToken = runCatching {
            tokenProvider.getIdToken()
        }.getOrNull()
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
        val followedCastIds = mutableListOf<String>()
        val followsByCafeId = mutableMapOf<String, MutableList<String>>()

        followDocuments.forEach { document ->
            val fields = document["fields"]?.jsonObject ?: return@forEach
            val castId = fields.getFirestoreString("castId")?.takeIf { value -> value.isNotBlank() }
                ?: return@forEach
            val cafeId = fields.getFirestoreString("cafeId")?.takeIf { value -> value.isNotBlank() }
                ?: return@forEach
            val castIds = followsByCafeId.getOrPut(cafeId) { mutableListOf() }

            followedCastIds.add(castId)
            castIds.add(castId)
        }
        if (followsByCafeId.isEmpty()) {
            val idSet = followedCastIds.toSet()
            return fetchCastsByIdsRemote(idSet.toList())
        }
        val resolvedCasts = mutableListOf<Cast>()

        followsByCafeId.forEach { (cafeId, castIds) ->
            val distinctCastIds = castIds.distinct()

            distinctCastIds.chunked(MAX_FIRESTORE_IN_FILTER_VALUES).forEach { chunk ->
                val documents = runCatching {
                    runCafeCastIdsInQuery(
                        cafeId = cafeId,
                        castIds = chunk,
                        idToken = idToken
                    )
                }.recoverCatching {
                    runCafeCastIdsInQuery(
                        cafeId = cafeId,
                        castIds = chunk,
                        idToken = null
                    )
                }.getOrElse { emptyList() }
                val parsed = documents.mapNotNull { document ->
                    parseCastDocument(cafeId = cafeId, document = document)
                }

                resolvedCasts.addAll(parsed)
            }
        }
        val idSet = followedCastIds.toSet()
        return if (resolvedCasts.isNotEmpty()) {
            val resolvedDistinct = resolvedCasts.distinctBy { cast -> cast.id }
            val unresolvedIds = idSet - resolvedDistinct.map { cast -> cast.id }.toSet()
            if (unresolvedIds.isEmpty()) {
                resolvedDistinct
            } else {
                (resolvedDistinct + fetchCastsByIdsRemote(unresolvedIds.toList()))
                    .distinctBy { cast -> cast.id }
            }
        } else {
            fetchCastsByIdsRemote(idSet.toList())
        }
    }

    suspend fun fetchFollowedCastIdsRemote(userId: String): List<String> {
        val idToken = runCatching {
            tokenProvider.getIdToken()
        }.getOrNull()
        val tokenUserId = tokenProvider.getCurrentUserId()
        val primaryFollowDocuments = runCatching {
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
        }.getOrElse { error ->
            println(
                "TEST, fetchFollowedCastIdsRemote query failed: " +
                    "userId=$userId tokenUserId=$tokenUserId tokenPresent=${!idToken.isNullOrBlank()} " +
                    "sameUser=${tokenUserId == userId} message=${error.message}"
            )
            emptyList()
        }
        val legacyFieldDocuments = if (primaryFollowDocuments.isEmpty()) {
            runLegacyUserFieldQuery(
                collectionId = FirestorePaths.CAST_FOLLOWS,
                userId = userId,
                idToken = idToken,
                legacyFieldPath = "uid"
            )
        } else {
            emptyList()
        }
        val legacyNameDocuments = if (primaryFollowDocuments.isEmpty() && legacyFieldDocuments.isEmpty()) {
            runDocumentNamePrefixQuery(
                collectionId = FirestorePaths.CAST_FOLLOWS,
                documentIdPrefix = "${sanitizeDocumentIdPart(userId)}_",
                idToken = idToken
            )
        } else {
            emptyList()
        }
        val followDocuments = (primaryFollowDocuments + legacyFieldDocuments + legacyNameDocuments)
            .distinctBy { document ->
                document["name"]?.jsonPrimitive?.contentOrNull ?: document.toString()
            }
        val castIdsFromDocuments = followDocuments
            .mapNotNull { document ->
                document["fields"]?.jsonObject?.getFirestoreString("castId")
            }
            .distinct()
            .sorted()
        val legacyProfileCastIds = if (castIdsFromDocuments.isEmpty()) {
            loadUserIdListFromProfileDocument(
                userId = userId,
                idToken = idToken,
                candidates = FOLLOWED_CAST_ID_FIELD_CANDIDATES
            )
        } else {
            emptyList()
        }
        val castIds = (castIdsFromDocuments + legacyProfileCastIds)
            .filter { value -> value.isNotBlank() }
            .distinct()
            .sorted()
        println(
            "TEST, fetchFollowedCastIdsRemote result: " +
                "userId=$userId tokenUserId=$tokenUserId tokenPresent=${!idToken.isNullOrBlank()} " +
                "sameUser=${tokenUserId == userId} " +
                "documents=${followDocuments.size} castIds=${castIds.size} " +
                "legacyFieldDocuments=${legacyFieldDocuments.size} legacyNameDocuments=${legacyNameDocuments.size} " +
                "legacyProfileCastIds=${legacyProfileCastIds.size}"
        )
        return castIds
    }

    suspend fun refreshFavoriteCafeIds(userId: String) {
        fetchFavoriteCafeIdsRemote(userId)
    }

    suspend fun refreshFollowerUserIds(castId: String) {
        fetchFollowerUserIdsRemote(castId)
    }

    suspend fun getCastFollowerSnapshots(castId: String): List<CastFollowerSnapshot> {
        val idToken = tokenProvider.getIdToken()
        val followDocuments = runCastFollowerQuery(
            castId = castId,
            idToken = idToken,
            orderByCreatedAtDesc = true,
            limit = RECENT_FOLLOWER_FETCH_LIMIT
        )
        val snapshots = followDocuments.mapNotNull { document ->
            parseCastFollowDocument(document)
        }.filter { snapshot ->
            snapshot.userId.isNotBlank()
        }.sortedByDescending { snapshot ->
            snapshot.followedAt
        }
        return snapshots
    }

    suspend fun fetchFollowerUserIdsRemote(castId: String): List<String> {
        val snapshots = getCastFollowerSnapshots(castId)
        return snapshots
            .map { snapshot -> snapshot.userId }
            .distinct()
            .sorted()
    }

    suspend fun followCastRemote(userId: String, castId: String) {
        ensureAuthenticatedUserMatch(requestedUserId = userId, action = "followCastRemote")
        val idToken = tokenProvider.getIdToken()
        val cast = fetchCastsByIdsRemote(listOf(castId))
            .firstOrNull()
            ?: throw NoSuchElementException("cast not found")
        val followId = buildCastFollowDocumentId(userId = userId, castId = castId)
        val path = "${config.documentBasePath()}/${FirestorePaths.CAST_FOLLOWS}/$followId"
        val createdAt = Clock.System.now().toString()
        val follower = runCatching {
            parseUserDocument(loadUserDocument(userId = userId, idToken = idToken))
        }.recoverCatching {
            parseUserDocument(loadUserDocument(userId = userId, idToken = null))
        }.getOrNull()
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

    suspend fun unfollowCastRemote(userId: String, castId: String) {
        val idToken = tokenProvider.getIdToken()
        val followId = buildCastFollowDocumentId(userId = userId, castId = castId)
        val path = "${config.documentBasePath()}/${FirestorePaths.CAST_FOLLOWS}/$followId"
        restApi.delete(path, idToken)
    }

    suspend fun refreshCastClaimsForUser(userId: String) {
        fetchCastClaimsForUserRemote(userId)
    }

    suspend fun fetchCastClaimsForUserRemote(userId: String): List<CastClaim> {
        val idToken = runCatching {
            tokenProvider.getIdToken()
        }.getOrNull()
        val claimDocuments = runCatching {
            runUserScopedQuery(
                collectionId = FirestorePaths.CAST_CLAIMS,
                userId = userId,
                idToken = idToken
            )
        }.recoverCatching {
            runUserScopedQuery(
                collectionId = FirestorePaths.CAST_CLAIMS,
                userId = userId,
                idToken = null
            )
        }.getOrElse { emptyList() }
        return claimDocuments
            .mapNotNull { document -> parseCastClaimDocument(document) }
            .sortedByDescending { claim -> claim.createdAt }
    }

    suspend fun refreshCastClaimsForCafe(cafeId: String) {
        fetchCastClaimsForCafeRemote(cafeId)
    }

    suspend fun fetchCastClaimsForCafeRemote(cafeId: String): List<CastClaim> {
        val idToken = runCatching {
            tokenProvider.getIdToken()
        }.getOrNull()
        val claimDocuments = runCatching {
            runFieldScopedQuery(
                collectionId = FirestorePaths.CAST_CLAIMS,
                fieldPath = "cafeId",
                fieldValue = cafeId,
                idToken = idToken,
                orderByCreatedAtDesc = false
            )
        }.recoverCatching {
            runFieldScopedQuery(
                collectionId = FirestorePaths.CAST_CLAIMS,
                fieldPath = "cafeId",
                fieldValue = cafeId,
                idToken = null,
                orderByCreatedAtDesc = false
            )
        }.getOrElse { emptyList() }
        return claimDocuments
            .mapNotNull { document -> parseCastClaimDocument(document) }
            .sortedByDescending { claim -> claim.createdAt }
    }

    suspend fun fetchAllCastClaimsRemote(): List<CastClaim> {
        val idToken = runCatching {
            tokenProvider.getIdToken()
        }.getOrNull()
        val claimDocuments = runCatching {
            runCollectionQuery(
                collectionId = FirestorePaths.CAST_CLAIMS,
                idToken = idToken
            )
        }.recoverCatching {
            runCollectionQuery(
                collectionId = FirestorePaths.CAST_CLAIMS,
                idToken = null
            )
        }.getOrElse { emptyList() }
        return claimDocuments
            .mapNotNull { document -> parseCastClaimDocument(document) }
            .sortedByDescending { claim -> claim.createdAt }
    }

    suspend fun hasCastClaimCafeSyncChanged(cafeId: String): Boolean {
        return true
    }

    suspend fun createCastClaimRemote(
        userId: String,
        cafeId: String,
        castId: String,
        message: String?
    ): CastClaim {
        val castName = fetchCastsByIdsRemote(listOf(castId))
            .firstOrNull { cast -> cast.cafeId == cafeId }
            ?.name
            ?: castId
        val idToken = tokenProvider.getIdToken()
        val claimId = nextFirestoreEntityId("cast-claim")
        val createdAt = Clock.System.now().toString()
        val normalizedMessage = message?.trim()?.takeIf { value -> value.isNotEmpty() }
        val requester = parseUserDocument(loadUserDocument(userId = userId, idToken = idToken))
            ?: throw NoSuchElementException("user not found")
        val requesterNickname = requester?.nickname?.trim()?.takeIf { value -> value.isNotEmpty() }
        val requesterProfileImage = requester?.profileImage?.trim()?.takeIf { value -> value.isNotEmpty() }
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
        val claim = CastClaim(
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

        return claim
    }

    suspend fun updateCastClaimStatusRemote(
        claimId: String,
        reviewedBy: String,
        status: CastClaimStatus
    ): CastClaim {
        val idToken = tokenProvider.getIdToken()
        val existing = resolveCastClaimById(claimId = claimId, idToken = idToken)
            ?: throw NoSuchElementException("claim not found")
        val reviewedAt = Clock.System.now().toString()
        val path = "${config.documentBasePath()}/${FirestorePaths.CAST_CLAIMS}/$claimId" +
            "?updateMask.fieldPaths=status" +
            "&updateMask.fieldPaths=reviewedBy" +
            "&updateMask.fieldPaths=reviewedAt"
        val body = firestoreDocumentBody(
            mapOf(
                "status" to firestoreString(status.name),
                "reviewedBy" to firestoreString(reviewedBy),
                "reviewedAt" to firestoreString(reviewedAt)
            )
        )

        restApi.patch(path, body, idToken)
        val updated = existing.copy(
            status = status,
            reviewedBy = reviewedBy,
            reviewedAt = reviewedAt
        )
        return updated
    }

    suspend fun favoriteCafeRemote(userId: String, cafeId: String) {
        ensureAuthenticatedUserMatch(requestedUserId = userId, action = "favoriteCafeRemote")
        val idToken = tokenProvider.getIdToken()

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
    }

    suspend fun unfavoriteCafeRemote(userId: String, cafeId: String) {
        val idToken = tokenProvider.getIdToken()
        val favoriteId = buildCafeFavoriteDocumentId(userId = userId, cafeId = cafeId)
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFE_FAVORITES}/$favoriteId"

        restApi.delete(path, idToken)
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
        return parsedReviews.isNotEmpty()
    }

    suspend fun updateCafeInfoRemote(update: CafeInfoUpdate): CafeDetail {
        val idToken = tokenProvider.getIdToken()
        val cafe = runCatching {
            parseCafeDocument(loadCafeDocument(cafeId = update.cafeId, idToken = idToken))
        }.recoverCatching {
            parseCafeDocument(loadCafeDocument(cafeId = update.cafeId, idToken = null))
        }.getOrNull()
            ?: throw NoSuchElementException("cafe not found")
        val currentDetail = fetchCafeDetailFullRemoteInternal(update.cafeId)
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
        restApi.patch(path, body, idToken)
        val updated = fetchCafeDetailFullRemoteInternal(update.cafeId)

        return updated ?: throw NoSuchElementException("cafe detail not found")
    }

    suspend fun updateCafeSocialMediaRemote(
        cafeId: String,
        instagramId: String?,
        twitterId: String?,
        tiktokId: String?,
        youtubeId: String?
    ) {
        val idToken = tokenProvider.getIdToken()
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFES}/$cafeId" +
            "?updateMask.fieldPaths=socialMedia"
        val mapEntries = buildMap<String, JsonElement> {
            if (instagramId != null) put("instagram", firestoreString(instagramId))
            if (twitterId != null) put("twitter", firestoreString(twitterId))
            if (tiktokId != null) put("tiktok", firestoreString(tiktokId))
            if (youtubeId != null) put("youtube", firestoreString(youtubeId))
        }
        val socialMediaValue = JsonObject(
            mapOf(
                "mapValue" to JsonObject(
                    mapOf("fields" to JsonObject(mapEntries))
                )
            )
        )
        val body = firestoreDocumentBody(mapOf("socialMedia" to socialMediaValue))
        restApi.patch(path, body, idToken)
    }

    suspend fun updateCafeReservationUrlRemote(cafeId: String, reservationUrl: String?) {
        val idToken = tokenProvider.getIdToken()
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFES}/$cafeId" +
            "?updateMask.fieldPaths=reservationUrl"
        val body = firestoreDocumentBody(
            mapOf("reservationUrl" to firestoreNullableString(reservationUrl))
        )
        restApi.patch(path, body, idToken)
    }

    suspend fun upsertCastRemote(update: CastUpsert): CastDetail {
        require(update.name.isNotBlank()) { "cast name is required" }
        require(update.conceptRole.isNotBlank()) { "concept role is required" }

        val providedCastId = update.castId
            ?.takeIf { value -> value.isNotBlank() }
        val remoteExistingCast = providedCastId
            ?.let { castId -> fetchCastsByIdsRemote(listOf(castId)).firstOrNull() }
        val idToken = tokenProvider.getIdToken()
        val targetCafeId = update.cafeId
            ?.takeIf { value -> value.isNotBlank() }
            ?: remoteExistingCast?.cafeId
            ?: if (providedCastId != null) {
                resolveCafeIdByCastId(castId = providedCastId, idToken = idToken)
            } else {
                null
            }
            ?: throw IllegalArgumentException("cafeId is required")
        val existingCast = remoteExistingCast
        val targetCafeDocument = runCatching {
            loadCafeDocument(cafeId = targetCafeId, idToken = idToken)
        }.recoverCatching {
            loadCafeDocument(cafeId = targetCafeId, idToken = null)
        }.getOrThrow()
        parseCafeDocument(targetCafeDocument)
            ?: throw NoSuchElementException("cafe not found")
        val castId = existingCast?.id
            ?: update.castId?.takeIf { value -> value.isNotBlank() }
            ?: nextFirestoreEntityId("cast")
        val normalizedName = update.name.trim()
        val normalizedConceptRole = update.conceptRole.trim()
        val normalizedIntroduction = update.introduction.trim()
        val normalizedBirthdaySource = update.birthday
            ?.trim()
            ?.takeIf { value -> value.isNotEmpty() }
        val normalizedBirthday = normalizedBirthdaySource?.normalizeBirthdayToIsoOrNull()
            ?: normalizedBirthdaySource
        val normalizedBirthdayKey = normalizedBirthday.toBirthdayKeyOrNull()
        val normalizedProfileImage = update.profileImage
            ?.trim()
            ?.takeIf { value -> value.isNotEmpty() }
            ?: existingCast?.profileImage
        val normalizedGalleryImages = update.galleryImages
            .map { image -> image.trim() }
            .filter { image -> image.isNotEmpty() }
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

        runCatching {
            restApi.patch(path, body, idToken)
        }.onFailure { error ->
            println("TEST, upsertCastRemote patch failed: ${error.message}")
        }.getOrThrow()
        runCatching {
            refreshCafeDetail(targetCafeId)
        }.onFailure { error ->
            println("TEST, upsertCastRemote refreshCafeDetail failed: ${error.message}")
        }
        val refreshedDetail = fetchCastDetailRemoteByCafeAndCastId(
            cafeId = targetCafeId,
            castId = castId,
            idToken = idToken
        )

        if (refreshedDetail == null) {
            throw NoSuchElementException("cast detail not found")
        } else {
            return refreshedDetail
        }
    }

    suspend fun fetchBirthdayCastsRemote(
        month: Int,
        dayOfMonth: Int,
        limit: Int
    ): List<Cast> {
        val safeLimit = if (limit > 0) limit else 1
        val birthdayKey = monthDayToBirthdayKey(month = month, dayOfMonth = dayOfMonth)
        val idToken = runCatching {
            tokenProvider.getIdToken()
        }.getOrNull()
        val castDocuments = runCatching {
            runCastByBirthdayKeyQuery(
                birthdayKey = birthdayKey,
                idToken = idToken,
                limit = safeLimit
            )
        }.recoverCatching {
            runCastByBirthdayKeyQuery(
                birthdayKey = birthdayKey,
                idToken = null,
                limit = safeLimit
            )
        }.getOrElse { throwable ->
            throw IllegalStateException("Failed to load birthday casts", throwable)
        }
        return parseCollectionGroupCastDocuments(castDocuments)
            .filter { cast ->
                cast.birthday.matchesMonthAndDay(month = month, dayOfMonth = dayOfMonth)
            }
            .take(safeLimit)
    }

    suspend fun getHomePopularCastPageRemote(
        cursor: String?,
        pageSize: Int
    ): PagedResult<Cast> {
        return searchCastsRemote(
            query = null,
            country = null,
            city = null,
            sort = CastSort.POPULAR,
            cursor = cursor,
            pageSize = pageSize
        )
    }

    suspend fun searchCafesRemote(
        query: String?,
        country: String?,
        city: String?,
        sort: CafeSort,
        cursor: String?,
        pageSize: Int
    ): PagedResult<Cafe> {
        val safePageSize = if (pageSize > 0) pageSize else 1
        val normalizedQuery = query?.trim()?.takeIf { value -> value.isNotEmpty() }
        val normalizedCountry = country?.trim()?.takeIf { value -> value.isNotEmpty() }
        val normalizedCity = city?.trim()?.takeIf { value -> value.isNotEmpty() }
        val queryBatchSize = safePageSize.coerceAtMost(50) * 3
        var nextCursorToken = cursor
        var exhausted = false
        val aggregated = mutableListOf<Cafe>()
        val idToken = runCatching {
            tokenProvider.getIdToken()
        }.getOrNull()

        while (aggregated.size < safePageSize && !exhausted) {
            val queryCursor = nextCursorToken
            val documents = runCatching {
                runCafeCollectionQuery(
                    sort = sort,
                    cursor = queryCursor,
                    limit = queryBatchSize + 1,
                    query = normalizedQuery,
                    country = normalizedCountry,
                    city = normalizedCity,
                    idToken = idToken
                )
            }.recoverCatching {
                runCafeCollectionQuery(
                    sort = sort,
                    cursor = queryCursor,
                    limit = queryBatchSize + 1,
                    query = normalizedQuery,
                    country = normalizedCountry,
                    city = normalizedCity,
                    idToken = null
                )
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
                val matchesQuery = if (normalizedQuery == null) {
                    true
                } else {
                    cafe.name.contains(normalizedQuery, ignoreCase = true)
                }
                cafe.approved && matchesQuery
            }
            val remaining = safePageSize - aggregated.size
            val takenWithDocs = filteredWithDocs.take(remaining)

            aggregated.addAll(takenWithDocs.map { (_, cafe) -> cafe })
            val hasMoreInBatch = takenWithDocs.size < filteredWithDocs.size
            nextCursorToken = if (hasMoreInBatch) {
                takenWithDocs.lastOrNull()?.first?.toCafeQueryCursor(sort)
                    ?: lastBatchDocument?.toCafeQueryCursor(sort)
            } else {
                lastBatchDocument?.toCafeQueryCursor(sort)
            }
            exhausted = !hasMoreBatch && !hasMoreInBatch
        }
        return PagedResult(
            items = aggregated,
            nextCursor = if (exhausted) null else nextCursorToken,
            hasNext = !exhausted
        )
    }

    suspend fun searchCastsRemote(
        query: String?,
        country: String?,
        city: String?,
        sort: CastSort,
        cursor: String?,
        pageSize: Int
    ): PagedResult<Cast> {
        val normalizedQuery = query?.trim()?.takeIf { value -> value.isNotEmpty() }
        val normalizedCountry = country?.trim()?.takeIf { value -> value.isNotEmpty() }
        val normalizedCity = city?.trim()?.takeIf { value -> value.isNotEmpty() }
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        val targetCafeIds = loadCafeIdsByRegionRemote(
            country = normalizedCountry,
            city = normalizedCity,
            idToken = idToken
        )

        if (targetCafeIds != null && targetCafeIds.isEmpty()) {
            return PagedResult(
                items = emptyList(),
                nextCursor = null,
                hasNext = false
            )
        }
        val safePageSize = if (pageSize > 0) pageSize else 1
        val queryBatchSize = safePageSize.coerceAtMost(50) * 3
        var nextCursorToken = cursor
        var exhausted = false
        val aggregated = mutableListOf<Cast>()

        while (aggregated.size < safePageSize && !exhausted) {
            val queryCursor = nextCursorToken
            val documents = runCatching {
                runCastCollectionGroupQuery(
                    sort = sort,
                    cursor = queryCursor,
                    limit = queryBatchSize + 1,
                    idToken = idToken
                )
            }.recoverCatching {
                runCastCollectionGroupQuery(
                    sort = sort,
                    cursor = queryCursor,
                    limit = queryBatchSize + 1,
                    idToken = null
                )
            }.getOrElse { throwable ->
                throw IllegalStateException("Failed to search casts: ${throwable.message}", throwable)
            }
            val batch = documents.take(queryBatchSize)
            val hasMoreBatch = documents.size > queryBatchSize
            val lastBatchDocument = batch.lastOrNull()
            val parsedWithDocs = batch.mapNotNull { document ->
                val documentName = document["name"]?.jsonPrimitive?.contentOrNull
                val fields = document["fields"]?.jsonObject
                val cafeId = documentName?.toCafeIdFromCastDocumentName()
                    ?: fields?.getFirestoreString("cafeId")
                    ?: ""
                parseCastDocument(cafeId = cafeId, document = document)?.let { cast -> document to cast }
            }
            val filteredWithDocs = parsedWithDocs.filter { (_, cast) ->
                val matchesQuery = if (normalizedQuery == null) {
                    true
                } else {
                    cast.name.contains(normalizedQuery, ignoreCase = true)
                }
                val matchesCafe = targetCafeIds?.contains(cast.cafeId) ?: true
                matchesQuery && matchesCafe
            }
            val remaining = safePageSize - aggregated.size
            val takenWithDocs = filteredWithDocs.take(remaining)
            aggregated.addAll(takenWithDocs.map { (_, cast) -> cast })
            val hasMoreInBatch = takenWithDocs.size < filteredWithDocs.size
            nextCursorToken = if (hasMoreInBatch) {
                takenWithDocs.lastOrNull()?.first?.toCastQueryCursor(sort)
                    ?: lastBatchDocument?.toCastQueryCursor(sort)
            } else {
                lastBatchDocument?.toCastQueryCursor(sort)
            }
            exhausted = !hasMoreBatch && !hasMoreInBatch
        }
        return PagedResult(
            items = aggregated,
            nextCursor = if (exhausted) null else nextCursorToken,
            hasNext = !exhausted
        )
    }

    suspend fun fetchAllCastsRemote(): List<Cast> {
        val loaded = mutableListOf<Cast>()
        var cursor: String? = null
        var hasNext = true

        while (hasNext) {
            val page = searchCastsRemote(
                query = null,
                country = null,
                city = null,
                sort = CastSort.LATEST,
                cursor = cursor,
                pageSize = 200
            )

            loaded.addAll(page.items)
            cursor = page.nextCursor
            hasNext = page.hasNext && !cursor.isNullOrBlank()
        }
        return loaded.distinctBy { cast -> cast.id }
    }


    suspend fun getCafeCastPageRemote(
        cafeId: String,
        cursor: String?,
        pageSize: Int
    ): PagedResult<Cast> {
        val safePageSize = if (pageSize > 0) pageSize else 1
        val idToken = runCatching {
            tokenProvider.getIdToken()
        }.getOrNull()
        val documents = runCatching {
            runCafeCastQuery(
                cafeId = cafeId,
                cursor = cursor,
                limit = safePageSize + 1,
                idToken = idToken
            )
        }.recoverCatching {
            runCafeCastQuery(
                cafeId = cafeId,
                cursor = cursor,
                limit = safePageSize + 1,
                idToken = null
            )
        }.getOrElse { throwable ->
            throw IllegalStateException("Failed to load cafe cast page($cafeId): ${throwable.message}", throwable)
        }
        val parsed = documents.mapNotNull { document ->
            parseCastDocument(cafeId = cafeId, document = document)
        }
        val hasNext = parsed.size > safePageSize
        val items = parsed.take(safePageSize)
        val nextCursorToken = if (hasNext) {
            documents.getOrNull(safePageSize - 1)?.toCafeCastQueryCursor()
        } else {
            null
        }
        return PagedResult(
            items = items,
            nextCursor = nextCursorToken,
            hasNext = hasNext
        )
    }

    suspend fun fetchCafeCastsByCafeIdRemote(cafeId: String): List<Cast> {
        val loaded = mutableListOf<Cast>()
        var cursor: String? = null
        var hasNext = true

        while (hasNext) {
            val page = getCafeCastPageRemote(
                cafeId = cafeId,
                cursor = cursor,
                pageSize = 200
            )

            loaded.addAll(page.items)
            cursor = page.nextCursor
            hasNext = page.hasNext && !cursor.isNullOrBlank()
        }
        return loaded.distinctBy { cast -> cast.id }
    }

    suspend fun fetchCafeCastCountRemote(cafeId: String): Int {
        val idToken = runCatching {
            tokenProvider.getIdToken()
        }.getOrNull()
        return runCatching {
            loadSubCollectionDocumentCount(
                parentPath = "${config.documentBasePath()}/${FirestorePaths.CAFES}/$cafeId",
                collectionId = FirestorePaths.CAFE_CASTS,
                idToken = idToken
            )
        }.recoverCatching {
            loadSubCollectionDocumentCount(
                parentPath = "${config.documentBasePath()}/${FirestorePaths.CAFES}/$cafeId",
                collectionId = FirestorePaths.CAFE_CASTS,
                idToken = null
            )
        }.getOrThrow()
    }

    suspend fun fetchCastsByIdsRemote(castIds: List<String>): List<Cast> {
        val targetIds = castIds
            .asSequence()
            .map { castId -> castId.trim() }
            .filter { castId -> castId.isNotEmpty() }
            .toSet()

        if (targetIds.isEmpty()) {
            return emptyList()
        }
        val idToken = runCatching {
            tokenProvider.getIdToken()
        }.getOrNull()
        val resolved = mutableListOf<Cast>()
        targetIds.forEach { castId ->
            val cafeId = resolveCafeIdByCastId(castId = castId, idToken = idToken) ?: return@forEach
            val cast = resolveCafeCastById(
                cafeId = cafeId,
                castId = castId,
                idToken = idToken
            ) ?: return@forEach
            resolved.add(cast)
        }
        return resolved.distinctBy { cast -> cast.id }
    }

    suspend fun deleteCastRemote(castId: String): Cast {
        require(castId.isNotBlank()) { "castId is required" }

        val existingCast = fetchCastsByIdsRemote(listOf(castId))
            .firstOrNull()
            ?: throw NoSuchElementException("cast detail not found")
        val idToken = tokenProvider.getIdToken()
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFES}/${existingCast.cafeId}/${FirestorePaths.CAFE_CASTS}/$castId"

        restApi.delete(path, idToken)
        runCatching {
            refreshCafeDetail(existingCast.cafeId)
        }
        return existingCast
    }

    suspend fun refreshCastSchedulesRemote(
        castId: String,
        fromDate: String,
        toDate: String
    ) {
        require(castId.isNotBlank()) { "castId is required" }
        val idToken = tokenProvider.getIdToken()
        val schedulesDocuments = runCastScheduleRangeQuery(
            castId = castId,
            fromDate = fromDate,
            toDate = toDate,
            idToken = idToken
        )
        val remoteEntries = schedulesDocuments.mapNotNull { document ->
            parseCastScheduleDocument(document)
        }.filter { scheduleEntry ->
            scheduleEntry.castId == castId && scheduleEntry.date >= fromDate && scheduleEntry.date <= toDate
        }
    }

    suspend fun refreshCastByLinkedUserId(userId: String): Cast? {
        require(userId.isNotBlank()) { "userId is required" }
        val idToken = tokenProvider.getIdToken()
        val documents = resolveCastDocumentsByUser(userId = userId, idToken = idToken)
        val firstDocument = documents.firstOrNull() ?: return null
        val name = firstDocument["name"]?.jsonPrimitive?.contentOrNull ?: return null
        val cafeId = extractCafeIdFromCastDocumentName(name) ?: return null
        println(
            "TEST, refreshCastByLinkedUserId result: " +
                "userId=$userId documents=${documents.size} cafeId=$cafeId"
        )
        return parseCastDocument(cafeId = cafeId, document = firstDocument)
    }

    suspend fun refreshCafeCastsRemote(cafeId: String) {
        require(cafeId.isNotBlank()) { "cafeId is required" }
        fetchCafeCastsByCafeIdRemote(cafeId)
    }

    suspend fun refreshCastDetailRemote(castId: String) {
        require(castId.isNotBlank()) { "castId is required" }
        fetchCastDetailRemoteById(castId)
    }

    suspend fun fetchCastDetailRemote(castId: String): CastDetail? {
        require(castId.isNotBlank()) { "castId is required" }

        return fetchCastDetailRemoteById(castId)
    }

    suspend fun fetchCastSchedulesRemote(
        castId: String,
        fromDate: String,
        toDate: String
    ): List<CastSchedule> {
        require(castId.isNotBlank()) { "castId is required" }
        return fetchCastSchedulesRemoteInternal(
            castId = castId,
            fromDate = fromDate,
            toDate = toDate,
            idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        )
    }

    suspend fun fetchCastScheduleStatusesRemote(
        castId: String,
        fromDate: String,
        toDate: String
    ): Map<String, CastScheduleStatus> {
        require(castId.isNotBlank()) { "castId is required" }

        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        val documents = runCatching {
            runCastScheduleRangeQuery(
                castId = castId,
                fromDate = fromDate,
                toDate = toDate,
                idToken = idToken
            )
        }.recoverCatching {
            runCastScheduleRangeQuery(
                castId = castId,
                fromDate = fromDate,
                toDate = toDate,
                idToken = null
            )
        }.getOrElse { emptyList() }
        return documents.mapNotNull { document ->
            val entry = parseCastScheduleDocument(document) ?: return@mapNotNull null
            entry.date to entry.status
        }.toMap()
    }

    suspend fun isReviewPromptDismissedRemote(userId: String, visitId: String): Boolean {
        require(userId.isNotBlank()) { "userId is required" }
        require(visitId.isNotBlank()) { "visitId is required" }

        val userDocument = runCatching {
            loadUserDocument(userId = userId, idToken = tokenProvider.getIdToken())
        }.recoverCatching {
            loadUserDocument(userId = userId, idToken = null)
        }.getOrNull() ?: return false
        val fields = userDocument["fields"]?.jsonObject ?: return false
        val dismissedVisitIds = fields.getFirestoreStringList("dismissedReviewPromptVisitIds")
        return dismissedVisitIds.contains(visitId)
    }

    suspend fun dismissReviewPromptRemote(userId: String, visitId: String) {
        require(userId.isNotBlank()) { "userId is required" }
        require(visitId.isNotBlank()) { "visitId is required" }

        val userDocument = runCatching {
            loadUserDocument(userId = userId, idToken = tokenProvider.getIdToken())
        }.recoverCatching {
            loadUserDocument(userId = userId, idToken = null)
        }.getOrNull()
        val fields = userDocument
            ?.get("fields")
            ?.jsonObject
        val currentIds = fields
            ?.getFirestoreStringList("dismissedReviewPromptVisitIds")
            .orEmpty()
        val nextIds = (currentIds + visitId).distinct()
        val path = "${config.documentBasePath()}/${FirestorePaths.USERS}/$userId?updateMask.fieldPaths=dismissedReviewPromptVisitIds"
        val body = firestoreDocumentBody(
            mapOf(
                "dismissedReviewPromptVisitIds" to firestoreStringArray(nextIds)
            )
        )

        runCatching {
            restApi.patch(path, body, tokenProvider.getIdToken())
        }.recoverCatching {
            restApi.patch(path, body, null)
        }.getOrElse { throwable ->
            throw IllegalStateException("Failed to dismiss review prompt for user: $userId", throwable)
        }
    }

    suspend fun updateCastScheduleRemote(update: CastScheduleUpdate): CastSchedule? {
        require(update.castId.isNotBlank()) { "castId is required" }
        val idToken = tokenProvider.getIdToken()
        val resolvedCafeId = resolveCafeIdByCastId(castId = update.castId, idToken = idToken)
            ?: throw NoSuchElementException("cast detail not found")
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
                        "cafeId" to firestoreString(resolvedCafeId),
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
                        "cafeId" to firestoreString(resolvedCafeId),
                        "date" to firestoreString(update.date),
                        "startTime" to firestoreNullableString(null),
                        "endTime" to firestoreNullableString(null),
                        "status" to firestoreString(update.status.name)
                    )
                )
                restApi.patch(schedulePath, scheduleBody, idToken)
            }
        }

        refreshCastSchedulesRemote(update.castId, update.date, update.date)
        return fetchCastSchedulesRemoteInternal(
            castId = update.castId,
            fromDate = update.date,
            toDate = update.date,
            idToken = idToken
        ).firstOrNull()
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
        val menuPath = "${config.documentBasePath()}/${FirestorePaths.CAFES}/${update.cafeId}/${FirestorePaths.CAFE_MENUS}/$itemId"
        val goodsPath = "${config.documentBasePath()}/${FirestorePaths.CAFES}/${update.cafeId}/${FirestorePaths.CAFE_GOODS}/$itemId"
        val existingMenu = runCatching {
            parseMenuDocument(Json.parseToJsonElement(restApi.get(menuPath, idToken)).jsonObject)
        }.recoverCatching {
            parseMenuDocument(Json.parseToJsonElement(restApi.get(menuPath, null)).jsonObject)
        }.getOrNull()
        val existingGoods = runCatching {
            parseGoodsDocument(Json.parseToJsonElement(restApi.get(goodsPath, idToken)).jsonObject)
        }.recoverCatching {
            parseGoodsDocument(Json.parseToJsonElement(restApi.get(goodsPath, null)).jsonObject)
        }.getOrNull()
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
            restApi.patch(goodsPath, body, idToken)
            if (existingMenu != null) {
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
            restApi.patch(menuPath, body, idToken)
            if (existingGoods != null) {
                runCatching { restApi.delete(goodsPath, idToken) }
            }
        }
        val updated = fetchCafeDetailFullRemoteInternal(update.cafeId)
        return updated ?: throw NoSuchElementException("cafe detail not found")
    }

    suspend fun deleteCafeMenuGoodsRemote(cafeId: String, itemId: String): CafeDetail {
        require(cafeId.isNotBlank()) { "cafeId is required" }
        require(itemId.isNotBlank()) { "itemId is required" }

        val idToken = tokenProvider.getIdToken()
        val menuPath = "${config.documentBasePath()}/${FirestorePaths.CAFES}/$cafeId/${FirestorePaths.CAFE_MENUS}/$itemId"
        val goodsPath = "${config.documentBasePath()}/${FirestorePaths.CAFES}/$cafeId/${FirestorePaths.CAFE_GOODS}/$itemId"
        var deleted = false

        if (runCatching { restApi.delete(menuPath, idToken) }.isSuccess) {
            deleted = true
        }
        if (runCatching { restApi.delete(goodsPath, idToken) }.isSuccess) {
            deleted = true
        }
        if (!deleted) {
            throw NoSuchElementException("menu goods item not found")
        }
        val updated = fetchCafeDetailFullRemoteInternal(cafeId)
        return updated ?: throw NoSuchElementException("cafe detail not found")
    }

    suspend fun fetchAffiliatedCafeIdRemote(userId: String): String? {
        val idToken = runCatching {
            tokenProvider.getIdToken()
        }.getOrNull()
        val userDocument = runCatching {
            loadUserDocument(userId = userId, idToken = idToken)
        }.recoverCatching {
            loadUserDocument(userId = userId, idToken = null)
        }.getOrNull()
        return if (userDocument == null) {
            null
        } else {
            parseUserAffiliatedCafeId(userDocument)
        }
    }

    suspend fun setAffiliatedCafeIdRemote(userId: String, cafeId: String) {
        val idToken = tokenProvider.getIdToken()
        val path = "${config.documentBasePath()}/${FirestorePaths.USERS}/$userId?updateMask.fieldPaths=affiliatedCafeId"
        val body = firestoreDocumentBody(
            mapOf(
                "affiliatedCafeId" to firestoreString(cafeId)
            )
        )

        restApi.patch(path, body, idToken)
    }

    suspend fun clearAffiliatedCafeIdRemote(userId: String) {
        val idToken = tokenProvider.getIdToken()
        val path = "${config.documentBasePath()}/${FirestorePaths.USERS}/$userId?updateMask.fieldPaths=affiliatedCafeId"
        val body = firestoreDocumentBody(
            mapOf(
                "affiliatedCafeId" to firestoreNullableString(null)
            )
        )

        restApi.patch(path, body, idToken)
    }

    override suspend fun fetchUser(userId: String): User? {
        val idToken = runCatching {
            tokenProvider.getIdToken()
        }.getOrNull()
        val path = "${config.documentBasePath()}/${FirestorePaths.USERS}/$userId"
        return runCatching {
            val response = restApi.get(path, idToken)
            val parsed = Json.parseToJsonElement(response).jsonObject
            parseUserDocument(parsed)
        }.recoverCatching {
            val response = restApi.get(path, null)
            val parsed = Json.parseToJsonElement(response).jsonObject
            parseUserDocument(parsed)
        }.getOrNull()
    }

    override suspend fun fetchMyPageSummary(userId: String): MyPageSummary? {
        val idToken = runCatching {
            tokenProvider.getIdToken()
        }.getOrNull()
        val path = "${config.documentBasePath()}/${FirestorePaths.USERS}/$userId"
        return runCatching {
            val response = restApi.get(path, idToken)
            val parsed = Json.parseToJsonElement(response).jsonObject
            val parsedSummary = parseMyPageSummaryDocument(userId = userId, document = parsed)
            val resolvedVisitCount = resolveMyPageVisitCount(
                userId = userId,
                fallbackVisitCount = parsedSummary.totalVisits,
                idToken = idToken
            )
            val resolvedStampCount = resolveMyPageStampCount(
                userId = userId,
                fallbackStampCount = parsedSummary.badgesCount,
                idToken = idToken
            )

            parsedSummary.copy(
                totalVisits = resolvedVisitCount,
                badgesCount = resolvedStampCount
            )
        }.getOrNull()
    }

    override suspend fun updateUserProfile(
        userId: String,
        nickname: String,
        profileImage: String?
    ) {
        val idToken = tokenProvider.getIdToken()
        val path = "${config.documentBasePath()}/${FirestorePaths.USERS}/$userId?updateMask.fieldPaths=nickname&updateMask.fieldPaths=profileImage"
        val body = firestoreDocumentBody(
            mapOf(
                "nickname" to firestoreString(nickname),
                "profileImage" to firestoreNullableString(profileImage)
            )
        )

        restApi.patch(path, body, idToken)
    }

    override suspend fun pushUser(user: User) {
        val idToken = tokenProvider.getIdToken()
        val path = "${config.documentBasePath()}/${FirestorePaths.USERS}/${user.id}"
        val existingAffiliatedCafeId = runCatching {
            loadUserDocument(userId = user.id, idToken = idToken)
        }.recoverCatching {
            loadUserDocument(userId = user.id, idToken = null)
        }.getOrNull()?.let { document ->
            parseUserAffiliatedCafeId(document)
        }
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
        restApi.patch(path, body, idToken)
    }

    override suspend fun deleteUser(userId: String) {
        val idToken = tokenProvider.getIdToken()
        val path = "${config.documentBasePath()}/${FirestorePaths.USERS}/$userId"
        restApi.delete(path, idToken)
    }

    override suspend fun deleteCurrentUserCascade(idToken: String) {
        val path = "${config.functionsBaseUrl()}/deleteCurrentUserCascade"
        restApi.post(path = path, body = "{}", idToken = idToken)
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
        val idToken = runCatching {
            tokenProvider.getIdToken()
        }.getOrNull()

        runCatching {
            loadHomeBanners(idToken = idToken)
        }.recoverCatching {
            loadHomeBanners(idToken = null)
        }.getOrThrow()
    }

    suspend fun fetchHomeBanners(): List<HomeBanner> {
        val idToken = runCatching {
            tokenProvider.getIdToken()
        }.getOrNull()

        return runCatching {
            loadHomeBanners(idToken = idToken)
        }.recoverCatching {
            loadHomeBanners(idToken = null)
        }.getOrThrow()
    }

    suspend fun fetchAllCafesRemote(): List<Cafe> {
        val loaded = mutableListOf<Cafe>()
        var cursor: String? = null
        var hasNext = true

        while (hasNext) {
            val page = searchCafesRemote(
                query = null,
                country = null,
                city = null,
                sort = CafeSort.LATEST,
                cursor = cursor,
                pageSize = 200
            )

            loaded.addAll(page.items)
            cursor = page.nextCursor
            hasNext = page.hasNext && !cursor.isNullOrBlank()
        }
        return loaded.distinctBy { cafe -> cafe.id }
    }

    suspend fun fetchFavoriteCafeIdsRemote(userId: String, limit: Int? = null): List<String> {
        val queryLimit = if (limit != null && limit > 0) limit else null
        val idToken = runCatching {
            tokenProvider.getIdToken()
        }.getOrNull()
        val tokenUserId = tokenProvider.getCurrentUserId()
        val primaryFavoriteDocuments = runCatching {
            runUserScopedQuery(
                collectionId = FirestorePaths.CAFE_FAVORITES,
                userId = userId,
                idToken = idToken,
                limit = queryLimit
            )
        }.recoverCatching {
            runUserScopedQuery(
                collectionId = FirestorePaths.CAFE_FAVORITES,
                userId = userId,
                idToken = null,
                limit = queryLimit
            )
        }.getOrElse { error ->
            println(
                "TEST, fetchFavoriteCafeIdsRemote query failed: " +
                    "userId=$userId tokenUserId=$tokenUserId tokenPresent=${!idToken.isNullOrBlank()} " +
                    "sameUser=${tokenUserId == userId} message=${error.message}"
            )
            emptyList()
        }
        val legacyFieldDocuments = if (primaryFavoriteDocuments.isEmpty()) {
            runLegacyUserFieldQuery(
                collectionId = FirestorePaths.CAFE_FAVORITES,
                userId = userId,
                idToken = idToken,
                legacyFieldPath = "uid"
            )
        } else {
            emptyList()
        }
        val legacyNameDocuments = if (primaryFavoriteDocuments.isEmpty() && legacyFieldDocuments.isEmpty()) {
            runDocumentNamePrefixQuery(
                collectionId = FirestorePaths.CAFE_FAVORITES,
                documentIdPrefix = "${sanitizeDocumentIdPart(userId)}_",
                idToken = idToken
            )
        } else {
            emptyList()
        }
        val favoriteDocuments = (primaryFavoriteDocuments + legacyFieldDocuments + legacyNameDocuments)
            .distinctBy { document ->
                document["name"]?.jsonPrimitive?.contentOrNull ?: document.toString()
            }
        val cafeIdsFromDocuments = favoriteDocuments
            .mapNotNull { document ->
                document["fields"]?.jsonObject?.getFirestoreString("cafeId")
            }
            .distinct()
            .sorted()
        val legacyProfileCafeIds = if (cafeIdsFromDocuments.isEmpty()) {
            loadUserIdListFromProfileDocument(
                userId = userId,
                idToken = idToken,
                candidates = FAVORITE_CAFE_ID_FIELD_CANDIDATES
            )
        } else {
            emptyList()
        }
        val cafeIds = (cafeIdsFromDocuments + legacyProfileCafeIds)
            .filter { value -> value.isNotBlank() }
            .distinct()
            .sorted()
            .let { ids ->
                if (queryLimit != null) ids.take(queryLimit) else ids
            }
        println(
            "TEST, fetchFavoriteCafeIdsRemote result: " +
                "userId=$userId tokenUserId=$tokenUserId tokenPresent=${!idToken.isNullOrBlank()} " +
                "sameUser=${tokenUserId == userId} " +
                "documents=${favoriteDocuments.size} cafeIds=${cafeIds.size} " +
                "legacyFieldDocuments=${legacyFieldDocuments.size} legacyNameDocuments=${legacyNameDocuments.size} " +
                "legacyProfileCafeIds=${legacyProfileCafeIds.size}"
        )
        return cafeIds
    }

    suspend fun fetchOwnedCafeIdsRemote(userId: String): Set<String> {
        val idToken = runCatching {
            tokenProvider.getIdToken()
        }.getOrNull()
        val ownerClaimDocuments = runCatching {
            runUserScopedQuery(
                collectionId = FirestorePaths.CAFE_OWNER_CLAIMS,
                userId = userId,
                idToken = idToken
            )
        }.recoverCatching {
            runUserScopedQuery(
                collectionId = FirestorePaths.CAFE_OWNER_CLAIMS,
                userId = userId,
                idToken = null
            )
        }.getOrElse { emptyList() }
        val registrationClaimDocuments = runCatching {
            runUserScopedQuery(
                collectionId = FirestorePaths.CAFE_REGISTRATION_CLAIMS,
                userId = userId,
                idToken = idToken
            )
        }.recoverCatching {
            runUserScopedQuery(
                collectionId = FirestorePaths.CAFE_REGISTRATION_CLAIMS,
                userId = userId,
                idToken = null
            )
        }.getOrElse { emptyList() }
        val ownedCafeIds = mutableSetOf<String>()

        ownerClaimDocuments
            .mapNotNull { document -> parsePendingCafeOwnerClaimDocument(document) }
            .forEach { claim ->
                if (claim.status.isApprovedClaimStatus()) {
                    ownedCafeIds.add(claim.cafeId)
                }
            }
        registrationClaimDocuments.forEach { document ->
            val fields = document["fields"]?.jsonObject
            val status = fields?.getFirestoreString("status").orEmpty()
            val approvedCafeId = fields?.getFirestoreString("approvedCafeId")
                ?: fields?.getFirestoreString("cafeId")

            if (status.isApprovedClaimStatus() && !approvedCafeId.isNullOrBlank()) {
                ownedCafeIds.add(approvedCafeId)
            }
        }
        val userDocument = runCatching {
            loadUserDocument(userId = userId, idToken = idToken)
        }.recoverCatching {
            loadUserDocument(userId = userId, idToken = null)
        }.getOrNull()
        val ownedCafeIdsFromUser = userDocument
            ?.get("fields")
            ?.jsonObject
            ?.getFirestoreStringList("ownedCafeIds")
            .orEmpty()

        ownedCafeIds.addAll(ownedCafeIdsFromUser)
        return ownedCafeIds
    }

    suspend fun fetchPendingCafeOwnerClaimsRemote(userId: String): List<CafeManagementData.PendingClaimSummary> {
        val idToken = runCatching {
            tokenProvider.getIdToken()
        }.getOrNull()
        val ownerClaimDocuments = runCatching {
            runUserScopedQuery(
                collectionId = FirestorePaths.CAFE_OWNER_CLAIMS,
                userId = userId,
                idToken = idToken
            )
        }.recoverCatching {
            runUserScopedQuery(
                collectionId = FirestorePaths.CAFE_OWNER_CLAIMS,
                userId = userId,
                idToken = null
            )
        }.getOrElse { emptyList() }
        return ownerClaimDocuments
            .mapNotNull { document -> parsePendingCafeOwnerClaimDocument(document) }
            .sortedByDescending { claim -> claim.requestedAt }
    }

    suspend fun fetchPendingCafeRegistrationClaimsRemote(userId: String): List<CafeRegistrationClaim> {
        val idToken = runCatching {
            tokenProvider.getIdToken()
        }.getOrNull()
        val registrationClaimDocuments = runCatching {
            runUserScopedQuery(
                collectionId = FirestorePaths.CAFE_REGISTRATION_CLAIMS,
                userId = userId,
                idToken = idToken
            )
        }.recoverCatching {
            runUserScopedQuery(
                collectionId = FirestorePaths.CAFE_REGISTRATION_CLAIMS,
                userId = userId,
                idToken = null
            )
        }.getOrElse { emptyList() }
        return registrationClaimDocuments
            .mapNotNull { document -> parseCafeRegistrationClaimDocument(document) }
            .sortedByDescending { claim -> claim.requestedAt }
    }

    suspend fun fetchCafeTodayCheckInCountRemote(cafeId: String): Int {
        val today = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
            .toString()
        val visits = fetchVisitsByCafeRemote(cafeId)
        return visits.count { visit -> visit.visitedAt.startsWith(today) }
    }

    suspend fun fetchCafeCheckInCountRemote(cafeId: String): Int {
        val idToken = runCatching {
            tokenProvider.getIdToken()
        }.getOrNull()
        return runCatching {
            loadCollectionDocumentCount(
                collectionId = FirestorePaths.VISITS,
                idToken = idToken,
                equalsFilterFieldPath = "cafeId",
                equalsFilterValue = firestoreString(cafeId)
            )
        }.recoverCatching {
            loadCollectionDocumentCount(
                collectionId = FirestorePaths.VISITS,
                idToken = null,
                equalsFilterFieldPath = "cafeId",
                equalsFilterValue = firestoreString(cafeId)
            )
        }.getOrThrow()
    }

    suspend fun fetchCafeTodayReviewCountRemote(cafeId: String): Int {
        val today = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
            .toString()
        val reviews = getCafeReviewsPageRemote(cafeId = cafeId, cursor = null, pageSize = 2000).items
        return reviews.count { review -> review.createdAt.startsWith(today) }
    }

    suspend fun fetchCafeHomeBannerPreviewRemote(cafeId: String): CafeDashboardData.HomeBannerPreview? {
        val banners = fetchHomeBanners()
        val targetBanner = banners
            .asSequence()
            .filter { banner -> banner.cafeId == cafeId }
            .sortedWith(
                compareByDescending<HomeBanner> { banner -> banner.statusLabel.uppercase() == "ACTIVE" }
                    .thenByDescending { banner -> banner.createdAtEpochMillis }
            )
            .firstOrNull()
        return if (targetBanner == null) {
            null
        } else {
            val statusLabel = when (targetBanner.statusLabel.uppercase()) {
                "ACTIVE" -> "노출 중"
                "SCHEDULED" -> "예약 중"
                else -> "미노출"
            }

            CafeDashboardData.HomeBannerPreview(
                title = targetBanner.title,
                period = resolvePeriodLabel(targetBanner.displayDays),
                statusLabel = statusLabel,
                imageUrl = targetBanner.imageUrl
            )
        }
    }

    suspend fun fetchNoticeCountRemote(cafeId: String): Int {
        val idToken = runCatching {
            tokenProvider.getIdToken()
        }.getOrNull()
        return runCatching {
            loadSubCollectionDocumentCount(
                parentPath = "${config.documentBasePath()}/${FirestorePaths.CAFES}/$cafeId",
                collectionId = FirestorePaths.CAFE_NOTICES,
                idToken = idToken
            )
        }.recoverCatching {
            loadSubCollectionDocumentCount(
                parentPath = "${config.documentBasePath()}/${FirestorePaths.CAFES}/$cafeId",
                collectionId = FirestorePaths.CAFE_NOTICES,
                idToken = null
            )
        }.getOrThrow()
    }

    override suspend fun refreshCafeManagementData(userId: String) {
        fetchOwnedCafeIdsRemote(userId)
        fetchPendingCafeOwnerClaimsRemote(userId)
        fetchPendingCafeRegistrationClaimsRemote(userId)
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
        return preview.copy(approvedCafeId = newCafeId)
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

    private suspend fun loadHomeBanners(idToken: String?): List<HomeBanner> {
        val response = restApi.get("${config.documentBasePath()}/${FirestorePaths.HOME_BANNERS}", idToken)
        val parsed = Json.parseToJsonElement(response).jsonObject
        val documents = parsed["documents"]?.jsonArray.orEmpty()
        val banners = documents.mapNotNull { element ->
            parseHomeBannerDocument(element.jsonObject)
        }
        return banners.sortedByDescending { banner -> banner.createdAtEpochMillis }
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
        orderByDescending: Boolean = false,
        limit: Int? = null
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
        val limitSection = if (limit != null && limit > 0) {
            """
            ,
                "limit": ${limit}
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
                  }$orderBySection$limitSection
                }
            }
        """.trimIndent()
        val response = restApi.post(path = path, body = body, idToken = idToken)
        val parsed = Json.parseToJsonElement(response).jsonArray
        return parsed.mapNotNull { element ->
            element.jsonObject["document"]?.jsonObject
        }
    }

    private suspend fun runLegacyUserFieldQuery(
        collectionId: String,
        userId: String,
        idToken: String?,
        legacyFieldPath: String
    ): List<JsonObject> {
        return runCatching {
            runFieldScopedQuery(
                collectionId = collectionId,
                fieldPath = legacyFieldPath,
                fieldValue = userId,
                idToken = idToken,
                orderByCreatedAtDesc = false
            )
        }.recoverCatching {
            runFieldScopedQuery(
                collectionId = collectionId,
                fieldPath = legacyFieldPath,
                fieldValue = userId,
                idToken = null,
                orderByCreatedAtDesc = false
            )
        }.getOrElse { emptyList() }
    }

    private suspend fun runDocumentNamePrefixQuery(
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
                "from": [
                  { "collectionId": "$escapedCollectionId" }
                ],
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
        val response = runCatching {
            restApi.post(path = path, body = body, idToken = idToken)
        }.recoverCatching {
            restApi.post(path = path, body = body, idToken = null)
        }.getOrElse { return emptyList() }
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

    private fun <T> toPaged(items: List<T>, cursor: String?, pageSize: Int): PagedResult<T> {
        val safePageSize = if (pageSize > 0) pageSize else 1
        val startIndex = cursor?.toIntOrNull()?.coerceAtLeast(0) ?: 0
        val safeStartIndex = startIndex.coerceAtMost(items.size)
        val endIndex = (safeStartIndex + safePageSize).coerceAtMost(items.size)
        val pagedItems = items.subList(safeStartIndex, endIndex)
        val hasNext = endIndex < items.size
        val nextCursor = if (hasNext) endIndex.toString() else null
        return PagedResult(
            items = pagedItems,
            nextCursor = nextCursor,
            hasNext = hasNext
        )
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
        val safeLimit = if (limit > 0) limit else 1
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

    private suspend fun runCastByLinkedUserIdQuery(
        userId: String,
        idToken: String?
    ): List<JsonObject> {
        val path = "${config.documentBasePath()}:runQuery"
        val body = """
            {
              "structuredQuery": {
                "from": [
                  {
                    "collectionId": "${FirestorePaths.CAFE_CASTS}",
                    "allDescendants": true
                  }
                ],
                "where": {
                  "fieldFilter": {
                    "field": { "fieldPath": "linkedUserId" },
                    "op": "EQUAL",
                    "value": { "stringValue": "${escapeFirestoreQueryString(userId)}" }
                  }
                },
                "limit": 1
              }
            }
        """.trimIndent()
        val response = restApi.post(path = path, body = body, idToken = idToken)
        val parsed = Json.parseToJsonElement(response).jsonArray
        return parsed.mapNotNull { element ->
            element.jsonObject["document"]?.jsonObject
        }
    }

    private suspend fun runUserVisitPageQuery(
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
                "from": [
                  { "collectionId": "${FirestorePaths.VISITS}" }
                ],
                "where": {
                  "fieldFilter": {
                    "field": { "fieldPath": "userId" },
                    "op": "EQUAL",
                    "value": { "stringValue": "${escapeFirestoreQueryString(userId)}" }
                  }
                },
                "orderBy": [
                  {
                    "field": { "fieldPath": "visitedAt" },
                    "direction": "DESCENDING"
                  },
                  {
                    "field": { "fieldPath": "__name__" },
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

    private suspend fun runCastByUserFieldQuery(
        userId: String,
        idToken: String?,
        fieldPath: String
    ): List<JsonObject> {
        val path = "${config.documentBasePath()}:runQuery"
        val body = """
            {
              "structuredQuery": {
                "from": [
                  {
                    "collectionId": "${FirestorePaths.CAFE_CASTS}",
                    "allDescendants": true
                  }
                ],
                "where": {
                  "fieldFilter": {
                    "field": { "fieldPath": "${escapeFirestoreQueryString(fieldPath)}" },
                    "op": "EQUAL",
                    "value": { "stringValue": "${escapeFirestoreQueryString(userId)}" }
                  }
                },
                "limit": 1
              }
            }
        """.trimIndent()
        val response = restApi.post(path = path, body = body, idToken = idToken)
        val parsed = Json.parseToJsonElement(response).jsonArray
        return parsed.mapNotNull { element ->
            element.jsonObject["document"]?.jsonObject
        }
    }

    private suspend fun resolveCastDocumentsByUser(
        userId: String,
        idToken: String?
    ): List<JsonObject> {
        val byLinkedUserId = runCatching {
            runCastByLinkedUserIdQuery(userId = userId, idToken = idToken)
        }.recoverCatching {
            runCastByLinkedUserIdQuery(userId = userId, idToken = null)
        }.getOrElse {
            emptyList()
        }
        if (byLinkedUserId.isNotEmpty()) {
            return byLinkedUserId
        }

        val byUserIdField = runCatching {
            runCastByUserFieldQuery(userId = userId, idToken = idToken, fieldPath = "userId")
        }.recoverCatching {
            runCastByUserFieldQuery(userId = userId, idToken = null, fieldPath = "userId")
        }.getOrElse {
            emptyList()
        }
        if (byUserIdField.isNotEmpty()) {
            return byUserIdField
        }

        val byUidField = runCatching {
            runCastByUserFieldQuery(userId = userId, idToken = idToken, fieldPath = "uid")
        }.recoverCatching {
            runCastByUserFieldQuery(userId = userId, idToken = null, fieldPath = "uid")
        }.getOrElse {
            emptyList()
        }
        if (byUidField.isNotEmpty()) {
            return byUidField
        }

        val affiliatedCafeId = runCatching {
            fetchAffiliatedCafeIdRemote(userId)
        }.getOrNull()
        if (affiliatedCafeId.isNullOrBlank()) {
            return emptyList()
        }
        val cafeCastDocuments = runCatching {
            runCafeCastQuery(
                cafeId = affiliatedCafeId,
                cursor = null,
                limit = 200,
                idToken = idToken
            )
        }.recoverCatching {
            runCafeCastQuery(
                cafeId = affiliatedCafeId,
                cursor = null,
                limit = 200,
                idToken = null
            )
        }.getOrElse {
            emptyList()
        }
        return cafeCastDocuments.filter { document ->
            val fields = document["fields"]?.jsonObject ?: return@filter false
            val linked = fields.getFirestoreString("linkedUserId")
            val legacyUserId = fields.getFirestoreString("userId")
            val legacyUid = fields.getFirestoreString("uid")
            linked == userId || legacyUserId == userId || legacyUid == userId
        }
    }

    private suspend fun runCastByBirthdayKeyQuery(
        birthdayKey: String,
        idToken: String?,
        limit: Int
    ): List<JsonObject> {
        val safeLimit = if (limit > 0) limit else 1
        val path = "${config.documentBasePath()}:runQuery"
        val body = """
            {
              "structuredQuery": {
                "from": [
                  {
                    "collectionId": "${FirestorePaths.CAFE_CASTS}",
                    "allDescendants": true
                  }
                ],
                "where": {
                  "fieldFilter": {
                    "field": { "fieldPath": "$BIRTHDAY_KEY_FIELD" },
                    "op": "EQUAL",
                    "value": { "stringValue": "${escapeFirestoreQueryString(birthdayKey)}" }
                  }
                },
                "orderBy": [
                  {
                    "field": { "fieldPath": "name" },
                    "direction": "ASCENDING"
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

    private suspend fun runCastCollectionGroupQuery(
        sort: CastSort,
        cursor: String?,
        limit: Int,
        idToken: String?
    ): List<JsonObject> {
        val safeLimit = if (limit > 0) limit else 1
        val isLatestSort = sort == CastSort.LATEST
        val startAfterSection = cursor.toCastStartAfterSection(sort)
        val orderBySection = if (isLatestSort) {
            """
                "orderBy": [
                  {
                    "field": { "fieldPath": "__name__" },
                    "direction": "DESCENDING"
                  }
                ]"""
        } else {
            """
                "orderBy": [
                  {
                    "field": { "fieldPath": "followerCount" },
                    "direction": "DESCENDING"
                  },
                  {
                    "field": { "fieldPath": "__name__" },
                    "direction": "ASCENDING"
                  }
                ]"""
        }
        val path = "${config.documentBasePath()}:runQuery"
        val body = """
            {
              "structuredQuery": {
                "from": [
                  {
                    "collectionId": "${FirestorePaths.CAFE_CASTS}",
                    "allDescendants": true
                  }
                ],
                $orderBySection$startAfterSection,
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

    private suspend fun runCafeCollectionQuery(
        sort: CafeSort,
        cursor: String?,
        limit: Int,
        query: String?,
        country: String?,
        city: String?,
        idToken: String?
    ): List<JsonObject> {
        val safeLimit = if (limit > 0) limit else 1
        val isLatestSort = sort == CafeSort.LATEST
        val orderFieldPath = if (isLatestSort) {
            "__name__"
        } else if (sort == CafeSort.POPULAR) {
            "reviewCount"
        } else {
            "ratingAvg"
        }
        val filters = mutableListOf<String>()

        if (!country.isNullOrBlank()) {
            filters.add(
                """
                {
                  "fieldFilter": {
                    "field": { "fieldPath": "region.country" },
                    "op": "EQUAL",
                    "value": { "stringValue": "${escapeFirestoreQueryString(country)}" }
                  }
                }
                """.trimIndent()
            )
        }
        if (!city.isNullOrBlank()) {
            filters.add(
                """
                {
                  "fieldFilter": {
                    "field": { "fieldPath": "region.city" },
                    "op": "EQUAL",
                    "value": { "stringValue": "${escapeFirestoreQueryString(city)}" }
                  }
                }
                """.trimIndent()
            )
        }
        val whereSection = when (filters.size) {
            0 -> ""
            1 -> """,
                "where": ${filters.first()}"""
            else -> """,
                "where": {
                  "compositeFilter": {
                    "op": "AND",
                    "filters": [
                      ${filters.joinToString(",\n                      ")}
                    ]
                  }
                }"""
        }
        val startAfterSection = cursor.toStartAfterSection(latestSort = isLatestSort)
        val path = "${config.documentBasePath()}:runQuery"
        val body = """
            {
              "structuredQuery": {
                "from": [
                  {
                    "collectionId": "${FirestorePaths.CAFES}"
                  }
                ],
                "orderBy": [
                  {
                    "field": { "fieldPath": "$orderFieldPath" },
                    "direction": "DESCENDING"
                  }${if (!isLatestSort) """,
                  {
                    "field": { "fieldPath": "__name__" },
                    "direction": "ASCENDING"
                  }""" else ""}
                ]$whereSection$startAfterSection,
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

    private suspend fun runCafeCastQuery(
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
                "from": [
                  {
                    "collectionId": "${FirestorePaths.CAFE_CASTS}"
                  }
                ],
                "orderBy": [
                  {
                    "field": { "fieldPath": "name" },
                    "direction": "ASCENDING"
                  },
                  {
                    "field": { "fieldPath": "__name__" },
                    "direction": "ASCENDING"
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

    private suspend fun runCafeReviewPageQuery(
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
                "from": [
                  {
                    "collectionId": "${FirestorePaths.REVIEWS}"
                  }
                ],
                "where": {
                  "fieldFilter": {
                    "field": { "fieldPath": "cafeId" },
                    "op": "EQUAL",
                    "value": { "stringValue": "${escapeFirestoreQueryString(cafeId)}" }
                  }
                },
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

    private suspend fun runCafeNoticePageQuery(
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
                "from": [
                  {
                    "collectionId": "${FirestorePaths.CAFE_NOTICES}"
                  }
                ],
                "orderBy": [
                  {
                    "field": { "fieldPath": "createdAt" },
                    "direction": "DESCENDING"
                  },
                  {
                    "field": { "fieldPath": "__name__" },
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

    private suspend fun runCafeCastIdsInQuery(
        cafeId: String,
        castIds: List<String>,
        idToken: String?
    ): List<JsonObject> {
        if (castIds.isEmpty()) {
            return emptyList()
        }
        val references = castIds.joinToString(",") { castId ->
            val sanitizedCastId = castId.trim()
            """
            { "referenceValue": "${config.documentBasePath()}/${FirestorePaths.CAFES}/${escapeFirestoreQueryString(cafeId)}/${FirestorePaths.CAFE_CASTS}/${escapeFirestoreQueryString(sanitizedCastId)}" }
            """.trimIndent()
        }
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFES}/$cafeId:runQuery"
        val body = """
            {
              "structuredQuery": {
                "from": [
                  {
                    "collectionId": "${FirestorePaths.CAFE_CASTS}"
                  }
                ],
                "where": {
                  "fieldFilter": {
                    "field": { "fieldPath": "__name__" },
                    "op": "IN",
                    "value": {
                      "arrayValue": {
                        "values": [ $references ]
                      }
                    }
                  }
                },
                "limit": ${castIds.size}
              }
            }
        """.trimIndent()
        val response = restApi.post(path = path, body = body, idToken = idToken)
        val parsed = Json.parseToJsonElement(response).jsonArray
        return parsed.mapNotNull { element ->
            element.jsonObject["document"]?.jsonObject
        }
    }

    private suspend fun runInquiryPageQuery(
        cursor: String?,
        limit: Int,
        idToken: String?
    ): List<JsonObject> {
        val safeLimit = if (limit > 0) limit else 1
        val startAfterSection = cursor.toStringFieldStartAfterSection()
        val path = "${config.documentBasePath()}:runQuery"
        val body = """
            {
              "structuredQuery": {
                "from": [
                  {
                    "collectionId": "${FirestorePaths.INQUIRIES}"
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

    private suspend fun runCafeEventPageQuery(
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
                "from": [
                  {
                    "collectionId": "${FirestorePaths.CAFE_EVENTS}"
                  }
                ],
                "orderBy": [
                  {
                    "field": { "fieldPath": "startDate" },
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

    private suspend fun runRecentNoticeFeedQuery(
        limit: Int,
        idToken: String?
    ): List<JsonObject> {
        val safeLimit = if (limit > 0) limit else 1
        val path = "${config.documentBasePath()}:runQuery"
        val body = """
            {
              "structuredQuery": {
                "from": [
                  {
                    "collectionId": "${FirestorePaths.CAFE_NOTICES}",
                    "allDescendants": true
                  }
                ],
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

    private suspend fun runCastWithBirthdayFieldQuery(idToken: String?): List<JsonObject> {
        val path = "${config.documentBasePath()}:runQuery"
        val body = """
            {
              "structuredQuery": {
                "from": [
                  {
                    "collectionId": "${FirestorePaths.CAFE_CASTS}",
                    "allDescendants": true
                  }
                ],
                "where": {
                  "fieldFilter": {
                    "field": { "fieldPath": "birthday" },
                    "op": "GREATER_THAN",
                    "value": { "stringValue": "0000-00-00" }
                  }
                },
                "orderBy": [
                  {
                    "field": { "fieldPath": "birthday" },
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

    private suspend fun runCastFollowerQuery(
        castId: String,
        idToken: String?,
        orderByCreatedAtDesc: Boolean,
        limit: Int
    ): List<JsonObject> {
        val safeLimit = limit.coerceAtLeast(1)
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
                  { "collectionId": "${FirestorePaths.CAST_FOLLOWS}" }
                ],
                "where": {
                  "fieldFilter": {
                    "field": { "fieldPath": "castId" },
                    "op": "EQUAL",
                    "value": { "stringValue": "${escapeFirestoreQueryString(castId)}" }
                  }
                }$orderBySection,
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
        val path = "${config.documentBasePath()}/${FirestorePaths.REVIEWS}/$reviewId"
        val document = runCatching {
            Json.parseToJsonElement(restApi.get(path, idToken)).jsonObject
        }.recoverCatching {
            Json.parseToJsonElement(restApi.get(path, null)).jsonObject
        }.getOrNull()
        return document?.let { parseReviewDocument(it) }
    }

    private suspend fun resolveCastClaimById(claimId: String, idToken: String?): CastClaim? {
        val path = "${config.documentBasePath()}/${FirestorePaths.CAST_CLAIMS}/$claimId"
        val document = try {
            Json.parseToJsonElement(restApi.get(path, idToken)).jsonObject
        } catch (throwable: Throwable) {
            if (throwable.isFirestoreNotFound()) {
                return null
            } else {
                throw throwable
            }
        }
        return parseCastClaimDocument(document)
    }

    private suspend fun resolveCafeCastById(cafeId: String, castId: String, idToken: String?): Cast? {
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFES}/$cafeId/${FirestorePaths.CAFE_CASTS}/$castId"
        val document = runCatching {
            Json.parseToJsonElement(restApi.get(path, idToken)).jsonObject
        }.recoverCatching {
            Json.parseToJsonElement(restApi.get(path, null)).jsonObject
        }.getOrNull() ?: return null
        return parseCastDocument(cafeId = cafeId, document = document)
    }

    private suspend fun resolveVisitById(visitId: String, idToken: String?): Visit? {
        val path = "${config.documentBasePath()}/${FirestorePaths.VISITS}/$visitId"
        val document = runCatching {
            Json.parseToJsonElement(restApi.get(path, idToken)).jsonObject
        }.recoverCatching {
            Json.parseToJsonElement(restApi.get(path, null)).jsonObject
        }.getOrNull()
        return document?.let { value -> parseVisitDocument(value) }
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

    private suspend fun loadSubCollectionDocumentCount(
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

    private suspend fun resolveCafeIdByCastId(
        castId: String,
        idToken: String?
    ): String? {
        val indexedCafeId = loadCastDirectoryCafeId(
            castId = castId,
            idToken = idToken
        )
        if (!indexedCafeId.isNullOrBlank()) {
            return indexedCafeId
        }
        val tokenUserId = tokenProvider.getCurrentUserId()
        val selfCastDocuments = if (tokenUserId.isNullOrBlank()) {
            emptyList()
        } else {
            resolveCastDocumentsByUser(
                userId = tokenUserId,
                idToken = idToken
            )
        }
        val selfMatchedCafeId = selfCastDocuments.firstNotNullOfOrNull { document ->
            val name = document["name"]?.jsonPrimitive?.contentOrNull
            if (name.isNullOrBlank()) {
                null
            } else {
                if (name.endsWith("/$castId")) {
                    extractCafeIdFromCastDocumentName(name)
                } else {
                    null
                }
            }
        }
        if (selfMatchedCafeId != null) {
            println(
                "TEST, resolveCafeIdByCastId hit self-cast mapping: " +
                    "castId=$castId userId=$tokenUserId cafeId=$selfMatchedCafeId"
            )
            return selfMatchedCafeId
        }

        val affiliatedCafeId = if (tokenUserId.isNullOrBlank()) {
            null
        } else {
            runCatching {
                fetchAffiliatedCafeIdRemote(tokenUserId)
            }.getOrNull()
        }
        val affiliatedMatchedCast = if (affiliatedCafeId.isNullOrBlank()) {
            null
        } else {
            resolveCafeCastById(
                cafeId = affiliatedCafeId,
                castId = castId,
                idToken = idToken
            )
        }
        if (affiliatedMatchedCast != null && !affiliatedCafeId.isNullOrBlank()) {
            println(
                "TEST, resolveCafeIdByCastId hit affiliated-cafe mapping: " +
                    "castId=$castId userId=$tokenUserId cafeId=$affiliatedCafeId"
            )
            return affiliatedCafeId
        }
        return null
    }

    private suspend fun loadCastDirectoryCafeId(
        castId: String,
        idToken: String?
    ): String? {
        val path = "${config.documentBasePath()}/${FirestorePaths.CAST_DIRECTORY}/$castId"
        val response = runCatching {
            restApi.get(path, idToken)
        }.recoverCatching {
            restApi.get(path, null)
        }.getOrNull() ?: return null
        val document = runCatching {
            Json.parseToJsonElement(response).jsonObject
        }.getOrNull() ?: return null
        return document["fields"]
            ?.jsonObject
            ?.getFirestoreString("cafeId")
            ?.takeIf { cafeId -> cafeId.isNotBlank() }
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

    private fun parseCollectionGroupCastDocuments(documents: List<JsonObject>): List<Cast> {
        return documents.mapNotNull { document ->
            val documentName = document["name"]?.jsonPrimitive?.contentOrNull
            val fields = document["fields"]?.jsonObject
            val cafeId = documentName?.toCafeIdFromCastDocumentName()
                ?: fields?.getFirestoreString("cafeId")
                ?: ""

            parseCastDocument(cafeId = cafeId, document = document)
        }
    }

    private suspend fun loadCafeIdsByRegionRemote(
        country: String?,
        city: String?,
        idToken: String?
    ): Set<String>? {
        if (country.isNullOrBlank() && city.isNullOrBlank()) {
            return null
        }
        val documents = runCatching {
            runCafeCollectionQuery(
                sort = CafeSort.LATEST,
                cursor = null,
                limit = REGION_FILTER_CAFE_CACHE_LIMIT,
                query = null,
                country = country,
                city = city,
                idToken = idToken
            )
        }.recoverCatching {
            runCafeCollectionQuery(
                sort = CafeSort.LATEST,
                cursor = null,
                limit = REGION_FILTER_CAFE_CACHE_LIMIT,
                query = null,
                country = country,
                city = city,
                idToken = null
            )
        }.getOrElse {
            emptyList()
        }
        return documents.mapNotNull { document ->
            parseCafeDocument(document)?.id
        }.toSet()
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
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFE_REGISTRATION_CLAIMS}/$claimId" +
            "?updateMask.fieldPaths=status" +
            "&updateMask.fieldPaths=message" +
            "&updateMask.fieldPaths=reviewedBy" +
            "&updateMask.fieldPaths=reviewedAt" +
            "&updateMask.fieldPaths=approvedCafeId"
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
        val path = "${config.documentBasePath()}/${FirestorePaths.CAFE_OWNER_CLAIMS}/$claimId" +
            "?updateMask.fieldPaths=status" +
            "&updateMask.fieldPaths=message" +
            "&updateMask.fieldPaths=reviewedBy" +
            "&updateMask.fieldPaths=reviewedAt"
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
        return
    }

    private fun applyApprovedOwnerClaimToCache(preview: PendingCafeOwnerClaimPreview) {
        return
    }

    private fun applyRejectedOwnerClaimToCache(preview: PendingCafeOwnerClaimPreview) {
        return
    }

    private fun applyRejectedRegistrationClaimToCache(
        preview: PendingCafeRegistrationClaimPreview,
        claimId: String
    ) {
        return
    }

    private suspend fun loadRankingItemsRemote(
        kind: String,
        period: RankingPeriod,
        country: String?,
        city: String?
    ): List<RankingItem> {
        val rankingId = rankingDocumentId(
            kind = kind,
            period = period,
            country = country,
            city = city
        )
        val path = "${config.documentBasePath()}/${FirestorePaths.RANKINGS}/$rankingId"
        val response = runCatching {
            restApi.get(path, null)
        }.getOrElse { throwable ->
            if (throwable.isFirestoreNotFound()) {
                return emptyList()
            }
            throw IllegalStateException("failed to load ranking document: $rankingId", throwable)
        }
        val document = Json.parseToJsonElement(response).jsonObject
        val rankingItems = parseRankingItemsDocument(document)
            .sortedBy { item -> item.rank }
        return rankingItems
    }

    private fun parseRankingItemsDocument(document: JsonObject): List<RankingItem> {
        val fields = document["fields"]?.jsonObject ?: return emptyList()
        val entryValues = fields["entries"]
            ?.jsonObject
            ?.get("arrayValue")
            ?.jsonObject
            ?.get("values")
            ?.jsonArray
            .orEmpty()
        return entryValues.mapNotNull { element ->
            val entryFields = element
                .jsonObject["mapValue"]
                ?.jsonObject
                ?.get("fields")
                ?.jsonObject
                ?: return@mapNotNull null
            val id = entryFields.getFirestoreString("id")
                ?: return@mapNotNull null
            val name = entryFields.getFirestoreString("name").orEmpty()
            val subtitle = entryFields.getFirestoreString("subtitle").orEmpty()
            val score = entryFields.getFirestoreLong("score")?.toInt()
                ?: entryFields.getFirestoreDouble("score")?.toInt()
                ?: 0
            val rank = entryFields.getFirestoreLong("rank")?.toInt()
                ?: entryFields.getFirestoreDouble("rank")?.toInt()
                ?: 0
            val change = entryFields.getFirestoreString("change") ?: "0"
            val imageUrl = entryFields.getFirestoreString("imageUrl")
            return@mapNotNull RankingItem(
                id = id,
                name = name,
                subtitle = subtitle,
                score = score,
                rank = rank,
                change = change,
                imageUrl = imageUrl
            )
        }
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
            images = images.ifEmpty { null },
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
            reservationUrl = fields.getFirestoreString("reservationUrl")
        )
    }

    private fun parseCastDocument(cafeId: String, document: JsonObject): Cast? {
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

    private fun parseInquiryDocument(document: JsonObject): Inquiry? {
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

    private fun parseNotificationDocument(userId: String, document: JsonObject): AppNotification? {
        val fields = document["fields"]?.jsonObject ?: return null
        val name = document["name"]?.jsonPrimitive?.contentOrNull ?: return null
        val notificationId = name.substringAfterLast("/")
        val createdAt = fields.getFirestoreString("createdAt").orEmpty()
        val type = fields.getFirestoreString("type").orEmpty()
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
            type = type,
            targetId = fields.getFirestoreString("targetId"),
            isRead = fields.getFirestoreBoolean("isRead") ?: false,
            createdAt = createdAt,
            relativeTime = relativeTime
        )
    }

    private fun parseNotificationSettingsDocument(document: JsonObject): UserNotificationSettings {
        val fields = document["fields"]?.jsonObject

        if (fields == null) {
            return UserNotificationSettings.default()
        } else {
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
                quietHoursMode = quietHoursMode
            )
        }
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

    private fun parseCastClaimDocument(document: JsonObject): CastClaim? {
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
        val date = normalizeScheduleDateOrNull(fields.getFirestoreString("date")) ?: return null
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
            date = date,
            status = status,
            startTime = startTime,
            endTime = endTime
        )
    }

    private fun parseWorkingCastScheduleDocument(document: JsonObject): CastSchedule? {
        val fields = document["fields"]?.jsonObject ?: return null
        val entry = parseCastScheduleDocument(document) ?: return null
        if (entry.status != CastScheduleStatus.WORK) {
            return null
        }
        val startTime = entry.startTime?.trim().orEmpty()
        val endTime = entry.endTime?.trim().orEmpty()
        if (startTime.isEmpty() || endTime.isEmpty()) {
            return null
        }
        val cafeId = fields.getFirestoreString("cafeId").orEmpty()
        if (cafeId.isBlank()) {
            return null
        }
        return CastSchedule(
            id = entry.id,
            castId = entry.castId,
            cafeId = cafeId,
            date = entry.date,
            startTime = startTime,
            endTime = endTime
        )
    }

    private fun parseCastFollowDocument(document: JsonObject): CastFollowerSnapshot? {
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

    private fun parsePendingCafeOwnerClaimDocument(document: JsonObject): CafeManagementData.PendingClaimSummary? {
        val fields = document["fields"]?.jsonObject ?: return null
        val name = document["name"]?.jsonPrimitive?.contentOrNull ?: return null
        val claimId = name.substringAfterLast("/")
        val cafeId = fields.getFirestoreString("cafeId").orEmpty()
        val resolvedCafeName = fields.getFirestoreString("cafeName")
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
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        val cafe = runCatching {
            parseCafeDocument(loadCafeDocument(cafeId = cafeId, idToken = idToken))
        }.recoverCatching {
            parseCafeDocument(loadCafeDocument(cafeId = cafeId, idToken = null))
        }.getOrNull()
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
            approvedCafeId = fields.getFirestoreString("approvedCafeId"),
            cafeName = fields.getFirestoreString("cafeName").orEmpty(),
            location = location,
            requestedAt = requestedAt,
            message = message,
            imageUrl = fields.getFirestoreString("thumbnailImage")
        )
    }

    private suspend fun resolveUserNickname(userId: String): String {
        val remoteUser = fetchUser(userId)

        if (remoteUser != null) {
            return remoteUser.nickname
        }
        return "알 수 없음"
    }

    private suspend fun fetchCastSchedulesRemoteInternal(
        castId: String,
        fromDate: String,
        toDate: String,
        idToken: String?
    ): List<CastSchedule> {
        val documents = runCatching {
            runCastScheduleRangeQuery(
                castId = castId,
                fromDate = fromDate,
                toDate = toDate,
                idToken = idToken
            )
        }.recoverCatching {
            runCastScheduleRangeQuery(
                castId = castId,
                fromDate = fromDate,
                toDate = toDate,
                idToken = null
            )
        }.getOrElse { emptyList() }

        return documents.mapNotNull { document ->
            val fields = document["fields"]?.jsonObject ?: return@mapNotNull null
            val entry = parseCastScheduleDocument(document) ?: return@mapNotNull null
            val cafeId = fields.getFirestoreString("cafeId").orEmpty()

            if (entry.status == CastScheduleStatus.WORK) {
                val startTime = entry.startTime?.trim().orEmpty()
                val endTime = entry.endTime?.trim().orEmpty()

                if (startTime.isNotEmpty() && endTime.isNotEmpty()) {
                    CastSchedule(
                        id = entry.id,
                        castId = entry.castId,
                        cafeId = cafeId,
                        date = entry.date,
                        startTime = startTime,
                        endTime = endTime
                    )
                } else {
                    null
                }
            } else {
                null
            }
        }.sortedBy { schedule -> schedule.date }
    }

    private suspend fun fetchCafeDetailSummaryRemoteInternal(cafeId: String): CafeDetail? {
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        val cafeDocument = runCatching {
            loadCafeDocument(cafeId = cafeId, idToken = idToken)
        }.recoverCatching {
            loadCafeDocument(cafeId = cafeId, idToken = null)
        }.getOrNull() ?: return null
        val cafe = parseCafeDocument(cafeDocument) ?: return null
        val detailMetadata = parseCafeDetailMetadata(cafeDocument)
        val castsDocuments = runCatching {
            loadCafeSubCollectionDocuments(cafeId, FirestorePaths.CAFE_CASTS, idToken)
        }.recoverCatching {
            loadCafeSubCollectionDocuments(cafeId, FirestorePaths.CAFE_CASTS, null)
        }.getOrElse { emptyList() }
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

    private suspend fun fetchCafeDetailFullRemoteInternal(cafeId: String): CafeDetail? {
        val summary = fetchCafeDetailSummaryRemoteInternal(cafeId) ?: return null
        val menuGoods = fetchCafeMenuGoodsRemoteInternal(cafeId)
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        val noticesDocuments = runCatching {
            loadCafeSubCollectionDocuments(cafeId, FirestorePaths.CAFE_NOTICES, idToken)
        }.recoverCatching {
            loadCafeSubCollectionDocuments(cafeId, FirestorePaths.CAFE_NOTICES, null)
        }.getOrElse { emptyList() }
        val parsedNotices = noticesDocuments
            .mapNotNull { document ->
                parseNoticeDocument(
                    cafeId = cafeId,
                    cafeName = summary.cafe.name,
                    document = document
                )
            }
            .sortedByDescending { notice -> notice.createdAt }
        return summary.copy(
            menus = menuGoods?.menus.orEmpty(),
            goods = menuGoods?.goods.orEmpty(),
            notices = parsedNotices
        )
    }

    private suspend fun fetchCafeMenuGoodsRemoteInternal(cafeId: String): CafeMenuGoodsSection? {
        fetchCafeByIdRemote(cafeId) ?: return null
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
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
        return CafeMenuGoodsSection(
            menus = parsedMenus,
            goods = parsedGoods
        )
    }

    private suspend fun fetchCastDetailRemoteById(castId: String): CastDetail? {
        val idToken = runCatching { tokenProvider.getIdToken() }.getOrNull()
        val cafeId = resolveCafeIdByCastId(castId = castId, idToken = idToken) ?: return null
        return fetchCastDetailRemoteByCafeAndCastId(
            cafeId = cafeId,
            castId = castId,
            idToken = idToken
        )
    }

    private suspend fun fetchCastDetailRemoteByCafeAndCastId(
        cafeId: String,
        castId: String,
        idToken: String?
    ): CastDetail? {
        val cafeDocument = runCatching {
            loadCafeDocument(cafeId = cafeId, idToken = idToken)
        }.recoverCatching {
            loadCafeDocument(cafeId = cafeId, idToken = null)
        }.getOrNull() ?: return null
        val castDocument = runCatching {
            loadCafeCastDocument(cafeId = cafeId, castId = castId, idToken = idToken)
        }.recoverCatching {
            loadCafeCastDocument(cafeId = cafeId, castId = castId, idToken = null)
        }.getOrNull() ?: return null
        val cafe = parseCafeDocument(cafeDocument) ?: return null
        val cast = parseCastDocument(cafeId = cafeId, document = castDocument) ?: return null
        val castFields = castDocument["fields"]?.jsonObject
        val galleryImages = castFields
            ?.getFirestoreStringList("galleryImages")
            ?.filter { image -> image.isNotBlank() }
            .orEmpty()
        val images = buildList {
            cast.profileImage?.takeIf { image -> image.isNotBlank() }?.let { image -> add(image) }
            addAll(galleryImages.filterNot { image -> image == cast.profileImage })
        }
        val currentDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val fromDate = (currentDate + DatePeriod(days = -30)).toString()
        val toDate = (currentDate + DatePeriod(days = 60)).toString()
        val schedules = fetchCastSchedulesRemoteInternal(
            castId = castId,
            fromDate = fromDate,
            toDate = toDate,
            idToken = idToken
        )
        return CastDetail(
            cast = cast,
            cafe = cafe,
            images = images,
            schedule = schedules,
            visitCertificationCount = cast.visitCertificationCount
        )
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

    private suspend fun resolveMyPageVisitCount(
        userId: String,
        fallbackVisitCount: Int,
        idToken: String?
    ): Int {
        val aggregatedVisitCount = runCatching {
            loadCollectionDocumentCount(
                collectionId = FirestorePaths.VISITS,
                idToken = idToken,
                equalsFilterFieldPath = "userId",
                equalsFilterValue = firestoreString(userId)
            )
        }.getOrNull()
        return if (aggregatedVisitCount != null) {
            aggregatedVisitCount
        } else {
            fallbackVisitCount
        }
    }

    private suspend fun resolveMyPageStampCount(
        userId: String,
        fallbackStampCount: Int,
        idToken: String?
    ): Int {
        val aggregatedStampCount = runCatching {
            loadCollectionDocumentCount(
                collectionId = FirestorePaths.STAMPS,
                idToken = idToken,
                equalsFilterFieldPath = "userId",
                equalsFilterValue = firestoreString(userId)
            )
        }.getOrNull()
        return if (aggregatedStampCount != null) {
            aggregatedStampCount
        } else {
            fallbackStampCount
        }
    }

    private suspend fun fetchVisitsByCafeRemote(cafeId: String): List<Visit> {
        val idToken = runCatching {
            tokenProvider.getIdToken()
        }.getOrNull()
        val visitDocuments = runCatching {
            runFieldScopedQuery(
                collectionId = FirestorePaths.VISITS,
                fieldPath = "cafeId",
                fieldValue = cafeId,
                idToken = idToken,
                orderByCreatedAtDesc = false
            )
        }.recoverCatching {
            runFieldScopedQuery(
                collectionId = FirestorePaths.VISITS,
                fieldPath = "cafeId",
                fieldValue = cafeId,
                idToken = null,
                orderByCreatedAtDesc = false
            )
        }.getOrElse { emptyList() }
        return visitDocuments.mapNotNull { document ->
            parseVisitDocument(document)
        }
    }

    private fun ensureAuthenticatedUserMatch(requestedUserId: String, action: String) {
        val tokenUserId = tokenProvider.getCurrentUserId()

        if (!tokenUserId.isNullOrBlank() && tokenUserId != requestedUserId) {
            throw IllegalStateException(
                "Auth user mismatch in $action: tokenUserId=$tokenUserId requestedUserId=$requestedUserId"
            )
        }
    }

    private fun sanitizeDocumentIdPart(value: String): String {
        return value.replace("/", "_")
    }

    private suspend fun loadUserIdListFromProfileDocument(
        userId: String,
        idToken: String?,
        candidates: List<String>
    ): List<String> {
        val userDocument = runCatching {
            loadUserDocument(userId = userId, idToken = idToken)
        }.recoverCatching {
            loadUserDocument(userId = userId, idToken = null)
        }.getOrNull() ?: return emptyList()
        val fields = userDocument["fields"]?.jsonObject ?: return emptyList()
        val stats = fields.getFirestoreMap("stats")

        candidates.forEach { fieldName ->
            val fromRoot = fields.getFirestoreStringList(fieldName)
                .map { value -> value.trim() }
                .filter { value -> value.isNotEmpty() }
            if (fromRoot.isNotEmpty()) {
                return fromRoot
            }
            val fromStats = stats?.getFirestoreStringList(fieldName)
                .orEmpty()
                .map { value -> value.trim() }
                .filter { value -> value.isNotEmpty() }
            if (fromStats.isNotEmpty()) {
                return fromStats
            }
        }
        return emptyList()
    }

    private fun parseStampDocument(document: JsonObject): Stamp? {
        val name = document["name"]?.jsonPrimitive?.content ?: return null
        val fields = document["fields"]?.jsonObject ?: return null
        val stampId = name.substringAfterLast("/")
        val userId = fields.getFirestoreString("userId").orEmpty()
        val cafeId = fields.getFirestoreString("cafeId").orEmpty()
        val visitId = fields.getFirestoreString("visitId").orEmpty()
        val earnedAt = fields.getFirestoreString("earnedAt").orEmpty()
        return if (stampId.isBlank() || userId.isBlank() || cafeId.isBlank() || visitId.isBlank() || earnedAt.isBlank()) {
            null
        } else {
            Stamp(
                id = stampId,
                userId = userId,
                cafeId = cafeId,
                visitId = visitId,
                earnedAt = earnedAt
            )
        }
    }

    private suspend fun resolveCafeNameForNoticeWrite(cafeId: String, idToken: String?): String {
        val remoteName = runCatching {
            parseCafeDocument(loadCafeDocument(cafeId = cafeId, idToken = idToken))
                ?.name
                ?.trim()
                ?.takeIf { value -> value.isNotEmpty() }
        }.recoverCatching {
            parseCafeDocument(loadCafeDocument(cafeId = cafeId, idToken = null))
                ?.name
                ?.trim()
                ?.takeIf { value -> value.isNotEmpty() }
        }.getOrNull()
        return remoteName ?: cafeId
    }

    private companion object {
        const val DEFAULT_OWNER_CLAIM_STATUS = "승인 대기 중"
        const val DEFAULT_REGISTRATION_CLAIM_STATUS = "승인 대기 중"
        const val RECENT_FOLLOWER_FETCH_LIMIT = 30
        const val BIRTHDAY_KEY_FIELD = "birthdayKey"
        val FAVORITE_CAFE_ID_FIELD_CANDIDATES = listOf(
            "favoriteCafeIds",
            "favorites",
            "favoriteCafes"
        )
        val FOLLOWED_CAST_ID_FIELD_CANDIDATES = listOf(
            "followedCastIds",
            "followingCastIds",
            "followCastIds"
        )
        val RECENT_VISIT_CAFE_ID_FIELD_CANDIDATES = listOf(
            "recentVisitCafeIds",
            "recentVisitedCafeIds",
            "visitedCafeIds"
        )
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
            is kotlinx.serialization.json.JsonArray -> parsedElement.firstNotNullOfOrNull { item ->
                extractCountFromAggregationItem(
                    item.jsonObject
                )
            }
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

private fun extractCafeIdFromCastDocumentName(name: String): String? {
    val regex = Regex(""".*/cafes/([^/]+)/casts/[^/]+$""")
    val match = regex.matchEntire(name) ?: return null
    return match.groupValues.getOrNull(1)
}

private fun normalizeScheduleDateOrNull(raw: String?): String? {
    val candidate = raw?.trim().orEmpty()
    val regex = Regex("""(\d{4})[-./](\d{2})[-./](\d{2})""")
    val match = regex.find(candidate) ?: return null
    val year = match.groupValues.getOrNull(1) ?: return null
    val month = match.groupValues.getOrNull(2) ?: return null
    val day = match.groupValues.getOrNull(3) ?: return null
    return "$year-$month-$day"
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

private fun JsonObject.toCastQueryCursor(sort: CastSort): String? {
    val fields = this["fields"]?.jsonObject ?: return null
    return if (sort == CastSort.LATEST) {
        this["name"]?.jsonPrimitive?.contentOrNull
    } else {
        val followerScore = fields.getFirestoreLong("followerCount")?.toString()
            ?: fields.getFirestoreDouble("followerCount")?.toString()
            ?: "0"
        val docName = this["name"]?.jsonPrimitive?.contentOrNull ?: return null
        "$followerScore|$docName"
    }
}

private fun JsonObject.toCafeQueryCursor(sort: CafeSort): String? {
    val fields = this["fields"]?.jsonObject ?: return null
    return when (sort) {
        CafeSort.LATEST -> {
            this["name"]?.jsonPrimitive?.contentOrNull
        }
        CafeSort.POPULAR -> {
            val score = fields.getFirestoreLong("reviewCount")?.toString()
                ?: fields.getFirestoreDouble("reviewCount")?.toString()
                ?: "0"
            val docName = this["name"]?.jsonPrimitive?.contentOrNull ?: return null
            "$score|$docName"
        }
        else -> {
            val score = fields.getFirestoreDouble("ratingAvg")?.toString()
                ?: fields.getFirestoreLong("ratingAvg")?.toString()
                ?: "0"
            val docName = this["name"]?.jsonPrimitive?.contentOrNull ?: return null
            "$score|$docName"
        }
    }
}

private fun JsonObject.toCafeCastQueryCursor(): String? {
    return this["name"]?.jsonPrimitive?.contentOrNull
}

private fun JsonObject.toReviewQueryCursor(): String? {
    val fields = this["fields"]?.jsonObject ?: return null
    return fields.getFirestoreString("createdAt")
}

private fun JsonObject.toVisitQueryCursor(): String? {
    val documentName = this["name"]?.jsonPrimitive?.contentOrNull ?: return null
    val fields = this["fields"]?.jsonObject ?: return null
    val visitedAt = fields.getFirestoreString("visitedAt") ?: return null
    return "$visitedAt|$documentName"
}

private fun JsonObject.toNoticeQueryCursor(): String? {
    val documentName = this["name"]?.jsonPrimitive?.contentOrNull ?: return null
    val fields = this["fields"]?.jsonObject ?: return null
    val createdAt = fields.getFirestoreString("createdAt") ?: return null
    return "$createdAt|$documentName"
}

private fun JsonObject.toEventQueryCursor(): String? {
    val fields = this["fields"]?.jsonObject ?: return null
    return fields.getFirestoreString("startDate")
}

private fun JsonObject.toStringFieldCursor(fieldPath: String): String? {
    val fields = this["fields"]?.jsonObject ?: return null
    return fields.getFirestoreString(fieldPath)
}

private fun String?.toStartAfterSection(latestSort: Boolean): String {
    val cursorValue = this
    if (cursorValue.isNullOrBlank()) {
        return ""
    }
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
        val scoreValue = scoreRaw.toDoubleOrNull()

        if (scoreValue == null) {
            ""
        } else {
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

private fun String?.toCastStartAfterSection(sort: CastSort): String {
    val cursorValue = this
    if (cursorValue.isNullOrBlank()) {
        return ""
    }
    return if (sort == CastSort.LATEST) {
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
        val scoreValue = scoreRaw.toDoubleOrNull()
        if (scoreValue == null) {
            ""
        } else {
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

private fun String?.toCafeCastStartAfterSection(): String {
    val cursorValue = this
    if (cursorValue.isNullOrBlank()) {
        return ""
    }
    val escapedDocumentName = escapeFirestoreQueryString(cursorValue)
    return """,
                "startAt": {
                  "values": [
                    { "referenceValue": "$escapedDocumentName" }
                  ],
                  "before": false
                }"""
}

private fun String?.toReviewStartAfterSection(): String {
    val cursorValue = this
    if (cursorValue.isNullOrBlank()) {
        return ""
    }
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

private fun String?.toVisitStartAfterSection(): String {
    val cursorValue = this
    if (cursorValue.isNullOrBlank()) {
        return ""
    }
    val visitedAt = cursorValue.substringBefore("|")
    val documentName = cursorValue.substringAfter("|", missingDelimiterValue = "")
    if (visitedAt.isBlank() || documentName.isBlank()) {
        return ""
    }
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

private fun String?.toStringFieldStartAfterSection(): String {
    val cursorValue = this
    if (cursorValue.isNullOrBlank()) {
        return ""
    }
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

private fun String?.toNoticeStartAfterSection(): String {
    val cursorValue = this
    if (cursorValue.isNullOrBlank()) {
        return ""
    }
    val createdAt = cursorValue.substringBefore("|")
    val documentName = cursorValue.substringAfter("|", missingDelimiterValue = "")
    if (createdAt.isBlank() || documentName.isBlank()) {
        return ""
    }
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

private fun monthDayToBirthdayKey(month: Int, dayOfMonth: Int): String {
    val normalizedMonth = month.coerceIn(1, 12).toString().padStart(2, '0')
    val normalizedDay = dayOfMonth.coerceIn(1, 31).toString().padStart(2, '0')
    return "$normalizedMonth-$normalizedDay"
}

private fun haversineMeters(
    latitude1: Double,
    longitude1: Double,
    latitude2: Double,
    longitude2: Double
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

private fun String?.toBirthdayKeyOrNull(): String? {
    val monthDay = this?.toBirthMonthDayOrNull()

    if (monthDay == null) {
        return null
    }
    val month = monthDay.first
    val day = monthDay.second
    return monthDayToBirthdayKey(month = month, dayOfMonth = day)
}

private fun String?.matchesMonthAndDay(month: Int, dayOfMonth: Int): Boolean {
    val monthDay = this?.toBirthMonthDayOrNull()
    if (monthDay == null) {
        return false
    }
    val birthMonth = monthDay.first
    val birthDay = monthDay.second
    return birthMonth == month && birthDay == dayOfMonth
}

private fun String.toBirthMonthDayOrNull(): Pair<Int, Int>? {
    val normalized = trim()
    val isoParts = normalized.split("-")

    if (isoParts.size == 3) {
        val month = isoParts[1].toIntOrNull()
        val day = isoParts[2].toIntOrNull()
        if (month != null && day != null) {
            return month to day
        }
    }
    val slashParts = normalized.split("/")
    if (slashParts.size == 3) {
        val month = slashParts[0].toIntOrNull()
        val day = slashParts[1].toIntOrNull()
        if (month != null && day != null) {
            return month to day
        }
    }
    return null
}

private fun String.normalizeBirthdayToIsoOrNull(): String? {
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

private fun String.toCafeIdFromCastDocumentName(): String? {
    val segments = split("/")
    val castsIndex = segments.indexOf(FirestorePaths.CAFE_CASTS)
    val cafeSegmentIndex = castsIndex - 1
    val cafeCollectionIndex = castsIndex - 2

    if (castsIndex <= 1 || cafeCollectionIndex < 0 || cafeSegmentIndex < 0) {
        return null
    }
    return if (segments[cafeCollectionIndex] != FirestorePaths.CAFES) null else segments[cafeSegmentIndex].takeIf { value -> value.isNotBlank() }
}

private fun String.toCafeIdFromNoticeDocumentName(): String? {
    val segments = split("/")
    val noticesIndex = segments.indexOf(FirestorePaths.CAFE_NOTICES)
    val cafeSegmentIndex = noticesIndex - 1
    val cafeCollectionIndex = noticesIndex - 2

    if (noticesIndex <= 1 || cafeCollectionIndex < 0 || cafeSegmentIndex < 0) {
        return null
    }
    return if (segments[cafeCollectionIndex] != FirestorePaths.CAFES) null else segments[cafeSegmentIndex].takeIf { value -> value.isNotBlank() }
}

private fun rankingDocumentId(
    kind: String,
    period: RankingPeriod,
    country: String?,
    city: String?
): String {
    val kindValue = kind.trim().lowercase()
    val periodValue = period.name.lowercase()
    val countryValue = country?.trim()?.takeIf { value -> value.isNotEmpty() }?.slugifyRankingToken() ?: "all"
    val cityValue = city?.trim()?.takeIf { value -> value.isNotEmpty() }?.slugifyRankingToken() ?: "all"
    return "${kindValue}_${periodValue}_${countryValue}_${cityValue}"
}

private fun String.slugifyRankingToken(): String {
    val normalized = trim().lowercase()
    val alphanumeric = Regex("[^a-z0-9]+").replace(normalized, "-")
    val collapsed = Regex("-+").replace(alphanumeric, "-")
    return collapsed.trim('-').ifBlank { "all" }
}

private fun nextFirestoreEntityId(prefix: String): String {
    val now = Clock.System.now().toEpochMilliseconds()
    return "$prefix-$now"
}

private fun Throwable.isFirestoreNotFound(): Boolean {
    return message?.contains("request failed(404)") == true
}

private fun isFirestorePermissionDenied(error: Throwable): Boolean {
    val message = error.message.orEmpty()
    return message.contains("PERMISSION_DENIED", ignoreCase = true) ||
        message.contains("Missing or insufficient permissions", ignoreCase = true) ||
        message.contains("request failed(403)", ignoreCase = true)
}

private const val RANKING_KIND_CAST = "cast"
private const val RANKING_KIND_CAFE = "cafe"
private const val MAX_FIRESTORE_IN_FILTER_VALUES = 10
private const val REGION_FILTER_CAFE_CACHE_LIMIT = 200
private const val CAFE_FANOUT_CACHE_LIMIT = 120
private const val CAFE_CAST_RESOLUTION_CAFE_FETCH_LIMIT = 300
private const val CAST_FANOUT_CAFE_LIMIT = 40
private const val CAST_FANOUT_PER_CAFE_LIMIT = 20
private const val NOTICE_FANOUT_CAFE_LIMIT = 30
private const val NOTICE_FANOUT_PER_CAFE_LIMIT = 6
