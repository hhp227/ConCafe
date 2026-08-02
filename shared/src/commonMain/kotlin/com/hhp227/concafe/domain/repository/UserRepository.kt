package com.hhp227.concafe.domain.repository

import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.AdminUserFilter
import com.hhp227.concafe.domain.model.DormantAccountFilter
import com.hhp227.concafe.domain.model.MyPageSummary
import com.hhp227.concafe.domain.model.User

interface UserRepository {
    suspend fun getUser(userId: String): User

    suspend fun getAdminUserPage(
        filter: AdminUserFilter,
        cursor: String?,
        pageSize: Int
    ): PagedResult<User>

    suspend fun getDormantAccountPage(
        filter: DormantAccountFilter,
        lastLoginBefore: String,
        cursor: String?,
        pageSize: Int
    ): PagedResult<User>

    suspend fun updateDormantStatus(userId: String, dormant: Boolean): User

    suspend fun updateProfile(userId: String, nickname: String, profileImage: String?)

    suspend fun getMyPageSummary(userId: String): MyPageSummary
}
