package org.hhp227.concafe.data.source

import kotlinx.coroutines.flow.Flow
import org.hhp227.concafe.domain.common.PagedResult
import org.hhp227.concafe.domain.model.AppNotification
import org.hhp227.concafe.domain.model.Cafe
import org.hhp227.concafe.domain.model.CafeDashboardData
import org.hhp227.concafe.domain.model.CafeManagementData
import org.hhp227.concafe.domain.model.CafeDetail
import org.hhp227.concafe.domain.model.CafeInfoUpdate
import org.hhp227.concafe.domain.model.CafeMenuGoodsUpsert
import org.hhp227.concafe.domain.model.Cast
import org.hhp227.concafe.domain.model.CastDetail
import org.hhp227.concafe.domain.model.HomeBanner
import org.hhp227.concafe.domain.model.MyPageSummary
import org.hhp227.concafe.domain.model.Notice
import org.hhp227.concafe.domain.model.RankingItem
import org.hhp227.concafe.domain.model.Review
import org.hhp227.concafe.domain.model.User
import org.hhp227.concafe.domain.model.Visit
import org.hhp227.concafe.domain.model.VisitVerificationResult

interface ConCafeDataSource {
    var currentUserId: String?

    val users: MutableList<User>

    val cafes: MutableList<Cafe>

    val casts: List<Cast>

    val banners: List<HomeBanner>

    val notices: List<Notice>

    val reviews: MutableList<Review>

    val visits: MutableList<Visit>

    val notifications: MutableList<AppNotification>

    val favoriteCafeIdsByUser: MutableMap<String, MutableSet<String>>

    val followedCastIdsByUser: MutableMap<String, MutableSet<String>>

    val ownedCafeIdsByUser: Map<String, List<String>>

    val pendingCafeClaimsByUser: Map<String, List<CafeManagementData.PendingClaimSummary>>

    val cafeCheckInCountById: Map<String, Int>

    val cafeTodayCheckInCountById: Map<String, Int>

    val cafeTodayReviewCountById: Map<String, Int>

    val onShiftCastIdsByCafeId: Map<String, Set<String>>

    val cafeHomeBannerPreviewByCafeId: Map<String, CafeDashboardData.HomeBannerPreview>

    val castTodayVisitCountById: Map<String, Int>

    val cafeDetailsById: MutableMap<String, CafeDetail>

    fun defaultMyPageSummary(userId: String): MyPageSummary

    fun cafeDetail(cafeId: String): CafeDetail?

    fun observeCafeDetail(cafeId: String): Flow<CafeDetail?>

    fun updateCafeInfo(update: CafeInfoUpdate): CafeDetail

    fun upsertCafeMenuGoods(update: CafeMenuGoodsUpsert): CafeDetail

    fun deleteCafeMenuGoods(cafeId: String, itemId: String): CafeDetail

    fun castDetail(castId: String): CastDetail?

    fun rankingItemsFromCasts(): List<RankingItem>

    fun rankingItemsFromCafes(): List<RankingItem>

    fun verifyVisitResult(cafeId: String, latitude: Double, longitude: Double): VisitVerificationResult

    fun <T> toPaged(items: List<T>, cursor: String?, pageSize: Int): PagedResult<T>
}
