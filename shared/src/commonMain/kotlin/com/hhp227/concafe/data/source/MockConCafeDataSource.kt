package com.hhp227.concafe.data.source

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.AppNotification
import com.hhp227.concafe.domain.model.Cafe
import com.hhp227.concafe.domain.model.CafeDashboardData
import com.hhp227.concafe.domain.model.CafeManagementData
import com.hhp227.concafe.domain.model.CafeDetail
import com.hhp227.concafe.domain.model.CafeInfoUpdate
import com.hhp227.concafe.domain.model.CafeMenuGoodsUpsert
import com.hhp227.concafe.domain.model.Cast
import com.hhp227.concafe.domain.model.CastDetail
import com.hhp227.concafe.domain.model.CastSchedule
import com.hhp227.concafe.domain.model.CastUpsert
import com.hhp227.concafe.domain.model.GeoPoint
import com.hhp227.concafe.domain.model.Goods
import com.hhp227.concafe.domain.model.HomeBanner
import com.hhp227.concafe.domain.model.CafeMenu
import com.hhp227.concafe.domain.model.MyPageSummary
import com.hhp227.concafe.domain.model.Notice
import com.hhp227.concafe.domain.model.RankingItem
import com.hhp227.concafe.domain.model.Region
import com.hhp227.concafe.domain.model.Review
import com.hhp227.concafe.domain.model.User
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.model.Visit
import com.hhp227.concafe.domain.model.VisitVerificationResult
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class MockConCafeDataSource : ConCafeDataSource {
    override var currentUserId: String? = null

    override val users = mutableListOf(
        User(
            id = "user-1",
            email = "user1@concafe.app",
            nickname = "리본냥",
            profileImage = null,
            role = UserRole.VISITOR,
            banned = false,
            createdAt = "2026-03-01T09:00:00Z"
        ),
        User(
            id = "user-2",
            email = "cast@concafe.app",
            nickname = "사쿠라",
            profileImage = null,
            role = UserRole.CAST,
            banned = false,
            createdAt = "2026-03-01T09:10:00Z"
        ),
        User(
            id = "user-3",
            email = "owner@concafe.app",
            nickname = "메이드하우스점장",
            profileImage = null,
            role = UserRole.CAFE_OWNER,
            banned = false,
            createdAt = "2026-03-01T09:20:00Z"
        ),
        User(
            id = "user-4",
            email = "admin@concafe.app",
            nickname = "콘카페관리자",
            profileImage = null,
            role = UserRole.ADMIN,
            banned = false,
            createdAt = "2026-03-01T09:30:00Z"
        ),
        User(
            id = "user-5",
            email = "owner.nocafe@concafe.app",
            nickname = "카페연결대기점장",
            profileImage = null,
            role = UserRole.CAFE_OWNER,
            banned = false,
            createdAt = "2026-03-10T09:40:00Z"
        ),
        User(
            id = "user-6",
            email = "fan.1@concafe.app",
            nickname = "메이드팬123",
            profileImage = null,
            role = UserRole.VISITOR,
            banned = false,
            createdAt = "2026-03-10T10:00:00Z"
        ),
        User(
            id = "user-7",
            email = "fan.2@concafe.app",
            nickname = "리본러버",
            profileImage = null,
            role = UserRole.VISITOR,
            banned = false,
            createdAt = "2026-03-10T10:10:00Z"
        ),
        User(
            id = "user-8",
            email = "fan.3@concafe.app",
            nickname = "사쿠라오시",
            profileImage = null,
            role = UserRole.VISITOR,
            banned = false,
            createdAt = "2026-03-10T10:20:00Z"
        )
    )

    override val cafes = mutableListOf(
        Cafe(
            id = "cafe-1",
            name = "메이드 하우스",
            desc = "강남 인기 메이드카페",
            region = Region("KR", "Seoul", "강남구 테헤란로", GeoPoint(37.499, 127.031)),
            thumbnailImage = null,
            ratingAvg = 4.8,
            reviewCount = 221,
            approved = true,
            conceptType = "MAID"
        ),
        Cafe(
            id = "cafe-2",
            name = "핑크 캐슬",
            desc = "신촌 감성 카페",
            region = Region("KR", "Seoul", "서대문구 신촌로", GeoPoint(37.555, 126.936)),
            thumbnailImage = null,
            ratingAvg = 4.9,
            reviewCount = 178,
            approved = true,
            conceptType = "MAID"
        ),
        Cafe(
            id = "cafe-3",
            name = "리본 카페",
            desc = "홍대 서브컬쳐 카페",
            region = Region("KR", "Seoul", "마포구 와우산로", GeoPoint(37.556, 126.923)),
            thumbnailImage = null,
            ratingAvg = 4.7,
            reviewCount = 149,
            approved = true,
            conceptType = "MAID"
        ),
        Cafe(
            id = "cafe-4",
            name = "슈가 드롭",
            desc = "합정의 달콤한 분위기 메이드카페",
            region = Region("KR", "Seoul", "마포구 양화로", GeoPoint(37.550, 126.914)),
            thumbnailImage = null,
            ratingAvg = 4.6,
            reviewCount = 132,
            approved = true,
            conceptType = "MAID"
        ),
        Cafe(
            id = "cafe-5",
            name = "루나 살롱",
            desc = "건대 감성의 클래식 메이드카페",
            region = Region("KR", "Seoul", "광진구 아차산로", GeoPoint(37.540, 127.069)),
            thumbnailImage = null,
            ratingAvg = 4.5,
            reviewCount = 118,
            approved = true,
            conceptType = "MAID"
        ),
        Cafe(
            id = "cafe-6",
            name = "체리 블룸",
            desc = "잠실의 봄 테마 메이드카페",
            region = Region("KR", "Seoul", "송파구 올림픽로", GeoPoint(37.513, 127.102)),
            thumbnailImage = null,
            ratingAvg = 4.8,
            reviewCount = 167,
            approved = true,
            conceptType = "MAID"
        ),
        Cafe(
            id = "cafe-7",
            name = "클로버 가든",
            desc = "성수의 조용한 가든풍 메이드카페",
            region = Region("KR", "Seoul", "성동구 연무장길", GeoPoint(37.545, 127.043)),
            thumbnailImage = null,
            ratingAvg = 4.4,
            reviewCount = 91,
            approved = true,
            conceptType = "MAID"
        ),
        Cafe(
            id = "cafe-8",
            name = "에뜨왈 라운지",
            desc = "명동 중심가의 프리미엄 메이드카페",
            region = Region("KR", "Seoul", "중구 명동길", GeoPoint(37.563, 126.985)),
            thumbnailImage = null,
            ratingAvg = 4.9,
            reviewCount = 204,
            approved = true,
            conceptType = "MAID"
        ),
        Cafe(
            id = "cafe-9",
            name = "민트 퍼레이드",
            desc = "노원의 캐주얼 메이드카페",
            region = Region("KR", "Seoul", "노원구 상계로", GeoPoint(37.654, 127.060)),
            thumbnailImage = null,
            ratingAvg = 4.3,
            reviewCount = 76,
            approved = true,
            conceptType = "MAID"
        ),
        Cafe(
            id = "cafe-10",
            name = "오로라 티룸",
            desc = "이대 앞 티룸 스타일 메이드카페",
            region = Region("KR", "Seoul", "서대문구 이화여대길", GeoPoint(37.561, 126.946)),
            thumbnailImage = null,
            ratingAvg = 4.7,
            reviewCount = 143,
            approved = true,
            conceptType = "MAID"
        ),
        Cafe(
            id = "cafe-11",
            name = "벨벳 스테이지",
            desc = "혜화 공연 콘셉트 메이드카페",
            region = Region("KR", "Seoul", "종로구 대학로", GeoPoint(37.582, 127.002)),
            thumbnailImage = null,
            ratingAvg = 4.6,
            reviewCount = 109,
            approved = true,
            conceptType = "MAID"
        )
    )
    override val casts = mutableListOf(
        Cast("maid-1", "cafe-1", "사쿠라", "user-2", null, "메이드 하우스 대표 메이드", "2001-03-11", "maid", 1234, 4.9),
        Cast("maid-2", "cafe-2", "미유", null, null, "핑크 캐슬 시그니처 메이드", "2002-04-10", "maid", 987, 4.8),
        Cast("maid-3", "cafe-3", "유이", null, null, "리본 카페 인기 메이드", "2000-05-14", "maid", 856, 4.7),
        Cast("maid-4", "cafe-2", "나나", null, null, "생일 이벤트 진행 중", "2001-03-05", "maid", 700, 4.8),
        Cast("maid-5", "cafe-1", "레이", null, null, "생일 위크", "2003-03-05", "maid", 620, 4.6),
        Cast("maid-6", "cafe-3", "미키", null, null, "생일 한정 출근", "2002-03-05", "maid", 540, 4.5),
        *maidHouseAdditionalCasts.toTypedArray()
    )

    override val banners = listOf(
        HomeBanner("banner-1", "3월 특별 이벤트", "F8A3C5", "F76C9E"),
        HomeBanner("banner-2", "신규 메이드 입점", "FFC2A7", "FF8F7A"),
        HomeBanner("banner-3", "주말 예약 오픈", "B6A5FF", "7E88FF")
    )

    override val notices = listOf(
        Notice("notice-1", "cafe-1", "메이드 하우스", "3월 특별 이벤트", "3월 특별 이벤트 진행 중!", "2026-03-05T07:00:00Z", "2시간 전"),
        Notice("notice-2", "cafe-2", "핑크 캐슬", "신규 메이드 입장", "신규 메이드 입장! 많은 관심 부탁드려요", "2026-03-05T04:00:00Z", "5시간 전"),
        Notice("notice-3", "cafe-3", "리본 카페", "주말 예약 마감", "주말 예약이 마감되었습니다", "2026-03-04T09:00:00Z", "1일 전")
    )

    override val cafeDetailsById = cafes.associate { cafe ->
        cafe.id to buildCafeDetail(cafe)
    }.toMutableMap()
    private val cafeDetailsState = MutableStateFlow(cafeDetailsById.toMap())
    private val cafeCastVersionState = MutableStateFlow(
        cafes.associate { it.id to 0 }
    )
    private val castVersionState = MutableStateFlow(
        casts.associate { it.id to 0 }
    )
    private val castImagesById = mutableMapOf(
        "maid-1" to listOf("", ""),
        "maid-2" to listOf("", ""),
        "maid-3" to listOf("", ""),
        "maid-4" to listOf("", ""),
        "maid-5" to listOf("", ""),
        "maid-6" to listOf("", "")
    ).apply {
        maidHouseAdditionalCasts.forEach { cast ->
            this[cast.id] = emptyList()
        }
    }
    private val castSchedulesByCastId = mutableMapOf(
        "maid-1" to defaultCastSchedules("maid-1", "cafe-1", listOf("MONDAY", "TUESDAY")),
        "maid-2" to defaultCastSchedules("maid-2", "cafe-2", listOf("MONDAY", "WEDNESDAY")),
        "maid-3" to defaultCastSchedules("maid-3", "cafe-3", listOf("TUESDAY", "THURSDAY")),
        "maid-4" to defaultCastSchedules("maid-4", "cafe-2", listOf("WEDNESDAY", "FRIDAY")),
        "maid-5" to defaultCastSchedules("maid-5", "cafe-1", listOf("MONDAY", "FRIDAY")),
        "maid-6" to defaultCastSchedules("maid-6", "cafe-3", listOf("THURSDAY", "SATURDAY"))
    ).apply {
        maidHouseAdditionalCasts.forEachIndexed { index, cast ->
            this[cast.id] = defaultCastSchedules(
                cast.id,
                cast.cafeId,
                maidHouseWorkingDaysByIndex(index)
            )
        }
    }

    override val reviews = mutableListOf(
        Review("review-1", "user-1", "cafe-1", 4.5f, "분위기가 좋아요", emptyList(), 3, "2026-03-03T10:00:00Z"),
        Review("review-2", "user-1", "cafe-2", 5.0f, "친절하고 재밌었어요", emptyList(), 5, "2026-03-04T14:00:00Z")
    )

    override val visits = mutableListOf(
        Visit("visit-1", "user-1", "cafe-1", "2026-03-09T08:30:00Z", "오픈 시간에 맞춰 방문", true),
        Visit("visit-2", "user-1", "cafe-2", "2026-03-09T13:15:00Z", "신규 메이드 이벤트 확인", true),
        Visit("visit-3", "user-1", "cafe-3", "2026-03-09T18:40:00Z", "저녁 타임 분위기 좋음", true),
        Visit("visit-4", "user-1", "cafe-6", "2026-03-08T20:10:00Z", "체리 시즌 메뉴 주문", true),
        Visit("visit-5", "user-1", "cafe-8", "2026-03-07T15:25:00Z", "명동 일정 중 방문", true)
    )

    override val notifications = mutableListOf(
        AppNotification("noti-1", "user-1", "사쿠라님이 출근했어요", "메이드 하우스에서 만나보세요!", "CAST_SHIFT", "maid-1", false, "2026-03-09T09:50:00Z", "10분 전"),
        AppNotification("noti-2", "user-1", "유이님이 출근했어요", "리본 카페에서 만나보세요!", "CAST_SHIFT", "maid-3", false, "2026-03-09T09:00:00Z", "1시간 전"),
        AppNotification("noti-3", "user-1", "미유님의 생일이에요", "축하 메시지를 남겨보세요!", "BIRTHDAY", "maid-2", false, "2026-03-09T08:00:00Z", "2시간 전"),
        AppNotification("noti-4", "user-1", "나나 생일 위크가 시작됐어요", "이번 주 생일 한정 이벤트를 확인해보세요.", "BIRTHDAY", "maid-4", true, "2026-03-08T12:00:00Z", "1일 전"),
        AppNotification("noti-5", "user-1", "메이드 하우스 공지", "3월 특별 이벤트가 시작되었어요!", "CAFE_NOTICE", "cafe-1", false, "2026-03-09T06:00:00Z", "4시간 전"),
        AppNotification("noti-6", "user-1", "핑크 캐슬 공지", "신규 메이드 입장 안내를 확인하세요.", "CAFE_NOTICE", "cafe-2", true, "2026-03-08T09:00:00Z", "1일 전"),
        AppNotification("noti-7", "user-1", "새로운 팔로워", "메이드팬123님이 회원님을 팔로우했어요.", "FOLLOW_UPDATE", null, false, "2026-03-09T05:00:00Z", "5시간 전"),
        AppNotification("noti-8", "user-1", "팬클럽 가입 알림", "리본러버님이 회원님을 새로 팔로우했어요.", "FOLLOW_UPDATE", null, true, "2026-03-07T08:00:00Z", "2일 전")
    )

    override val favoriteCafeIdsByUser = mutableMapOf("user-1" to mutableSetOf("cafe-1"))

    override val followedCastIdsByUser = mutableMapOf(
        "user-1" to mutableSetOf("maid-1"),
        "user-6" to mutableSetOf("maid-1"),
        "user-7" to mutableSetOf("maid-1"),
        "user-8" to mutableSetOf("maid-1")
    )

    override val ownedCafeIdsByUser = mapOf(
        "user-3" to listOf("cafe-1", "cafe-2", "cafe-3")
    )

    override val pendingCafeClaimsByUser = mapOf(
        "user-3" to listOf(
            CafeManagementData.PendingClaimSummary(
                cafeName = "Ribbon Cafe Hongdae",
                requestedAt = "2026.03.10",
                status = "승인 대기 중",
                message = "관리자 승인 후 내 카페 목록에 자동 연결됩니다"
            )
        ),
        "user-5" to listOf(
            CafeManagementData.PendingClaimSummary(
                cafeName = "Pink Castle Sinchon",
                requestedAt = "2026.03.09",
                status = "승인 대기 중",
                message = "기존 카페 운영자 신청이 검토 중입니다"
            )
        )
    )

    override val cafeCheckInCountById = mapOf(
        "cafe-1" to 482,
        "cafe-2" to 451,
        "cafe-8" to 429,
        "cafe-6" to 410,
        "cafe-3" to 384,
        "cafe-10" to 331,
        "cafe-4" to 298,
        "cafe-5" to 276,
        "cafe-11" to 243,
        "cafe-7" to 219,
        "cafe-9" to 187
    )

    override val cafeTodayCheckInCountById = mapOf(
        "cafe-1" to 12,
        "cafe-2" to 7,
        "cafe-3" to 0
    )

    override val cafeTodayReviewCountById = mapOf(
        "cafe-1" to 3,
        "cafe-2" to 1,
        "cafe-3" to 0
    )

    override val onShiftCastIdsByCafeId = mapOf(
        "cafe-1" to setOf("maid-1", "maid-5", "maid-7", "maid-12", "maid-18", "maid-24", "maid-31"),
        "cafe-2" to setOf("maid-2", "maid-4"),
        "cafe-3" to emptySet()
    )

    override val cafeHomeBannerPreviewByCafeId = mapOf(
        "cafe-1" to CafeDashboardData.HomeBannerPreview(
            title = "여름 한정 신메뉴 출시!",
            period = "2026.06.01 - 2026.08.31",
            statusLabel = "노출 중"
        ),
        "cafe-2" to CafeDashboardData.HomeBannerPreview(
            title = "주말 콜라보 디저트 오픈",
            period = "2026.03.14 - 2026.03.31",
            statusLabel = "예약 중"
        ),
        "cafe-3" to CafeDashboardData.HomeBannerPreview(
            title = "신규 오픈 안내 배너",
            period = "2026.03.20 - 2026.04.20",
            statusLabel = "검수 중"
        )
    )

    override val castTodayVisitCountById = mapOf(
        "maid-1" to 94,
        "maid-2" to 88,
        "maid-3" to 74,
        "maid-4" to 63,
        "maid-5" to 58,
        "maid-6" to 49
    )

    override fun defaultMyPageSummary(userId: String): MyPageSummary {
        val favoritesCount = favoriteCafeIdsByUser[userId]?.size ?: 0
        val followedCount = followedCastIdsByUser[userId]?.size ?: 0
        val visitCount = visits.count { it.userId == userId }
        return MyPageSummary(userId, visitCount, favoritesCount, followedCount, badgesCount = 3, level = 4)
    }

    override fun cafeDetail(cafeId: String): CafeDetail? {
        return cafeDetailsById[cafeId]
    }

    override fun observeCafeDetail(cafeId: String): Flow<CafeDetail?> {
        return cafeDetailsState.asStateFlow().map { detailsById -> detailsById[cafeId] }
    }

    override fun observeCafeCastVersion(cafeId: String): Flow<Int> {
        return cafeCastVersionState.asStateFlow().map { it[cafeId] ?: 0 }
    }

    override fun observeCastVersion(castId: String): Flow<Int> {
        return castVersionState.asStateFlow().map { it[castId] ?: 0 }
    }

    override fun updateCafeInfo(update: CafeInfoUpdate): CafeDetail {
        val cafeIndex = cafes.indexOfFirst { it.id == update.cafeId }
        if (cafeIndex == -1) {
            throw NoSuchElementException("cafe not found")
        }
        val currentCafe = cafes[cafeIndex]
        val currentDetail = cafeDetailsById[update.cafeId] ?: buildCafeDetail(currentCafe)
        val updatedCafe = currentCafe.copy(
            name = update.name,
            desc = update.description,
            region = currentCafe.region.copy(address = update.address)
        )
        val updatedDetail = currentDetail.copy(
            cafe = updatedCafe,
            businessHours = formatBusinessHours(update),
            phoneNumber = update.contactNumber
        )
        cafes[cafeIndex] = updatedCafe
        cafeDetailsById[update.cafeId] = updatedDetail
        cafeDetailsState.value = cafeDetailsById.toMap()
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
        cafeDetailsState.value = cafeDetailsById.toMap()
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
        cafeDetailsState.value = cafeDetailsById.toMap()
        return updatedDetail
    }

    private fun buildCafeDetail(cafe: Cafe): CafeDetail {
        val cafeCasts = casts.filter { it.cafeId == cafe.id }
        val cafeNotices = notices.filter { it.cafeId == cafe.id }
        return CafeDetail(
            cafe = cafe,
            images = listOf(
                "https://images.unsplash.com/photo-1714889988208-1ea67650789f?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=800",
                "https://images.unsplash.com/photo-1699275509309-0764566eef5d?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=800"
            ),
            casts = cafeCasts,
            menus = listOf(
                CafeMenu(
                    "menu-1",
                    "딸기 파르페",
                    12000,
                    "대표 디저트",
                    "https://images.unsplash.com/photo-1766043650707-49e74514218a?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=400",
                    "food",
                    false
                ),
                CafeMenu(
                    "menu-2",
                    "핑크 라떼",
                    8000,
                    "시그니처 음료",
                    "https://images.unsplash.com/photo-1766043650707-49e74514218a?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=400",
                    "drink"
                ),
                CafeMenu(
                    "menu-3",
                    "오므라이스",
                    13500,
                    "메이드 카페 클래식 인기 메뉴",
                    null,
                    "food"
                ),
                CafeMenu(
                    "menu-4",
                    "체리 에이드",
                    7500,
                    "상큼한 탄산 시그니처 드링크",
                    null,
                    "drink",
                    false
                ),
                CafeMenu(
                    "menu-5",
                    "리본 케이크",
                    9800,
                    "핑크 크림으로 마무리한 디저트",
                    null,
                    "dessert"
                ),
                CafeMenu(
                    "menu-6",
                    "카레 라이스",
                    12800,
                    "부드러운 일본식 카레",
                    null,
                    "food"
                ),
                CafeMenu(
                    "menu-7",
                    "바닐라 밀크티",
                    8200,
                    "달콤한 향이 강한 인기 메뉴",
                    null,
                    "drink"
                ),
                CafeMenu(
                    "menu-8",
                    "초코 브라우니",
                    6800,
                    "따뜻하게 제공되는 진한 초콜릿 디저트",
                    null,
                    "dessert",
                    false
                ),
                CafeMenu(
                    "menu-9",
                    "나폴리탄",
                    14200,
                    "레트로 감성의 토마토 파스타",
                    null,
                    "food"
                ),
                CafeMenu(
                    "menu-10",
                    "화이트 모카",
                    7900,
                    "부드러운 크림과 에스프레소 조합",
                    null,
                    "drink"
                ),
                CafeMenu(
                    "menu-11",
                    "허니 토스트",
                    11500,
                    "둘이 나눠 먹기 좋은 시그니처 토스트",
                    null,
                    "dessert"
                ),
                CafeMenu(
                    "menu-12",
                    "복숭아 아이스티",
                    7000,
                    "깔끔하고 가벼운 베이직 음료",
                    null,
                    "drink"
                )
            ),
            goods = listOf(
                Goods("goods-1", "랜덤 포토카드", 5000, null, 50)
            ),
            notices = cafeNotices,
            businessHours = "매일 11:00 - 22:00",
            phoneNumber = "02-1234-5678"
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

    override fun castDetail(castId: String): CastDetail? {
        val cast = casts.firstOrNull { it.id == castId } ?: return null
        val cafe = cafes.firstOrNull { it.id == cast.cafeId } ?: return null

        return CastDetail(
            cast = cast,
            cafe = cafe,
            images = castImagesById[cast.id].orEmpty(),
            schedule = castSchedulesByCastId[cast.id].orEmpty()
        )
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
        val castId = existingCast?.id ?: nextId("maid", casts.map { it.id })
        val nextCast = Cast(
            id = castId,
            cafeId = targetCafeId,
            name = update.name.trim(),
            linkedUserId = existingCast?.linkedUserId ?: currentUserId,
            profileImage = existingCast?.profileImage,
            desc = update.introduction.trim(),
            birthday = normalizedBirthday,
            conceptRole = update.conceptRole.trim(),
            followerCount = existingCast?.followerCount ?: 0,
            rating = existingCast?.rating ?: 0.0
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

        if (castImagesById[castId] == null) {
            castImagesById[castId] = listOfNotNull(nextCast.profileImage)
        }
        castSchedulesByCastId[castId] = buildCastSchedules(castId, targetCafeId, update.workingDays)
        cafeCastVersionState.value = cafeCastVersionState.value.toMutableMap().apply {
            this[targetCafeId] = (this[targetCafeId] ?: 0) + 1
        }
        castVersionState.value = castVersionState.value.toMutableMap().apply {
            this[castId] = (this[castId] ?: 0) + 1
        }

        return CastDetail(
            cast = nextCast,
            cafe = targetCafe,
            images = castImagesById[castId].orEmpty(),
            schedule = castSchedulesByCastId[castId].orEmpty()
        )
    }

    override fun rankingItemsFromCasts(): List<RankingItem> {
        return casts
            .sortedByDescending { it.followerCount }
            .mapIndexed { index, cast ->
                RankingItem(cast.id, cast.name, cast.followerCount, index + 1, cast.profileImage)
            }
    }

    override fun rankingItemsFromCafes(): List<RankingItem> {
        return cafes
            .sortedByDescending { it.ratingAvg }
            .mapIndexed { index, cafe ->
                RankingItem(cafe.id, cafe.name, (cafe.ratingAvg * 100).toInt(), index + 1, cafe.thumbnailImage)
            }
    }

    override fun verifyVisitResult(cafeId: String, latitude: Double, longitude: Double): VisitVerificationResult {
        val cafe = cafes.firstOrNull { it.id == cafeId }
        val cafeLat = cafe?.region?.location?.latitude ?: latitude
        val cafeLon = cafe?.region?.location?.longitude ?: longitude
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
}

private fun defaultCastSchedules(castId: String, cafeId: String, workingDays: List<String>): List<CastSchedule> {
    return buildCastSchedules(castId, cafeId, workingDays)
}

private val maidHouseAdditionalCasts = listOf(
    "아카리", "하즈키", "마리", "코코", "루루", "시온", "히나", "노아", "세이라", "유즈",
    "린", "모모", "아오이", "하나", "이오리", "카논", "리리", "마호", "네네", "스즈",
    "미나", "카에데", "치카", "에리", "미오", "세나", "우이", "호노카", "리코", "유나",
    "사나", "코하루", "아야", "츠키", "루나", "미레", "키라", "토와", "미호", "유리",
    "아린", "나기사", "시로", "아이", "유카", "리사", "미카", "하루"
).mapIndexed { index, name ->
    val number = index + 7
    Cast(
        id = "maid-$number",
        cafeId = "cafe-1",
        name = name,
        linkedUserId = null,
        profileImage = null,
        desc = "메이드 하우스 인기 캐스트 $name",
        birthday = maidHouseBirthdayByIndex(index),
        conceptRole = maidHouseConceptRoleByIndex(index),
        followerCount = 580 - (index * 7),
        rating = 4.2 + ((index % 7) * 0.1)
    )
}

private fun maidHouseWorkingDaysByIndex(index: Int): List<String> {
    return when (index % 5) {
        0 -> listOf("MONDAY", "WEDNESDAY")
        1 -> listOf("TUESDAY", "THURSDAY")
        2 -> listOf("WEDNESDAY", "FRIDAY")
        3 -> listOf("THURSDAY", "SATURDAY")
        else -> listOf("FRIDAY", "SUNDAY")
    }
}

private fun maidHouseBirthdayByIndex(index: Int): String {
    val month = (index % 12) + 1
    val day = (index % 27) + 1
    return "200${index % 5}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
}

private fun maidHouseConceptRoleByIndex(index: Int): String {
    return when (index % 6) {
        0 -> "maid"
        1 -> "tea master"
        2 -> "dessert maid"
        3 -> "floor leader"
        4 -> "live maid"
        else -> "apprentice maid"
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
