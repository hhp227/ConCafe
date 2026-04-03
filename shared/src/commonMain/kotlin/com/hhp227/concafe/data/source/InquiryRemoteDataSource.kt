package com.hhp227.concafe.data.source

import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.Inquiry
import com.hhp227.concafe.domain.model.InquiryCreate

interface InquiryRemoteDataSource {
    suspend fun createInquiry(
        userId: String,
        userNickname: String,
        input: InquiryCreate
    ): Inquiry

    suspend fun fetchInquiryPage(
        cursor: String?,
        pageSize: Int
    ): PagedResult<Inquiry>
}
