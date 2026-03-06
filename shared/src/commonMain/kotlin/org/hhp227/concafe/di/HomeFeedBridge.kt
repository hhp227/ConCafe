package org.hhp227.concafe.di

import org.hhp227.concafe.domain.common.AppResult
import org.hhp227.concafe.domain.usecase.GetHomeFeedUseCase
import org.koin.core.context.GlobalContext

fun provideGetHomeFeedUseCase(): GetHomeFeedUseCase {
    return GlobalContext.get().get()
}

suspend fun executeGetHomeFeedUseCase(
    useCase: GetHomeFeedUseCase,
    limit: Int
) = when (val result = useCase(limit)) {
    is AppResult.Success -> result.data
    is AppResult.Failure -> null
}
