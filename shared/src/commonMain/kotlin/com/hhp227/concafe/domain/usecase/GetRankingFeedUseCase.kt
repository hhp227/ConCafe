package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.RankingFeed
import com.hhp227.concafe.domain.model.RankingFeedEntry
import com.hhp227.concafe.domain.model.RankingPeriod
import com.hhp227.concafe.domain.model.RankingPromoAd
import com.hhp227.concafe.domain.repository.RankingRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class GetRankingFeedUseCase(
    private val rankingRepository: RankingRepository
) {
    suspend operator fun invoke(
        period: RankingPeriod,
        country: String?,
        city: String?
    ): AppResult<RankingFeed> = coroutineScope {
        val castDeferred = async {
            runCatching {
                rankingRepository.getCastRanking(period = period, country = country, city = city)
            }
        }
        val cafeDeferred = async {
            runCatching {
                rankingRepository.getCafeRanking(period = period, country = country, city = city)
            }
        }
        val castResult = castDeferred.await()
        val cafeResult = cafeDeferred.await()
        val allFailed = castResult.isFailure && cafeResult.isFailure
        val firstError = castResult.exceptionOrNull() ?: cafeResult.exceptionOrNull()

        if (allFailed) {
            val throwable = firstError
            return@coroutineScope when (throwable) {
                is NoSuchElementException -> {
                    AppResult.Failure(AppError.NotFound)
                }
                is IllegalArgumentException -> {
                    AppResult.Failure(AppError.ValidationFailed(throwable.message ?: "invalid request"))
                }
                else -> {
                    AppResult.Failure(AppError.Unknown(throwable?.message))
                }
            }
        }
        val castRankings = castResult.getOrElse { emptyList() }.mapIndexed { index, item ->
            RankingFeedEntry(
                id = item.id,
                rank = item.rank,
                name = item.name,
                subtitle = item.subtitle,
                score = item.score,
                change = item.change,
                startColorHex = castColors(index).first,
                endColorHex = castColors(index).second,
                symbol = castSymbol(index)
            )
        }
        val cafeRankings = cafeResult.getOrElse { emptyList() }.mapIndexed { index, item ->
            RankingFeedEntry(
                id = item.id,
                rank = item.rank,
                name = item.name,
                subtitle = item.subtitle,
                score = item.score,
                change = item.change,
                startColorHex = cafeColors(index).first,
                endColorHex = cafeColors(index).second,
                symbol = cafeSymbol(index)
            )
        }
        return@coroutineScope AppResult.Success(
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
