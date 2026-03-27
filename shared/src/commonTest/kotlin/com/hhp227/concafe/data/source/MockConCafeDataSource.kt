package com.hhp227.concafe.data.source

import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.AppNotification
import com.hhp227.concafe.domain.model.BannerLinkTargetType
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
import com.hhp227.concafe.domain.model.CastClaimStatus
import com.hhp227.concafe.domain.model.CastDetail
import com.hhp227.concafe.domain.model.CastSchedule
import com.hhp227.concafe.domain.model.CastScheduleStatus
import com.hhp227.concafe.domain.model.CastScheduleUpdate
import com.hhp227.concafe.domain.model.CastUpsert
import com.hhp227.concafe.domain.model.ExternalLink
import com.hhp227.concafe.domain.model.GeoPoint
import com.hhp227.concafe.domain.model.Goods
import com.hhp227.concafe.domain.model.HomeBanner
import com.hhp227.concafe.domain.model.Inquiry
import com.hhp227.concafe.domain.model.CafeMenu
import com.hhp227.concafe.domain.model.MyPageSummary
import com.hhp227.concafe.domain.model.Notice
import com.hhp227.concafe.domain.model.NoticeStatusAccent
import com.hhp227.concafe.domain.model.RankingItem
import com.hhp227.concafe.domain.model.Region
import com.hhp227.concafe.domain.model.Review
import com.hhp227.concafe.domain.model.Stamp
import com.hhp227.concafe.domain.model.User
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.model.Visit
import com.hhp227.concafe.domain.model.VisitVerificationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class MockConCafeDataSource : ConCafeDataSource {
    private val _currentUserId = MutableStateFlow<String?>("user-1")

    override var currentUserId: String?
        get() = _currentUserId.value
        set(value) {
            _currentUserId.value = value
        }

    override val currentUserIdFlow: StateFlow<String?> = _currentUserId.asStateFlow()

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
        ),
        User(
            id = "user-9",
            email = "cast.pending@concafe.app",
            nickname = "마유",
            profileImage = null,
            role = UserRole.CAST,
            banned = false,
            createdAt = "2026-03-10T10:30:00Z"
        ),
        User(
            id = "user-10",
            email = "cast1@concafe.app",
            nickname = "유메",
            profileImage = null,
            role = UserRole.CAST,
            banned = false,
            createdAt = "2026-03-13T11:00:00Z"
        ),
        User(
            id = "user-11",
            email = "owner1@concafe.app",
            nickname = "카페신청전점장",
            profileImage = null,
            role = UserRole.CAFE_OWNER,
            banned = false,
            createdAt = "2026-03-14T09:00:00Z"
        )
    )

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
        return users.removeAll { user -> user.id == userId }
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
        ),
        *additionalMockCafes.toTypedArray()
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

    override val castClaims = mutableListOf(
        CastClaim(
            id = "cast-claim-1",
            userId = "user-9",
            cafeId = "cafe-1",
            castId = "maid-5",
            status = CastClaimStatus.PENDING,
            message = "현재 활동 중인 마유입니다. 레이 프로필과 연결 부탁드려요.",
            createdAt = "2026-03-12T09:00:00Z",
            createdAtLabel = "1일 전"
        )
    )

    override val banners = mutableListOf(
        HomeBanner("banner-1", "3월 특별 이벤트", "F8A3C5", "F76C9E", subtitle = "3월 한정 혜택을 확인해보세요", cafeId = "cafe-1", targetType = BannerLinkTargetType.EVENT_DETAIL, targetValue = "event-management-1", displayDays = 7),
        HomeBanner("banner-2", "신규 메이드 입점", "FFC2A7", "FF8F7A", subtitle = "핑크 캐슬 신규 캐스트 소식을 확인하세요", cafeId = "cafe-2", targetType = BannerLinkTargetType.NOTICE, targetValue = "notice-management-2", displayDays = 5),
        HomeBanner("banner-3", "주말 예약 오픈", "B6A5FF", "7E88FF", subtitle = "주말 예약 일정을 미리 확인하세요", cafeId = "cafe-3", targetType = BannerLinkTargetType.CAFE_DETAIL, targetValue = "cafe-3", displayDays = 3)
    )

    override val inquiries = mutableListOf<Inquiry>()

    override val notices = mutableListOf(
        Notice("notice-1", "cafe-1", "메이드 하우스", "3월 특별 이벤트", "3월 특별 이벤트 진행 중!", "2026-03-05T07:00:00Z", "2시간 전"),
        Notice("notice-2", "cafe-2", "핑크 캐슬", "신규 메이드 입장", "신규 메이드 입장! 많은 관심 부탁드려요", "2026-03-05T04:00:00Z", "5시간 전"),
        Notice("notice-3", "cafe-3", "리본 카페", "주말 예약 마감", "주말 예약이 마감되었습니다", "2026-03-04T09:00:00Z", "1일 전")
    )

    override val cafeNoticeManagementItems = buildCafeNoticeManagementItems().toMutableList()

    override val cafeEventManagementItems = buildCafeEventManagementItems().toMutableList()

    override val cafeDetailsById = cafes.associate { cafe ->
        cafe.id to buildCafeDetail(cafe)
    }.toMutableMap()
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
        "maid-1" to buildCastSchedules("maid-1", "cafe-1", listOf("MONDAY", "TUESDAY")),
        "maid-2" to buildCastSchedules("maid-2", "cafe-2", listOf("MONDAY", "WEDNESDAY")),
        "maid-3" to buildCastSchedules("maid-3", "cafe-3", listOf("TUESDAY", "THURSDAY")),
        "maid-4" to buildCastSchedules("maid-4", "cafe-2", listOf("WEDNESDAY", "FRIDAY")),
        "maid-5" to buildCastSchedules("maid-5", "cafe-1", listOf("MONDAY", "FRIDAY")),
        "maid-6" to buildCastSchedules("maid-6", "cafe-3", listOf("THURSDAY", "SATURDAY"))
    ).apply {
        maidHouseAdditionalCasts.forEachIndexed { index, cast ->
            this[cast.id] = buildCastSchedules(
                cast.id,
                cast.cafeId,
                maidHouseWorkingDaysByIndex(index)
            )
        }
    }
    override val castScheduleStatusByCastId = buildCastScheduleStatusesByCastId(castSchedulesByCastId)

    override val reviews = mutableListOf(
        Review("review-1", "user-1", "cafe-1", "visit-1", 5.0f, "사쿠라가 응대도 좋고 전체 분위기도 정말 만족스러웠어요. 재방문 의사 있습니다.", emptyList(), listOf("maid-1"), 12, "2026-03-09T19:00:00Z"),
        Review("review-2", "user-6", "cafe-1", "visit-1", 4.0f, "디저트가 맛있고 사쿠라 태그 남기고 싶을 만큼 기억에 남는 방문이었어요.", emptyList(), listOf("maid-1"), 4, "2026-03-09T18:40:00Z"),
        Review("review-3", "user-7", "cafe-1", "visit-1", 5.0f, "좌석 간격도 편했고 사쿠라가 있는 타임 분위기가 특히 좋았습니다.", emptyList(), listOf("maid-1"), 7, "2026-03-09T18:20:00Z"),
        Review("review-4", "user-8", "cafe-1", "visit-1", 4.0f, "시그니처 음료가 괜찮았고 사쿠라 태그 후기 남길 정도로 응대가 인상적이었어요.", emptyList(), listOf("maid-1"), 3, "2026-03-09T18:00:00Z"),
        Review("review-5", "user-1", "cafe-1", "visit-1", 5.0f, "주말 오픈 직후 방문했는데 사쿠라 포함 전체 서비스가 안정적이었어요.", emptyList(), listOf("maid-1"), 9, "2026-03-09T17:40:00Z"),
        Review("review-6", "user-6", "cafe-1", "visit-1", 4.0f, "매장 음악 볼륨이 적당했고 사쿠라 태그를 남기고 싶을 정도로 친절했습니다.", emptyList(), listOf("maid-1"), 5, "2026-03-09T17:20:00Z"),
        Review("review-7", "user-7", "cafe-1", "visit-1", 5.0f, "오므라이스와 체리 에이드 조합이 좋았고 사쿠라 응대도 만족스러웠어요.", emptyList(), listOf("maid-1"), 6, "2026-03-09T17:00:00Z"),
        Review("review-8", "user-8", "cafe-1", "visit-1", 4.0f, "대기 시간이 길지 않았고 사쿠라가 있는 시간대 분위기가 편안했어요.", emptyList(), listOf("maid-1"), 2, "2026-03-09T16:40:00Z"),
        Review("review-9", "user-1", "cafe-1", "visit-1", 5.0f, "사진 찍기 좋은 포인트가 많고 사쿠라 태그 후기 남길 만큼 기억에 남았습니다.", emptyList(), listOf("maid-1"), 11, "2026-03-09T16:20:00Z"),
        Review("review-10", "user-6", "cafe-1", "visit-1", 4.0f, "첫 방문이었는데 사쿠라 덕분에 입문하기 좋은 카페라는 인상을 받았어요.", emptyList(), listOf("maid-1"), 4, "2026-03-09T16:00:00Z"),
        Review("review-11", "user-7", "cafe-1", "visit-1", 5.0f, "전체 연출이 과하지 않고 사쿠라 태그를 남기고 싶을 정도로 밸런스가 좋았어요.", emptyList(), listOf("maid-1"), 8, "2026-03-09T15:40:00Z"),
        Review("review-12", "user-8", "cafe-1", "visit-1", 4.0f, "디저트 플레이팅이 예쁘고 사쿠라가 응대한 테이블 분위기도 좋았습니다.", emptyList(), listOf("maid-1"), 3, "2026-03-09T15:20:00Z"),
        Review("review-13", "user-1", "cafe-1", "visit-1", 5.0f, "재방문했는데도 만족도가 높았고 사쿠라 태그를 꼭 남기고 싶은 날이었어요.", emptyList(), listOf("maid-1"), 10, "2026-03-09T15:00:00Z"),
        Review("review-14", "user-6", "cafe-1", "visit-1", 4.0f, "매장 청결도와 서비스 템포가 좋았고 사쿠라 응대가 안정적이었습니다.", emptyList(), listOf("maid-1"), 4, "2026-03-09T14:40:00Z"),
        Review("review-15", "user-7", "cafe-1", "visit-1", 5.0f, "친구와 방문했는데 사쿠라 태그 후기 남길 정도로 전체 경험이 좋았어요.", emptyList(), listOf("maid-1"), 7, "2026-03-09T14:20:00Z"),
        Review("review-16", "user-8", "cafe-1", "visit-1", 4.0f, "브라우니가 맛있었고 사쿠라와 함께한 시간대 응대가 자연스러웠습니다.", emptyList(), listOf("maid-1"), 2, "2026-03-09T14:00:00Z"),
        Review("review-17", "user-1", "cafe-1", "visit-1", 5.0f, "사쿠라 태그를 남기지 않기 아쉬울 정도로 전체 연출과 서비스가 좋았어요.", emptyList(), listOf("maid-1"), 9, "2026-03-09T13:40:00Z"),
        Review("review-18", "user-6", "cafe-1", "visit-1", 4.0f, "음료 나오는 속도가 빨랐고 사쿠라 응대도 깔끔해서 만족했습니다.", emptyList(), listOf("maid-1"), 3, "2026-03-09T13:20:00Z"),
        Review("review-19", "user-7", "cafe-1", "visit-1", 5.0f, "좌석이 편하고 대화하기 좋았으며 사쿠라 태그 후기로 남길 만한 방문이었어요.", emptyList(), listOf("maid-1"), 6, "2026-03-09T13:00:00Z"),
        Review("review-20", "user-8", "cafe-1", "visit-1", 4.0f, "적당히 활기찬 분위기라 좋았고 사쿠라가 있는 시간대 만족도가 높았습니다.", emptyList(), listOf("maid-1"), 2, "2026-03-09T12:40:00Z"),
        Review("review-21", "user-1", "cafe-1", "visit-1", 5.0f, "메뉴 설명이 친절했고 사쿠라 태그 후기를 남기고 싶을 정도로 응대가 좋았어요.", emptyList(), listOf("maid-1"), 8, "2026-03-09T12:20:00Z"),
        Review("review-22", "user-6", "cafe-1", "visit-1", 4.0f, "처음엔 긴장했는데 사쿠라 덕분에 편하게 즐기고 왔습니다.", emptyList(), listOf("maid-1"), 4, "2026-03-09T12:00:00Z"),
        Review("review-23", "user-7", "cafe-1", "visit-1", 5.0f, "사쿠라 태그와 함께 남기고 싶은 정도로 이날 전체 접객 흐름이 좋았습니다.", emptyList(), listOf("maid-1"), 7, "2026-03-09T11:40:00Z"),
        Review("review-24", "user-8", "cafe-1", "visit-1", 4.0f, "카페 컨셉이 선명하고 사쿠라 응대가 특히 기억에 남아요.", emptyList(), listOf("maid-1"), 3, "2026-03-09T11:20:00Z"),
        Review("review-25", "user-1", "cafe-1", "visit-1", 5.0f, "사쿠라 태그 리뷰를 남기기 위해 다시 생각날 만큼 기분 좋은 방문이었어요.", emptyList(), listOf("maid-1"), 9, "2026-03-09T11:00:00Z"),
        Review("review-26", "user-6", "cafe-1", "visit-1", 4.0f, "주문 동선이 매끄럽고 사쿠라가 있는 타임의 텐션이 안정적이었습니다.", emptyList(), listOf("maid-1"), 3, "2026-03-08T20:40:00Z"),
        Review("review-27", "user-7", "cafe-1", "visit-1", 5.0f, "친구 추천으로 갔는데 사쿠라 태그 남길 만큼 서비스 경험이 좋았어요.", emptyList(), listOf("maid-1"), 8, "2026-03-08T20:20:00Z"),
        Review("review-28", "user-8", "cafe-1", "visit-1", 4.0f, "대기 후 입장했지만 사쿠라 응대 덕분에 피로감이 덜했습니다.", emptyList(), listOf("maid-1"), 2, "2026-03-08T20:00:00Z"),
        Review("review-29", "user-1", "cafe-1", "visit-1", 5.0f, "사쿠라 태그와 함께 남기는 후기답게 응대와 분위기 모두 만족이에요.", emptyList(), listOf("maid-1"), 10, "2026-03-08T19:40:00Z"),
        Review("review-30", "user-6", "cafe-1", "visit-1", 4.0f, "메뉴 가격 대비 만족도가 높고 사쿠라가 있는 시간대가 특히 좋았습니다.", emptyList(), listOf("maid-1"), 4, "2026-03-08T19:20:00Z"),
        Review("review-31", "user-7", "cafe-1", "visit-1", 5.0f, "사진보다 실물이 더 예쁜 공간이었고 사쿠라 응대도 기대 이상이었어요.", emptyList(), listOf("maid-1"), 6, "2026-03-08T19:00:00Z"),
        Review("review-32", "user-8", "cafe-1", "visit-1", 4.0f, "사쿠라 태그 후기 남길 정도로 첫 응대 인상이 좋았던 카페입니다.", emptyList(), listOf("maid-1"), 3, "2026-03-08T18:40:00Z"),
        Review("review-33", "user-1", "cafe-1", "visit-1", 5.0f, "공간 연출과 서비스 톤이 잘 맞았고 사쿠라 덕분에 더 즐거웠어요.", emptyList(), listOf("maid-1"), 9, "2026-03-08T18:20:00Z"),
        Review("review-34", "user-6", "cafe-1", "visit-1", 4.0f, "한적한 시간대라 여유롭게 즐겼고 사쿠라 태그를 남기고 싶었습니다.", emptyList(), listOf("maid-1"), 2, "2026-03-08T18:00:00Z"),
        Review("review-35", "user-7", "cafe-1", "visit-1", 5.0f, "사쿠라 태그와 함께 추천하고 싶은 카페예요. 메뉴와 접객이 안정적입니다.", emptyList(), listOf("maid-1"), 7, "2026-03-08T17:40:00Z"),
        Review("review-36", "user-8", "cafe-1", "visit-1", 4.0f, "디저트 퀄리티가 기대 이상이었고 사쿠라 응대도 부드러웠어요.", emptyList(), listOf("maid-1"), 3, "2026-03-08T17:20:00Z"),
        Review("review-37", "user-1", "cafe-1", "visit-1", 5.0f, "사쿠라 태그를 붙인 이유가 분명할 만큼 전체 경험이 선명하게 좋았습니다.", emptyList(), listOf("maid-1"), 11, "2026-03-08T17:00:00Z"),
        Review("review-38", "user-6", "cafe-1", "visit-1", 4.0f, "조명이 예쁘고 사진도 잘 나와서 사쿠라 태그 후기 남기기 좋았어요.", emptyList(), listOf("maid-1"), 4, "2026-03-08T16:40:00Z"),
        Review("review-39", "user-7", "cafe-1", "visit-1", 5.0f, "친절한 설명과 안정적인 서비스 덕분에 사쿠라 태그 후기를 남깁니다.", emptyList(), listOf("maid-1"), 8, "2026-03-08T16:20:00Z"),
        Review("review-40", "user-8", "cafe-1", "visit-1", 4.0f, "적당히 활기차고 부담 없는 분위기라 사쿠라 응대가 더 돋보였어요.", emptyList(), listOf("maid-1"), 2, "2026-03-08T16:00:00Z"),
        Review("review-41", "user-1", "cafe-1", "visit-1", 5.0f, "사쿠라 태그 후기를 남기면서도 카페 전체 만족도가 높다고 말할 수 있어요.", emptyList(), listOf("maid-1"), 9, "2026-03-08T15:40:00Z"),
        Review("review-42", "user-6", "cafe-1", "visit-1", 4.0f, "재방문 의사 있고 사쿠라가 있는 날 다시 오고 싶을 정도였습니다.", emptyList(), listOf("maid-1"), 3, "2026-03-08T15:20:00Z"),
        Review("review-43", "user-7", "cafe-1", "visit-1", 5.0f, "메뉴 구성, 응대, 공간 분위기까지 전반적으로 좋아 사쿠라 태그를 달았어요.", emptyList(), listOf("maid-1"), 6, "2026-03-08T15:00:00Z"),
        Review("review-44", "user-8", "cafe-1", "visit-1", 4.0f, "크게 시끄럽지 않아 대화하기 좋았고 사쿠라 응대가 자연스러웠습니다.", emptyList(), listOf("maid-1"), 2, "2026-03-08T14:40:00Z"),
        Review("review-45", "user-1", "cafe-1", "visit-1", 5.0f, "사쿠라 태그와 함께 남길 정도로 방문 경험 전체가 선명하게 좋았어요.", emptyList(), listOf("maid-1"), 10, "2026-03-08T14:20:00Z"),
        Review("review-46", "user-6", "cafe-1", "visit-1", 4.0f, "메뉴 추천이 좋았고 사쿠라 응대도 차분해서 만족했습니다.", emptyList(), listOf("maid-1"), 4, "2026-03-08T14:00:00Z"),
        Review("review-47", "user-7", "cafe-1", "visit-1", 5.0f, "처음 방문한 친구도 만족했고 사쿠라 태그 후기를 같이 남기고 싶었어요.", emptyList(), listOf("maid-1"), 7, "2026-03-08T13:40:00Z"),
        Review("review-48", "user-8", "cafe-1", "visit-1", 4.0f, "사쿠라가 태그된 후기답게 응대 인상이 좋았고 카페도 깔끔했습니다.", emptyList(), listOf("maid-1"), 3, "2026-03-08T13:20:00Z"),
        Review("review-49", "user-1", "cafe-1", "visit-1", 5.0f, "공간과 서비스가 잘 맞물려서 사쿠라 태그와 함께 추천하고 싶은 카페예요.", emptyList(), listOf("maid-1"), 8, "2026-03-08T13:00:00Z"),
        Review("review-50", "user-6", "cafe-1", "visit-1", 4.0f, "마무리까지 만족스러운 방문이었고 사쿠라 태그 후기 남기고 갑니다.", emptyList(), listOf("maid-1"), 4, "2026-03-08T12:40:00Z"),
        Review("review-51", "user-1", "cafe-2", "visit-2", 5.0f, "친절하고 재밌었어요", emptyList(), listOf("maid-3"), 5, "2026-03-04T14:00:00Z")
    )

    override val visits = mutableListOf(
        Visit("visit-1", "user-1", "cafe-1", "2026-03-09T08:30:00Z", "오픈 시간에 맞춰 방문", true),
        Visit("visit-2", "user-1", "cafe-2", "2026-03-09T13:15:00Z", "신규 메이드 이벤트 확인", true),
        Visit("visit-3", "user-1", "cafe-3", "2026-03-09T18:40:00Z", "저녁 타임 분위기 좋음", true),
        Visit("visit-4", "user-1", "cafe-4", "2026-03-08T17:20:00Z", "퇴근 후 방문", true),
        Visit("visit-5", "user-1", "cafe-5", "2026-03-08T19:00:00Z", "주말 메뉴 확인", true),
        Visit("visit-6", "user-1", "cafe-6", "2026-03-08T20:10:00Z", "체리 시즌 메뉴 주문", true),
        Visit("visit-7", "user-1", "cafe-7", "2026-03-07T16:40:00Z", "가든 분위기 확인", true),
        Visit("visit-8", "user-1", "cafe-8", "2026-03-07T15:25:00Z", "명동 일정 중 방문", true),
        Visit("visit-9", "user-1", "cafe-9", "2026-03-06T18:05:00Z", "캐주얼 타임 방문", true),
        Visit("visit-10", "user-1", "cafe-10", "2026-03-06T14:30:00Z", "티룸 콘셉트 체험", true),
        Visit("visit-11", "user-1", "cafe-11", "2026-03-05T19:10:00Z", "공연 콘셉트 카페 방문", true)
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
    override val favoriteUserIdsByCafeId = mutableMapOf(
        "cafe-1" to mutableSetOf("user-1")
    )

    override val followedCastIdsByUser = mutableMapOf(
        "user-1" to mutableSetOf("maid-1"),
        "user-6" to mutableSetOf("maid-1"),
        "user-7" to mutableSetOf("maid-1"),
        "user-8" to mutableSetOf("maid-1")
    )
    override val followerUserIdsByCastId = mutableMapOf(
        "maid-1" to mutableSetOf("user-1", "user-6", "user-7", "user-8")
    )

    override val cafeExternalLinksByCafeId = mutableMapOf(
        "cafe-1" to mutableListOf(
            ExternalLink(
                id = "cafe-link-1",
                platform = "INSTAGRAM",
                title = "메이드 하우스 인스타",
                url = "https://instagram.com/maidhouse",
                isVisible = true,
                sortOrder = 1,
                createdAt = "2026-03-01T09:00:00Z",
                updatedAt = "2026-03-01T09:00:00Z"
            )
        )
    )

    override val castExternalLinksByCastId = mutableMapOf(
        "maid-1" to mutableListOf(
            ExternalLink(
                id = "cast-link-1",
                platform = "X",
                title = "사쿠라 X",
                url = "https://x.com/sakura_concafe",
                isVisible = true,
                sortOrder = 1,
                createdAt = "2026-03-01T09:00:00Z",
                updatedAt = "2026-03-01T09:00:00Z"
            )
        )
    )

    override val stamps = mutableListOf(
        Stamp(
            id = "stamp-1",
            userId = "user-1",
            cafeId = "cafe-1",
            visitId = "visit-1",
            earnedAt = "2026-03-09T08:30:00Z"
        )
    )

    override val dismissedReviewPromptVisitIdsByUser = mutableMapOf<String, MutableSet<String>>()

    override val ownedCafeIdsByUser = mutableMapOf(
        "user-3" to mutableListOf("cafe-1", "cafe-2", "cafe-3")
    )

    override val pendingCafeClaimsByUser = mutableMapOf(
        "user-3" to mutableListOf(
            CafeManagementData.PendingClaimSummary(
                claimId = "cafe-claim-1",
                cafeId = "cafe-4",
                cafeName = "슈가 드롭",
                requestedAt = "2026.03.10",
                status = "승인 대기 중",
                message = "관리자 승인 후 내 카페 목록에 자동 연결됩니다"
            )
        ),
        "user-5" to mutableListOf(
            CafeManagementData.PendingClaimSummary(
                claimId = "cafe-claim-2",
                cafeId = "cafe-2",
                cafeName = "Pink Castle Sinchon",
                requestedAt = "2026.03.09",
                status = "승인 대기 중",
                message = "기존 카페 운영자 신청이 검토 중입니다"
            )
        )
    )

    override val pendingCafeRegistrationClaimsByUser = mutableMapOf<String, MutableList<CafeRegistrationClaim>>()

    override val affiliatedCafeIdByUser = mutableMapOf(
        "user-2" to "cafe-1",
        "user-9" to "cafe-1",
        "user-10" to "cafe-1"
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

    override val cafeHomeBannerPreviewByCafeId = mutableMapOf(
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
            region = currentCafe.region.copy(address = update.address)
        )
        val updatedDetail = currentDetail.copy(
            cafe = updatedCafe,
            images = if (nextImages.isNotEmpty()) nextImages else currentDetail.images,
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
            schedule = castSchedulesByCastId[cast.id].orEmpty(),
            visitCertificationCount = visits.count { visit -> visit.cafeId == cafe.id }
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
            linkedUserId = existingCast?.linkedUserId ?: currentUserId,
            profileImage = normalizedProfileImage,
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
            visitCertificationCount = visits.count { visit -> visit.cafeId == targetCafe.id }
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

    override fun rankingItemsFromCasts(): List<RankingItem> {
        return casts
            .sortedByDescending { it.followerCount }
            .take(RANKING_MAX_COUNT)
            .mapIndexed { index, cast ->
                RankingItem(cast.id, cast.name, cast.followerCount, index + 1, cast.profileImage)
            }
    }

    override fun rankingItemsFromCafes(): List<RankingItem> {
        return cafes
            .sortedByDescending { it.ratingAvg }
            .take(RANKING_MAX_COUNT)
            .mapIndexed { index, cafe ->
                RankingItem(cafe.id, cafe.name, (cafe.ratingAvg * 100).toInt(), index + 1, cafe.thumbnailImage)
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

    private companion object {
        private const val HOME_POPULAR_CAST_MAX_COUNT = 50
        private const val RANKING_MAX_COUNT = 50
    }
}

private fun buildCafeNoticeManagementItems(): List<CafeNoticeManagementItem> {
    val longNoticeContent = """
        이번 공지에서는 운영 시간, 입장 대기, 촬영 가능 구역, 주문 마감 시간까지 한 번에 안내드립니다.
        방문 전 반드시 확인해 주시고, 현장 상황에 따라 일부 운영 방식이 조정될 수 있습니다.
        원활한 이용을 위해 예약 시간 10분 전 도착과 기본 이용 수칙 준수를 부탁드립니다.
    """.trimIndent()

    val cafe1 = listOf(
        Triple("[필독] 추석 연휴 영업 안내", "2026-03-13T09:00:00Z", true),
        Triple("화이트데이 한정 디저트 출시", "2026-03-12T08:00:00Z", false),
        Triple("3월 셋째 주 예약 오픈", "2026-03-11T10:30:00Z", false),
        Triple("주말 입장 웨이팅 정책 안내", "2026-03-10T11:20:00Z", false),
        Triple("포토타임 운영 시간 변경", "2026-03-09T12:10:00Z", false),
        Triple("사쿠라 생일 위크 현장 유의사항", "2026-03-08T13:00:00Z", true),
        Triple("한정 굿즈 재입고 안내", "2026-03-07T14:10:00Z", false),
        Triple("우천 시 우산 보관 안내", "2026-03-06T15:15:00Z", false),
        Triple("테라스석 운영 재개", "2026-03-05T16:20:00Z", false),
        Triple("3월 포인트 적립 이벤트 안내", "2026-03-04T17:25:00Z", false),
        Triple("카운터 주문 동선 변경", "2026-03-03T18:10:00Z", false),
        Triple("늦은 밤 타임 좌석 제한 안내", "2026-03-02T19:40:00Z", false),
        Triple("메이드 하우스 촬영 정책 업데이트", "2026-03-01T20:00:00Z", true),
        Triple("신규 방문자 스탬프 적립 안내", "2026-02-28T14:00:00Z", false),
        Triple("매장 리뉴얼 공사 일정 공지", "2026-02-27T13:00:00Z", false),
        Triple("봄 시즌 신메뉴 선공개", "2026-02-26T12:00:00Z", false),
        Triple("평일 오픈 시간 조정 안내", "2026-02-25T11:00:00Z", false),
        Triple("가맹 굿즈 택배 수령 지연 안내", "2026-02-24T10:00:00Z", false),
        Triple("2월 마지막 주 좌석 배치 변경", "2026-02-23T09:00:00Z", false),
        Triple("발렌타인 스페셜 종료 안내", "2026-02-22T08:00:00Z", false),
        Triple("메이드 하우스 포토존 정비 일정", "2026-02-21T07:00:00Z", false),
        Triple("봄 시즌 유니폼 선공개", "2026-02-20T12:00:00Z", true),
        Triple("2월 셋째 주 예약 안내", "2026-02-19T11:00:00Z", false),
        Triple("매장 내 취식 시간 안내", "2026-02-18T10:00:00Z", false),
        Triple("체키 촬영 운영 시간 변경", "2026-02-17T09:30:00Z", false),
        Triple("시그니처 음료 일시 품절 공지", "2026-02-16T09:10:00Z", false),
        Triple("메이드 하우스 멤버십 혜택 안내", "2026-02-15T08:40:00Z", true),
        Triple("주말 선입장 티켓 오픈", "2026-02-14T08:20:00Z", false),
        Triple("발렌타인 한정 포토카드 배부", "2026-02-13T07:50:00Z", false),
        Triple("2월 둘째 주 출근 스케줄 요약", "2026-02-12T07:20:00Z", false),
        Triple("라스트 오더 기준 변경 안내", "2026-02-11T12:40:00Z", false),
        Triple("신규 굿즈 온라인 판매 일정", "2026-02-10T12:10:00Z", false),
        Triple("메이드 하우스 이용 수칙 업데이트", "2026-02-09T11:30:00Z", true),
        Triple("주중 한정 디저트 프로모션", "2026-02-08T10:50:00Z", false),
        Triple("2월 첫째 주 예약 오픈", "2026-02-07T10:10:00Z", false),
        Triple("매장 배경음악 플레이리스트 변경", "2026-02-06T09:30:00Z", false),
        Triple("촬영 가능 구역 재안내", "2026-02-05T09:00:00Z", false),
        Triple("1월 방문 스탬프 정산 공지", "2026-02-04T08:00:00Z", false),
        Triple("메이드 하우스 2월 운영 캘린더", "2026-02-03T07:00:00Z", true)
    ).mapIndexed { index, (title, createdAt, pinned) ->
        CafeNoticeManagementItem(
            id = "cafe-1-notice-${index + 1}",
            cafeId = "cafe-1",
            title = title,
            content = if (index in setOf(0, 5, 12, 20, 32)) longNoticeContent else "$title 관련 상세 운영 안내입니다.",
            createdAt = createdAt,
            displayDate = createdAt.take(10).replace("-", "."),
            isPinned = pinned,
            statusLabel = if (index % 5 == 0) "임시 저장" else "게시 중",
            statusAccent = if (index % 5 == 0) NoticeStatusAccent.DRAFT else NoticeStatusAccent.PUBLISHED
        )
    }

    val cafe2 = listOf(
        CafeNoticeManagementItem("cafe-2-notice-1", "cafe-2", "핑크 캐슬 신규 메이드 입장", "신규 메이드 입장 안내입니다.", "2026-03-12T09:00:00Z", "2026.03.12", false, "게시 중", NoticeStatusAccent.PUBLISHED),
        CafeNoticeManagementItem("cafe-2-notice-2", "cafe-2", "주말 예약 조기 마감", "주말 예약이 조기 마감되었습니다.", "2026-03-10T09:00:00Z", "2026.03.10", false, "게시 중", NoticeStatusAccent.PUBLISHED)
    )

    val cafe3 = listOf(
        CafeNoticeManagementItem("cafe-3-notice-1", "cafe-3", "리본 카페 3월 이벤트 티저", "3월 이벤트 예고 안내입니다.", "2026-03-11T09:00:00Z", "2026.03.11", true, "게시 중", NoticeStatusAccent.PUBLISHED)
    )

    return cafe1 + cafe2 + cafe3
}

private fun buildCafeEventManagementItems(): List<CafeEventManagementItem> {
    val primaryImage = "https://lh3.googleusercontent.com/aida-public/AB6AXuA2f4YforgbxHgDTUjuv_2-RNCTUL3Nre_9UOtgIPd1ugt6LYUiIx76nm7_LgA5CEqxoInyz5vaG6_Y96e9PU_B8AU5MlUWUmBHksD3K88DkEvW6pvdLEL20-1X4le2RT-qXGt5K36xGWrhrbrf9JixW_R24QHx0M1qwPSCPasTk8ptf-Qy5TT7nHf9zj-2Jm-AZmrXB0Q9DGhfGb0fn-6Rw2jBi0LSh_21SpOScyRYwxqn5c1F4mp1uK7J7kJV3ktNxj8oaAp6bA"
    val secondaryImage = "https://lh3.googleusercontent.com/aida-public/AB6AXuDArCFaz3BWKTYq7t8oOL1HsuyHfrKScipbsR3WQ-W_afd8Yw_pYUfesi9f0iQJcZNrA5ikV_MRjFqc9S_KvTEiJnblQVm4gFSdsxXTKdjO3ZTZSw-0PkABSNHwTvHeQ1TM48PsVSw9AzZfrmmt9wrA_hNyhSL9GI859V7XvGYkXFv90pS4sAyYlc5uvrC9zSB-lVBYsyQjSwQmuQ9h9_txvxSFAcAmvXZr3DIzZ8EbYjvV04z7XQNuPJi5FiwqXya9zWY_6Zd1vw"

    val cafe1 = listOf(
        "화이트데이 커플 세트 프로모션",
        "메이드 하우스 봄 한정 파르페 이벤트",
        "사쿠라 생일 위크 스페셜",
        "리본 스탬프 더블 적립전",
        "야간 타임 음료 업그레이드 이벤트",
        "신규 방문자 웰컴 쿠폰",
        "3월 한정 체키 세트 판매",
        "테라스 오픈 기념 음료 할인",
        "평일 런치 타임 디저트 증정",
        "메이드 인기투표 이벤트",
        "굿즈 패키지 할인 주간",
        "화이트 라떼 재출시 이벤트",
        "주말 선착순 브로마이드 증정",
        "3월 말 대관 이벤트 안내",
        "봄 시즌 메뉴 사전 체험단",
        "밤 10시 이후 디저트 타임",
        "메이드 하우스 사진 콘테스트"
    ).mapIndexed { index, title ->
        val monthDay = (13 - (index % 10)).coerceAtLeast(1).toString().padStart(2, '0')
        val startDate = "2026.03.$monthDay"
        val endDate = "2026.03.${(monthDay.toInt() + 7).coerceAtMost(31).toString().padStart(2, '0')}"
        val isEnded = index >= 12
        CafeEventManagementItem(
            id = "cafe-1-event-${index + 1}",
            cafeId = "cafe-1",
            title = title,
            content = "$title 관련 진행 안내입니다.",
            imageUrl = if (index % 2 == 0) primaryImage else secondaryImage,
            startDate = startDate,
            endDate = endDate,
            statusLabel = if (isEnded) "종료" else "진행 중",
            isDimmed = isEnded
        )
    }

    val cafe2 = listOf(
        CafeEventManagementItem("cafe-2-event-1", "cafe-2", "핑크 캐슬 신규 메이드 데뷔 이벤트", "신규 메이드 데뷔 이벤트입니다.", primaryImage, "2026.03.08", "2026.03.20", "진행 중", false),
        CafeEventManagementItem("cafe-2-event-2", "cafe-2", "화이트데이 스페셜 세트", "화이트데이 스페셜 구성 안내입니다.", secondaryImage, "2026.03.01", "2026.03.14", "진행 중", false)
    )

    val cafe3 = listOf(
        CafeEventManagementItem("cafe-3-event-1", "cafe-3", "리본 카페 주말 예약 이벤트", "주말 예약 이벤트 안내입니다.", secondaryImage, "2026.03.03", "2026.03.31", "진행 중", false)
    )

    return cafe1 + cafe2 + cafe3
}

private val maidHouseAdditionalCastNames = buildList {
    addAll(
        listOf(
            "아카리", "하즈키", "마리", "코코", "루루", "시온", "히나", "노아", "세이라", "유즈",
            "린", "모모", "아오이", "하나", "이오리", "카논", "리리", "마호", "네네", "스즈",
            "미나", "카에데", "치카", "에리", "미오", "세나", "우이", "호노카", "리코", "유나",
            "사나", "코하루", "아야", "츠키", "루나", "미레", "키라", "토와", "미호", "유리",
            "아린", "나기사", "시로", "아이", "유카", "리사", "미카", "하루"
        )
    )

    val firstParts = listOf("아", "유", "미", "리", "카", "하", "루", "세", "나", "코", "시", "마")
    val secondParts = listOf("리", "나", "호", "유", "미", "라", "카", "하", "루", "아", "오", "에")

    for (first in firstParts) {
        for (second in secondParts) {
            val name = first + second
            if (name !in this) {
                add(name)
            }
            if (size >= 144) {
                return@buildList
            }
        }
    }
}

private val maidHouseAdditionalCasts = maidHouseAdditionalCastNames.mapIndexed { index, name ->
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
        followerCount = 960 - (index * 4),
        rating = 4.2 + ((index % 7) * 0.1)
    )
}

private val additionalMockCafes = buildAdditionalMockCafes()

private fun buildAdditionalMockCafes(): List<Cafe> {
    val seoulDistricts = listOf(
        "용산구 이태원로" to GeoPoint(37.534, 126.994),
        "동작구 노량진로" to GeoPoint(37.513, 126.942),
        "강동구 천호대로" to GeoPoint(37.538, 127.123),
        "은평구 연서로" to GeoPoint(37.619, 126.921),
        "영등포구 여의대로" to GeoPoint(37.526, 126.924)
    )
    val tokyoDistricts = listOf(
        "Akihabara Chiyoda" to GeoPoint(35.698, 139.773),
        "Ikebukuro Toshima" to GeoPoint(35.729, 139.710),
        "Shibuya Center-gai" to GeoPoint(35.659, 139.700),
        "Nakano Broadway" to GeoPoint(35.709, 139.665),
        "Ueno Okachimachi" to GeoPoint(35.707, 139.774)
    )
    val osakaDistricts = listOf(
        "Nipponbashi Naniwa" to GeoPoint(34.659, 135.506),
        "Shinsaibashi Chuo" to GeoPoint(34.675, 135.501),
        "Tennoji Abeno" to GeoPoint(34.646, 135.513),
        "Umeda Kita" to GeoPoint(34.705, 135.498),
        "Namba Sennichimae" to GeoPoint(34.665, 135.503)
    )
    val adjectives = listOf("로즈", "슈가", "드림", "미스티", "퓨어", "멜로디", "스텔라", "코코아", "플럼", "오팔")
    val nouns = listOf("하우스", "라운지", "살롱", "스테이지", "가든", "팔레트", "테라스", "아틀리에")

    return (12..50).map { idNumber ->
        val zeroBasedIndex = idNumber - 12
        val regionIndex = zeroBasedIndex % 3
        val cycleIndex = zeroBasedIndex / 3
        val region = when (regionIndex) {
            0 -> {
                val (address, point) = seoulDistricts[cycleIndex % seoulDistricts.size]
                Region("KR", "Seoul", address, point.offsetBy(cycleIndex))
            }
            1 -> {
                val (address, point) = tokyoDistricts[cycleIndex % tokyoDistricts.size]
                Region("JP", "Tokyo", address, point.offsetBy(cycleIndex))
            }
            else -> {
                val (address, point) = osakaDistricts[cycleIndex % osakaDistricts.size]
                Region("JP", "Osaka", address, point.offsetBy(cycleIndex))
            }
        }

        Cafe(
            id = "cafe-$idNumber",
            name = "${adjectives[zeroBasedIndex % adjectives.size]} ${nouns[cycleIndex % nouns.size]}",
            desc = when (region.city) {
                "Seoul" -> "서울 서브컬처 감성 메이드카페"
                "Tokyo" -> "도쿄 중심가 정통 메이드카페"
                else -> "오사카 투어 코스로 인기인 메이드카페"
            },
            region = region,
            thumbnailImage = null,
            ratingAvg = 4.2 + ((zeroBasedIndex % 8) * 0.1),
            reviewCount = 52 + (zeroBasedIndex * 7),
            approved = true,
            conceptType = "MAID"
        )
    }
}

private fun GeoPoint.offsetBy(index: Int): GeoPoint {
    val offset = (index % 5) * 0.002
    return GeoPoint(
        latitude = latitude + offset,
        longitude = longitude + (offset / 2.0)
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
