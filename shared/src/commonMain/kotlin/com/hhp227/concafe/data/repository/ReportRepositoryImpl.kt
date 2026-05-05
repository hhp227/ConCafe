package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.ReportRemoteDataSource
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.Report
import com.hhp227.concafe.domain.model.ReportCreate
import com.hhp227.concafe.domain.repository.ReportRepository

class ReportRepositoryImpl(
    private val reportRemoteDataSource: ReportRemoteDataSource
) : ReportRepository {
    override suspend fun createReport(
        reporterUserId: String,
        reporterNickname: String,
        input: ReportCreate
    ): Report {
        if (input.targetId.isBlank()) throw IllegalArgumentException("신고 대상이 올바르지 않습니다.")
        if (input.reportType.isBlank()) throw IllegalArgumentException("신고 유형을 선택해 주세요.")
        return reportRemoteDataSource.createReport(
            reporterUserId = reporterUserId,
            reporterNickname = reporterNickname,
            input = input
        )
    }

    override suspend fun getReportPage(cursor: String?, pageSize: Int): PagedResult<Report> {
        return reportRemoteDataSource.fetchReportPage(cursor = cursor, pageSize = pageSize.coerceAtLeast(1))
    }
}
