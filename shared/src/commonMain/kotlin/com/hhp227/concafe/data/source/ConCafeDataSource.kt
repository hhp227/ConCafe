package com.hhp227.concafe.data.source

import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.AppNotification
import com.hhp227.concafe.domain.model.Cafe
import com.hhp227.concafe.domain.model.CafeDashboardData
import com.hhp227.concafe.domain.model.CafeDetail
import com.hhp227.concafe.domain.model.CafeEventManagementItem
import com.hhp227.concafe.domain.model.CafeNoticeManagementItem
import com.hhp227.concafe.domain.model.CafeInfoUpdate
import com.hhp227.concafe.domain.model.CafeMenuGoodsUpsert
import com.hhp227.concafe.domain.model.CafeManagementData
import com.hhp227.concafe.domain.model.CafeRegistrationClaim
import com.hhp227.concafe.domain.model.Cast
import com.hhp227.concafe.domain.model.CastClaim
import com.hhp227.concafe.domain.model.CastDetail
import com.hhp227.concafe.domain.model.CastSchedule
import com.hhp227.concafe.domain.model.CastScheduleStatus
import com.hhp227.concafe.domain.model.CastScheduleUpdate
import com.hhp227.concafe.domain.model.CastUpsert
import com.hhp227.concafe.domain.model.HomeBanner
import com.hhp227.concafe.domain.model.Inquiry
import com.hhp227.concafe.domain.model.MyPageSummary
import com.hhp227.concafe.domain.model.Notice
import com.hhp227.concafe.domain.model.RankingItem
import com.hhp227.concafe.domain.model.Review
import com.hhp227.concafe.domain.model.User
import com.hhp227.concafe.domain.model.Visit
import com.hhp227.concafe.domain.model.VisitVerificationResult

enum class BannerOwnerType {
    ADMIN,
    CAFE_OWNER
}

enum class BannerLinkType {
    CAFE,
    EVENT,
    NOTICE,
    EXTERNAL
}

enum class BannerStatus {
    DRAFT,
    SCHEDULED,
    ACTIVE,
    ENDED,
    PAUSED
}

data class HomeBannerDocument(
    val id: String,
    val ownerType: BannerOwnerType,
    val ownerId: String,
    val relatedCafeId: String?,
    val title: String,
    val subtitle: String,
    val imageUrl: String?,
    val linkType: BannerLinkType,
    val linkTarget: String,
    val priority: Int,
    val maxVisibleGroup: Int,
    val displayDays: Int,
    val activeFrom: String?,
    val activeUntil: String?,
    val status: BannerStatus,
    val createdAt: String,
    val updatedAt: String
)

data class ExternalLinkDocument(
    val id: String,
    val platform: String,
    val title: String,
    val url: String,
    val isVisible: Boolean,
    val sortOrder: Int,
    val createdAt: String,
    val updatedAt: String
)

data class CastScheduleStatusDocument(
    val status: CastScheduleStatus,
    val updatedAt: String,
    val updatedBy: String
)

data class StampDocument(
    val id: String,
    val userId: String,
    val cafeId: String,
    val visitId: String,
    val earnedAt: String
)

interface AuthDataSource {
    var currentUserId: String?
    val users: MutableList<User>
}

interface CafeDataSource {
    val cafes: MutableList<Cafe>
    val cafeDetailsById: MutableMap<String, CafeDetail>
    val ownedCafeIdsByUser: MutableMap<String, MutableList<String>>
    val pendingCafeClaimsByUser: MutableMap<String, MutableList<CafeManagementData.PendingClaimSummary>>
    val pendingCafeRegistrationClaimsByUser: MutableMap<String, MutableList<CafeRegistrationClaim>>
    val cafeCheckInCountById: Map<String, Int>
    val cafeTodayCheckInCountById: Map<String, Int>
    val cafeTodayReviewCountById: Map<String, Int>
    val onShiftCastIdsByCafeId: Map<String, Set<String>>
    val cafeHomeBannerPreviewByCafeId: MutableMap<String, CafeDashboardData.HomeBannerPreview>

    fun cafeDetail(cafeId: String): CafeDetail?
    fun updateCafeInfo(update: CafeInfoUpdate): CafeDetail
    fun upsertCafeMenuGoods(update: CafeMenuGoodsUpsert): CafeDetail
    fun deleteCafeMenuGoods(cafeId: String, itemId: String): CafeDetail
}

interface CastDataSource {
    val casts: List<Cast>
    val affiliatedCafeIdByUser: MutableMap<String, String>
    val castTodayVisitCountById: Map<String, Int>

    fun castDetail(castId: String): CastDetail?
    fun castSchedules(castId: String, fromDate: String, toDate: String): List<CastSchedule>
    fun castScheduleStatuses(
        castId: String,
        fromDate: String,
        toDate: String
    ): Map<String, CastScheduleStatus>

    fun updateCastSchedule(update: CastScheduleUpdate): CastSchedule?
    fun upsertCast(update: CastUpsert): CastDetail
    fun deleteCast(castId: String): Cast
}

interface CastClaimDataSource {
    val castClaims: MutableList<CastClaim>
}

interface BannerDataSource {
    val banners: MutableList<HomeBanner>
    val homeBannerDocuments: MutableList<HomeBannerDocument>
}

interface InquiryDataSource {
    val inquiries: MutableList<Inquiry>
}

interface NoticeDataSource {
    val notices: MutableList<Notice>
    val cafeNoticeManagementItems: MutableList<CafeNoticeManagementItem>
    val cafeEventManagementItems: MutableList<CafeEventManagementItem>
}

interface ReviewDataSource {
    val reviews: MutableList<Review>
    fun refreshReviewProjections(cafeId: String, taggedCastIds: List<String>)
}

interface VisitDataSource {
    val visits: MutableList<Visit>
    fun verifyVisitResult(cafeId: String, latitude: Double, longitude: Double): VisitVerificationResult
}

interface ExternalLinkDataSource {
    val cafeExternalLinksByCafeId: MutableMap<String, MutableList<ExternalLinkDocument>>
    val castExternalLinksByCastId: MutableMap<String, MutableList<ExternalLinkDocument>>
}

interface StampDataSource {
    val stamps: MutableList<StampDocument>
}

interface ScheduleStatusDataSource {
    val castScheduleStatusDocumentsByCastId: MutableMap<String, MutableMap<String, CastScheduleStatusDocument>>
}

interface NotificationDataSource {
    val notifications: MutableList<AppNotification>
}

interface SocialDataSource {
    val favoriteCafeIdsByUser: MutableMap<String, MutableSet<String>>
    val followedCastIdsByUser: MutableMap<String, MutableSet<String>>
    val favoriteUserIdsByCafeId: MutableMap<String, MutableSet<String>>
    val followerUserIdsByCastId: MutableMap<String, MutableSet<String>>
}

interface MyInfoDataSource {
    val dismissedReviewPromptVisitIdsByUser: MutableMap<String, MutableSet<String>>
    fun defaultMyPageSummary(userId: String): MyPageSummary
}

interface RankingDataSource {
    fun rankingItemsFromCasts(): List<RankingItem>
    fun rankingItemsFromCafes(): List<RankingItem>
    fun homePopularCastPage(cursor: String?, pageSize: Int): PagedResult<Cast>
}

interface PagingDataSource {
    fun <T> toPaged(items: List<T>, cursor: String?, pageSize: Int): PagedResult<T>
}

interface ConCafeDataSource :
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
}
