package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.InquiryRemoteDataSource
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.Inquiry
import com.hhp227.concafe.domain.model.InquiryCreate
import com.hhp227.concafe.domain.repository.InquiryRepository

class InquiryRepositoryImpl(
    private val inquiryRemoteDataSource: InquiryRemoteDataSource
) : InquiryRepository {
    override suspend fun createInquiry(
        userId: String,
        userNickname: String,
        input: InquiryCreate
    ): Inquiry {
        if (input.inquiryType.isBlank()) throw IllegalArgumentException("문의 유형을 선택해 주세요.")
        if (input.title.isBlank()) throw IllegalArgumentException("문의 제목을 입력해 주세요.")
        if (input.content.isBlank()) throw IllegalArgumentException("문의 내용을 입력해 주세요.")
        return inquiryRemoteDataSource.createInquiry(
            userId = userId,
            userNickname = userNickname,
            input = input
        )
    }

    override suspend fun getInquiryPage(
        cursor: String?,
        pageSize: Int
    ): PagedResult<Inquiry> {
        val safePageSize = pageSize.coerceAtLeast(1)
        return inquiryRemoteDataSource.fetchInquiryPage(
            cursor = cursor,
            pageSize = safePageSize
        )
    }
}
