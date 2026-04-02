package com.hhp227.concafe.data.source

import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.AppNotification
import com.hhp227.concafe.domain.model.Cafe
import com.hhp227.concafe.domain.model.CafeDashboardData
import com.hhp227.concafe.domain.model.CafeManagementData
import com.hhp227.concafe.domain.model.CafeDetail
import com.hhp227.concafe.domain.model.CafeEventManagementItem
import com.hhp227.concafe.domain.model.CafeInfoUpdate
import com.hhp227.concafe.domain.model.CafeRegistrationClaim
import com.hhp227.concafe.domain.model.CafeNoticeManagementItem
import com.hhp227.concafe.domain.model.CafeMenuGoodsUpsert
import com.hhp227.concafe.domain.model.Cast
import com.hhp227.concafe.domain.model.CastClaim
import com.hhp227.concafe.domain.model.CastDetail
import com.hhp227.concafe.domain.model.CastSchedule
import com.hhp227.concafe.domain.model.CastScheduleStatus
import com.hhp227.concafe.domain.model.CastScheduleUpdate
import com.hhp227.concafe.domain.model.CastUpsert
import com.hhp227.concafe.domain.model.ExternalLink
import com.hhp227.concafe.domain.model.Goods
import com.hhp227.concafe.domain.model.HomeBanner
import com.hhp227.concafe.domain.model.Inquiry
import com.hhp227.concafe.domain.model.CafeMenu
import com.hhp227.concafe.domain.model.MyPageSummary
import com.hhp227.concafe.domain.model.Notice
import com.hhp227.concafe.domain.model.RankingItem
import com.hhp227.concafe.domain.model.RankingPeriod
import com.hhp227.concafe.domain.model.Review
import com.hhp227.concafe.domain.model.Stamp
import com.hhp227.concafe.domain.model.User
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.model.Visit
import com.hhp227.concafe.domain.model.VisitVerificationResult
import com.hhp227.concafe.domain.model.UserNotificationSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

class FirestoreCacheDataSource :
    AuthDataSource,
    CafeDataSource,
    CastDataSource,
    CastClaimDataSource,
    BannerDataSource,
    InquiryDataSource,
    NoticeDataSource,
    ReviewDataSource,
    VisitDataSource,
    ExternalLinkDataSource,
    StampDataSource,
    ScheduleStatusDataSource,
    NotificationDataSource,
    SocialDataSource,
    MyInfoDataSource,
    RankingDataSource,
    PagingDataSource {
    private val _currentUserId = MutableStateFlow<String?>(null)

    override var currentUserId: String?
        get() = _currentUserId.value
        set(value) {
            _currentUserId.value = value
        }

    override val currentUserIdFlow: StateFlow<String?> = _currentUserId.asStateFlow()

    private val users = mutableListOf<User>()

    override fun findUserById(userId: String): User? {
        return users.firstOrNull { it.id == userId }
    }

    override fun findUserByEmail(email: String): User? {
        return users.firstOrNull { it.email == email }
    }

    override fun isEmailTaken(email: String): Boolean {
        return users.any { it.email == email }
    }

    override fun addUser(user: User) {
        users.add(user)
    }

    override fun removeUser(userId: String): Boolean {
        val removed = users.removeAll { user -> user.id == userId }

        if (!removed) {
            return false
        }
        if (currentUserId == userId) {
            currentUserId = null
        }
        favoriteCafeIdsByUser.remove(userId)
        followedCastIdsByUser.remove(userId)
        dismissedReviewPromptVisitIdsByUser.remove(userId)
        ownedCafeIdsByUser.remove(userId)
        pendingCafeClaimsByUser.remove(userId)
        pendingCafeRegistrationClaimsByUser.remove(userId)
        affiliatedCafeIdByUser.remove(userId)
        favoriteUserIdsByCafeId.values.forEach { userIds ->
            userIds.remove(userId)
        }
        followerUserIdsByCastId.values.forEach { userIds ->
            userIds.remove(userId)
        }
        notificationSettingsByUser.remove(userId)
        notifications.removeAll { notification -> notification.userId == userId }
        inquiries.removeAll { inquiry -> inquiry.userId == userId }
        visits.removeAll { visit -> visit.userId == userId }
        reviews.removeAll { review -> review.userId == userId }
        stamps.removeAll { stamp -> stamp.userId == userId }
        castClaims.removeAll { claim -> claim.userId == userId }
        return true
    }

    override fun replaceUser(user: User): Boolean {
        val index = users.indexOfFirst { it.id == user.id }

        if (index == -1) {
            return false
        }
        users[index] = user
        return true
    }

    override fun replaceAllUsers(users: List<User>) {
        this.users.clear()
        this.users.addAll(users)
    }

    override val cafes = mutableListOf<Cafe>()

    override val casts = mutableListOf<Cast>()

    override val castClaims = mutableListOf<CastClaim>()

    override val banners = mutableListOf<HomeBanner>()

    override val inquiries = mutableListOf<Inquiry>()

    override val notices = mutableListOf<Notice>()

    override val cafeNoticeManagementItems = mutableListOf<CafeNoticeManagementItem>()

    override val cafeEventManagementItems = mutableListOf<CafeEventManagementItem>()

    override val cafeDetailsById = mutableMapOf<String, CafeDetail>()

    private val castImagesById = mutableMapOf<String, List<String>>()

    private val castSchedulesByCastId = mutableMapOf<String, List<CastSchedule>>()

    override val castScheduleStatusByCastId = mutableMapOf<String, MutableMap<String, CastScheduleStatus>>()

    override val reviews = mutableListOf<Review>()

    override val visits = mutableListOf<Visit>()

    override val notifications = mutableListOf<AppNotification>()

    private val notificationSettingsByUser = mutableMapOf<String, UserNotificationSettings>()

    override val favoriteCafeIdsByUser = mutableMapOf<String, MutableSet<String>>()

    override val favoriteUserIdsByCafeId = mutableMapOf<String, MutableSet<String>>()

    override val followedCastIdsByUser = mutableMapOf<String, MutableSet<String>>()

    override val followerUserIdsByCastId = mutableMapOf<String, MutableSet<String>>()

    override val cafeExternalLinksByCafeId = mutableMapOf<String, MutableList<ExternalLink>>()

    override val castExternalLinksByCastId = mutableMapOf<String, MutableList<ExternalLink>>()

    override val stamps = mutableListOf<Stamp>()

    override val dismissedReviewPromptVisitIdsByUser = mutableMapOf<String, MutableSet<String>>()

    override val ownedCafeIdsByUser = mutableMapOf<String, MutableList<String>>()

    override val pendingCafeClaimsByUser = mutableMapOf<String, MutableList<CafeManagementData.PendingClaimSummary>>()

    override val pendingCafeRegistrationClaimsByUser = mutableMapOf<String, MutableList<CafeRegistrationClaim>>()

    override val affiliatedCafeIdByUser = mutableMapOf<String, String>()

    override val cafeCheckInCountById = emptyMap<String, Int>()

    override val cafeTodayCheckInCountById: Map<String, Int>
        get() {
            val today = Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date.toString()
            return visits
                .filter { it.visitedAt.startsWith(today) }
                .groupingBy { it.cafeId }
                .eachCount()
        }

    override val cafeTodayReviewCountById: Map<String, Int>
        get() {
            val today = Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date.toString()
            return reviews
                .filter { it.createdAt.startsWith(today) }
                .groupingBy { it.cafeId }
                .eachCount()
        }

    override val onShiftCastIdsByCafeId = emptyMap<String, Set<String>>()

    override val cafeHomeBannerPreviewByCafeId = mutableMapOf<String, CafeDashboardData.HomeBannerPreview>()

    override val castTodayVisitCountById = emptyMap<String, Int>()

    override fun defaultMyPageSummary(userId: String): MyPageSummary {
        val favoritesCount = favoriteCafeIdsByUser[userId]?.size ?: 0
        val followedCount = followedCastIdsByUser[userId]?.size ?: 0
        val visitCount = visits.count { it.userId == userId }
        val badgesCount = stamps.count { it.userId == userId }
        val level = max(1, 1 + (visitCount / 5))
        return MyPageSummary(userId, visitCount, favoritesCount, followedCount, badgesCount, level)
    }

    override fun cafeDetail(cafeId: String): CafeDetail? {
        return cafeDetailsById[cafeId]
    }

    override fun updateCafeInfo(update: CafeInfoUpdate): CafeDetail {
        val cafeIndex = cafes.indexOfFirst { it.id == update.cafeId }
        if (cafeIndex == -1) {
            throw NoSuchElementException("cafe not found")
        }
        val currentCafe = cafes[cafeIndex]
        val currentDetail = cafeDetailsById[update.cafeId] ?: buildCafeDetail(currentCafe)
        val representativeImage = update.representativeImageUrl
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: currentDetail.images.firstOrNull()
            ?: currentCafe.thumbnailImage
        val galleryImages = update.galleryImages
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val nextImages = buildList {
            representativeImage?.let { add(it) }
            addAll(galleryImages.filterNot { it == representativeImage })
        }
        val updatedCafe = currentCafe.copy(
            name = update.name,
            desc = update.description,
            thumbnailImage = representativeImage,
            region = currentCafe.region.copy(
                address = update.address,
                location = update.location ?: currentCafe.region.location
            )
        )
        val updatedDetail = currentDetail.copy(
            cafe = updatedCafe,
            images = nextImages.ifEmpty { currentDetail.images },
            businessHours = formatBusinessHours(update),
            phoneNumber = update.contactNumber
        )
        cafes[cafeIndex] = updatedCafe
        cafeDetailsById[update.cafeId] = updatedDetail
        return updatedDetail
    }

    override fun upsertCafeMenuGoods(update: CafeMenuGoodsUpsert): CafeDetail {
        require(update.name.isNotBlank()) { "name is required" }
        require(update.price >= 0) { "price must be zero or positive" }

        val cafe = cafes.firstOrNull { it.id == update.cafeId }
            ?: throw NoSuchElementException("cafe not found")
        val currentDetail = cafeDetailsById[update.cafeId] ?: buildCafeDetail(cafe)
        val normalizedCategory = update.category.lowercase()
        val isGoodsCategory = normalizedCategory == "goods"
        val existingMenu = currentDetail.menus.firstOrNull { it.id == update.itemId }
        val existingGoods = currentDetail.goods.firstOrNull { it.id == update.itemId }
        val filteredMenus = currentDetail.menus.filterNot { it.id == update.itemId }.toMutableList()
        val filteredGoods = currentDetail.goods.filterNot { it.id == update.itemId }.toMutableList()
        val updatedMenus = if (isGoodsCategory) {
            filteredMenus
        } else {
            filteredMenus.apply {
                add(
                    CafeMenu(
                        id = existingMenu?.id
                            ?: nextId(prefix = "menu", ids = currentDetail.menus.map { it.id }),
                        name = update.name,
                        price = update.price,
                        desc = update.description,
                        image = update.imageUrl ?: existingMenu?.image ?: existingGoods?.image,
                        category = normalizedCategory,
                        isAvailable = update.isInStock
                    )
                )
            }
        }
        val updatedGoods = if (isGoodsCategory) {
            filteredGoods.apply {
                val nextStock = when {
                    update.isInStock && (existingGoods?.stock ?: 0) > 0 -> existingGoods?.stock ?: 50
                    update.isInStock -> 50
                    else -> 0
                }
                add(
                    Goods(
                        id = existingGoods?.id ?: nextId(prefix = "goods", ids = currentDetail.goods.map { it.id }),
                        name = update.name,
                        price = update.price,
                        image = update.imageUrl ?: existingGoods?.image,
                        stock = nextStock
                    )
                )
            }
        } else {
            filteredGoods
        }
        val updatedDetail = currentDetail.copy(
            menus = updatedMenus,
            goods = updatedGoods
        )
        cafeDetailsById[update.cafeId] = updatedDetail
        return updatedDetail
    }

    override fun deleteCafeMenuGoods(cafeId: String, itemId: String): CafeDetail {
        val cafe = cafes.firstOrNull { it.id == cafeId }
            ?: throw NoSuchElementException("cafe not found")
        val currentDetail = cafeDetailsById[cafeId] ?: buildCafeDetail(cafe)
        val updatedMenus = currentDetail.menus.filterNot { it.id == itemId }
        val updatedGoods = currentDetail.goods.filterNot { it.id == itemId }

        if (updatedMenus.size == currentDetail.menus.size && updatedGoods.size == currentDetail.goods.size) {
            throw NoSuchElementException("menu goods item not found")
        }

        val updatedDetail = currentDetail.copy(
            menus = updatedMenus,
            goods = updatedGoods
        )
        cafeDetailsById[cafeId] = updatedDetail
        return updatedDetail
    }

    private fun buildCafeDetail(cafe: Cafe): CafeDetail {
        val cafeCasts = casts.filter { it.cafeId == cafe.id }
        val cafeNotices = notices.filter { it.cafeId == cafe.id }
        return CafeDetail(
            cafe = cafe,
            images = emptyList(),
            casts = cafeCasts,
            menus = emptyList(),
            goods = emptyList(),
            notices = cafeNotices,
            businessHours = "",
            phoneNumber = ""
        )
    }

    private fun formatBusinessHours(update: CafeInfoUpdate): String {
        val weekday = listOf(update.weekdayOpen, update.weekdayClose).all { it.isNotBlank() }
        val weekend = listOf(update.weekendOpen, update.weekendClose).all { it.isNotBlank() }
        return when {
            weekday && weekend && update.weekdayOpen == update.weekendOpen && update.weekdayClose == update.weekendClose ->
                "매일 ${update.weekdayOpen} - ${update.weekdayClose}"
            weekday && weekend ->
                "평일 ${update.weekdayOpen} - ${update.weekdayClose} / 주말 ${update.weekendOpen} - ${update.weekendClose}"
            weekday ->
                "평일 ${update.weekdayOpen} - ${update.weekdayClose}"
            weekend ->
                "주말 ${update.weekendOpen} - ${update.weekendClose}"
            else -> ""
        }
    }

    private fun nextId(prefix: String, ids: List<String>): String {
        val nextNumber = ids.mapNotNull { id ->
            id.removePrefix("$prefix-").toIntOrNull()
        }.maxOrNull()?.plus(1) ?: 1
        return "$prefix-$nextNumber"
    }

    fun updateCastImages(castId: String, images: List<String>) {
        if (images.isNotEmpty()) {
            castImagesById[castId] = images
        }
    }

    override fun castDetail(castId: String): CastDetail? {
        val cast = casts.firstOrNull { it.id == castId } ?: return null
        val cafe = cafes.firstOrNull { it.id == cast.cafeId } ?: return null
        return CastDetail(
            cast = cast,
            cafe = cafe,
            images = castImagesById[cast.id].orEmpty(),
            schedule = castSchedulesByCastId[cast.id].orEmpty(),
            visitCertificationCount = cast.visitCertificationCount
        )
    }

    override fun castSchedules(castId: String, fromDate: String, toDate: String): List<CastSchedule> {
        castDetail(castId) ?: throw NoSuchElementException("cast detail not found")
        return castSchedulesByCastId[castId]
            .orEmpty()
            .filter { it.date >= fromDate && it.date <= toDate }
            .sortedBy { it.date }
    }

    override fun castScheduleStatuses(
        castId: String,
        fromDate: String,
        toDate: String
    ): Map<String, CastScheduleStatus> {
        castDetail(castId) ?: throw NoSuchElementException("cast detail not found")
        return castScheduleStatusByCastId[castId]
            .orEmpty()
            .filterKeys { date -> date >= fromDate && date <= toDate }
            .toMap()
    }

    override fun updateCastSchedule(update: CastScheduleUpdate): CastSchedule? {
        val cast = casts.firstOrNull { it.id == update.castId }
            ?: throw NoSuchElementException("cast detail not found")
        val date = update.date
        val existingSchedules = castSchedulesByCastId[update.castId].orEmpty()
        val nextSchedules = existingSchedules.filterNot { it.date == date }.toMutableList()
        val nextStatuses = castScheduleStatusByCastId
            .getOrPut(update.castId) { mutableMapOf() }
        val updatedSchedule = when (update.status) {
            CastScheduleStatus.WORK -> {
                val startTime = update.startTime?.takeIf { it.isNotBlank() }
                    ?: throw IllegalArgumentException("start time is required")
                val endTime = update.endTime?.takeIf { it.isNotBlank() }
                    ?: throw IllegalArgumentException("end time is required")
                require(startTime < endTime) { "end time must be after start time" }
                CastSchedule(
                    id = existingSchedules.firstOrNull { it.date == date }?.id
                        ?: "schedule-${update.castId}-${date.replace("-", "")}",
                    castId = update.castId,
                    cafeId = cast.cafeId,
                    date = date,
                    startTime = startTime,
                    endTime = endTime
                ).also { nextSchedules += it }
            }
            CastScheduleStatus.OFF,
            CastScheduleStatus.VACATION -> null
        }

        castSchedulesByCastId[update.castId] = nextSchedules.sortedBy { it.date }
        nextStatuses[date] = update.status
        return updatedSchedule
    }

    override fun upsertCast(update: CastUpsert): CastDetail {
        if (update.name.isBlank()) {
            throw IllegalArgumentException("cast name is required")
        }
        if (update.conceptRole.isBlank()) {
            throw IllegalArgumentException("concept role is required")
        }

        val existingCast = update.castId?.let { castId ->
            casts.firstOrNull { it.id == castId } ?: throw NoSuchElementException("cast detail not found")
        }
        val targetCafeId = update.cafeId
            ?: existingCast?.cafeId
            ?: throw IllegalArgumentException("cafeId is required")
        val targetCafe = cafes.firstOrNull { it.id == targetCafeId }
            ?: throw NoSuchElementException("cafe not found")
        val normalizedBirthday = update.birthday?.takeIf { it.isNotBlank() }
        val normalizedProfileImage = update.profileImage?.trim()?.takeIf { it.isNotEmpty() } ?: existingCast?.profileImage
        val normalizedGalleryImages = update.galleryImages
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val castId = existingCast?.id ?: nextId("maid", casts.map { it.id })
        val nextCast = Cast(
            id = castId,
            cafeId = targetCafeId,
            name = update.name.trim(),
            linkedUserId = existingCast?.linkedUserId,
            profileImage = normalizedProfileImage,
            desc = update.introduction.trim(),
            birthday = normalizedBirthday,
            conceptRole = update.conceptRole.trim(),
            followerCount = existingCast?.followerCount ?: 0,
            rating = existingCast?.rating ?: 0.0,
            visitCertificationCount = existingCast?.visitCertificationCount ?: 0
        )
        val existingIndex = casts.indexOfFirst { it.id == castId }

        if (existingIndex >= 0) {
            casts[existingIndex] = nextCast
        } else {
            casts.add(nextCast)
        }
        currentUserId?.let { signedInUserId ->
            val userIndex = users.indexOfFirst { it.id == signedInUserId }

            if (userIndex >= 0) {
                val currentUser = users[userIndex]

                if (currentUser.role == UserRole.CAST && nextCast.linkedUserId == signedInUserId) {
                    users[userIndex] = currentUser.copy(nickname = nextCast.name)
                }
            }
        }
        val nextCastImages = buildList {
            normalizedProfileImage?.let { add(it) }
            addAll(normalizedGalleryImages.filterNot { it == normalizedProfileImage })
        }

        if (nextCastImages.isNotEmpty()) {
            castImagesById[castId] = nextCastImages
        } else if (castImagesById[castId] == null) {
            castImagesById[castId] = listOfNotNull(nextCast.profileImage)
        }
        castSchedulesByCastId[castId] = buildCastSchedules(castId, targetCafeId, update.workingDays)
        castScheduleStatusByCastId[castId] = castSchedulesByCastId[castId]
            .orEmpty()
            .associate { schedule -> schedule.date to CastScheduleStatus.WORK }
            .toMutableMap()
        return CastDetail(
            cast = nextCast,
            cafe = targetCafe,
            images = castImagesById[castId].orEmpty(),
            schedule = castSchedulesByCastId[castId].orEmpty(),
            visitCertificationCount = nextCast.visitCertificationCount
        )
    }

    override fun deleteCast(castId: String): Cast {
        val castIndex = casts.indexOfFirst { it.id == castId }

        if (castIndex == -1) {
            throw NoSuchElementException("cast detail not found")
        }
        val deletedCast = casts.removeAt(castIndex)
        castImagesById.remove(castId)
        castSchedulesByCastId.remove(castId)
        castScheduleStatusByCastId.remove(castId)
        castClaims.removeAll { it.castId == castId }
        followedCastIdsByUser.values.forEach { it.remove(castId) }
        followerUserIdsByCastId.remove(castId)
        deletedCast.linkedUserId?.let { linkedUserId ->
            if (!affiliatedCafeIdByUser.containsKey(linkedUserId)) {
                affiliatedCafeIdByUser[linkedUserId] = deletedCast.cafeId
            }
        }
        cafeDetailsById[deletedCast.cafeId]?.let { currentDetail ->
            cafeDetailsById[deletedCast.cafeId] = currentDetail.copy(
                casts = currentDetail.casts.filterNot { it.id == castId }
            )
        }
        return deletedCast
    }

    override fun refreshReviewProjections(cafeId: String, taggedCastIds: List<String>) {
        if (taggedCastIds.isNotEmpty()) {
            // 캐스트 집계 갱신은 후속 단계에서 연결하고, 현재는 카페 리뷰 집계만 갱신한다.
        }

        val cafeIndex = cafes.indexOfFirst { it.id == cafeId }
        if (cafeIndex == -1) return

        val cafeReviews = reviews.filter { it.cafeId == cafeId }
        val reviewCount = cafeReviews.size
        val ratingAverage = if (cafeReviews.isEmpty()) {
            0.0
        } else {
            cafeReviews.map { it.rating.toDouble() }.average()
        }
        val currentCafe = cafes[cafeIndex]
        val updatedCafe = currentCafe.copy(
            ratingAvg = ratingAverage,
            reviewCount = reviewCount
        )
        cafes[cafeIndex] = updatedCafe
        val currentDetail = cafeDetailsById[cafeId] ?: buildCafeDetail(updatedCafe)
        cafeDetailsById[cafeId] = currentDetail.copy(cafe = updatedCafe)

    }

    override suspend fun rankingItemsFromCasts(
        period: RankingPeriod,
        country: String?,
        city: String?
    ): List<RankingItem> {
        val cafeNameById = cafes.associate { cafe -> cafe.id to cafe.name }
        var filtered = casts.asSequence()

        if (!country.isNullOrBlank()) {
            val normalizedCountry = country.trim()
            filtered = filtered.filter { cast ->
                val cafeCountry = cafes.firstOrNull { cafe -> cafe.id == cast.cafeId }?.region?.country
                cafeCountry.equals(normalizedCountry, ignoreCase = true)
            }
        }
        if (!city.isNullOrBlank()) {
            val normalizedCity = city.trim()
            filtered = filtered.filter { cast ->
                val cafeCity = cafes.firstOrNull { cafe -> cafe.id == cast.cafeId }?.region?.city
                cafeCity.equals(normalizedCity, ignoreCase = true)
            }
        }
        return filtered.toList()
            .sortedByDescending { it.followerCount }
            .take(RANKING_MAX_COUNT)
            .mapIndexed { index, cast ->
                RankingItem(
                    id = cast.id,
                    name = cast.name,
                    subtitle = cafeNameById[cast.cafeId] ?: cast.cafeId,
                    score = cast.followerCount,
                    rank = index + 1,
                    change = "0",
                    imageUrl = cast.profileImage
                )
            }
    }

    override suspend fun getNotifications(
        userId: String,
        cursor: String?,
        pageSize: Int
    ): PagedResult<AppNotification> {
        val items = notifications
            .filter { notification -> notification.userId == userId }
            .sortedByDescending { notification -> notification.createdAt }
        return toPaged(items, cursor, pageSize)
    }

    override suspend fun markNotificationAsRead(userId: String, notificationId: String) {
        val index = notifications.indexOfFirst { notification ->
            notification.userId == userId && notification.id == notificationId
        }

        if (index == -1) {
            throw NoSuchElementException("notification not found")
        } else {
            val current = notifications[index]
            notifications[index] = current.copy(isRead = true)
        }
    }

    override suspend fun getNotificationSettings(userId: String): UserNotificationSettings {
        val cached = notificationSettingsByUser[userId]

        if (cached != null) {
            return cached
        } else {
            return UserNotificationSettings.default()
        }
    }

    override suspend fun updateNotificationSettings(
        userId: String,
        settings: UserNotificationSettings
    ): UserNotificationSettings {
        notificationSettingsByUser[userId] = settings
        return settings
    }

    override suspend fun registerPushToken(userId: String, platform: String, token: String) {
        if (userId.isBlank() || platform.isBlank() || token.isBlank()) {
            throw IllegalArgumentException("invalid push token payload")
        }
    }

    override suspend fun disableAllPushTokens(userId: String) {
        if (userId.isBlank()) {
            throw IllegalArgumentException("invalid push token payload")
        } else {
            Unit
        }
    }

    override suspend fun sendFanAnnouncement(
        userId: String,
        cafeId: String,
        castId: String,
        title: String,
        body: String
    ) {
        if (userId.isBlank() || cafeId.isBlank() || castId.isBlank()) {
            throw IllegalArgumentException("fan announcement target is required")
        }
        if (title.isBlank()) {
            throw IllegalArgumentException("fan announcement title is required")
        }
        if (body.isBlank()) {
            throw IllegalArgumentException("fan announcement body is required")
        }
    }

    override suspend fun rankingItemsFromCafes(
        period: RankingPeriod,
        country: String?,
        city: String?
    ): List<RankingItem> {
        var filtered = cafes.asSequence()

        if (!country.isNullOrBlank()) {
            val normalizedCountry = country.trim()
            filtered = filtered.filter { cafe ->
                cafe.region.country.equals(normalizedCountry, ignoreCase = true)
            }
        }
        if (!city.isNullOrBlank()) {
            val normalizedCity = city.trim()
            filtered = filtered.filter { cafe ->
                cafe.region.city.equals(normalizedCity, ignoreCase = true)
            }
        }
        return filtered.toList()
            .sortedByDescending { it.ratingAvg }
            .take(RANKING_MAX_COUNT)
            .mapIndexed { index, cafe ->
                RankingItem(
                    id = cafe.id,
                    name = cafe.name,
                    subtitle = cafe.region.address.substringBefore("구").substringBefore("로").ifBlank { cafe.region.city },
                    score = (cafe.ratingAvg * 100).toInt(),
                    rank = index + 1,
                    change = "0",
                    imageUrl = cafe.thumbnailImage
                )
            }
    }

    override fun homePopularCastPage(cursor: String?, pageSize: Int): PagedResult<Cast> {
        val cappedCasts = casts
            .sortedByDescending { it.followerCount }
            .take(HOME_POPULAR_CAST_MAX_COUNT)
        return toPaged(cappedCasts, cursor, pageSize)
    }

    override fun verifyVisitResult(cafeId: String, latitude: Double, longitude: Double): VisitVerificationResult {
        val cafe = cafes.firstOrNull { it.id == cafeId }

        if (cafe == null) {
            return VisitVerificationResult(false, Double.POSITIVE_INFINITY, 100.0, "카페 위치 정보를 확인할 수 없습니다")
        }
        val cafeLat = cafe.region.location.latitude
        val cafeLon = cafe.region.location.longitude
        val distance = haversineMeters(cafeLat, cafeLon, latitude, longitude)
        val allowed = 100.0
        return if (distance <= allowed) {
            VisitVerificationResult(true, distance, allowed, "방문 인증 성공")
        } else {
            VisitVerificationResult(false, distance, allowed, "카페 반경 100m 밖입니다")
        }
    }

    override fun <T> toPaged(items: List<T>, cursor: String?, pageSize: Int): PagedResult<T> {
        val start = cursor?.toIntOrNull() ?: 0
        val endExclusive = (start + pageSize).coerceAtMost(items.size)
        val next = if (endExclusive < items.size) endExclusive.toString() else null
        return PagedResult(items.subList(start, endExclusive), next, next != null)
    }

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = (lat2 - lat1).toRadians()
        val dLon = (lon2 - lon1).toRadians()
        val a = sin(dLat / 2).times(sin(dLat / 2)) +
            cos(lat1.toRadians()).times(cos(lat2.toRadians()).times(sin(dLon / 2).times(sin(dLon / 2))))
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    private fun Double.toRadians(): Double {
        return this * PI / 180.0
    }

    private companion object {
        private const val HOME_POPULAR_CAST_MAX_COUNT = 50
        private const val RANKING_MAX_COUNT = 50
    }
}

private fun buildCastSchedules(castId: String, cafeId: String, workingDays: List<String>): List<CastSchedule> {
    val dateByWorkingDay = mapOf(
        "MONDAY" to "2026-03-09",
        "TUESDAY" to "2026-03-10",
        "WEDNESDAY" to "2026-03-11",
        "THURSDAY" to "2026-03-12",
        "FRIDAY" to "2026-03-13",
        "SATURDAY" to "2026-03-14",
        "SUNDAY" to "2026-03-15"
    )
    return workingDays.distinct().mapIndexedNotNull { index, workingDay ->
        val date = dateByWorkingDay[workingDay] ?: return@mapIndexedNotNull null
        CastSchedule(
            id = "schedule-${castId}-${index + 1}",
            castId = castId,
            cafeId = cafeId,
            date = date,
            startTime = "18:00",
            endTime = "22:00"
        )
    }
}

private fun buildCastScheduleStatusesByCastId(
    schedulesByCastId: Map<String, List<CastSchedule>>
): MutableMap<String, MutableMap<String, CastScheduleStatus>> {
    return schedulesByCastId
        .mapValues { (_, schedules) ->
            schedules.associate { schedule ->
                schedule.date to CastScheduleStatus.WORK
            }.toMutableMap()
        }
        .toMutableMap()
}
