package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.HomePopularCastPage
import com.hhp227.concafe.domain.repository.CafeRepository
import com.hhp227.concafe.domain.repository.CastRepository

class GetPopularCastPageUseCase(
    private val castRepository: CastRepository,
    private val cafeRepository: CafeRepository
) {
    suspend operator fun invoke(cursor: String?): AppResult<HomePopularCastPage> {
        return try {
            val page = castRepository.getHomePopularCastPage(cursor = cursor, pageSize = PAGE_SIZE)
            val cafeIds = page.items.map { it.cafeId }.distinct()
            val cafeNames = if (cafeIds.isEmpty()) {
                emptyMap()
            } else {
                runCatching {
                    cafeRepository.getCafesByIds(cafeIds).associate { it.id to it.name }
                }.getOrElse { emptyMap() }
            }

            AppResult.Success(
                HomePopularCastPage(
                    casts = page.items,
                    cafeNames = cafeNames,
                    nextCursor = page.nextCursor,
                    hasNext = page.hasNext
                )
            )
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }

    companion object {
        private const val PAGE_SIZE = 15
    }
}
