package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.repository.CafeRepository

class UpdateCafeSocialMediaUseCase(
    private val cafeRepository: CafeRepository
) {
    suspend operator fun invoke(
        cafeId: String,
        instagramId: String?,
        twitterId: String?,
        tiktokId: String?,
        youtubeId: String?
    ): AppResult<Unit> {
        return try {
            cafeRepository.updateCafeSocialMedia(
                cafeId = cafeId,
                instagramId = instagramId?.trim()?.takeIf { it.isNotEmpty() },
                twitterId = twitterId?.trim()?.takeIf { it.isNotEmpty() },
                tiktokId = tiktokId?.trim()?.takeIf { it.isNotEmpty() },
                youtubeId = youtubeId?.trim()?.takeIf { it.isNotEmpty() }
            )
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
