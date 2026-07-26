package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.Report
import com.hhp227.concafe.domain.model.ReportCreate
import com.hhp227.concafe.domain.model.ReportTargetType
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.ReportRepository

class CreateCommunityPostReportUseCase(
    private val authRepository: AuthRepository,
    private val reportRepository: ReportRepository
) {
    suspend operator fun invoke(postId: String, reportType: String): AppResult<Report> {
        return createReport(
            targetType = ReportTargetType.COMMUNITY_POST,
            targetId = postId,
            reportType = reportType
        )
    }

    suspend fun createCommentReport(postId: String, commentId: String, reportType: String): AppResult<Report> {
        return createReport(
            targetType = ReportTargetType.COMMUNITY_COMMENT,
            targetId = "${postId.trim()}/${commentId.trim()}",
            reportType = reportType
        )
    }

    private suspend fun createReport(
        targetType: ReportTargetType,
        targetId: String,
        reportType: String
    ): AppResult<Report> {
        return try {
            val currentUser = authRepository.getCurrentUser()
                ?: return AppResult.Failure(AppError.Unauthorized)
            AppResult.Success(
                reportRepository.createReport(
                    reporterUserId = currentUser.id,
                    reporterNickname = currentUser.nickname,
                    input = ReportCreate(
                        targetType = targetType,
                        targetId = targetId,
                        reportType = reportType
                    )
                )
            )
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid request"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
