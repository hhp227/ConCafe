package org.hhp227.concafe.data.source

import org.hhp227.concafe.data.repository.MockFixtures
import org.hhp227.concafe.domain.common.PagedResult
import org.hhp227.concafe.domain.model.AppNotification
import org.hhp227.concafe.domain.model.Cafe
import org.hhp227.concafe.domain.model.CafeDetail
import org.hhp227.concafe.domain.model.Cast
import org.hhp227.concafe.domain.model.CastDetail
import org.hhp227.concafe.domain.model.HomeFeed
import org.hhp227.concafe.domain.model.MyPageSummary
import org.hhp227.concafe.domain.model.Notice
import org.hhp227.concafe.domain.model.RankingItem
import org.hhp227.concafe.domain.model.Review
import org.hhp227.concafe.domain.model.User
import org.hhp227.concafe.domain.model.Visit
import org.hhp227.concafe.domain.model.VisitVerificationResult

class MockConCafeDataSource : ConCafeDataSource {
    override var currentUserId: String?
        get() = MockFixtures.currentUserId
        set(value) {
            MockFixtures.currentUserId = value
        }

    override val users: MutableList<User> = MockFixtures.users

    override val cafes: List<Cafe> = MockFixtures.cafes

    override val casts: List<Cast> = MockFixtures.casts

    override val notices: List<Notice> = MockFixtures.notices

    override val homeFeed: HomeFeed = MockFixtures.homeFeed

    override val reviews: MutableList<Review> = MockFixtures.reviews

    override val visits: MutableList<Visit> = MockFixtures.visits

    override val notifications: MutableList<AppNotification> = MockFixtures.notifications

    override val favoriteCafeIdsByUser: MutableMap<String, MutableSet<String>> = MockFixtures.favoriteCafeIdsByUser

    override val followedCastIdsByUser: MutableMap<String, MutableSet<String>> = MockFixtures.followedCastIdsByUser

    override fun defaultMyPageSummary(userId: String): MyPageSummary {
        return MockFixtures.defaultMyPageSummary(userId)
    }

    override fun cafeDetail(cafeId: String): CafeDetail? {
        return MockFixtures.cafeDetail(cafeId)
    }

    override fun castDetail(castId: String): CastDetail? {
        return MockFixtures.castDetail(castId)
    }

    override fun rankingItemsFromCasts(): List<RankingItem> {
        return MockFixtures.rankingItemsFromCasts()
    }

    override fun rankingItemsFromCafes(): List<RankingItem> {
        return MockFixtures.rankingItemsFromCafes()
    }

    override fun verifyVisitResult(cafeId: String, latitude: Double, longitude: Double): VisitVerificationResult {
        return MockFixtures.verifyVisitResult(cafeId, latitude, longitude)
    }

    override fun <T> toPaged(items: List<T>, cursor: String?, pageSize: Int): PagedResult<T> {
        return MockFixtures.toPaged(items, cursor, pageSize)
    }
}
