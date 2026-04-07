package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.CafeRemoteDataSource
import com.hhp227.concafe.data.source.VisitRemoteDataSource
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.Cafe
import com.hhp227.concafe.domain.model.CafeDetail
import com.hhp227.concafe.domain.model.CafeInfoUpdate
import com.hhp227.concafe.domain.model.CafeMenuGoodsUpsert
import com.hhp227.concafe.domain.model.CafeSort
import com.hhp227.concafe.domain.model.CheckInCafeSummary
import com.hhp227.concafe.domain.repository.CafeRepository

class CafeRepositoryImpl(
    private val cafeRemoteDataSource: CafeRemoteDataSource,
    private val visitRemoteDataSource: VisitRemoteDataSource
) : CafeRepository {
    override suspend fun searchCafes(
        query: String?,
        country: String?,
        city: String?,
        sort: CafeSort,
        cursor: String?,
        pageSize: Int
    ): PagedResult<Cafe> {
        return cafeRemoteDataSource.searchCafesRemote(
            query = query,
            country = country,
            city = city,
            sort = sort,
            cursor = cursor,
            pageSize = pageSize
        )
    }

    override suspend fun getCafeDetail(cafeId: String): CafeDetail {
        return cafeRemoteDataSource.fetchCafeDetail(cafeId)
    }

    override suspend fun updateCafeInfo(update: CafeInfoUpdate): CafeDetail {
        return cafeRemoteDataSource.updateCafeInfoRemote(update)
    }

    override suspend fun upsertCafeMenuGoods(update: CafeMenuGoodsUpsert): CafeDetail {
        return cafeRemoteDataSource.upsertCafeMenuGoodsRemote(update)
    }

    override suspend fun deleteCafeMenuGoods(cafeId: String, itemId: String): CafeDetail {
        return cafeRemoteDataSource.deleteCafeMenuGoodsRemote(cafeId, itemId)
    }

    override suspend fun isFavorite(userId: String, cafeId: String): Boolean {
        val favoriteCafeIds = cafeRemoteDataSource.fetchFavoriteCafeIds(userId)
        return favoriteCafeIds.contains(cafeId)
    }

    override suspend fun getFavoriteCafeIds(userId: String): List<String> {
        return cafeRemoteDataSource.fetchFavoriteCafeIds(userId)
    }

    override suspend fun getCafesByIds(cafeIds: List<String>): List<Cafe> {
        if (!cafeIds.isEmpty()) {
            val distinctCafeIds = cafeIds.distinct()
            val cafesById = distinctCafeIds.associateWith { cafeId ->
                runCatching {
                    cafeRemoteDataSource.fetchCafeDetail(cafeId).cafe
                }.getOrNull()
            }
            return distinctCafeIds
                .mapNotNull { cafeId -> cafesById[cafeId] }
                .filter { cafe -> cafe.approved }
        }
        return emptyList()
    }

    override suspend fun toggleFavorite(userId: String, cafeId: String): Boolean {
        val favoriteCafeIds = cafeRemoteDataSource.fetchFavoriteCafeIds(userId)
        val isFavorite = favoriteCafeIds.contains(cafeId)

        if (isFavorite) {
            cafeRemoteDataSource.unfavoriteCafeRemote(
                userId = userId,
                cafeId = cafeId
            )
            return false
        } else {
            cafeRemoteDataSource.favoriteCafeRemote(
                userId = userId,
                cafeId = cafeId
            )
            return true
        }
    }

    override suspend fun updateCafeSocialMedia(
        cafeId: String,
        instagramId: String?,
        twitterId: String?,
        tiktokId: String?,
        youtubeId: String?
    ) {
        cafeRemoteDataSource.updateCafeSocialMediaRemote(
            cafeId = cafeId,
            instagramId = instagramId,
            twitterId = twitterId,
            tiktokId = tiktokId,
            youtubeId = youtubeId
        )
    }

    override suspend fun updateCafeReservationUrl(cafeId: String, reservationUrl: String?) {
        cafeRemoteDataSource.updateCafeReservationUrlRemote(cafeId, reservationUrl)
    }

    override suspend fun getPopularCheckInCafes(limit: Int): List<CheckInCafeSummary> {
        val safeLimit = if (limit > 0) limit else 1
        val sourceCafes = cafeRemoteDataSource.searchCafesRemote(
            query = null,
            country = null,
            city = null,
            sort = CafeSort.POPULAR,
            cursor = null,
            pageSize = safeLimit
        ).items
        return sourceCafes.map { cafe ->
            val resolvedVisitCount = visitRemoteDataSource.fetchVisitCountByCafe(cafe.id)

            CheckInCafeSummary(
                id = cafe.id,
                name = cafe.name,
                locationLabel = cafe.region.city,
                geoPoint = cafe.region.location,
                rating = cafe.ratingAvg,
                checkInCount = resolvedVisitCount,
                thumbnailImage = cafe.thumbnailImage
            )
        }
    }
}
