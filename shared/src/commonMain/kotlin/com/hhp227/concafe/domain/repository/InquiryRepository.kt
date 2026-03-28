package com.hhp227.concafe.domain.repository

import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.Inquiry
import com.hhp227.concafe.domain.model.InquiryCreate

interface InquiryRepository {
    suspend fun createInquiry(
        userId: String,
        userNickname: String,
        input: InquiryCreate
    ): Inquiry

    suspend fun getInquiryPage(
        cursor: String?,
        pageSize: Int
    ): PagedResult<Inquiry>
}
