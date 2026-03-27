package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.CafeSort
import com.hhp227.concafe.domain.model.CastSort
import com.hhp227.concafe.domain.model.RankingFeed
import com.hhp227.concafe.domain.model.RankingFeedEntry
import com.hhp227.concafe.domain.model.RankingPeriod
import com.hhp227.concafe.domain.model.RankingPromoAd
import com.hhp227.concafe.domain.repository.CafeRepository
import com.hhp227.concafe.domain.repository.CastRepository

class GetRankingFeedUseCase(
    private val castRepository: CastRepository,
    private val cafeRepository: CafeRepository
) {
    suspend operator fun invoke(
        period: RankingPeriod,
        country: String?,
        city: String?
    ): AppResult<RankingFeed> {
        val castResult = runCatching {
            castRepository.searchCasts(
                query = null,
                country = country,
                city = city,
                sort = CastSort.FOLLOWERS,
                cursor = null,
                pageSize = RANKING_FETCH_LIMIT
            ).items
        }
        val cafeResult = runCatching {
            cafeRepository.searchCafes(
                query = null,
                country = country,
                city = city,
                sort = CafeSort.RATING,
                cursor = null,
                pageSize = RANKING_FETCH_LIMIT
            ).items
        }
        val allFailed = castResult.isFailure && cafeResult.isFailure
        val firstError = castResult.exceptionOrNull() ?: cafeResult.exceptionOrNull()

        if (allFailed) {
            val throwable = firstError
            return if (throwable is NoSuchElementException) {
                AppResult.Failure(AppError.NotFound)
            } else if (throwable is IllegalArgumentException) {
                AppResult.Failure(AppError.ValidationFailed(throwable.message ?: "invalid request"))
            } else {
                AppResult.Failure(AppError.Unknown(throwable?.message))
            }
        }
        val casts = castResult.getOrElse { emptyList() }
        val cafes = cafeResult.getOrElse { emptyList() }
        val cafeNameById = cafes.associateBy({ cafe -> cafe.id }, { cafe -> cafe.name })
        val castRankings = casts.mapIndexed { index, cast ->
            RankingFeedEntry(
                id = cast.id,
                rank = index + 1,
                name = cast.name,
                subtitle = cafeNameById[cast.cafeId] ?: cast.cafeId,
                score = cast.followerCount,
                change = rankChange(index),
                startColorHex = castColors(index).first,
                endColorHex = castColors(index).second,
                symbol = castSymbol(index)
            )
        }
        val cafeRankings = cafes.mapIndexed { index, cafe ->
            RankingFeedEntry(
                id = cafe.id,
                rank = index + 1,
                name = cafe.name,
                subtitle = cafe.region.address.substringBefore("구").substringBefore("로").ifBlank { cafe.region.city },
                score = (cafe.ratingAvg * 100).toInt(),
                change = rankChange(index + 1),
                startColorHex = cafeColors(index).first,
                endColorHex = cafeColors(index).second,
                symbol = cafeSymbol(index)
            )
        }
        return AppResult.Success(
            RankingFeed(
                ads = defaultAds(),
                castRankings = castRankings,
                cafeRankings = cafeRankings
            )
        )
    }

    private fun defaultAds(): List<RankingPromoAd> {
        return listOf(
            RankingPromoAd("ad-premium", "AD", "프리미엄 멤버십", "첫 달 50% 할인!", "특별한 혜택을 받아보세요", "A66BFF", "F58CCF", "✨"),
            RankingPromoAd("ad-coupon", "AD", "3월 특별 쿠폰", "전 메뉴 20% 할인", "메이드 하우스에서 사용 가능", "F6A8C5", "FFC8A2", "🎁"),
            RankingPromoAd("ad-open", "AD", "신규 카페 오픈", "리본 카페 홍대점", "오픈 기념 이벤트 진행중!", "7AC7FF", "7BE7D8", "🎀")
        )
    }

    private fun rankChange(index: Int): String {
        val values = listOf("+2", "-1", "+1", "0", "+3")
        return values[index % values.size]
    }

    private fun castColors(index: Int): Pair<String, String> {
        val colors = listOf(
            "FFD6EA" to "FFB5D1",
            "E7D9FF" to "C7B6FF",
            "FFE3D4" to "FFC3B8"
        )
        return colors[index % colors.size]
    }

    private fun cafeColors(index: Int): Pair<String, String> {
        val colors = listOf(
            "FFE1C7" to "FFCEAE",
            "FFD8EB" to "FFC1DD",
            "D7F0FF" to "B5E4FF"
        )
        return colors[index % colors.size]
    }

    private fun castSymbol(index: Int): String {
        val symbols = listOf("🎀", "🫖", "🍓", "🩷", "🌟")
        return symbols[index % symbols.size]
    }

    private fun cafeSymbol(index: Int): String {
        val symbols = listOf("☕", "🍰", "🎀", "🧁")
        return symbols[index % symbols.size]
    }
}

private const val RANKING_FETCH_LIMIT = 50
