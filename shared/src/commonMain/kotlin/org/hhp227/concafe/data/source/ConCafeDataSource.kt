package org.hhp227.concafe.data.source

import org.hhp227.concafe.domain.common.PagedResult
import org.hhp227.concafe.domain.model.AppNotification
import org.hhp227.concafe.domain.model.Cafe
import org.hhp227.concafe.domain.model.CafeManagementData
import org.hhp227.concafe.domain.model.CafeDetail
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

    val cafes: List<Cafe>

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

    val castTodayVisitCountById: Map<String, Int>

    fun defaultMyPageSummary(userId: String): MyPageSummary

    fun cafeDetail(cafeId: String): CafeDetail?

    fun castDetail(castId: String): CastDetail?

    fun rankingItemsFromCasts(): List<RankingItem>

    fun rankingItemsFromCafes(): List<RankingItem>

    fun verifyVisitResult(cafeId: String, latitude: Double, longitude: Double): VisitVerificationResult

    fun <T> toPaged(items: List<T>, cursor: String?, pageSize: Int): PagedResult<T>
}
