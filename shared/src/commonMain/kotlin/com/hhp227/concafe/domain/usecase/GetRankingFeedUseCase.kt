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
import com.hhp227.concafe.domain.repository.RankingRepository

class GetRankingFeedUseCase(
    private val rankingRepository: RankingRepository,
    private val castRepository: CastRepository,
    private val cafeRepository: CafeRepository
) {
    suspend operator fun invoke(
        period: RankingPeriod,
        country: String?,
        city: String?
    ): AppResult<RankingFeed> {
        return try {
            val castRankings = rankingRepository.getCastRanking(period, country, city)
            val cafeRankings = rankingRepository.getCafeRanking(period, country, city)
            val casts = castRepository.searchCasts(
                query = null,
                country = country,
                city = city,
                sort = CastSort.FOLLOWERS,
                cursor = null,
                pageSize = Int.MAX_VALUE
            ).items.associateBy { it.id }
            val cafes = cafeRepository.searchCafes(
                query = null,
                country = country,
                city = city,
                sort = CafeSort.RATING,
                cursor = null,
                pageSize = Int.MAX_VALUE
            ).items.associateBy { it.id }

            AppResult.Success(
                RankingFeed(
                    ads = defaultAds(),
                    castRankings = castRankings.mapIndexedNotNull { index, item ->
                        val cast = casts[item.id] ?: return@mapIndexedNotNull null
                        val cafe = cafes[cast.cafeId]
                        RankingFeedEntry(
                            id = item.id,
                            rank = item.rank,
                            name = item.name,
                            subtitle = cafe?.name ?: cast.cafeId,
                            score = item.score,
                            change = rankChange(index),
                            startColorHex = castColors(index).first,
                            endColorHex = castColors(index).second,
                            symbol = castSymbol(index)
                        )
                    },
                    cafeRankings = cafeRankings.mapIndexedNotNull { index, item ->
                        val cafe = cafes[item.id] ?: return@mapIndexedNotNull null
                        RankingFeedEntry(
                            id = item.id,
                            rank = item.rank,
                            name = item.name,
                            subtitle = cafe.region.address.substringBefore("구").substringBefore("로").ifBlank { cafe.region.city },
                            score = item.score,
                            change = rankChange(index + 1),
                            startColorHex = cafeColors(index).first,
                            endColorHex = cafeColors(index).second,
                            symbol = cafeSymbol(index)
                        )
                    }
                )
            )
        } catch (e: NoSuchElementException) {
            AppResult.Failure(AppError.NotFound)
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid request"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
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
