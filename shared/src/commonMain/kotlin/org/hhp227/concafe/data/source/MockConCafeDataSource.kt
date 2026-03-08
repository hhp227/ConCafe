package org.hhp227.concafe.data.source

import org.hhp227.concafe.domain.common.PagedResult
import org.hhp227.concafe.domain.model.AppNotification
import org.hhp227.concafe.domain.model.Cafe
import org.hhp227.concafe.domain.model.CafeDetail
import org.hhp227.concafe.domain.model.Cast
import org.hhp227.concafe.domain.model.CastDetail
import org.hhp227.concafe.domain.model.CastSchedule
import org.hhp227.concafe.domain.model.GeoPoint
import org.hhp227.concafe.domain.model.Goods
import org.hhp227.concafe.domain.model.HomeBanner
import org.hhp227.concafe.domain.model.CafeMenu
import org.hhp227.concafe.domain.model.MyPageSummary
import org.hhp227.concafe.domain.model.Notice
import org.hhp227.concafe.domain.model.RankingItem
import org.hhp227.concafe.domain.model.Region
import org.hhp227.concafe.domain.model.Review
import org.hhp227.concafe.domain.model.User
import org.hhp227.concafe.domain.model.UserRole
import org.hhp227.concafe.domain.model.Visit
import org.hhp227.concafe.domain.model.VisitVerificationResult
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
        )
    )

    override val cafes = listOf(
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

    override val casts = listOf(
        Cast("maid-1", "cafe-1", "사쿠라", null, "메이드 하우스 대표 메이드", "2001-03-11", "maid", 1234),
        Cast("maid-2", "cafe-2", "미유", null, "핑크 캐슬 시그니처 메이드", "2002-04-10", "maid", 987),
        Cast("maid-3", "cafe-3", "유이", null, "리본 카페 인기 메이드", "2000-05-14", "maid", 856),
        Cast("maid-4", "cafe-2", "나나", null, "생일 이벤트 진행 중", "2001-03-05", "maid", 700),
        Cast("maid-5", "cafe-1", "레이", null, "생일 위크", "2003-03-05", "maid", 620),
        Cast("maid-6", "cafe-3", "미키", null, "생일 한정 출근", "2002-03-05", "maid", 540)
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

    override val reviews = mutableListOf(
        Review("review-1", "user-1", "cafe-1", 4.5f, "분위기가 좋아요", emptyList(), 3, "2026-03-03T10:00:00Z"),
        Review("review-2", "user-1", "cafe-2", 5.0f, "친절하고 재밌었어요", emptyList(), 5, "2026-03-04T14:00:00Z")
    )

    override val visits = mutableListOf(
        Visit("visit-1", "user-1", "cafe-1", "2026-03-01T12:00:00Z", "첫 방문", true)
    )

    override val notifications = mutableListOf(
        AppNotification("noti-1", "user-1", "사쿠라 출근 알림", "오늘 18:00 출근 예정", "CAST_SHIFT", "maid-1", false, "2026-03-05T08:00:00Z"),
        AppNotification("noti-2", "user-1", "생일 이벤트", "나나 생일 이벤트 진행 중", "BIRTHDAY", "maid-4", true, "2026-03-04T08:00:00Z")
    )

    override val favoriteCafeIdsByUser = mutableMapOf("user-1" to mutableSetOf("cafe-1"))

    override val followedCastIdsByUser = mutableMapOf("user-1" to mutableSetOf("maid-1"))

    override fun defaultMyPageSummary(userId: String): MyPageSummary {
        val favoritesCount = favoriteCafeIdsByUser[userId]?.size ?: 0
        val followedCount = followedCastIdsByUser[userId]?.size ?: 0
        val visitCount = visits.count { it.userId == userId }
        return MyPageSummary(userId, visitCount, favoritesCount, followedCount, badgesCount = 3, level = 4)
    }

    override fun cafeDetail(cafeId: String): CafeDetail? {
        val cafe = cafes.firstOrNull { it.id == cafeId } ?: return null
        val cafeCasts = casts.filter { it.cafeId == cafeId }
        val cafeNotices = notices.filter { it.cafeId == cafeId }
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
                    "food"
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
                    "drink"
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
                    "dessert"
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

    override fun castDetail(castId: String): CastDetail? {
        val cast = casts.firstOrNull { it.id == castId } ?: return null
        val cafe = cafes.firstOrNull { it.id == cast.cafeId } ?: return null
        val todaySchedule = if (cast.id == "maid-1" || cast.id == "maid-2" || cast.id == "maid-5") {
            CastSchedule("schedule-1", cast.id, cast.cafeId, "2026-03-08", "18:00", "22:00")
        } else {
            CastSchedule("schedule-1", cast.id, cast.cafeId, "2026-03-09", "18:00", "22:00")
        }

        return CastDetail(
            cast = cast,
            cafe = cafe,
            images = listOf("", ""),
            schedule = listOf(
                todaySchedule,
                CastSchedule("schedule-2", cast.id, cast.cafeId, "2026-03-10", "16:00", "21:00")
            )
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
