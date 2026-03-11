package org.hhp227.concafe.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.hhp227.concafe.domain.repository.CastRepository

class ObserveCafeCastVersionUseCase(
    private val castRepository: CastRepository
) {
    operator fun invoke(cafeId: String): Flow<Int> {
        return castRepository.observeCafeCastVersion(cafeId)
    }
}
