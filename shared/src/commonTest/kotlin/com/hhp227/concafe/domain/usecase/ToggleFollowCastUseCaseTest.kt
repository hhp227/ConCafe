package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.data.repository.test.FakeAuthRepository
import com.hhp227.concafe.data.repository.test.FakeCastRepository
import com.hhp227.concafe.data.source.MockConCafeDataSource
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.event.CastEvent
import com.hhp227.concafe.domain.event.publisher.CastEventPublisher
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ToggleFollowCastUseCaseTest {
    @Test
    fun followPublishesIncrementedFollowerCountEvenWhenRepositorySnapshotIsStale() = runBlocking {
        val dataSource = MockConCafeDataSource()
        val authRepository = FakeAuthRepository(dataSource)
        val castRepository = FakeCastRepository(dataSource)
        val castEventPublisher = CastEventPublisher()
        val useCase = ToggleFollowCastUseCase(authRepository, castRepository, castEventPublisher)
        val eventDeferred = async { castEventPublisher.events.first() }

        val result = useCase("maid-2")
        val event = eventDeferred.await()

        assertIs<AppResult.Success<Boolean>>(result)
        assertEquals(true, result.data)
        assertIs<CastEvent.Updated>(event)
        assertEquals("maid-2", event.cast.id)
        assertEquals(988, event.cast.followerCount)
        assertEquals(true, event.isFollowing)
    }

    @Test
    fun unfollowPublishesDecrementedFollowerCountEvenWhenRepositorySnapshotIsStale() = runBlocking {
        val dataSource = MockConCafeDataSource()
        val authRepository = FakeAuthRepository(dataSource)
        val castRepository = FakeCastRepository(dataSource)
        val castEventPublisher = CastEventPublisher()
        val useCase = ToggleFollowCastUseCase(authRepository, castRepository, castEventPublisher)
        val eventDeferred = async { castEventPublisher.events.first() }

        val result = useCase("maid-1")
        val event = eventDeferred.await()

        assertIs<AppResult.Success<Boolean>>(result)
        assertEquals(false, result.data)
        assertIs<CastEvent.Updated>(event)
        assertEquals("maid-1", event.cast.id)
        assertEquals(1233, event.cast.followerCount)
        assertEquals(false, event.isFollowing)
    }
}
