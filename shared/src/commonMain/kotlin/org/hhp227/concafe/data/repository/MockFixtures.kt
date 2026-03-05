package org.hhp227.concafe.data.repository

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
import org.hhp227.concafe.domain.model.HomeBirthdayCast
import org.hhp227.concafe.domain.model.HomeFeed
import org.hhp227.concafe.domain.model.HomeNearbyCafe
import org.hhp227.concafe.domain.model.HomePopularCast
import org.hhp227.concafe.domain.model.Menu
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

internal object MockFixtures {
    val users = mutableListOf(
        User(
            id = "user-1",
            email = "user1@concafe.app",
            nickname = "리본냥",
            profileImage = null,
            role = UserRole.USER,
            banned = false,
            createdAt = "2026-03-01T09:00:00Z"
        )
    )

    var currentUserId: String? = "user-1"

    val cafes = listOf(
        Cafe(
            id = "cafe-1",
            name = "메이드 하우스",
            description = "강남 인기 메이드카페",
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
            description = "신촌 감성 카페",
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
            description = "홍대 서브컬쳐 카페",
            region = Region("KR", "Seoul", "마포구 와우산로", GeoPoint(37.556, 126.923)),
            thumbnailImage = null,
            ratingAvg = 4.7,
            reviewCount = 149,
            approved = true,
            conceptType = "MAID"
        )
    )

    val casts = listOf(
        Cast("maid-1", "cafe-1", "사쿠라", null, "메이드 하우스 대표 메이드", "2001-03-11", "maid", 1234),
        Cast("maid-2", "cafe-2", "미유", null, "핑크 캐슬 시그니처 메이드", "2002-04-10", "maid", 987),
        Cast("maid-3", "cafe-3", "유이", null, "리본 카페 인기 메이드", "2000-05-14", "maid", 856),
        Cast("maid-4", "cafe-2", "나나", null, "생일 이벤트 진행 중", "2001-03-05", "maid", 700),
        Cast("maid-5", "cafe-1", "레이", null, "생일 위크", "2003-03-05", "maid", 620),
        Cast("maid-6", "cafe-3", "미키", null, "생일 한정 출근", "2002-03-05", "maid", 540)
    )

    val notices = listOf(
        Notice("notice-1", "cafe-1", "메이드 하우스", "3월 특별 이벤트", "3월 특별 이벤트 진행 중!", "2026-03-05T07:00:00Z", "2시간 전"),
        Notice("notice-2", "cafe-2", "핑크 캐슬", "신규 메이드 입장", "신규 메이드 입장! 많은 관심 부탁드려요", "2026-03-05T04:00:00Z", "5시간 전"),
        Notice("notice-3", "cafe-3", "리본 카페", "주말 예약 마감", "주말 예약이 마감되었습니다", "2026-03-04T09:00:00Z", "1일 전")
    )

    val homeFeed = HomeFeed(
        banners = listOf(
            HomeBanner("banner-1", "3월 특별 이벤트", "F8A3C5", "F76C9E"),
            HomeBanner("banner-2", "신규 메이드 입점", "FFC2A7", "FF8F7A"),
            HomeBanner("banner-3", "주말 예약 오픈", "B6A5FF", "7E88FF")
        ),
        popularCasts = listOf(
            HomePopularCast("maid-1", "사쿠라", "메이드 하우스", 1234, null),
            HomePopularCast("maid-2", "미유", "핑크 캐슬", 987, null),
            HomePopularCast("maid-3", "유이", "리본 카페", 856, null)
        ),
        nearbyCafes = listOf(
            HomeNearbyCafe("cafe-1", "메이드 하우스", 4.8, "강남", "0.5km", null),
            HomeNearbyCafe("cafe-2", "핑크 캐슬", 4.9, "신촌", "1.2km", null),
            HomeNearbyCafe("cafe-3", "리본 카페", 4.7, "홍대", "2.1km", null)
        ),
        birthdayCasts = listOf(
            HomeBirthdayCast("maid-4", "나나", null),
            HomeBirthdayCast("maid-5", "레이", null),
            HomeBirthdayCast("maid-6", "미키", null)
        ),
        notices = notices
    )

    val reviews = mutableListOf(
        Review("review-1", "user-1", "cafe-1", 4.5f, "분위기가 좋아요", emptyList(), 3, "2026-03-03T10:00:00Z"),
        Review("review-2", "user-1", "cafe-2", 5.0f, "친절하고 재밌었어요", emptyList(), 5, "2026-03-04T14:00:00Z")
    )

    val visits = mutableListOf(
        Visit("visit-1", "user-1", "cafe-1", "2026-03-01T12:00:00Z", "첫 방문", true)
    )

    val notifications = mutableListOf(
        AppNotification("noti-1", "user-1", "사쿠라 출근 알림", "오늘 18:00 출근 예정", "CAST_SHIFT", "maid-1", false, "2026-03-05T08:00:00Z"),
        AppNotification("noti-2", "user-1", "생일 이벤트", "나나 생일 이벤트 진행 중", "BIRTHDAY", "maid-4", true, "2026-03-04T08:00:00Z")
    )

    val favoriteCafeIdsByUser = mutableMapOf("user-1" to mutableSetOf("cafe-1"))

    val followedCastIdsByUser = mutableMapOf("user-1" to mutableSetOf("maid-1"))

    fun defaultMyPageSummary(userId: String): MyPageSummary {
        val favoritesCount = favoriteCafeIdsByUser[userId]?.size ?: 0
        val followedCount = followedCastIdsByUser[userId]?.size ?: 0
        val visitCount = visits.count { it.userId == userId }
        return MyPageSummary(userId, visitCount, favoritesCount, followedCount, badgesCount = 3, level = 4)
    }

    fun cafeDetail(cafeId: String): CafeDetail? {
        val cafe = cafes.firstOrNull { it.id == cafeId } ?: return null
        val cafeCasts = casts.filter { it.cafeId == cafeId }
        val cafeNotices = notices.filter { it.cafeId == cafeId }
        return CafeDetail(
            cafe = cafe,
            images = listOf("", ""),
            casts = cafeCasts,
            menus = listOf(
                Menu("menu-1", "딸기 파르페", 12000, "대표 디저트", null, "food"),
                Menu("menu-2", "핑크 라떼", 8000, "시그니처 음료", null, "drink")
            ),
            goods = listOf(
                Goods("goods-1", "랜덤 포토카드", 5000, null, 50)
            ),
            notices = cafeNotices
        )
    }

    fun castDetail(castId: String): CastDetail? {
        val cast = casts.firstOrNull { it.id == castId } ?: return null
        val cafe = cafes.firstOrNull { it.id == cast.cafeId } ?: return null
        return CastDetail(
            cast = cast,
            cafe = cafe,
            images = listOf("", ""),
            schedule = listOf(
                CastSchedule("schedule-1", cast.id, cast.cafeId, "2026-03-06", "18:00", "22:00"),
                CastSchedule("schedule-2", cast.id, cast.cafeId, "2026-03-07", "16:00", "21:00")
            )
        )
    }

    fun rankingItemsFromCasts(): List<RankingItem> {
        return casts
            .sortedByDescending { it.followerCount }
            .mapIndexed { index, cast ->
                RankingItem(cast.id, cast.name, cast.followerCount, index + 1, cast.profileImage)
            }
    }

    fun rankingItemsFromCafes(): List<RankingItem> {
        return cafes
            .sortedByDescending { it.ratingAvg }
            .mapIndexed { index, cafe ->
                RankingItem(cafe.id, cafe.name, (cafe.ratingAvg * 100).toInt(), index + 1, cafe.thumbnailImage)
            }
    }

    fun verifyVisitResult(cafeId: String, latitude: Double, longitude: Double): VisitVerificationResult {
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

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = (lat2 - lat1).toRadians()
        val dLon = (lon2 - lon1).toRadians()
        val a = sin(dLat / 2).times(sin(dLat / 2)) +
            cos(lat1.toRadians()).times(cos(lat2.toRadians())).times(
                sin(dLon / 2).times(sin(dLon / 2))
            )
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    private fun Double.toRadians(): Double {
        return this * PI / 180.0
    }

    fun <T> toPaged(items: List<T>, cursor: String?, pageSize: Int): PagedResult<T> {
        val start = cursor?.toIntOrNull() ?: 0
        val endExclusive = (start + pageSize).coerceAtMost(items.size)
        val next = if (endExclusive < items.size) endExclusive.toString() else null
        return PagedResult(items.subList(start, endExclusive), next, next != null)
    }
}
