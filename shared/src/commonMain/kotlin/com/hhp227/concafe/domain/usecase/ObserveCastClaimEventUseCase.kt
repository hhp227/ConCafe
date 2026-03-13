package com.hhp227.concafe.domain.usecase

import kotlinx.coroutines.flow.Flow
import com.hhp227.concafe.domain.model.CastClaimEvent
import com.hhp227.concafe.domain.repository.CastClaimRepository

class ObserveCastClaimEventUseCase(
    private val castClaimRepository: CastClaimRepository
) {
    operator fun invoke(): Flow<CastClaimEvent> = castClaimRepository.observeCastClaimEvent()
}
