package com.hhp227.concafe.data.source

import kotlinx.coroutines.flow.Flow
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.AppNotification
import com.hhp227.concafe.domain.model.Cafe
import com.hhp227.concafe.domain.model.CafeDashboardData
import com.hhp227.concafe.domain.model.CafeEventManagementItem
import com.hhp227.concafe.domain.model.CafeManagementData
import com.hhp227.concafe.domain.model.CafeRegistrationClaim
import com.hhp227.concafe.domain.model.CafeDetail
import com.hhp227.concafe.domain.model.CafeNoticeManagementItem
import com.hhp227.concafe.domain.model.CafeInfoUpdate
import com.hhp227.concafe.domain.model.CafeMenuGoodsUpsert
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

interface ConCafeDataSource {
    var currentUserId: String?

    val users: MutableList<User>

    val cafes: MutableList<Cafe>

    val casts: List<Cast>

    val castClaims: MutableList<CastClaim>

    val banners: MutableList<HomeBanner>

    val inquiries: MutableList<Inquiry>

    val notices: MutableList<Notice>

    val cafeNoticeManagementItems: MutableList<CafeNoticeManagementItem>

    val cafeEventManagementItems: MutableList<CafeEventManagementItem>

    val reviews: MutableList<Review>

    val visits: MutableList<Visit>

    val notifications: MutableList<AppNotification>

    val favoriteCafeIdsByUser: MutableMap<String, MutableSet<String>>

    val followedCastIdsByUser: MutableMap<String, MutableSet<String>>

    val dismissedReviewPromptVisitIdsByUser: MutableMap<String, MutableSet<String>>

    val ownedCafeIdsByUser: MutableMap<String, MutableList<String>>

    val pendingCafeClaimsByUser: MutableMap<String, MutableList<CafeManagementData.PendingClaimSummary>>

    val pendingCafeRegistrationClaimsByUser: MutableMap<String, MutableList<CafeRegistrationClaim>>

    val affiliatedCafeIdByUser: MutableMap<String, String>

    val cafeCheckInCountById: Map<String, Int>

    val cafeTodayCheckInCountById: Map<String, Int>

    val cafeTodayReviewCountById: Map<String, Int>

    val onShiftCastIdsByCafeId: Map<String, Set<String>>

    val cafeHomeBannerPreviewByCafeId: MutableMap<String, CafeDashboardData.HomeBannerPreview>

    val castTodayVisitCountById: Map<String, Int>

    val cafeDetailsById: MutableMap<String, CafeDetail>

    fun defaultMyPageSummary(userId: String): MyPageSummary

    fun cafeDetail(cafeId: String): CafeDetail?

    fun updateCafeInfo(update: CafeInfoUpdate): CafeDetail

    fun upsertCafeMenuGoods(update: CafeMenuGoodsUpsert): CafeDetail

    fun deleteCafeMenuGoods(cafeId: String, itemId: String): CafeDetail

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

    fun refreshReviewProjections(cafeId: String, taggedCastIds: List<String>)

    fun rankingItemsFromCasts(): List<RankingItem>

    fun rankingItemsFromCafes(): List<RankingItem>

    fun homePopularCastPage(cursor: String?, pageSize: Int): PagedResult<Cast>

    fun verifyVisitResult(cafeId: String, latitude: Double, longitude: Double): VisitVerificationResult

    fun <T> toPaged(items: List<T>, cursor: String?, pageSize: Int): PagedResult<T>
}
