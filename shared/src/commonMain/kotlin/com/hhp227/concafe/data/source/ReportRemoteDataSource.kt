package com.hhp227.concafe.data.source

import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.Report
import com.hhp227.concafe.domain.model.ReportCreate

interface ReportRemoteDataSource {
    suspend fun createReport(
        reporterUserId: String,
        reporterNickname: String,
        input: ReportCreate
    ): Report

    suspend fun fetchReportPage(
        cursor: String?,
        pageSize: Int
    ): PagedResult<Report>
}
