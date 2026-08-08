package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.model.ContentLayout
import com.hhp227.concafe.domain.repository.UserPreferenceRepository
import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import kotlinx.coroutines.flow.Flow

class ObserveContentLayoutUseCase(
    private val repository: UserPreferenceRepository
) {
    @NativeCoroutines
    operator fun invoke(): Flow<ContentLayout> {
        return repository.observeContentLayout()
    }
}
