package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.data.repository.test.FakeAuthRepository
import com.hhp227.concafe.data.repository.test.FakeUserRepository
import com.hhp227.concafe.data.source.MockConCafeDataSource
import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.User
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UpdateUserDormantStatusUseCaseTest {
    @Test
    fun adminMarksVisitorAsDormant() = runBlocking {
        val dataSource = MockConCafeDataSource()
        val authRepository = FakeAuthRepository(dataSource)
        val userRepository = FakeUserRepository(dataSource)
        val useCase = UpdateUserDormantStatusUseCase(authRepository, userRepository)
        dataSource.currentUserId = "user-4"

        val result = useCase(userId = "user-1", dormant = true)

        assertIs<AppResult.Success<User>>(result)
        assertTrue(result.data.dormant)
        assertNotNull(result.data.dormantAt)
        assertTrue(dataSource.users.first { it.id == "user-1" }.dormant)
    }

    @Test
    fun adminReleasesDormantAccount() = runBlocking {
        val dataSource = MockConCafeDataSource()
        val authRepository = FakeAuthRepository(dataSource)
        val userRepository = FakeUserRepository(dataSource)
        val useCase = UpdateUserDormantStatusUseCase(authRepository, userRepository)
        dataSource.currentUserId = "user-4"
        useCase(userId = "user-1", dormant = true)

        val result = useCase(userId = "user-1", dormant = false)

        assertIs<AppResult.Success<User>>(result)
        assertEquals(false, result.data.dormant)
        assertNull(result.data.dormantAt)
        assertEquals(false, dataSource.users.first { it.id == "user-1" }.dormant)
    }

    @Test
    fun adminCannotMarkAdminAccountAsDormant() = runBlocking {
        val dataSource = MockConCafeDataSource()
        val authRepository = FakeAuthRepository(dataSource)
        val userRepository = FakeUserRepository(dataSource)
        val useCase = UpdateUserDormantStatusUseCase(authRepository, userRepository)
        dataSource.currentUserId = "user-4"

        val result = useCase(userId = "user-4", dormant = true)

        assertIs<AppResult.Failure>(result)
        assertEquals(AppError.PermissionDenied, result.error)
    }

    @Test
    fun nonAdminGetsPermissionDenied() = runBlocking {
        val dataSource = MockConCafeDataSource()
        val authRepository = FakeAuthRepository(dataSource)
        val userRepository = FakeUserRepository(dataSource)
        val useCase = UpdateUserDormantStatusUseCase(authRepository, userRepository)
        dataSource.currentUserId = "user-1"

        val result = useCase(userId = "user-2", dormant = true)

        assertIs<AppResult.Failure>(result)
        assertEquals(AppError.PermissionDenied, result.error)
    }

    @Test
    fun unknownUserGetsNotFound() = runBlocking {
        val dataSource = MockConCafeDataSource()
        val authRepository = FakeAuthRepository(dataSource)
        val userRepository = FakeUserRepository(dataSource)
        val useCase = UpdateUserDormantStatusUseCase(authRepository, userRepository)
        dataSource.currentUserId = "user-4"

        val result = useCase(userId = "no-such-user", dormant = true)

        assertIs<AppResult.Failure>(result)
        assertEquals(AppError.NotFound, result.error)
    }
}
