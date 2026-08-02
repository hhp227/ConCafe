package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.data.repository.test.FakeAuthRepository
import com.hhp227.concafe.data.repository.test.FakeUserRepository
import com.hhp227.concafe.data.source.MockConCafeDataSource
import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.AuthProvider
import com.hhp227.concafe.domain.model.DormantAccountFilter
import com.hhp227.concafe.domain.model.User
import com.hhp227.concafe.domain.model.UserRole
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GetDormantAccountPageUseCaseTest {
    private fun buildUser(
        id: String,
        dormant: Boolean,
        lastLoginAt: String?,
        dormantAt: String? = null
    ): User {
        return User(
            id = id,
            email = "$id@concafe.app",
            nickname = id,
            profileImage = null,
            authProvider = AuthProvider.EMAIL,
            role = UserRole.VISITOR,
            banned = false,
            createdAt = "2026-01-01T00:00:00Z",
            lastLoginAt = lastLoginAt,
            dormant = dormant,
            dormantAt = dormantAt
        )
    }

    @Test
    fun adminGetsOnlyDormantAccountsWithDormantFilter() = runBlocking {
        val dataSource = MockConCafeDataSource()
        val authRepository = FakeAuthRepository(dataSource)
        val userRepository = FakeUserRepository(dataSource)
        val useCase = GetDormantAccountPageUseCase(authRepository, userRepository)
        dataSource.currentUserId = "user-4"
        dataSource.users.add(buildUser(id = "dormant-1", dormant = true, lastLoginAt = "2024-01-01T00:00:00Z", dormantAt = "2025-01-01T00:00:00Z"))
        dataSource.users.add(buildUser(id = "active-1", dormant = false, lastLoginAt = "2026-08-01T00:00:00Z"))

        val result = useCase(filter = DormantAccountFilter.DORMANT, cursor = null)

        assertIs<AppResult.Success<PagedResult<User>>>(result)
        assertEquals(listOf("dormant-1"), result.data.items.map { it.id })
    }

    @Test
    fun adminGetsLongInactiveAccountsWithCandidateFilter() = runBlocking {
        val dataSource = MockConCafeDataSource()
        val authRepository = FakeAuthRepository(dataSource)
        val userRepository = FakeUserRepository(dataSource)
        val useCase = GetDormantAccountPageUseCase(authRepository, userRepository)
        dataSource.currentUserId = "user-4"
        dataSource.users.add(buildUser(id = "stale-1", dormant = false, lastLoginAt = "2024-01-01T00:00:00Z"))
        dataSource.users.add(buildUser(id = "dormant-1", dormant = true, lastLoginAt = "2024-01-01T00:00:00Z"))
        dataSource.users.add(buildUser(id = "active-1", dormant = false, lastLoginAt = "2026-08-01T00:00:00Z"))

        val result = useCase(filter = DormantAccountFilter.CANDIDATE, cursor = null)

        assertIs<AppResult.Success<PagedResult<User>>>(result)
        assertEquals(listOf("stale-1"), result.data.items.map { it.id })
    }

    @Test
    fun candidatePageSupportsCursorPaging() = runBlocking {
        val dataSource = MockConCafeDataSource()
        val authRepository = FakeAuthRepository(dataSource)
        val userRepository = FakeUserRepository(dataSource)
        val useCase = GetDormantAccountPageUseCase(authRepository, userRepository)
        dataSource.currentUserId = "user-4"
        dataSource.users.add(buildUser(id = "stale-1", dormant = false, lastLoginAt = "2024-01-01T00:00:00Z"))
        dataSource.users.add(buildUser(id = "stale-2", dormant = false, lastLoginAt = "2024-02-01T00:00:00Z"))
        dataSource.users.add(buildUser(id = "stale-3", dormant = false, lastLoginAt = "2024-03-01T00:00:00Z"))

        val firstPage = useCase(filter = DormantAccountFilter.CANDIDATE, cursor = null, pageSize = 2)

        assertIs<AppResult.Success<PagedResult<User>>>(firstPage)
        assertEquals(listOf("stale-1", "stale-2"), firstPage.data.items.map { it.id })
        assertTrue(firstPage.data.hasNext)

        val secondPage = useCase(filter = DormantAccountFilter.CANDIDATE, cursor = firstPage.data.nextCursor, pageSize = 2)

        assertIs<AppResult.Success<PagedResult<User>>>(secondPage)
        assertEquals(listOf("stale-3"), secondPage.data.items.map { it.id })
        assertEquals(false, secondPage.data.hasNext)
    }

    @Test
    fun nonAdminGetsPermissionDenied() = runBlocking {
        val dataSource = MockConCafeDataSource()
        val authRepository = FakeAuthRepository(dataSource)
        val userRepository = FakeUserRepository(dataSource)
        val useCase = GetDormantAccountPageUseCase(authRepository, userRepository)
        dataSource.currentUserId = "user-1"

        val result = useCase(filter = DormantAccountFilter.DORMANT, cursor = null)

        assertIs<AppResult.Failure>(result)
        assertEquals(AppError.PermissionDenied, result.error)
    }

    @Test
    fun guestGetsUnauthorized() = runBlocking {
        val dataSource = MockConCafeDataSource()
        val authRepository = FakeAuthRepository(dataSource)
        val userRepository = FakeUserRepository(dataSource)
        val useCase = GetDormantAccountPageUseCase(authRepository, userRepository)
        dataSource.currentUserId = null

        val result = useCase(filter = DormantAccountFilter.DORMANT, cursor = null)

        assertIs<AppResult.Failure>(result)
        assertEquals(AppError.Unauthorized, result.error)
    }
}
