package org.hhp227.concafe.domain.usecase

import org.hhp227.concafe.domain.common.AppError
import org.hhp227.concafe.domain.common.AppResult
import org.hhp227.concafe.domain.model.HomeFeed
import org.hhp227.concafe.domain.repository.CafeRepository

class GetHomeFeedUseCase(
    private val cafeRepository: CafeRepository
) {
    suspend operator fun invoke(limit: Int): AppResult<HomeFeed> {
        return try {
            AppResult.Success(cafeRepository.getHomeFeed(limit))
        } catch (e: NoSuchElementException) {
            AppResult.Failure(AppError.NotFound)
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid request"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }

}
