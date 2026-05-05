package com.hhp227.concafe.domain.repository

import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.Report
import com.hhp227.concafe.domain.model.ReportCreate

interface ReportRepository {
    suspend fun createReport(
        reporterUserId: String,
        reporterNickname: String,
        input: ReportCreate
    ): Report

    suspend fun getReportPage(
        cursor: String?,
        pageSize: Int
    ): PagedResult<Report>
}
