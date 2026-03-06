package org.hhp227.concafe.domain.usecase

import org.hhp227.concafe.domain.common.AppResult
import org.hhp227.concafe.domain.model.HomeFeed

class HomeUseCaseWrapper(
    private val getHomeFeedUseCase: GetHomeFeedUseCase
) {
    suspend fun getHomeFeedOrNull(limit: Int): HomeFeed? {
        return when (val result = getHomeFeedUseCase(limit)) {
            is AppResult.Success -> result.data
            is AppResult.Failure -> null
        }
    }
}
